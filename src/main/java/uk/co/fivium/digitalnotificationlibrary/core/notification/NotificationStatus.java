package uk.co.fivium.digitalnotificationlibrary.core.notification;

/**
 * Enum with all the different statuses that notifications can have.
 */
public enum NotificationStatus {
  /** A notification that is queued prior to sending to GOV.UK notify. */
  QUEUED,
  /** A notification which has been sent to GOV.UK notify. */
  SENT_TO_NOTIFY,
  /** A notification which the library has failed to send and the library will attempt a retry. */
  RETRY,
  /** A notification which the library has failed to send and the library will not be retrying. */
  FAILED_NOT_SENT,
  TEMPORARY_FAILURE
}
