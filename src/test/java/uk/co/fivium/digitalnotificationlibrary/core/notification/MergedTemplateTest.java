package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class MergedTemplateTest {

  @ParameterizedTest
  @NullAndEmptySource
  void withMailMergeField_whenNullOrEmptyName_thenException(String nullOrEmptyName) {

    var template = TemplateTestUtil.builder().build();
    var mergedTemplateBuilder = MergedTemplate.builder(template);

    assertThatThrownBy(() -> mergedTemplateBuilder.withMailMergeField(nullOrEmptyName, "value"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void withMailMergeFields_whenNullOrEmptySet_thenEmptySet(Set<MailMergeField> nullOrEmptyMailMergeFields) {

    var template = TemplateTestUtil.builder().build();

    var resultingMergedTemplate = MergedTemplate.builder(template)
        .withMailMergeFields(nullOrEmptyMailMergeFields)
        .merge();

    assertThat(resultingMergedTemplate.getMailMergeFields()).isEmpty();
  }

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
