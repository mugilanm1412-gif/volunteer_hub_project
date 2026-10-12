package com.volunteerhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteerhub.dto.AttendanceRecordRequest;
import com.volunteerhub.dto.BatchAttendanceRequest;
import com.volunteerhub.dto.HoursReviewRequest;
import com.volunteerhub.entity.*;
import com.volunteerhub.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AttendanceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NgoProfileRepository ngoProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private User createUniqueUser(String emailPrefix, Role role) {
        String email = emailPrefix + "_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        User user = new User(email, passwordEncoder.encode("password123"), role, UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private NgoProfile createUniqueNgo(User user, String name) {
        NgoProfile ngo = new NgoProfile(user, name, "Community", "Testing", "REG-" + UUID.randomUUID().toString().substring(0, 6),
                "Contact Person", "+1-555-0100", "Test Address", NgoVerificationStatus.VERIFIED);
        return ngoProfileRepository.save(ngo);
    }

    private Event createTestEvent(User ngoUser, LocalDate date, EventStatus status, LocalTime start, LocalTime end) {
        Event event = new Event(
                ngoUser,
                "Test Event " + UUID.randomUUID().toString().substring(0, 6),
                "Description for attendance testing",
                "Environment",
                "Community Park",
                date,
                start,
                end,
                "Gardening, Teamwork",
                10,
                status
        );
        return eventRepository.save(event);
    }

    private Application createTestApplication(User volunteer, Event event, ApplicationStatus status) {
        Application application = new Application(volunteer, event, status);
        return applicationRepository.save(application);
    }

    private Attendance createTestAttendance(Application app, AttendanceStatus status, BigDecimal hours, User markedBy) {
        Attendance attendance = new Attendance(app, status, hours, markedBy);
        return attendanceRepository.save(attendance);
    }

    @Test
    @DisplayName("Volunteer calling NGO attendance endpoint receives 403 Forbidden")
    @WithMockUser(username = "volunteer@test.com", roles = {"VOLUNTEER"})
    void testVolunteerCannotMarkAttendance() throws Exception {
        BatchAttendanceRequest request = new BatchAttendanceRequest(List.of(
                new AttendanceRecordRequest(1L, AttendanceStatus.PRESENT, BigDecimal.valueOf(3.0))
        ));

        mockMvc.perform(post("/api/events/1/attendance")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Non-owner NGO calling attendance endpoint receives 403 Forbidden")
    void testNonOwnerNgoCannotMarkAttendance() throws Exception {
        User ownerUser = createUniqueUser("owner_ngo", Role.NGO);
        createUniqueNgo(ownerUser, "Owner NGO");
        Event event = createTestEvent(ownerUser, LocalDate.now().minusDays(1), EventStatus.PUBLISHED, LocalTime.of(9, 0), LocalTime.of(12, 0));

        User otherUser = createUniqueUser("other_ngo", Role.NGO);
        createUniqueNgo(otherUser, "Other NGO");

        User volunteer = createUniqueUser("vol", Role.VOLUNTEER);
        Application app = createTestApplication(volunteer, event, ApplicationStatus.APPROVED);

        BatchAttendanceRequest request = new BatchAttendanceRequest(List.of(
                new AttendanceRecordRequest(app.getId(), AttendanceStatus.PRESENT, BigDecimal.valueOf(3.0))
        ));

        mockMvc.perform(post("/api/events/" + event.getId() + "/attendance")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(otherUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("You do not have permission")));
    }

    @Test
    @DisplayName("Marking attendance on a future event that is not COMPLETED fails with 400 Bad Request")
    void testCannotMarkAttendanceOnFutureEvent() throws Exception {
        User ngoUser = createUniqueUser("ngo_future", Role.NGO);
        createUniqueNgo(ngoUser, "Future NGO");
        // Event is tomorrow, status PUBLISHED (not COMPLETED)
        Event event = createTestEvent(ngoUser, LocalDate.now().plusDays(1), EventStatus.PUBLISHED, LocalTime.of(10, 0), LocalTime.of(14, 0));

        User volunteer = createUniqueUser("vol_future", Role.VOLUNTEER);
        Application app = createTestApplication(volunteer, event, ApplicationStatus.APPROVED);

        BatchAttendanceRequest request = new BatchAttendanceRequest(List.of(
                new AttendanceRecordRequest(app.getId(), AttendanceStatus.PRESENT, BigDecimal.valueOf(3.0))
        ));

        mockMvc.perform(post("/api/events/" + event.getId() + "/attendance")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("after the event date has passed or the event is marked as COMPLETED")));
    }

    @Test
    @DisplayName("Marking attendance for a non-approved registration fails with 400 Bad Request")
    void testCannotMarkAttendanceForNonApprovedRegistration() throws Exception {
        User ngoUser = createUniqueUser("ngo_nonapp", Role.NGO);
        createUniqueNgo(ngoUser, "NonApp NGO");
        Event event = createTestEvent(ngoUser, LocalDate.now().minusDays(1), EventStatus.PUBLISHED, LocalTime.of(10, 0), LocalTime.of(13, 0));

        User volunteer = createUniqueUser("vol_pending", Role.VOLUNTEER);
        Application pendingApp = createTestApplication(volunteer, event, ApplicationStatus.PENDING);

        BatchAttendanceRequest request = new BatchAttendanceRequest(List.of(
                new AttendanceRecordRequest(pendingApp.getId(), AttendanceStatus.PRESENT, BigDecimal.valueOf(2.5))
        ));

        mockMvc.perform(post("/api/events/" + event.getId() + "/attendance")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("only be marked for APPROVED registrations")));
    }

    @Test
    @DisplayName("Marking excessive hours beyond duration plus tolerance fails with 400 Bad Request")
    void testCannotMarkExcessiveHours() throws Exception {
        User ngoUser = createUniqueUser("ngo_excess", Role.NGO);
        createUniqueNgo(ngoUser, "Excessive NGO");
        // Duration: 10:00 to 12:00 = 2.0 hours. Tolerance = 1.0 hour. Max allowed = 3.0 hours.
        Event event = createTestEvent(ngoUser, LocalDate.now().minusDays(1), EventStatus.PUBLISHED, LocalTime.of(10, 0), LocalTime.of(12, 0));

        User volunteer = createUniqueUser("vol_excess", Role.VOLUNTEER);
        Application app = createTestApplication(volunteer, event, ApplicationStatus.APPROVED);

        // Submit 3.5 hours (> 3.0 max allowed)
        BatchAttendanceRequest request = new BatchAttendanceRequest(List.of(
                new AttendanceRecordRequest(app.getId(), AttendanceStatus.PRESENT, BigDecimal.valueOf(3.5))
        ));

        mockMvc.perform(post("/api/events/" + event.getId() + "/attendance")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("exceeds maximum scheduled duration plus tolerance")));
    }

    @Test
    @DisplayName("Marking zero or negative hours for PRESENT volunteer fails with 400 Bad Request")
    void testCannotMarkZeroHoursForPresent() throws Exception {
        User ngoUser = createUniqueUser("ngo_zero", Role.NGO);
        createUniqueNgo(ngoUser, "Zero NGO");
        Event event = createTestEvent(ngoUser, LocalDate.now().minusDays(1), EventStatus.PUBLISHED, LocalTime.of(9, 0), LocalTime.of(12, 0));

        User volunteer = createUniqueUser("vol_zero", Role.VOLUNTEER);
        Application app = createTestApplication(volunteer, event, ApplicationStatus.APPROVED);

        BatchAttendanceRequest request = new BatchAttendanceRequest(List.of(
                new AttendanceRecordRequest(app.getId(), AttendanceStatus.PRESENT, BigDecimal.ZERO)
        ));

        mockMvc.perform(post("/api/events/" + event.getId() + "/attendance")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("greater than 0")));
    }

    @Test
    @DisplayName("Successful batch attendance marking and volunteer query")
    void testSuccessfulBatchAttendanceAndVolunteerQuery() throws Exception {
        User ngoUser = createUniqueUser("ngo_success", Role.NGO);
        createUniqueNgo(ngoUser, "Success NGO");
        // Scheduled: 09:00 - 13:00 (4.0 hrs). Max with tolerance = 5.0 hrs.
        Event event = createTestEvent(ngoUser, LocalDate.now().minusDays(2), EventStatus.PUBLISHED, LocalTime.of(9, 0), LocalTime.of(13, 0));

        User vol1 = createUniqueUser("vol_success_1", Role.VOLUNTEER);
        User vol2 = createUniqueUser("vol_success_2", Role.VOLUNTEER);

        Application app1 = createTestApplication(vol1, event, ApplicationStatus.APPROVED);
        Application app2 = createTestApplication(vol2, event, ApplicationStatus.APPROVED);

        BatchAttendanceRequest request = new BatchAttendanceRequest(List.of(
                new AttendanceRecordRequest(app1.getId(), AttendanceStatus.PRESENT, BigDecimal.valueOf(4.5)),
                new AttendanceRecordRequest(app2.getId(), AttendanceStatus.ABSENT, BigDecimal.ZERO)
        ));

        // Submit batch attendance as NGO
        mockMvc.perform(post("/api/events/" + event.getId() + "/attendance")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].attendanceStatus", is("PRESENT")))
                .andExpect(jsonPath("$[0].hoursWorked", is(4.5)))
                .andExpect(jsonPath("$[0].hoursStatus", is("PENDING")))
                .andExpect(jsonPath("$[1].attendanceStatus", is("ABSENT")))
                .andExpect(jsonPath("$[1].hoursWorked", is(0.0)));

        // Duplicate attendance submission must fail with 409 Conflict
        mockMvc.perform(post("/api/events/" + event.getId() + "/attendance")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already been marked")));

        // Volunteer 1 queries own attendance
        mockMvc.perform(get("/api/volunteers/me/attendance")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(vol1.getEmail()).roles("VOLUNTEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventId", is(event.getId().intValue())))
                .andExpect(jsonPath("$[0].attendanceStatus", is("PRESENT")))
                .andExpect(jsonPath("$[0].hoursWorked", is(4.5)))
                .andExpect(jsonPath("$[0].hoursStatus", is("PENDING")));
    }

    @Test
    @DisplayName("Volunteer calling hours-review endpoint receives 403 Forbidden")
    @WithMockUser(username = "volunteer@test.com", roles = {"VOLUNTEER"})
    void testVolunteerCannotReviewHours() throws Exception {
        HoursReviewRequest request = new HoursReviewRequest(HoursStatus.APPROVED, null);

        mockMvc.perform(patch("/api/attendance/1/hours-review")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Non-owner NGO cannot review volunteer hours (403 Forbidden)")
    void testNonOwnerNgoCannotReviewHours() throws Exception {
        User ownerNgo = createUniqueUser("owner_ngo_rev", Role.NGO);
        createUniqueNgo(ownerNgo, "Owner NGO Rev");
        Event event = createTestEvent(ownerNgo, LocalDate.now().minusDays(3), EventStatus.COMPLETED, LocalTime.of(9, 0), LocalTime.of(12, 0));

        User vol = createUniqueUser("vol_rev_other", Role.VOLUNTEER);
        Application app = createTestApplication(vol, event, ApplicationStatus.APPROVED);
        Attendance attendance = createTestAttendance(app, AttendanceStatus.PRESENT, BigDecimal.valueOf(3.0), ownerNgo);

        User otherNgo = createUniqueUser("other_ngo_rev", Role.NGO);
        createUniqueNgo(otherNgo, "Other NGO Rev");

        HoursReviewRequest request = new HoursReviewRequest(HoursStatus.APPROVED, null);

        mockMvc.perform(patch("/api/attendance/" + attendance.getId() + "/hours-review")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(otherNgo.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("You do not have permission")));
    }

    @Test
    @DisplayName("Rejection of hours without a reason fails with 400 Bad Request")
    void testRejectionWithoutReasonFails() throws Exception {
        User ngoUser = createUniqueUser("ngo_noreason", Role.NGO);
        createUniqueNgo(ngoUser, "NoReason NGO");
        Event event = createTestEvent(ngoUser, LocalDate.now().minusDays(2), EventStatus.COMPLETED, LocalTime.of(9, 0), LocalTime.of(13, 0));

        User vol = createUniqueUser("vol_noreason", Role.VOLUNTEER);
        Application app = createTestApplication(vol, event, ApplicationStatus.APPROVED);
        Attendance attendance = createTestAttendance(app, AttendanceStatus.PRESENT, BigDecimal.valueOf(4.0), ngoUser);

        HoursReviewRequest request = new HoursReviewRequest(HoursStatus.REJECTED, "   ");

        mockMvc.perform(patch("/api/attendance/" + attendance.getId() + "/hours-review")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("reason is mandatory when rejecting volunteer hours")));
    }

    @Test
    @DisplayName("Successful hours approval creates audit log and in-app notification")
    void testSuccessfulApprovalCreatesAuditLogAndNotification() throws Exception {
        User ngoUser = createUniqueUser("ngo_appr", Role.NGO);
        createUniqueNgo(ngoUser, "Appr NGO");
        Event event = createTestEvent(ngoUser, LocalDate.now().minusDays(2), EventStatus.COMPLETED, LocalTime.of(9, 0), LocalTime.of(13, 0));

        User vol = createUniqueUser("vol_appr", Role.VOLUNTEER);
        Application app = createTestApplication(vol, event, ApplicationStatus.APPROVED);
        Attendance attendance = createTestAttendance(app, AttendanceStatus.PRESENT, BigDecimal.valueOf(4.0), ngoUser);

        HoursReviewRequest request = new HoursReviewRequest(HoursStatus.APPROVED, null);

        mockMvc.perform(patch("/api/attendance/" + attendance.getId() + "/hours-review")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoursStatus", is("APPROVED")));

        // Verify Audit Log entry
        List<AuditLog> logs = auditLogRepository.findAll();
        boolean auditFound = logs.stream().anyMatch(l ->
                "APPROVE_HOURS".equals(l.getAction()) &&
                "ATTENDANCE".equals(l.getEntityType()) &&
                attendance.getId().equals(l.getEntityId()));
        org.junit.jupiter.api.Assertions.assertTrue(auditFound, "Audit log row must be recorded for APPROVE_HOURS");

        // Verify In-App Notification entry
        List<Notification> notifs = notificationRepository.findAll();
        boolean notifFound = notifs.stream().anyMatch(n ->
                n.getUser().getId().equals(vol.getId()) &&
                "HOURS_APPROVED".equals(n.getType()) &&
                attendance.getId().equals(n.getRelatedEntityId()));
        org.junit.jupiter.api.Assertions.assertTrue(notifFound, "Notification row must be created for volunteer with HOURS_APPROVED");
    }

    @Test
    @DisplayName("Approved record is immutable and cannot be changed (400 Bad Request)")
    void testApprovedRecordCannotBeChanged() throws Exception {
        User ngoUser = createUniqueUser("ngo_immut", Role.NGO);
        createUniqueNgo(ngoUser, "Immut NGO");
        Event event = createTestEvent(ngoUser, LocalDate.now().minusDays(2), EventStatus.COMPLETED, LocalTime.of(9, 0), LocalTime.of(13, 0));

        User vol = createUniqueUser("vol_immut", Role.VOLUNTEER);
        Application app = createTestApplication(vol, event, ApplicationStatus.APPROVED);
        Attendance attendance = createTestAttendance(app, AttendanceStatus.PRESENT, BigDecimal.valueOf(4.0), ngoUser);

        // Approve it first
        HoursReviewRequest approveReq = new HoursReviewRequest(HoursStatus.APPROVED, null);
        mockMvc.perform(patch("/api/attendance/" + attendance.getId() + "/hours-review")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoursStatus", is("APPROVED")));

        // Try to modify or re-approve the approved record -> must fail with 400 Bad Request
        HoursReviewRequest rejectReq = new HoursReviewRequest(HoursStatus.REJECTED, "Changed my mind");
        mockMvc.perform(patch("/api/attendance/" + attendance.getId() + "/hours-review")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already been approved and cannot be modified")));
    }

    @Test
    @DisplayName("Admin can review any attendance record and rejection creates audit log and notification")
    void testAdminCanReviewAnyRecordAndRejectionCreatesLogs() throws Exception {
        User ngoUser = createUniqueUser("ngo_admin_rev", Role.NGO);
        createUniqueNgo(ngoUser, "AdminRev NGO");
        Event event = createTestEvent(ngoUser, LocalDate.now().minusDays(2), EventStatus.COMPLETED, LocalTime.of(9, 0), LocalTime.of(13, 0));

        User vol = createUniqueUser("vol_admin_rev", Role.VOLUNTEER);
        Application app = createTestApplication(vol, event, ApplicationStatus.APPROVED);
        Attendance attendance = createTestAttendance(app, AttendanceStatus.PRESENT, BigDecimal.valueOf(4.0), ngoUser);

        User adminUser = createUniqueUser("admin_rev_user", Role.ADMIN);

        HoursReviewRequest rejectReq = new HoursReviewRequest(HoursStatus.REJECTED, "Admin audit detected insufficient onsite hours proof");

        mockMvc.perform(patch("/api/attendance/" + attendance.getId() + "/hours-review")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(adminUser.getEmail()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoursStatus", is("REJECTED")))
                .andExpect(jsonPath("$.rejectReason", is("Admin audit detected insufficient onsite hours proof")));

        // Verify Audit Log entry
        List<AuditLog> logs = auditLogRepository.findAll();
        boolean auditFound = logs.stream().anyMatch(l ->
                "REJECT_HOURS".equals(l.getAction()) &&
                "ATTENDANCE".equals(l.getEntityType()) &&
                attendance.getId().equals(l.getEntityId()));
        org.junit.jupiter.api.Assertions.assertTrue(auditFound, "Audit log row must be recorded for REJECT_HOURS");

        // Verify In-App Notification entry
        List<Notification> notifs = notificationRepository.findAll();
        boolean notifFound = notifs.stream().anyMatch(n ->
                n.getUser().getId().equals(vol.getId()) &&
                "HOURS_REJECTED".equals(n.getType()) &&
                attendance.getId().equals(n.getRelatedEntityId()));
        org.junit.jupiter.api.Assertions.assertTrue(notifFound, "Notification row must be created for volunteer with HOURS_REJECTED");
    }
}

