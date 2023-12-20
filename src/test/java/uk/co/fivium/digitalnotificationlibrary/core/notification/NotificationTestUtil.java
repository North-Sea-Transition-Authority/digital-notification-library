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

    private UUID id = UUID.randomUUID();

    private NotificationType type = NotificationType.EMAIL;

    private NotificationStatus status = NotificationStatus.QUEUED;

    private String notifyTemplateId = "notify-template-id";

    private String notifyNotificationId = "notify-notification-id";

    private String notifyStatus = "notify-status";

    private String recipient = "recipient";

    private final Set<MailMergeField> mailMergeFields = new HashSet<>();

    private String domainReferenceId = "domain-reference-id";

    private String domainReferenceType = "domain-reference-type";

    private String logCorrelationId = "log-correlation-id";

    private Instant requestedOn = Instant.now();

    private String failureReason;

    private Builder() {
    }

    Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    Builder withType(NotificationType type) {
      this.type = type;
      return this;
    }

    Builder withStatus(NotificationStatus status) {
      this.status = status;
      return this;
    }

    Builder withNotifyTemplateId(String providerTemplateId) {
      this.notifyTemplateId = providerTemplateId;
      return this;
    }

    Builder withNotifyStatus(String providerStatus) {
      this.notifyStatus = providerStatus;
      return this;
    }

    Builder withNotifyNotificationId(String notifyNotificationId) {
      this.notifyNotificationId = notifyNotificationId;
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

    Builder withDomainReferenceId(String domainReferenceId) {
      this.domainReferenceId = domainReferenceId;
      return this;
    }

    Builder withDomainReferenceType(String domainReferenceType) {
      this.domainReferenceType = domainReferenceType;
      return this;
    }

    Builder withLogCorrelationId(String logCorrelationId) {
      this.logCorrelationId = logCorrelationId;
      return this;
    }

    Builder withRequestedOn(Instant requestedOn) {
      this.requestedOn = requestedOn;
      return this;
    }

    Builder withFailureReason(String failureReason) {
      this.failureReason = failureReason;
      return this;
    }

    Notification build() {
      var notification = new Notification(id);
      notification.setType(type);
      notification.setStatus(status);
      notification.setNotifyTemplateId(notifyTemplateId);
      notification.setNotifyNotificationId(notifyNotificationId);
      notification.setNotifyStatus(notifyStatus);
      notification.setRecipient(recipient);
      notification.setMailMergeFields(mailMergeFields);
      notification.setDomainReferenceId(domainReferenceId);
      notification.setDomainReferenceType(domainReferenceType);
      notification.setLogCorrelationId(logCorrelationId);
      notification.setRequestedOn(requestedOn);
      notification.setFailureReason(failureReason);
      return notification;
    }
  }
}
