package uk.co.fivium.testapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.sms.SmsNotification;
import uk.co.fivium.digitalnotificationlibrary.core.notification.sms.SmsRecipient;
import uk.gov.service.notify.Notification;

//@DisplayName("GIVEN I want to send an sms")
//@IntegrationTest
class SendSmsIntegrationTest extends AbstractIntegrationTest {

//  @DisplayName("WHEN notify sends the sms successfully")
//  @Nested
//  class WhenSentSuccessfully {
//
//    @DisplayName("THEN the sms is only sent once")
//    @Test
//    void thenTheSmsIsOnlySentOnce() {
//
//      var logCorrelationId = "sendSms_whenSentSuccessfully_thenTheSmsIsOnlySentOnce-%s".formatted(UUID.randomUUID());
//
//      sendSms(SmsRecipient.directPhoneNumber("07462856752"), logCorrelationId);
//
//      await()
//          .during(getNotificationPollDuration().multipliedBy(5))
//          .untilAsserted(() -> {
//
//            List<Notification> deliveredNotifyNotifications = getNotifications(logCorrelationId);
//
//            assertThat(deliveredNotifyNotifications)
//                .extracting(Notification::getPhoneNumber, Notification::getStatus, Notification::getReference)
//
//                .containsExactly(
//                    tuple(
//                        Optional.of("07462856752"),
//                        "delivered",
//                        Optional.of(logCorrelationId)
//                    )
//                );
//          });
//    }
//  }
//
//  @DisplayName("WHEN notify responds with a permanent failure")
//  @Nested
//  class WhenPermanentFailure {
//
//    private static final SmsRecipient PERMANENT_FAILURE_SMS_RECIPIENT
//        = SmsRecipient.directPhoneNumber("07700900002");
//
//    @DisplayName("THEN sms is not sent again")
//    @Test
//    void thenTheSmsIsNotRetried() {
//
//      var logCorrelationId = "sendSms_whenPermanentFailure_thenTheSmsIsNotRetried-%s".formatted(UUID.randomUUID());
//
//      sendSms(PERMANENT_FAILURE_SMS_RECIPIENT, logCorrelationId);
//
//      await()
//          .during(getNotificationPollDuration().multipliedBy(5))
//          .untilAsserted(() -> {
//            List<Notification> notificationSentToNotify = getNotifications(logCorrelationId);
//            assertThat(notificationSentToNotify)
//                .extracting(Notification::getPhoneNumber, Notification::getStatus, Notification::getReference)
//                .containsExactly(
//                    tuple(
//                        Optional.of(PERMANENT_FAILURE_SMS_RECIPIENT.getSmsRecipient()),
//                        "permanent-failure",
//                        Optional.of(logCorrelationId)
//                    )
//                );
//          });
//    }
//  }
//
//  @DisplayName("WHEN notify responds with a temporary failure")
//  @Nested
//  class WhenTemporaryFailure {
//
//    private static final SmsRecipient TEMPORARY_FAILURE_SMS_RECIPIENT
//        = SmsRecipient.directPhoneNumber("07700900003");
//
//    @DisplayName("THEN the sms will be retried")
//    @Test
//    void thenSmsWillBeRetried() {
//
//      var logCorrelationId = "sendSms_whenTemporaryFailure_thenSmsWillBeRetried-%s".formatted(UUID.randomUUID());
//
//      sendSms(TEMPORARY_FAILURE_SMS_RECIPIENT, logCorrelationId);
//
//      await()
//          .atMost(getNotificationPollDuration().multipliedBy(5))
//          .untilAsserted(() -> {
//
//            List<Notification> notificationSentToNotify = getNotifications(logCorrelationId);
//
//            // there will be at least two due to the retry
//            assertThat(notificationSentToNotify)
//                .extracting(Notification::getStatus, Notification::getPhoneNumber, Notification::getReference)
//                .contains(
//                    tuple(
//                        "temporary-failure",
//                        Optional.of(TEMPORARY_FAILURE_SMS_RECIPIENT.getSmsRecipient()),
//                        Optional.of(logCorrelationId)
//                    ),
//                    tuple(
//                        "temporary-failure",
//                        Optional.of(TEMPORARY_FAILURE_SMS_RECIPIENT.getSmsRecipient()),
//                        Optional.of(logCorrelationId)
//                    )
//                );
//          });
//    }
//
//    @DisplayName("AND the temporary failure is resolved")
//    @Nested
//    class WhenSuccessfulOnRetry {
//
//      @DisplayName("THEN the sms will be sent on the next retry")
//      @Test
//      void thenSmsWillBeSentSecondTime() {
//
//        var logCorrelationId = "sendSms_whenSuccessfulOnRetry_thenSmsWillBeSentSecondTime-%s".formatted(UUID.randomUUID());
//
//        // GIVEN a sms which results in a temporary failure
//        SmsNotification sentSms = sendSms(TEMPORARY_FAILURE_SMS_RECIPIENT, logCorrelationId);
//
//        // THEN notify will respond with a temporary failure
//        await()
//            .atMost(getNotificationPollDuration().multipliedBy(2))
//            .untilAsserted(() -> {
//
//              List<Notification> notificationSentToNotify = getNotifications(logCorrelationId);
//
//              assertThat(notificationSentToNotify)
//                  .extracting(Notification::getStatus, Notification::getPhoneNumber, Notification::getReference)
//                  .contains(
//                      tuple(
//                          "temporary-failure",
//                          Optional.of(TEMPORARY_FAILURE_SMS_RECIPIENT.getSmsRecipient()),
//                          Optional.of(logCorrelationId)
//                      )
//                  );
//            });
//
//        // WHEN we update the recipient to a recipient that will send successfully
//        jdbcTemplate.execute("""
//          UPDATE integration_test.notification_library_notifications
//          SET recipient = '07843276583', retry_count = 0, last_send_attempt_at = '%s'
//          WHERE id = '%s'
//            """.formatted(Instant.now().minusSeconds(600), sentSms.id())
//        );
//
//        // THEN notify will send the sms
//        await()
//            // wait long enough for the sms to be sent to notify
//            .atMost(getNotificationPollDuration().multipliedBy(5))
//            .untilAsserted(() -> {
//
//              List<Notification> notificationSentToNotify = getNotifications(logCorrelationId);
//
//              assertThat(notificationSentToNotify)
//                  .extracting(Notification::getPhoneNumber, Notification::getStatus, Notification::getReference)
//                  .contains(
//                      tuple(
//                          Optional.of("07843276583"),
//                          "delivered",
//                          Optional.of(logCorrelationId)
//                      )
//                  );
//            });
//      }
//    }
//
//    @DisplayName("WHEN max retry time exceeded")
//    @Nested
//    class WhenMaxRetryTimeExceeded {
//
//      @DisplayName("THEN the sms will not be retried")
//      @Test
//      void thenSmsWillNotBeRetried() {
//
//        var logCorrelationId = "sendSms_whenMaxRetryTimeExceeded_thenSmsWillNotBeRetried-%s".formatted(UUID.randomUUID());
//
//        // GIVEN a sms which results in a temporary failure
//        SmsNotification sentSms = sendSms(TEMPORARY_FAILURE_SMS_RECIPIENT, logCorrelationId);
//
//        // THEN notify will respond with a temporary failure
//        await()
//            .during(getNotificationPollDuration().multipliedBy(5))
//            .untilAsserted(() -> {
//
//              List<Notification> notificationSentToNotify = getNotifications(logCorrelationId);
//
//              assertThat(notificationSentToNotify)
//                  .extracting(Notification::getStatus, Notification::getPhoneNumber, Notification::getReference)
//                  .contains(
//                      tuple(
//                          "temporary-failure",
//                          Optional.of(TEMPORARY_FAILURE_SMS_RECIPIENT.getSmsRecipient()),
//                          Optional.of(logCorrelationId)
//                      )
//                  );
//            });
//
//        var overThreeDaysAgo = Instant.now().minus(80, ChronoUnit.HOURS);
//
//        // WHEN we update the recipient to a recipient that will send successfully
//        jdbcTemplate.execute("""
//          UPDATE integration_test.notification_library_notifications
//          SET
//            recipient = '07843276583', --reset recipient so test will fail if it sends
//            requested_on = '%s'
//          WHERE id = '%s'
//            """.formatted(overThreeDaysAgo, sentSms.id())
//        );
//
//        await()
//            .during(getNotificationPollDuration().multipliedBy(5))
//            .untilAsserted(() -> {
//
//              List<Notification> notificationSentToNotify = getNotifications(logCorrelationId);
//
//              assertThat(notificationSentToNotify)
//                  .extracting(Notification::getPhoneNumber, Notification::getStatus, Notification::getReference)
//                  .doesNotContain(
//                      tuple(
//                          Optional.of("07843276583"),
//                          "delivered",
//                          Optional.of(logCorrelationId)
//                      )
//                  );
//            });
//      }
//    }
//  }
//
//  private SmsNotification sendSms(SmsRecipient smsRecipient, String logCorrelationId) {
//
//    var mergedTemplate = getSmsMergeTemplate();
//
//    return notificationLibraryClient.sendSms(
//        mergedTemplate,
//        smsRecipient,
//        DomainReference.from("id", "integration-test"),
//        logCorrelationId
//    );
//  }
}
