# What does the consumer API interaction look like?

* Status: proposed
* Deciders: 
* Date: 2023-06-14

Technical Story: [FN-45](https://jira.fivium.co.uk/browse/FN-45)

## Context and Problem Statement

We need to decide what the API interactions is going to look like between the consuming services and Fivium notify 
library.

## Decision Drivers

* Need to support service being able to send emails and SMS notifications
* No current requirement to support sending letters
* Need to be able to handle log correlation IDs being provided from consumers 
* Easy for consumers to call
* Easy support for future changes to required parameters
* Ensure templates are uniquely identifiable

## Considered Options

* Option 1: Single class and methods for all notification types
* Option 2: Notification specific classes and methods
* Option 3: Option 1 but without a single object param
* Option 4: Overloaded methods for different parameter requirements and notification types

## Some useful GOV.UK notify notes

* GOV.UK notify has a `reference` property consumers can set which appears as the `client_reference` when looking on
  the dashboards. It is used to [uniquely identify a notification or a batch of notifications](https://docs.notifications.service.gov.uk/java.html#send-an-email-arguments-reference-required).
  I am suggesting we use this for our log correlation ID. If you don't provide one then a `client_reference` is not set.
* GOV.UK notify have a [emailReplyToId](https://docs.notifications.service.gov.uk/java.html#emailreplytoid-optional)
  and a [smsSenderId](https://docs.notifications.service.gov.uk/java.html#smssenderid-optional) which are UUIDs which
  link to a reply to email address or the name of the sms sender. These need to be added to Notify manually through the
  web client. As both are UUIDs we could simply a single property on our notification class representing this regardless
  if it is for sms or email.

## Decision Outcome

Regardless of the option chosen we would do a validation prior to sending the request to notify to send which would 
check that all the mail merge fields have been provided (excluding any optionals fields). This can be done by getting 
the template based on ID which will return the mail merge fields for us to compare against the consumer provided fields.

Option 3 would allow us to do this check construction of the mail merge template but with option 1 or 2 we would do 
the check at time of constructing the whole notification object which is less desirable.

I think the preferred options are 1 or 3 with my preference being more towards option 1 to more easily allow optional
values to be provided without needing to have method overloads (optional params being log correlation ID and sender ID).
If we are happy with the method overloading in the client API then option 3 may be preferable. If we ever needed to 
change the signature we could easily deprecate methods and remove in subsequent versions.

## Pros and Cons of the Options 

### Option 1: Single class and methods for all notification types

When comparing the [SMS](https://docs.notifications.service.gov.uk/java.html#send-a-text-message) and
[email](https://docs.notifications.service.gov.uk/java.html#send-an-email) documentation, both notification types take
the same parameters:

- the template ID
- a string representing an email address or a phone number
- a personalisation map of mail merge fields
- a reference identifier for consumers (e.g a log correlation ID)
- an identifier for the sender (UUID representing a reply to email address or name of sender for SMS)

As a result we could create a notification class which represent either an email or an SMS message and streamline our
client API to something as simple as:

```java
class NotificationClient {

  /**
   * Send a single notification
   */
  NotificationResponse sendNotification(Notification notification) {
    // ...
  }

  /**
   * Util for sending multiple notifications. This would just be a forEach and call the above method. This is to avoid 
   * consumers forEach'ing over their recipients because notify doesn't allow bulk send through the Java API. Most 
   * consumers are sending emails to all people in certain roles or teams so is quite a common use case.
   */
  NotificationResponse sendNotifications(Collection<Notification> notifications) {
    // ...
  }

  /**
   * Probably only useful for verify templateIds are correct if used in enums or as environment variables. Mainly see
   * this for use in actuators style checks as opposed to used in the services
   */
  NotificationTemplate getTemplateById(String templateId) {
    // ...
  } 
}
```

Where a `Notification` could look like

```java
class Notification {

  private final String templateId;
  
  private final ContactableRecipient recipient;

  private final Map<String, Object> mailMergeFields;

  private final String logCorrelationId;

  private final String senderId;

  private Notification(...) {
    this.templateId = templateId;
    // other properties excluded
  }

  static Builder builder(String templateId, ContactableRecipient recipient) {
    return new Builder(templateId, recipient);
  }

  static class Builder {

    private final String templateId;

    // other properties excluded

    Builder(String templateId, String recipient) {
      this.templateId = templateId;
      this.recipient = recipient;
    }

    Builder withMergeField(String key, Object value) {
      mailMergeFields.put(key, value);
      return this;
    }

    Builder withMergeFields(Map<String, Object> mailMergeFields) {
      this.mailMergeFields.putAll(mailMergeFields);
      return this;
    }
    
    // other builder methods excluded

    Notification build() {
      return new Notification(...);
    }
  }
}
```

Where a `ContactableRecipient` would be an interface (see below) that consumers could add to their object that represent
users. For example, a common use case is email all members of a team so consumers would already have objects representing
a team member and could implement the following interface. For contact single email addresses or phone numbers such as
regulator shared email inboxes they could use the `ContactableRecipient.withEmailAddress("generic@regulator.com)`.

```java
interface ContactableRecipient {
  
  String getNotificationEmailAddress();

  String getNotificationSmsNumber();

  static ContactableRecipient withEmailAddress(String emailAddress) {
    return new ContactableRecipient() {
      @Override
      public String getNotificationEmailAddress() {
        return emailAddress;
      }

      @Override
      public String getNotificationSmsNumber() {
        throw new UnsupportedOperationException("not allowed");
      }
    };
  }

  static ContactableRecipient withSmsNumber(String smsNumber) {
    return new ContactableRecipient() {
      @Override
      public String getNotificationEmailAddress() {
        throw new UnsupportedOperationException("not allowed");
      }

      @Override
      public String getNotificationSmsNumber() {
        return smsNumber;
      }
    };
  }
}
```

In our call to the notify API we would first get the template using the ID provided and then check the type and call
the relevant notify method, for example:

```java
class NotificationService {

  NotificationResponse sendNotification(Notification notification) {
    var template = govkNotifyClient.getTemplateById(notification.getTemplateId());
    return switch (template.getType) {
      EMAIL -> govukNotifyClient.sendEmail(notification.getRecipient().getEmailAddress(), ...);
      SMS -> govukNotifyClient.sendSms(notification.getRecipient().getSmsNumber(), ...);
    };
  }
}
```

If a consumer wanted to send an email or an SMS they would simply do

```java
var notification = Notification.builder("template-id", contactableRecipient)
    .withMergeField("someMailMergeField", "some mail merge value")
    .withLogCorrelationId("log-correlation-id")
    .build();

notificationClient.sendNotification(notification);
```

* Good, because only provides method we actually have use cases for
* Good, because we don't need to provide overloaded methods for optional params for both sms and email methods
* Good, because using single objects as parameter avoids breaking changes to consumers or needing to have deprecated
  methods hanging around in the client
* Good, because we could provide a method to send a collection of notifications instead of the consumers needing to loop and send
* Good, because if the consumer wanted to switch the template from email to SMS they don't need to change the code, just the ID
* Bad, because would consumers be expecting `sendSms` and `sendEmail` methods like gov client provides? (do we care?)
* Bad, because wouldn't easily support letter notification types as not all properties are the same (do we care?)
* Bad, because there isn't a good separation of building the mail merge template and providing other information e.g log correlation, sender ID etc

### Option 2: Notification specific classes and methods

In a similar concept to option 1 we could have specific classes representing an email and sms notification and then
specific email and sms methods exposed to the consumers. Assumption here is that both `EmailNotification` and 
`TextNotification` classes are similar to the `Notification` class used in option 1. The log correlation ID and sender 
ID concepts would be the same here just wrapped inside other classes.

```java
class NotificationClient {
  
  NotificationResponse sendEmail(EmailNotification notification) {
    // ...
  }
  
  NotificationResponse sendEmails(Collection<EmailNotification> notifications) {
    // ...
  }

  NotificationResponse sendText(TextNotification notification) {
    // ...
  }

  NotificationResponse sendTexts(Collection<TextNotification> notifications) {
    // ...
  }

  NotificationTemplate getTemplateById(String templateId) {
    // ...
  } 
}
```

* Good, because might be a more expected API from the consumers perspective and similar to methods on the notify client
* Good, because only exposes methods we know we have use cases for
* Good, because using single objects as parameter avoids breaking changes to consumers or needing to have deprecated
  methods hanging around in the client
* Good, because we can easily extend to support letter types if we needed to as represented by different objects 
  (not sure if we would ever want to, but it would work)
* Bad, because `EmailNotification` and `TextNotification` objects would be the same under the hood 
  (would likely mitigate with abstract class)
* Bad, because if you wanted to change from an email to sms notification it would be a code change

### Option 3: Option 1 but without a single object param

This option is very similar to option 1 whereby there is a single `sendNotification` method regardless of if the 
template is for a email or sms. The main difference being instead of consumers passing a single object to the library 
it would be broken up into different classes.

```java
class NotificationClient {

  /**
   * Send a single notification without a log correlation ID or sender ID
   */
  NotificationResponse sendNotification(NotificationTemplate notificationTemplate, ContactableRecipient recipient) {
    // ...
  }

  /**
   * Send a single notification with a log correlation ID but no sender ID
   */
  NotificationResponse sendNotification(NotificationTemplate notificationTemplate, 
                                        ContactableRecipient recipient, 
                                        LogCorrelationId logCorrelationId) {
    // ...
  }

  /**
   * Send a single notification with a log correlation ID and sender ID
   */
  NotificationResponse sendNotification(NotificationTemplate notificationTemplate, 
                                        ContactableRecipient recipient,
                                        LogCorrelationId logCorrelationId, 
                                        SenderId senderId) {
    // ...
  }
}
```

In this case the `NotificationTemplate` would contain the mail merge fields

```java
NotificationTemplate notificationTemplate = notificationClient.getTemplateById("template-id")
  .withMergeField("RECIPIENT_NAME", applicantTeamMember.name())
  .withMergeField("CASE_REFERENCE", "ABC/2023/1")
  .merge();
```

The `ContactableRecipient`, `LogCorrelationId` and `SenderId` are the same concepts from option 1.

* Good, because only provides method we actually have use cases for
* Good, because we don't need to provide overloaded methods for optional params for both sms and email methods
* Good, because we could provide a method to send a collection of notifications instead of the consumers needing to loop and send
* Good, because if the consumer wanted to switch the template from email to SMS they don't need to change the code, just the ID
* Good, because we split up the concept of getting the template and building up the mail merge data from the call to send
* Bad, because if we add extra params to methods we would have to deprecate existing methods but keep them for backwards compatibility
* Bad, requires three methods exposed just to handle different client requirements (log correlation ID, sender ID). Could
  mitigate this as clients could just pass `null` but not as nice API.
* Bad, because would consumers be expecting `sendSms` and `sendEmail` methods like gov client provides? (do we care?)
* Bad, because wouldn't easily support letter notification types as not all properties are the same (do we care?)
 
### Option 4: Overloaded methods for different parameter requirements and notification types

Similar to current GOV.UK client whereby there are multiple methods with different parameter options

```java
class NotificationClient {
  
  NotificationResponse sendEmail(String templateId, String recipientEmail, Map<String, Object> mailMergeFields) {
    // ...
  }

  NotificationResponse sendEmail(String templateId, String recipientEmail, Map<String, Object> mailMergeFields, String logCorrelationId) {
    // ...
  }
}
```

* Good, easy for consumers as same as existing GOV.UK client
* Bad, because lots of methods to maintain
* Bad, because easy to make an error and pass in wrong value
* Bad, because any new parameters in future results in yet more method overloads