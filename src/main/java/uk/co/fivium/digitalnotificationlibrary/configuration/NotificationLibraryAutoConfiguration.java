package uk.co.fivium.digitalnotificationlibrary.configuration;

import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import uk.co.fivium.digitalnotificationlibrary.core.notification.EmailAttachmentResolver;
import uk.gov.service.notify.NotificationClient;

@ComponentScan(basePackages = "uk.co.fivium.digitalnotificationlibrary")
@EnableConfigurationProperties(NotificationLibraryConfigurationProperties.class)
class NotificationLibraryAutoConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationLibraryAutoConfiguration.class);

  NotificationLibraryAutoConfiguration(NotificationLibraryConfigurationProperties libraryProperties) {
    LOGGER.info(
        "Digital notification library Spring Boot Starter has been enabled using {} mode", libraryProperties.mode()
    );
  }

  @Bean
  NotificationClient providerNotificationClient(NotificationLibraryConfigurationProperties libraryProperties) {
    return new NotificationClient(libraryProperties.govukNotify().apiKey());
  }

  @Bean
  @ConditionalOnMissingBean
  Clock clock() {
    return Clock.systemDefaultZone();
  }

  @Bean
  @ConditionalOnMissingBean
  public EmailAttachmentResolver noOpEmailAttachmentResolver() {
    return new EmailAttachmentResolver() {

      @Override
      public byte[] resolveFileAttachment(UUID fileId) {
        throw new RuntimeException(
            "Email attachment found but consumer did not provide an EmailAttachmentResolver implementation");
      }
    };
  }
}
