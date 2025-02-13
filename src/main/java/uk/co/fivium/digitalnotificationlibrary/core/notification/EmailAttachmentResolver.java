package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.UUID;

public interface EmailAttachmentResolver {

  Byte[] resolveFileAttachment(UUID fileId);
}
