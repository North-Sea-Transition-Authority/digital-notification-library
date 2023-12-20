package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.HashSet;
import java.util.Set;

class TemplateTestUtil {

  private TemplateTestUtil() {
    throw new IllegalStateException("This is a utility class and should not be instantiated");
  }

  static Builder builder() {
    return new Builder();
  }

  static class Builder {

    private String notifyTemplateId = "notify-template-id";

    private TemplateType type = TemplateType.EMAIL;

    private final Set<String> requiredMailMergeFields = new HashSet<>();

    private Template.VerificationStatus verificationStatus = Template.VerificationStatus.CONFIRMED_NOTIFY_TEMPLATE;

    private Builder() {
    }

    Builder withNotifyTemplateId(String notifyTemplateId) {
      this.notifyTemplateId = notifyTemplateId;
      return this;
    }

    Builder withType(TemplateType type) {
      this.type = type;
      return this;
    }

    Builder withMailMergeField(String name) {
      this.requiredMailMergeFields.add(name);
      return this;
    }

    Builder withVerificationStatus(Template.VerificationStatus verificationStatus) {
      this.verificationStatus = verificationStatus;
      return this;
    }

    Template build() {
      return new Template(
          notifyTemplateId,
          type,
          requiredMailMergeFields,
          verificationStatus
      );
    }

  }
}
