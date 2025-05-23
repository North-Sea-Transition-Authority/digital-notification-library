package uk.co.fivium.digitalnotificationlibrary.core.notification;

/**
 * Exception which will be thrown publicly to consumers of the library for file upload issues.
 */
public class NotificationLibraryFileException extends Exception {

  /**
   * Create an instance of this exception with a given message.
   * @param message The error message summarising the error
   */
  public NotificationLibraryFileException(String message) {
    super(message);
  }
}
