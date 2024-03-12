package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationPropertiesTestUtil;

@ExtendWith(MockitoExtension.class)
class NotificationRetryScheduleServiceTest {

  private static final Integer POLL_TIME_SECONDS = 10;

  private static final Instant FIXED_INSTANT = Instant.now();

  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneId.systemDefault());

  private static final NotificationLibraryConfigurationProperties CONFIGURATION_PROPERTIES =
      NotificationLibraryConfigurationPropertiesTestUtil.builder()
          .withNotificationPollTimeSeconds(POLL_TIME_SECONDS)
          .build();

  private static NotificationRetryScheduleService notificationRetryScheduleService;

  @BeforeAll
  static void setup() {
    notificationRetryScheduleService = new NotificationRetryScheduleService(FIXED_CLOCK, CONFIGURATION_PROPERTIES);
  }

  @DisplayName("GIVEN I want to know if a notification has reached its max retry time")
  @Nested
  class HasReachedMaxRetry {

    @DisplayName("WHEN the notification requested date is before 72 hours from now")
    @Nested
    class LessThanMaxRetryTime {

      @DisplayName("THEN we have not reached the maximum retry time")
      @Test
      void hasReachedMaxRetryTime_whenLessThatMaxRetryTime_thenFalse() {

        var notification = NotificationTestUtil.builder()
            .withRequestedOn(FIXED_CLOCK.instant().minus(10, ChronoUnit.HOURS))
            .build();

        assertFalse(() -> notificationRetryScheduleService.hasReachedMaxRetryTime(notification));
      }
    }

    @DisplayName("WHEN the notification requested date is after 72 hours from now")
    @Nested
    class MoreThanMaxRetryTime {

      @DisplayName("THEN we have reached the maximum retry time")
      @Test
      void hasReachedMaxRetryTime_whenMoreThanMaxRetryTime_thenTrue() {

        var notification = NotificationTestUtil.builder()
            .withRequestedOn(FIXED_CLOCK.instant().minus(100, ChronoUnit.HOURS))
            .build();

        assertTrue(() -> notificationRetryScheduleService.hasReachedMaxRetryTime(notification));
      }
    }

    @DisplayName("WHEN the notification requested date is exactly 72 hours from now")
    @Nested
    class ExactlyMaxRetryTime {

      @DisplayName("THEN we have reached the maximum retry time")
      @Test
      void hasReachedMaxRetryTime_whenMoreThanMaxRetryTime_thenTrue() {

        var notification = NotificationTestUtil.builder()
            .withRequestedOn(FIXED_CLOCK.instant().minus(72, ChronoUnit.HOURS))
            .build();

        assertTrue(() -> notificationRetryScheduleService.hasReachedMaxRetryTime(notification));
      }
    }
  }

  @DisplayName("GIVEN I want to know if a notification has reached its next retry time")
  @Nested
  class HasReachedNextRetryTime {

    @DisplayName("WHEN the last send attempt date is less than the next retry time")
    @Nested
    class LessThanNextTryTime {

      @DisplayName("THEN we have not reached the next retry time")
      @Test
      void hasReachedNextRetryTime_whenLessThanNextRetryTime_thenFalse() {

        var oneSecondAgo = FIXED_CLOCK.instant().minus(1, ChronoUnit.SECONDS);

        var notification = NotificationTestUtil.builder()
            .withRetryCount(1) // first retry is after 10 seconds
            .withLastSendAttemptAt(oneSecondAgo)
            .build();

        assertFalse(() -> notificationRetryScheduleService.hasReachedNextRetryTime(notification));
      }
    }

    @DisplayName("WHEN the last send attempt date is after the next retry time")
    @Nested
    class MoreThanNextTryTime {

      @DisplayName("THEN we have reached the next retry time")
      @Test
      void hasReachedNextRetryTime_whenLessThanNextRetryTime_thenFalse() {

        var twentySecondAgo = FIXED_CLOCK.instant().minus(20, ChronoUnit.SECONDS);

        var notification = NotificationTestUtil.builder()
            .withRetryCount(1) // first retry is after 10 seconds
            .withLastSendAttemptAt(twentySecondAgo)
            .build();

        assertTrue(() -> notificationRetryScheduleService.hasReachedNextRetryTime(notification));
      }
    }

    @DisplayName("WHEN the last send attempt date is exactly on the next retry time")
    @Nested
    class ExactlyNextTryTime {

      @DisplayName("THEN we have reached the next retry time")
      @Test
      void hasReachedNextRetryTime_whenLessThanNextRetryTime_thenFalse() {

        var tenSecondAgo = FIXED_CLOCK.instant().minus(10, ChronoUnit.SECONDS);

        var notification = NotificationTestUtil.builder()
            .withRetryCount(1) // first retry is after 10 seconds
            .withLastSendAttemptAt(tenSecondAgo)
            .build();

        assertTrue(() -> notificationRetryScheduleService.hasReachedNextRetryTime(notification));
      }
    }

    @DisplayName("WHEN the retry count is zero or null")
    @Nested
    class WhenRetryCountIsZero {

      @DisplayName("THEN notification will be treated as if it was the first retry")
      @ParameterizedTest
      @NullSource
      @ValueSource(ints = 0)
      void hasReachedNextRetryTime_whenNullOrZeroRetryCount(Integer nullOrZeroRetryCount) {

        var tenSecondAgo = FIXED_CLOCK.instant().minus(10, ChronoUnit.SECONDS);

        var notification = NotificationTestUtil.builder()
            .withRetryCount(nullOrZeroRetryCount) // first retry is after 10 seconds
            .withLastSendAttemptAt(tenSecondAgo)
            .build();

        assertTrue(() -> notificationRetryScheduleService.hasReachedNextRetryTime(notification));
      }
    }

    @DisplayName("WHEN the notification does not have a last retried date")
    @Nested
    class WhenNoLastRetriedDate {

      @DisplayName("THEN notification will be treated as having been retried just now")
      @Test
      void hasReachedNextRetryTime_whenLastSendAttemptAtIsNull_thenCurrentTimeUsed() {

        var notification = NotificationTestUtil.builder()
            .withLastSendAttemptAt(null)
            .build();

        assertFalse(() -> notificationRetryScheduleService.hasReachedNextRetryTime(notification));
      }
    }
  }

  @DisplayName("GIVEN I want to know the next retry offset for a notification")
  @ParameterizedTest(name = "WHEN retry count is {0} THEN expected offset seconds is {1}")
  @MethodSource("getRetryOffsetArguments")
  void getNextRetryOffsetDurationInSeconds(int retryCount, long offsetSeconds) {

    var notification = NotificationTestUtil.builder()
        .withRetryCount(retryCount)
        .build();

    var resultingOffset = notificationRetryScheduleService.getNextRetryOffsetDuration(notification);

    assertThat(resultingOffset.getSeconds()).isEqualTo(offsetSeconds);
  }

  private static Stream<Arguments> getRetryOffsetArguments() {
    return Stream.of(
        Arguments.of(1, 10),
        Arguments.of(2, 20),
        Arguments.of(3, 40),
        Arguments.of(4, 80),
        Arguments.of(5, 160),
        Arguments.of(6, 320),
        Arguments.of(7, 640),
        Arguments.of(8, 1280)
    );
  }
}
