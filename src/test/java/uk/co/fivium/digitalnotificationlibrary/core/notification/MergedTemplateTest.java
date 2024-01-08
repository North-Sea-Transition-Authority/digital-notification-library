package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("GIVEN I want to add a mail merge field to a template")
class MergedTemplateTest {

  @DisplayName("WHEN a mail merge field is added")
  @Nested
  class WhenMailMergeFieldAdded {

    @DisplayName("THEN the mail merge field is added to the template")
    @Test
    void merge_verifyBuiltMergedTemplate() {

      var template = TemplateTestUtil.builder().build();

      var resultingMergedTemplate = MergedTemplate.builder(template)
          .withMailMergeField("field-name", "value")
          .merge();

      assertThat(resultingMergedTemplate.getTemplate()).isEqualTo(template);
      assertThat(resultingMergedTemplate.getMailMergeFields())
          .extracting(MailMergeField::name, MailMergeField::value)
          .containsExactly(tuple("field-name", "value"));
    }
  }

  @DisplayName("WHEN a mail merge field name is not provided")
  @Nested
  class WhenNullOrEmptyName {

    @DisplayName("THEN an exception is raised")
    @ParameterizedTest
    @MethodSource("nullOrEmptyArguments")
    void withMailMergeField_whenNullOrEmptyName_thenException(String nullOrEmptyName) {

      var template = TemplateTestUtil.builder().build();
      var mergedTemplateBuilder = MergedTemplate.builder(template);

      assertThatThrownBy(() -> mergedTemplateBuilder.withMailMergeField(nullOrEmptyName, "value"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> nullOrEmptyArguments() {
      return Stream.of(
          Arguments.of(Named.of("WHEN a null name is provided", null)),
          Arguments.of(Named.of("WHEN a empty String name is provided", ""))
      );
    }
  }

  @DisplayName("WHEN a null or empty set of mail merge fields are provided")
  @Nested
  class WhenNullOrEmptyMailMergeFields {

    @ParameterizedTest
    @MethodSource("nullOrEmptyArguments")
    void withMailMergeFields_whenNullOrEmptySet_thenEmptySet(Set<MailMergeField> nullOrEmptyMailMergeFields) {

      var template = TemplateTestUtil.builder().build();

      var resultingMergedTemplate = MergedTemplate.builder(template)
          .withMailMergeFields(nullOrEmptyMailMergeFields)
          .merge();

      assertThat(resultingMergedTemplate.getMailMergeFields()).isEmpty();
    }

    private static Stream<Arguments> nullOrEmptyArguments() {
      return Stream.of(
          Arguments.of(Named.of("WHEN a null set is provided", null)),
          Arguments.of(Named.of("WHEN a empty set is provided", Collections.emptySet()))
      );
    }
  }

  @DisplayName("WHEN a mail merge field already exists")
  @Nested
  class WhenMailMergeFieldAlreadyExists {

    @DisplayName("THEN the most recent entry overwrites the previous one")
    @Test
    void withMailMergeField_whenFieldAlreadyExistsByName_thenValueOverwritten() {

      var template = TemplateTestUtil.builder().build();

      var mergedTemplate = MergedTemplate.builder(template)
          .withMailMergeField("field-name", "value")
          .withMailMergeField("field-name", "other-value")
          .merge();

      assertThat(mergedTemplate.getMailMergeFields())
          .extracting(MailMergeField::name, MailMergeField::value)
          .containsExactly(tuple("field-name", "other-value"));
    }
  }
}