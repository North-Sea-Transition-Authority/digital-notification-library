package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.Arrays;
import java.util.Optional;

enum GovukNotifyNotificationStatus {
  CREATED("created"),
  SENDING("sending"),
  PENDING("pending"),
  TECHNICAL_FAILURE("technical-failure"),
  TEMPORARY_FAILURE("temporary-failure"),
  PERMANENT_FAILURE("permanent-failure"),
  DELIVERED("delivered"),
  SENT("sent");

  private final String status;

  GovukNotifyNotificationStatus(String status) {
    this.status = status;
  }

  String getStatus() {
    return status;
  }

  static Optional<GovukNotifyNotificationStatus> fromNotifyStatus(String notifyNotificationStatus) {
    return Arrays.stream(GovukNotifyNotificationStatus.values())
        .filter(notificationStatus -> notificationStatus.getStatus().equals(notifyNotificationStatus))
        .findFirst();
  }
}
