package uk.co.fivium.digitalnotificationlibrary.core.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
}