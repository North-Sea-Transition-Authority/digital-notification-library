package uk.co.fivium.digitalnotificationlibrary.core.notification.sms;

/**
 * Interface allowing consumers to convert their existing classes into a sms recipient
 * to be used within the library.
 */
public interface SmsRecipient {

  /**
   * Get the sms number of the recipient.
   * @return the sms number
   */
  String getSmsRecipient();

  /**
   * Utility method to create an SmsRecipient from a String sms number
   * instead of an object implementing this interface.
   * @param smsNumber The sms number address of the recipient
   * @return An SmsRecipient instance of the provided phone number
   */
  static SmsRecipient directPhoneNumber(String smsNumber) {
    return () -> smsNumber;
  }
}
