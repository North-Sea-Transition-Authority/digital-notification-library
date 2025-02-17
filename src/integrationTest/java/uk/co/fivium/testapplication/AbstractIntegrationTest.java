package uk.co.fivium.testapplication;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MergedTemplate;
import uk.co.fivium.digitalnotificationlibrary.core.notification.NotificationLibraryClient;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

abstract class AbstractIntegrationTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationTest.class);

  @Autowired
  protected NotificationLibraryClient notificationLibraryClient;

  @Autowired
  protected NotificationLibraryConfigurationProperties libraryConfigurationProperties;

  @Autowired
  protected NotificationClient notifyNotificationClient;

  @Autowired
  protected JdbcTemplate jdbcTemplate;

  @Autowired
  protected EmailAttachmentResolverTestImpl emailAttachmentResolver;

  @AfterEach
  void afterEachDatabaseTeardown() {
    jdbcTemplate.execute("DELETE FROM integration_test.notification_library_notifications");
    jdbcTemplate.execute("DELETE FROM integration_test.notification_library_notifications_aud");
    LOGGER.info("notification_library_notifications table state has been reset following test execution");
  }

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

  Duration getNotificationPollDuration() {
    return Duration.ofSeconds(libraryConfigurationProperties.notification().pollTimeSeconds());
  }

  private MergedTemplate getMergeTemplate(GovukNotifyTemplate govukNotifyTemplate) {
    return notificationLibraryClient.getTemplate(govukNotifyTemplate.getGovukNotifyTemplateId())
        .withMailMergeField("name", "name-value")
        .withMailMergeField("reference", "reference-value")
        .withFileAttachment("link_to_file", UUID.randomUUID(), "fileName.pdf")
        .merge();
  }
}
