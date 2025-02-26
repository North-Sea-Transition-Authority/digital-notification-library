package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FileAttachmentUtilsTest {

  @Test
  void getValidFileExtensions() {
    assertThat(FileAttachmentUtils.getValidFileExtensions())
        .containsExactlyInAnyOrder(
            ".csv",
            ".jpeg",
            ".jpg",
            ".png",
            ".xlsx",
            ".doc",
            ".docx",
            ".pdf",
            ".json",
            ".odt",
            ".rtf",
            ".txt"
        );
  }

  @Test
  void getFileSizeLimit() {
    assertThat(FileAttachmentUtils.getFileSizeLimit())
        .isEqualTo(2 * 1024 * 1024);
  }

  @Test
  void getFileNameCharacterLimit() {
    assertThat(FileAttachmentUtils.getFileNameCharacterLimit())
        .isEqualTo(100);
  }
}