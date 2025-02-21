package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.EnumSet;

enum NotificationStatus {
  QUEUED,
  SENT_TO_NOTIFY,
  SENT,
  RETRY,
  FAILED_TO_SEND_TO_NOTIFY,
  FAILED_NOT_SENT,
  UNEXPECTED_NOTIFY_STATUS;

  static EnumSet<NotificationStatus> getAllowedStatusesForSendingANotification() {
    return EnumSet.of(QUEUED, RETRY);
  }
}
