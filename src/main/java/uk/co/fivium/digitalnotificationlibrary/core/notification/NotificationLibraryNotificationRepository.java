package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.Collection;
import java.util.List;
import java.util.Set;
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

  List<Notification> findNotificationByStatusOrderByRequestedOnAsc(NotificationStatus status, Pageable pageable);

  Set<Notification> findAllByIdInAndType(Collection<UUID> id, NotificationType type);
}
