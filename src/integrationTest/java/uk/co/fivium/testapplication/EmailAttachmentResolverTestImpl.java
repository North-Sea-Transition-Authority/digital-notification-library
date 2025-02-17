package uk.co.fivium.testapplication;

import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitalnotificationlibrary.core.notification.EmailAttachmentResolver;

@Service
public class EmailAttachmentResolverTestImpl implements EmailAttachmentResolver {

  public byte[] resolveFileAttachment(UUID fileId) throws IOException {
    return new byte[]{1, 2, 3};
  }
}
