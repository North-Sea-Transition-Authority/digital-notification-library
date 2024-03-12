package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.time.Instant;
import java.util.UUID;
import org.json.JSONObject;
import uk.gov.service.notify.Notification;

class NotifyNotificationTestUtil {

  private NotifyNotificationTestUtil() {
    throw new IllegalStateException("This is a utility class and cannot be instantiated");
  }

  static Builder builder() {
    return new Builder();
  }

  static class Builder {

    private String status = GovukNotifyNotificationStatus.SENT.getStatus();

    private Instant sentAt = Instant.now();

    private Builder() {}

    Builder withStatus(String status) {
      this.status = status;
      return this;
    }

    Builder withStatus(GovukNotifyNotificationStatus status) {
      return withStatus(status.getStatus());
    }

    Builder withSentAt(Instant sentAt) {
      this.sentAt = sentAt;
      return this;
    }

    Notification build() {

      var notificationJson = new JSONObject();
      notificationJson.put("id", UUID.randomUUID().toString());
      notificationJson.put("status", status);
      notificationJson.put("type", "email");

      var templateJson = new JSONObject();
      templateJson.put("id", UUID.randomUUID().toString());
      templateJson.put("version", "1");
      templateJson.put("uri", "template-uri");

      notificationJson.put("template", templateJson);
      notificationJson.put("body", "body");
      notificationJson.put("created_at", "2023-12-03T10:15:30+01:00");

      if (sentAt != null) {
        notificationJson.put("sent_at", sentAt.toString());
      }

      return new Notification(notificationJson);
    }

  }
}