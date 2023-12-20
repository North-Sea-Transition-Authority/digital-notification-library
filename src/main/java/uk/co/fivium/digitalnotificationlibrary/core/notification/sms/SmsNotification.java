package uk.co.fivium.digitalnotificationlibrary.core.notification.sms;

import uk.co.fivium.digitalnotificationlibrary.core.notification.NotificationStatus;


/**
 * Class representing a sms notification within the library.
 * @param id The ID of the notification
 * @param status The status of the notification
 */
public record SmsNotification(String id, NotificationStatus status) {
}
