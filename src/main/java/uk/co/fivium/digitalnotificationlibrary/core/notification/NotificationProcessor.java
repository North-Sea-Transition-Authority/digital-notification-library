package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.concurrent.TimeUnit;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
class NotificationProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationProcessor.class);

  private final NotificationSendingService notificationSendingService;

  private final NotificationStatusUpdateService notificationStatusUpdateService;

  @Autowired
  NotificationProcessor(NotificationSendingService notificationSendingService,
                        NotificationStatusUpdateService notificationStatusUpdateService) {
    this.notificationSendingService = notificationSendingService;
    this.notificationStatusUpdateService = notificationStatusUpdateService;
  }

  @Scheduled(
      fixedDelayString = "${digital-notification-library.notification.poll-time-seconds:10}",
      timeUnit = TimeUnit.SECONDS
  )
  @SchedulerLock(name = "NotificationScheduler_processNotifications")
  void processNotifications() {

    LOGGER.debug("Starting scheduled processing of notifications");

    LockAssert.assertLocked();

    notificationStatusUpdateService.updateNotificationStatuses();
    notificationSendingService.sendNotificationsToNotify();

    LOGGER.debug("Finished scheduled processing of notifications");
  }
}
