package uk.co.fivium.testapplication;

import static com.github.tomakehurst.wiremock.client.WireMock.forbidden;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.co.fivium.digitalnotificationlibrary.core.DigitalNotificationLibraryException;
import uk.co.fivium.digitalnotificationlibrary.core.notification.NotificationLibraryClient;
import uk.co.fivium.digitalnotificationlibrary.core.notification.Template;
import uk.co.fivium.digitalnotificationlibrary.core.notification.TemplateType;

@IntegrationTest
@WireMockTest
class NotificationLibraryClientIntegrationTest {

  private static String notifyBaseUrl;

  @Autowired
  private NotificationLibraryClient notificationLibraryClient;

  @BeforeAll
  static void setup(WireMockRuntimeInfo wireMockRuntimeInfo) {
    notifyBaseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
  }

  @DynamicPropertySource
  private static void configure(DynamicPropertyRegistry registry) {
    registry.add("digital-notification-library.govuk-notify.base-url", () -> notifyBaseUrl);
  }

  @Test
  void getTemplate_whenOkResponse_thenVerifyConfirmedMappedTemplate() {

    var templateId = UUID.randomUUID().toString();

    var resultingNotifyTemplateJson = NotifyTemplateTestUtil.builder()
        .withId(templateId)
        .withType(TemplateType.EMAIL)
        .withPersonalisation("field-1", "value-1")
        .withPersonalisation("field-2", "value-2")
        .buildAsJsonObject();

    stubFor(get(urlEqualTo("/v2/template/%s".formatted(templateId)))
        .willReturn(ok()
            .withHeader("Content-Type", "application/json")
            .withBody(String.valueOf(resultingNotifyTemplateJson))
        ));

    Template resultingTemplate = notificationLibraryClient.getTemplate(templateId);

    assertThat(resultingTemplate)
        .extracting(
            Template::notifyTemplateId,
            Template::type,
            Template::verificationStatus
        )
        .containsExactly(
            templateId,
            TemplateType.EMAIL,
            Template.VerificationStatus.CONFIRMED_NOTIFY_TEMPLATE
        );

    assertThat(resultingTemplate.requiredMailMergeFields())
        .containsExactlyInAnyOrder("field-1", "field-2");
  }

  @Test
  void getTemplate_whenTemplateTypeNotSupported_thenException() {

    var templateId = UUID.randomUUID().toString();

    var resultingNotifyTemplateJson = NotifyTemplateTestUtil.builder()
        .withType("not-supported")
        .withId(templateId)
        .buildAsJsonObject();

    stubFor(get(urlEqualTo("/v2/template/%s".formatted(templateId)))
        .willReturn(ok()
            .withHeader("Content-Type", "application/json")
            .withBody(String.valueOf(resultingNotifyTemplateJson))
        ));

    assertThatThrownBy(() -> notificationLibraryClient.getTemplate(templateId))
        .isInstanceOf(DigitalNotificationLibraryException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = { 500, 503, 404, 400 })
  void getTemplate_whenNonOkAndNonForbiddenResponse_thenVerifyUnconfirmedMappedTemplate(int nonOkHttpResponseStatus) {

    var templateId = UUID.randomUUID().toString();

    stubFor(get(urlEqualTo("/v2/template/%s".formatted(templateId)))
        .willReturn(status(nonOkHttpResponseStatus)));

    Template resultingTemplate = notificationLibraryClient.getTemplate(templateId);

    assertThat(resultingTemplate)
        .extracting(
            Template::notifyTemplateId,
            Template::type,
            Template::verificationStatus,
            Template::requiredMailMergeFields
        )
        .contains(
            templateId,
            TemplateType.UNKNOWN,
            Template.VerificationStatus.UNCONFIRMED_NOTIFY_TEMPLATE,
            Set.of()
        );
  }

  @Test
  void getTemplate_whenForbiddenResponse_thenException() {

    var templateId = UUID.randomUUID().toString();

    stubFor(get(urlEqualTo("/v2/template/%s".formatted(templateId)))
        .willReturn(forbidden()));

    assertThatThrownBy(() -> notificationLibraryClient.getTemplate(templateId))
        .isInstanceOf(DigitalNotificationLibraryException.class);
  }
}
