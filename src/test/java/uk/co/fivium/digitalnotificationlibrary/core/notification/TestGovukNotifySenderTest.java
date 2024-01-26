package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.util.Collections;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationPropertiesTestUtil;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationMode;

@DisplayName("GIVEN I am running the library in test mode")
@ExtendWith(MockitoExtension.class)
class TestGovukNotifySenderTest {

  private static final NotificationLibraryConfigurationProperties CONFIGURATION_PROPERTIES =
      NotificationLibraryConfigurationPropertiesTestUtil.builder().build();

  private static GovukNotifySenderService govukNotifySenderService;

  private static TestGovukNotifySender testGovukNotifySender;

  @BeforeAll
  static void setup() {
    govukNotifySenderService = mock(GovukNotifySenderService.class);
    testGovukNotifySender = new TestGovukNotifySender(govukNotifySenderService, CONFIGURATION_PROPERTIES);
  }

  @DisplayName("AND I want to send an email")
  @Nested
  class AndSendEmail {

    @DisplayName("WHEN I have configured test recipients")
    @Nested
    class WhenTestRecipient {

      @DisplayName("THEN only the test recipients will receive the email")
      @Test
      void sendEmail_whenTestRecipients() {

        var libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
            .withMode(NotificationMode.TEST)
            .withTestEmailRecipient("someone@example.com")
            .withTestEmailRecipient("someone.else@example.com")
            .build();

        testGovukNotifySender = new TestGovukNotifySender(govukNotifySenderService, libraryConfigurationProperties);

        var notification = NotificationTestUtil.builder()
            .withRecipient("real.recipient@example.com")
            .withType(NotificationType.EMAIL)
            .build();

        testGovukNotifySender.sendEmail(notification);

        then(govukNotifySenderService)
            .should()
            .sendEmail(notification, "someone@example.com");

        then(govukNotifySenderService)
            .should()
            .sendEmail(notification, "someone.else@example.com");

        then(govukNotifySenderService)
            .should(never())
            .sendEmail(notification, "real.recipient@example.com");
      }
    }

    @DisplayName("WHEN I have not configured test recipients")
    @Nested
    class WhenNoTestRecipient {

      @DisplayName("THEN only the real recipient will receive the email")
      @Test
      void sendEmail_whenNoTestRecipients() {

        var libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
            .withMode(NotificationMode.TEST)
            .withTestEmailRecipients(Collections.emptySet())
            .build();

        testGovukNotifySender = new TestGovukNotifySender(govukNotifySenderService, libraryConfigurationProperties);

        var notification = NotificationTestUtil.builder()
            .withRecipient("real.recipient@example.com")
            .withType(NotificationType.EMAIL)
            .build();

        testGovukNotifySender.sendEmail(notification);

        then(govukNotifySenderService)
            .should()
            .sendEmail(notification, "real.recipient@example.com");
      }
    }
  }

  @DisplayName("AND I want to send an sms")
  @Nested
  class AndSendSms {

    @DisplayName("WHEN I have configured test recipients")
    @Nested
    class WhenTestRecipient {

      @DisplayName("THEN only the test recipients will receive the email")
      @Test
      void sendSms_whenTestRecipients() {

        var libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
            .withMode(NotificationMode.TEST)
            .withTestSmsRecipient("0123456789")
            .withTestSmsRecipient("9876543210")
            .build();

        testGovukNotifySender = new TestGovukNotifySender(govukNotifySenderService, libraryConfigurationProperties);

        var notification = NotificationTestUtil.builder()
            .withRecipient("0011223344")
            .withType(NotificationType.SMS)
            .build();

        testGovukNotifySender.sendSms(notification);

        then(govukNotifySenderService)
            .should()
            .sendSms(notification, "0123456789");

        then(govukNotifySenderService)
            .should()
            .sendSms(notification, "9876543210");

        then(govukNotifySenderService)
            .should(never())
            .sendSms(notification, "0011223344");
      }
    }

    @DisplayName("WHEN I have not configured test recipients")
    @Nested
    class WhenNoTestRecipient {

      @DisplayName("THEN only the real recipient will receive the sms")
      @Test
      void sendSms_whenNoTestRecipients() {

        var libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
            .withMode(NotificationMode.TEST)
            .withTestSmsRecipients(Collections.emptySet())
            .build();

        testGovukNotifySender = new TestGovukNotifySender(govukNotifySenderService, libraryConfigurationProperties);

        var notification = NotificationTestUtil.builder()
            .withRecipient("0123456789")
            .withType(NotificationType.SMS)
            .build();

        testGovukNotifySender.sendSms(notification);

        then(govukNotifySenderService)
            .should()
            .sendSms(notification, "0123456789");
      }
    }
  }
}
