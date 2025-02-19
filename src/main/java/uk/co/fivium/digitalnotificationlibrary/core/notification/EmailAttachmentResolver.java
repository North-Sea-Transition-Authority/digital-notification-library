package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The interface that the consumer should implement if file attachments are required.
 */
public interface EmailAttachmentResolver {

  byte[] resolveFileAttachment(UUID fileId);
}
