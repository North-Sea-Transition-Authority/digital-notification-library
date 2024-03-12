package uk.co.fivium.testapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.gov.service.notify.Notification;

@DisplayName("GIVEN I have notifications in the database")
@IntegrationTest
class NotificationProcessingOrderIntegrationTest extends AbstractIntegrationTest {

  @DisplayName("THEN they will be processed based on oldest send attempt date with nulls first")
  @Test
  void notificationsProcessedByLastSendAttemptNullsFirst() {

    var mergeTemplate = getEmailMergeTemplate();

    // schedule emails so in database, then update values to set up specifics for the test.
    // If notify sends them already they will be ignored and resent later in the test. The log
    // correlation IDs are replaced to avoid assertion picking up original notifications sent
    // before we have set up the test cases.

    var logCorrelationId = "notificationsProcessedByLastSendAttemptNullsFirst-" + UUID.randomUUID();

    var emailWithNullLastSendAttemptAt = notificationLibraryClient.sendEmail(
        mergeTemplate,
        EmailRecipient.directEmailAddress("never-sent-before@example.com"),
        DomainReference.from("id", "name"),
        "log-correlation-id"
    );

    jdbcTemplate.execute("""
          UPDATE integration_test.notification_library_notifications
          SET last_send_attempt_at = null, log_correlation_id = '%s', status = '%s'
          WHERE id = '%s'
          """
        .formatted(
            logCorrelationId,
            "QUEUED",
            emailWithNullLastSendAttemptAt.id()
        )
    );

    var mostRecentLastSendAttemptAt = notificationLibraryClient.sendEmail(
        mergeTemplate,
        EmailRecipient.directEmailAddress("most-recent-last-send-attempt@example.com"),
        DomainReference.from("id", "name"),
        "log-correlation-id"
    );

    jdbcTemplate.execute("""
          UPDATE integration_test.notification_library_notifications
          SET last_send_attempt_at = '%s', log_correlation_id = '%s', status = '%s'
          WHERE id = '%s'
          """
        .formatted(
            Instant.now().minusSeconds(10),
            logCorrelationId,
            "QUEUED",
            mostRecentLastSendAttemptAt.id()
        )
    );

    var earliestLastSendAttemptAt = notificationLibraryClient.sendEmail(
        mergeTemplate,
        EmailRecipient.directEmailAddress("earliest-last-send-attempt@example.com"),
        DomainReference.from("id", "name"),
        "log-correlation-id"
    );

    jdbcTemplate.execute("""
          UPDATE integration_test.notification_library_notifications
          SET last_send_attempt_at = '%s', log_correlation_id = '%s', status = '%s'
          WHERE id = '%s'
          """
        .formatted(
            Instant.now().minusSeconds(50),
            logCorrelationId,
            "QUEUED",
            earliestLastSendAttemptAt.id()
        )
    );

    await()
        .atMost(getNotificationPollDuration().multipliedBy(5))
        .untilAsserted(() -> {

          List<Notification> notificationSentToNotify = getNotifications(logCorrelationId)
              .stream()
              .sorted(Comparator.comparing(Notification::getCreatedAt))
              .toList();

          // assert they were sent to notify in the correct order
          assertThat(notificationSentToNotify)
              .extracting(Notification::getEmailAddress)
              .containsExactly(
                  Optional.of("never-sent-before@example.com"),
                  Optional.of("earliest-last-send-attempt@example.com"),
                  Optional.of("most-recent-last-send-attempt@example.com")
              );

          var notificationsOrderedByStatusUpdate = """
                  SELECT id
                  FROM integration_test.notification_library_notifications
                  WHERE id IN ('%s', '%s', '%s')
                  ORDER BY notify_status_last_updated_at ASC
              """
              .formatted(
                  emailWithNullLastSendAttemptAt.id(),
                  earliestLastSendAttemptAt.id(),
                  mostRecentLastSendAttemptAt.id()
              );

          List<String> notificationStatusUpdates = jdbcTemplate.queryForList(notificationsOrderedByStatusUpdate, String.class);

          // assert that the notification statuses are updated in the order we expect
          assertThat(notificationStatusUpdates)
              .containsExactly(
                  emailWithNullLastSendAttemptAt.id(),
                  earliestLastSendAttemptAt.id(),
                  mostRecentLastSendAttemptAt.id()
              );
        });
  }
}
