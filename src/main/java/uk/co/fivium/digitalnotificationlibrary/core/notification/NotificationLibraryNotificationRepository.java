package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * A repository for doing CRUD operations on Notifications. This repository is public only for
 * ease of configuring via the NotificationLibraryEntityAutoConfiguration. Consumers should not inject
 * this repository directly.
 */
@Repository
public interface NotificationLibraryNotificationRepository extends CrudRepository<Notification, UUID> {

  /**
   * Get all notifications with the provided status taking into account the requested pagination. Results are
   * ordered by requested on date in ascending order.
   * @param status The status of notifications to return
   * @param pageable The pagination information to restrict results to
   * @return notifications with the requested status and pagination ordered by requested on date ascending
   */
  List<Notification> findNotificationByStatusOrderByRequestedOnAsc(NotificationStatus status, Pageable pageable);
}
