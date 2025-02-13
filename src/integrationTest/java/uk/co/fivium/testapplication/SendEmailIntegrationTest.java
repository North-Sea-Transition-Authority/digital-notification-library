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
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailNotification;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.gov.service.notify.Notification;
//
//@DisplayName("GIVEN I want to send an email")
//@IntegrationTest
class SendEmailIntegrationTest extends AbstractIntegrationTest {

//  @DisplayName("WHEN notify sends the email successfully")
//  @Nested
//  class WhenSentSuccessfully {
//
//    @DisplayName("THEN the email is only sent once")
//    @Test
//    void thenTheEmailIsOnlySentOnce() {
//
//      var logCorrelationId = "sendEmail_whenSentSuccessfully_thenTheEmailIsOnlySentOnce-%s".formatted(UUID.randomUUID());
//
//      sendEmail(EmailRecipient.directEmailAddress("tess.mann@fivium.co.uk"), logCorrelationId);
//
//      await()
//          .during(getNotificationPollDuration().multipliedBy(5))
//          .untilAsserted(() -> {
//
//            List<Notification> deliveredNotifyNotifications = getNotifications(logCorrelationId);
//
//            assertThat(deliveredNotifyNotifications)
//                .extracting(Notification::getEmailAddress, Notification::getStatus, Notification::getReference)
//
//                .containsExactly(
//                    tuple(
//                        Optional.of("tess.mann@fivium.co.uk"),
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
//    private static final EmailRecipient PERMANENT_FAILURE_EMAIL_RECIPIENT
//        = EmailRecipient.directEmailAddress("perm-fail@simulator.notify");
//
//    @DisplayName("THEN email is not sent again")
//    @Test
//    void thenTheEmailIsNotRetried() {
//
//      var logCorrelationId = "sendEmail_whenPermanentFailure_thenTheEmailIsNotRetried-%s".formatted(UUID.randomUUID());
//
//      sendEmail(PERMANENT_FAILURE_EMAIL_RECIPIENT, logCorrelationId);
//
//      await()
//          .during(getNotificationPollDuration().multipliedBy(5))
//          .untilAsserted(() -> {
//            List<Notification> notificationSentToNotify = getNotifications(logCorrelationId);
//            assertThat(notificationSentToNotify)
//                .extracting(Notification::getEmailAddress, Notification::getStatus, Notification::getReference)
//                .containsExactly(
//                    tuple(
//                        Optional.of(PERMANENT_FAILURE_EMAIL_RECIPIENT.getEmailAddress()),
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
//    private static final EmailRecipient TEMPORARY_FAILURE_EMAIL_RECIPIENT
//        = EmailRecipient.directEmailAddress("temp-fail@simulator.notify");
//
//    @DisplayName("THEN the email will be retried")
//    @Test
//    void thenEmailWillBeRetried() {
//
//      var logCorrelationId = "sendEmail_whenTemporaryFailure_thenEmailWillBeRetried-%s".formatted(UUID.randomUUID());
//
//      sendEmail(TEMPORARY_FAILURE_EMAIL_RECIPIENT, logCorrelationId);
//
//      await()
//          .atMost(getNotificationPollDuration().multipliedBy(5))
//          .untilAsserted(() -> {
//
//            List<Notification> notificationSentToNotify = getNotifications(logCorrelationId);
//
//            // there will be at least two due to the retry
//            assertThat(notificationSentToNotify)
//                .extracting(Notification::getStatus, Notification::getEmailAddress, Notification::getReference)
//                .contains(
//                    tuple(
//                        "temporary-failure",
//                        Optional.of(TEMPORARY_FAILURE_EMAIL_RECIPIENT.getEmailAddress()),
//                        Optional.of(logCorrelationId)
//                    ),
//                    tuple(
//                        "temporary-failure",
//                        Optional.of(TEMPORARY_FAILURE_EMAIL_RECIPIENT.getEmailAddress()),
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
//      @DisplayName("THEN the email will be sent on the next retry")
//      @Test
//      void thenEmailWillBeSentSecondTime() {
//
//        var logCorrelationId = "sendEmail_whenSuccessfulOnRetry_thenEmailWillBeSentSecondTime-%s".formatted(UUID.randomUUID());
//
//        // GIVEN an email which results in a temporary failure
//        EmailNotification sentEmail = sendEmail(TEMPORARY_FAILURE_EMAIL_RECIPIENT, logCorrelationId);
//
//        // THEN notify will respond with a temporary failure
//        await()
//            .atMost(getNotificationPollDuration().multipliedBy(2))
//            .untilAsserted(() -> {
//
//              List<Notification> notificationSentToNotify = getNotifications(logCorrelationId);
//
//              assertThat(notificationSentToNotify)
//                  .extracting(Notification::getStatus, Notification::getEmailAddress, Notification::getReference)
//                  .contains(
//                      tuple(
//                          "temporary-failure",
//                          Optional.of(TEMPORARY_FAILURE_EMAIL_RECIPIENT.getEmailAddress()),
//                          Optional.of(logCorrelationId)
//                      )
//                  );
//            });
//
//        // WHEN we update the recipient to a recipient that will send successfully
//        jdbcTemplate.execute("""
//          UPDATE integration_test.notification_library_notifications
//          SET recipient = 'tess.mann@fivium.co.uk', retry_count = 0, last_send_attempt_at = '%s'
//          WHERE id = '%s'
//             """.formatted(Instant.now().minusSeconds(60), sentEmail.id())
//        );
//
//        // THEN notify will send the email
//        await()
//            .atMost(getNotificationPollDuration().multipliedBy(5))
//            .untilAsserted(() -> {
//
//              List<Notification> notificationSentToNotify = getNotifications(logCorrelationId);
//
//              assertThat(notificationSentToNotify)
//                  .extracting(Notification::getEmailAddress, Notification::getStatus, Notification::getReference)
//                  .contains(
//                      tuple(
//                          Optional.of("tess.mann@fivium.co.uk"),
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
//      @DisplayName("THEN the email will not be retried")
//      @Test
//      void thenEmailWillNotBeRetried() {
//
//        var logCorrelationId = "sendEmail_whenMaxRetryTimeExceeded_thenEmailWillNotBeRetried-%s".formatted(UUID.randomUUID());
//
//        // GIVEN an email which results in a temporary failure
//        EmailNotification sentEmail = sendEmail(TEMPORARY_FAILURE_EMAIL_RECIPIENT, logCorrelationId);
//
//        // THEN notify will respond with a temporary failure
//        await()
//            .during(getNotificationPollDuration().multipliedBy(5))
//            .untilAsserted(() -> {
//
//              List<Notification> notificationSentToNotify = getNotifications(logCorrelationId);
//
//              assertThat(notificationSentToNotify)
//                  .extracting(Notification::getStatus, Notification::getEmailAddress, Notification::getReference)
//                  .contains(
//                      tuple(
//                          "temporary-failure",
//                          Optional.of(TEMPORARY_FAILURE_EMAIL_RECIPIENT.getEmailAddress()),
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
//            recipient = 'tess.mann@fivium.co.uk', --reset recipient so test will fail if it sends
//            requested_on = '%s'
//          WHERE id = '%s'
//            """.formatted(overThreeDaysAgo, sentEmail.id())
//        );
//
//        await()
//            .during(getNotificationPollDuration().multipliedBy(5))
//            .untilAsserted(() -> {
//
//              List<Notification> notificationSentToNotify = getNotifications(logCorrelationId);
//
//              assertThat(notificationSentToNotify)
//                  .extracting(Notification::getEmailAddress, Notification::getStatus, Notification::getReference)
//                  .doesNotContain(
//                      tuple(
//                          Optional.of("tess.mann@fivium.co.uk"),
//                          "delivered",
//                          Optional.of(logCorrelationId)
//                      )
//                  );
//            });
//      }
//    }
//  }
//
//  private EmailNotification sendEmail(EmailRecipient emailRecipient, String logCorrelationId) {
//
//    var mergedTemplate = getEmailMergeTemplate();
//
//    return notificationLibraryClient.sendEmail(
//        mergedTemplate,
//        emailRecipient,
//        DomainReference.from("id", "integration-test"),
//        logCorrelationId
//    );
//  }
}
