package com.volunteerhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteerhub.dto.HoursReviewRequest;
import com.volunteerhub.entity.*;
import com.volunteerhub.repository.*;
import com.volunteerhub.service.CertificateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 6a: Certificate Generation Backend Integration Tests")
public class CertificateIntegrationTests {

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
    private CertificateRepository certificateRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NgoProfileRepository ngoProfileRepository;

    @Autowired
    private VolunteerProfileRepository volunteerProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CertificateService certificateService;

    private User createUniqueUser(String prefix, Role role) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = prefix + "_" + unique + "@test.com";
        User user = new User(email, passwordEncoder.encode("Password123!"), role, UserStatus.ACTIVE);
        User saved = userRepository.save(user);

        if (role == Role.VOLUNTEER) {
            VolunteerProfile profile = new VolunteerProfile(saved, "Volunteer " + unique, "+1-555-0199", "Metro City", "Gardening", "Environment", "Test bio", true);
            volunteerProfileRepository.save(profile);
        }
        return saved;
    }

    private NgoProfile createUniqueNgo(User user, String name) {
        NgoProfile ngo = new NgoProfile(user, name, "Community", "Testing", "REG-" + UUID.randomUUID().toString().substring(0, 6),
                "Contact Person", "+1-555-0100", "Test Address", NgoVerificationStatus.VERIFIED);
        return ngoProfileRepository.save(ngo);
    }

    private Event createTestEvent(User ngoUser, LocalDate date, EventStatus status, LocalTime start, LocalTime end) {
        Event event = new Event(
                ngoUser,
                "Certificate Test Event " + UUID.randomUUID().toString().substring(0, 6),
                "Description for certificate testing",
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
        attendance.setHoursStatus(HoursStatus.PENDING);
        return attendanceRepository.save(attendance);
    }

    @Test
    @DisplayName("Hours approval auto-issues exactly one certificate with valid sequence number")
    void testHoursApprovalAutoIssuesCertificate() throws Exception {
        User ngoUser = createUniqueUser("ngo_cert1", Role.NGO);
        createUniqueNgo(ngoUser, "Cert Test NGO 1");
        Event event = createTestEvent(ngoUser, LocalDate.now().minusDays(2), EventStatus.COMPLETED, LocalTime.of(9, 0), LocalTime.of(13, 0));

        User vol = createUniqueUser("vol_cert1", Role.VOLUNTEER);
        Application app = createTestApplication(vol, event, ApplicationStatus.APPROVED);
        Attendance attendance = createTestAttendance(app, AttendanceStatus.PRESENT, BigDecimal.valueOf(4.0), ngoUser);

        // Approve hours via endpoint
        HoursReviewRequest request = new HoursReviewRequest(HoursStatus.APPROVED, null);

        mockMvc.perform(patch("/api/attendance/" + attendance.getId() + "/hours-review")
                        .with(csrf())
                        .with(user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoursStatus", is("APPROVED")));

        // Verify Certificate was automatically created
        Optional<Certificate> certOpt = certificateRepository.findByAttendanceId(attendance.getId());
        assertTrue(certOpt.isPresent(), "Certificate should be automatically created upon hours approval");

        Certificate cert = certOpt.get();
        assertNotNull(cert.getCertificateNumber());
        assertTrue(cert.getCertificateNumber().matches("^VH-\\d{4}-\\d{6}$"),
                "Certificate number format must be VH-<year>-<6-digit sequence>, got: " + cert.getCertificateNumber());

        // Verify Audit Log was recorded
        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId("CERTIFICATE", cert.getId());
        assertFalse(auditLogs.isEmpty(), "Audit log entry should be created for CERTIFICATE_ISSUED");
        assertEquals("CERTIFICATE_ISSUED", auditLogs.get(0).getAction());

        // Verify In-App Notification was generated for the volunteer
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(vol.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        boolean hasCertNotification = notifications.stream().anyMatch(n -> "CERTIFICATE_ISSUED".equals(n.getType()));
        assertTrue(hasCertNotification, "Volunteer should receive an in-app notification when certificate is issued");
    }

    @Test
    @DisplayName("Duplicate approval or issuance attempts are strictly idempotent")
    void testIdempotentCertificateIssuance() {
        User ngoUser = createUniqueUser("ngo_cert_idem", Role.NGO);
        createUniqueNgo(ngoUser, "Cert Idem NGO");
        Event event = createTestEvent(ngoUser, LocalDate.now().minusDays(2), EventStatus.COMPLETED, LocalTime.of(9, 0), LocalTime.of(12, 0));

        User vol = createUniqueUser("vol_cert_idem", Role.VOLUNTEER);
        Application app = createTestApplication(vol, event, ApplicationStatus.APPROVED);
        Attendance attendance = createTestAttendance(app, AttendanceStatus.PRESENT, BigDecimal.valueOf(3.0), ngoUser);
        attendance.setHoursStatus(HoursStatus.APPROVED);
        attendance = attendanceRepository.save(attendance);

        // Issue once
        Certificate cert1 = certificateService.issueCertificateForAttendance(attendance, ngoUser);
        assertNotNull(cert1);

        // Issue second time (duplicate call)
        Certificate cert2 = certificateService.issueCertificateForAttendance(attendance, ngoUser);
        assertNotNull(cert2);
        assertEquals(cert1.getId(), cert2.getId(), "Subsequent issuance attempt must return the same certificate ID");
        assertEquals(cert1.getCertificateNumber(), cert2.getCertificateNumber(), "Certificate numbers must match exactly");

        // Verify DB only has 1 record
        long count = certificateRepository.count();
        assertTrue(count >= 1);
        Optional<Certificate> byAttendance = certificateRepository.findByAttendanceId(attendance.getId());
        assertTrue(byAttendance.isPresent());
    }

    @Test
    @DisplayName("Rejected hours generate no certificate")
    void testRejectedHoursGenerateNoCertificate() throws Exception {
        User ngoUser = createUniqueUser("ngo_cert_rej", Role.NGO);
        createUniqueNgo(ngoUser, "Cert Rej NGO");
        Event event = createTestEvent(ngoUser, LocalDate.now().minusDays(2), EventStatus.COMPLETED, LocalTime.of(9, 0), LocalTime.of(12, 0));

        User vol = createUniqueUser("vol_cert_rej", Role.VOLUNTEER);
        Application app = createTestApplication(vol, event, ApplicationStatus.APPROVED);
        Attendance attendance = createTestAttendance(app, AttendanceStatus.PRESENT, BigDecimal.valueOf(3.0), ngoUser);

        HoursReviewRequest request = new HoursReviewRequest(HoursStatus.REJECTED, "Incomplete volunteer shift hours");

        mockMvc.perform(patch("/api/attendance/" + attendance.getId() + "/hours-review")
                        .with(csrf())
                        .with(user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoursStatus", is("REJECTED")));

        Optional<Certificate> certOpt = certificateRepository.findByAttendanceId(attendance.getId());
        assertTrue(certOpt.isEmpty(), "No certificate should be generated when hours are rejected");
    }

    @Test
    @DisplayName("Volunteer can list their issued certificates")
    void testVolunteerCanListTheirCertificates() throws Exception {
        User ngoUser = createUniqueUser("ngo_cert_list", Role.NGO);
        createUniqueNgo(ngoUser, "Cert List NGO");
        Event event = createTestEvent(ngoUser, LocalDate.now().minusDays(3), EventStatus.COMPLETED, LocalTime.of(9, 0), LocalTime.of(14, 0));

        User vol = createUniqueUser("vol_cert_list", Role.VOLUNTEER);
        Application app = createTestApplication(vol, event, ApplicationStatus.APPROVED);
        Attendance attendance = createTestAttendance(app, AttendanceStatus.PRESENT, BigDecimal.valueOf(5.0), ngoUser);
        attendance.setHoursStatus(HoursStatus.APPROVED);
        attendance = attendanceRepository.save(attendance);

        Certificate cert = certificateService.issueCertificateForAttendance(attendance, ngoUser);

        mockMvc.perform(get("/api/volunteers/me/certificates")
                        .with(user(vol.getEmail()).roles("VOLUNTEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(cert.getId().intValue())))
                .andExpect(jsonPath("$[0].certificateNumber", is(cert.getCertificateNumber())))
                .andExpect(jsonPath("$[0].eventTitle", is(event.getTitle())))
                .andExpect(jsonPath("$[0].ngoName", is("Cert List NGO")))
                .andExpect(jsonPath("$[0].downloadUrl", is("/api/certificates/" + cert.getId() + "/pdf")));
    }

    @Test
    @DisplayName("Volunteer can download their own certificate PDF")
    void testVolunteerCanDownloadTheirCertificatePdf() throws Exception {
        User ngoUser = createUniqueUser("ngo_cert_dl", Role.NGO);
        createUniqueNgo(ngoUser, "Cert Download NGO");
        Event event = createTestEvent(ngoUser, LocalDate.now().minusDays(1), EventStatus.COMPLETED, LocalTime.of(10, 0), LocalTime.of(14, 0));

        User vol = createUniqueUser("vol_cert_dl", Role.VOLUNTEER);
        Application app = createTestApplication(vol, event, ApplicationStatus.APPROVED);
        Attendance attendance = createTestAttendance(app, AttendanceStatus.PRESENT, BigDecimal.valueOf(4.0), ngoUser);
        attendance.setHoursStatus(HoursStatus.APPROVED);
        attendance = attendanceRepository.save(attendance);

        Certificate cert = certificateService.issueCertificateForAttendance(attendance, ngoUser);

        byte[] responseBytes = mockMvc.perform(get("/api/certificates/" + cert.getId() + "/pdf")
                        .with(user(vol.getEmail()).roles("VOLUNTEER")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"certificate-" + cert.getCertificateNumber() + ".pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertNotNull(responseBytes);
        assertTrue(responseBytes.length > 500, "PDF byte array should contain valid non-trivial document content");
        // Check PDF magic header %PDF-
        String header = new String(responseBytes, 0, Math.min(responseBytes.length, 5));
        assertEquals("%PDF-", header, "File content must begin with PDF magic header %PDF-");
    }

    @Test
    @DisplayName("Volunteer downloading another volunteer's certificate receives 403 Forbidden")
    void testVolunteerCannotDownloadOtherVolunteerCertificate() throws Exception {
        User ngoUser = createUniqueUser("ngo_cert_forbid", Role.NGO);
        createUniqueNgo(ngoUser, "Cert Forbid NGO");
        Event event = createTestEvent(ngoUser, LocalDate.now().minusDays(1), EventStatus.COMPLETED, LocalTime.of(10, 0), LocalTime.of(14, 0));

        User ownerVol = createUniqueUser("vol_cert_owner", Role.VOLUNTEER);
        Application app = createTestApplication(ownerVol, event, ApplicationStatus.APPROVED);
        Attendance attendance = createTestAttendance(app, AttendanceStatus.PRESENT, BigDecimal.valueOf(4.0), ngoUser);
        attendance.setHoursStatus(HoursStatus.APPROVED);
        attendance = attendanceRepository.save(attendance);

        Certificate cert = certificateService.issueCertificateForAttendance(attendance, ngoUser);

        User attackerVol = createUniqueUser("vol_cert_attacker", Role.VOLUNTEER);

        mockMvc.perform(get("/api/certificates/" + cert.getId() + "/pdf")
                        .with(user(attackerVol.getEmail()).roles("VOLUNTEER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can download any certificate PDF")
    void testAdminCanDownloadAnyCertificatePdf() throws Exception {
        User ngoUser = createUniqueUser("ngo_cert_admin", Role.NGO);
        createUniqueNgo(ngoUser, "Cert Admin NGO");
        Event event = createTestEvent(ngoUser, LocalDate.now().minusDays(1), EventStatus.COMPLETED, LocalTime.of(10, 0), LocalTime.of(14, 0));

        User vol = createUniqueUser("vol_cert_admin_vol", Role.VOLUNTEER);
        Application app = createTestApplication(vol, event, ApplicationStatus.APPROVED);
        Attendance attendance = createTestAttendance(app, AttendanceStatus.PRESENT, BigDecimal.valueOf(4.0), ngoUser);
        attendance.setHoursStatus(HoursStatus.APPROVED);
        attendance = attendanceRepository.save(attendance);

        Certificate cert = certificateService.issueCertificateForAttendance(attendance, ngoUser);

        User admin = createUniqueUser("admin_cert", Role.ADMIN);

        byte[] responseBytes = mockMvc.perform(get("/api/certificates/" + cert.getId() + "/pdf")
                        .with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertNotNull(responseBytes);
        assertTrue(responseBytes.length > 500);
        String header = new String(responseBytes, 0, Math.min(responseBytes.length, 5));
        assertEquals("%PDF-", header);
    }

    @Test
    @DisplayName("Unauthenticated request to download certificate returns 401 Unauthorized")
    void testUnauthenticatedDownloadFails() throws Exception {
        mockMvc.perform(get("/api/certificates/999/pdf"))
                .andExpect(status().isUnauthorized());
    }
}
