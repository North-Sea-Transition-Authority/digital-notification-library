package uk.co.fivium.digitalnotificationlibrary.core.notification;

/**
 * A domain reference links a consumers domain concept (e.g. application, team, consent etc.) to a notification. For
 * example if you had an application with ID 123 the domain reference would be DomainReference("123", "APPLICATION").
 * The purpose of this interface is easier association in the tables/logs so consumers can see what domain concept a
 * given notification was for.
 */
public interface DomainReference {

  /**
   * Get the ID of the domain object that is associated to the notification.
   * @return The ID of the domain object
   */
  String getDomainId();

  /**
   * Get the type of the domain object that is associated to the notification.
   * @return The type of the domain object
   */
  String getDomainType();

  /**
   * Utility method to easily to statically construct a domain reference.
   * @param domainId The identifier of the domain object
   * @param domainType The type of the domain object, e.g. APPLICATION
   * @return a constructed domain reference object
   */
  static DomainReference from(String domainId, String domainType) {
    return new DomainReference() {
      @Override
      public String getDomainId() {
        return domainId;
      }

      @Override
      public String getDomainType() {
        return domainType;
      }
    };
  }
}