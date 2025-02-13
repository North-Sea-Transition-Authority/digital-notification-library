package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;

public interface EmailAttachmentResolver {

  byte[] resolveFileAttachment(UUID fileId) throws IOException;
}
