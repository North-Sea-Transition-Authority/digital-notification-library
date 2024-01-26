package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.Set;
import uk.co.fivium.digitalnotificationlibrary.configuration.NotificationLibraryConfigurationProperties;
import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

class TestGovukNotifySender implements GovukNotifySender {

  private final GovukNotifySenderService govukNotifySenderService;

  private final NotificationLibraryConfigurationProperties libraryConfigurationProperties;

  TestGovukNotifySender(GovukNotifySenderService govukNotifySenderService,
                        NotificationLibraryConfigurationProperties libraryConfigurationProperties) {
    this.govukNotifySenderService = govukNotifySenderService;
    this.libraryConfigurationProperties = libraryConfigurationProperties;
  }

  @Override
  public Response<SendEmailResponse> sendEmail(Notification notification) {

    Response<SendEmailResponse> emailResponse = null;

    for (String recipient: getEmailRecipients(notification)) {
      emailResponse = govukNotifySenderService.sendEmail(notification, recipient);
    }

    return emailResponse;
  }

  @Override
  public Response<SendSmsResponse> sendSms(Notification notification) {

    Response<SendSmsResponse> smsResponse = null;

    for (String recipient: getSmsRecipients(notification)) {
      smsResponse = govukNotifySenderService.sendSms(notification, recipient);
    }

    return smsResponse;
  }

  private Set<String> getEmailRecipients(Notification notification) {
    if (libraryConfigurationProperties.hasTestEmailRecipients()) {
      return libraryConfigurationProperties.testMode().emailRecipients();
    } else {
      return Set.of(notification.getRecipient());
    }
  }

  private Set<String> getSmsRecipients(Notification notification) {
    if (libraryConfigurationProperties.hasTestSmsRecipients()) {
      return libraryConfigurationProperties.testMode().smsRecipients();
    } else {
      return Set.of(notification.getRecipient());
    }
  }
}
