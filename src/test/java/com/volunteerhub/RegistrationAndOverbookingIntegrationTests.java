package com.volunteerhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteerhub.dto.ApplicationStatusUpdateRequest;
import com.volunteerhub.entity.*;
import com.volunteerhub.repository.*;
import com.volunteerhub.service.ApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class RegistrationAndOverbookingIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationService applicationService;

    private Event createTestEvent(String title, int capacity, LocalDate date, EventStatus status) {
        User ngoUser = userRepository.findByEmail("contact@greenearth.org").orElseThrow();
        Event event = new Event(
            ngoUser,
            title,
            "Description for " + title,
            "Environment",
            "Eco Park, Sector 5",
            date,
            LocalTime.of(9, 0),
            LocalTime.of(13, 0),
            "Planting, Teamwork",
            capacity,
            status
        );
        return eventRepository.save(event);
    }

    private User createTestVolunteer(String emailPrefix) {
        String email = emailPrefix + "_" + System.nanoTime() + "@example.com";
        User user = new User(email, passwordEncoder.encode("password123"), Role.VOLUNTEER, UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    @Test
    @DisplayName("P3b-01: Browse published events with filters (q, category, location, from, to, hasSlots)")
    void testBrowseEvents_WithFilters() throws Exception {
        mockMvc.perform(get("/api/events")
                .param("q", "Beach")
                .param("category", "Environment")
                .param("location", "Eco City")
                .param("from", LocalDate.now().minusDays(30).toString())
                .param("to", LocalDate.now().plusDays(30).toString())
                .param("hasSlots", "false")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", notNullValue()))
            .andExpect(jsonPath("$.page", is(0)));
    }

    @Test
    @DisplayName("P3b-02: Volunteer registers for event successfully (returns 201 Created)")
    @WithMockUser(username = "bob@example.com", roles = {"VOLUNTEER"})
    void testRegisterForEvent_Success() throws Exception {
        Event event = createTestEvent("Clean Energy Drive " + System.currentTimeMillis(), 10, LocalDate.now().plusDays(5), EventStatus.PUBLISHED);

        mockMvc.perform(post("/api/events/" + event.getId() + "/applications")
                .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.eventId", is(event.getId().intValue())))
            .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    @DisplayName("P3b-03: Duplicate registration returns HTTP 409 Conflict")
    @WithMockUser(username = "charlie@example.com", roles = {"VOLUNTEER"})
    void testDuplicateRegistration_ReturnsConflict() throws Exception {
        Event event = createTestEvent("Recycling Awareness Drive", 10, LocalDate.now().plusDays(8), EventStatus.PUBLISHED);

        // First registration
        mockMvc.perform(post("/api/events/" + event.getId() + "/applications")
                .with(csrf()))
            .andExpect(status().isCreated());

        // Duplicate registration
        mockMvc.perform(post("/api/events/" + event.getId() + "/applications")
                .with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", is(409)))
            .andExpect(jsonPath("$.error", is("Conflict")))
            .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    @DisplayName("P3b-04: Registering for a cancelled event is refused (HTTP 400 Bad Request)")
    @WithMockUser(username = "alice@example.com", roles = {"VOLUNTEER"})
    void testRegisterCancelledEvent_Refused() throws Exception {
        Event event = createTestEvent("Cancelled River Trek", 10, LocalDate.now().plusDays(5), EventStatus.CANCELLED);

        mockMvc.perform(post("/api/events/" + event.getId() + "/applications")
                .with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("CANCELLED")));
    }

    @Test
    @DisplayName("P3b-05: Registering for a past event is refused (HTTP 400 Bad Request)")
    @WithMockUser(username = "alice@example.com", roles = {"VOLUNTEER"})
    void testRegisterPastEvent_Refused() throws Exception {
        Event event = createTestEvent("Historical Cleanup", 10, LocalDate.now().minusDays(3), EventStatus.PUBLISHED);

        mockMvc.perform(post("/api/events/" + event.getId() + "/applications")
                .with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("past")));
    }

    @Test
    @DisplayName("P3b-06: Registering for a full event is refused (HTTP 400 Bad Request)")
    @WithMockUser(username = "diana@example.com", roles = {"VOLUNTEER"})
    void testRegisterFullEvent_Refused() throws Exception {
        Event event = createTestEvent("Micro Workshop - Full", 1, LocalDate.now().plusDays(4), EventStatus.PUBLISHED);
        User volunteer1 = createTestVolunteer("first_vol");
        Application app1 = new Application(volunteer1, event, ApplicationStatus.APPROVED);
        applicationRepository.save(app1);

        // Diana attempts to register for full event
        mockMvc.perform(post("/api/events/" + event.getId() + "/applications")
                .with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("capacity")));
    }

    @Test
    @DisplayName("P3b-07: NGO approves registration, creates approved notification for volunteer")
    @WithMockUser(username = "contact@greenearth.org", roles = {"NGO"})
    void testNgoApproveRegistration_CreatesNotification() throws Exception {
        Event event = createTestEvent("Tree Pruning Session", 5, LocalDate.now().plusDays(7), EventStatus.PUBLISHED);
        User volunteer = createTestVolunteer("app_vol");
        Application app = new Application(volunteer, event, ApplicationStatus.PENDING);
        app = applicationRepository.save(app);

        ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest(ApplicationStatus.APPROVED);

        mockMvc.perform(patch("/api/applications/" + app.getId() + "/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("APPROVED")));

        // Verify volunteer received notification
        org.springframework.data.domain.Page<Notification> notifsPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(
            volunteer.getId(), org.springframework.data.domain.PageRequest.of(0, 10)
        );
        boolean hasApprovedNotif = notifsPage.getContent().stream().anyMatch(n -> "APPLICATION_APPROVED".equals(n.getType()));
        assertTrue(hasApprovedNotif, "Volunteer should receive an APPLICATION_APPROVED notification");
    }

    @Test
    @DisplayName("P3b-08: NGO rejects registration, creates rejected notification for volunteer")
    @WithMockUser(username = "contact@greenearth.org", roles = {"NGO"})
    void testNgoRejectRegistration_CreatesNotification() throws Exception {
        Event event = createTestEvent("Seedling Sorting", 5, LocalDate.now().plusDays(7), EventStatus.PUBLISHED);
        User volunteer = createTestVolunteer("reject_vol");
        Application app = new Application(volunteer, event, ApplicationStatus.PENDING);
        app = applicationRepository.save(app);

        ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest(ApplicationStatus.REJECTED);

        mockMvc.perform(patch("/api/applications/" + app.getId() + "/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("REJECTED")));

        org.springframework.data.domain.Page<Notification> notifsPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(
            volunteer.getId(), org.springframework.data.domain.PageRequest.of(0, 10)
        );
        boolean hasRejectedNotif = notifsPage.getContent().stream().anyMatch(n -> "APPLICATION_REJECTED".equals(n.getType()));
        assertTrue(hasRejectedNotif, "Volunteer should receive an APPLICATION_REJECTED notification");
    }

    @Test
    @DisplayName("P3b-09: Volunteer cancels application successfully (returns 200 OK)")
    void testVolunteerCancelsApplication_Success() throws Exception {
        Event event = createTestEvent("Park Cleanup Effort", 10, LocalDate.now().plusDays(6), EventStatus.PUBLISHED);
        User volunteer = createTestVolunteer("cancelling_vol");
        Application app = new Application(volunteer, event, ApplicationStatus.PENDING);
        app = applicationRepository.save(app);

        mockMvc.perform(post("/api/applications/" + app.getId() + "/cancel")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(volunteer.getEmail()).roles("VOLUNTEER"))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("P3b-10: Volunteer retrieves My Events tabs (upcoming, completed, pending)")
    @WithMockUser(username = "alice@example.com", roles = {"VOLUNTEER"})
    void testGetVolunteerMyEventsTabs() throws Exception {
        mockMvc.perform(get("/api/volunteers/me/applications?tab=upcoming")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", notNullValue()));

        mockMvc.perform(get("/api/volunteers/me/applications?tab=completed")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", notNullValue()));

        mockMvc.perform(get("/api/volunteers/me/applications?tab=pending")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("P3b-11: Concurrent overbooking protection guarantees approved count never exceeds capacity")
    void testConcurrentOverbookingProtection() throws Exception {
        final int capacity = 2;
        final int applicantCount = 6;

        Event event = createTestEvent("High Demand Eco Rally", capacity, LocalDate.now().plusDays(10), EventStatus.PUBLISHED);

        List<Application> applications = new ArrayList<>();
        for (int i = 0; i < applicantCount; i++) {
            User volunteer = createTestVolunteer("concurrent_vol_" + i);
            Application app = new Application(volunteer, event, ApplicationStatus.PENDING);
            applications.add(applicationRepository.save(app));
        }

        ExecutorService executor = Executors.newFixedThreadPool(applicantCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(applicantCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (Application app : applications) {
            executor.submit(() -> {
                try {
                    startGate.await(); // Synchronize all threads to hit approval simultaneously
                    ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest(ApplicationStatus.APPROVED);
                    applicationService.updateApplicationStatus(app.getId(), "contact@greenearth.org", req);
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    failureCount.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown(); // Fire all threads concurrently
        boolean finished = endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "All approval attempts should complete within timeout");
        assertEquals(capacity, successCount.get(), "Exactly " + capacity + " applications should be approved");
        assertEquals(applicantCount - capacity, failureCount.get(), "Remaining applications must be rejected due to capacity lock");

        long actualApprovedInDb = applicationRepository.countByEventIdAndStatus(event.getId(), ApplicationStatus.APPROVED);
        assertEquals(capacity, actualApprovedInDb, "Database approved count must strictly equal event capacity (" + capacity + ")");
    }
}
