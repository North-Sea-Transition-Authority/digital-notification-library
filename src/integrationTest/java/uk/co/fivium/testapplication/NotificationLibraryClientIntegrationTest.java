package uk.co.fivium.testapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;
import uk.co.fivium.digitalnotificationlibrary.core.DigitalNotificationLibraryException;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.NotificationLibraryClient;
import uk.co.fivium.digitalnotificationlibrary.core.notification.Template;
import uk.co.fivium.digitalnotificationlibrary.core.notification.TemplateType;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.fivium.digitalnotificationlibrary.core.notification.sms.SmsRecipient;
import uk.gov.service.notify.Notification;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@IntegrationTest
@DisplayName("GIVEN I am a consumer of the library")
class NotificationLibraryClientIntegrationTest {

  @Autowired
  private NotificationLibraryClient notificationLibraryClient;

  @Autowired
  private NotificationClient notifyNotificationClient;

  @Autowired
  private NotificationLibraryConfigurationProperties libraryConfigurationProperties;

  @Nested
  @DisplayName("WHEN I call the getTemplate method")
  class GetTemplateTestScenarios {

    @Test
    @DisplayName("THEN a template is returned when the template exists in GOV.UK Notify")
    void getTemplate_whenTemplateExistsInNotify() {

      var template = GovukNotifyTemplate.EMAIL_TEMPLATE;

      var resultingTemplate = notificationLibraryClient.getTemplate(template.getGovukNotifyTemplateId());

      assertThat(resultingTemplate)
          .extracting(
              Template::notifyTemplateId,
              Template::type,
              Template::verificationStatus
          )
          .contains(
              template.getGovukNotifyTemplateId(),
              TemplateType.EMAIL,
              Template.VerificationStatus.CONFIRMED_NOTIFY_TEMPLATE
          );

      assertThat(resultingTemplate.requiredMailMergeFields())
          .containsExactlyInAnyOrder("name", "reference");
    }

    @Test
    @DisplayName("THEN an exception is raised when the template doesn't exist in GOV.UK Notify")
    void getTemplate_whenIdHasValidNotifyFormat_andNoTemplateExists() {

      var validFormatTemplateId = UUID.randomUUID().toString();

      assertThatThrownBy(() -> notificationLibraryClient.getTemplate(validFormatTemplateId))
          .isInstanceOf(DigitalNotificationLibraryException.class);
    }

    @Test
    @DisplayName("THEN an exception is raised when the template ID is not in the correct format")
    void getTemplate_whenNotValidNotifyTemplateIdFormat() {

      var invalidFormatTemplateId = "not-a-uuid-format";

      assertThatThrownBy(() -> notificationLibraryClient.getTemplate(invalidFormatTemplateId))
          .isInstanceOf(DigitalNotificationLibraryException.class);
    }
  }

  @Nested
  @DisplayName("WHEN I call the sendEmail method")
  class SendEmailTestScenarios {

    @Test
    @DisplayName("THEN an email is sent by GOV.UK Notify")
    void sendEmail_verifyNotifySendsEmail() {

      var logCorrelationId = "sendEmail_verifyNotifySendsEmail-%s".formatted(UUID.randomUUID());

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

      long queuedNotificationPollTimeSeconds = libraryConfigurationProperties.notification().queued().pollTimeSeconds();

      await()
          .timeout(Duration.of(2 * queuedNotificationPollTimeSeconds, ChronoUnit.SECONDS))
          .untilAsserted(() -> {

            Optional<Notification> deliveredNotifyNotification = getNotification(logCorrelationId);

            assertThat(deliveredNotifyNotification).get()
                .extracting(Notification::getStatus, Notification::getReference)
                .containsExactly("delivered", Optional.of(logCorrelationId));
          });
    }
  }

  @Nested
  @DisplayName("WHEN I call the sendSms method")
  class SendSmsTestScenarios {

    @Test
    @DisplayName("THEN an sms is sent by GOV.UK Notify")
    void sendSms_verifyNotifySendsSms() {

      var logCorrelationId = "sendSms_verifyNotifySendsSms-%s".formatted(UUID.randomUUID());

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

      long queuedNotificationPollTimeSeconds = libraryConfigurationProperties.notification().queued().pollTimeSeconds();

      await()
          .timeout(Duration.of(2 * queuedNotificationPollTimeSeconds, ChronoUnit.SECONDS))
          .untilAsserted(() -> {

            Optional<Notification> sentNotifyNotification = getNotification(logCorrelationId);

            assertThat(sentNotifyNotification).get()
                .extracting(Notification::getStatus, Notification::getReference)
                .containsExactly("delivered", Optional.of(logCorrelationId));
          });
    }
  }

  private Optional<Notification> getNotification(String logCorrelationId) throws NotificationClientException {
    return notifyNotificationClient.getNotifications(null, null, logCorrelationId, null)
        .getNotifications()
        .stream()
        .findFirst();
  }

}
