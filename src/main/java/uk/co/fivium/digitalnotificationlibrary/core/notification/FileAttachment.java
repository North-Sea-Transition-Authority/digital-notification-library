package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.UUID;

/**
 * Class representing a file attachment that is populated with a fileId.
 */
public record FileAttachment(String key, UUID fileId, String fileName) {
}
