package uk.co.fivium.testapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.NotificationLibraryClient;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.fivium.digitalnotificationlibrary.core.notification.sms.SmsRecipient;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@DisplayName("GIVEN the library is running in production mode")
@IntegrationTest
@TestPropertySource(properties = {
    "digital-notification-library.mode=production",
    "digital-notification-library.test-mode.email-recipients=",
    "digital-notification-library.test-mode.sms-recipients="
})
class ProductionModeIntegrationTest {

  @Autowired
  private NotificationLibraryClient notificationLibraryClient;

  @Autowired
  private NotificationLibraryConfigurationProperties libraryConfigurationProperties;

  @Autowired
  private NotificationClient notifyNotificationClient;

  @DisplayName("WHEN I send an email")
  @Nested
  class WhenSendEmail {

    @DisplayName("THEN the email is sent to the intended recipient")
    @Test
    void thenIntendedRecipient() {

      var logCorrelationId = "sendEmail_thenIntendedRecipient-%s".formatted(UUID.randomUUID());

      var template = GovukNotifyTemplate.EMAIL_TEMPLATE;

      var mergedTemplate = notificationLibraryClient.getTemplate(template.getGovukNotifyTemplateId())
          .withMailMergeField("name", "name-value")
          .withMailMergeField("reference", "reference-value")
          .merge();

      notificationLibraryClient.sendEmail(
          mergedTemplate,
          EmailRecipient.directEmailAddress("tess.mann@fivium.co.uk"),
          DomainReference.from("id", "integration-test"),
          logCorrelationId
      );

      long notificationPollTimeSeconds = libraryConfigurationProperties.notification().pollTimeSeconds();

      await()
          .during(Duration.ofSeconds(2 * notificationPollTimeSeconds))
          .atMost(Duration.ofSeconds(20))
          .untilAsserted(() -> {

            List<Notification> deliveredNotifyNotifications = getNotifications(logCorrelationId);

            assertThat(deliveredNotifyNotifications)
                .extracting(Notification::getEmailAddress, Notification::getStatus, Notification::getReference)

                .containsExactly(
                    tuple(
                        Optional.of("tess.mann@fivium.co.uk"),
                        "delivered",
                        Optional.of(logCorrelationId)
                    )
                );
          });
    }
  }

  @DisplayName("WHEN I send an sms")
  @Nested
  class WhenSendSms {

    @DisplayName("THEN the sms is sent to the intended recipient")
    @Test
    void thenIntendedRecipient() {

      var logCorrelationId = "sendSms_thenIntendedRecipient-%s".formatted(UUID.randomUUID());

      var template = GovukNotifyTemplate.SMS_TEMPLATE;

      var mergedTemplate = notificationLibraryClient.getTemplate(template.getGovukNotifyTemplateId())
          .withMailMergeField("name", "name-value")
          .withMailMergeField("reference", "reference-value")
          .merge();

      notificationLibraryClient.sendSms(
          mergedTemplate,
          SmsRecipient.directPhoneNumber("07913487392"),
          DomainReference.from("id", "integration-test"),
          logCorrelationId
      );

      long notificationPollTimeSeconds = libraryConfigurationProperties.notification().pollTimeSeconds();

      await()
          .during(Duration.ofSeconds(2 * notificationPollTimeSeconds))
          .atMost(Duration.ofSeconds(20))
          .untilAsserted(() -> {

            List<Notification> deliveredNotifyNotifications = getNotifications(logCorrelationId);

            assertThat(deliveredNotifyNotifications)
                .extracting(Notification::getPhoneNumber, Notification::getStatus, Notification::getReference)
                .containsExactly(
                    tuple(
                        Optional.of("07913487392"),
                        "delivered",
                        Optional.of(logCorrelationId)
                    )
                );
          });
    }
  }

  private List<Notification> getNotifications(String logCorrelationId) throws NotificationClientException {
    return notifyNotificationClient.getNotifications(null, null, logCorrelationId, null)
        .getNotifications();
  }
}
