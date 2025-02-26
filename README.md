# Digital Notification Library [![Build Status](https://drone-github.fivium.co.uk/api/badges/Fivium/digital-notification-library/status.svg?ref=refs/heads/develop)](https://drone-github.fivium.co.uk/Fivium/digital-notification-library)

## Background

The purpose of this library is to extend the functionality provided by [GOV.UK Notify](https://www.notifications.service.gov.uk/)
in sending emails and text messages within applications. The key benefits this library provides in addition to the 
features provided by GOV.UK Notify are:
- automatic resending of emails or text messages if GOV.UK Notify is down or the recipient cannot receive the notification
  (for example if their mailbox is full)
- improved observability of notifications that are being sent. By default, GOV.UK Notify only keeps notifications in the 
  logs for 7 days
- an easy way to have notifications sent to test recipients and not the intended recipients for testing purposes

The library currently supports the sending of emails and sms messages only. GOV.UK Notify supports the sending of letters
but this is not supported by the library due to cost implications for customers.

This library is a Spring Boot starter designed to be used within Spring applications.

## Prerequisites

In order to use this library your application must:

### Have an account on GOV.UK notify

Your application needs to be registered on [GOV.UK Notify](https://www.notifications.service.gov.uk/) before integrating 
with the library. Your customer will need to register the application on our behalf as only users from a government 
department (e.g. with a gov.uk domain) or from an approved regulator can create new Notify accounts. They will need 
to add someone from Fivium to the account once it has been registered.

### Be on at least version 3.0.0 of Spring Boot

The library persists notifications to your applications database. This uses the features from the `jakarta.persistence`
package. As a result, your application needs to be running at least version `3.0.0` of Spring Boot.

### Be managing its database migrations using Flyway

The library will create tables within your applications schema using [Flyway](https://flywaydb.org/). As a result, your
application will need to have configured to allow migrations via Flyway. 

### Provide a ShedLock LockProvider bean

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

### Provide Hibernate Envers auditing

The library requires [Hibernate Envers](https://hibernate.org/orm/envers/) in order to automatically audit the 
changes to the entities used with the library. If your application already uses Hibernate Envers you can skip this step.

You need to have the following dependency:

```groovy
implementation 'org.springframework.data:spring-data-envers'
```

As well as an audit revision entity (`@RevisionEntity`) with a table called `audit_revisions`.

## Using the library

### Add the dependency

Check the [releases](https://github.com/Fivium/digital-notification-library/releases) page for the latest tagged release
of the library. Once you have found the version you want to use add the following to your gradle file.

```groovy
implementation 'uk.co.fivium:digital-notification-library-spring-boot-starter:<version>'
```

If you don't already have the Fivium nexus repository added to your `repositories` list then this should be added as per
the below:

```groovy
repositories {
    // ... any other repos
    maven { url "https://nexus.fivium.co.uk/repository/maven-releases/" }
}
```

### Add required properties

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

GOV.UK Notify have more information about their [API keys](https://docs.notifications.service.gov.uk/java.html#api-keys)
and [daily limits](https://docs.notifications.service.gov.uk/java.html#limits).

#### For test mode

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

#### For production mode

```groovy
digital-notification-library.mode=production
digital-notification-library.govuk-notify.api-key=<your GOV.UK Notify API key>
```

If you provide `digital-notification-library.test-mode.email-recipients` or `digital-notification-library.test-mode.sms-recipients`
properties when running in `production` mode they will be ignored and the notifications will be sent to the intended recipients. 

### Getting a template

You can inject a `NotificationLibraryClient` bean into a bean within your application. This allows you to access the
public client api methods to interact with the library.

You can get a template to use for a notification from GOV.UK Notify by using the following method. The template must
exist on GOV.UK Notify. If a template doesn't exist you will get an exception. 

```java
Template template = notificationLibraryClient.getTemplate("<the template ID from GOV.UK notify>");
```

The template class contains
- `notifyTemplateId`: The ID of the template on GOV.UK notify 
- `type` – The type of the template, e.g. `email` or `sms`
- `requiredMailMergeFields` – The mail merge fields that are required to be set in order to use the template 
- `verificationStatus` – Status indicating if the template is a confirmed notify template or not. If Notify is down 
  you wil get a status of `UNCONFIRMED_NOTIFY_TEMPLATE`. If Notify responds you get a status of `CONFIRMED_NOTIFY_TEMPLATE`.

### Sending an email or sms

#### Domain reference

Notifications require a `DomainReference` object to be provided. The aim of this object is to make it easy to identify what 
domain concept a notification is for within your application. For example if you are sending a notification relating to 
a version of an application and that was modelled in your application via a `ApplicationVersion` object, you could 
create a `DomainReference` in the following way:

```java
class ApplicationVersion implements DomainReference {
  
  @Override
  String getId() {
    return application.getId();
  }
  
  @Override
  String getType() {
    return "APPLICATION_VERSION";
  }
  
}
```

Alternatively, if you don't have an object you can create a `DomainReference` object manually by doing:

```java
DomainReference domainReference = DomainReference.of(applicationVersion.id(), "APPLICATION_VERSION");
```

The domain reference is not used by anything within the library it is just used to enable easy querying of the table for 
notifications that were sent for a given domain concept. For example, using the example above if you wanted to check what 
notifications were sent for an application version, you can query the database table filtering on the domain ID being the 
application version ID and the domain type being `APPLICATION_VERSION`.

#### Mail merge fields

Once you have a template you can call the `.withMailMergeField()` method to add mail merge fields to the template. You
can call this method as many times as you need to add mail merge fields.

```java
MergedTemplate mergedTemplate = notificationLibraryClient.getTemplate("notify template ID")
    .withMailMergeField("recipient_name", "Claire")
    .withMailMergeField("application_reference", "DA/2024/123")
    .merge();
```

#### Recipient

The recipient of the notification can be configured in two ways. 

If you have a string email address or phone number that isn't associated with a user object you can create a recipient 
by doing 
- `EmailRecipient.directEmailAddress("someone@example.com")` for an email address or 
- `SmsRecipient.directPhoneNumber("07642347589")` for a sms

This is ideal for scenarios such as having business/regulator email that come from property file as opposed sending
notifications to users of your application.

The more common scenario is when you want to send notifications to users of your application. If you have modelled your
user objects you can make them implement the `EmailRecipient` or `SmsRecipient` interfaces depending on the notifications 
you want to send.

```java
public record User(String id, String name, String emailAddress) implements EmailRecipient {
  
  @Override
  public String getEmailAddress() {
    return emailAddress;
  }
}
```

This will allow you to pass an entire `User` object to the `sendEmail()` or `sendSms()` method as per the below example.

```java
class ApplicationService {
  
  void sendUsersEmail() {

    // don't call .merge() so we can add the users name to the template in a loop
    MergedTemplateBuilder mergedTemplateBuilder = notificationLibraryClient.getTemplate("notify template ID")
        .withMailMergeField("application_reference", "DA/2024/123");

    Set<User> users = myServiceMethod.getUsersToEmail();

    DomainReference domainReference = DomainReference.of("<id of a domain object>", "<type of domain object>");

    users.forEach(user -> {

      MergedTemplate mergedTemplate = mergedTemplateBuilder
          .withMailMergeField("name", user.name())
          .merge();

      notificationLibraryClient.sendEmail(
          mergedTemplate,
          user,
          domainReference,
          "log-correlation-id"
      );
    });

    // Additionally send the regulator the email
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
}
```

#### File attachments 

##### How to attach files to an email notification
It is possible to include file attachments to an email notification. In the consumer, there will need to be a `NotificationLibraryEmailAttachmentResolver` bean
which takes in a `fileId` and returns the file contents as a `byte[]`. On the consumer side, this will most likely involving retrieving the file 
from the consumer's S3 bucket. The library will store the file ids as a JSON list on the `notification_library_notifications` table. Files will be
passed to the email template in the similar way to the mail merge fields

```java
MergedTemplate mergedTemplate = notificationLibraryClient.getTemplate("notify template ID")
    .withMailMergeField("recipient_name", "Claire")
    .withMailMergeField("application_reference", "DA/2024/123")
    .withFileAttachment("file_link_mm", daa0433a-9361-4b95-9e72-e9867e9a6ee0, "File.pdf")    
    .merge();
```

The Notify account settings for the consumer will also need to be updated to allow file attachments. There will need to be contact details provided,
which will be used for the file download page (for the users to contact if there is an issue with the download). You can either provide a link to a website, 
email address or phone number. 

The recipient will be emailed with a link to download their file from GOVUK Notify. When they access this link, they will need to verify themselves by entering 
their email address. After verification, they will be taken to a page to download the file, this file will only be retained by GOVUK Notify for 6 months by default.

##### Notify file attachment restrictions 
There is a file size limit of 2MB and file name length limit of 100 characters. There should be validation in place on the consumer to ensure that only valid 
files are passed through to the library to be attached to emails. As the consumer, you can call the `isFileAttachable` method which will carry out checks on the 
file that you would like to attach.
To see the full list of GOVUK Notify sending file error codes, see [here](https://docs.notifications.service.gov.uk/java.html#send-a-file-by-email-error-codes)
If the file does breach any of the restrictions from GOVUK Notify, there will be an error thrown that the consumer will need to handle. 


### What if a notification fails to send?

A notification can fail to send for a number of reasons. If the failure reason is due to a permanent error such as email
address or phone number doesn't exist or not enough mail merge fields have been provided for the template then the 
notification will not be retried as the same error would occur each time.

If the failure reason is due to a temporary error such as the mailbox being full or GOV.UK Notify being unavailable then 
the library will automatically attempt to retry sending the notification up until 72 hours after the notification was 
requested to be sent originally. The idea being that after 72 hours the notification is likely to be no longer relevant 
to the user. The retry attempts use an exponential backoff strategy defaulting to:
- first retry after 10 seconds of last send attempt
- second retry after 20 seconds of last send attempt
- third retry after 40 seconds of last send attempt
- fourth retry after 80 seconds of last send attempt
- fifth retry after 160 seconds of last send attempt
- after 72 hours the notification will no longer attempt to send if it hasn't been sent since

__Note__: It could be the case that if 10 seconds after the last send attempt is 09:00:01 and the job runs at 09:00:00 then
this iteration of the job will not pick up the notification as the time hasn't elapsed yet. It has been accepted that this
is not a high priority problem and can be solved by documentation. If we are going address this problem their is a [proposed
solution](https://github.com/Fivium/digital-notification-library/pull/25#discussion_r1477895444) discussed.

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

**Note**: The above change will also mean the first retry of a notification will not be until 30 seconds after the first
send attempt, then 60 seconds, 120 seconds etc.

### How can I see notifications being sent within the library?

The library will create a table `notification_library_notifications` within your applications' schema. This table stores
all the information about a notification including the mail merge fields, sent, failure and update times as well as 
the IDs of template and notifications from GOV.UK Notify. You cannot interact with this table directly from your consuming
application.

When a request is made to send an email or sms a row will be added to this table with a status of `QUEUED`. Every n seconds
where n is the value of `digital-notification-library.notification.poll-time-seconds` (10 by default) the notification will
attempt to send to GOV.UK Notify for processing.

### What are the statuses of notifications that are used within the library?

Notifications within the library can have the following statuses. These will only be visible in the database table.
- `QUEUED`: indicates a notification has not yet been sent to GOV.UK Notify
- `SENT_TO_NOTIFY`: indicates a notification has been sent to GOV.UK Notify. At this stage the recipient may have already 
  received it. The notification status will be automatically updated the next time the notifications are processed.
- `SENT`: indicates the notification has been delivered to the recipient
- `RETRY`: indicates the notification has previously failed to sent via GOV.UK Notify and another send attempt will be made.
- `FAILED_TO_SEND_TO_NOTIFY`: indicates the notification could not be sent to GOV.UK Notify and the GOV.UK Notify dashboard
  will have no record of it. This is likely if GOV.UK Notify was down when the request was made.
- `FAILED_NOT_SENT`: indicates the notification has not been sent and will not be retried.
- `UNEXPECTED_NOTIFY_STATUS`: indicates the library received a status from GOV.UK Notify that it didn't know how to handle.
  The recipient may have received the notification if we hit this status.

### Ensure your application can contact GOV.UK notify

Ensure that the environments where your application is deployed can make REST request to GOV.UK Notify by ensuring 
GOV.UK Notify is on your environments whitelist and any proxy is configured where needed. Request to GOV.UK Notify 
from within the library are made via request to `https://api.notifications.service.gov.uk`.

## Contributing 

### Running integration tests

The integration tests within the library sends and asserts notifications to GOV.UK Notify. As a result you need to set
the `INTEGRATION_TEST_GOVUK_NOTIFY_API_KEY` environment variable as part of your tests run configuration. This should be 
set to [this API key](https://tpm.fivium.co.uk/index.php/pwd/view/2246), which uses the 
[Fivium account](https://www.notifications.service.gov.uk/services/95f4e6d8-0261-40d2-89cc-b79049346011) on GOV.UK Notify. 
Speak with an SME of this library to be provided with access if you require it for debugging or template creation purposes.

### Snapshot publications

Any push to drone will cause a snapshot artifact to be published on any branch. These can be used in your application for 
getting latest, non-published changes. For example, if you are adding new functionality to the library, you can make a 
branch and push, then you'll get an artifact published including your branch name once the build has passed. In order
to be able to reference the snapshots you will need to include the Fivium nexus snapshots maven repository.

```groovy
repositories {
    // ... any other repos
    maven { url "https://nexus.fivium.co.uk/repository/maven-snapshots/" }
}
```

### Creating a release

Release artifacts should be considered production ready, and can only be made on the `main` branch (or bug-fix branches).

Before the release ensure a merge commit has been created from the develop branch to the main branch. Feature branches 
can be squashed when merging to `develop`.

To create a release, you need to tag a commit on the `main` branch. This is done by creating a release in GitHub, 
and choosing a commit on the `main` branch. This will produce an artifact that can be imported into your application.

The documentation can also be auto-generated based on the previous merge commit into `main`. It should auto-detect the 
previous release to work out the release notes.

The tag-name is used for the version of the client. You should tag the release following standard version standards: 
major:minor:bug-fix. The basic rules are:
- If you're not changing any part of the public api: just increment bug-fix
- If you're making a change the api, but it's non-breaking: increment minor and reset bug-fix to 0
- If you're making a breaking change to the api: increment major and reset bug-fix and minor to 0 

If you need to fix an urgent bug on an older release, and there's been other minor/major changes since that commit which 
cannot be incorporated into applications, then you can create a bugfix-{release} branch from the commit you need to fix. 
You can then tag that commit with an incremented bug-fix version without forcing the app to upgrade. You should consider 
merging that release back into master, or just leaving the branch if it's already fixed in a later release.
