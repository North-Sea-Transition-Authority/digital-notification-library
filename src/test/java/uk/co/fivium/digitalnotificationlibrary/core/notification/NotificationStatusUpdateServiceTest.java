package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationPropertiesTestUtil;

@DisplayName("GIVEN I want to update the status of a notification")
@ExtendWith(MockitoExtension.class)
class NotificationStatusUpdateServiceTest {

  private static final Instant FIXED_INSTANT = Instant.now();

  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneId.systemDefault());

  private static final Integer BULK_RETRIEVAL_LIMIT = 10;

  private static NotificationLibraryNotificationRepository notificationRepository;

  private static PlatformTransactionManager transactionManager;

  private static GovukNotifyNotificationService govukNotifyNotificationService;

  private static NotificationRetryScheduleService notificationRetryScheduleService;

  private static NotificationLibraryConfigurationProperties libraryConfigurationProperties;

  private static NotificationStatusUpdateService notificationStatusUpdateService;

  @Captor
  private ArgumentCaptor<Notification> notificationCaptor;

  @BeforeAll
  static void setup() {

    notificationRepository = mock(NotificationLibraryNotificationRepository.class);

    transactionManager = mock(PlatformTransactionManager.class);

    govukNotifyNotificationService = mock(GovukNotifyNotificationService.class);

    notificationRetryScheduleService = mock(NotificationRetryScheduleService.class);

    libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
        .withNotificationRetrievalLimit(BULK_RETRIEVAL_LIMIT)
        .build();

    notificationStatusUpdateService = new NotificationStatusUpdateService(
        transactionManager,
        notificationRepository,
        libraryConfigurationProperties,
        govukNotifyNotificationService,
        FIXED_CLOCK,
        notificationRetryScheduleService
    );
  }

  @DisplayName("WHEN there are no notifications to update")
  @Nested
  class WhenNoNotifications {

    @BeforeAll
    static void setup() {

      notificationRepository = mock(NotificationLibraryNotificationRepository.class);

      notificationStatusUpdateService = new NotificationStatusUpdateService(
          transactionManager,
          notificationRepository,
          libraryConfigurationProperties,
          govukNotifyNotificationService,
          FIXED_CLOCK,
          notificationRetryScheduleService
      );
    }

    @DisplayName("THEN no notifications are saved")
    @Test
    void whenNoNotificationsToCheck() {

      givenDatabaseReturnsNoNotifications();

      notificationStatusUpdateService.updateNotificationStatuses();

      then(notificationRepository)
          .should(never())
          .save(any());
    }
  }

  @DisplayName("WHEN notification already sent to notify")
  @Nested
  class WhenSentToNotifyNotification {

    private Notification sentToNotifyNotification;

    @BeforeEach
    void beforeEachSetup() {

      sentToNotifyNotification = new Notification();
      sentToNotifyNotification.setStatus(NotificationStatus.SENT_TO_NOTIFY);

      libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
          .withNotificationRetrievalLimit(BULK_RETRIEVAL_LIMIT)
          .build();

      notificationRepository = mock(NotificationLibraryNotificationRepository.class);

      notificationStatusUpdateService = new NotificationStatusUpdateService(
          transactionManager,
          notificationRepository,
          libraryConfigurationProperties,
          govukNotifyNotificationService,
          FIXED_CLOCK,
          notificationRetryScheduleService
      );
    }

    @DisplayName("AND notify returns an error looking up the notification")
    @Nested
    class AndErrorResponseFromNotify {

      @DisplayName("THEN status remains as sent to notify")
      @Test
      void whenErrorResponse_thenStatusSame() {

        givenDatabaseReturnsNotification(sentToNotifyNotification);

        given(govukNotifyNotificationService.getNotification(sentToNotifyNotification))
            .willReturn(Response.failedResponse(500, "notify is down"));

        notificationStatusUpdateService.updateNotificationStatuses();

        then(notificationRepository)
            .should()
            .save(notificationCaptor.capture());

        assertThat(notificationCaptor.getValue())
            .extracting(Notification::getStatus, Notification::getLastFailedAt)
            .containsExactly(NotificationStatus.SENT_TO_NOTIFY, FIXED_INSTANT);

        assertThat(notificationCaptor.getValue().getFailureReason()).isNotNull();
      }
    }

    @DisplayName("AND notify returns a status unknown to the library")
    @Nested
    class WhenUnknownNotifyStatus {

      @DisplayName("THEN notification status is UNEXPECTED_NOTIFY_RESPONSE")
      @Test
      void whenUnknownNotifyStatus_thenStatusUnexpectedNotifyResponse() {

        givenDatabaseReturnsNotification(sentToNotifyNotification);

        var notifyNotification = NotifyNotificationTestUtil.builder()
            .withStatus("unknown-notify-status")
            .build();

        given(govukNotifyNotificationService.getNotification(sentToNotifyNotification))
            .willReturn(Response.successfulResponse(notifyNotification));

        notificationStatusUpdateService.updateNotificationStatuses();

        then(notificationRepository)
            .should()
            .save(notificationCaptor.capture());

        var savedNotification = notificationCaptor.getValue();

        assertThat(savedNotification)
            .extracting(
                Notification::getStatus,
                Notification::getNotifyStatus,
                Notification::getNotifyStatusLastUpdatedAt
            )
            .containsExactly(
                NotificationStatus.UNEXPECTED_NOTIFY_STATUS,
                "unknown-notify-status",
                FIXED_INSTANT
            );

        assertThat(savedNotification)
            .extracting(Notification::getFailureReason, Notification::getLastFailedAt)
            .containsOnlyNulls();
      }
    }

    @DisplayName("AND notify returns a status indicating we should not retry sending")
    @Nested
    class WhenPermanentFailureNotifyStatus {

      @DisplayName("THEN notification status is FAILED_NOT_SENT")
      @Test
      void whenPermanentFailureNotifyStatus_thenStatusIsFailedNotSent() {

        givenDatabaseReturnsNotification(sentToNotifyNotification);

        var notifyNotification = NotifyNotificationTestUtil.builder()
            .withStatus(GovukNotifyNotificationStatus.PERMANENT_FAILURE)
            .build();

        given(govukNotifyNotificationService.getNotification(sentToNotifyNotification))
            .willReturn(Response.successfulResponse(notifyNotification));

        notificationStatusUpdateService.updateNotificationStatuses();

        then(notificationRepository)
            .should()
            .save(notificationCaptor.capture());

        var savedNotification = notificationCaptor.getValue();

        assertThat(savedNotification)
            .extracting(
                Notification::getStatus,
                Notification::getNotifyStatus,
                Notification::getNotifyStatusLastUpdatedAt,
                Notification::getFailureReason,
                Notification::getLastFailedAt
            )
            .containsExactly(
                NotificationStatus.FAILED_NOT_SENT,
                GovukNotifyNotificationStatus.PERMANENT_FAILURE.getStatus(),
                FIXED_INSTANT,
                "GOV.UK notify returned permanent failure response.",
                FIXED_INSTANT
            );
      }
    }

    @DisplayName("AND notify returns a status indicating we should retry sending")
    @Nested
    class WhenRetryableErrorNotifyStatus {

      private Notification failingNotification;

      @BeforeEach
      void setup() {

        Instant yesterday = FIXED_CLOCK.instant().minus(1, ChronoUnit.DAYS);

        failingNotification = new Notification();
        failingNotification.setStatus(NotificationStatus.SENT_TO_NOTIFY);
        failingNotification.setLastFailedAt(yesterday);
        failingNotification.setFailureReason("failure reason");
      }

      static class RetryableNotifyStatus implements ArgumentsProvider {

        @Override
        public Stream<Arguments> provideArguments(ExtensionContext context) {
          return Stream.of(
              Arguments.of(GovukNotifyNotificationStatus.TEMPORARY_FAILURE),
              Arguments.of(GovukNotifyNotificationStatus.TECHNICAL_FAILURE)
          );
        }
      }

      @DisplayName("AND the maximum retry time has been exceeded")
      @Nested
      class WhenMaxRetryTimeHasBeenExceeded {

        @DisplayName("THEN notification status is FAILED_NOT_SEND")
        @ParameterizedTest(name = "WHEN notify status is {0}")
        @ArgumentsSource(RetryableNotifyStatus.class)
        void whenMaxRetryTimeHasBeenExceeded_thenStatusIsFailedNotSent(GovukNotifyNotificationStatus notifyNotificationStatus) {

          givenDatabaseReturnsNotification(failingNotification);

          var notifyNotification = NotifyNotificationTestUtil.builder()
              .withStatus(notifyNotificationStatus)
              .build();

          given(govukNotifyNotificationService.getNotification(failingNotification))
              .willReturn(Response.successfulResponse(notifyNotification));

          given(notificationRetryScheduleService.hasReachedMaxRetryTime(failingNotification))
              .willReturn(true);

          notificationStatusUpdateService.updateNotificationStatuses();

          then(notificationRepository)
              .should()
              .save(notificationCaptor.capture());

          var savedNotification = notificationCaptor.getValue();

          assertThat(savedNotification)
              .extracting(
                  Notification::getStatus,
                  Notification::getNotifyStatus,
                  Notification::getNotifyStatusLastUpdatedAt,
                  Notification::getFailureReason,
                  Notification::getLastFailedAt
              )
              .containsExactly(
                  NotificationStatus.FAILED_NOT_SENT,
                  notifyNotificationStatus.getStatus(),
                  FIXED_INSTANT,
                  "Maximum retry time since requested on date exceeded",
                  FIXED_INSTANT
              );
        }
      }

      @DisplayName("AND the next retry time has been reached")
      @Nested
      class WhenNextRetryTimeReached {

        @DisplayName("THEN notification status is RETRY")
        @ParameterizedTest(name = "WHEN notify status is {0}")
        @ArgumentsSource(RetryableNotifyStatus.class)
        void whenNextRetryTimeReached_thenStatusIsRetry(GovukNotifyNotificationStatus notifyNotificationStatus) {

          givenDatabaseReturnsNotification(failingNotification);

          var notifyNotification = NotifyNotificationTestUtil.builder()
              .withStatus(notifyNotificationStatus)
              .build();

          given(govukNotifyNotificationService.getNotification(failingNotification))
              .willReturn(Response.successfulResponse(notifyNotification));

          given(notificationRetryScheduleService.hasReachedNextRetryTime(failingNotification))
              .willReturn(true);

          notificationStatusUpdateService.updateNotificationStatuses();

          then(notificationRepository)
              .should()
              .save(notificationCaptor.capture());

          var savedNotification = notificationCaptor.getValue();

          assertThat(savedNotification)
              .extracting(
                  Notification::getStatus,
                  Notification::getNotifyStatus,
                  Notification::getNotifyStatusLastUpdatedAt,
                  Notification::getLastFailedAt
              )
              .containsExactly(
                  NotificationStatus.RETRY,
                  notifyNotificationStatus.getStatus(),
                  FIXED_INSTANT,
                  FIXED_INSTANT
              );

          assertThat(savedNotification.getFailureReason()).isNull();
        }
      }

      @DisplayName("AND the next retry time has not been reached")
      @Nested
      class WhenNextRetryTimeNotReached {

        @DisplayName("THEN notification status is not updated")
        @ParameterizedTest(name = "WHEN notify status is {0}")
        @ArgumentsSource(RetryableNotifyStatus.class)
        void whenMaxRetryTimeHasBeenExceeded_thenStatusIsNotUpdated(GovukNotifyNotificationStatus notifyNotificationStatus) {

          givenDatabaseReturnsNotification(failingNotification);

          var notifyNotification = NotifyNotificationTestUtil.builder()
              .withStatus(notifyNotificationStatus)
              .build();

          given(govukNotifyNotificationService.getNotification(failingNotification))
              .willReturn(Response.successfulResponse(notifyNotification));

          given(notificationRetryScheduleService.hasReachedNextRetryTime(failingNotification))
              .willReturn(false);

          notificationStatusUpdateService.updateNotificationStatuses();

          then(notificationRepository)
              .should()
              .save(notificationCaptor.capture());

          var savedNotification = notificationCaptor.getValue();

          assertThat(savedNotification)
              .extracting(
                  Notification::getStatus,
                  Notification::getNotifyStatus,
                  Notification::getNotifyStatusLastUpdatedAt,
                  Notification::getLastFailedAt,
                  Notification::getFailureReason
              )
              .containsExactly(
                  NotificationStatus.SENT_TO_NOTIFY,
                  notifyNotificationStatus.getStatus(),
                  FIXED_INSTANT,
                  FIXED_INSTANT,
                  failingNotification.getFailureReason()
              );
        }
      }
    }

    @DisplayName("AND notify returns a status indicating notification was sent")
    @Nested
    class WhenSentNotifyStatus {

      @DisplayName("THEN notification status is SENT")
      @ParameterizedTest(name = "WHEN notify status is {0}")
      @EnumSource(
          value = GovukNotifyNotificationStatus.class,
          mode = EnumSource.Mode.INCLUDE,
          names = {"SENT", "DELIVERED"}
      )
      void whenSentNotifyStatus_thenStatusIsSent(GovukNotifyNotificationStatus notifyNotificationStatus) {

        givenDatabaseReturnsNotification(sentToNotifyNotification);

        var yesterday = Instant.now().minus(1, ChronoUnit.DAYS);

        var notifyNotification = NotifyNotificationTestUtil.builder()
            .withStatus(notifyNotificationStatus)
            .withSentAt(yesterday)
            .build();

        given(govukNotifyNotificationService.getNotification(sentToNotifyNotification))
            .willReturn(Response.successfulResponse(notifyNotification));

        notificationStatusUpdateService.updateNotificationStatuses();

        then(notificationRepository)
            .should()
            .save(notificationCaptor.capture());

        var savedNotification = notificationCaptor.getValue();

        assertThat(savedNotification)
            .extracting(
                Notification::getStatus,
                Notification::getNotifyStatus,
                Notification::getNotifyStatusLastUpdatedAt,
                Notification::getSentAt
            )
            .containsExactly(
                NotificationStatus.SENT,
                notifyNotificationStatus.getStatus(),
                FIXED_INSTANT,
                yesterday
            );
      }
    }

    @DisplayName("AND notify returns a status indicating notification is sending")
    @Nested
    class WhenSendingNotifyStatus {

      @DisplayName("THEN notification status is SENT_TO_NOTIFY")
      @ParameterizedTest(name = "WHEN notify status is {0}")
      @EnumSource(
          value = GovukNotifyNotificationStatus.class, mode =
          EnumSource.Mode.INCLUDE,
          names = {"CREATED", "PENDING", "SENDING"}
      )
      void whenSentNotifyStatus_thenStatusIsSent(GovukNotifyNotificationStatus notifyNotificationStatus) {

        givenDatabaseReturnsNotification(sentToNotifyNotification);

        var notifyNotification = NotifyNotificationTestUtil.builder()
            .withStatus(notifyNotificationStatus)
            .withSentAt(null)
            .build();

        given(govukNotifyNotificationService.getNotification(sentToNotifyNotification))
            .willReturn(Response.successfulResponse(notifyNotification));

        notificationStatusUpdateService.updateNotificationStatuses();

        then(notificationRepository)
            .should()
            .save(notificationCaptor.capture());

        var savedNotification = notificationCaptor.getValue();

        assertThat(savedNotification)
            .extracting(
                Notification::getStatus,
                Notification::getNotifyStatus,
                Notification::getNotifyStatusLastUpdatedAt,
                Notification::getSentAt
            )
            .containsExactly(
                NotificationStatus.SENT_TO_NOTIFY,
                notifyNotificationStatus.getStatus(),
                FIXED_INSTANT,
                null // notification has not been sent
            );
      }
    }
  }

  @DisplayName("WHEN no custom bulk retrieval value provided")
  @Nested
  class WhenNoCustomBulkRetrievalSet {

    @BeforeAll
    static void setup() {

      libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
          .withNotificationRetrievalLimit(null)
          .build();

      notificationStatusUpdateService = new NotificationStatusUpdateService(
          transactionManager,
          notificationRepository,
          libraryConfigurationProperties,
          govukNotifyNotificationService,
          FIXED_CLOCK,
          notificationRetryScheduleService
      );
    }

    @DisplayName("THEN the default library value will be used instead")
    @Test
    void whenNoBulkRetrievalPropertySet_thenVerifyDefaultUsed() {

      notificationStatusUpdateService.updateNotificationStatuses();

      then(notificationRepository)
          .should()
          .findNotificationsByStatuses(
              Set.of(NotificationStatus.SENT_TO_NOTIFY, NotificationStatus.FAILED_TO_SEND_TO_NOTIFY),
              PageRequest.of(0, NotificationLibraryConfigurationProperties.DEFAULT_BULK_RETRIEVAL_LIMIT)
          );
    }

    @DisplayName("AND no other custom notification properties are set")
    @Nested
    class WhenNoCustomNotificationPropertiesSet {

      @BeforeEach
      void setup() {

        libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
            .withNotificationProperties(null)
            .build();

        notificationRepository = mock(NotificationLibraryNotificationRepository.class);

        notificationStatusUpdateService = new NotificationStatusUpdateService(
            transactionManager,
            notificationRepository,
            libraryConfigurationProperties,
            govukNotifyNotificationService,
            FIXED_CLOCK,
            notificationRetryScheduleService
        );
      }

      @DisplayName("THEN the default library value will be used instead")
      @Test
      void whenNoNotificationPropertySet_thenVerifyDefaultUsed() {

        notificationStatusUpdateService.updateNotificationStatuses();

        then(notificationRepository)
            .should()
            .findNotificationsByStatuses(
                Set.of(NotificationStatus.SENT_TO_NOTIFY, NotificationStatus.FAILED_TO_SEND_TO_NOTIFY),
                PageRequest.of(0, NotificationLibraryConfigurationProperties.DEFAULT_BULK_RETRIEVAL_LIMIT)
            );
      }
    }
  }

  @DisplayName("WHEN custom bulk retrieval value provided")
  @Nested
  class WhenCustomBulkRetrievalSet {

    @BeforeEach
    void setup() {

      libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
          .withNotificationRetrievalLimit(BULK_RETRIEVAL_LIMIT)
          .build();

      notificationRepository = mock(NotificationLibraryNotificationRepository.class);

      notificationStatusUpdateService = new NotificationStatusUpdateService(
          transactionManager,
          notificationRepository,
          libraryConfigurationProperties,
          govukNotifyNotificationService,
          FIXED_CLOCK,
          notificationRetryScheduleService
      );
    }

    @DisplayName("THEN custom bulk retrieval value used")
    @Test
    void whenBulkRetrievalPropertySet_thenVerifyDefaultUsed() {

      notificationStatusUpdateService.updateNotificationStatuses();

      then(notificationRepository)
          .should()
          .findNotificationsByStatuses(
              Set.of(NotificationStatus.SENT_TO_NOTIFY, NotificationStatus.FAILED_TO_SEND_TO_NOTIFY),
              PageRequest.of(0, BULK_RETRIEVAL_LIMIT)
          );
    }
  }

  private void givenDatabaseReturnsNotifications(List<Notification> notifications) {
    given(notificationRepository.findNotificationsByStatuses(
        Set.of(NotificationStatus.SENT_TO_NOTIFY, NotificationStatus.FAILED_TO_SEND_TO_NOTIFY),
        PageRequest.of(0, BULK_RETRIEVAL_LIMIT)
    ))
        .willReturn(notifications);
  }

  private void givenDatabaseReturnsNotification(Notification notification) {
    givenDatabaseReturnsNotifications(List.of(notification));
  }

  private void givenDatabaseReturnsNoNotifications() {
    givenDatabaseReturnsNotifications(Collections.emptyList());
  }
}
