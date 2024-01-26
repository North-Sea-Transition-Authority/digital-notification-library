package uk.co.fivium.digitalnotificationlibrary.core.notification;

import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

class ProductionGovukNotifySender implements GovukNotifySender {

  private final GovukNotifySenderService govukNotifySenderService;

  ProductionGovukNotifySender(GovukNotifySenderService govukNotifySenderService) {
    this.govukNotifySenderService = govukNotifySenderService;
  }

  @Override
  public Response<SendEmailResponse> sendEmail(Notification notification) {
    return govukNotifySenderService.sendEmail(notification);
  }

  @Override
  public Response<SendSmsResponse> sendSms(Notification notification) {
    return govukNotifySenderService.sendSms(notification);
  }
}
