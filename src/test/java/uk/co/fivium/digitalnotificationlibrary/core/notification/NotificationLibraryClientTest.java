package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitalnotificationlibrary.core.DigitalNotificationLibraryException;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.fivium.digitalnotificationlibrary.core.notification.sms.SmsRecipient;

@ExtendWith(MockitoExtension.class)
class NotificationLibraryClientTest {

  static final Instant FIXED_INSTANT = Instant.now();

  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneId.systemDefault());

  @Mock
  private GovukNotifyService govukNotifyService;

  @Mock
  private NotificationLibraryNotificationRepository notificationRepository;

  @Captor
  private ArgumentCaptor<Notification> notificationCaptor;

  @InjectMocks
  private NotificationLibraryClient notificationLibraryClient;

  @BeforeEach
  void setup() {
    notificationLibraryClient = new NotificationLibraryClient(govukNotifyService, notificationRepository, FIXED_CLOCK);
  }

  @Test
  void getTemplate_whenNotifyTemplate_thenConfirmedTemplateReturned() {

    var templateId = UUID.randomUUID();

    var knownTemplate = NotifyTemplateTestUtil.builder()
        .withId(templateId)
        .withType(TemplateType.EMAIL)
        .withPersonalisation("field", "value")
        .build();

    given(govukNotifyService.getTemplate(templateId.toString()))
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

    given(govukNotifyService.getTemplate(templateId))
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

    given(govukNotifyService.getTemplate(templateId))
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
  @EnumSource(value = TemplateType.class, mode = EnumSource.Mode.EXCLUDE, names = "EMAIL")
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
        .hasMessage("Cannot send an email with template of type %s".formatted(nonEmailTemplateType));

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("Cannot send an email with template of type %s".formatted(nonEmailTemplateType));
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
        .hasMessage("EmailRecipient must not be null or empty");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipientWithNullOrEmptyEmail,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("EmailRecipient must not be null or empty");
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
        .hasMessage("EmailRecipient must not be null or empty");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            nullRecipient,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("EmailRecipient must not be null or empty");
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
        .hasMessage("DomainReference must not be null");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendEmail(
            mergedTemplate,
            recipient,
            nullDomainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("DomainReference must not be null");
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
            Notification::getNotifyStatus
        )
        .containsExactly(
            NotificationStatus.QUEUED,
            NotificationType.EMAIL,
            recipient.getEmailAddress(),
            domainReference.getId(),
            domainReference.getType(),
            template.notifyTemplateId(),
            FIXED_INSTANT,
            logCorrelationId,
            null // not sent to notify so no status set
        );

    assertThat(savedNotification.getMailMergeFields())
        .extracting(MailMergeField::name, MailMergeField::value)
        .containsExactlyInAnyOrder(
            tuple("field-1", "value-1"),
            tuple("field-2", "value-2")
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
            Notification::getNotifyStatus
        )
        .containsExactly(
            null, // log correlation id
            NotificationStatus.QUEUED,
            NotificationType.EMAIL,
            recipient.getEmailAddress(),
            domainReference.getId(),
            domainReference.getType(),
            template.notifyTemplateId(),
            FIXED_INSTANT,
            null // not sent to notify so no status set
        );

    assertThat(savedNotification.getMailMergeFields())
        .extracting(MailMergeField::name, MailMergeField::value)
        .containsExactlyInAnyOrder(
            tuple("field-1", "value-1"),
            tuple("field-2", "value-2")
        );
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
  @EnumSource(value = TemplateType.class, mode = EnumSource.Mode.EXCLUDE, names = "SMS")
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
        .hasMessage("Cannot send an sms with template of type %s".formatted(nonSmsTemplateType));

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendSms(
            mergedTemplate,
            recipient,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("Cannot send an sms with template of type %s".formatted(nonSmsTemplateType));
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
        .hasMessage("SmsRecipient must not be null or empty");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendSms(
            mergedTemplate,
            recipientWithNullOrEmptyNumber,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("SmsRecipient must not be null or empty");
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
        .hasMessage("SmsRecipient must not be null or empty");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendSms(
            mergedTemplate,
            nullRecipient,
            domainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("SmsRecipient must not be null or empty");
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
        .hasMessage("DomainReference must not be null");

    // without log correlation ID
    assertThatThrownBy(
        () -> notificationLibraryClient.sendSms(
            mergedTemplate,
            recipient,
            nullDomainReference
        )
    )
        .isInstanceOf(DigitalNotificationLibraryException.class)
        .hasMessage("DomainReference must not be null");
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
            Notification::getNotifyStatus
        )
        .containsExactly(
            NotificationStatus.QUEUED,
            NotificationType.SMS,
            recipient.getSmsRecipient(),
            domainReference.getId(),
            domainReference.getType(),
            template.notifyTemplateId(),
            FIXED_INSTANT,
            logCorrelationId,
            null // not sent to notify so no status set
        );

    assertThat(savedNotification.getMailMergeFields())
        .extracting(MailMergeField::name, MailMergeField::value)
        .containsExactlyInAnyOrder(
            tuple("field-1", "value-1"),
            tuple("field-2", "value-2")
        );
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
            Notification::getNotifyStatus
        )
        .containsExactly(
            null, // log correlation id
            NotificationStatus.QUEUED,
            NotificationType.SMS,
            recipient.getSmsRecipient(),
            domainReference.getId(),
            domainReference.getType(),
            template.notifyTemplateId(),
            FIXED_INSTANT,
            null // not sent to notify so no status set
        );

    assertThat(savedNotification.getMailMergeFields())
        .extracting(MailMergeField::name, MailMergeField::value)
        .containsExactlyInAnyOrder(
            tuple("field-1", "value-1"),
            tuple("field-2", "value-2")
        );
  }

  private MergedTemplate givenMergedTemplate(TemplateType type) {

    var template = TemplateTestUtil.builder()
        .withType(type)
        .build();

    return MergedTemplate.builder(template).merge();
  }
}
