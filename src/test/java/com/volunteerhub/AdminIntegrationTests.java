package com.volunteerhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteerhub.dto.AdminEventRemoveRequest;
import com.volunteerhub.dto.LoginRequest;
import com.volunteerhub.dto.UserStatusUpdateRequest;
import com.volunteerhub.entity.*;
import com.volunteerhub.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 7a: Admin Panel Backend Integration Tests")
public class AdminIntegrationTests {

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
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("P7a-01: Non-admin users are strictly forbidden from accessing admin endpoints (403 Forbidden)")
    void testNonAdminEndpoints_Forbidden() throws Exception {
        // Volunteer access
        mockMvc.perform(get("/api/admin/users")
                .with(user("alice@example.com").roles("VOLUNTEER")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/events")
                .with(user("alice@example.com").roles("VOLUNTEER")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/stats")
                .with(user("alice@example.com").roles("VOLUNTEER")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/audit-logs")
                .with(user("alice@example.com").roles("VOLUNTEER")))
            .andExpect(status().isForbidden());

        // NGO access
        mockMvc.perform(get("/api/admin/users")
                .with(user("contact@greenearth.org").roles("NGO")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/stats")
                .with(user("contact@greenearth.org").roles("NGO")))
            .andExpect(status().isForbidden());

        // Unauthenticated access
        mockMvc.perform(get("/api/admin/stats"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("P7a-02: Admin can search and list users with pagination and role/status filters")
    void testAdminGetUsers_Success() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .with(user("admin@volunteerhub.org").roles("ADMIN"))
                .param("query", "alice")
                .param("role", "VOLUNTEER")
                .param("status", "ACTIVE")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", not(empty())))
            .andExpect(jsonPath("$.content[0].email", is("alice@example.com")))
            .andExpect(jsonPath("$.content[0].role", is("VOLUNTEER")))
            .andExpect(jsonPath("$.content[0].status", is("ACTIVE")))
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("P7a-03: Admin updating user status to SUSPENDED blocks subsequent logins, reactivating restores access")
    void testAdminUpdateUserStatus_SuspendAndReactivateLifecycle() throws Exception {
        String testEmail = "lifecycle_user_" + System.currentTimeMillis() + "@example.com";
        User testUser = new User(testEmail, passwordEncoder.encode("Password@123"), Role.VOLUNTEER, UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);

        // 1. Verify user can initially log in
        LoginRequest loginReq = new LoginRequest(testEmail, "Password@123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isOk());

        // 2. Admin suspends the user
        UserStatusUpdateRequest suspendReq = new UserStatusUpdateRequest(UserStatus.SUSPENDED);
        mockMvc.perform(patch("/api/admin/users/" + testUser.getId() + "/status")
                .with(csrf())
                .with(user("admin@volunteerhub.org").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(suspendReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(testUser.getId().intValue())))
            .andExpect(jsonPath("$.status", is("SUSPENDED")));

        // 3. Suspended user attempts login and is rejected with 401
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message", containsString("suspended")));

        // 4. Admin reactivates the user
        UserStatusUpdateRequest activateReq = new UserStatusUpdateRequest(UserStatus.ACTIVE);
        mockMvc.perform(patch("/api/admin/users/" + testUser.getId() + "/status")
                .with(csrf())
                .with(user("admin@volunteerhub.org").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("ACTIVE")));

        // 5. Reactivated user can log in successfully
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", is(testEmail)));
    }

    @Test
    @DisplayName("P7a-04: Admin cannot suspend or alter their own account status (400 Bad Request)")
    void testAdminCannotModifyOwnStatus() throws Exception {
        User adminUser = userRepository.findByEmail("admin@volunteerhub.org").orElseThrow();

        UserStatusUpdateRequest req = new UserStatusUpdateRequest(UserStatus.SUSPENDED);

        mockMvc.perform(patch("/api/admin/users/" + adminUser.getId() + "/status")
                .with(csrf())
                .with(user("admin@volunteerhub.org").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("cannot change their own account status")));
    }

    @Test
    @DisplayName("P7a-05: Admin can search and paginate platform events")
    void testAdminGetEvents_Success() throws Exception {
        mockMvc.perform(get("/api/admin/events")
                .with(user("admin@volunteerhub.org").roles("ADMIN"))
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", not(empty())))
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.content[0].title", notNullValue()));
    }

    @Test
    @DisplayName("P7a-06: Admin soft-removing an event sets status to REMOVED, notifies volunteers & host NGO, and logs audit trail")
    void testAdminRemoveEvent_SuccessWithNotificationsAndAudit() throws Exception {
        User ngoUser = userRepository.findByEmail("contact@greenearth.org").orElseThrow();
        User volunteer = userRepository.findByEmail("alice@example.com").orElseThrow();

        // Create a new event
        Event event = new Event(
                ngoUser,
                "Inappropriate Event " + System.currentTimeMillis(),
                "Description for inappropriate event",
                "Community",
                "City Center",
                LocalDate.now().plusDays(5),
                LocalTime.of(10, 0),
                LocalTime.of(14, 0),
                "Skills",
                25,
                EventStatus.PUBLISHED
        );
        event = eventRepository.save(event);

        // Register volunteer
        Application application = new Application(volunteer, event, ApplicationStatus.APPROVED);
        applicationRepository.save(application);

        String removalReason = "Violates community guidelines regarding commercial promotion.";
        AdminEventRemoveRequest removeReq = new AdminEventRemoveRequest(removalReason);

        mockMvc.perform(post("/api/admin/events/" + event.getId() + "/remove")
                .with(csrf())
                .with(user("admin@volunteerhub.org").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(removeReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(event.getId().intValue())))
            .andExpect(jsonPath("$.status", is("REMOVED")))
            .andExpect(jsonPath("$.removedReason", is(removalReason)));

        // Verify event in database
        Event updatedEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertEquals(EventStatus.REMOVED, updatedEvent.getStatus());
        assertEquals(removalReason, updatedEvent.getRemovedReason());

        // Verify volunteer received in-app notification
        List<Notification> volunteerNotifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                volunteer.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        boolean volunteerNotified = volunteerNotifs.stream()
                .anyMatch(n -> "EVENT_REMOVED".equals(n.getType()) && n.getMessage().contains(removalReason));
        assertTrue(volunteerNotified, "Registered volunteer should receive EVENT_REMOVED notification");

        // Verify host NGO received in-app notification
        List<Notification> ngoNotifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                ngoUser.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        boolean ngoNotified = ngoNotifs.stream()
                .anyMatch(n -> "EVENT_REMOVED".equals(n.getType()) && n.getMessage().contains(removalReason));
        assertTrue(ngoNotified, "Host NGO should receive EVENT_REMOVED notification");

        // Verify audit log entry
        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId("EVENT", event.getId());
        assertFalse(auditLogs.isEmpty(), "Audit log entry must be created for event removal");
        assertEquals("EVENT_REMOVED", auditLogs.get(0).getAction());
        assertTrue(auditLogs.get(0).getDetails().contains(removalReason));
    }

    @Test
    @DisplayName("P7a-07: Admin removing an event without a mandatory reason returns 400 Bad Request")
    void testAdminRemoveEvent_BlankReason_BadRequest() throws Exception {
        User ngoUser = userRepository.findByEmail("contact@greenearth.org").orElseThrow();
        Event event = new Event(
                ngoUser,
                "Blank Reason Event " + System.currentTimeMillis(),
                "Description",
                "Community",
                "City Center",
                LocalDate.now().plusDays(5),
                LocalTime.of(10, 0),
                LocalTime.of(14, 0),
                "Skills",
                25,
                EventStatus.PUBLISHED
        );
        event = eventRepository.save(event);

        AdminEventRemoveRequest blankReq = new AdminEventRemoveRequest("   ");

        mockMvc.perform(post("/api/admin/events/" + event.getId() + "/remove")
                .with(csrf())
                .with(user("admin@volunteerhub.org").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blankReq)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("P7a-08: Admin can inspect audit logs with filtering and pagination")
    void testAdminGetAuditLogs_Success() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs")
                .with(user("admin@volunteerhub.org").roles("ADMIN"))
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", notNullValue()))
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("P7a-09: Admin platform overview stats endpoint returns accurate aggregated figures")
    void testAdminGetStats_Success() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .with(user("admin@volunteerhub.org").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalUsers", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.totalVolunteers", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.totalNgos", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.totalAdmins", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.totalEvents", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.totalVerifiedHours", notNullValue()))
            .andExpect(jsonPath("$.totalCertificatesIssued", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.pendingNgoVerifications", greaterThanOrEqualTo(0)));
    }
}
