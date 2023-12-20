package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
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
class GovukNotifyServiceTest {

  @Mock
  private NotificationClient notifyClient;

  @InjectMocks
  private GovukNotifyService notifyService;

  @ParameterizedTest
  @EnumSource(value = NotificationType.class, mode = EnumSource.Mode.EXCLUDE, names = "EMAIL")
  void sendEmail_whenNotificationNotEmailType_thenException(NotificationType nonEmailNotificationType) {

    var nonEmailNotification = NotificationTestUtil.builder()
        .withType(nonEmailNotificationType)
        .build();

    assertThatThrownBy(() -> notifyService.sendEmail(nonEmailNotification))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void sendEmail_whenSuccessfulRequestToNotify_thenSuccessfulResponse() throws NotificationClientException, IOException {

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

    var resultingNotifyEmailResponse = notifyService.sendEmail(notification);

    assertThat(resultingNotifyEmailResponse)
        .extracting(Response::successResponseObject)
        .isEqualTo(expectedEmailResponse);
  }

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

    var resultingNotifyEmailResponse = notifyService.sendEmail(notification);

    assertThat(resultingNotifyEmailResponse.error())
        .extracting(Response.ErrorResponse::httpStatus, Response.ErrorResponse::message)
        .contains(expectedNotifyException.getHttpResult(), expectedNotifyException.getMessage());
  }

  @ParameterizedTest
  @EnumSource(value = NotificationType.class, mode = EnumSource.Mode.EXCLUDE, names = "SMS")
  void sendSms_whenNotificationNotSmsType_thenException(NotificationType nonSmsNotificationType) {

    var nonEmailNotification = NotificationTestUtil.builder()
        .withType(nonSmsNotificationType)
        .build();

    assertThatThrownBy(() -> notifyService.sendSms(nonEmailNotification))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void sendSms_whenSuccessfulRequestToNotify_thenSuccessfulResponse() throws NotificationClientException, IOException {

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

    var resultingNotifySmsResponse = notifyService.sendSms(notification);

    assertThat(resultingNotifySmsResponse)
        .extracting(Response::successResponseObject)
        .isEqualTo(expectedSmsResponse);
  }

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

    var resultingNotifyEmailResponse = notifyService.sendSms(notification);

    assertThat(resultingNotifyEmailResponse.error())
        .extracting(Response.ErrorResponse::httpStatus, Response.ErrorResponse::message)
        .contains(expectedNotifyException.getHttpResult(), expectedNotifyException.getMessage());
  }

  @Test
  void getTemplate_whenSuccessfulNotifyResponse_thenSuccessfulResponseReturned() throws NotificationClientException {

    var templateId = UUID.randomUUID().toString();
    var expectedNotifyTemplate = NotifyTemplateTestUtil.builder().build();

    given(notifyClient.getTemplateById(templateId))
        .willReturn(expectedNotifyTemplate);

    var resultingTemplateResponse = notifyService.getTemplate(templateId);

    assertThat(resultingTemplateResponse)
        .extracting(Response::successResponseObject)
        .isEqualTo(expectedNotifyTemplate);
  }

  @Test
  void getTemplate_whenUnsuccessfulNotifyResponse_thenFailureResponseReturned() throws NotificationClientException {

    var templateId = UUID.randomUUID().toString();
    var expectedNotifyException = new NotificationClientException("exception-message");

    given(notifyClient.getTemplateById(templateId))
        .willThrow(expectedNotifyException);

    var resultingTemplateResponse = notifyService.getTemplate(templateId);

    assertThat(resultingTemplateResponse)
        .extracting(
            response -> response.error().httpStatus(),
            response -> response.error().message()
        )
        .contains(
            expectedNotifyException.getHttpResult(),
            "exception-message"
        );
  }

  private byte[] readFileData(String resourceName) throws IOException {
    var file = ResourceUtils.getFile(
        "classpath:uk/co/fivium/digitalnotificationlibrary/core/notification/notify/" + resourceName
    );
    return Files.readAllBytes(file.toPath());
  }
}
