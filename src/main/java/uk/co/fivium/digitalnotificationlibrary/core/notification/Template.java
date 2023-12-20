package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import uk.co.fivium.digitalnotificationlibrary.core.DigitalNotificationLibraryException;

/**
 * The libraries representation of a template from GOV.UK
 * @param notifyTemplateId The ID of the template on GOV.UK notify
 * @param type The type of the template
 * @param requiredMailMergeFields The mail merge fields that are required to be set for the template
 * @param verificationStatus Status indicating if the template is a confirmed notify template or not
 */
public record Template(
    String notifyTemplateId,
    TemplateType type,
    Set<String> requiredMailMergeFields,
    VerificationStatus verificationStatus
) {

  /**
   * An enum with values determining if we have a template known to notify or not.
   */
  public enum VerificationStatus {
    /** If this is a template we know exists in notify. */
    CONFIRMED_NOTIFY_TEMPLATE,
    /** If this is a template we don't know exists in notify. */
    UNCONFIRMED_NOTIFY_TEMPLATE
  }

  static Template createUnconfirmedTemplate(String notifyTemplateId) {
    return new Template(
        notifyTemplateId,
        TemplateType.UNKNOWN,
        Set.of(),
        VerificationStatus.UNCONFIRMED_NOTIFY_TEMPLATE
    );
  }

  static Template fromNotifyTemplate(uk.gov.service.notify.Template notifyTemplate) {

    var templateType = TemplateType.fromNotifyTemplateType(notifyTemplate.getTemplateType())
        .orElseThrow(() -> new DigitalNotificationLibraryException(
            "Template with ID %s does not have a supported template type %s use in this library"
                .formatted(notifyTemplate.getId(), notifyTemplate.getTemplateType())
        ));

    Set<String> requiredMailMergeFields = notifyTemplate.getPersonalisation()
        .map(Map::keySet)
        .orElse(Collections.emptySet());

    return new Template(
        String.valueOf(notifyTemplate.getId()),
        templateType,
        requiredMailMergeFields,
        VerificationStatus.CONFIRMED_NOTIFY_TEMPLATE
    );
  }
}