package com.volunteerhub.service;

import com.volunteerhub.entity.Application;
import com.volunteerhub.entity.Event;
import com.volunteerhub.entity.User;
import com.volunteerhub.repository.ApplicationRepository;
import com.volunteerhub.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service providing idempotent notification dispatch for volunteers participating in upcoming events.
 */
@Service
public class EventReminderService {

    private static final Logger log = LoggerFactory.getLogger(EventReminderService.class);
    private static final String REMINDER_NOTIFICATION_TYPE = "EVENT_REMINDER";
    private static final String REMINDER_ENTITY_TYPE = "EVENT";

    private final ApplicationRepository applicationRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public EventReminderService(ApplicationRepository applicationRepository,
                                NotificationRepository notificationRepository,
                                NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    /**
     * Finds all approved registrations for published events scheduled on the target date,
     * and dispatches an in-app reminder notification to each volunteer.
     * Guaranteed to be idempotent: repeated runs for the same target date produce zero duplicate notifications.
     *
     * @param targetDate date of upcoming events (defaults to tomorrow if null)
     * @return count of new reminder notifications dispatched
     */
    @Transactional
    public int sendUpcomingEventReminders(LocalDate targetDate) {
        if (targetDate == null) {
            targetDate = LocalDate.now().plusDays(1);
        }

        List<Application> approvedApps = applicationRepository.findApprovedApplicationsForEventDate(targetDate);
        log.info("Checking upcoming event reminders for date {}: found {} approved registrations.",
                targetDate, approvedApps.size());

        int sentCount = 0;
        for (Application app : approvedApps) {
            User volunteer = app.getVolunteer();
            Event event = app.getEvent();

            // Strict idempotency check: prevent duplicate reminders for the same volunteer and event
            boolean alreadyReminded = notificationRepository.existsByUserIdAndTypeAndRelatedEntityTypeAndRelatedEntityId(
                    volunteer.getId(), REMINDER_NOTIFICATION_TYPE, REMINDER_ENTITY_TYPE, event.getId());

            if (alreadyReminded) {
                log.debug("Skipping reminder for volunteer {} on event {}: already notified.",
                        volunteer.getEmail(), event.getId());
                continue;
            }

            String title = "Reminder: Upcoming Initiative Tomorrow";
            String message = String.format("Friendly reminder: You are scheduled to participate in '%s' on %s at %s (%s).",
                    event.getTitle(), event.getEventDate(), event.getStartTime(), event.getLocation());

            notificationService.createNotification(
                    volunteer,
                    REMINDER_NOTIFICATION_TYPE,
                    title,
                    message,
                    REMINDER_ENTITY_TYPE,
                    event.getId()
            );

            sentCount++;
        }

        log.info("Dispatched {} event reminders for date {}.", sentCount, targetDate);
        return sentCount;
    }
}
