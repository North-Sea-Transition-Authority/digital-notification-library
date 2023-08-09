# What does the consumer API interaction look like?

* Status: approved
* Deciders: James Barnett, Chris Tasker
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

* Option 1: Methods per notification type with multiple parameters
* Option 2: Single class and methods for all notification types with multiple parameters
* Option 3: Single parameter methods for each notification type

## Decision Outcome

Option 1 is the preferred solution as the API design would both be the most expected by developers and also reads the
clearest when reference in the code. This is very similar to the existing GOV.UK notify library and also follows close
to existing API designs in EPA where the overloaded methods are used.

```java
class NotificationService {

  void emailTeamMembers() {
  
      // option 1 is obvious what is happening
      notificationClient.sendEmail();
          
      // option 2 is less clear as you don't know if it's an email or SMS from just reading the code
      notificationClient.sendNotification();
  }
}
```

The primary downside of lots of overloaded methods if new params are added in the future could be mitigated by
deprecating old methods and removing them in subsequent releases in a similar way in which the spring releases do.

## Pros and Cons of the Options

### Option 1: Methods per notification type with multiple parameters

Similar to the existing GOV.UK notify client we can provide separate `sendEmail` and `sendSms` methods. There will be
various overloaded methods for each notification type. If we need to support a new notification type in the future
additional methods will simply be added.

```java
class NotificationClient {

    NotificationId sendEmail(EmailNotification emailNotification, 
                             EmailRecipient recipient,
                             DomainReference domainReference,
                             LogCorrelationId logCorrelationId) {
        // ...
    }

    NotificationId sendEmail(EmailNotification emailNotification,
                             EmailRecipient recipient,
                             DomainReference domainReference) {
        // ...
    }

    NotificationId sendSms(SmsNotification smsNotification,
                           SmsRecipient recipient,
                           DomainReference domainReference,
                           LogCorrelationId logCorrelationId) {
        // ...
    }

    NotificationId sendSms(SmsNotification smsNotification,
                           SmsRecipient recipient,
                           DomainReference domainReference) {
        // ...
    }
}
```

The following class designs will be the similar for all options but are included here for completeness.

In the above example an `EmailNotification` and `SmsNotification` would be a class representing the GOV.UK notify
template to use and the mail merge properties. As there are currently no difference between sending an email and sms in
terms of the notification we could simply have a `Notification` class if we think that is preferable.

```java
class EmailNotification {

    private String templateId;

    private Map<String, Object> mailMergeProperties = new HashMap<>();
    
    // ...
    
    static class NotificationBuilder(String templateId) {

        private String templateId;

        private Map<String, Object> mailMergeProperties = new HashMap<>();

        NotificationBuilder withMailMergeField(String key, Object value) {
            mailMergeProperties.add(key, value);
            return this;
        }
        
        NotificationBuilder withMailMergeFields(Map<String, Object> mailMergeProperties) {
            mailMergeProperties.addAll(mailMergeProperties);
            return this;
        }
        
        EmailNotification build() {
            return new EmailNotification(templateId, mailMergeProperties);
        }
    }

}
```

The `EmailRecipient` and `SmsRecipient` will be interfaces which can be added to classes representing people who should
receive the notifications. In order to support being able to send notifications to a single email address or phone
number, a static method will exist on the interface so an object is not required in order to send notifications.

```java
interface EmailRecipient {

    String getEmailAddress();

    static EmailRecipient of(String emailAddress) {
        return new EmailRecipient() {
            @Override
            public String getEmailAddress() {
                return emailAddress;
            }
        };
    }
}
```

Assuming the consumers have a `TeamMember` class representing someone who would receive an email, the implementation
would be as follows:

```java
class TeamMember implements EmailRecipient {

    // ...
    
    private String emailAddress;

    @Override
    public String getEmailAddress() {
        return emailAddress;
    }
}
```

An example usage of sending an email to members of a team and a hard coded email address is as follows:

```java
class NotificationService {

  void emailTeamMembers(Team team) {

      EmailNotification notification = constructEmail();
      
      List<TeamMember> teamMembers = getTeamMembers(team);
      
      var domainReference = new DomainReference(team.id(), "TEAM");
  
      // email all team members
      teamMembers().forEach(teamMember -> sendEmail(notification, teamMember, domainReference));
  
      // send email to a fixed email address
      sendEmail(notification, EmailRecipient.of("someone@example.com"), domainReference);
  }
}
```

GOV.UK notify has a `reference` property consumers can set which appears as the `client_reference` when looking on
the dashboards. It is used to [uniquely identify a notification or a batch of notifications](https://docs.notifications.service.gov.uk/java.html#send-an-email-arguments-reference-required).
I am suggesting we use this for our log correlation ID. If you don't provide one then a `client_reference` is not set.
This way the log correlation ID will make its way all the way to the GOV.UK notify logs.

The `DomainReference` will be used to allow consumers to associate a notification with a related domain object , e.g.
a team, application etc. This domain reference will be written to the libraries database table. An example being
if all members of a team were sent an email then the domain reference could be `new DomainReference("123", "TEAM")`.
Developers would be able to look in the database and see for a give domain object the emails that were sent for it.
This data wouldn't go to notify as they only provide a single `client_referene` property which we are already using for
the log correlation ID.

Ignoring the `EmailRecipient`, `LogCorrelationId` and `DomainReference` shared concepts, the pros and cons of this
option are:

* Good, because this is the more expected API from the consumers perspective and similar to methods on the notify client
* Good, because only exposes methods we know we have use cases for
* Good, because methods for one notification type can change independently of each other
* Good, because we can easily extend to support letter types by adding new methods
* Bad, because if we want to add a new param we need to create a new method and possibly deprecate others and look to
  remove them over time

### Option 2: Single class and methods for all notification types with multiple parameters

When comparing the [SMS](https://docs.notifications.service.gov.uk/java.html#send-a-text-message) and
[email](https://docs.notifications.service.gov.uk/java.html#send-an-email) documentation, both notification types take
the same parameters:

- the template ID
- a string representing an email address or a phone number
- a personalisation map of mail merge fields
- a reference identifier for consumers (e.g a log correlation ID)
- an identifier for the sender (UUID representing a reply to email address or name of sender for SMS)

As a result we could create single client API such as:

```java
class NotificationClient {

  NotificationId sendNotification(Notification notification, 
                                  String recipient, 
                                  DomainReference domainReference, 
                                  LogCorrelationId logCorrelationId) {
  // ...
  }

  NotificationId sendNotification(Notification notification, 
                                  String recipient, 
                                  DomainReference domainReference) {
  // ...
  }
}
```

Where the `DomainReference` `LogCorrelationId` concepts are the same as option 1 and the `Notification` is the same as
the `EmailNotification` class.

* Good, because avoids very similar methods for email and sms sending
* Good, because we don't need to provide overloaded methods for optional params for both sms and email methods
* Good, because we could provide a method to send a collection of notifications instead of the consumers needing to loop and send
* Good, because if the consumer wanted to switch the template from email to SMS they don't need to change the code,
  just the template ID. Not sure if this is a benefit as developers would expect to change some code.
* Bad, because would consumers be expecting `sendSms` and `sendEmail` methods like gov client provides?
* Bad, because wouldn't easily support letter notification types as not all properties are the same

### Option 3: Single parameter methods for each notification type

We could combine the recipient, domain reference and log correlation ID into the `EmailNotification` and
`SmsNotification` classes. This would mean we construct a single object with all of the properties for a notifcation.

```java
class NotificationClient {

    NotificationId sendEmail(EmailNotification emailNotification) {
        // ...
    }

    NotificationId sendSms(SmsNotification smsNotification) {
        // ...
    }
}
```

An example of how this might look is below:

```java
class EmailNotification {

  private String templateId;
  
  private Map<String, Object> mailMergeProperties = new HashMap<>();
  
  private EmailRecipient recipient;
  
  private DomainReference domainReference;
  
  private LogCorrelationId logCorrelationId;
  
  // ...
  
  static class NotificationBuilder(String templateId, EmailRecipient recipient, DomainReference domainReference) {
  
      private Map<String, Object> mailMergeProperties = new HashMap<>();
  
      private LogCorrelationId logCorrelationId;
  
      NotificationBuilder withMailMergeField(String key, Object value) {
          mailMergeProperties.add(key, value);
          return this;
      }
  
      NotificationBuilder withMailMergeFields(Map<String, Object> mailMergeProperties) {
          mailMergeProperties.addAll(mailMergeProperties);
          return this;
      }
      
      NotificationBuilder withLogCorrelationId(LogCorrelationId logCorrelationId) {
          this.logCorrelationId = logCorrelationId;
          return this;
      }
  
      EmailNotification build() {
          return new EmailNotification(templateId, mailMergeProperties, recipient, domainReference, logCorrelationId);
      }
  }
}
```

* Good, because if we need to add a new property to either notification we can do without it being a breaking change
  for the consumer or needing an overloaded method
* Bad, because most params are required so not that much of a valid use case for a builder
* Bad, because the API would be harder to read when used in the code as all the important info is hidden in an object