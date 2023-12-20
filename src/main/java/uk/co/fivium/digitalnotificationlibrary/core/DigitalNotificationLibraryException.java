package uk.co.fivium.digitalnotificationlibrary.core;

/**
 * Exception which will be thrown publicly to consumers of the library.
 */
public class DigitalNotificationLibraryException extends RuntimeException {

  /**
   * Create an instance of this exception with a given message.
   * @param message The error message summarising the error
   */
  public DigitalNotificationLibraryException(String message) {
    super(message);
  }
}
