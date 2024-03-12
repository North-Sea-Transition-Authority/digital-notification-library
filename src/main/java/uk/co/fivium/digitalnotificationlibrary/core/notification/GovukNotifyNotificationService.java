package uk.co.fivium.digitalnotificationlibrary.core.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@Service
class GovukNotifyNotificationService {

  private final NotificationClient govukNotificationClient;

  @Autowired
  GovukNotifyNotificationService(NotificationClient govukNotificationClient) {
    this.govukNotificationClient = govukNotificationClient;
  }

  Response<uk.gov.service.notify.Notification> getNotification(Notification notification) {
    try {
      return Response.successfulResponse(
          govukNotificationClient.getNotificationById(notification.getNotifyNotificationId())
      );
    } catch (NotificationClientException exception) {
      return Response.failedResponse(exception.getHttpResult(), exception.getMessage());
    }
  }
}
