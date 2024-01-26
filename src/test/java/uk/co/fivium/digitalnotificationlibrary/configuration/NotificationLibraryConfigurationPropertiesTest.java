package uk.co.fivium.digitalnotificationlibrary.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BeanPropertyBindingResult;

@DisplayName("GIVEN I want to configure the library")
class NotificationLibraryConfigurationPropertiesTest {

  private static final NotificationLibraryConfigurationPropertiesTestUtil.Builder propertiesBuilder
      = NotificationLibraryConfigurationPropertiesTestUtil.builder();

  @DisplayName("WHEN I am in test mode")
  @Nested
  class WhenTestMode {

    @BeforeAll
    static void setup() {
      propertiesBuilder.withMode(NotificationMode.TEST);
    }

    @DisplayName("AND I have set all test recipients")
    @Nested
    class AndNoTestRecipients {

      @DisplayName("THEN there will be no errors in the library properties")
      @Test
      void whenNoTestRecipients() {

        propertiesBuilder
            .withTestEmailRecipients(Set.of("someone@example.com"))
            .withTestSmsRecipients(Set.of("0123456789"));

        var bindingResult = new BeanPropertyBindingResult(propertiesBuilder.build(), "properties");

        propertiesBuilder.build().validate(propertiesBuilder.build(), bindingResult);

        assertFalse(bindingResult.hasFieldErrors());
      }
    }

    @DisplayName("AND I don't set any test email recipients")
    @Nested
    class AndNoTestEmailRecipients {

      @DisplayName("THEN there will be errors in the library properties")
      @ParameterizedTest
      @ArgumentsSource(NullOrEmptyRecipients.class)
      void whenNoEmailRecipients(Set<String> nullOrEmptyEmailRecipients) {

        propertiesBuilder
            .withTestEmailRecipients(nullOrEmptyEmailRecipients)
            .withTestSmsRecipients(Set.of("0123456789"));

        var bindingResult = new BeanPropertyBindingResult(propertiesBuilder.build(), "properties");

        propertiesBuilder.build().validate(propertiesBuilder.build(), bindingResult);

        assertThat(bindingResult.getAllErrors())
            .extracting(DefaultMessageSourceResolvable::getDefaultMessage)
            .containsExactly("You must set test email and sms recipients when in test mode");
      }
    }

    @DisplayName("AND I don't set any test sms recipients")
    @Nested
    class AndNoTestSmsRecipients {

      @DisplayName("THEN there will be errors in the library properties")
      @ParameterizedTest
      @ArgumentsSource(NullOrEmptyRecipients.class)
      void whenNoSmsRecipients(Set<String> nullOrEmptySmsRecipients) {

        propertiesBuilder
            .withTestSmsRecipients(nullOrEmptySmsRecipients)
            .withTestEmailRecipients(Set.of("someone@example.com"));

        var bindingResult = new BeanPropertyBindingResult(propertiesBuilder.build(), "properties");

        propertiesBuilder.build().validate(propertiesBuilder.build(), bindingResult);

        assertThat(bindingResult.getAllErrors())
            .extracting(DefaultMessageSourceResolvable::getDefaultMessage)
            .containsExactly("You must set test email and sms recipients when in test mode");
      }
    }

    static class NullOrEmptyRecipients implements ArgumentsProvider {

      @Override
      public Stream<Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
            Arguments.of(Named.of("WHEN null recipients are provided", null)),
            Arguments.of(Named.of("WHEN an empty set of recipients are provided", Set.of()))
        );
      }
    }
  }

  @DisplayName("WHEN I am in production mode")
  @Nested
  class WhenProductionMode {

    @BeforeAll
    static void setup() {
      propertiesBuilder.withMode(NotificationMode.PRODUCTION);
    }

    @DisplayName("AND I have not set any test recipients")
    @Nested
    class AndNoTestRecipients {

      @DisplayName("THEN there will be no errors in the library properties")
      @Test
      void whenNoTestRecipients() {

        propertiesBuilder
            .withTestEmailRecipients(Set.of())
            .withTestSmsRecipients(Set.of());

        var bindingResult = new BeanPropertyBindingResult(propertiesBuilder.build(), "properties");

        propertiesBuilder.build().validate(propertiesBuilder.build(), bindingResult);

        assertFalse(bindingResult.hasFieldErrors());
      }
    }

    @DisplayName("AND I have set a test email recipient")
    @Nested
    class AndTestEmailRecipients {

      @DisplayName("THEN there will be no errors in the library properties")
      @Test
      void whenTestEmailRecipient() {

        propertiesBuilder
            .withTestEmailRecipients(Set.of("someone@example.com"))
            .withTestSmsRecipients(Set.of());

        var bindingResult = new BeanPropertyBindingResult(propertiesBuilder.build(), "properties");

        propertiesBuilder.build().validate(propertiesBuilder.build(), bindingResult);

        assertFalse(bindingResult.hasFieldErrors());
      }
    }

    @DisplayName("AND I have set a test sms recipient")
    @Nested
    class AndTestSmsRecipients {

      @DisplayName("THEN there will be no errors in the library properties")
      @Test
      void whenTestSmsRecipient() {

        propertiesBuilder
            .withTestSmsRecipients(Set.of("0123456789"))
            .withTestEmailRecipients(Set.of());

        var bindingResult = new BeanPropertyBindingResult(propertiesBuilder.build(), "properties");

        propertiesBuilder.build().validate(propertiesBuilder.build(), bindingResult);

        assertFalse(bindingResult.hasFieldErrors());
      }
    }
  }
}
