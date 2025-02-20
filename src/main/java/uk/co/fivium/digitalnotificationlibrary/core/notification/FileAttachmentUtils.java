package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.Set;

class FileAttachmentUtils {

  private FileAttachmentUtils() {
    throw new IllegalStateException("This is a util class and should not be instantiated");
  }

  static Set<String> getValidFileExtensions() {
    return Set.of(
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
