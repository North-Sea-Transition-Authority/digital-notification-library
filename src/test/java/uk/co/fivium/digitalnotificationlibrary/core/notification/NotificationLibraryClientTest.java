package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationPropertiesTestUtil;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationMode;
import uk.co.fivium.digitalnotificationlibrary.core.DigitalNotificationLibraryException;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.fivium.digitalnotificationlibrary.core.notification.sms.SmsRecipient;

@ExtendWith(MockitoExtension.class)
class NotificationLibraryClientTest {

  static final Instant FIXED_INSTANT = Instant.now();

  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneId.systemDefault());

  @Mock
  private TemplateService templateService;

  @Mock
  private NotificationLibraryNotificationRepository notificationRepository;

  @Captor
  private ArgumentCaptor<Notification> notificationCaptor;

  private NotificationLibraryConfigurationProperties libraryConfigurationProperties;

  private NotificationLibraryClient notificationLibraryClient;

  @Mock
  private NotificationLibraryEmailAttachmentResolver emailAttachmentResolver;

  @BeforeEach
  void setup() {
    libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder().build();
    notificationLibraryClient = new NotificationLibraryClient(
        notificationRepository,
        templateService,
        FIXED_CLOCK,
        libraryConfigurationProperties,
        emailAttachmentResolver
    );
  }

  @Test
  void getTemplate_whenNotifyTemplate_thenConfirmedTemplateReturned() {

    var templateId = UUID.randomUUID();

    var knownTemplate = NotifyTemplateTestUtil.builder()
        .withId(templateId)
        .withType(TemplateType.EMAIL)
        .withPersonalisation("field", "value")
        .build();

    given(templateService.getTemplate(templateId.toString()))
        .willReturn(Response.successfulResponse(knownTemplate));

    var resultingTemplate = notificationLibraryClient.getTemplate(templateId.toString());

    assertThat(resultingTemplate)
        .extracting(
            Template::notifyTemplateId,
            Template::type,
            Template::requiredMailMergeFields,
            Template::verificationStatus
        )
        .containsExactly(
            String.valueOf(templateId),
            TemplateType.EMAIL,
            Set.of("field"),
            Template.VerificationStatus.CONFIRMED_NOTIFY_TEMPLATE
        );
  }

  @ParameterizedTest
  @ValueSource(ints = {500, 503})
  void getTemplate_whenNotifyError_thenUnconfirmedTemplateReturned(int notifyHttpResponseStatusResultingInNoException) {

    var templateId = "unknown-template-id";

    given(templateService.getTemplate(templateId))
        .willReturn(Response.failedResponse(notifyHttpResponseStatusResultingInNoException, "error-message"));

    var resultingTemplate = notificationLibraryClient.getTemplate(templateId);

    assertThat(resultingTemplate)
        .extracting(
            Template::notifyTemplateId,
            Template::type,
            Template::requiredMailMergeFields,
            Template::verificationStatus
        )
        .containsExactly(
            templateId,
            TemplateType.UNKNOWN,
            Set.of(),
            Template.VerificationStatus.UNCONFIRMED_NOTIFY_TEMPLATE
        );
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 403, 404})
  void getTemplate_whenConsumerError_thenException(int notifyHttpResponseStatusResultingInException) {

    var templateId = "unknown-template-id";

    given(templateService.getTemplate(templateId))
        .willReturn(Response.failedResponse(notifyHttpResponseStatusResultingInException, "error-message"));

    assertThatThrownBy(() -> notificationLibraryClient.getTemplate(templateId))
        .isInstanceOf(DigitalNotificationLibraryException.class);
  }

  @Test
  void sendEmail_whenMergedTemplateIsNull_thenException() {

    MergedTemplate nullMergedTemplate = null;

    var recipient = EmailRecipient.directEmailAddress("someone@example.com");
    var domainReference = DomainReference.from("id", "type");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            nullMergedTemplate,
            recipient,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("MergedTemplate must not be null");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            nullMergedTemplate,
            recipient,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("MergedTemplate must not be null");
  }

  @ParameterizedTest
  @EnumSource(value = TemplateType.class, mode = EnumSource.Mode.EXCLUDE, names = {"EMAIL", "UNKNOWN"})
  void sendEmail_whenTemplateTypeIsNotEmail_thenException(TemplateType nonEmailTemplateType) {

    var nonEmailTemplate = TemplateTestUtil.builder()
        .withType(nonEmailTemplateType)
        .build();

    var mergedTemplate = MergedTemplate.builder(nonEmailTemplate).merge();

    var recipient = EmailRecipient.directEmailAddress("someone@example.com");
    var domainReference = DomainReference.from("id", "type");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("Cannot send an email for template with ID %s and type %s"
            .formatted(mergedTemplate.getTemplate().notifyTemplateId(), nonEmailTemplateType)
        );

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("Cannot send an email for template with ID %s and type %s"
            .formatted(mergedTemplate.getTemplate().notifyTemplateId(), nonEmailTemplateType)
        );
  }

  @ParameterizedTest
  @NullAndEmptySource
  void sendEmail_whenRecipientEmailIsNullOrEmpty_thenException(String nullOrEmptyRecipientEmail) {

    EmailRecipient recipientWithNullOrEmptyEmail = EmailRecipient.directEmailAddress(nullOrEmptyRecipientEmail);

    var mergedTemplate = givenMergedTemplate(TemplateType.EMAIL);
    var domainReference = DomainReference.from("id", "type");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipientWithNullOrEmptyEmail,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("EmailRecipient must not be null or empty for notification with correlation ID log-correlation-id");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipientWithNullOrEmptyEmail,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("EmailRecipient must not be null or empty for notification with correlation ID null");
  }

  @Test
  void sendEmail_whenRecipientIsNull_thenException() {

    EmailRecipient nullRecipient = null;

    var mergedTemplate = givenMergedTemplate(TemplateType.EMAIL);
    var domainReference = DomainReference.from("id", "type");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            nullRecipient,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("EmailRecipient must not be null or empty for notification with correlation ID log-correlation-id");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            nullRecipient,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("EmailRecipient must not be null or empty for notification with correlation ID null");
  }

  @Test
  void sendEmail_whenDomainReferenceIsNull_thenException() {

    DomainReference nullDomainReference = null;

    var mergedTemplate = givenMergedTemplate(TemplateType.EMAIL);
    var recipient = EmailRecipient.directEmailAddress("someone@example.com");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            nullDomainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("DomainReference must not be null for notification with correlation ID log-correlation-id");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            nullDomainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("DomainReference must not be null for notification with correlation ID null");
  }

  @Test
  void sendEmail_whenWrongMergedTemplateIsPassedIn_thenException() {

    EmailRecipient nullRecipient = null;

    var mergedTemplate = givenMergedTemplateWithFiles(UUID.randomUUID(), "name");
    var domainReference = DomainReference.from("id", "type");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            (MergedTemplate) mergedTemplate,
            nullRecipient,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("MergedTemplate parameter must not be an instance of MergedTemplateWithFiles");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            (MergedTemplate) mergedTemplate,
            nullRecipient,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("MergedTemplate parameter must not be an instance of MergedTemplateWithFiles");
  }

  @Test
  void sendEmail_withLogCorrelationId_verifyQueuedNotification() {

    var template = TemplateTestUtil.builder()
        .withNotifyTemplateId("notify-template-id")
        .withType(TemplateType.EMAIL)
        .build();

    var mergedTemplate = MergedTemplate.builder(template)
        .withMailMergeField("field-1", "value-1")
        .withMailMergeField("field-2", "value-2")
        .merge();

    var recipient = EmailRecipient.directEmailAddress("someone@example.com");

    var domainReference = DomainReference.from("domain-id", "domain-type");

    var logCorrelationId = "log-correlation-id";

    notificationLibraryClient.sendEmail(
        mergedTemplate,
        recipient,
        domainReference,
        logCorrelationId
    );

    then(notificationRepository)
        .should()
        .save(notificationCaptor.capture());

    var savedNotification = notificationCaptor.getValue();

    assertThat(savedNotification)
        .extracting(
            Notification::getStatus,
            Notification::getType,
            Notification::getRecipient,
            Notification::getDomainReferenceId,
            Notification::getDomainReferenceType,
            Notification::getNotifyTemplateId,
            Notification::getRequestedOn,
            Notification::getLogCorrelationId,
            Notification::getNotifyStatus,
            Notification::getRetryCount
        )
        .containsExactly(
            NotificationStatus.QUEUED,
            NotificationType.EMAIL,
            recipient.getEmailAddress(),
            domainReference.getDomainId(),
            domainReference.getDomainType(),
            template.notifyTemplateId(),
            FIXED_INSTANT,
            logCorrelationId,
            null, // not sent to notify so no status set
            0 // default retry count
        );
  }

  @Test
  void sendEmail_withoutLogCorrelationId_verifyQueuedNotification() {

    var template = TemplateTestUtil.builder()
        .withNotifyTemplateId("notify-template-id")
        .withType(TemplateType.EMAIL)
        .build();

    var mergedTemplate = MergedTemplate.builder(template)
        .withMailMergeField("field-1", "value-1")
        .withMailMergeField("field-2", "value-2")
        .merge();

    var recipient = EmailRecipient.directEmailAddress("someone@example.com");

    var domainReference = DomainReference.from("domain-id", "domain-type");

    notificationLibraryClient.sendEmail(
        mergedTemplate,
        recipient,
        domainReference
    );

    then(notificationRepository)
        .should()
        .save(notificationCaptor.capture());

    var savedNotification = notificationCaptor.getValue();

    assertThat(savedNotification)
        .extracting(
            Notification::getLogCorrelationId,
            Notification::getStatus,
            Notification::getType,
            Notification::getRecipient,
            Notification::getDomainReferenceId,
            Notification::getDomainReferenceType,
            Notification::getNotifyTemplateId,
            Notification::getRequestedOn,
            Notification::getNotifyStatus,
            Notification::getRetryCount
        )
        .containsExactly(
            null, // log correlation id
            NotificationStatus.QUEUED,
            NotificationType.EMAIL,
            recipient.getEmailAddress(),
            domainReference.getDomainId(),
            domainReference.getDomainType(),
            template.notifyTemplateId(),
            FIXED_INSTANT,
            null, // not sent to notify so no status set
            0 // default retry count
        );
  }

  @Test
  void sendEmail_withFiles_whenMergedTemplateIsNull_thenException() {

    MergedTemplateWithFiles nullMergedTemplate = null;

    var recipient = EmailRecipient.directEmailAddress("someone@example.com");
    var domainReference = DomainReference.from("id", "type");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            nullMergedTemplate,
            recipient,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("MergedTemplate must not be null");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            nullMergedTemplate,
            recipient,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("MergedTemplate must not be null");
  }

  @ParameterizedTest
  @EnumSource(value = TemplateType.class, mode = EnumSource.Mode.EXCLUDE, names = {"EMAIL", "UNKNOWN"})
  void sendEmail_withFiles_whenTemplateTypeIsNotEmail_thenException(TemplateType nonEmailTemplateType) {

    var nonEmailTemplate = TemplateTestUtil.builder()
        .withType(nonEmailTemplateType)
        .build();

    var mergedTemplate = MergedTemplateWithFiles.builder(nonEmailTemplate).merge();

    var recipient = EmailRecipient.directEmailAddress("someone@example.com");
    var domainReference = DomainReference.from("id", "type");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("Cannot send an email for template with ID %s and type %s"
            .formatted(mergedTemplate.getTemplate().notifyTemplateId(), nonEmailTemplateType)
        );

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("Cannot send an email for template with ID %s and type %s"
            .formatted(mergedTemplate.getTemplate().notifyTemplateId(), nonEmailTemplateType)
        );
  }

  @ParameterizedTest
  @NullAndEmptySource
  void sendEmail_withFiles_whenRecipientEmailIsNullOrEmpty_thenException(String nullOrEmptyRecipientEmail) {

    EmailRecipient recipientWithNullOrEmptyEmail = EmailRecipient.directEmailAddress(nullOrEmptyRecipientEmail);

    var mergedTemplate = givenMergedTemplateWithFiles(UUID.randomUUID(), "filename.pdf");
    var domainReference = DomainReference.from("id", "type");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipientWithNullOrEmptyEmail,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("EmailRecipient must not be null or empty for notification with correlation ID log-correlation-id");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipientWithNullOrEmptyEmail,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("EmailRecipient must not be null or empty for notification with correlation ID null");
  }

  @Test
  void sendEmail_withFiles_whenRecipientIsNull_thenException() {

    EmailRecipient nullRecipient = null;

    var mergedTemplate = givenMergedTemplateWithFiles(UUID.randomUUID(), "filename.pdf");
    var domainReference = DomainReference.from("id", "type");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            nullRecipient,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("EmailRecipient must not be null or empty for notification with correlation ID log-correlation-id");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            nullRecipient,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("EmailRecipient must not be null or empty for notification with correlation ID null");
  }

  @Test
  void sendEmail_withFiles_whenDomainReferenceIsNull_thenException() {

    DomainReference nullDomainReference = null;

    var mergedTemplate = givenMergedTemplateWithFiles(UUID.randomUUID(), "filename.pdf");
    var recipient = EmailRecipient.directEmailAddress("someone@example.com");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            nullDomainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("DomainReference must not be null for notification with correlation ID log-correlation-id");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            nullDomainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("DomainReference must not be null for notification with correlation ID null");
  }

  @Test
  void sendEmail_withFiles_whenFileIsTooLarge_thenException() {
    var fileId = UUID.randomUUID();
    var mergedTemplate = givenMergedTemplateWithFiles(fileId, "filename.pdf");
    var recipient = EmailRecipient.directEmailAddress("someone@example.com");
    var domainReference = DomainReference.from("id", "type");
    var fileContents = new byte[(2 * 1024 * 1024 + 1)];

    given(emailAttachmentResolver.resolveFileAttachment(fileId)).willReturn(fileContents);

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(NotificationLibraryFileException.class)
        .hasMessage("File attachment cannot be bigger than 2MB");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            domainReference
        )
    )
        .isInstanceOf(NotificationLibraryFileException.class)
        .hasMessage("File attachment cannot be bigger than 2MB");
  }

  @Test
  void sendEmail_withFiles_whenFileNameIsInvalid_thenException() {
    var fileId = UUID.randomUUID();
    var recipient = EmailRecipient.directEmailAddress("someone@example.com");
    var domainReference = DomainReference.from("id", "type");
    var fileContents = new byte[]{};
    var nameWithOver100Characters = StringUtils.repeat("*", 101);
    var mergedTemplate = givenMergedTemplateWithFiles(fileId, nameWithOver100Characters);

    given(emailAttachmentResolver.resolveFileAttachment(fileId)).willReturn(fileContents);

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(NotificationLibraryFileException.class)
        .hasMessage("File name must have 100 characters or less.");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            domainReference
        )
    )
        .isInstanceOf(NotificationLibraryFileException.class)
        .hasMessage("File name must have 100 characters or less.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"filename", "filename.eml"})
  void sendEmail_withFiles_whenFileHasNoExtension_thenException(String invalidFileExtension) {
    var fileId = UUID.randomUUID();
    var recipient = EmailRecipient.directEmailAddress("someone@example.com");
    var domainReference = DomainReference.from("id", "type");
    var fileContents = new byte[]{};
    var mergedTemplate = givenMergedTemplateWithFiles(fileId, invalidFileExtension);

    given(emailAttachmentResolver.resolveFileAttachment(fileId)).willReturn(fileContents);

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(NotificationLibraryFileException.class)
        .hasMessage("File name must include a valid file extension");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            domainReference
        )
    )
        .isInstanceOf(NotificationLibraryFileException.class)
        .hasMessage("File name must include a valid file extension");
  }

  @Test
  void sendEmail_withFiles_withLogCorrelationId_verifyQueuedNotification() throws NotificationLibraryFileException {

    var template = TemplateTestUtil.builder()
        .withNotifyTemplateId("notify-template-id")
        .withType(TemplateType.EMAIL)
        .build();

    var fileId1 = UUID.randomUUID();

    var mergedTemplate = MergedTemplate.builder(template)
        .withMailMergeField("field-1", "value-1")
        .withMailMergeField("field-2", "value-2")
        .withFileAttachment("file 1", fileId1, "file name 1.pdf")
        .merge();

    var recipient = EmailRecipient.directEmailAddress("someone@example.com");

    var domainReference = DomainReference.from("domain-id", "domain-type");

    var logCorrelationId = "log-correlation-id";

    when(emailAttachmentResolver.resolveFileAttachment(fileId1)).thenReturn(new byte[]{1, 2, 3});

    notificationLibraryClient.sendEmail(
        mergedTemplate,
        recipient,
        domainReference,
        logCorrelationId
    );

    then(notificationRepository)
        .should()
        .save(notificationCaptor.capture());

    var savedNotification = notificationCaptor.getValue();

    assertThat(savedNotification)
        .extracting(
            Notification::getStatus,
            Notification::getType,
            Notification::getRecipient,
            Notification::getDomainReferenceId,
            Notification::getDomainReferenceType,
            Notification::getNotifyTemplateId,
            Notification::getRequestedOn,
            Notification::getLogCorrelationId,
            Notification::getNotifyStatus,
            Notification::getRetryCount
        )
        .containsExactly(
            NotificationStatus.QUEUED,
            NotificationType.EMAIL,
            recipient.getEmailAddress(),
            domainReference.getDomainId(),
            domainReference.getDomainType(),
            template.notifyTemplateId(),
            FIXED_INSTANT,
            logCorrelationId,
            null, // not sent to notify so no status set
            0 // default retry count
        );

    assertThat(savedNotification.getMailMergeFields())
        .extracting(MailMergeField::name, MailMergeField::value)
        .containsExactlyInAnyOrder(
            tuple("field-1", "value-1"),
            tuple("field-2", "value-2")
        );

    assertThat(savedNotification.getFileAttachments())
        .extracting(FileAttachment::key, FileAttachment::fileId, FileAttachment::fileName)
        .containsExactlyInAnyOrder(
            tuple("file 1", fileId1, "file name 1.pdf")
        );
  }

  @Test
  void sendEmail_withFiles_withoutLogCorrelationId_verifyQueuedNotification() throws NotificationLibraryFileException {

    var template = TemplateTestUtil.builder()
        .withNotifyTemplateId("notify-template-id")
        .withType(TemplateType.EMAIL)
        .build();

    var fileId1 = UUID.randomUUID();

    var mergedTemplate = MergedTemplate.builder(template)
        .withMailMergeField("field-1", "value-1")
        .withMailMergeField("field-2", "value-2")
        .withFileAttachment("file 1", fileId1, "file name 1.pdf")
        .merge();

    var recipient = EmailRecipient.directEmailAddress("someone@example.com");

    var domainReference = DomainReference.from("domain-id", "domain-type");

    when(emailAttachmentResolver.resolveFileAttachment(fileId1)).thenReturn(new byte[]{1, 2, 3});

    notificationLibraryClient.sendEmail(
        mergedTemplate,
        recipient,
        domainReference
    );

    then(notificationRepository)
        .should()
        .save(notificationCaptor.capture());

    var savedNotification = notificationCaptor.getValue();

    assertThat(savedNotification)
        .extracting(
            Notification::getStatus,
            Notification::getType,
            Notification::getRecipient,
            Notification::getDomainReferenceId,
            Notification::getDomainReferenceType,
            Notification::getNotifyTemplateId,
            Notification::getRequestedOn,
            Notification::getLogCorrelationId,
            Notification::getNotifyStatus,
            Notification::getRetryCount
        )
        .containsExactly(
            NotificationStatus.QUEUED,
            NotificationType.EMAIL,
            recipient.getEmailAddress(),
            domainReference.getDomainId(),
            domainReference.getDomainType(),
            template.notifyTemplateId(),
            FIXED_INSTANT,
            null,
            null, // not sent to notify so no status set
            0 // default retry count
        );

    assertThat(savedNotification.getMailMergeFields())
        .extracting(MailMergeField::name, MailMergeField::value)
        .containsExactlyInAnyOrder(
            tuple("field-1", "value-1"),
            tuple("field-2", "value-2")
        );

    assertThat(savedNotification.getFileAttachments())
        .extracting(FileAttachment::key, FileAttachment::fileId, FileAttachment::fileName)
        .containsExactlyInAnyOrder(tuple("file 1", fileId1, "file name 1.pdf"));
  }


  @Test
  void sendSms_whenMergedTemplateIsNull_thenException() {

    MergedTemplate nullMergedTemplate = null;

    var recipient = SmsRecipient.directPhoneNumber("01234567890");
    var domainReference = DomainReference.from("id", "type");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendSms(
            nullMergedTemplate,
            recipient,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("MergedTemplate must not be null");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendSms(
            nullMergedTemplate,
            recipient,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("MergedTemplate must not be null");
  }

  @ParameterizedTest
  @EnumSource(value = TemplateType.class, mode = EnumSource.Mode.EXCLUDE, names = {"SMS", "UNKNOWN"})
  void sendSms_whenTemplateTypeIsNotEmail_thenException(TemplateType nonSmsTemplateType) {

    var nonEmailTemplate = TemplateTestUtil.builder()
        .withType(nonSmsTemplateType)
        .build();

    var mergedTemplate = MergedTemplate.builder(nonEmailTemplate).merge();

    var recipient = SmsRecipient.directPhoneNumber("0123456789");
    var domainReference = DomainReference.from("id", "type");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendSms(
            mergedTemplate,
            recipient,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("Cannot send an sms for template with ID %s and type %s"
            .formatted(mergedTemplate.getTemplate().notifyTemplateId(), nonSmsTemplateType)
        );

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendSms(
            mergedTemplate,
            recipient,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("Cannot send an sms for template with ID %s and type %s"
            .formatted(mergedTemplate.getTemplate().notifyTemplateId(), nonSmsTemplateType)
        );
  }

  @ParameterizedTest
  @NullAndEmptySource
  void sendSms_whenRecipientNumberIsNullOrEmpty_thenException(String nullOrEmptyRecipientNumber) {

    SmsRecipient recipientWithNullOrEmptyNumber = SmsRecipient.directPhoneNumber(nullOrEmptyRecipientNumber);

    var mergedTemplate = givenMergedTemplate(TemplateType.SMS);
    var domainReference = DomainReference.from("id", "type");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendSms(
            mergedTemplate,
            recipientWithNullOrEmptyNumber,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("SmsRecipient must not be null or empty for notification with correlation ID log-correlation-id");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendSms(
            mergedTemplate,
            recipientWithNullOrEmptyNumber,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("SmsRecipient must not be null or empty for notification with correlation ID null");
  }

  @Test
  void sendSms_whenRecipientIsNull_thenException() {

    SmsRecipient nullRecipient = null;

    var mergedTemplate = givenMergedTemplate(TemplateType.SMS);
    var domainReference = DomainReference.from("id", "type");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendSms(
            mergedTemplate,
            nullRecipient,
            domainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("SmsRecipient must not be null or empty for notification with correlation ID log-correlation-id");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendSms(
            mergedTemplate,
            nullRecipient,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("SmsRecipient must not be null or empty for notification with correlation ID null");
  }

  @Test
  void sendSms_whenDomainReferenceIsNull_thenException() {

    DomainReference nullDomainReference = null;

    var mergedTemplate = givenMergedTemplate(TemplateType.SMS);
    var recipient = SmsRecipient.directPhoneNumber("01234567890");

    // with log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendSms(
            mergedTemplate,
            recipient,
            nullDomainReference,
            "log-correlation-id"
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("DomainReference must not be null for notification with correlation ID log-correlation-id");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendSms(
            mergedTemplate,
            recipient,
            nullDomainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("DomainReference must not be null for notification with correlation ID null");
  }

  @Test
  void sendSms_withLogCorrelationId_verifyQueuedNotification() {

    var template = TemplateTestUtil.builder()
        .withNotifyTemplateId("notify-template-id")
        .withType(TemplateType.SMS)
        .build();

    var mergedTemplate = MergedTemplate.builder(template)
        .withMailMergeField("field-1", "value-1")
        .withMailMergeField("field-2", "value-2")
        .merge();

    var recipient = SmsRecipient.directPhoneNumber("01234567890");

    var domainReference = DomainReference.from("domain-id", "domain-type");

    var logCorrelationId = "log-correlation-id";

    notificationLibraryClient.sendSms(
        mergedTemplate,
        recipient,
        domainReference,
        logCorrelationId
    );

    then(notificationRepository)
        .should()
        .save(notificationCaptor.capture());

    var savedNotification = notificationCaptor.getValue();

    assertThat(savedNotification)
        .extracting(
            Notification::getStatus,
            Notification::getType,
            Notification::getRecipient,
            Notification::getDomainReferenceId,
            Notification::getDomainReferenceType,
            Notification::getNotifyTemplateId,
            Notification::getRequestedOn,
            Notification::getLogCorrelationId,
            Notification::getNotifyStatus,
            Notification::getRetryCount
        )
        .containsExactly(
            NotificationStatus.QUEUED,
            NotificationType.SMS,
            recipient.getSmsRecipient(),
            domainReference.getDomainId(),
            domainReference.getDomainType(),
            template.notifyTemplateId(),
            FIXED_INSTANT,
            logCorrelationId,
            null, // not sent to notify so no status set
            0 // default retry count
        );

    assertThat(savedNotification.getMailMergeFields())
        .extracting(MailMergeField::name, MailMergeField::value)
        .containsExactlyInAnyOrder(
            tuple("field-1", "value-1"),
            tuple("field-2", "value-2")
        );

    assertThat(savedNotification.getFileAttachments()).isEmpty();
  }

  @Test
  void sendSms_withoutLogCorrelationId_verifyQueuedNotification() {

    var template = TemplateTestUtil.builder()
        .withNotifyTemplateId("notify-template-id")
        .withType(TemplateType.SMS)
        .build();

    var mergedTemplate = MergedTemplate.builder(template)
        .withMailMergeField("field-1", "value-1")
        .withMailMergeField("field-2", "value-2")
        .merge();

    var recipient = SmsRecipient.directPhoneNumber("01234567890");

    var domainReference = DomainReference.from("domain-id", "domain-type");

    notificationLibraryClient.sendSms(
        mergedTemplate,
        recipient,
        domainReference
    );

    then(notificationRepository)
        .should()
        .save(notificationCaptor.capture());

    var savedNotification = notificationCaptor.getValue();

    assertThat(savedNotification)
        .extracting(
            Notification::getLogCorrelationId,
            Notification::getStatus,
            Notification::getType,
            Notification::getRecipient,
            Notification::getDomainReferenceId,
            Notification::getDomainReferenceType,
            Notification::getNotifyTemplateId,
            Notification::getRequestedOn,
            Notification::getNotifyStatus,
            Notification::getRetryCount
        )
        .containsExactly(
            null, // log correlation id
            NotificationStatus.QUEUED,
            NotificationType.SMS,
            recipient.getSmsRecipient(),
            domainReference.getDomainId(),
            domainReference.getDomainType(),
            template.notifyTemplateId(),
            FIXED_INSTANT,
            null, // not sent to notify so no status set
            0 // default retry count
        );

    assertThat(savedNotification.getMailMergeFields())
        .extracting(MailMergeField::name, MailMergeField::value)
        .containsExactlyInAnyOrder(
            tuple("field-1", "value-1"),
            tuple("field-2", "value-2")
        );

    assertThat(savedNotification.getFileAttachments()).isEmpty();
  }

  @Test
  void isRunningTestMode_whenTestMode_thenTrue() {

    libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
        .withMode(NotificationMode.TEST)
        .build();

    notificationLibraryClient = new NotificationLibraryClient(
        notificationRepository,
        templateService,
        FIXED_CLOCK,
        libraryConfigurationProperties,
        emailAttachmentResolver
    );

    assertTrue(notificationLibraryClient.isRunningTestMode());
  }

  @ParameterizedTest
  @EnumSource(value = NotificationMode.class, mode = EnumSource.Mode.EXCLUDE, names = "TEST")
  void isRunningTestMode_whenNotTestMode_thenFalse(NotificationMode nonTestMode) {

    libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
        .withMode(nonTestMode)
        .build();

    notificationLibraryClient = new NotificationLibraryClient(
        notificationRepository,
        templateService,
        FIXED_CLOCK,
        libraryConfigurationProperties,
        emailAttachmentResolver
    );

    assertFalse(notificationLibraryClient.isRunningTestMode());
  }

  @Test
  void isRunningProductionMode_whenProductionMode_thenTrue() {

    libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
        .withMode(NotificationMode.PRODUCTION)
        .build();

    notificationLibraryClient = new NotificationLibraryClient(
        notificationRepository,
        templateService,
        FIXED_CLOCK,
        libraryConfigurationProperties,
        emailAttachmentResolver
    );

    assertTrue(notificationLibraryClient.isRunningProductionMode());
  }

  @ParameterizedTest
  @EnumSource(value = NotificationMode.class, mode = EnumSource.Mode.EXCLUDE, names = "PRODUCTION")
  void isRunningProductionMode_whenNotProductionMode_thenFalse(NotificationMode nonProductionMode) {

    libraryConfigurationProperties = NotificationLibraryConfigurationPropertiesTestUtil.builder()
        .withMode(nonProductionMode)
        .build();

    notificationLibraryClient = new NotificationLibraryClient(
        notificationRepository,
        templateService,
        FIXED_CLOCK,
        libraryConfigurationProperties,
        emailAttachmentResolver
    );

    assertFalse(notificationLibraryClient.isRunningProductionMode());
  }

  @Test
  void isFileAttachable_fileTooLarge() {
    var maxFileLength = 2 * 1024 * 1024;
    var attachableFileResult = notificationLibraryClient.isFileAttachable(maxFileLength + 1, "validFileName.pdf");
    assertThat(attachableFileResult).isEqualTo(AttachableFileResult.FILE_TOO_LARGE);
  }

  @Test
  void isFileAttachable_invalidFileName() {
    var nameWithOver100Characters = StringUtils.repeat("*", 101);
    var attachableFileResult = notificationLibraryClient.isFileAttachable(5000, nameWithOver100Characters);
    assertThat(attachableFileResult).isEqualTo(AttachableFileResult.INVALID_FILE_NAME);
  }

  @Test
  void isFileAttachable_incorrectFileExtension() {
    var invalidFileExtension = "filename.eml";
    var attachableFileResult = notificationLibraryClient.isFileAttachable(5000, invalidFileExtension);
    assertThat(attachableFileResult).isEqualTo(AttachableFileResult.INCORRECT_FILE_EXTENSION);
  }

  @Test
  void isFileAttachable_noFileExtension() {
    var noFileExtension = "filename";
    var attachableFileResult = notificationLibraryClient.isFileAttachable(5000, noFileExtension);
    assertThat(attachableFileResult).isEqualTo(AttachableFileResult.INCORRECT_FILE_EXTENSION);
  }

  @Test
  void isFileAttachable_success() {
    var attachableFileResult = notificationLibraryClient.isFileAttachable(5000, "validFileName.pdf");
    assertThat(attachableFileResult).isEqualTo(AttachableFileResult.SUCCESS);
  }

  private MergedTemplate givenMergedTemplate(TemplateType type) {

    var template = TemplateTestUtil.builder()
        .withType(type)
        .build();

    return MergedTemplate.builder(template).merge();
  }

  private MergedTemplateWithFiles givenMergedTemplateWithFiles(UUID fileId, String fileName) {

    var template = TemplateTestUtil.builder()
        .withType(TemplateType.EMAIL)
        .build();

    return MergedTemplateWithFiles.builder(template)
        .withFileAttachment("link_to_file", fileId, fileName)
        .merge();
  }
}
