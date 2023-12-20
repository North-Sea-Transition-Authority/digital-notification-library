package uk.co.fivium.digitalnotificationlibrary.core.notification;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification_library_notifications")
@Audited
class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Enumerated(EnumType.STRING)
  private NotificationType type;

  @Enumerated(EnumType.STRING)
  private NotificationStatus status;

  private String notifyTemplateId;

  private String notifyStatus;

  private String recipient;

  @JdbcTypeCode(SqlTypes.JSON)
  private Set<MailMergeField> mailMergeFields;

  private String domainReferenceId;

  private String domainReferenceType;

  private String logCorrelationId;

  private Instant requestedOn;

  UUID getId() {
    return id;
  }

  NotificationType getType() {
    return type;
  }

  void setType(NotificationType type) {
    this.type = type;
  }

  NotificationStatus getStatus() {
    return status;
  }

  void setStatus(NotificationStatus status) {
    this.status = status;
  }

  String getNotifyTemplateId() {
    return notifyTemplateId;
  }

  void setNotifyTemplateId(String notifyTemplateId) {
    this.notifyTemplateId = notifyTemplateId;
  }

  String getNotifyStatus() {
    return notifyStatus;
  }

  void setNotifyStatus(String notifyStatus) {
    this.notifyStatus = notifyStatus;
  }

  String getRecipient() {
    return recipient;
  }

  void setRecipient(String recipient) {
    this.recipient = recipient;
  }

  Set<MailMergeField> getMailMergeFields() {
    return mailMergeFields;
  }

  void setMailMergeFields(Set<MailMergeField> mailMergeFields) {
    this.mailMergeFields = mailMergeFields;
  }

  String getDomainReferenceId() {
    return domainReferenceId;
  }

  void setDomainReferenceId(String domainReferenceId) {
    this.domainReferenceId = domainReferenceId;
  }

  String getDomainReferenceType() {
    return domainReferenceType;
  }

  void setDomainReferenceType(String domainReferenceType) {
    this.domainReferenceType = domainReferenceType;
  }

  String getLogCorrelationId() {
    return logCorrelationId;
  }

  void setLogCorrelationId(String logCorrelationId) {
    this.logCorrelationId = logCorrelationId;
  }

  Instant getRequestedOn() {
    return requestedOn;
  }

  void setRequestedOn(Instant requestedOn) {
    this.requestedOn = requestedOn;
  }

  @Override
  public String toString() {
    return "Notification{" +
        "id=" + id +
        ", type=" + type +
        ", status=" + status +
        ", notifyTemplateId='" + notifyTemplateId + '\'' +
        ", notifyStatus='" + notifyStatus + '\'' +
        ", recipient='" + recipient + '\'' +
        ", mailMergeFields=" + mailMergeFields +
        ", domainReferenceId='" + domainReferenceId + '\'' +
        ", domainReferenceType='" + domainReferenceType + '\'' +
        ", logCorrelationId='" + logCorrelationId + '\'' +
        ", requestedOn=" + requestedOn +
        '}';
  }
}