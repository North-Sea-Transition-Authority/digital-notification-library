package uk.co.fivium.digitalnotificationlibrary.configuration;

import java.util.HashSet;
import java.util.Set;

public class NotificationLibraryConfigurationPropertiesTestUtil {

  private NotificationLibraryConfigurationPropertiesTestUtil() {
    throw new IllegalStateException("Cannot instantiate a utility class");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private boolean hasSetQueuedPropertyObject = false;

    private final Integer queuedNotificationPollTimeSeconds = 10;

    private Integer queuedNotificationBulkRetrievalLimit = 5;

    private NotificationLibraryConfigurationProperties.Notification.Queued queuedNotificationProperties =
        new NotificationLibraryConfigurationProperties.Notification.Queued(
            queuedNotificationPollTimeSeconds,
            queuedNotificationBulkRetrievalLimit
        );

    private boolean hasSetNotificationPropertyObject = false;

    private NotificationLibraryConfigurationProperties.Notification notificationProperties =
        new NotificationLibraryConfigurationProperties.Notification(queuedNotificationProperties);

    private NotificationMode notificationMode = NotificationMode.TEST;

    private Set<String> testEmailRecipients = new HashSet<>();

    private Set<String> testSmsRecipients = new HashSet<>();

    private Builder() {
    }

    public Builder withQueuedNotificationProperties(
        NotificationLibraryConfigurationProperties.Notification.Queued queuedNotificationProperties
    ) {
      this.queuedNotificationProperties = queuedNotificationProperties;
      this.hasSetQueuedPropertyObject = true;
      return this;
    }

    public Builder withNotificationProperties(NotificationLibraryConfigurationProperties.Notification notificationProperties) {
      this.notificationProperties = notificationProperties;
      this.hasSetNotificationPropertyObject = true;
      return this;
    }

    public Builder withQueuedNotificationRetrievalLimit(Integer queuedNotificationBulkRetrievalLimit) {
      this.queuedNotificationBulkRetrievalLimit = queuedNotificationBulkRetrievalLimit;
      return this;
    }

    public Builder withMode(NotificationMode notificationMode) {
      this.notificationMode = notificationMode;
      return this;
    }

    public Builder withTestEmailRecipient(String emailRecipient) {
      this.testEmailRecipients.add(emailRecipient);
      return this;
    }

    public Builder withTestEmailRecipients(Set<String> emailRecipients) {
      this.testEmailRecipients = emailRecipients;
      return this;
    }

    public Builder withTestSmsRecipient(String smsRecipient) {
      this.testSmsRecipients.add(smsRecipient);
      return this;
    }

    public Builder withTestSmsRecipients(Set<String> smsRecipients) {
      this.testSmsRecipients = smsRecipients;
      return this;
    }

    public NotificationLibraryConfigurationProperties build() {

      var queuedObjectProperties = hasSetQueuedPropertyObject
          ? queuedNotificationProperties
          : new NotificationLibraryConfigurationProperties.Notification.Queued(
              queuedNotificationPollTimeSeconds,
          queuedNotificationBulkRetrievalLimit
          );

      var notificationObjectProperties = hasSetNotificationPropertyObject
          ? notificationProperties
          : new NotificationLibraryConfigurationProperties.Notification(queuedObjectProperties);

      var testMode = new NotificationLibraryConfigurationProperties.TestMode(testEmailRecipients, testSmsRecipients);

      return new NotificationLibraryConfigurationProperties(
          new NotificationLibraryConfigurationProperties.GovukNotify("govuk-notify-api-key"),
          notificationObjectProperties,
          notificationMode,
          testMode
      );
    }
  }
}

