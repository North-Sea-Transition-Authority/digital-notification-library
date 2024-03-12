package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;

@Service
class NotificationRetryScheduleService {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationRetryScheduleService.class);

  private final Clock clock;

  private final NotificationLibraryConfigurationProperties libraryConfigurationProperties;

  @Autowired
  NotificationRetryScheduleService(Clock clock,
                                   NotificationLibraryConfigurationProperties libraryConfigurationProperties) {
    this.clock = clock;
    this.libraryConfigurationProperties = libraryConfigurationProperties;
  }

  boolean hasReachedMaxRetryTime(Notification notification) {
    var threeDaysAfterSendRequest = notification.getRequestedOn().plus(72, ChronoUnit.HOURS);
    return isCurrentTimeOnOrAfter(threeDaysAfterSendRequest);
  }

  /**
   * Each time we retry the notification we wait twice as long since the last send attempt. If we have never retried
   * before, the first retry will be 10 seconds after last send attempt, the second retry will be 20 seconds after last
   * send attempt etc. Each retry following will be double the delay of the last sent attempt.
   * @param notification The notification to check if we have reached the next retry time
   * @return true if the next retry time has been reached, false otherwise
   */
  boolean hasReachedNextRetryTime(Notification notification) {

    if (notification.getLastSendAttemptAt() == null) {
      LOGGER.warn("""
            A call was made to check if the next retry time has been reached for notification %s but the
            notification has never previously made a send attempt.
          """.formatted(notification.getId())
      );
      return false;
    }

    Duration nextRetryOffset = getNextRetryOffsetDuration(notification);

    Instant nextRetryTime = notification.getLastSendAttemptAt().plus(nextRetryOffset);

    return isCurrentTimeOnOrAfter(nextRetryTime);
  }

  Duration getNextRetryOffsetDuration(Notification notification) {

    var pollTimeSeconds = libraryConfigurationProperties.notification().pollTimeSeconds();
    int retryCount = Optional.ofNullable(notification.getRetryCount()).orElse(0);

    // next retry offset is twice the previous. This works out as (x * 2^(n-1)) where x is the poll time and n is
    // the retry count. Check max of 0 or (retryCount - 1) to avoid 2^-1 = 0.5.
    return Duration.ofSeconds(pollTimeSeconds * (long) Math.pow(2, Math.max(0, retryCount - 1)));
  }

  private boolean isCurrentTimeOnOrAfter(Instant timeToCheckAgainst) {
    var currentTime = clock.instant();
    return currentTime.equals(timeToCheckAgainst) || currentTime.isAfter(timeToCheckAgainst);
  }
}
