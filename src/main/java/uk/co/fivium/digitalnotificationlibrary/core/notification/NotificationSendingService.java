package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.javacrumbs.shedlock.core.LockAssert;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

@Service
class NotificationSendingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSendingService.class);

  private static final String NOTIFICATION_FAILURE_REASON_MESSAGE_FORMAT = "%s - GOV.UK Notify exception %s";

  static final int DEFAULT_BULK_RETRIEVAL_LIMIT = 100;

  private final TransactionTemplate transactionTemplate;

  private final NotificationLibraryNotificationRepository notificationRepository;

  private final GovukNotifyService govukNotifyService;

  private final NotificationLibraryConfigurationProperties libraryConfigurationProperties;

  @Autowired
  NotificationSendingService(PlatformTransactionManager transactionManager,
                             NotificationLibraryNotificationRepository notificationRepository,
                             GovukNotifyService govukNotifyService,
                             NotificationLibraryConfigurationProperties libraryConfigurationProperties) {
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.notificationRepository = notificationRepository;
    this.govukNotifyService = govukNotifyService;
    this.libraryConfigurationProperties = libraryConfigurationProperties;
  }

  @ScheduledTask(
      fixedDelayString = "${digital-notification-library.notification.queued.poll-time-seconds:10}",
      timeUnit = TimeUnit.SECONDS,
      lockName = "NotificationSendingService_sendQueuedNotificationToNotify"
  )
  void sendQueuedNotificationToNotify() {

    LockAssert.assertLocked();

    LOGGER.debug("Polling notifications with status {} to send to notify", NotificationStatus.QUEUED);

    var bulkRetrievalLimit = getBulkRetrievalLimit();

    List<Notification> queuedNotifications = notificationRepository.findNotificationByStatusOrderByRequestedOnAsc(
        NotificationStatus.QUEUED,
        PageRequest.of(0, bulkRetrievalLimit)
    );

    queuedNotifications.forEach(queuedNotification ->
        transactionTemplate.executeWithoutResult(status -> {
          var notification = sendNotification(queuedNotification);
          notificationRepository.save(notification);
        })
    );
  }

  private Notification sendNotification(Notification notification) {

    switch (notification.getType()) {
      case EMAIL -> {

        Response<SendEmailResponse> response = govukNotifyService.sendEmail(notification);

        if (response.isSuccessfulResponse()) {
          setPropertiesForSentToGovukNotify(notification, response.successResponseObject().getNotificationId());
        } else {
          handleErrorResponse(notification, response.error());
        }
      }
      case SMS -> {

        Response<SendSmsResponse> response = govukNotifyService.sendSms(notification);

        if (response.isSuccessfulResponse()) {
          setPropertiesForSentToGovukNotify(notification, response.successResponseObject().getNotificationId());
        } else {
          handleErrorResponse(notification, response.error());
        }
      }
    }

    return notification;
  }

  private void handleErrorResponse(Notification notification, Response.ErrorResponse response) {

    NotificationStatus notificationStatus;
    String failureReason;

    if (isForbiddenResponse(response)) {

      notificationStatus = NotificationStatus.FAILED_NOT_SENT;

      var errorMessage = ("Failed with 403 response from GOV.UK Notify when sending notification " +
          "with ID %s to notify. Library will not retrying sending.")
          .formatted(notification.getId());

      LOGGER.error(errorMessage);

      failureReason = NOTIFICATION_FAILURE_REASON_MESSAGE_FORMAT.formatted(errorMessage, response.message());

    } else if (isBadRequestResponse(response)) {

      notificationStatus = NotificationStatus.FAILED_NOT_SENT;

      var errorMessage = ("Failed with 400 response from GOV.UK Notify when sending notification " +
          "with ID %s to notify. Library will not retrying sending.")
          .formatted(notification.getId());

      LOGGER.error(errorMessage);

      failureReason = NOTIFICATION_FAILURE_REASON_MESSAGE_FORMAT.formatted(errorMessage, response.message());

    } else {

      var errorMessage = ("Failed with %s response from GOV.UK Notify when sending notification " +
          "with ID %s to notify. Library will retrying sending.")
          .formatted(response.httpStatus(), notification.getId());

      LOGGER.info(errorMessage);

      notificationStatus = NotificationStatus.TEMPORARY_FAILURE;
      failureReason = NOTIFICATION_FAILURE_REASON_MESSAGE_FORMAT.formatted(errorMessage, response.message());
    }

    notification.setStatus(notificationStatus);
    notification.setFailureReason(failureReason);
    notification.setNotifyNotificationId(null);
  }

  private void setPropertiesForSentToGovukNotify(Notification notification, UUID notifyNotificationId) {
    notification.setStatus(NotificationStatus.SENT_TO_NOTIFY);
    notification.setNotifyNotificationId(String.valueOf(notifyNotificationId));
    LOGGER.debug("Sent notification with ID {} to notify", notification.getId());
  }

  private boolean isForbiddenResponse(Response.ErrorResponse response) {
    return response.httpStatus() == HttpStatus.SC_FORBIDDEN;
  }

  private boolean isBadRequestResponse(Response.ErrorResponse response) {
    return response.httpStatus() == HttpStatus.SC_BAD_REQUEST;
  }

  private int getBulkRetrievalLimit() {

    var notificationProperties = libraryConfigurationProperties.notification();

    return notificationProperties != null && notificationProperties.queued() != null
        ? Optional.ofNullable(notificationProperties.queued().bulkRetrievalLimit()).orElse(DEFAULT_BULK_RETRIEVAL_LIMIT)
        : DEFAULT_BULK_RETRIEVAL_LIMIT;
  }
}