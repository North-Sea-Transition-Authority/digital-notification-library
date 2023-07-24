# What does the data model look like?

* Status: proposed
* Deciders:
* Date: 2023-07-13

Technical Story: [FN-47](https://fivium.atlassian.net/browse/FN-47]) and [FN-48](https://fivium.atlassian.net/browse/FN-48)

## Context and Problem Statement

The library will store notifications in the database so that they can be scheduled, retried and viewed as an audit
of notifications that have been sent.

## Decision Drivers

* Simply data model for storing notification data
* Ideally not enforce consumers have tables they don't need if they don't want to send SMS for example

## Considered Options

* Option 1: Single table for all notification types
* Option 2: A table per notification type
* Option 3: Separate table for personalisation

## Decision Outcome

Regardless of the approach taken the data model will be created via flyway migrations within the library. The consumer
will not be required to create the necessary tables. This is the same approach as taken for the
[file upload library](https://github.com/Fivium/file-upload-library).

I propose option 1, (Single table for all notification types) is used. This option involves the least duplication
and least numbers of rows per notification. If there was a need to move to option 2 in the future we can easily migrate
to it from the option 1 based on the `type` column. Until we have a requirement to capture notification specific data
I suggest we avoid the overhead in that model and enforcing all consumers to have sms and email tables when they might
not need them.

## Pros and Cons of the Options

### Option 1: Single table for all notification types

The only thing that is different for the both email and sms notifications is whether the recipient address is an email
or a phone number. It seems logic to just have a single table and just differentiate the notifications with a `TYPE`
column.

`notification_library_outbound_notifications`

| Name                  | Data type    | Nullable? | Notes                                                        |
|:----------------------|:-------------|:----------|:-------------------------------------------------------------|
| id                    | UUID         | No        | A unique identifier for the notification                     |
| type                  | VARCHAR(255) | No        | The type of notification, e.g. `EMAIL` or `SMS`              |
| notify_template_id    | VARCHAR(255) | No        | The ID of the Notify template to use                         |
| recipient             | VARCHAR(255) | No        | Either the email address or phone number of the recipient    |
| domain_reference_id   | TEXT         | No        | An ID for a domain object relating to a notification         |
| domain_reference_type | TEXT         | No        | A type of domain object the ID relates to e.g. `APPLICATION` |
| log_correlation_id    | TEXT         | Yes       | An optional log correlation ID                               |
| mail_merge_data       | JSON         | No        | A JSON document containing all the mail merge fields         |
| status                | VARCHAR(255) | No        | The library status of the notification                       |
| provider_status       | VARCHAR(255) | No        | The status of the notification from GOV.UK Notify            |
| created_at            | TIMESTAMP    | No        | The time the notification was created in this table          |
| sent_at               | TIMESTAMP    | Yes       | The time the notification was send by GOV.UK Notify          |
| retried_at            | TIMESTAMP    | Yes       | The last time the notification attempted to be resent        |
| retry_attempts        | INT          | Yes       | The number of times the notification has tried to be resent  |

If a consumer wanted to send one email and one SMS for the same template, domain reference and log correlation the
data model would look like the below:

| id      | type  | notify_template_id | recipient           | domain_reference_id | domain_reference_type | log_correlation_id | mail_merge_data                                            | status          | provider_status   | created_at          | sent_at             | retried_at          | retry_attempts |
|:--------|:------|:-------------------|:--------------------|:--------------------|:----------------------|:-------------------|:-----------------------------------------------------------|:----------------|:------------------|:--------------------|:--------------------|:--------------------|:---------------|
| 123-456 | EMAIL | 456-789            | someone@example.com | 789-123             | APPLICATION           | 123-abc            | { "mail-merge-fields": [{"key": "name", "value": "Jill"}]} | DELIVERED       | DELIVERED         | 2023-07-12 13:05:06 | 2023-07-12 13:05:16 | null                | 0              |
| 456-123 | SMS   | 456-789            | 07915832679         | 789-123             | APPLICATION           | 123-abc            | { "mail-merge-fields": [{"key": "name", "value": "Jill"}]} | FAILED_RETRYING | TEMPORARY_FAILURE | 2023-07-12 13:05:06 | null                | 2023-07-12 14:05:00 | 1              |


* Good, because it reduces the need to near identical tables per type of notification
* Good, because we don't need an additional table for mail merge data
* Good, because we can easily split out into notification specific tables based on `type` column if we need to in future
* Good, because it solves problem of consumers having a table for SMS notifications even if they don't have a
  requirement to sent SMS.
* Bad, because if we need to have a column specific to a single notification type then it would likely require a data
  model change or optional columns added to this table

### Option 2: A table per notification type

This option is very similar to option 1 with the only difference being there is a table per notification type. All
other columns are identical.

`notification_library_email_notifications`

| Name                  | Data type     | Nullable? | Notes                                                        |
|:----------------------|:--------------|:----------|:-------------------------------------------------------------|  
| id                    | UUID          | No        | A unique identifier for the notification                     |
| notify_template_id    | VARCHAR(255)  | No        | The ID of the Notify template to use                         |
| recipient             | VARCHAR2(255) | No        | The email address of the recipient                           |
| domain_reference_id   | TEXT          | No        | An ID for a domain object relating to a notification         |
| domain_reference_type | TEXT          | No        | A type of domain object the ID relates to e.g. `APPLICATION` |
| log_correlation_id    | TEXT          | Yes       | An optional log correlation ID                               |
| mail_merge_data       | JSON          | No        | A JSON document containing all the mail merge fields         |
| status                | VARCHAR(255)  | No        | The library status of the notification                       |
| provider_status       | VARCHAR(255)  | No        | The status of the notification from GOV.UK Notify            |
| created_at            | TIMESTAMP     | No        | The time the notification was created in this table          |
| sent_at               | TIMESTAMP     | Yes       | The time the notification was send by GOV.UK Notify          |
| retried_at            | TIMESTAMP     | Yes       | The last time the notification attempted to be resent        |
| retry_attempts        | INT           | Yes       | The number of times the notification has tried to be resent  |

`notification_library_sms_notifications`

| Name                  | Data type     | Nullable? | Notes                                                        |
|:----------------------|:--------------|:----------|:-------------------------------------------------------------| 
| id                    | UUID          | No        | A unique identifier for the notification                     |
| notify_template_id    | VARCHAR(255)  | No        | The ID of the Notify template to use                         |
| recipient             | VARCHAR2(255) | No        | The phone number of the recipient                            |
| domain_reference_id   | TEXT          | No        | An ID for a domain object relating to a notification         |
| domain_reference_type | TEXT          | No        | A type of domain object the ID relates to e.g. `APPLICATION` |
| log_correlation_id    | TEXT          | Yes       | An optional log correlation ID                               |
| mail_merge_data       | JSON          | No        | A JSON document containing all the mail merge fields         |
| status                | VARCHAR(255)  | No        | The library status of the notification                       |
| provider_status       | VARCHAR(255)  | No        | The status of the notification from GOV.UK Notify            |
| created_at            | TIMESTAMP     | No        | The time the notification was created in this table          |
| sent_at               | TIMESTAMP     | Yes       | The time the notification was send by GOV.UK Notify          |
| retried_at            | TIMESTAMP     | Yes       | The last time the notification attempted to be resent        |
| retry_attempts        | INT           | Yes       | The number of times the notification has tried to be resent  |

* Good, because if we need to add specific columns for different notifications types we can do so easily
* Bad, because consumers would need to have both tables even if they only support one notification type. This could be
  mitigated with a way for consumers to opt into specific notification types but is unnecessary additional coding
* Bad, because it results in large amounts of duplication between the tables which would require being kept in sync
* Bad, because it requires additional coding for the sending and retry jobs to do the same actions in both tables
* Bad, because consumers have two tables to look in for audit purposes

### Option 3: Separate table for personalisation

Instead of having the mail merge fields in a JSON column this approach has the mail merge data in its own table with a
link back to the notification. This is similar to what the existing Fivium Notify library data model looked like.

The `notification_library_outbound_notifications` is identical to option 1 apart from the removal of the
`mail_merge_data` column.

`notification_library_outbound_notifications`

| Name                  | Data type    | Nullable? | Notes                                                        |
|:----------------------|:-------------|:----------|:-------------------------------------------------------------|  
| id                    | UUID         | No        | A unique identifier for the notification                     |
| type                  | VARCHAR(255) | No        | The type of notification, e.g. `EMAIL` or `SMS`              |
| notify_template_id    | VARCHAR(255) | No        | The ID of the Notify template to use                         |
| recipient             | VARCHAR(255) | No        | Either the email address or phone number of the recipient    |
| domain_reference_id   | TEXT         | No        | An ID for a domain object relating to a notification         |
| domain_reference_type | TEXT         | No        | A type of domain object the ID relates to e.g. `APPLICATION` |
| log_correlation_id    | TEXT         | Yes       | An optional log correlation ID                               |
| status                | VARCHAR(255) | No        | The library status of the notification                       |
| provider_status       | VARCHAR(255) | No        | The status of the notification from GOV.UK Notify            |
| created_at            | TIMESTAMP    | No        | The time the notification was created in this table          |
| sent_at               | TIMESTAMP    | Yes       | The time the notification was send by GOV.UK Notify          |
| retried_at            | TIMESTAMP    | Yes       | The last time the notification attempted to be resent        |
| retry_attempts        | INT          | Yes       | The number of times the notification has tried to be resent  |

`notification_library_mail_merge_fields`

| Name              | Data type    | Nullable? | Notes                                                         |
|:------------------|:-------------|:----------|:--------------------------------------------------------------|
| id                | UUID         | No        | A unique identifier for the notification                      |
| notification_id   | VARCHAR(255) | No        | A foreign key to the notification the mail merge field is for |
| merge_field_name  | VARCHAR(255) | No        | The name of the mail merge field                              |
| merge_field_value | JSON         | No        | The value of the mail merge field                             |

* Good, because there is a separation of concern between mail merge fields and is more of a relational structure
* Bad, because notification usually have lots of mail merge fields so lots of rows will be generated per notification
* Bad, because it requires 2 queries of the database in order to get the data required to send a notification