package uk.co.fivium.testapplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.co.fivium.digitalnotificationlibrary.core.notification.NotificationLibraryEmailAttachmentResolver;

@Component
public class NotificationLibraryEmailAttachmentResolverTestImpl implements NotificationLibraryEmailAttachmentResolver {

  public byte[] resolveFileAttachment(UUID fileId) {
    var initialFile = new File("src/main/resources/TestDocument.pdf");
    try (var fileInputStream = new FileInputStream(initialFile)) {
      return fileInputStream.readAllBytes();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
