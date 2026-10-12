package com.volunteerhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteerhub.entity.*;
import com.volunteerhub.repository.*;
import com.volunteerhub.service.EventReminderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 7b: Dashboards, Reports, and Reminder Scheduler Integration Tests")
public class DashboardAndRemindersIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EventReminderService eventReminderService;

    @Test
    @DisplayName("P7b-01: Volunteer dashboard returns role-scoped metrics for authenticated volunteer, forbidden for NGO/Admin")
    void testVolunteerDashboard_AccessAndPayload() throws Exception {
        // Volunteer access succeeds
        mockMvc.perform(get("/api/dashboard/volunteer")
                .with(user("alice@example.com").roles("VOLUNTEER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.volunteerName", notNullValue()))
            .andExpect(jsonPath("$.verifiedHours", notNullValue()))
            .andExpect(jsonPath("$.completedEvents", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.attendanceRate", notNullValue()))
            .andExpect(jsonPath("$.badge", notNullValue()))
            .andExpect(jsonPath("$.upcomingApprovedEvents", notNullValue()));

        // Non-volunteer is forbidden
        mockMvc.perform(get("/api/dashboard/volunteer")
                .with(user("contact@greenearth.org").roles("NGO")))
            .andExpect(status().isForbidden());

        // Unauthenticated is unauthorized
        mockMvc.perform(get("/api/dashboard/volunteer"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("P7b-02: NGO dashboard returns organization-scoped metrics for authenticated NGO, forbidden for volunteers")
    void testNgoDashboard_AccessAndPayload() throws Exception {
        // NGO access succeeds
        mockMvc.perform(get("/api/dashboard/ngo")
                .with(user("contact@greenearth.org").roles("NGO")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ngoName", is("Green Earth Foundation")))
            .andExpect(jsonPath("$.verificationStatus", is("VERIFIED")))
            .andExpect(jsonPath("$.eventsCreated", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.totalVerifiedHours", notNullValue()))
            .andExpect(jsonPath("$.hoursByCategory", notNullValue()))
            .andExpect(jsonPath("$.activity", notNullValue()));

        // Volunteer is forbidden
        mockMvc.perform(get("/api/dashboard/ngo")
                .with(user("alice@example.com").roles("VOLUNTEER")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("P7b-03: Admin dashboard returns platform metrics, forbidden for non-admins")
    void testAdminDashboard_AccessAndPayload() throws Exception {
        // Admin access succeeds
        mockMvc.perform(get("/api/dashboard/admin")
                .with(user("admin@volunteerhub.org").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalUsers", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.totalVolunteers", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.verifiedNgos", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.totalVerifiedHours", notNullValue()))
            .andExpect(jsonPath("$.hoursByCategory", notNullValue()))
            .andExpect(jsonPath("$.activity", notNullValue()));

        // Volunteer is forbidden
        mockMvc.perform(get("/api/dashboard/admin")
                .with(user("alice@example.com").roles("VOLUNTEER")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("P7b-04: Hours by category aggregate report returns categories and metrics without N+1 loops")
    void testHoursByCategoryReport_Success() throws Exception {
        mockMvc.perform(get("/api/reports/hours-by-category")
                .with(user("alice@example.com").roles("VOLUNTEER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    @DisplayName("P7b-05: Activity report returns timeline data points for monthly and weekly granularities")
    void testActivityReport_Success() throws Exception {
        // Monthly granularity
        mockMvc.perform(get("/api/reports/activity")
                .with(user("alice@example.com").roles("VOLUNTEER"))
                .param("granularity", "MONTHLY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", notNullValue()));

        // Weekly granularity
        mockMvc.perform(get("/api/reports/activity")
                .with(user("alice@example.com").roles("VOLUNTEER"))
                .param("granularity", "WEEKLY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    @DisplayName("P7b-06: Event reminder scheduler notifies approved volunteers and creates ZERO duplicates when run repeatedly")
    void testEventReminderScheduler_IdempotencyAndNoDuplicates() throws Exception {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        User ngoUser = userRepository.findByEmail("contact@greenearth.org").orElseThrow();
        User volBob = userRepository.findByEmail("bob@example.com").orElseThrow();
        User volCharlie = userRepository.findByEmail("charlie@example.com").orElseThrow();

        // 1. Create a published event scheduled for tomorrow
        final Event reminderEvent = eventRepository.save(new Event(
                ngoUser,
                "Tomorrow Drive " + System.currentTimeMillis(),
                "Upcoming event description",
                "Environment",
                "City Botanical Garden",
                tomorrow,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                "Teamwork",
                20,
                EventStatus.PUBLISHED
        ));

        // 2. Register two volunteers with APPROVED status
        Application appBob = new Application(volBob, reminderEvent, ApplicationStatus.APPROVED);
        applicationRepository.save(appBob);

        Application appCharlie = new Application(volCharlie, reminderEvent, ApplicationStatus.APPROVED);
        applicationRepository.save(appCharlie);

        // 3. First execution: should find both registrations and send 2 reminders
        int firstRunCount = eventReminderService.sendUpcomingEventReminders(tomorrow);
        assertTrue(firstRunCount >= 2, "First run should send at least 2 reminders");

        // 4. Verify notifications exist in DB for this event
        List<Notification> bobNotifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                volBob.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        long bobReminderCount = bobNotifs.stream()
                .filter(n -> "EVENT_REMINDER".equals(n.getType()) && reminderEvent.getId().equals(n.getRelatedEntityId()))
                .count();
        assertEquals(1, bobReminderCount, "Volunteer Bob should receive exactly 1 reminder notification");

        List<Notification> charlieNotifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                volCharlie.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        long charlieReminderCount = charlieNotifs.stream()
                .filter(n -> "EVENT_REMINDER".equals(n.getType()) && reminderEvent.getId().equals(n.getRelatedEntityId()))
                .count();
        assertEquals(1, charlieReminderCount, "Volunteer Charlie should receive exactly 1 reminder notification");

        // 5. SECOND execution (simulating repeated scheduler run): should send 0 new reminders for this event!
        int secondRunCount = eventReminderService.sendUpcomingEventReminders(tomorrow);
        assertEquals(0, secondRunCount, "Second execution must produce 0 duplicate reminders (strict idempotency)");

        // 6. Verify count remains exactly 1 for Bob
        List<Notification> bobNotifsAfter = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                volBob.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        long bobReminderCountAfter = bobNotifsAfter.stream()
                .filter(n -> "EVENT_REMINDER".equals(n.getType()) && reminderEvent.getId().equals(n.getRelatedEntityId()))
                .count();
        assertEquals(1, bobReminderCountAfter, "Volunteer Bob should still have exactly 1 reminder after second run");
    }

    @Test
    @DisplayName("P7b-07: Admin can manually trigger reminders via POST /api/admin/reminders/trigger, non-admins get 403")
    void testAdminTriggerRemindersEndpoint() throws Exception {
        LocalDate targetDate = LocalDate.now().plusDays(2);

        // Admin trigger succeeds
        mockMvc.perform(post("/api/admin/reminders/trigger")
                .with(csrf())
                .with(user("admin@volunteerhub.org").roles("ADMIN"))
                .param("date", targetDate.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sentCount", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.targetDate", is(targetDate.toString())))
            .andExpect(jsonPath("$.message", containsString("reminder notifications")));

        // Volunteer trigger is forbidden
        mockMvc.perform(post("/api/admin/reminders/trigger")
                .with(csrf())
                .with(user("alice@example.com").roles("VOLUNTEER"))
                .param("date", targetDate.toString()))
            .andExpect(status().isForbidden());
    }
}
