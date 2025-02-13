package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationPropertiesTestUtil;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

//@DisplayName("GIVEN I want to send notifications to notify")
//@ExtendWith(MockitoExtension.class)
class NotificationSendingServiceTest {

//  private static final Integer BULK_RETRIEVAL_SIZE = 5;
//
//  private static final Instant FIXED_INSTANT = Instant.now();
//
//  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneId.systemDefault());
//
//  private static NotificationLibraryNotificationRepository notificationRepository;
//
//  private static TestGovukNotifySender govukNotifyService;
//
//  private static PlatformTransactionManager transactionManager;
//
//  private static NotificationLibraryConfigurationProperties libraryConfigurationProperties;
//  private EmailAttachmentResolver emailAttachmentResolver;
//
//  @Captor
//  private ArgumentCaptor<Notification> notificationCaptor;
//
//  private static NotificationSendingService notificationSendingService;
//
//  @BeforeAll
//  static void beforeAllSetup() {
//
//    libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder().build();
//
//    govukNotifyService = mock(TestGovukNotifySender.class);
//
//    transactionManager = mock(PlatformTransactionManager.class);
//  }
//
//  @BeforeEach
//  void beforeEachSetup() {
//
//    notificationRepository = mock(NotificationLibraryNotificationRepository.class);
//
//    notificationSendingService = new NotificationSendingService(
//        transactionManager,
//        notificationRepository,
//        govukNotifyService,
//        libraryConfigurationProperties,
//        FIXED_CLOCK,
//        emailAttachmentResolver
//    );
//  }
//
//  @DisplayName("WHEN no custom bulk retrieval value provided")
//  @Nested
//  class WhenNoCustomBulkRetrievalSet {
//
//    @DisplayName("THEN the default library value will be used instead")
//    @Test
//    void sendNotificationToNotify_whenNoBulkRetrievalPropertySet_thenVerifyDefaultUsed() {
//
//      var libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
//          .withNotificationRetrievalLimit(null)
//          .build();
//
//      notificationSendingService = new NotificationSendingService(
//          transactionManager,
//          notificationRepository,
//          govukNotifyService,
//          libraryConfigurationProperties,
//          FIXED_CLOCK,
//          emailAttachmentResolver
//      );
//
//      notificationSendingService.sendNotificationsToNotify();
//
//      then(notificationRepository)
//          .should()
//          .findNotificationsByStatuses(
//              Set.of(NotificationStatus.QUEUED, NotificationStatus.RETRY),
//              PageRequest.of(0, NotificationLibraryConfigurationProperties.DEFAULT_BULK_RETRIEVAL_LIMIT)
//          );
//    }
//
//    @DisplayName("AND no other custom notification properties are set")
//    @Nested
//    class WhenNoCustomNotificationPropertiesSet {
//
//      @DisplayName("THEN the default library value will be used instead")
//      @Test
//      void sendNotificationToNotify_whenNoNotificationPropertySet_thenVerifyDefaultUsed() {
//
//        var libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
//            .withNotificationProperties(null)
//            .build();
//
//        notificationSendingService = new NotificationSendingService(
//            transactionManager,
//            notificationRepository,
//            govukNotifyService,
//            libraryConfigurationProperties,
//            FIXED_CLOCK,
//            emailAttachmentResolver
//        );
//
//        notificationSendingService.sendNotificationsToNotify();
//
//        then(notificationRepository)
//            .should()
//            .findNotificationsByStatuses(
//                Set.of(NotificationStatus.QUEUED, NotificationStatus.RETRY),
//                PageRequest.of(0, NotificationLibraryConfigurationProperties.DEFAULT_BULK_RETRIEVAL_LIMIT)
//            );
//      }
//    }
//  }
//
//  @DisplayName("WHEN custom bulk retrieval value provided")
//  @Nested
//  class WhenCustomBulkRetrievalSet {
//
//    @DisplayName("THEN custom bulk retrieval value used")
//    @Test
//    void sendNotificationToNotify_whenBulkRetrievalPropertySet_thenVerifyDefaultUsed() {
//
//      var libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
//          .withNotificationRetrievalLimit(42)
//          .build();
//
//      notificationSendingService = new NotificationSendingService(
//          transactionManager,
//          notificationRepository,
//          govukNotifyService,
//          libraryConfigurationProperties,
//          FIXED_CLOCK,
//          emailAttachmentResolver
//      );
//
//      notificationSendingService.sendNotificationsToNotify();
//
//      then(notificationRepository)
//          .should()
//          .findNotificationsByStatuses(
//              Set.of(NotificationStatus.QUEUED, NotificationStatus.RETRY),
//              PageRequest.of(0, 42)
//          );
//    }
//  }
//
//  @DisplayName("WHEN there are no notifications to send")
//  @Nested
//  class WhenNoNotificationsToSend {
//
//    @DisplayName("THEN no notifications updated")
//    @Test
//    void sendNotificationToNotify_whenNoNotifications_thenVerifyInteractions() {
//
//      givenDatabaseReturnsNoNotifications();
//
//      notificationSendingService.sendNotificationsToNotify();
//
//      then(notificationRepository)
//          .should(never())
//          .saveAll(anySet());
//    }
//  }
//
//  @DisplayName("WHEN there are notifications to send")
//  @Nested
//  class WhenNotificationsToSend {
//
//    @DisplayName("AND the email is QUEUED")
//    @Nested
//    class AndEmailStatusIsQueued {
//
//      @DisplayName("THEN the email is sent to notify")
//      @Test
//      void whenQueuedEmailNotification_andSuccessfulNotifyRequest_thenVerifySavedProperties() throws IOException {
//
//        var queuedNotification = NotificationTestUtil.builder()
//            .withType(NotificationType.EMAIL)
//            .withStatus(NotificationStatus.QUEUED)
//            .withLastSendAttemptAt(null)
//            .build();
//
//        givenDatabaseReturnsNotification(queuedNotification);
//
//        var fileData = readFileData("notifySendEmailResponse.json");
//
//        Response<SendEmailResponse> expectedEmailResponse = Response.successfulResponse(
//            new SendEmailResponse(new String(fileData))
//        );
//
//        given(govukNotifyService.sendEmail(queuedNotification))
//            .willReturn(expectedEmailResponse);
//
//        notificationSendingService.sendNotificationsToNotify();
//
//        then(notificationRepository)
//            .should()
//            .save(notificationCaptor.capture());
//
//        var savedNotification = notificationCaptor.getValue();
//
//        assertThat(savedNotification)
//            .extracting(
//                Notification::getStatus,
//                Notification::getNotifyNotificationId,
//                Notification::getLastSendAttemptAt
//            )
//            .containsExactly(
//                NotificationStatus.SENT_TO_NOTIFY,
//                String.valueOf(expectedEmailResponse.successResponseObject().getNotificationId()),
//                FIXED_INSTANT
//            );
//
//        assertThat(savedNotification)
//            .extracting(Notification::getFailureReason, Notification::getRetryCount, Notification::getLastFailedAt)
//            .containsOnlyNulls();
//      }
//    }
//
//    @DisplayName("AND the email is RETRY")
//    @Nested
//    class AndEmailStatusIsRetry {
//
//      @DisplayName("THEN the email is sent to notify for its first retry attempt")
//      @Test
//      void whenFirstRetryEmailNotification_andSuccessfulNotifyRequest_thenVerifySavedProperties() throws IOException {
//
//        Instant yesterday = FIXED_CLOCK.instant().minus(1, ChronoUnit.DAYS);
//
//        var retryNotification = NotificationTestUtil.builder()
//            .withType(NotificationType.EMAIL)
//            .withStatus(NotificationStatus.RETRY)
//            .withRetryCount(0)
//            .withLastSendAttemptAt(yesterday)
//            .withLastFailedAt(yesterday)
//            .build();
//
//        givenDatabaseReturnsNotification(retryNotification);
//
//        var fileData = readFileData("notifySendEmailResponse.json");
//
//        Response<SendEmailResponse> expectedEmailResponse = Response.successfulResponse(
//            new SendEmailResponse(new String(fileData))
//        );
//
//        given(govukNotifyService.sendEmail(retryNotification))
//            .willReturn(expectedEmailResponse);
//
//        notificationSendingService.sendNotificationsToNotify();
//
//        then(notificationRepository)
//            .should()
//            .save(notificationCaptor.capture());
//
//        var savedNotification = notificationCaptor.getValue();
//
//        assertThat(savedNotification)
//            .extracting(
//                Notification::getStatus,
//                Notification::getNotifyNotificationId,
//                Notification::getLastSendAttemptAt,
//                Notification::getFailureReason,
//                Notification::getRetryCount,
//                Notification::getLastFailedAt
//            )
//            .containsExactly(
//                NotificationStatus.SENT_TO_NOTIFY,
//                String.valueOf(expectedEmailResponse.successResponseObject().getNotificationId()),
//                FIXED_INSTANT,
//                null, // not failure reason as send request was successful
//                1, // as successfully sent again the retry count is incremented by 1,
//                yesterday
//            );
//      }
//
//      @DisplayName("THEN the email is sent to notify for its next retry attempt")
//      @Test
//      void whenSecondRetryEmailNotification_andSuccessfulNotifyRequest_thenVerifySavedProperties() throws IOException {
//
//        Instant yesterday = FIXED_CLOCK.instant().minus(1, ChronoUnit.DAYS);
//
//        var retryNotification = NotificationTestUtil.builder()
//            .withType(NotificationType.EMAIL)
//            .withStatus(NotificationStatus.RETRY)
//            .withRetryCount(1)
//            .withLastSendAttemptAt(yesterday)
//            .withLastFailedAt(yesterday)
//            .build();
//
//        givenDatabaseReturnsNotification(retryNotification);
//
//        var fileData = readFileData("notifySendEmailResponse.json");
//
//        Response<SendEmailResponse> expectedEmailResponse = Response.successfulResponse(
//            new SendEmailResponse(new String(fileData))
//        );
//
//        given(govukNotifyService.sendEmail(retryNotification))
//            .willReturn(expectedEmailResponse);
//
//        notificationSendingService.sendNotificationsToNotify();
//
//        then(notificationRepository)
//            .should()
//            .save(notificationCaptor.capture());
//
//        var savedNotification = notificationCaptor.getValue();
//
//        assertThat(savedNotification)
//            .extracting(
//                Notification::getStatus,
//                Notification::getNotifyNotificationId,
//                Notification::getLastSendAttemptAt,
//                Notification::getFailureReason,
//                Notification::getRetryCount,
//                Notification::getLastFailedAt
//            )
//            .containsExactly(
//                NotificationStatus.SENT_TO_NOTIFY,
//                String.valueOf(expectedEmailResponse.successResponseObject().getNotificationId()),
//                FIXED_INSTANT,
//                null, // not failure reason as send request was successful
//                2, // as successfully sent again the retry count is incremented by 1
//                yesterday
//            );
//      }
//    }
//
//    @DisplayName("AND the sms is QUEUED")
//    @Nested
//    class AndSmsStatusIsQueued {
//
//      @DisplayName("THEN the sms is sent to notify")
//      @Test
//      void whenQueuedSmsNotification_andSuccessfulNotifyRequest_thenVerifySavedProperties() throws IOException {
//
//        var queuedNotification = NotificationTestUtil.builder()
//            .withType(NotificationType.SMS)
//            .withStatus(NotificationStatus.QUEUED)
//            .withLastSendAttemptAt(null)
//            .build();
//
//        givenDatabaseReturnsNotification(queuedNotification);
//
//        var fileData = readFileData("notifySendSmsResponse.json");
//
//        Response<SendSmsResponse> expectedSmsResponse = Response.successfulResponse(
//            new SendSmsResponse(new String(fileData))
//        );
//
//        given(govukNotifyService.sendSms(queuedNotification))
//            .willReturn(expectedSmsResponse);
//
//        notificationSendingService.sendNotificationsToNotify();
//
//        then(notificationRepository)
//            .should()
//            .save(notificationCaptor.capture());
//
//        var savedNotification = notificationCaptor.getValue();
//
//        assertThat(savedNotification)
//            .extracting(
//                Notification::getStatus,
//                Notification::getNotifyNotificationId,
//                Notification::getLastSendAttemptAt
//            )
//            .containsExactly(
//                NotificationStatus.SENT_TO_NOTIFY,
//                String.valueOf(expectedSmsResponse.successResponseObject().getNotificationId()),
//                FIXED_INSTANT
//            );
//
//        assertThat(savedNotification)
//            .extracting(Notification::getFailureReason, Notification::getRetryCount, Notification::getLastFailedAt)
//            .containsOnlyNulls();
//      }
//    }
//
//    @DisplayName("AND the sms is RETRY")
//    @Nested
//    class AndSmsStatusIsRetry {
//
//      @DisplayName("THEN the sms is sent to notify for its first retry attempt")
//      @Test
//      void whenFirstRetrySmsNotification_andSuccessfulNotifyRequest_thenVerifySavedProperties() throws IOException {
//
//        Instant yesterday = FIXED_CLOCK.instant().minus(1, ChronoUnit.DAYS);
//
//        var retryNotification = NotificationTestUtil.builder()
//            .withType(NotificationType.SMS)
//            .withStatus(NotificationStatus.RETRY)
//            .withLastSendAttemptAt(yesterday)
//            .withRetryCount(0)
//            .withLastFailedAt(yesterday)
//            .build();
//
//        givenDatabaseReturnsNotification(retryNotification);
//
//        var fileData = readFileData("notifySendSmsResponse.json");
//
//        Response<SendSmsResponse> expectedSmsResponse = Response.successfulResponse(
//            new SendSmsResponse(new String(fileData))
//        );
//
//        given(govukNotifyService.sendSms(retryNotification))
//            .willReturn(expectedSmsResponse);
//
//        notificationSendingService.sendNotificationsToNotify();
//
//        then(notificationRepository)
//            .should()
//            .save(notificationCaptor.capture());
//
//        var savedNotification = notificationCaptor.getValue();
//
//        assertThat(savedNotification)
//            .extracting(
//                Notification::getStatus,
//                Notification::getNotifyNotificationId,
//                Notification::getLastSendAttemptAt,
//                Notification::getFailureReason,
//                Notification::getRetryCount,
//                Notification::getLastFailedAt
//            )
//            .containsExactly(
//                NotificationStatus.SENT_TO_NOTIFY,
//                String.valueOf(expectedSmsResponse.successResponseObject().getNotificationId()),
//                FIXED_INSTANT,
//                null, // not failure reason as send request was successful
//                1, // as successfully sent again the retry count is incremented by 1
//                yesterday
//            );
//      }
//
//      @DisplayName("THEN the sms is sent to notify for its next retry attempt")
//      @Test
//      void whenSecondRetrySmsNotification_andSuccessfulNotifyRequest_thenVerifySavedProperties() throws IOException {
//
//        Instant yesterday = FIXED_CLOCK.instant().minus(1, ChronoUnit.DAYS);
//
//        var retryNotification = NotificationTestUtil.builder()
//            .withType(NotificationType.SMS)
//            .withStatus(NotificationStatus.RETRY)
//            .withLastSendAttemptAt(yesterday)
//            .withRetryCount(1)
//            .withLastFailedAt(yesterday)
//            .build();
//
//        givenDatabaseReturnsNotification(retryNotification);
//
//        var fileData = readFileData("notifySendSmsResponse.json");
//
//        Response<SendSmsResponse> expectedSmsResponse = Response.successfulResponse(
//            new SendSmsResponse(new String(fileData))
//        );
//
//        given(govukNotifyService.sendSms(retryNotification))
//            .willReturn(expectedSmsResponse);
//
//        notificationSendingService.sendNotificationsToNotify();
//
//        then(notificationRepository)
//            .should()
//            .save(notificationCaptor.capture());
//
//        var savedNotification = notificationCaptor.getValue();
//
//        assertThat(savedNotification)
//            .extracting(
//                Notification::getStatus,
//                Notification::getNotifyNotificationId,
//                Notification::getLastSendAttemptAt,
//                Notification::getFailureReason,
//                Notification::getRetryCount,
//                Notification::getLastFailedAt
//            )
//            .containsExactly(
//                NotificationStatus.SENT_TO_NOTIFY,
//                String.valueOf(expectedSmsResponse.successResponseObject().getNotificationId()),
//                FIXED_INSTANT,
//                null, // not failure reason as send request was successful
//                2, // as successfully sent again the retry count is incremented by 1
//                yesterday
//            );
//      }
//    }
//  }
//
//  @DisplayName("WHEN notify responds with a permanent client error")
//  @Nested
//  class WhenPermanentErrorFromNotify {
//
//    @DisplayName("AND we have an email notification")
//    @Nested
//    class AndEmailNotification {
//
//      @ParameterizedTest(name = "THEN the email is set to FAILED_NOT_SENT when a {0} response is returned from notify")
//      @ValueSource(ints = {403, 400})
//      void whenEmailNotification_andPermanentErrorNotifyResponse_thenVerifySavedProperties(int permanentErrorHttpStatus) {
//
//        var queuedNotification = NotificationTestUtil.builder()
//            .withType(NotificationType.EMAIL)
//            .withStatus(NotificationStatus.QUEUED)
//            .withLastSendAttemptAt(null)
//            .build();
//
//        givenDatabaseReturnsNotification(queuedNotification);
//
//        given(govukNotifyService.sendEmail(queuedNotification))
//            .willReturn(Response.failedResponse(permanentErrorHttpStatus, "error-message"));
//
//        notificationSendingService.sendNotificationsToNotify();
//
//        then(notificationRepository)
//            .should()
//            .save(notificationCaptor.capture());
//
//        var savedNotification = notificationCaptor.getValue();
//
//        assertThat(savedNotification)
//            .extracting(
//                Notification::getStatus,
//                Notification::getNotifyNotificationId,
//                Notification::getLastSendAttemptAt,
//                Notification::getLastFailedAt
//            )
//            .containsExactly(
//                NotificationStatus.FAILED_NOT_SENT,
//                null, // no notification ID from notify as we never successfully sent
//                FIXED_INSTANT,
//                FIXED_INSTANT
//            );
//
//        assertThat(savedNotification.getFailureReason()).isNotNull();
//      }
//    }
//
//    @DisplayName("AND we have an sms notification")
//    @Nested
//    class AndSmsNotification {
//
//      @ParameterizedTest(name = "THEN the sms is set to FAILED_NOT_SENT when a {0} response is returned from notify")
//      @ValueSource(ints = {403, 400})
//      void whenSmsNotification_andPermanentErrorNotifyResponse_thenVerifySavedProperties(int permanentErrorHttpStatus) {
//
//        var queuedNotification = NotificationTestUtil.builder()
//            .withType(NotificationType.SMS)
//            .withStatus(NotificationStatus.QUEUED)
//            .withLastSendAttemptAt(null)
//            .build();
//
//        givenDatabaseReturnsNotification(queuedNotification);
//
//        given(govukNotifyService.sendSms(queuedNotification))
//            .willReturn(Response.failedResponse(permanentErrorHttpStatus, "error-message"));
//
//        notificationSendingService.sendNotificationsToNotify();
//
//        then(notificationRepository)
//            .should()
//            .save(notificationCaptor.capture());
//
//        var savedNotification = notificationCaptor.getValue();
//
//        assertThat(savedNotification)
//            .extracting(
//                Notification::getStatus,
//                Notification::getNotifyNotificationId,
//                Notification::getLastSendAttemptAt,
//                Notification::getLastFailedAt
//            )
//            .containsExactly(
//                NotificationStatus.FAILED_NOT_SENT,
//                null, // no notification ID from notify as we never successfully sent
//                FIXED_INSTANT,
//                FIXED_INSTANT
//            );
//
//        assertThat(savedNotification.getFailureReason()).isNotNull();
//      }
//    }
//  }
//
//  @DisplayName("WHEN notify responds with a temporary client error")
//  @Nested
//  class WhenTemporaryErrorFromNotify {
//
//    @DisplayName("AND we have an email notification")
//    @Nested
//    class AndEmailNotification {
//
//      @DisplayName("THEN the email is set to FAILED_TO_SEND_TO_NOTIFY")
//      @Test
//      void whenEmailNotification_andTemporaryErrorNotifyResponse_thenVerifySavedProperties() {
//
//        var queuedNotification = NotificationTestUtil.builder()
//            .withType(NotificationType.EMAIL)
//            .withStatus(NotificationStatus.QUEUED)
//            .withLastSendAttemptAt(null)
//            .build();
//
//        givenDatabaseReturnsNotification(queuedNotification);
//
//        given(govukNotifyService.sendEmail(queuedNotification))
//            .willReturn(Response.failedResponse(500, "error-message"));
//
//        notificationSendingService.sendNotificationsToNotify();
//
//        then(notificationRepository)
//            .should()
//            .save(notificationCaptor.capture());
//
//        var savedNotification = notificationCaptor.getValue();
//
//        assertThat(savedNotification)
//            .extracting(
//                Notification::getStatus,
//                Notification::getNotifyNotificationId,
//                Notification::getLastSendAttemptAt,
//                Notification::getLastFailedAt
//            )
//            .containsExactly(
//                NotificationStatus.FAILED_TO_SEND_TO_NOTIFY,
//                null, // no notification ID from notify as we never successfully sent
//                FIXED_INSTANT,
//                FIXED_INSTANT
//            );
//
//        assertThat(savedNotification.getFailureReason()).isNotNull();
//      }
//    }
//
//    @DisplayName("AND we have an sms notification")
//    @Nested
//    class AndSmsNotification {
//
//      @DisplayName("THEN the sms is set to FAILED_TO_SEND_TO_NOTIFY")
//      @Test
//      void whenSmsNotification_andTemporaryErrorNotifyResponse_thenVerifySavedProperties() {
//
//        var queuedNotification = NotificationTestUtil.builder()
//            .withType(NotificationType.SMS)
//            .withStatus(NotificationStatus.QUEUED)
//            .withLastSendAttemptAt(null)
//            .build();
//
//        givenDatabaseReturnsNotification(queuedNotification);
//
//        given(govukNotifyService.sendSms(queuedNotification))
//            .willReturn(Response.failedResponse(500, "error-message"));
//
//        notificationSendingService.sendNotificationsToNotify();
//
//        then(notificationRepository)
//            .should()
//            .save(notificationCaptor.capture());
//
//        var savedNotification = notificationCaptor.getValue();
//
//        assertThat(savedNotification)
//            .extracting(
//                Notification::getStatus,
//                Notification::getNotifyNotificationId,
//                Notification::getLastSendAttemptAt,
//                Notification::getLastFailedAt
//            )
//            .containsExactly(
//                NotificationStatus.FAILED_TO_SEND_TO_NOTIFY,
//                null, // no notification ID from notify as we never successfully sent
//                FIXED_INSTANT,
//                FIXED_INSTANT
//            );
//
//        assertThat(savedNotification.getFailureReason()).isNotNull();
//      }
//    }
//  }
//
//  private byte[] readFileData(String resourceName) throws IOException {
//    var file = ResourceUtils.getFile(
//        "classpath:uk/co/fivium/digitalnotificationlibrary/core/notification/notify/" + resourceName
//    );
//    return Files.readAllBytes(file.toPath());
//  }
//
//  private void givenDatabaseReturnsNotifications(List<Notification> notifications) {
//    given(notificationRepository.findNotificationsByStatuses(
//        Set.of(NotificationStatus.QUEUED, NotificationStatus.RETRY),
//        PageRequest.of(0, BULK_RETRIEVAL_SIZE)
//    ))
//        .willReturn(notifications);
//  }
//
//  private void givenDatabaseReturnsNotification(Notification notification) {
//    givenDatabaseReturnsNotifications(List.of(notification));
//  }
//
//  private void givenDatabaseReturnsNoNotifications() {
//    givenDatabaseReturnsNotifications(Collections.emptyList());
//  }
}