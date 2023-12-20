package uk.co.fivium.digitalnotificationlibrary.core.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The main service consumers will use to interact with the library.
 */
@Service
public class NotificationLibraryClient {

  private final GovukNotifyService govukNotifyService;

  /**
   * Create an instance of NotificationLibraryClient.
   * @param govukNotifyService The Gov UK notify service.
   */
  @Autowired
  public NotificationLibraryClient(GovukNotifyService govukNotifyService) {
    this.govukNotifyService = govukNotifyService;
  }

  /**
   * Get the template associated to the provided template ID. If Notify is down a template will still be
   * returned but the type, mail merge fields will be unknown.
   * @param notifyTemplateId The ID of the notify template to return
   * @return A template known to notify or an unconfirmed template if notify is down.
   */
  public Template getTemplate(String notifyTemplateId) {
    return govukNotifyService.getTemplate(notifyTemplateId)
        .map(Template::fromNotifyTemplate)
        .orElse(Template.createUnconfirmedTemplate(notifyTemplateId));
  }
}
