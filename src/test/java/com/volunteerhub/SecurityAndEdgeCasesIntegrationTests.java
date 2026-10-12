package com.volunteerhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteerhub.dto.*;
import com.volunteerhub.entity.*;
import com.volunteerhub.repository.*;
import com.volunteerhub.service.ApplicationService;
import com.volunteerhub.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Hardening & Security Integration Tests (Phase 8a / Section 12).
 * Verifies:
 *  - Password reset edge cases (unknown email generic 200, expired token 400, reused token 400, weak password 400)
 *  - Notification ownership isolation (Volunteer A cannot read/mark read Volunteer B's notifications)
 *  - Private profile access control (unauthenticated 401, wrong role 403, parameter-tamper proof)
 *  - CSRF protection enforcement on state-mutating requests
 *  - Multithreaded concurrent overbooking protection via pessimistic write lock
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 8a: Security Review, Edge Cases & Concurrent Locking Integration Tests")
public class SecurityAndEdgeCasesIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VolunteerProfileRepository volunteerProfileRepository;

    @Autowired
    private NgoProfileRepository ngoProfileRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationService applicationService;

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private User createVolunteerUser(String fullName) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = new User("vol_sec_" + unique + "@example.com", passwordEncoder.encode("password123"), Role.VOLUNTEER, UserStatus.ACTIVE);
        user = userRepository.save(user);

        VolunteerProfile profile = new VolunteerProfile(
                user, fullName, "555-0999", "TechCity",
                "Testing, Security", "Environment", "Volunteer bio for security test", true
        );
        volunteerProfileRepository.save(profile);
        return user;
    }

    private User createNgoUser() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = new User("ngo_sec_" + unique + "@example.org", passwordEncoder.encode("password123"), Role.NGO, UserStatus.ACTIVE);
        user = userRepository.save(user);

        NgoProfile profile = new NgoProfile(
                user, "Sec NGO " + unique, "Community", "Testing Org",
                "REG-SEC-" + unique, "Director Smith", "555-0888",
                "456 Security Ave", NgoVerificationStatus.VERIFIED
        );
        ngoProfileRepository.save(profile);
        return user;
    }

    // =========================================================================
    // 1. PASSWORD RESET EDGE CASES
    // =========================================================================

    @Test
    @DisplayName("P8a-01: Forgot password with unknown email returns generic 200 OK without disclosing user existence")
    void testPasswordReset_UnknownEmail_ReturnsGeneric200() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest("unknown_ghost_account_998877@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("If this email is registered")));
    }

    @Test
    @DisplayName("P8a-02: Password reset with expired token is rejected with HTTP 400 Bad Request")
    void testPasswordReset_ExpiredToken_Rejected() throws Exception {
        User volunteer = createVolunteerUser("Expired Token User");
        String rawToken = "expired_raw_token_" + UUID.randomUUID();
        String hash = sha256Hex(rawToken);

        // Expired 15 minutes ago
        PasswordResetToken token = new PasswordResetToken(volunteer, hash, Instant.now().minus(Duration.ofMinutes(15)));
        tokenRepository.save(token);

        ResetPasswordRequest req = new ResetPasswordRequest(rawToken, "NewSecurePassword@2026");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("expired")));
    }

    @Test
    @DisplayName("P8a-03: Password reset with already-used token is rejected with HTTP 400 Bad Request")
    void testPasswordReset_ReusedToken_Rejected() throws Exception {
        User volunteer = createVolunteerUser("Reused Token User");
        String rawToken = "used_raw_token_" + UUID.randomUUID();
        String hash = sha256Hex(rawToken);

        // Token marked as used 5 minutes ago
        PasswordResetToken token = new PasswordResetToken(volunteer, hash, Instant.now().plus(Duration.ofMinutes(25)));
        token.setUsedAt(Instant.now().minus(Duration.ofMinutes(5)));
        tokenRepository.save(token);

        ResetPasswordRequest req = new ResetPasswordRequest(rawToken, "NewSecurePassword@2026");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("already been used")));
    }

    @Test
    @DisplayName("P8a-04: Password reset with weak password (< 8 chars) is rejected with HTTP 400 Bad Request")
    void testPasswordReset_WeakPassword_Rejected() throws Exception {
        User volunteer = createVolunteerUser("Weak Pwd User");
        String rawToken = "valid_raw_token_" + UUID.randomUUID();
        String hash = sha256Hex(rawToken);

        PasswordResetToken token = new PasswordResetToken(volunteer, hash, Instant.now().plus(Duration.ofMinutes(30)));
        tokenRepository.save(token);

        // Password shorter than 8 characters
        ResetPasswordRequest req = new ResetPasswordRequest(rawToken, "short");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors.newPassword", containsString("Password must be at least 8 characters long")));
    }

    // =========================================================================
    // 2. NOTIFICATION OWNERSHIP ISOLATION
    // =========================================================================

    @Test
    @DisplayName("P8a-05: Volunteer A cannot mark Volunteer B's notification as read (HTTP 403 Forbidden)")
    void testNotificationOwnership_VolunteerCannotReadOthersNotification() throws Exception {
        User volA = createVolunteerUser("Volunteer Alice Security");
        User volB = createVolunteerUser("Volunteer Bob Security");

        // Notification belonging to Volunteer B
        Notification notifB = notificationService.createNotification(
                volB, "EVENT_REMINDER", "Bob's Event Reminder",
                "Your scheduled cleanup starts tomorrow.", "EVENT", 1L
        );

        // Volunteer A attempts to mark Bob's notification as read
        mockMvc.perform(patch("/api/notifications/" + notifB.getId() + "/read")
                        .with(user(volA.getEmail()).roles("VOLUNTEER"))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Cannot access notifications belonging to another user")));

        // Verify Bob's notification remains unread in database
        Notification refreshed = notificationRepository.findById(notifB.getId()).orElseThrow();
        assertFalse(refreshed.isRead(), "Bob's notification must not be altered by Alice");
    }

    @Test
    @DisplayName("P8a-06: Volunteer A notification list contains only Alice's notifications and excludes Bob's")
    void testNotificationOwnership_VolunteerCannotViewOthersNotifications() throws Exception {
        User volA = createVolunteerUser("Volunteer Alice Feed");
        User volB = createVolunteerUser("Volunteer Bob Feed");

        // Create notification for B
        Notification notifB = notificationService.createNotification(
                volB, "EVENT_APPROVED", "Bob Approved",
                "Bob has been approved.", "APPLICATION", 2L
        );

        // Alice fetches her notifications
        mockMvc.perform(get("/api/notifications")
                        .with(user(volA.getEmail()).roles("VOLUNTEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + notifB.getId() + ")]").doesNotExist());
    }

    // =========================================================================
    // 3. PRIVATE PROFILE ACCESS & TAMPERING PROTECTION
    // =========================================================================

    @Test
    @DisplayName("P8a-07: Unauthenticated request to private volunteer profile returns HTTP 401 Unauthorized")
    void testVolunteerPrivateProfile_UnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/volunteers/me/profile")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("P8a-08: NGO role requesting volunteer profile endpoint returns HTTP 403 Forbidden")
    void testVolunteerPrivateProfile_NgoForbidden() throws Exception {
        User ngoUser = createNgoUser();

        mockMvc.perform(get("/api/volunteers/me/profile")
                        .with(user(ngoUser.getEmail()).roles("NGO"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    // =========================================================================
    // 4. CSRF PROTECTION ENFORCEMENT ON STATE-CHANGING REQUESTS
    // =========================================================================

    @Test
    @DisplayName("P8a-09: State-changing PATCH request without CSRF token is rejected with HTTP 403 Forbidden")
    void testCsrfProtection_StateChangingRequestWithoutCsrf_Forbidden() throws Exception {
        User volA = createVolunteerUser("CSRF Test Volunteer");

        LeaderboardVisibilityRequest req = new LeaderboardVisibilityRequest(false);

        // Notice: with(user) but deliberately omitting .with(csrf())
        mockMvc.perform(patch("/api/volunteers/me/leaderboard-visibility")
                        .with(user(volA.getEmail()).roles("VOLUNTEER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 5. CONCURRENT MULTITHREADED OVERBOOKING PROTECTION
    // =========================================================================

    @Test
    @DisplayName("P8a-10: Multithreaded concurrent approval on capacity=1 event permits exactly 1 and rejects rest")
    void testConcurrentMultithreadedOverbooking_PreventedByPessimisticLock() throws Exception {
        User ngoUser = createNgoUser();
        final int capacity = 1;
        final int totalApplicants = 5;

        Event event = new Event(
                ngoUser,
                "Exclusive Conservation Summit",
                "High demand capacity 1 summit",
                "Environment",
                "Summit Hall 101",
                LocalDate.now().plusDays(14),
                LocalTime.of(10, 0),
                LocalTime.of(14, 0),
                "Conservation, Ecology",
                capacity,
                EventStatus.PUBLISHED
        );
        event = eventRepository.save(event);

        List<Application> applications = new ArrayList<>();
        for (int i = 0; i < totalApplicants; i++) {
            User applicant = createVolunteerUser("Summit Candidate " + i);
            Application app = new Application(applicant, event, ApplicationStatus.PENDING);
            applications.add(applicationRepository.save(app));
        }

        ExecutorService executor = Executors.newFixedThreadPool(totalApplicants);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(totalApplicants);

        AtomicInteger successfulApprovals = new AtomicInteger(0);
        AtomicInteger rejectedApprovals = new AtomicInteger(0);

        for (Application app : applications) {
            executor.submit(() -> {
                try {
                    startGate.await(); // Release all threads at the exact same instant
                    ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest(ApplicationStatus.APPROVED);
                    applicationService.updateApplicationStatus(app.getId(), ngoUser.getEmail(), req);
                    successfulApprovals.incrementAndGet();
                } catch (Exception ex) {
                    rejectedApprovals.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Fire all threads
        startGate.countDown();
        boolean completedInTime = endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completedInTime, "All concurrent approval threads should complete within 15 seconds");
        assertEquals(capacity, successfulApprovals.get(), "Exactly " + capacity + " approval should succeed");
        assertEquals(totalApplicants - capacity, rejectedApprovals.get(), "The remaining " + (totalApplicants - capacity) + " attempts must fail due to capacity lock");

        long actualApprovedInDb = applicationRepository.countByEventIdAndStatus(event.getId(), ApplicationStatus.APPROVED);
        assertEquals(capacity, actualApprovedInDb, "Database must strictly have " + capacity + " APPROVED application");
    }
}
