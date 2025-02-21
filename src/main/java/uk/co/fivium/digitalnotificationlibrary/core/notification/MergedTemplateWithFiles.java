package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class MergedTemplateWithFiles extends MergedTemplate {

  private final Set<FileAttachment> fileAttachments;

  protected MergedTemplateWithFiles(Template template, Set<MailMergeField> mailMergeFields, Set<FileAttachment> fileAttachments) {
    super(template, mailMergeFields);
    this.fileAttachments = fileAttachments;
  }

  /**
   * Get the field attachments for the template.
   * @return the field attachments
   */
  public Set<FileAttachment> getFileAttachments() {
    return fileAttachments;
  }

  /**
   * Get an instantiated MergedTemplateBuilder object.
   * @param mergedTemplateBuilder The template to use to construct this merge template
   * @return An instance of the MergedTemplateBuilder with the provided template
   */
  public static MergedTemplateWithFilesBuilder builder(MergedTemplateBuilder mergedTemplateBuilder) {
    return new MergedTemplateWithFilesBuilder(mergedTemplateBuilder);
  }

  /**
   * Class for constructing a merged template object.
   */
  public static class MergedTemplateWithFilesBuilder extends MergedTemplate.MergedTemplateBuilder {
    private final Set<FileAttachment> fileAttachments = new HashSet<>();

    MergedTemplateWithFilesBuilder(MergedTemplateBuilder mergedTemplateBuilder) {
      super(mergedTemplateBuilder.merge().getTemplate());
      super.withMailMergeFields(mergedTemplateBuilder.merge().getMailMergeFields());
    }

    /**
     * Utility method to add a file attachment.
     * @param mailMergeFieldName The name of the mail merge field for this attachment
     * @param fileId The file id
     * @param fileName The file name which must end with a file E.g. .pdf or .csv
     * @return The builder
     */
    @Override
    public MergedTemplateWithFilesBuilder withFileAttachment(String mailMergeFieldName, UUID fileId, String fileName) {

      if (StringUtils.isBlank(mailMergeFieldName)) {
        throw new IllegalArgumentException("A non empty file attachment mailMergeFieldName must be provided");
      } else if (Objects.isNull(fileId)) {
        throw new IllegalArgumentException("A non empty file attachment fileId must be provided");
      } else if (StringUtils.isBlank(fileName)) {
        throw new IllegalArgumentException("A non empty file attachment name must be provided");
      }

      fileAttachments.add(new FileAttachment(mailMergeFieldName, fileId, fileName));
      return this;
    }

    /**
     * Utility method to add a collection of file attachments.
     * @param fileAttachments The collection of file attachments to add
     * @return The builder
     */
    @Override
    public MergedTemplateWithFilesBuilder withFileAttachments(Set<FileAttachment> fileAttachments) {
      if (CollectionUtils.isNotEmpty(fileAttachments)) {
        fileAttachments.forEach(fileAttachment ->
            withFileAttachment(fileAttachment.key(), fileAttachment.fileId(), fileAttachment.fileName())
        );
      }
      return this;
    }

    @Override
    public MergedTemplateWithFilesBuilder withMailMergeField(String name, Object value) {
      super.withMailMergeField(name, value);
      return this;
    }

    @Override
    public MergedTemplateWithFilesBuilder withMailMergeFields(Set<MailMergeField> mailMergeFields) {
      super.withMailMergeFields(mailMergeFields);
      return this;
    }

    /**
     * Utility method to create a new merged template with files.
     * @return an instantiated merged template with files.
     */
    @Override
    public MergedTemplateWithFiles merge() {

      Set<MailMergeField> mailMergeFieldSet = mailMergeFields
          .entrySet()
          .stream()
          .map(field -> new MailMergeField(field.getKey(), field.getValue()))
          .collect(Collectors.toSet());

      return new MergedTemplateWithFiles(template, mailMergeFieldSet, fileAttachments);
    }

  }

}
