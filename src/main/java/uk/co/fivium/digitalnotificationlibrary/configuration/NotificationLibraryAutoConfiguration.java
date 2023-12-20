package uk.co.fivium.digitalnotificationlibrary.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = "uk.co.fivium.digitalnotificationlibrary")
class NotificationLibraryAutoConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationLibraryAutoConfiguration.class);

  NotificationLibraryAutoConfiguration() {
    LOGGER.info("Digital notification library Spring Boot Starter has been enabled");
  }
}
