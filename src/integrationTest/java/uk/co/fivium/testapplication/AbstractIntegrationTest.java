package uk.co.fivium.testapplication;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MergedTemplate;
import uk.co.fivium.digitalnotificationlibrary.core.notification.NotificationLibraryClient;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

abstract class AbstractIntegrationTest {

  @Autowired
  protected NotificationLibraryClient notificationLibraryClient;

  @Autowired
  protected NotificationLibraryConfigurationProperties libraryConfigurationProperties;

  @Autowired
  protected NotificationClient notifyNotificationClient;

  List<Notification> getNotifications(String logCorrelationId) throws NotificationClientException {
    return notifyNotificationClient.getNotifications(null, null, logCorrelationId, null)
        .getNotifications();
  }

  MergedTemplate getEmailMergeTemplate() {
    return getMergeTemplate(GovukNotifyTemplate.EMAIL_TEMPLATE);
  }

  MergedTemplate getSmsMergeTemplate() {
    return getMergeTemplate(GovukNotifyTemplate.SMS_TEMPLATE);
  }

  private MergedTemplate getMergeTemplate(GovukNotifyTemplate govukNotifyTemplate) {
    return notificationLibraryClient.getTemplate(govukNotifyTemplate.getGovukNotifyTemplateId())
        .withMailMergeField("name", "name-value")
        .withMailMergeField("reference", "reference-value")
        .merge();
  }
}
