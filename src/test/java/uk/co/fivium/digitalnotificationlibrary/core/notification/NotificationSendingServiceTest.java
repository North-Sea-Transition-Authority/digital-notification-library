package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ResourceUtils;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

@ExtendWith(MockitoExtension.class)
class NotificationSendingServiceTest {

  private static final Integer BULK_RETRIEVAL_SIZE = 10;

  private static NotificationLibraryNotificationRepository notificationRepository;

  private static GovukNotifyService govukNotifyService;

  private static PlatformTransactionManager transactionManager;

  private static NotificationLibraryConfigurationProperties libraryConfigurationProperties;

  @Captor
  private ArgumentCaptor<Notification> notificationCaptor;

  private static NotificationSendingService notificationSendingService;

  @BeforeAll
  static void beforeAllSetup() {

    libraryConfigurationProperties = PropertiesTestBuilder.builder().build();

    govukNotifyService = mock(GovukNotifyService.class);

    transactionManager = mock(PlatformTransactionManager.class);

    LockAssert.TestHelper.makeAllAssertsPass(true);
  }

  @BeforeEach
  void beforeEachSetup() {

    notificationRepository = mock(NotificationLibraryNotificationRepository.class);

    notificationSendingService = new NotificationSendingService(
        transactionManager,
        notificationRepository,
        govukNotifyService,
        libraryConfigurationProperties
    );
  }

  @Test
  void sendQueuedNotificationToNotify_whenNoBulkRetrievalPropertySet_thenVerifyDefaultUsed() {

    var libraryConfigurationProperties = PropertiesTestBuilder.builder()
        .withQueuedNotificationRetrievalLimit(null)
        .build();

    notificationSendingService = new NotificationSendingService(
        transactionManager,
        notificationRepository,
        govukNotifyService,
        libraryConfigurationProperties
    );

    notificationSendingService.sendQueuedNotificationToNotify();

    then(notificationRepository)
        .should()
        .findNotificationByStatusOrderByRequestedOnAsc(
            NotificationStatus.QUEUED,
            PageRequest.of(0, NotificationSendingService.DEFAULT_BULK_RETRIEVAL_LIMIT)
        );
  }

  @Test
  void sendQueuedNotificationToNotify_whenNoNotificationPropertySet_thenVerifyDefaultUsed() {

    var libraryConfigurationProperties = new NotificationLibraryConfigurationProperties(
        new NotificationLibraryConfigurationProperties.GovUkNotify("key"),
        null
    );

    notificationSendingService = new NotificationSendingService(
        transactionManager,
        notificationRepository,
        govukNotifyService,
        libraryConfigurationProperties
    );

    notificationSendingService.sendQueuedNotificationToNotify();

    then(notificationRepository)
        .should()
        .findNotificationByStatusOrderByRequestedOnAsc(
            NotificationStatus.QUEUED,
            PageRequest.of(0, NotificationSendingService.DEFAULT_BULK_RETRIEVAL_LIMIT)
        );
  }

  @Test
  void sendQueuedNotificationToNotify_whenNoQueuedPropertySet_thenVerifyDefaultUsed() {

    var libraryConfigurationProperties = new NotificationLibraryConfigurationProperties(
        new NotificationLibraryConfigurationProperties.GovUkNotify("key"),
        new NotificationLibraryConfigurationProperties.Notification(null)
    );

    notificationSendingService = new NotificationSendingService(
        transactionManager,
        notificationRepository,
        govukNotifyService,
        libraryConfigurationProperties
    );

    notificationSendingService.sendQueuedNotificationToNotify();

    then(notificationRepository)
        .should()
        .findNotificationByStatusOrderByRequestedOnAsc(
            NotificationStatus.QUEUED,
            PageRequest.of(0, NotificationSendingService.DEFAULT_BULK_RETRIEVAL_LIMIT)
        );
  }

  @Test
  void sendQueuedNotificationToNotify_whenBulkRetrievalPropertySet_thenVerifyDefaultUsed() {

    var libraryConfigurationProperties = PropertiesTestBuilder.builder()
        .withQueuedNotificationRetrievalLimit(42)
        .build();

    notificationSendingService = new NotificationSendingService(
        transactionManager,
        notificationRepository,
        govukNotifyService,
        libraryConfigurationProperties
    );

    notificationSendingService.sendQueuedNotificationToNotify();

    then(notificationRepository)
        .should()
        .findNotificationByStatusOrderByRequestedOnAsc(
            NotificationStatus.QUEUED,
            PageRequest.of(0, 42)
        );

  }

  @Test
  void sendQueuedNotificationToNotify_whenNoNotifications_thenVerifyInteractions() {

    given(notificationRepository.findNotificationByStatusOrderByRequestedOnAsc(
        NotificationStatus.QUEUED,
        PageRequest.of(0, BULK_RETRIEVAL_SIZE)
    ))
        .willReturn(Collections.emptyList());

    notificationSendingService.sendQueuedNotificationToNotify();

    then(notificationRepository)
        .should(never())
        .saveAll(anySet());
  }

  @Test
  void sendQueuedNotificationToNotify_whenQueuedEmailNotification_andSuccessfulNotifyRequest_thenVerifySavedProperties()
      throws IOException {

    var queuedNotification = NotificationTestUtil.builder()
        .withType(NotificationType.EMAIL)
        .withStatus(NotificationStatus.QUEUED)
        .build();

    given(notificationRepository.findNotificationByStatusOrderByRequestedOnAsc(
        NotificationStatus.QUEUED,
        PageRequest.of(0, BULK_RETRIEVAL_SIZE)
    ))
        .willReturn(List.of(queuedNotification));

    var fileData = readFileData("notifySendEmailResponse.json");

    Response<SendEmailResponse> expectedEmailResponse = Response.successfulResponse(
        new SendEmailResponse(new String(fileData))
    );

    given(govukNotifyService.sendEmail(queuedNotification))
        .willReturn(expectedEmailResponse);

    notificationSendingService.sendQueuedNotificationToNotify();

    then(notificationRepository)
        .should()
        .save(notificationCaptor.capture());

    var savedNotification = notificationCaptor.getValue();

    assertThat(savedNotification)
        .extracting(
            Notification::getStatus,
            Notification::getNotifyNotificationId,
            Notification::getFailureReason
        )
        .containsExactly(
            NotificationStatus.SENT_TO_NOTIFY,
            String.valueOf(expectedEmailResponse.successResponseObject().getNotificationId()),
            null // not failure reason as send request was successful
        );
  }

  @Test
  void sendQueuedNotificationToNotify_whenQueuedSmsNotification_andSuccessfulNotifyRequest_thenVerifySavedProperties()
      throws IOException {

    var queuedNotification = NotificationTestUtil.builder()
        .withType(NotificationType.SMS)
        .withStatus(NotificationStatus.QUEUED)
        .build();

    given(notificationRepository.findNotificationByStatusOrderByRequestedOnAsc(
        NotificationStatus.QUEUED,
        PageRequest.of(0, BULK_RETRIEVAL_SIZE)
    ))
        .willReturn(List.of(queuedNotification));

    var fileData = readFileData("notifySendSmsResponse.json");
    Response<SendSmsResponse> expectedSmsResponse = Response.successfulResponse(
        new SendSmsResponse(new String(fileData))
    );

    given(govukNotifyService.sendSms(queuedNotification))
        .willReturn(expectedSmsResponse);

    notificationSendingService.sendQueuedNotificationToNotify();

    then(notificationRepository)
        .should()
        .save(notificationCaptor.capture());

    var savedNotification = notificationCaptor.getValue();

    assertThat(savedNotification)
        .extracting(
            Notification::getStatus,
            Notification::getNotifyNotificationId,
            Notification::getFailureReason
        )
        .containsExactly(
            NotificationStatus.SENT_TO_NOTIFY,
            String.valueOf(expectedSmsResponse.successResponseObject().getNotificationId()),
            null // not failure reason as send request was successful
        );
  }

  @ParameterizedTest
  @ValueSource(ints = {403, 400})
  void sendQueuedNotificationToNotify_whenEmailNotification_andPermanentErrorNotifyResponse_thenVerifySavedProperties(
      int permanentErrorHttpStatus
  ) {

    var queuedNotification = NotificationTestUtil.builder()
        .withType(NotificationType.EMAIL)
        .withStatus(NotificationStatus.QUEUED)
        .build();

    given(notificationRepository.findNotificationByStatusOrderByRequestedOnAsc(
        NotificationStatus.QUEUED,
        PageRequest.of(0, BULK_RETRIEVAL_SIZE)
    ))
        .willReturn(List.of(queuedNotification));

    given(govukNotifyService.sendEmail(queuedNotification))
        .willReturn(Response.failedResponse(permanentErrorHttpStatus, "error-message"));

    notificationSendingService.sendQueuedNotificationToNotify();

    then(notificationRepository)
        .should()
        .save(notificationCaptor.capture());

    var savedNotification = notificationCaptor.getValue();

    assertThat(savedNotification)
        .extracting(Notification::getStatus, Notification::getNotifyNotificationId)
        .containsExactly(NotificationStatus.FAILED_NOT_SENT, null);

    assertThat(savedNotification.getFailureReason()).isNotNull();
  }

  @Test
  void sendQueuedNotificationToNotify_whenEmailNotification_andTemporaryErrorNotifyResponse_thenVerifySavedProperties() {

    var queuedNotification = NotificationTestUtil.builder()
        .withType(NotificationType.EMAIL)
        .withStatus(NotificationStatus.QUEUED)
        .build();

    given(notificationRepository.findNotificationByStatusOrderByRequestedOnAsc(
        NotificationStatus.QUEUED,
        PageRequest.of(0, BULK_RETRIEVAL_SIZE)
    ))
        .willReturn(List.of(queuedNotification));

    given(govukNotifyService.sendEmail(queuedNotification))
        .willReturn(Response.failedResponse(500, "error-message"));

    notificationSendingService.sendQueuedNotificationToNotify();

    then(notificationRepository)
        .should()
        .save(notificationCaptor.capture());

    var savedNotification = notificationCaptor.getValue();

    assertThat(savedNotification)
        .extracting(Notification::getStatus, Notification::getNotifyNotificationId)
        .containsExactly(NotificationStatus.TEMPORARY_FAILURE, null);

    assertThat(savedNotification.getFailureReason()).isNotNull();
  }

  @ParameterizedTest
  @ValueSource(ints = {403, 400})
  void sendQueuedNotificationToNotify_whenSmsNotification_andPermanentErrorNotifyResponse_thenVerifySavedProperties(
      int permanentErrorHttpStatus
  ) {

    var queuedNotification = NotificationTestUtil.builder()
        .withType(NotificationType.SMS)
        .withStatus(NotificationStatus.QUEUED)
        .build();

    given(notificationRepository.findNotificationByStatusOrderByRequestedOnAsc(
        NotificationStatus.QUEUED,
        PageRequest.of(0, BULK_RETRIEVAL_SIZE)
    ))
        .willReturn(List.of(queuedNotification));

    given(govukNotifyService.sendSms(queuedNotification))
        .willReturn(Response.failedResponse(permanentErrorHttpStatus, "error-message"));

    notificationSendingService.sendQueuedNotificationToNotify();

    then(notificationRepository)
        .should()
        .save(notificationCaptor.capture());

    var savedNotification = notificationCaptor.getValue();

    assertThat(savedNotification)
        .extracting(Notification::getStatus, Notification::getNotifyNotificationId)
        .containsExactly(NotificationStatus.FAILED_NOT_SENT, null);

    assertThat(savedNotification.getFailureReason()).isNotNull();
  }

  @Test
  void sendQueuedNotificationToNotify_whenSmsNotification_andTemporaryErrorNotifyResponse_thenVerifySavedProperties() {

    var queuedNotification = NotificationTestUtil.builder()
        .withType(NotificationType.SMS)
        .withStatus(NotificationStatus.QUEUED)
        .build();

    given(notificationRepository.findNotificationByStatusOrderByRequestedOnAsc(
        NotificationStatus.QUEUED,
        PageRequest.of(0, BULK_RETRIEVAL_SIZE)
    ))
        .willReturn(List.of(queuedNotification));

    given(govukNotifyService.sendSms(queuedNotification))
        .willReturn(Response.failedResponse(500, "error-message"));

    notificationSendingService.sendQueuedNotificationToNotify();

    then(notificationRepository)
        .should()
        .save(notificationCaptor.capture());

    var savedNotification = notificationCaptor.getValue();

    assertThat(savedNotification)
        .extracting(Notification::getStatus, Notification::getNotifyNotificationId)
        .containsExactly(NotificationStatus.TEMPORARY_FAILURE, null);

    assertThat(savedNotification.getFailureReason()).isNotNull();
  }

  private byte[] readFileData(String resourceName) throws IOException {
    var file = ResourceUtils.getFile(
        "classpath:uk/co/fivium/digitalnotificationlibrary/core/notification/notify/" + resourceName
    );
    return Files.readAllBytes(file.toPath());
  }

  static class PropertiesTestBuilder {

    static Builder builder() {
      return new Builder();
    }

    static class Builder {

      private Integer queuedNotificationBulkRetrievalLimit = 10;

      Builder withQueuedNotificationRetrievalLimit(Integer queuedNotificationBulkRetrievalLimit) {
        this.queuedNotificationBulkRetrievalLimit = queuedNotificationBulkRetrievalLimit;
        return this;
      }

      NotificationLibraryConfigurationProperties build() {
        return new NotificationLibraryConfigurationProperties(
            new NotificationLibraryConfigurationProperties.GovUkNotify("api-key"),
            new NotificationLibraryConfigurationProperties.Notification(
                new NotificationLibraryConfigurationProperties.Notification.Queued(1, queuedNotificationBulkRetrievalLimit)
            )
        );
      }
    }
  }
}