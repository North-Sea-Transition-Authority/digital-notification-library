package uk.co.fivium.digitalnotificationlibrary.core.notification.email;

import uk.co.fivium.digitalnotificationlibrary.core.notification.NotificationStatus;

/**
 * Class representing an email notification within the library.
 * @param id The ID of the notification
 * @param status The status of the notification
 */
public record EmailNotification(String id, NotificationStatus status) {
}
