package uk.co.fivium.digitalnotificationlibrary.core.notification;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.Set;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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

  private final GovukNotifyService govukNotifyService;

  private final NotificationLibraryNotificationRepository notificationRepository;

  private final Clock clock;

  /**
   * Create an instance of NotificationLibraryClient.
   *
   * @param govukNotifyService The Gov UK notify service.
   * @param notificationRepository The notification repository
   * @param clock The clock instance
   */
  @Autowired
  public NotificationLibraryClient(GovukNotifyService govukNotifyService,
                                   NotificationLibraryNotificationRepository notificationRepository,
                                   Clock clock) {
    this.govukNotifyService = govukNotifyService;
    this.notificationRepository = notificationRepository;
    this.clock = clock;
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

    var templateResponse = govukNotifyService.getTemplate(notifyTemplateId);

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
   * @param mergedTemplate The template with mail merge fields to send
   * @param recipient The recipient of the notification
   * @param domainReference A reference to the consumers domain concept the notification is for
   * @param logCorrelationId An identifier for log correlation
   * @return A representation of the notification that has been queued to send
   */
  @Transactional
  public EmailNotification sendEmail(MergedTemplate mergedTemplate,
                                     EmailRecipient recipient,
                                     DomainReference domainReference,
                                     String logCorrelationId) {

    if (mergedTemplate == null) {
      throw new DigitalNotificationLibraryException("MergedTemplate must not be null");
    }

    if (!TemplateType.EMAIL.equals(mergedTemplate.getTemplate().type())) {
      throw new DigitalNotificationLibraryException(
          "Cannot send an email with template of type %s".formatted(mergedTemplate.getTemplate().type())
      );
    }

    if (recipient == null || !StringUtils.hasText(recipient.getEmailAddress())) {
      throw new DigitalNotificationLibraryException("EmailRecipient must not be null or empty");
    }

    if (domainReference == null) {
      throw new DigitalNotificationLibraryException("DomainReference must not be null");
    }

    var notification = queueNotification(
        NotificationType.EMAIL,
        recipient.getEmailAddress(),
        domainReference,
        logCorrelationId,
        mergedTemplate.getMailMergeFields(),
        mergedTemplate.getTemplate()
    );

    return new EmailNotification(String.valueOf(notification.getId()));
  }

  /**
   * Queue an email notification to be sent.
   * @param mergedTemplate The template with mail merge fields to send
   * @param recipient The recipient of the notification
   * @param domainReference A reference to the consumers domain concept the notification is for
   * @return A representation of the notification that has been queued to send
   */
  @Transactional
  public EmailNotification sendEmail(MergedTemplate mergedTemplate,
                                     EmailRecipient recipient,
                                     DomainReference domainReference) {
    return sendEmail(mergedTemplate, recipient, domainReference, null);
  }

  /**
   * Queue an sms notification to be sent.
   * @param mergedTemplate The template with mail merge fields to send
   * @param recipient The recipient of the notification
   * @param domainReference A reference to the consumers domain concept the notification is for
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

    if (!TemplateType.SMS.equals(mergedTemplate.getTemplate().type())) {
      throw new DigitalNotificationLibraryException(
          "Cannot send an sms with template of type %s".formatted(mergedTemplate.getTemplate().type())
      );
    }

    if (recipient == null || !StringUtils.hasText(recipient.getSmsRecipient())) {
      throw new DigitalNotificationLibraryException("SmsRecipient must not be null or empty");
    }

    if (domainReference == null) {
      throw new DigitalNotificationLibraryException("DomainReference must not be null");
    }

    var notification = queueNotification(
        NotificationType.SMS,
        recipient.getSmsRecipient(),
        domainReference,
        logCorrelationId,
        mergedTemplate.getMailMergeFields(),
        mergedTemplate.getTemplate()
    );

    return new SmsNotification(String.valueOf(notification.getId()));
  }

  /**
   * Queue an sms notification to be sent.
   * @param mergedTemplate The template with mail merge fields to send
   * @param recipient The recipient of the notification
   * @param domainReference A reference to the consumers domain concept the notification is for
   * @return A representation of the notification that has been queued to send
   */
  @Transactional
  public SmsNotification sendSms(MergedTemplate mergedTemplate,
                                 SmsRecipient recipient,
                                 DomainReference domainReference) {
    return sendSms(mergedTemplate, recipient, domainReference, null);
  }


  private Notification queueNotification(NotificationType notificationType,
                                         String recipient,
                                         DomainReference domainReference,
                                         String logCorrelationId,
                                         Set<MailMergeField> mailMergeFields,
                                         Template template) {

    var notification = new Notification();
    notification.setStatus(NotificationStatus.QUEUED);
    notification.setType(notificationType);
    notification.setRecipient(recipient);
    notification.setDomainReferenceId(domainReference.getId());
    notification.setDomainReferenceType(domainReference.getType());
    notification.setMailMergeFields(mailMergeFields);
    notification.setNotifyTemplateId(template.notifyTemplateId());
    notification.setRequestedOn(clock.instant());

    if (StringUtils.hasText(logCorrelationId)) {
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
}
