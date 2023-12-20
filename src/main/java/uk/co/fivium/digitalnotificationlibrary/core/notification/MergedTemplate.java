package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.HashSet;
import java.util.Set;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Class representing a template with the mail merge fields to include.
 */
public class MergedTemplate {

  private final Template template;

  private final Set<MailMergeField> mailMergeFields;

  private MergedTemplate(Template template, Set<MailMergeField> mailMergeFields) {
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

    private final Template template;

    private final Set<MailMergeField> mailMergeFields = new HashSet<>();

    MergedTemplateBuilder(Template template) {
      this.template = template;
    }

    /**
     * Utility method to add a mail merge field.
     * @param name The name of the mail merge field
     * @param value The value of the mail merge field
     * @return The builder
     */
    public MergedTemplateBuilder withMailMergeField(String name, Object value) {

      if (StringUtils.hasText(name)) {
        mailMergeFields.add(new MailMergeField(name, value));
      } else {
        throw new IllegalArgumentException("A non empty mail merge field name must be provided");
      }

      return this;
    }

    /**
     * Utility method to add a collection of mail merge fields.
     * @param mailMergeFields The collection of mail merge fields to add
     * @return The builder
     */
    public MergedTemplateBuilder withMailMergeFields(Set<MailMergeField> mailMergeFields) {
      if (!CollectionUtils.isEmpty(mailMergeFields)) {
        mailMergeFields.forEach(mailMergeField ->
            withMailMergeField(mailMergeField.name(), mailMergeField.value())
        );
      }
      return this;
    }

    /**
     * Utility method to create a new merged template.
     * @return an instantiated merged template
     */
    public MergedTemplate merge() {
      return new MergedTemplate(template, mailMergeFields);
    }
  }
}
