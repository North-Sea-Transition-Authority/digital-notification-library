package uk.co.fivium.digitalnotificationlibrary.core.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;

@Configuration
class NotificationAutoConfiguration {

  @Bean
  @ConditionalOnProperty(prefix = "digital-notification-library", name = "mode", havingValue = "test")
  GovukNotifySender testGovukNotifySender(GovukNotifySenderService govukNotifySenderService,
                                          NotificationLibraryConfigurationProperties libraryConfigurationProperties) {
    return new TestGovukNotifySender(govukNotifySenderService, libraryConfigurationProperties);
  }

  @Bean
  @ConditionalOnProperty(prefix = "digital-notification-library", name = "mode", havingValue = "production")
  GovukNotifySender productionGovukNotifySender(GovukNotifySenderService govukNotifySenderService) {
    return new ProductionGovukNotifySender(govukNotifySenderService);
  }
}
