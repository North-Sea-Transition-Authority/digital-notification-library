package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GovukNotifyNotificationStatusTest {

  @Test
  void fromNotifyStatus_whenNoMatchingStatus_thenEmptyOptional() {
    var resultingNotifyStatus = GovukNotifyNotificationStatus.fromNotifyStatus("unknown-status");
    assertThat(resultingNotifyStatus).isEmpty();
  }

  @Test
  void fromNotifyStatus_whenMatchingStatus_thenStatusReturned() {
    var expectedStatus = GovukNotifyNotificationStatus.CREATED;
    var resultingNotifyStatus = GovukNotifyNotificationStatus.fromNotifyStatus(expectedStatus.getStatus());
    assertThat(resultingNotifyStatus).contains(expectedStatus);
  }
}
