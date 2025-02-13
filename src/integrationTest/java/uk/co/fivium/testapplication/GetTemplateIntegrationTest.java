package uk.co.fivium.testapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.fivium.digitalnotificationlibrary.core.DigitalNotificationLibraryException;
import uk.co.fivium.digitalnotificationlibrary.core.notification.NotificationLibraryClient;
import uk.co.fivium.digitalnotificationlibrary.core.notification.Template;
import uk.co.fivium.digitalnotificationlibrary.core.notification.TemplateType;

//@IntegrationTest
//@DisplayName("GIVEN I want to get a template")
class GetTemplateIntegrationTest {

//  @Autowired
//  private NotificationLibraryClient notificationLibraryClient;
//
//  @Nested
//  @DisplayName("WHEN the template exists in notify")
//  class WhenTemplateKnownToNotify {
//
//    @Test
//    @DisplayName("THEN a template is returned")
//    void whenTemplateExistsInNotify() {
//
//      var template = GovukNotifyTemplate.EMAIL_TEMPLATE;
//
//      var resultingTemplate = notificationLibraryClient.getTemplate(template.getGovukNotifyTemplateId());
//
//      assertThat(resultingTemplate)
//          .extracting(
//              Template::notifyTemplateId,
//              Template::type,
//              Template::verificationStatus
//          )
//          .contains(
//              template.getGovukNotifyTemplateId(),
//              TemplateType.EMAIL,
//              Template.VerificationStatus.CONFIRMED_NOTIFY_TEMPLATE
//          );
//
//      assertThat(resultingTemplate.requiredMailMergeFields())
//          .containsExactlyInAnyOrder("name", "reference");
//    }
//  }
//
//  @Nested
//  @DisplayName("WHEN the template doesn't exists in notify")
//  class WhenTemplateNotKnownToNotify {
//
//    @Test
//    @DisplayName("THEN an exception is raised")
//    void whenIdHasValidNotifyFormat_andNoTemplateExists() {
//
//      var validFormatTemplateId = UUID.randomUUID().toString();
//
//      assertThatThrownBy(() -> notificationLibraryClient.getTemplate(validFormatTemplateId))
//          .isInstanceOf(DigitalNotificationLibraryException.class);
//    }
//  }
//
//  @Nested
//  @DisplayName("WHEN the template ID is not in the notify format")
//  class WhenTemplateIdHasIncorrectFormat {
//
//    @Test
//    @DisplayName("THEN an exception is raised")
//    void getTemplate_whenNotValidNotifyTemplateIdFormat() {
//
//      var invalidFormatTemplateId = "not-a-uuid-format";
//
//      assertThatThrownBy(() -> notificationLibraryClient.getTemplate(invalidFormatTemplateId))
//          .isInstanceOf(DigitalNotificationLibraryException.class);
//    }
//  }
}
