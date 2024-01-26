package uk.co.fivium.digitalnotificationlibrary.configuration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.lang.NonNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("digital-notification-library")
@ConfigurationPropertiesScan
public record NotificationLibraryConfigurationProperties(@NotNull GovukNotify govukNotify,
                                                         Notification notification,
                                                         @NotNull NotificationMode mode,
                                                         TestMode testMode) implements Validator {

  public record GovukNotify(@NotEmpty String apiKey) {
  }

  public record Notification(Queued queued) {

    public record Queued(Integer pollTimeSeconds, Integer bulkRetrievalLimit) {
    }
  }

  public record TestMode(Set<String> emailRecipients, Set<String> smsRecipients) {
  }

  boolean isTestMode() {
    return NotificationMode.TEST.equals(mode);
  }

  public boolean hasTestEmailRecipients() {
    return testMode != null && CollectionUtils.isNotEmpty(testMode.emailRecipients());
  }

  public boolean hasTestSmsRecipients() {
    return testMode != null && CollectionUtils.isNotEmpty(testMode.smsRecipients());
  }

  @Override
  public boolean supports(@NonNull Class<?> clazz) {
    return NotificationLibraryConfigurationProperties.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NonNull Object target, @NonNull Errors errors) {

    if (isTestMode() && !(hasTestEmailRecipients() && hasTestSmsRecipients())) {
      errors.reject(
          "test-mode.no-test-recipients",
          "You must set test email and sms recipients when in test mode"
      );
    }
  }
}