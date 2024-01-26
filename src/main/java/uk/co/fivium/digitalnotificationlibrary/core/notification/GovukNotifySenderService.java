package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

@Service
class GovukNotifySenderService {

  private final NotificationClient notifyClient;

  @Autowired
  GovukNotifySenderService(NotificationClient notifyClient) {
    this.notifyClient = notifyClient;
  }

  Response<SendEmailResponse> sendEmail(Notification notification) {
    return sendEmail(notification, notification.getRecipient());
  }

  Response<SendEmailResponse> sendEmail(Notification notification, String recipient) {

    if (!NotificationType.EMAIL.equals(notification.getType())) {
      throw new IllegalStateException(
          "Cannot send an email for a notification of type %s".formatted(notification.getType())
      );
    }

    Map<String, Object> mailMergeFields = toNotifyMailMergeFormat(notification.getMailMergeFields());

    try {
      var notifyResponse = notifyClient.sendEmail(
          notification.getNotifyTemplateId(),
          recipient,
          mailMergeFields,
          notification.getLogCorrelationId()
      );

      return Response.successfulResponse(notifyResponse);

    } catch (NotificationClientException exception) {
      return Response.failedResponse(exception.getHttpResult(), exception.getMessage());
    }
  }

  Response<SendSmsResponse> sendSms(Notification notification) {
    return sendSms(notification, notification.getRecipient());
  }

  Response<SendSmsResponse> sendSms(Notification notification, String recipient) {

    if (!NotificationType.SMS.equals(notification.getType())) {
      throw new IllegalStateException(
          "Cannot send an sms for a notification of type %s".formatted(notification.getType())
      );
    }

    Map<String, Object> mailMergeFields = toNotifyMailMergeFormat(notification.getMailMergeFields());

    try {
      var notifyResponse = notifyClient.sendSms(
          notification.getNotifyTemplateId(),
          recipient,
          mailMergeFields,
          notification.getLogCorrelationId()
      );

      return Response.successfulResponse(notifyResponse);

    } catch (NotificationClientException exception) {
      return Response.failedResponse(exception.getHttpResult(), exception.getMessage());
    }
  }

  private Map<String, Object> toNotifyMailMergeFormat(Set<MailMergeField> mailMergeFields) {
    return mailMergeFields
        .stream()
        .collect(Collectors.toMap(MailMergeField::name, MailMergeField::value));
  }
}
