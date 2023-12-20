package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.Arrays;
import java.util.Optional;

/**
 * An enum with template types used by the library.
 */
public enum TemplateType {
  /** For email type templates. */
  EMAIL("email"),
  /** For SMS type templates. */
  SMS("sms"),
  /** For any templates where we couldn't contain notify to verify the type. */
  UNKNOWN;

  private final String notifyTemplateType;

  TemplateType(String notifyTemplateType) {
    this.notifyTemplateType = notifyTemplateType;
  }

  TemplateType() {
    this.notifyTemplateType = null;
  }


  /**
   * Get the type of the notify template.
   * @return The type of template
   */
  public String getNotifyTemplateType() {
    return notifyTemplateType;
  }

  static Optional<TemplateType> fromNotifyTemplateType(String notifyTemplateType) {
    return Arrays.stream(TemplateType.values())
        .filter(templateType ->
            templateType.getNotifyTemplateType() != null
                && templateType.getNotifyTemplateType().equals(notifyTemplateType)
        )
        .findFirst();
  }
}
