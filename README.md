# Digital Notification Library [![Build Status](https://drone-github.fivium.co.uk/api/badges/Fivium/digital-notification-library/status.svg?ref=refs/heads/develop)](https://drone-github.fivium.co.uk/Fivium/digital-notification-library)

## Background

The purpose of this library is to extend the functionality provided by [GOV.UK Notify](https://www.notifications.service.gov.uk/)
in sending emails and text messages within services. The key benefits this library provides in addition to the features
in notify are:
- automatic resending of emails or text messages if GOV.UK Notify is down or the recipient cannot receive the notification
  for example if their mailbox is full
- improved observability of notifications that are being sent. By default, GOV.UK Notify only keeps notifications in the 
  logs for 7 days
- an easy way to have notifications sent to test recipients and not the intended recipients

The library currently supports the sending of emails and sms messages only. GOV.UK Notify supports the sending of letters
but this is not supported by the library.

This library is a Spring Boot starter designed to be used within Spring applications.

## Prerequisites

In order to use this library your application must provide:

### Your service is registered on GOV.UK notify

Your service needs to be registered on [GOV.UK Notify](https://www.notifications.service.gov.uk/) before integrating 
with the library. Your customer will need to register the service on our behalf as only users from a government department
(e.g. with a gov.uk domain) or from an approved regulator can create new Notify accounts. They will need to add someone 
from Fivium to the account once it has been registered.

### A ShedLock LockProvider bean

The library requires [ShedLock](https://github.com/lukas-krecan/ShedLock) to handle concurrent locking for scheduled jobs. 
Your application must provide a [LockProvider](https://github.com/lukas-krecan/ShedLock?tab=readme-ov-file#configure-lockprovider) 
bean to allow this locking. For example:

```java
@Bean
public LockProvider lockProvider(JdbcTemplate jdbcTemplate) {
  return new JdbcTemplateLockProvider(builder()
    .withTableName("<your database schema>.shedlock")
    .withJdbcTemplate(jdbcTemplate)
    .usingDbTime()
    .build()
  );
}
```

You will also need to ensure your application has scheduling enabled using the `@EnableScheduling` annotation.

The library does not require any specific `LockProvider` configuration options. If your application uses scheduled tasks 
and ShedLock itself, then you should already have this bean and can skip this step.

### Hibernate Envers auditing

The library requires [Hibernate Envers](https://hibernate.org/orm/envers/) in order to automatically audit the 
changes to the entities used with the library. If your application already uses Hibernate Envers you can skip this step.

You need to have the following dependency:

```groovy
implementation 'org.springframework.data:spring-data-envers'
```

As well as an audit revision entity (`@RevisionEntity`) with a table called `audit_revisions`.

## Using the library

The library can be run in two modes, `test` and `production`.
- `test` mode allows you to have notifications send to a set of test recipients and not the actual recipient of the notification
- `production` mode will send the notification to the intended recipient

Depending on the mode you choose to run in you will need to set different environment variables.

Regardless of the mode you are running in you will need to provide the API key from your applications GOV.UK Notify account.

There are different types of API keys available in GOV.UK Notify:
- Test: pretends to send messages and will not use your daily quota of notifications. You can use this on development
  environments. Notifications will not be sent to actual inboxes but are viewable on the API integration section of
  the GOV.UK Notify dashboard.
- Team and guest list: limits who can receive notifications to only those in your applications GOV.UK Notify team. This 
  type of key can be used on environments where you want users to receive notifications for testing purposes, for example 
  QA and Pre-prod environments. Notifications using this key will use your daily notification limits.
- Live: sends notifications to anyone and is only available when your service is not trial mode.

GOV.UK Notify have information about their [daily limits](https://docs.notifications.service.gov.uk/java.html#limits).

### For test mode

Then running in `test` mode the library requires you to provide both test email and sms recipients. These can be single
email addresses or sms numbers or a csv list if you have more than one recipient. Test mode requires a sms recipient 
regardless of if your service uses sms messaging. If you don't use sms messaging this property can be set to a random 
number.

```groovy
digital-notification-library.mode=test
digital-notification-library.govuk-notify.api-key=<your GOV.UK Notify API key>
digital-notification-library.test-mode.email-recipients=someone@example.com,someone.else@example.com
digital-notification-library.test-mode.sms-recipients=07112357444
```

In the above configuration, if an email was being sent to `user@shell.co.uk` from within you application, the email will
instead be sent to `someone@example.com` and `someone.else@example.com`.

In the above configuration, if a sms was being sent to `07812357326` from within you application, the sms will
instead be sent to `07112357444`.

### For production mode

```groovy
digital-notification-library.mode=production
digital-notification-library.govuk-notify.api-key=<your GOV.UK Notify API key>
```

If you provide `digital-notification-library.test-mode.email-recipients` or `digital-notification-library.test-mode.sms-recipients`
properties when running in `production` mode they will be ignored and the notifications will be sent to the intended recipients. 

### Getting a template

You can inject a `NotificationLibraryClient` bean into a bean within your application. This allows you to access the
public client api methods to interact with the library.

You can get a template to use for a notification from GOV.UK Notify by using the following method: 

```java
Template template = notificationLibraryClient.getTemplate("<the template ID from GOV.UK notify>");
```
The template class contains
- `notifyTemplateId`: The ID of the template on GOV.UK notify 
- `type` – The type of the template 
- `requiredMailMergeFields` – The mail merge fields that are required to be set for the template 
- `verificationStatus` – Status indicating if the template is a confirmed notify template or not. If Notify is down 
  you wil get a status of `UNCONFIRMED_NOTIFY_TEMPLATE`. If Notify responds you get a status of `CONFIRMED_NOTIFY_TEMPLATE`

### Sending an email or sms

#### Domain reference

Emails require a `DomainReference` object to be provided. The aim of this object is to make it easy to identify what 
domain concept this email is for within your service. For example if you are sending an email relating to a version of
an application with an `ApplicationVersion` object within your service. You could create a `DomainReference`
in the following way:

```java
DomainReference domainReference = DomainReference.of(applicationVersion.id(), "APPLICATION_VERSION");
```

The domain reference is not used by anything within the library it is just used so you can easily query the table for 
notifications that were sent for a given domain concept.

#### Mail merge fields

Once you have a template you can call the `.withMailMergeField()` method to add mail merge fields to the template. You
can call this method as many times as you need to add mail merge fields. You can call a `withMailMergeFields()` variant
which takes a collection of mail merge fields if you preferred.

```java
MergedTemplate mergedTemplate = notificationLibraryClient.getTemplate("notify template ID")
    .withMailMergeField("my_mail_merge_field_name", "my_mail_merge_field_value")
    .merge();
```

#### Recipient

The recipient of the notification can be configured in two ways. 

If you have a string email address or phone number that isn't associated with a user object you can create a recipient 
by doing `EmailRecipient.directEmailAddress("someone@example.com")` for an email address or `SmsRecipient.directPhoneNumber("07642347589")`
when sending a sms. This is ideal for scenarios such as having business/regulator email that come from property file.

If you have user objects in your service you can implement the `EmailRecipient` or `SmsRecipient` interfaces depending 
the notifications you want to send.

```java
public record User(String id, ..., String emailAddress) implements EmailRecipient {
  @Override
  public String getEmailAddress() {
    return emailAddress;
  }
}
```

This will allow you to pass an entire `User` object to the `sendEmail()` or `sendSms()` method as per the below example.

```java
void sendUsersEmail() {
  
  MergedTemplateBuilder mergedTemplateBuilder = notificationLibraryClient.getTemplate("notify template ID")
    .withMailMergeField("my_mail_merge_field_name", "my_mail_merge_field_value");
  
  Set<User> users = myServiceMethod.getUsersToEmail();
  
  DomainReference domainReference = DomainReference.of("<id of a domain object>", "<type of domain object>");
  
  users.forEach(user -> {

    MergedTemplate mergedTemplate = mergedTemplateBuilder
      .withMailMergeField("name", "user.name()")
      .merge();
    
    notificationLibraryClient.sendEmail(
      mergedTemplate, 
      user, 
      domainReference,
      "log-correlation-id"
    );
  });

  MergedTemplate mergedTemplate = mergedTemplateBuilder
    .withMailMergeField("name", "Regulator")
    .merge();

  notificationLibraryClient.sendEmail(
    mergedTemplate,
    EmailRecipient.directEmailAddress("email@regulator.co.uk"),
    domainReference,
    "log-correlation-id"
  );
}
```

### What if a notification fails to send?

A notification can fail to send for a number of reasons. If the failure reason is due to a permanent error such as email
address or phone number doesn't exist or not enough mail merge fields have been provided for the template then the 
notification will not be retried.

If the failure reason is due to a temporary error such as the mailbox being full or GOV.UK Notify being unavailable then 
the library will automatically attempt to retry sending the notification up until 72 hours after the notification was 
requested to be sent originally. The idea being that after 72 hours the notification is likely to be no longer relevant 
to the user.

By default, the retry times will be as follows:
- first retry after 10 seconds of last send attempt
- second retry after 20 seconds of last send attempt
- third retry after 40 seconds of last send attempt
- fourth retry after 80 seconds of last send attempt
- fifth retry after 160 seconds of last send attempt
- after 72 hours the notification will no longer attempt to send if it hasn't been sent since

### Can I change how often notifications are sent or updated or how many are processed?

By default, a scheduled job runs every 10 seconds within the library which will send any notifications to notify which
have not yet been sent and also update the statuses of any notifications to see if they have been sent or not. Each 
iteration of the job will process 100 notifications at a time.

If you want to change any of the default settings, you can set either of the properties below. Setting the below will
result in the scheduled job running every 30 seconds and will do 500 notifications at a time.

```groovy
digital-notification-library.notification.poll-time-seconds=30
digital-notification-library.notification.bulk-retrieval-limit=500
```

### How can I see notifications being sent within the library?

### What are the statuses of notifications that are used within the library?