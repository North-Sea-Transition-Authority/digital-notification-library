package uk.co.fivium.testapplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitalnotificationlibrary.core.notification.EmailAttachmentResolver;

@Service
public class EmailAttachmentResolverTestImpl implements EmailAttachmentResolver {

  public byte[] resolveFileAttachment(UUID fileId) throws IOException {
    var initialFile = new File("src/main/resources/TestDocument.pdf");
    return new FileInputStream(initialFile).readAllBytes();
  }
}
