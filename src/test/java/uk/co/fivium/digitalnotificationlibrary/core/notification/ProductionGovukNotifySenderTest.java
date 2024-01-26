package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ResourceUtils;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

@DisplayName("GIVEN I am running the library in production mode")
@ExtendWith(MockitoExtension.class)
class ProductionGovukNotifySenderTest {

  @Mock
  private GovukNotifySenderService govukNotifySenderService;

  @InjectMocks
  private ProductionGovukNotifySender productionGovukNotifySender;

  @DisplayName("WHEN I want to send an email")
  @Nested
  class WhenSendEmail {

    @DisplayName("THEN the email is sent to the indented recipient")
    @Test
    void sendEmail() throws IOException {

      var notificationToSend = NotificationTestUtil.builder()
          .withType(NotificationType.EMAIL)
          .build();

      var fileData = readFileData("notifySendEmailResponse.json");
      var expectedEmailResponse = new SendEmailResponse(new String(fileData));

      given(govukNotifySenderService.sendEmail(notificationToSend))
          .willReturn(Response.successfulResponse(expectedEmailResponse));

      var resultingEmailResponse = productionGovukNotifySender.sendEmail(notificationToSend);

      assertThat(resultingEmailResponse)
          .extracting(Response::successResponseObject)
          .isEqualTo(expectedEmailResponse);
    }
  }

  @DisplayName("WHEN I want to send an sms")
  @Nested
  class WhenSendSms {

    @DisplayName("THEN the sms is sent to the indented recipient")
    @Test
    void sendSms() throws IOException {

      var notificationToSend = NotificationTestUtil.builder()
          .withType(NotificationType.SMS)
          .build();

      var fileData = readFileData("notifySendSmsResponse.json");
      var expectedSmsResponse = new SendSmsResponse(new String(fileData));

      given(govukNotifySenderService.sendSms(notificationToSend))
          .willReturn(Response.successfulResponse(expectedSmsResponse));

      var resultingSmsResponse = productionGovukNotifySender.sendSms(notificationToSend);

      assertThat(resultingSmsResponse)
          .extracting(Response::successResponseObject)
          .isEqualTo(expectedSmsResponse);
    }
  }

  private byte[] readFileData(String resourceName) throws IOException {
    var file = ResourceUtils.getFile(
        "classpath:uk/co/fivium/digitalnotificationlibrary/core/notification/notify/" + resourceName
    );
    return Files.readAllBytes(file.toPath());
  }
}
