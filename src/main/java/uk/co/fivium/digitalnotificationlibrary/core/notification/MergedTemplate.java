package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Class representing a template with the mail merge fields to include.
 */
public class MergedTemplate {

  private final Template template;

  private final Set<MailMergeField> mailMergeFields;

  protected MergedTemplate(Template template, Set<MailMergeField> mailMergeFields) {
    this.template = template;
    this.mailMergeFields = mailMergeFields;
  }

  /**
   * Get the template.
   * @return the template
   */
  public Template getTemplate() {
    return template;
  }

  /**
   * Get the mail merge fields for the template.
   * @return the mail merge fields
   */
  public Set<MailMergeField> getMailMergeFields() {
    return mailMergeFields;
  }

  /**
   * Get an instantiated MergedTemplateBuilder object.
   * @param template The template to use to construct this merge template
   * @return An instance of the MergedTemplateBuilder with the provided template
   */
  public static MergedTemplateBuilder builder(Template template) {
    return new MergedTemplateBuilder(template);
  }

  /**
   * Class for constructing a merged template object.
   */
  public static class MergedTemplateBuilder {

    protected final Template template;

    protected final Map<String, Object> mailMergeFields = new HashMap<>();

    MergedTemplateBuilder(Template template) {
      this.template = template;
    }

    /**
     * Utility method to add a mail merge field.
     * @param name The key of the mail merge field
     * @param value The value of the mail merge field
     * @return The builder
     */
    public MergedTemplateBuilder withMailMergeField(String name, Object value) {

      if (StringUtils.isNotBlank(name)) {
        mailMergeFields.put(name, value);
      } else {
        throw new IllegalArgumentException("A non empty mail merge field key must be provided");
      }

      return this;
    }

    /**
     * Utility method to add a collection of mail merge fields.
     * @param mailMergeFields The collection of mail merge fields to add
     * @return The builder
     */
    public MergedTemplateBuilder withMailMergeFields(Set<MailMergeField> mailMergeFields) {
      if (CollectionUtils.isNotEmpty(mailMergeFields)) {
        mailMergeFields.forEach(mailMergeField ->
            withMailMergeField(mailMergeField.name(), mailMergeField.value())
        );
      }
      return this;
    }

    /**
     * Utility method to add a file attachment.
     * @param mailMergeFieldName The name of the mail merge field for this attachment
     * @param fileId The file id
     * @param fileName The file name which must end with a file E.g. .pdf or .csv
     * @return The builder
     */
    public MergedTemplateWithFiles.MergedTemplateWithFilesBuilder withFileAttachment(String mailMergeFieldName, UUID fileId, String fileName) {
      return MergedTemplateWithFiles.builder(this)
          .withFileAttachment(mailMergeFieldName, fileId, fileName);
    }

    /**
     * Utility method to add a collection of file attachments.
     * @param fileAttachments The collection of file attachments to add
     * @return The builder
     */
    public MergedTemplateBuilder withFileAttachments(Set<FileAttachment> fileAttachments) {
      return MergedTemplateWithFiles.builder(this)
          .withFileAttachments(fileAttachments);
    }

    /**
     * Utility method to create a new merged template.
     * @return an instantiated merged template
     */
    public MergedTemplate merge() {

      Set<MailMergeField> mailMergeFieldSet = mailMergeFields
          .entrySet()
          .stream()
          .map(field -> new MailMergeField(field.getKey(), field.getValue()))
          .collect(Collectors.toSet());

      return new MergedTemplate(template, mailMergeFieldSet);
    }
  }
}
