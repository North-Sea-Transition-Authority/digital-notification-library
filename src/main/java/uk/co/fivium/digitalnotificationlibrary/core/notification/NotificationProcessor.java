package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.util.concurrent.TimeUnit;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
class NotificationProcessor {

  private final NotificationSendingService notificationSendingService;

  @Autowired
  NotificationProcessor(NotificationSendingService notificationSendingService) {
    this.notificationSendingService = notificationSendingService;
  }

  @Scheduled(
      fixedDelayString = "${digital-notification-library.notification.poll-time-seconds:10}",
      timeUnit = TimeUnit.SECONDS
  )
  @SchedulerLock(name = "NotificationScheduler_processNotifications")
  void processNotifications() {
    LockAssert.assertLocked();
    // TODO: poll notify for status updates before sending notifications again
    notificationSendingService.sendNotificationToNotify();
  }
}
