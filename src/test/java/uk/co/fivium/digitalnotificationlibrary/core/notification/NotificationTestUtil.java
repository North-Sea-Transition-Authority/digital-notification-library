package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

class NotificationTestUtil {

  private NotificationTestUtil() {
    throw new IllegalStateException("This is a utility class and should not be instantiated");
  }

  static Builder builder() {
    return new Builder();
  }

  static class Builder {

    private final UUID id = UUID.randomUUID();

    private NotificationType type = NotificationType.EMAIL;

    private NotificationStatus status = NotificationStatus.QUEUED;

    private String recipient = "recipient";

    private final Set<MailMergeField> mailMergeFields = new HashSet<>();

    private Instant requestedOn = Instant.now();

    private Instant lastSendAttemptAt = Instant.now();

    private Integer retryCount = null;

    private Instant lastFailedAt = null;

    private String notifyNotificationId;

    private Builder() {
    }

    Builder withType(NotificationType type) {
      this.type = type;
      return this;
    }

    Builder withStatus(NotificationStatus status) {
      this.status = status;
      return this;
    }

    Builder withRecipient(String recipient) {
      this.recipient = recipient;
      return this;
    }

    Builder withMailMergeField(String name, Object value) {
      this.mailMergeFields.add(new MailMergeField(name, value));
      return this;
    }

    Builder withRequestedOn(Instant requestedOn) {
      this.requestedOn = requestedOn;
      return this;
    }

    Builder withLastSendAttemptAt(Instant lastSendAttemptAt) {
      this.lastSendAttemptAt = lastSendAttemptAt;
      return this;
    }

    Builder withRetryCount(Integer retryCount) {
      this.retryCount = retryCount;
      return this;
    }

    Builder withLastFailedAt(Instant lastFailedAt) {
      this.lastFailedAt = lastFailedAt;
      return this;
    }

    Builder withNotifyNotificationId(String notifyNotificationId) {
      this.notifyNotificationId = notifyNotificationId;
      return this;
    }

    Notification build() {
      var notification = new Notification(id);
      notification.setType(type);
      notification.setStatus(status);
      notification.setNotifyTemplateId("notify-template-id");
      notification.setNotifyNotificationId(notifyNotificationId);
      notification.setNotifyStatus("notify-status");
      notification.setRecipient(recipient);
      notification.setMailMergeFields(mailMergeFields);
      notification.setDomainReferenceId("domain-reference-id");
      notification.setDomainReferenceType("domain-reference-type");
      notification.setLogCorrelationId("log-correlation-id");
      notification.setRequestedOn(requestedOn);
      notification.setRetryCount(retryCount);
      notification.setLastSendAttemptAt(lastSendAttemptAt);
      notification.setLastFailedAt(lastFailedAt);
      return notification;
    }
  }
}
