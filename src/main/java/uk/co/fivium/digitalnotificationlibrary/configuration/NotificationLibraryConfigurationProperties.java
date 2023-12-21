package uk.co.fivium.digitalnotificationlibrary.configuration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("digital-notification-library")
public record NotificationLibraryConfigurationProperties(@NotNull GovUkNotify govukNotify,
                                                         Notification notification) {

  public record GovUkNotify(@NotEmpty String apiKey) {
  }

  public record Notification(Queued queued) {

    public record Queued(Integer pollTimeSeconds, Integer bulkRetrievalLimit) {
    }
  }
}
