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

/**
 * Configuration class for library properties.
 * @param govukNotify The configuration for GOV.UK notify which is required to be provided
 * @param notification The configuration for notification processing within the library
 * @param mode The mode the library will be running in which is required to be provided
 * @param testMode The configuration for when running in test mode
 */
@Validated
@ConfigurationProperties("digital-notification-library")
@ConfigurationPropertiesScan
public record NotificationLibraryConfigurationProperties(@NotNull GovukNotify govukNotify,
                                                         Notification notification,
                                                         @NotNull NotificationMode mode,
                                                         TestMode testMode) implements Validator {

  /**
   * The configuration for interactions between the library and GOV.UK notify.
   * @param apiKey The API key to use for GOV.UK notify
   */
  public record GovukNotify(@NotEmpty String apiKey) {
  }

  /**
   * The configuration for handing and processing notifications.
   * @param pollTimeSeconds Number of seconds between each iteration of the notification processing job
   * @param bulkRetrievalLimit For each iteration of the notification processing job, how many notifications will be
   *                           processed in that interaction.
   */
  public record Notification(Integer pollTimeSeconds, Integer bulkRetrievalLimit) {
  }

  /**
   * The configuration for when running in test mode.
   * @param emailRecipients The recipients of email notifications when the library is in test mode
   * @param smsRecipients The recipients of sms notifications when the library is in test mode
   */
  public record TestMode(Set<String> emailRecipients, Set<String> smsRecipients) {
  }

  boolean isTestMode() {
    return NotificationMode.TEST.equals(mode);
  }

  /**
   * Check if test email recipients have been provided.
   * @return true if test email recipients have been provided, false otherwise
   */
  public boolean hasTestEmailRecipients() {
    return testMode != null && CollectionUtils.isNotEmpty(testMode.emailRecipients());
  }

  /**
   * Check if test sms recipients have been provided.
   * @return true if test sms recipients have been provided, false otherwise
   */
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