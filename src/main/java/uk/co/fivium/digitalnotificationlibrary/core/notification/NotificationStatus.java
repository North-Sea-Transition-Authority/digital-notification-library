package uk.co.fivium.digitalnotificationlibrary.core.notification;

enum NotificationStatus {
  QUEUED,
  SENT_TO_NOTIFY,
  SENT,
  RETRY,
  FAILED_TO_SEND_TO_NOTIFY,
  FAILED_NOT_SENT,
  UNEXPECTED_NOTIFY_STATUS

}
