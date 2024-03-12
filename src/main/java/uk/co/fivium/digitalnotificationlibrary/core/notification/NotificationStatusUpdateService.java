package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;

@Service
class NotificationStatusUpdateService {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationStatusUpdateService.class);

  private final NotificationLibraryNotificationRepository notificationRepository;

  private final NotificationLibraryConfigurationProperties libraryConfigurationProperties;

  private final TransactionTemplate transactionTemplate;

  private final GovukNotifyNotificationService govukNotifyNotificationService;

  private final Clock clock;

  private final NotificationRetryScheduleService notificationRetryScheduleService;

  @Autowired
  NotificationStatusUpdateService(PlatformTransactionManager transactionManager,
                                  NotificationLibraryNotificationRepository notificationRepository,
                                  NotificationLibraryConfigurationProperties libraryConfigurationProperties,
                                  GovukNotifyNotificationService govukNotifyNotificationService,
                                  Clock clock,
                                  NotificationRetryScheduleService notificationRetryScheduleService) {
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.notificationRepository = notificationRepository;
    this.libraryConfigurationProperties = libraryConfigurationProperties;
    this.govukNotifyNotificationService = govukNotifyNotificationService;
    this.clock = clock;
    this.notificationRetryScheduleService = notificationRetryScheduleService;
  }

  void updateNotificationStatuses() {

    var bulkRetrievalLimit = libraryConfigurationProperties.getBulkRetrievalLimit();

    List<Notification> notificationsToUpdate = notificationRepository.findNotificationsByStatuses(
        Set.of(NotificationStatus.SENT_TO_NOTIFY, NotificationStatus.FAILED_TO_SEND_TO_NOTIFY),
        PageRequest.of(0, bulkRetrievalLimit)
    );

    notificationsToUpdate.forEach(notification ->
        transactionTemplate.executeWithoutResult(status -> {
          if (NotificationStatus.SENT_TO_NOTIFY.equals(notification.getStatus())) {
            refreshNotificationStatus(notification);
          } else if (NotificationStatus.FAILED_TO_SEND_TO_NOTIFY.equals(notification.getStatus())) {
            setRetryState(notification);
          }

          notificationRepository.save(notification);
        })
    );
  }

  private void refreshNotificationStatus(Notification notification) {

    // Potential performance improvement here to get notifications from notify in bulk. With the current API
    // client there isn't an easy way to do that. Happy with n request for our use case.
    Response<uk.gov.service.notify.Notification> notifyNotificationResponse =
        govukNotifyNotificationService.getNotification(notification);

    if (notifyNotificationResponse.isErrorResponse()) {
      var errorResponse = notifyNotificationResponse.error();

      notification.setFailureReason(
          "Failed to update notification status due to %s status from GOV.UK Notify. GOV.UK Notify exception: %s"
              .formatted(errorResponse.httpStatus(), errorResponse.message())
      );

      notification.setLastFailedAt(clock.instant());

      return;
    }

    var notifyNotification = notifyNotificationResponse.successResponseObject();

    notification.setNotifyStatus(notifyNotification.getStatus());
    notification.setNotifyStatusLastUpdatedAt(clock.instant());
    notification.setFailureReason(null);
    notification.setLastFailedAt(null);

    Optional<GovukNotifyNotificationStatus> notifyNotificationStatus =
        GovukNotifyNotificationStatus.fromNotifyStatus(notifyNotification.getStatus());

    if (notifyNotificationStatus.isEmpty()) {

      LOGGER.error(
          "Notify notification with ID {} returned status {} which is not known to the library",
          notification.getNotifyNotificationId(),
          notifyNotificationStatus
      );

      notification.setStatus(NotificationStatus.UNEXPECTED_NOTIFY_STATUS);

    } else {
      switch (notifyNotificationStatus.get()) {
        case PERMANENT_FAILURE ->
            setAsFailedNotSent(notification, "GOV.UK notify returned permanent failure response.");
        case TEMPORARY_FAILURE, TECHNICAL_FAILURE -> setRetryState(notification);
        case SENT, DELIVERED -> {
          notification.setStatus(NotificationStatus.SENT);
          notifyNotification.getSentAt().ifPresent(sentAt ->
              notification.setSentAt(Instant.from(sentAt))
          );
        }
        case CREATED, PENDING, SENDING -> notification.setStatus(NotificationStatus.SENT_TO_NOTIFY);
      }
    }
  }

  private void setAsFailedNotSent(Notification notification, String failureReason) {
    notification.setStatus(NotificationStatus.FAILED_NOT_SENT);
    notification.setFailureReason(failureReason);
    notification.setLastFailedAt(clock.instant());
    LOGGER.warn("Notification with ID {} not sent and will not retry due to {}", notification.getId(), failureReason);
  }

  private void setRetryState(Notification notification) {
    notification.setLastFailedAt(clock.instant());
    if (notificationRetryScheduleService.hasReachedMaxRetryTime(notification)) {
      setAsFailedNotSent(notification, "Maximum retry time since requested on date exceeded");
    } else if (notificationRetryScheduleService.hasReachedNextRetryTime(notification)) {
      notification.setStatus(NotificationStatus.RETRY);
    }
  }
}
