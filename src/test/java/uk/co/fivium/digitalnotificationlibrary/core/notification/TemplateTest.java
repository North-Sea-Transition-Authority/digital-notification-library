package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.fivium.digitalnotificationlibrary.core.DigitalNotificationLibraryException;

class TemplateTest {

  @Test
  void fromNotifyTemplate_whenUnsupportedNotificationType_thenException() {

    var notifyTemplateWithUnsupportedType = NotifyTemplateTestUtil.builder()
        .withType("not-supported")
        .build();

    assertThrows(
        DigitalNotificationLibraryException.class,
        () -> Template.fromNotifyTemplate(notifyTemplateWithUnsupportedType)
    );
  }

  @Test
  void fromNotifyTemplate_thenVerifyTemplate() {

    var templateId = UUID.randomUUID();

    var notifyTemplate = NotifyTemplateTestUtil.builder()
        .withId(templateId)
        .withType(TemplateType.SMS)
        .withPersonalisation("field-1", "value-1")
        .withPersonalisation("field-2", "value-2")
        .build();

    var resultingTemplate = Template.fromNotifyTemplate(notifyTemplate);

    assertThat(resultingTemplate)
        .extracting(
            Template::notifyTemplateId,
            Template::type,
            Template::verificationStatus
        )
        .containsExactly(
            String.valueOf(templateId),
            TemplateType.SMS,
            Template.VerificationStatus.CONFIRMED_NOTIFY_TEMPLATE
        );

    assertThat(resultingTemplate.requiredMailMergeFields())
        .containsExactlyInAnyOrder("field-1", "field-2");
  }

  @Test
  void fromNotifyTemplate_whenNoMailMergeFields_thenEmptySetInResultingTemplate() {

    var templateId = UUID.randomUUID();

    var notifyTemplate = NotifyTemplateTestUtil.builder()
        .withId(templateId)
        .withType(TemplateType.SMS)
        .withPersonalisation(Collections.emptyMap())
        .build();

    var resultingTemplate = Template.fromNotifyTemplate(notifyTemplate);

    assertThat(resultingTemplate)
        .extracting(
            Template::notifyTemplateId,
            Template::type,
            Template::verificationStatus
        )
        .containsExactly(
            String.valueOf(templateId),
            TemplateType.SMS,
            Template.VerificationStatus.CONFIRMED_NOTIFY_TEMPLATE
        );

    assertThat(resultingTemplate.requiredMailMergeFields()).isEmpty();
  }
}