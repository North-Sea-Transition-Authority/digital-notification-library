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

@DisplayName("GIVEN I want to get a notification from notify")
@ExtendWith(MockitoExtension.class)
class GovukNotifyNotificationServiceTest {

  @Mock
  private NotificationClient govukNotificationClient;

  @InjectMocks
  private GovukNotifyNotificationService govukNotifyNotificationService;

  @DisplayName("WHEN the notification exists")
  @Nested
  class WhenSuccessfulNotifyResponse {

    @DisplayName("THEN the notification is returned")
    @Test
    void thenNotificationIsReturned() throws NotificationClientException {

      var libraryNotification = NotificationTestUtil.builder()
          .withNotifyNotificationId("notification-id")
          .build();

      var expectedNotifyNotification = NotifyNotificationTestUtil.builder().build();

      given(govukNotificationClient.getNotificationById("notification-id"))
          .willReturn(expectedNotifyNotification);

      var resultingNotifyResponse = govukNotifyNotificationService.getNotification(libraryNotification);

      assertThat(resultingNotifyResponse.successResponseObject()).isEqualTo(expectedNotifyNotification);
      assertThat(resultingNotifyResponse.error()).isNull();
    }
  }

  @DisplayName("WHEN notify returns a failure response")
  @Nested
  class WhenFailureNotifyResponse {

    @DisplayName("THEN an error response is returned")
    @Test
    void thenErrorResponseReturned() throws NotificationClientException {

      var libraryNotification = NotificationTestUtil.builder()
          .withNotifyNotificationId("notification-id")
          .build();

      var expectedException = new NotificationClientException("this exception");

      given(govukNotificationClient.getNotificationById("notification-id"))
          .willThrow(expectedException);

      var resultingNotifyResponse = govukNotifyNotificationService.getNotification(libraryNotification);

      assertThat(resultingNotifyResponse.error())
          .extracting(Response.ErrorResponse::httpStatus, Response.ErrorResponse::message)
          .containsExactly(expectedException.getHttpResult(), expectedException.getMessage());

      assertThat(resultingNotifyResponse.successResponseObject()).isNull();
    }

  }
}
