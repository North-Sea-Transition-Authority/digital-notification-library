package uk.co.fivium.digitalnotificationlibrary.configuration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("digital-notification-library")
record NotificationLibraryConfigurationProperties(@NotNull GovUkNotify govukNotify) {

  record GovUkNotify(@NotEmpty String apiKey, String baseUrl) {
  }
}
