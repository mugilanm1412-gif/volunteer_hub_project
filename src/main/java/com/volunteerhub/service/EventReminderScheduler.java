package com.volunteerhub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduled job executing daily at 08:00 (configurable via app.scheduler.event-reminder-cron)
 * to notify approved volunteers of events occurring tomorrow.
 */
@Component
@ConditionalOnProperty(name = "app.scheduler.event-reminder-enabled", havingValue = "true", matchIfMissing = true)
public class EventReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventReminderScheduler.class);

    private final EventReminderService eventReminderService;

    public EventReminderScheduler(EventReminderService eventReminderService) {
        this.eventReminderService = eventReminderService;
    }

    /**
     * Daily scheduled cron execution. Reminds volunteers of tomorrow's events.
     */
    @Scheduled(cron = "${app.scheduler.event-reminder-cron:0 0 8 * * ?}")
    public void runScheduledReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        log.info("Executing scheduled event reminder job for date: {}", tomorrow);
        int sent = eventReminderService.sendUpcomingEventReminders(tomorrow);
        log.info("Completed scheduled event reminder job: {} notifications sent.", sent);
    }
}
