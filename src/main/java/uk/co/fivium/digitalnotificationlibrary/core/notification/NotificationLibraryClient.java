package uk.co.fivium.digitalnotificationlibrary.core.notification;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationMode;
import uk.co.fivium.digitalnotificationlibrary.core.DigitalNotificationLibraryException;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailNotification;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.fivium.digitalnotificationlibrary.core.notification.sms.SmsNotification;
import uk.co.fivium.digitalnotificationlibrary.core.notification.sms.SmsRecipient;

/**
 * The main service consumers will use to interact with the library.
 */
@Service
public class NotificationLibraryClient {

  private final NotificationLibraryNotificationRepository notificationRepository;

  private final TemplateService templateService;

  private final Clock clock;

  private final NotificationLibraryConfigurationProperties libraryConfigurationProperties;

  private final EmailAttachmentResolver emailAttachmentResolver;

  /**
   * Create an instance of NotificationLibraryClient.
   *
   * @param notificationRepository         The notification repository
   * @param templateService                The service for retrieving templates
   * @param clock                          The clock instance
   * @param libraryConfigurationProperties The configuration properties for the library
   */
  @Autowired
  public NotificationLibraryClient(NotificationLibraryNotificationRepository notificationRepository,
                                   TemplateService templateService,
                                   Clock clock,
                                   NotificationLibraryConfigurationProperties libraryConfigurationProperties,
                                   EmailAttachmentResolver emailAttachmentResolver) {
    this.notificationRepository = notificationRepository;
    this.templateService = templateService;
    this.clock = clock;
    this.libraryConfigurationProperties = libraryConfigurationProperties;
    this.emailAttachmentResolver = emailAttachmentResolver;
  }

  /**
   * <p>Get the template associated to the provided template ID. If Notify is down a template will still be
   * returned but the type, mail merge fields will not be populated. Method will throw an exception if GOV.UK Notify
   * returns a 403 or 404 response.</p>
   *
   * <p>With throwing an exception for a 404 response from GOV.UK Notify there could be a risk that if GOV.UK remove the
   * Notify service then all our request will return a 404 (has happened historically). The thinking here is that a 404
   * response is more likely to be that developers have provided the wrong template ID in their configuration. We want
   * to know about this immediately via an exception, so it can be resolved before the change is deployed out to
   * environments. If GOV.UK Notify ever returns a 404 for their API not being around then all calls to this method
   * will throw and exception and that is an accepted risk.</p>
   *
   * @param notifyTemplateId The ID of the notify template to return
   * @return A template known to notify or an unconfirmed template if notify is down.
   */
  public Template getTemplate(String notifyTemplateId) {

    var templateResponse = templateService.getTemplate(notifyTemplateId);

    if (templateResponse.isSuccessfulResponse()) {
      return Template.fromNotifyTemplate(templateResponse.successResponseObject());
    } else if (isErrorStatusRelatedToConsumer(templateResponse.error())) {
      throw new DigitalNotificationLibraryException(
          "Failed with %s response from GOV.UK Notify when getting template with ID %s. GOV.UK Notify error: %s"
              .formatted(templateResponse.error().httpStatus(), notifyTemplateId, templateResponse.error().message())
      );
    } else {
      return Template.createUnconfirmedTemplate(notifyTemplateId);
    }
  }

  /**
   * Queue an email notification to be sent.
   *
   * @param mergedTemplate   The template with mail merge fields to send
   * @param recipient        The recipient of the notification
   * @param domainReference  A reference to the consumers domain concept the notification is for
   * @param logCorrelationId An identifier for log correlation
   * @return A representation of the notification that has been queued to send
   */
  @Transactional
  public EmailNotification sendEmail(MergedTemplate mergedTemplate,
                                     EmailRecipient recipient,
                                     DomainReference domainReference,
                                     String logCorrelationId) {
    checkEmailConfigIsValid(mergedTemplate, recipient, domainReference, logCorrelationId);
    return sendEmail(mergedTemplate, Set.of(), recipient, domainReference, logCorrelationId);
  }

  /**
   * Queue an email notification to be sent.
   *
   * @param mergedTemplate  The template with mail merge fields to send
   * @param recipient       The recipient of the notification
   * @param domainReference A reference to the consumers domain concept the notification is for
   * @return A representation of the notification that has been queued to send
   */
  @Transactional
  public EmailNotification sendEmail(MergedTemplate mergedTemplate,
                                     EmailRecipient recipient,
                                     DomainReference domainReference) {
    checkEmailConfigIsValid(mergedTemplate, recipient, domainReference, null);
    return sendEmail(mergedTemplate, recipient, domainReference, null);
  }

  /**
   * Queue an email notification with files to be sent.
   *
   * @param mergedTemplate   The template with mail merge fields and file attachments to send
   * @param recipient        The recipient of the notification
   * @param domainReference  A reference to the consumers domain concept the notification is for
   * @param logCorrelationId An identifier for log correlation
   * @return A representation of the notification that has been queued to send
   */
  @Transactional
  public EmailNotification sendEmail(MergedTemplateWithFiles mergedTemplate,
                                     EmailRecipient recipient,
                                     DomainReference domainReference,
                                     String logCorrelationId) throws NotificationFileException {
    checkEmailConfigIsValid(mergedTemplate, recipient, domainReference, logCorrelationId);

    if (CollectionUtils.isEmpty(mergedTemplate.getFileAttachments())) {
      throw new NotificationFileException("File attachments not provided for email notification");
    }

    EmailNotification emailNotification = null;
    for (FileAttachment fileAttachment : mergedTemplate.getFileAttachments()) {
      var resolvedFile = emailAttachmentResolver.resolveFileAttachment(fileAttachment.fileId());
      emailNotification = switch (isFileAttachable(resolvedFile.length, fileAttachment.fileName())) {
        case FILE_TOO_LARGE -> throw new NotificationFileException("File attachment cannot be bigger than 2MB");
        case INVALID_FILE_NAME -> throw new NotificationFileException("File name must have 100 characters or less.");
        case INCORRECT_FILE_EXTENSION ->
            throw new NotificationFileException("File name must include a valid file extension");
        case SUCCESS -> sendEmail(mergedTemplate, mergedTemplate.getFileAttachments(), recipient, domainReference,
            logCorrelationId);
      };
    }

    return emailNotification;
  }

  /**
   * Queue an email notification to be sent.
   *
   * @param mergedTemplate  The template with mail merge fields to send
   * @param recipient       The recipient of the notification
   * @param domainReference A reference to the consumers domain concept the notification is for
   * @return A representation of the notification that has been queued to send
   */
  @Transactional
  public EmailNotification sendEmail(MergedTemplateWithFiles mergedTemplate,
                                     EmailRecipient recipient,
                                     DomainReference domainReference) throws NotificationFileException {
    checkEmailConfigIsValid(mergedTemplate, recipient, domainReference, null);
    return sendEmail(mergedTemplate, recipient, domainReference, null);
  }

  /**
   * Queue an sms notification to be sent.
   *
   * @param mergedTemplate   The template with mail merge fields to send
   * @param recipient        The recipient of the notification
   * @param domainReference  A reference to the consumers domain concept the notification is for
   * @param logCorrelationId An identifier for log correlation
   * @return A representation of the notification that has been queued to send
   */
  @Transactional
  public SmsNotification sendSms(MergedTemplate mergedTemplate,
                                 SmsRecipient recipient,
                                 DomainReference domainReference,
                                 String logCorrelationId) {

    if (mergedTemplate == null) {
      throw new DigitalNotificationLibraryException("MergedTemplate must not be null");
    }

    if (!isSmsTemplateType(mergedTemplate)) {
      throw new DigitalNotificationLibraryException(
          "Cannot send an sms for template with ID %s and type %s"
              .formatted(mergedTemplate.getTemplate().notifyTemplateId(), mergedTemplate.getTemplate().type())
      );
    }

    if (recipient == null || StringUtils.isBlank(recipient.getSmsRecipient())) {
      throw new DigitalNotificationLibraryException(
          "SmsRecipient must not be null or empty for notification with correlation ID %s".formatted(logCorrelationId)
      );
    }

    if (domainReference == null) {
      throw new DigitalNotificationLibraryException(
          "DomainReference must not be null for notification with correlation ID %s".formatted(logCorrelationId)
      );
    }

    var notification = queueNotification(
        NotificationType.SMS,
        recipient.getSmsRecipient(),
        domainReference,
        logCorrelationId,
        mergedTemplate.getMailMergeFields(),
        Set.of(),
        mergedTemplate.getTemplate()
    );

    return new SmsNotification(String.valueOf(notification.getId()));
  }

  /**
   * Queue an sms notification to be sent.
   *
   * @param mergedTemplate  The template with mail merge fields to send
   * @param recipient       The recipient of the notification
   * @param domainReference A reference to the consumers domain concept the notification is for
   * @return A representation of the notification that has been queued to send
   */
  @Transactional
  public SmsNotification sendSms(MergedTemplate mergedTemplate,
                                 SmsRecipient recipient,
                                 DomainReference domainReference) {
    return sendSms(mergedTemplate, recipient, domainReference, null);
  }

  /**
   * Determines if the library is running in test mode.
   *
   * @return returns true if running in test mode, false otherwise
   */
  public boolean isRunningTestMode() {
    return NotificationMode.TEST.equals(libraryConfigurationProperties.mode());
  }

  /**
   * Determines if the library is running in production mode.
   *
   * @return returns true if running in production mode, false otherwise
   */
  public boolean isRunningProductionMode() {
    return NotificationMode.PRODUCTION.equals(libraryConfigurationProperties.mode());
  }

  /**
   * Determines if the file can be sent to notify as a file attachment mail merge field.
   * The conditions are as follows:
   * File size must be less than 2MB,
   * File name must be less than 100 characters and have a file extension,
   * File extension must match one of the valid file extensions defined as a configuration property.
   *
   * @param contentLength The length of the file content that will be attached
   * @param filename      The name of the document.
   * @return returns an AttachableFileResult, which informs the consumer whether the file can be sent via notify.
   *         The consumer can then decide how they handle invalid files
   */
  public AttachableFileResult isFileAttachable(long contentLength, String filename) {
    var validFileExtensions = FileAttachmentUtils.getValidFileExtensions();
    int dotIndex = filename.lastIndexOf(".");

    if (contentLength > 2 * 1024 * 1024) {
      return AttachableFileResult.FILE_TOO_LARGE;
    } else if (filename.toCharArray().length > 100) {
      return AttachableFileResult.INVALID_FILE_NAME;
    } else if (dotIndex < 0 || !validFileExtensions.contains(filename.substring(dotIndex))) {
      return AttachableFileResult.INCORRECT_FILE_EXTENSION;
    }
    return AttachableFileResult.SUCCESS;
  }

  private Notification queueNotification(NotificationType notificationType,
                                         String recipient,
                                         DomainReference domainReference,
                                         String logCorrelationId,
                                         Set<MailMergeField> mailMergeFields,
                                         Set<FileAttachment> fileAttachments,
                                         Template template) {

    var notification = new Notification();
    notification.setStatus(NotificationStatus.QUEUED);
    notification.setType(notificationType);
    notification.setRecipient(recipient);
    notification.setDomainReferenceId(domainReference.getDomainId());
    notification.setDomainReferenceType(domainReference.getDomainType());
    notification.setMailMergeFields(mailMergeFields);
    notification.setNotifyTemplateId(template.notifyTemplateId());
    notification.setRequestedOn(clock.instant());
    notification.setRetryCount(0);
    notification.setFileAttachments(fileAttachments);

    if (StringUtils.isNotBlank(logCorrelationId)) {
      notification.setLogCorrelationId(logCorrelationId);
    }

    notificationRepository.save(notification);
    return notification;
  }

  private boolean isErrorStatusRelatedToConsumer(Response.ErrorResponse response) {
    return switch (response.httpStatus()) {
      case HttpStatus.SC_FORBIDDEN, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_BAD_REQUEST -> true;
      default -> false;
    };
  }

  private boolean isEmailTemplateType(MergedTemplate template) {
    return Set.of(TemplateType.EMAIL, TemplateType.UNKNOWN).contains(template.getTemplate().type());
  }

  private boolean isSmsTemplateType(MergedTemplate template) {
    return Set.of(TemplateType.SMS, TemplateType.UNKNOWN).contains(template.getTemplate().type());
  }

  private void checkEmailConfigIsValid(MergedTemplate mergedTemplate, EmailRecipient recipient,
                                       DomainReference domainReference, String logCorrelationId) {
    if (mergedTemplate == null) {
      throw new DigitalNotificationLibraryException("MergedTemplate must not be null");
    }

    if (!isEmailTemplateType(mergedTemplate)) {
      throw new DigitalNotificationLibraryException(
          "Cannot send an email for template with ID %s and type %s"
              .formatted(mergedTemplate.getTemplate().notifyTemplateId(), mergedTemplate.getTemplate().type())
      );
    }

    if (recipient == null || StringUtils.isBlank(recipient.getEmailAddress())) {
      throw new DigitalNotificationLibraryException(
          "EmailRecipient must not be null or empty for notification with correlation ID %s".formatted(logCorrelationId)
      );
    }

    if (domainReference == null) {
      throw new DigitalNotificationLibraryException(
          "DomainReference must not be null for notification with correlation ID %s".formatted(logCorrelationId)
      );
    }
  }

  private EmailNotification sendEmail(MergedTemplate mergedTemplate, Set<FileAttachment> fileAttachments,
                                      EmailRecipient recipient, DomainReference domainReference,
                                      String logCorrelationId) {

    var notification = queueNotification(
        NotificationType.EMAIL,
        recipient.getEmailAddress(),
        domainReference,
        logCorrelationId,
        mergedTemplate.getMailMergeFields(),
        fileAttachments,
        mergedTemplate.getTemplate()
    );

    return new EmailNotification(String.valueOf(notification.getId()));
  }
}
