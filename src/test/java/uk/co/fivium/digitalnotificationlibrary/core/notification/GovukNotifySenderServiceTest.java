package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ResourceUtils;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

@ExtendWith(MockitoExtension.class)
class GovukNotifySenderServiceTest {

  @Mock
  private NotificationClient notifyClient;

  @InjectMocks
  private GovukNotifySenderService govukNotifySenderService;

  @DisplayName("GIVEN I want to send an email")
  @Nested
  class SendEmail {

    @DisplayName("WHEN the notification is not an email")
    @Nested
    class WhenNotificationIsNotEmail {

      @DisplayName("THEN an exception is thrown")
      @ParameterizedTest(name = "WHEN the notification is of type {0}")
      @EnumSource(value = NotificationType.class, mode = EnumSource.Mode.EXCLUDE, names = "EMAIL")
      void sendEmail_whenNotificationIsNotEmail(NotificationType nonEmailNotificationType) {

        var nonEmailNotification = NotificationTestUtil.builder()
            .withType(nonEmailNotificationType)
            .build();

        assertThatThrownBy(() -> govukNotifySenderService.sendEmail(nonEmailNotification))
            .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> govukNotifySenderService.sendEmail(nonEmailNotification, "someone@example.com"))
            .isInstanceOf(IllegalStateException.class);
      }
    }

    @DisplayName("WHEN the email is sent by notify")
    @Nested
    class WhenSuccessfullySentToNotify {

      @DisplayName("AND the email is sent to the original recipient")
      @Nested
      class AndSentToOriginalRecipient {

        @DisplayName("THEN a successful response is returned from notify")
        @Test
        void sendEmail_whenSuccessfullySent() throws IOException, NotificationClientException {

          var notification = NotificationTestUtil.builder()
              .withType(NotificationType.EMAIL)
              .withMailMergeField("key", "value")
              .build();

          var fileData = readFileData("notifySendEmailResponse.json");
          var expectedEmailResponse = new SendEmailResponse(new String(fileData));

          given(notifyClient.sendEmail(
              notification.getNotifyTemplateId(),
              notification.getRecipient(),
              Map.of("key", "value"),
              notification.getLogCorrelationId()
          ))
              .willReturn(expectedEmailResponse);

          var resultingNotifyEmailResponse = govukNotifySenderService.sendEmail(notification);

          assertThat(resultingNotifyEmailResponse)
              .extracting(Response::successResponseObject)
              .isEqualTo(expectedEmailResponse);
        }
      }

      @DisplayName("AND the email is not sent to the original recipient")
      @Nested
      class AndNotSentToOriginalRecipient {

        @DisplayName("THEN a successful response is returned from notify")
        @Test
        void sendEmail_whenSuccessfullySent() throws IOException, NotificationClientException {

          var notification = NotificationTestUtil.builder()
              .withType(NotificationType.EMAIL)
              .withRecipient("someone@example.com")
              .withMailMergeField("key", "value")
              .build();

          var fileData = readFileData("notifySendEmailResponse.json");
          var expectedEmailResponse = new SendEmailResponse(new String(fileData));

          given(notifyClient.sendEmail(
              notification.getNotifyTemplateId(),
              "someone.else@example.com",
              Map.of("key", "value"),
              notification.getLogCorrelationId()
          ))
              .willReturn(expectedEmailResponse);

          var resultingNotifyEmailResponse = govukNotifySenderService
              .sendEmail(notification, "someone.else@example.com");

          assertThat(resultingNotifyEmailResponse)
              .extracting(Response::successResponseObject)
              .isEqualTo(expectedEmailResponse);
        }
      }
    }

    @DisplayName("WHEN the email is not sent by notify")
    @Nested
    class WhenFailureResponseFromNotify {

      @DisplayName("AND the email is sent to the original recipient")
      @Nested
      class AndSentToOriginalRecipient {

        @DisplayName("THEN a failure response is returned from notify")
        @Test
        void sendEmail_whenUnsuccessfulRequestToNotify_thenFailureResponse() throws NotificationClientException {

          var notification = NotificationTestUtil.builder()
              .withType(NotificationType.EMAIL)
              .withMailMergeField("key", "value")
              .build();

          var expectedNotifyException = new NotificationClientException("error");

          given(notifyClient.sendEmail(
              notification.getNotifyTemplateId(),
              notification.getRecipient(),
              Map.of("key", "value"),
              notification.getLogCorrelationId()
          ))
              .willThrow(expectedNotifyException);

          var resultingNotifyEmailResponse = govukNotifySenderService.sendEmail(notification);

          assertThat(resultingNotifyEmailResponse.error())
              .extracting(Response.ErrorResponse::httpStatus, Response.ErrorResponse::message)
              .contains(expectedNotifyException.getHttpResult(), expectedNotifyException.getMessage());
        }
      }

      @DisplayName("AND the email is not sent to the original recipient")
      @Nested
      class AndNotSentToOriginalRecipient {

        @DisplayName("THEN a failure response is returned from notify")
        @Test
        void sendEmail_whenUnsuccessfulRequestToNotify_thenFailureResponse() throws NotificationClientException {

          var notification = NotificationTestUtil.builder()
              .withType(NotificationType.EMAIL)
              .withRecipient("someone@example.com")
              .withMailMergeField("key", "value")
              .build();

          var expectedNotifyException = new NotificationClientException("error");

          given(notifyClient.sendEmail(
              notification.getNotifyTemplateId(),
              "someone.else@example.com",
              Map.of("key", "value"),
              notification.getLogCorrelationId()
          ))
              .willThrow(expectedNotifyException);

          var resultingNotifyEmailResponse = govukNotifySenderService
              .sendEmail(notification, "someone.else@example.com");

          assertThat(resultingNotifyEmailResponse.error())
              .extracting(Response.ErrorResponse::httpStatus, Response.ErrorResponse::message)
              .contains(expectedNotifyException.getHttpResult(), expectedNotifyException.getMessage());
        }
      }
    }
  }

  @DisplayName("GIVEN I want to send an sms")
  @Nested
  class SendSms {

    @DisplayName("WHEN the notification is not an sms")
    @Nested
    class WhenNotificationIsNotSms {

      @DisplayName("THEN an exception is thrown")
      @ParameterizedTest(name = "WHEN the notification is of type {0}")
      @EnumSource(value = NotificationType.class, mode = EnumSource.Mode.EXCLUDE, names = "SMS")
      void sendSms_whenNotificationIsNotSms(NotificationType nonSmsNotificationType) {

        var nonSmsNotification = NotificationTestUtil.builder()
            .withType(nonSmsNotificationType)
            .build();

        assertThatThrownBy(() -> govukNotifySenderService.sendSms(nonSmsNotification))
            .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> govukNotifySenderService.sendSms(nonSmsNotification, "0123456789"))
            .isInstanceOf(IllegalStateException.class);
      }
    }

    @DisplayName("WHEN the sms is sent by notify")
    @Nested
    class WhenSuccessfullySentToNotify {

      @DisplayName("AND the sms is sent to the original recipient")
      @Nested
      class AndSentToOriginalRecipient {

        @DisplayName("THEN a successful response is returned from notify")
        @Test
        void sendSms_whenSuccessfullySent() throws IOException, NotificationClientException {

          var notification = NotificationTestUtil.builder()
              .withType(NotificationType.SMS)
              .withMailMergeField("key", "value")
              .build();

          var fileData = readFileData("notifySendSmsResponse.json");
          var expectedSmsResponse = new SendSmsResponse(new String(fileData));

          given(notifyClient.sendSms(
              notification.getNotifyTemplateId(),
              notification.getRecipient(),
              Map.of("key", "value"),
              notification.getLogCorrelationId()
          ))
              .willReturn(expectedSmsResponse);

          var resultingNotifySmsResponse = govukNotifySenderService.sendSms(notification);

          assertThat(resultingNotifySmsResponse)
              .extracting(Response::successResponseObject)
              .isEqualTo(expectedSmsResponse);
        }
      }

      @DisplayName("AND the sms is not sent to the original recipient")
      @Nested
      class AndNotSentToOriginalRecipient {

        @DisplayName("THEN a successful response is returned from notify")
        @Test
        void sendSms_whenSuccessfullySent() throws IOException, NotificationClientException {

          var notification = NotificationTestUtil.builder()
              .withType(NotificationType.SMS)
              .withRecipient("0123456789")
              .withMailMergeField("key", "value")
              .build();

          var fileData = readFileData("notifySendSmsResponse.json");
          var expectedSmsResponse = new SendSmsResponse(new String(fileData));

          given(notifyClient.sendSms(
              notification.getNotifyTemplateId(),
              "9876543210",
              Map.of("key", "value"),
              notification.getLogCorrelationId()
          ))
              .willReturn(expectedSmsResponse);

          var resultingNotifySmsResponse = govukNotifySenderService.sendSms(notification, "9876543210");

          assertThat(resultingNotifySmsResponse)
              .extracting(Response::successResponseObject)
              .isEqualTo(expectedSmsResponse);
        }
      }
    }

    @DisplayName("WHEN the sms is not sent by notify")
    @Nested
    class WhenFailureResponseFromNotify {

      @DisplayName("AND the sms is sent to the original recipient")
      @Nested
      class AndSentToOriginalRecipient {

        @DisplayName("THEN a failure response is returned from notify")
        @Test
        void sendSms_whenUnsuccessfulRequestToNotify_thenFailureResponse() throws NotificationClientException {

          var notification = NotificationTestUtil.builder()
              .withType(NotificationType.SMS)
              .withMailMergeField("key", "value")
              .build();

          var expectedNotifyException = new NotificationClientException("error");

          given(notifyClient.sendSms(
              notification.getNotifyTemplateId(),
              notification.getRecipient(),
              Map.of("key", "value"),
              notification.getLogCorrelationId()
          ))
              .willThrow(expectedNotifyException);

          var resultingNotifySmsResponse = govukNotifySenderService.sendSms(notification);

          assertThat(resultingNotifySmsResponse.error())
              .extracting(Response.ErrorResponse::httpStatus, Response.ErrorResponse::message)
              .contains(expectedNotifyException.getHttpResult(), expectedNotifyException.getMessage());
        }
      }

      @DisplayName("AND the sms is not sent to the original recipient")
      @Nested
      class AndNotSentToOriginalRecipient {

        @DisplayName("THEN a failure response is returned from notify")
        @Test
        void sendEmail_whenUnsuccessfulRequestToNotify_thenFailureResponse() throws NotificationClientException {

          var notification = NotificationTestUtil.builder()
              .withType(NotificationType.SMS)
              .withRecipient("0123456789")
              .withMailMergeField("key", "value")
              .build();

          var expectedNotifyException = new NotificationClientException("error");

          given(notifyClient.sendSms(
              notification.getNotifyTemplateId(),
              "9876543210",
              Map.of("key", "value"),
              notification.getLogCorrelationId()
          ))
              .willThrow(expectedNotifyException);

          var resultingNotifySmsResponse = govukNotifySenderService.sendSms(notification, "9876543210");

          assertThat(resultingNotifySmsResponse.error())
              .extracting(Response.ErrorResponse::httpStatus, Response.ErrorResponse::message)
              .contains(expectedNotifyException.getHttpResult(), expectedNotifyException.getMessage());
        }
      }
    }
  }

  private byte[] readFileData(String resourceName) throws IOException {
    var file = ResourceUtils.getFile(
        "classpath:uk/co/fivium/digitalnotificationlibrary/core/notification/notify/" + resourceName
    );
    return Files.readAllBytes(file.toPath());
  }
}