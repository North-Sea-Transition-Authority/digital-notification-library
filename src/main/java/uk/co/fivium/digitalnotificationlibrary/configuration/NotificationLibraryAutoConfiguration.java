package uk.co.fivium.digitalnotificationlibrary.configuration;

import java.time.Clock;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import uk.gov.service.notify.NotificationClient;

@ComponentScan(basePackages = "uk.co.fivium.digitalnotificationlibrary")
@EnableConfigurationProperties(NotificationLibraryConfigurationProperties.class)
class NotificationLibraryAutoConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationLibraryAutoConfiguration.class);

  private static final String NOTIFY_BASE_URL = "https://api.notifications.service.gov.uk";

  NotificationLibraryAutoConfiguration() {
    LOGGER.info("Digital notification library Spring Boot Starter has been enabled");
  }

  @Bean
  NotificationClient providerNotificationClient(NotificationLibraryConfigurationProperties notificationLibraryProperties) {
    return new NotificationClient(
        notificationLibraryProperties.govukNotify().apiKey(),
        Optional.ofNullable(notificationLibraryProperties.govukNotify().baseUrl()).orElse(NOTIFY_BASE_URL)
    );
  }

  @Bean
  @ConditionalOnMissingBean
  Clock clock() {
    return Clock.systemDefaultZone();
  }
}
