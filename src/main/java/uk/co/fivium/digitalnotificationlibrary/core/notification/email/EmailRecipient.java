package uk.co.fivium.digitalnotificationlibrary.core.notification.email;

/**
 * Interface allowing consumers to convert their existing classes into an email recipient
 * to be used within the library.
 */
public interface EmailRecipient {

  /**
   * Get the email address of the recipient.
   * @return the email address of the recipient
   */
  String getEmailAddress();

  /**
   * Utility method to create an EmailRecipient from a String email address
   * instead of an object.
   * @param emailAddress The email address of the recipient
   * @return An EmailRecipient instance of the provided email address
   */
  static EmailRecipient directEmailAddress(String emailAddress) {
    return () -> emailAddress;
  }
}