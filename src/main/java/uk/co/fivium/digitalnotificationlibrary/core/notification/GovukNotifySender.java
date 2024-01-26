package uk.co.fivium.digitalnotificationlibrary.core.notification;

import uk.gov.service.notify.SendEmailResponse;
import uk.gov.service.notify.SendSmsResponse;

interface GovukNotifySender {

  Response<SendEmailResponse> sendEmail(Notification notification);

  Response<SendSmsResponse> sendSms(Notification notification);
}
