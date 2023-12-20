package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationLibraryClientTest {

  @Mock
  private GovukNotifyService govukNotifyService;

  @InjectMocks
  private NotificationLibraryClient notificationLibraryClient;

  @Test
  void getTemplate_whenNotifyTemplate_thenConfirmedTemplateReturned() {

    var templateId = UUID.randomUUID();

    var knownTemplate = NotifyTemplateTestUtil.builder()
        .withId(templateId)
        .withType(TemplateType.EMAIL)
        .withPersonalisation("field", "value")
        .build();

    given(govukNotifyService.getTemplate(templateId.toString()))
        .willReturn(Optional.of(knownTemplate));

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

  @Test
  void getTemplate_whenNotifyReturnsEmptyOptional_thenUnconfirmedTemplateReturned() {

    var templateId = "unknown-template-id";

    given(govukNotifyService.getTemplate(templateId))
        .willReturn(Optional.empty());

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
}
