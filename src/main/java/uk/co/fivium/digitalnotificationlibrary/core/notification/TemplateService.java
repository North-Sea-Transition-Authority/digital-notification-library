package uk.co.fivium.digitalnotificationlibrary.core.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.Template;

@Service
class TemplateService {

  private final NotificationClient notifyClient;

  @Autowired
  TemplateService(NotificationClient notifyClient) {
    this.notifyClient = notifyClient;
  }

  Response<Template> getTemplate(String templateId) {
    try {
      return Response.successfulResponse(notifyClient.getTemplateById(templateId));
    } catch (NotificationClientException exception) {
      return Response.failedResponse(exception.getHttpResult(), exception.getMessage());
    }
  }
}
