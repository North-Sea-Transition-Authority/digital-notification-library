package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

@Service
class NotificationSendingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSendingService.class);

  private static final String NOTIFICATION_FAILURE_REASON_MESSAGE_FORMAT = "%s - GOV.UK Notify exception %s";

  private final TransactionTemplate transactionTemplate;

  private final NotificationLibraryNotificationRepository notificationRepository;

  private final GovukNotifySender govukNotifySender;

  private final NotificationLibraryConfigurationProperties libraryConfigurationProperties;

  private final Clock clock;

  private final NotificationLibraryEmailAttachmentResolver emailAttachmentResolver;

  @Autowired
  NotificationSendingService(PlatformTransactionManager transactionManager,
                             NotificationLibraryNotificationRepository notificationRepository,
                             GovukNotifySender govukNotifySender,
                             NotificationLibraryConfigurationProperties libraryConfigurationProperties,
                             Clock clock,
                             NotificationLibraryEmailAttachmentResolver emailAttachmentResolver) {
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.notificationRepository = notificationRepository;
    this.govukNotifySender = govukNotifySender;
    this.libraryConfigurationProperties = libraryConfigurationProperties;
    this.clock = clock;
    this.emailAttachmentResolver = emailAttachmentResolver;
  }

  void sendNotificationsToNotify() {

    LOGGER.debug(
        "Polling notifications with statuses [{}, {}] to send to notify",
        NotificationStatus.QUEUED, NotificationStatus.RETRY
    );

    var bulkRetrievalLimit = libraryConfigurationProperties.getBulkRetrievalLimit();

    List<Notification> notificationsToSend = notificationRepository.findNotificationsByStatuses(
        Set.of(NotificationStatus.QUEUED, NotificationStatus.RETRY),
        PageRequest.of(0, bulkRetrievalLimit)
    );

    notificationsToSend.forEach(notificationToSend ->
        transactionTemplate.executeWithoutResult(status -> {
          var notification = addFileAttachmentsAsMailMergeFields(notificationToSend);
          if (Set.of(NotificationStatus.QUEUED, NotificationStatus.RETRY).contains(notification.getStatus())) {
            sendNotification(notification);
          }
          notificationRepository.save(notification);
        })
    );
  }

  private Notification addFileAttachmentsAsMailMergeFields(Notification notification) {
    if (CollectionUtils.isNotEmpty(notification.getFileAttachments())
        && NotificationStatus.QUEUED.equals(notification.getStatus())) {

      for (FileAttachment fileAttachment : notification.getFileAttachments()) {
        byte[] fileContents;
        try {
          fileContents = emailAttachmentResolver.resolveFileAttachment(fileAttachment.fileId());
          var mailMergeFields = notification.getMailMergeFields();
          var fileMailMergeField = new MailMergeField(
              fileAttachment.key(),
              NotificationClient.prepareUpload(fileContents, fileAttachment.fileName())
          );
          mailMergeFields.add(fileMailMergeField);

        } catch (NotificationClientException e) {
          handleFileErrorResponse(notification, new Response.ErrorResponse(e.getHttpResult(), e.getMessage()));
        }
      }
    }
    return notification;
  }

  private void sendNotification(Notification notification) {

    if (NotificationStatus.RETRY.equals(notification.getStatus())) {
      notification.setRetryCount(notification.getRetryCount() + 1);
    }

    notification.setLastSendAttemptAt(clock.instant());

    switch (notification.getType()) {
      case EMAIL -> {

        Response<SendEmailResponse> response = govukNotifySender.sendEmail(notification);

        if (response.isSuccessfulResponse()) {
          setPropertiesForSentToGovukNotify(notification, response.successResponseObject().getNotificationId());
        } else {
          handleErrorResponse(notification, response.error());
        }
      }
      case SMS -> {

        Response<SendSmsResponse> response = govukNotifySender.sendSms(notification);

        if (response.isSuccessfulResponse()) {
          setPropertiesForSentToGovukNotify(notification, response.successResponseObject().getNotificationId());
        } else {
          handleErrorResponse(notification, response.error());
        }
      }
    }
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

      notificationStatus = NotificationStatus.FAILED_TO_SEND_TO_NOTIFY;
      failureReason = NOTIFICATION_FAILURE_REASON_MESSAGE_FORMAT.formatted(errorMessage, response.message());
    }

    notification.setStatus(notificationStatus);
    notification.setFailureReason(failureReason);
    notification.setNotifyNotificationId(null);
    notification.setLastFailedAt(clock.instant());
  }

  private void handleFileErrorResponse(Notification notification, Response.ErrorResponse response) {

    NotificationStatus notificationStatus;
    String failureReason;

    if (isBadRequestResponse(response)) {

      notificationStatus = NotificationStatus.FAILED_NOT_SENT;

      var errorMessage = ("Failed with 400 response from GOV.UK Notify when preparing to upload a file attachment " +
          "with ID %s to notify. Library will not retrying sending.")
          .formatted(notification.getId());

      LOGGER.error(errorMessage);

      failureReason = NOTIFICATION_FAILURE_REASON_MESSAGE_FORMAT.formatted(errorMessage, response.message());

    } else if (isRequestTooLong(response)) {

      notificationStatus = NotificationStatus.FAILED_NOT_SENT;

      var errorMessage = ("Failed with 413 response from GOV.UK Notify when preparing to upload a file attachment with ID %s" +
          " to notify. Library will not retrying sending.").formatted(notification.getId());

      LOGGER.error(errorMessage);

      failureReason = NOTIFICATION_FAILURE_REASON_MESSAGE_FORMAT.formatted(errorMessage, response.message());

    } else {

      var errorMessage = ("Failed with %s response from GOV.UK Notify when preparing to upload a file attachment with ID %s " +
          "to notify. Library will retrying sending.").formatted(response.httpStatus(), notification.getId());

      LOGGER.info(errorMessage);

      notificationStatus = NotificationStatus.FAILED_TO_SEND_TO_NOTIFY;
      failureReason = NOTIFICATION_FAILURE_REASON_MESSAGE_FORMAT.formatted(errorMessage, response.message());
    }

    notification.setStatus(notificationStatus);
    notification.setFailureReason(failureReason);
    notification.setNotifyNotificationId(null);
    notification.setLastFailedAt(clock.instant());
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

  private boolean isRequestTooLong(Response.ErrorResponse response) {
    return response.httpStatus() == HttpStatus.SC_REQUEST_TOO_LONG;
  }
}