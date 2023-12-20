package uk.co.fivium.digitalnotificationlibrary.core.notification;

/**
 * A domain reference links a consumers domain concept (e.g. application, team, consent etc.) to a notification.
 * @param id The ID of the domain concept relating to this notification
 * @param type A textual representation which describes what the ID represents, e.g. APPLICATION, TEAM, CONSENT
 */
public record DomainReference(String id, String type) {

  /**
   * Utility method to easily to statically construct a domain reference.
   * @param id The identifier of the domain object
   * @param type The type of the domain object, e.g. APPLICATION
   * @return a constructed domain reference object
   */
  public static DomainReference from(String id, String type) {
    return new DomainReference(id, type);
  }
}