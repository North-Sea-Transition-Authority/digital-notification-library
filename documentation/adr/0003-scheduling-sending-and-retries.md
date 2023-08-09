# How are we going to handle sending and retries?

* Status: approved
* Deciders: James Barnett, Chris Tasker
* Date: 2023-07-13

Technical Story: [FN-48](https://fivium.atlassian.net/browse/FN-48)

## Context and Problem Statement

We will be using the outbox pattern to send messages to GOV.UK Notify. When consumers request a notification to be
sent, the library won't send it directly but instead, the notifications are persisted in a database table. After that,
a job publish events to GOV.UK Notify at predefined time intervals.

We need to decide how we are going to schedule the following requirements:
* be able to send notifications we have never sent to Notify
* be able to update the status of notifications we have sent to Notify
* be able to resend notifications we received a failure for

### Decision outcome:

The following statuses are used by GOV.UK Notify
* [Email statues](https://docs.notifications.service.gov.uk/java.html#email-status-descriptions)
* [Sms statues](https://docs.notifications.service.gov.uk/java.html#text-message-status-descriptions)

In addition to the Notify status, the library will have its own statuses that are used within the code for logic.
The Notify statuses will not be used to control and logic and just more for information.

The following statuses are proposed:

| Library status     | Mapped Notify statuses                   | Mapped Notify response codes | Description                                                                                                                                                                                                                                                                |
|:-------------------|:-----------------------------------------|:-----------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `QUEUED`           | N/A                                      |                              | The consumer has requested a notification to be sent, but we haven't sent it to GOV.UK Notify yet                                                                                                                                                                          |
| `SENT_TO_PROVIDER` | `created`, `sending`, `pending`          |                              | The library has sent the request to Notify but the notification has not been confirmed as sent                                                                                                                                                                             |
| `DELIVERED`        | `delivered`, `sent`                      |                              | GOV.UK Notify has confirmed the notification has been delivered                                                                                                                                                                                                            |
| `RETRYING`         | `temporary-failure`, `technical-failure` | `4xx` or `5xx`               | GOV.UK Notify wasn't able to send the notification due to an error. The library will retry.                                                                                                                                                                                |
| `FAILED_NOT_SENT`  | `permanent-failure`                      |                              | The notification could not be sent and no further retries will be attempted. This will be the automatic status if Notify responds with `permanent-failure` but will only be the status on `technical-failure` or `temporary-failure` if we have exceed the retry threshold |

Notify details the [email](https://docs.notifications.service.gov.uk/java.html#send-an-email-error-codes) and
[sms](https://docs.notifications.service.gov.uk/java.html#error-codes) error codes you will receive for an unsuccessful
request. For the API limit exceeded or unhandled failure we should treat it as a retry same as a "successful"
`temporary-failure` or `technical-failure` response.

### Schedule a send request to Notify

As soon as the consumer makes a request to the `sendEmail()` or `sendSms()` methods a row will be inserted into the
table with the status of `QUEUED`. A job will poll the table every 10 seconds (but will be customisable by the consumer)
for any notifications with this status. For each notification a request will be sent to Notify and the status of the
notification will be set to `SENT_TO_PROVIDER`. This won't mean it has been sent but has been requested to be sent.

We will limit the amount of notifications that can be processed in each 10 second run to 100 as not to hit the 3000
requests per 60 seconds [rate limit](https://docs.notifications.service.gov.uk/java.html#rate-limits). Some services
send large mail shot emails to thousands of email addresses. We will still need some mechanism in the code to prevent
the same notification being picked up if it does take more than 10 seconds to process 100 notifications.

When Notify sends a notification, a [delivery receipt](https://docs.notifications.service.gov.uk/java.html#delivery-receipts)
will be sent to the callback URL registered in Notify. The problem is if Notify is offline it is unclear if the
delivery receipts will still be sent. Instead of relying on Notify to call back to our services we will have another job
which will poll every 30 seconds and ask Notify to provide us with an update on the statuses of the notifications with
the status `SENT_TO_PROVIDER`. We can then update our statuses based on what Notify sends back.

### Schedule a retry

If the response is `temporary-failure` (the provider could not deliver the message e.g. the recipient's
inbox is full, an anti-spam filter rejects it, phone is off or has no single etc.) or a `technical-failure` (the message
was not sent because there was a problem between Notify and the provider) we should retry to send the notification. To
do this we will need a third job which will poll for any notifications with the status `RETRYING`.

We will have to be aware of not wanting to send a notification too far after it was originally requested as it may no
longer be relevant and could cause confusion. Notify keeps a notification in the `sending` state for up to 72 hours
until it receives a confirmation from the provider that it was delivered. I think having a max of 72 hours for our 
library also makes sense.

I propose having another job which poll and attempt to resend emails and either send once an hour or try to and do an 
exponential back-off, e.g. first retry after 10 seconds, second 30 seconds, 2 minutes, 5 minutes, 30 minutes, 1 hour etc. 
Each time a retry is attempted we will increment the `retry_attempts` column and also set the `retried_at` to be the 
date of the retry.

If the notification is successful on the retry the status will be updated to `DELIVERED`. If after the first or second
retry we still have an `temporary-failure` or `technical-failure` response, the status will be kept at
`RETRYING`. If the same response is returned after the third attempt, we will no longer try to resend and the
status will be set to `FAILED_NOT_SENT`.

If we receive a `permanent-failure` response from Notify the status of the notification will be set to `FAILED_NOT_SENT`
and we will not retry the notification. A permanent failure from the Notify documentation states for this can happen if
the email address or phone number is wrong or if the network operator rejects the message.