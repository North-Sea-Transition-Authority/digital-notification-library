package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.Optional;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitalnotificationlibrary.core.DigitalNotificationLibraryException;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.Template;

@Service
class GovukNotifyService {

  private final NotificationClient notifyClient;

  @Autowired
  GovukNotifyService(NotificationClient notifyClient) {
    this.notifyClient = notifyClient;
  }

  Optional<Template> getTemplate(String templateId) {
    try {
      return Optional.of(notifyClient.getTemplateById(templateId));
    } catch (NotificationClientException exception) {

      // If we get a 403 response from notify then throw an exception as the consumer has
      // provided incorrect credentials. Any other response status return an empty optional in order
      // to continue even if notify is down.
      // See: https://docs.notifications.service.gov.uk/java.html#get-a-template-by-id-error-codes
      if (HttpStatus.SC_FORBIDDEN == exception.getHttpResult()) {
        throw new DigitalNotificationLibraryException(
            "Notify returned a 403 response trying to find template with ID %s: %s"
                .formatted(templateId, exception.getMessage())
        );
      }

      return Optional.empty();
    }
  }
}
