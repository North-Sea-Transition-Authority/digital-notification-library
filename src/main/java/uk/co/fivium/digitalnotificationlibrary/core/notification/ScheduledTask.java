package uk.co.fivium.digitalnotificationlibrary.core.notification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.core.annotation.AliasFor;
import org.springframework.scheduling.annotation.Scheduled;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Scheduled
@SchedulerLock
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@interface ScheduledTask {

  @AliasFor(annotation = Scheduled.class, attribute = "fixedDelayString")
  String fixedDelayString();

  @AliasFor(annotation = Scheduled.class, attribute = "timeUnit")
  TimeUnit timeUnit();

  @AliasFor(annotation = SchedulerLock.class, attribute = "name")
  String lockName();
}
