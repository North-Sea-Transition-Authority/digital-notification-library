package uk.co.fivium.digitalnotificationlibrary.core.notification;

enum NotificationStatus {
  QUEUED,
  SENT_TO_NOTIFY,
  TEMPORARY_FAILURE,
  RETRY,
  FAILED_NOT_SENT
}
