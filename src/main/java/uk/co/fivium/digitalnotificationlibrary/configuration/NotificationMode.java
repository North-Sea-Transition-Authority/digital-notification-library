package uk.co.fivium.digitalnotificationlibrary.configuration;

/**
 * Enum containing the configurable modes the library can be run in.
 */
public enum NotificationMode {
  /** Production mode will send notifications to the indented recipients. */
  PRODUCTION,
  /** Test mode will send notifications to the test recipients and not the indented recipients. */
  TEST
}
