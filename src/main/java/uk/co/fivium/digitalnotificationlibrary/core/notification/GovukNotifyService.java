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
import uk.gov.service.notify.Template;

@Service
class GovukNotifyService {

  private final NotificationClient notifyClient;

  @Autowired
  GovukNotifyService(NotificationClient notifyClient) {
    this.notifyClient = notifyClient;
  }

  Response<Template> getTemplate(String templateId) {
    try {
      return Response.successfulResponse(notifyClient.getTemplateById(templateId));
    } catch (NotificationClientException exception) {
      return Response.failedResponse(exception.getHttpResult(), exception.getMessage());
    }
  }

  Response<SendEmailResponse> sendEmail(Notification notification) {

    if (!NotificationType.EMAIL.equals(notification.getType())) {
      throw new IllegalStateException(
          "Cannot send an email for a notification of type %s".formatted(notification.getType())
      );
    }

    var mailMergeFields = convertMailMergedFieldsToNotifyFormat(notification.getMailMergeFields());

    try {
      var emailResponse = notifyClient.sendEmail(
          notification.getNotifyTemplateId(),
          notification.getRecipient(),
          mailMergeFields,
          notification.getLogCorrelationId()
      );

      return Response.successfulResponse(emailResponse);

    } catch (NotificationClientException exception) {
      return Response.failedResponse(exception.getHttpResult(), exception.getMessage());
    }
  }

  Response<SendSmsResponse> sendSms(Notification notification) {

    if (!NotificationType.SMS.equals(notification.getType())) {
      throw new IllegalStateException(
          "Cannot send an sms for a notification of type %s".formatted(notification.getType())
      );
    }

    var mailMergeFields = convertMailMergedFieldsToNotifyFormat(notification.getMailMergeFields());

    try {
      var emailResponse = notifyClient.sendSms(
          notification.getNotifyTemplateId(),
          notification.getRecipient(),
          mailMergeFields,
          notification.getLogCorrelationId()
      );

      return Response.successfulResponse(emailResponse);

    } catch (NotificationClientException exception) {
      return Response.failedResponse(exception.getHttpResult(), exception.getMessage());
    }
  }

  private Map<String, Object> convertMailMergedFieldsToNotifyFormat(Set<MailMergeField> mailMergeFields) {
    return mailMergeFields
        .stream()
        .collect(Collectors.toMap(MailMergeField::name, MailMergeField::value));
  }
}
