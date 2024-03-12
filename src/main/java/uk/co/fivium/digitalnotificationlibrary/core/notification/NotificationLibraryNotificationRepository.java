package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * A repository for doing CRUD operations on Notifications. This repository is public only for
 * ease of configuring via the NotificationLibraryEntityAutoConfiguration. Consumers should not inject
 * this repository directly.
 */
@Repository
public interface NotificationLibraryNotificationRepository extends CrudRepository<Notification, UUID> {

  /**
   * Get all notifications with the provided statuses taking into account the requested pagination. Notifications
   * are order by last send attempt date (taking into account null values) followed by the requested on date.
   * @param statuses The statuses of notifications to return
   * @param pageRequest pageable The pagination information to restrict results to
   * @return notifications with the requested statuses and pagination ordered by requested on date ascending
   */

  @Query(value = """
        SELECT n
        FROM Notification n
        WHERE n.status IN :statuses
        ORDER BY n.lastSendAttemptAt ASC NULLS FIRST, n.requestedOn ASC
      """)
  List<Notification> findNotificationsByStatuses(@Param("statuses") Collection<NotificationStatus> statuses,
                                                 PageRequest pageRequest);
}
