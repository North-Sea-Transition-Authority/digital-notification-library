package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

@DisplayName("GIVEN I want to add a mail merge field or file attachment to a template")
class MergedTemplateTest {

  @DisplayName("WHEN a mail merge field is added")
  @Nested
  class WhenMailMergeFieldAdded {

    @DisplayName("THEN the mail merge field is added to the template")
    @Test
    void merge_verifyBuiltMergedTemplate() {

      var template = TemplateTestUtil.builder().build();

      var resultingMergedTemplate = MergedTemplate.builder(template)
          .withMailMergeField("field-key", "value")
          .merge();

      assertThat(resultingMergedTemplate.getTemplate()).isEqualTo(template);
      assertThat(resultingMergedTemplate.getMailMergeFields())
          .extracting(MailMergeField::name, MailMergeField::value)
          .containsExactly(tuple("field-key", "value"));
    }
  }

  @DisplayName("WHEN a mail merge field key is not provided")
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
          Arguments.of(Named.of("WHEN a null key is provided", null)),
          Arguments.of(Named.of("WHEN a empty String key is provided", ""))
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
          .withMailMergeField("field-key", "value")
          .withMailMergeField("field-key", "other-value")
          .merge();

      assertThat(mergedTemplate.getMailMergeFields())
          .extracting(MailMergeField::name, MailMergeField::value)
          .containsExactly(tuple("field-key", "other-value"));
    }
  }

  @DisplayName("WHEN a fileAttachment is added")
  @Nested
  class WhenFileAttachmentAdded {

    @DisplayName("THEN the file attachment is added to the template")
    @Test
    void merge_verifyBuiltMergedTemplate() {

      var template = TemplateTestUtil.builder().build();
      var fileId = UUID.randomUUID();

      var resultingMergedTemplate = MergedTemplate.builder(template)
          .withFileAttachment("field-key", fileId, "fileName")
          .merge();

      assertThat(resultingMergedTemplate.getTemplate()).isEqualTo(template);
      assertThat(resultingMergedTemplate.getFileAttachments())
          .extracting(FileAttachment::key, FileAttachment::fileId, FileAttachment::fileName)
          .containsExactly(tuple("field-key", fileId, "fileName"));
    }
  }

  @DisplayName("WHEN a file attachment key is not provided")
  @Nested
  class WhenNullOrEmptyKey {

    @DisplayName("THEN an exception is raised")
    @ParameterizedTest
    @MethodSource("nullOrEmptyArguments")
    void withFileAttachment_whenNullOrEmptyName_thenException(String nullOrEmptyName) {

      var template = TemplateTestUtil.builder().build();
      var mergedTemplateBuilder = MergedTemplate.builder(template);

      assertThatThrownBy(() -> mergedTemplateBuilder.withFileAttachment(nullOrEmptyName, UUID.randomUUID(), "fileName"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> nullOrEmptyArguments() {
      return Stream.of(
          Arguments.of(Named.of("WHEN a null key is provided", null)),
          Arguments.of(Named.of("WHEN a empty String key is provided", ""))
      );
    }
  }

  @DisplayName("WHEN a file attachment id is not provided")
  @Nested
  class WhenNullId {

    @DisplayName("THEN an exception is raised")
    @Test
    void withFileAttachment_whenNullOrEmptyId_thenException() {

      var template = TemplateTestUtil.builder().build();
      var mergedTemplateBuilder = MergedTemplate.builder(template);

      assertThatThrownBy(() -> mergedTemplateBuilder.withFileAttachment("link_to_file", null, "fileName"))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @DisplayName("WHEN a file attachment name is not provided")
  @Nested
  class WhenNullOrEmptyFileName {

    @DisplayName("THEN an exception is raised")
    @ParameterizedTest
    @MethodSource("nullOrEmptyArguments")
    void withFileAttachment_whenNullOrEmptyName_thenException(String nullOrEmptyName) {

      var template = TemplateTestUtil.builder().build();
      var mergedTemplateBuilder = MergedTemplate.builder(template);

      assertThatThrownBy(() -> mergedTemplateBuilder.withFileAttachment("link_to_file", UUID.randomUUID(), nullOrEmptyName))
          .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> nullOrEmptyArguments() {
      return Stream.of(
          Arguments.of(Named.of("WHEN a null file name is provided", null)),
          Arguments.of(Named.of("WHEN a empty String file name is provided", ""))
      );
    }
  }
//
//  @DisplayName("WHEN a null or empty set of file attachments are provided")
//  @Nested
//  class WhenNullOrEmptyFileAttachments {
//
//    @ParameterizedTest
//    @MethodSource("nullOrEmptyArguments")
//    void withFileAttachments_whenNullOrEmptySet_thenEmptySet(Set<FileAttachment> nullOrEmptyFileAttachments) {
//
//      var template = TemplateTestUtil.builder().build();
//
//      var resultingMergedTemplate = MergedTemplate.builder(template)
//          .withFileAttachments(nullOrEmptyFileAttachments)
//          .merge();
//
//      assertThat(resultingMergedTemplate.getFileAttachments()).isEmpty();
//    }
//
//    private static Stream<Arguments> nullOrEmptyArguments() {
//      return Stream.of(
//          Arguments.of(Named.of("WHEN a null set is provided", null)),
//          Arguments.of(Named.of("WHEN a empty set is provided", Collections.emptySet()))
//      );
//    }
//  }
}