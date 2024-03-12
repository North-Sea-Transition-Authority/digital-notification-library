package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@DisplayName("GIVEN I want to get a template")
@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

  @Mock
  private NotificationClient notifyClient;

  @InjectMocks
  private TemplateService templateService;

  @DisplayName("WHEN the template exists")
  @Nested
  class WhenSuccessfulNotifyResponse {

    @DisplayName("THEN the template is returned")
    @Test
    void getTemplate_whenTemplateExists() throws NotificationClientException {

      var expectedNotifyTemplate = NotifyTemplateTestUtil.builder().build();

      given(notifyClient.getTemplateById("templateId"))
          .willReturn(expectedNotifyTemplate);

      var resultingTemplateResponse = templateService.getTemplate("templateId");

      assertThat(resultingTemplateResponse)
          .extracting(Response::successResponseObject, Response::error)
          .containsExactly(expectedNotifyTemplate, null);
    }
  }

  @DisplayName("WHEN we get a failure response from notify")
  @Nested
  class WhenFailureNotifyResponse {

    @DisplayName("THEN a failure response is returned")
    @Test
    void getTemplate_whenFailureResponse() throws NotificationClientException {

      var expectedException = new NotificationClientException("exception");

      given(notifyClient.getTemplateById("templateId"))
          .willThrow(expectedException);

      var resultingTemplateResponse = templateService.getTemplate("templateId");

      assertThat(resultingTemplateResponse)
          .extracting(
              templateResponse -> templateResponse.error().httpStatus(),
              templateResponse -> templateResponse.error().message()
          )
          .containsExactly(expectedException.getHttpResult(), expectedException.getMessage());

      assertThat(resultingTemplateResponse.successResponseObject()).isNull();
    }
  }
}
