package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationLibraryNotificationRepository extends CrudRepository<Notification, UUID> {
}
