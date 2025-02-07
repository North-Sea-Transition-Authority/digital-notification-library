# [short title of solved problem and solution]

* Deciders: llittle, swarner, dashworth
* Discussed on digital technical: https://fivium.slack.com/archives/C06L8PF8E9K/p1738593458271709

Technical Story: [S29-571](https://fivium.atlassian.net/browse/S29-571)

## Context and Problem Statement
Currently, the Digital Notification Library does not support sending email attachments, but GOVUK Notify does.
Java client documentation - GOV.UK Notify.

In S29 we have 2 options for sending letters - via portal and via a different method. Majority will be sent via the portal and industry users will log in and view them. 
But certain companies won't have access to the portal (e.g. a company based in Iran) and those are the ones that we want to send an email with the file attachment. 
In an ideal world, we would send them all via the portal and not have to worry about email attachments.

Note, we should always be linking downloads via the app unless we have a specific use case not to.

## How do notify attachments work?
The file gets uploaded as a mail merge field. For example
`personalisation.put("link_to_file", client.prepareUpload(fileContents));`
Notify provides the `prepareUpload` method which takes a byte array and adds the file to the personalisation json. For example:
```
{
    "personalisation":{
    "first_name": "Amala",
    "application_date": "2018-01-01",
    "link_to_file": {"file": "file as base64 encoded string"}
}
```
Their rest API then loops through each key in the personalisation object , and any with a value {"file": <content base 64 encoded>} should have the file uploaded and a secure link created in the body of the email.

## How do our mail merge fields currently work?
For context, when a consumer calls the `sendEmail` method from our Notification Library, we save all the email details (including a set of mail merge fields) to the 
notifications table. A scheduled job runs every 10 seconds and if there are any new entries, it retrieves them and sends them to notify.
The consumer defines mail merge fields using a `.withMailMergeField(name, value)` method.

## Considered Options

When a consumer is defining MM fields, they should be able to also define any files they want to upload. E.g.
```
notificationLibraryClient.getTemplate(notifyTemplate.getTemplateId())
        .withMailMergeField("SUBJECT_PREFIX", subjectPrefix)
        .withMailMergeField("SALUTATION", "Dear")
        .withFileAttachment("LINK_TO_FILE", fileUuid)
```

On the notify side, anything defined with the `withFileAttachment` method will be added to a `Set<FileAttachments> fileAttachments` column in the DB (which is a json column 
of mail merge field name as the key and the file id as the value).
Once we have the actual file as a `byte[]`, we can then add it to the personalisation/mail merge fields with the same MM field name.

For example:
In the database the mail merge fields would be:
```
{
    "mail merge fields ":{
        "first_name": "Amala",
        "application_date": "2018-01-01"
    }
}
```

and the file attachments would be:
```
{
    "file attachments":{
        "link_to_file": {"file": "file as base64 encoded string"}
    }
}
```

Then once the `client.prepareUpload` method is called, the email will be sent with the following mail merge fields (personalisation):
```
{
    "personalisation":{
    "first_name": "Amala",
    "application_date": "2018-01-01",
    "link_to_file": {"file": "file as base64 encoded string"}
}
```
Having an extra column on the Notifications table, keeps a separation of concerns and means we don't need to overwrite the value of a mail merge field.

### Option 1
The consumer provides notify with a url to get the file & a bearer token to authenticate the request. In the scheduled method, we can check to see if the notification has
any file attachments. If it does, we make a request to the url that the consumer provided, using the file id from the mail merge fields. Once we have the file we can
pass it into the `client.prepareUpload(fileContents)` method and add it as a mail merge field/personalisation for the template to use.

### Option 2
Pass the file to the library as an input stream. The notification library can then store the file in S3. In the scheduled method, we can check to see if the notification 
has any file attachments. If it does then we can fetch the file from S3 and add it as a mail merge field/personalisation for the template to use.

### Option 3
The consumer should provide an `emailAttachmentResolver` bean which takes in a `fileId` and returns a `byte[]`. A typical implementation will go and get the file from the 
consumers S3 bucket, however, it could also generate the file ad-hoc. The notification library will have the file ids stored in a FileAttachmentJson column on the notification table. 
In the scheduled method, the `emailAttachmentResolver` bean will be invoked using the fileId from the stored mail merge fields. We would then use the 
`client.prepareUpload()` method with the returned `byte[]` to add the file as a mail merge field and send the email to notify.

## Decision Outcome

// TODO need to do pros and cons here
Option 3. That way the consumer deals with the logic to resolve and store files, and it's decoupled from the notify library. It also simplifies things as the notify library doesn't need to have it's own S3 client/bucket, and avoids duplicating files in multiple buckets etc

### How will we handle retries/failing to send attachments?
// TODO need to fill this bit out

### Notify limitations
**Notify has a 2MB file size limit.**

Before sending the email, we can check whether the resolved file is >2Mb. If it is, we can throw a checked exception which the consumer is responsible for handling. 
This means we fail before we try to send the email and notify won't be spammed with retries.

**If users aren't able to access the Energy Portal, how do we know they can access the Notify link to download?**

This is something each consumer needs to consider for its own use case. On S29, we know that it's users from Iran who can't access the Energy Portal. We reached out to 
the Notify team to confirm that there are no restrictions on users in Iran. 

// TODO need to add email response from notify