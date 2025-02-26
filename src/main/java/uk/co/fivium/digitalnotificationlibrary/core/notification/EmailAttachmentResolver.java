package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.UUID;

/**
 * The interface that the consumer should implement if file attachments are required.
 */
public interface EmailAttachmentResolver {

  byte[] resolveFileAttachment(UUID fileId);
}
