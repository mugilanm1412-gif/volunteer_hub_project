package com.volunteerhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteerhub.dto.LeaderboardVisibilityRequest;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 5b: Volunteer Performance, Leaderboard & PDF Report Integration Tests")
public class PerformanceAndLeaderboardIntegrationTests {

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
    private AttendanceRepository attendanceRepository;

    @Autowired
    private VolunteerRatingRepository volunteerRatingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Volunteer can fetch their computed performance summary with badge and redistribution")
    void testVolunteerCanFetchOwnPerformanceSummary() throws Exception {
        User ngoUser = createNgoUser();
        User volUser = createVolunteerUser("Summary Volunteer " + UUID.randomUUID().toString().substring(0, 6));

        // Create completed event, approved registration, approved attendance of 8.0 hours
        Event event = createEvent(ngoUser, EventStatus.COMPLETED);
        Application app = createApplication(volUser, event, ApplicationStatus.APPROVED);
        Attendance att = createAttendance(app, ngoUser, AttendanceStatus.PRESENT, BigDecimal.valueOf(8.0), HoursStatus.APPROVED);

        // Add rating: punctuality=5, teamwork=4, effort=5 -> avg 4.67
        createRating(att, ngoUser, 5, 4, 5, "Remarkable contribution!");

        mockMvc.perform(get("/api/volunteers/me/performance")
                        .with(user(volUser.getEmail()).roles("VOLUNTEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVerifiedHours", is(8.0)))
                .andExpect(jsonPath("$.completedEvents", is(1)))
                .andExpect(jsonPath("$.attendanceRate", is(100.0)))
                .andExpect(jsonPath("$.averageRating", notNullValue()))
                .andExpect(jsonPath("$.hoursScore", is(8.0)))
                .andExpect(jsonPath("$.attendanceScore", is(100.0)))
                .andExpect(jsonPath("$.ratingScore", notNullValue()))
                .andExpect(jsonPath("$.performanceScore", greaterThan(0.0)))
                .andExpect(jsonPath("$.badge", notNullValue()))
                .andExpect(jsonPath("$.showOnLeaderboard", is(true)));
    }

    @Test
    @DisplayName("Volunteer can fetch progress time-series grouped by month and week")
    void testVolunteerCanFetchProgressTimeSeries() throws Exception {
        User ngoUser = createNgoUser();
        User volUser = createVolunteerUser("Progress Volunteer " + UUID.randomUUID().toString().substring(0, 6));

        Event event = createEvent(ngoUser, EventStatus.COMPLETED);
        Application app = createApplication(volUser, event, ApplicationStatus.APPROVED);
        createAttendance(app, ngoUser, AttendanceStatus.PRESENT, BigDecimal.valueOf(12.5), HoursStatus.APPROVED);

        // Month granularity
        mockMvc.perform(get("/api/volunteers/me/performance/progress")
                        .param("granularity", "month")
                        .with(user(volUser.getEmail()).roles("VOLUNTEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].period", matchesPattern("\\d{4}-\\d{2}")))
                .andExpect(jsonPath("$[0].hours", is(12.5)));

        // Week granularity
        mockMvc.perform(get("/api/volunteers/me/performance/progress")
                        .param("granularity", "week")
                        .with(user(volUser.getEmail()).roles("VOLUNTEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].period", matchesPattern("\\d{4}-W\\d{2}")))
                .andExpect(jsonPath("$[0].hours", is(12.5)));
    }

    @Test
    @DisplayName("Volunteer can toggle leaderboard visibility preference")
    void testVolunteerCanToggleLeaderboardVisibility() throws Exception {
        User volUser = createVolunteerUser("Toggle Volunteer " + UUID.randomUUID().toString().substring(0, 6));

        LeaderboardVisibilityRequest optOutRequest = new LeaderboardVisibilityRequest(false);

        mockMvc.perform(patch("/api/volunteers/me/leaderboard-visibility")
                        .with(user(volUser.getEmail()).roles("VOLUNTEER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(optOutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showOnLeaderboard", is(false)))
                .andExpect(jsonPath("$.message", containsString("successfully")));

        VolunteerProfile profile = volunteerProfileRepository.findByUserId(volUser.getId()).orElseThrow();
        assertFalse(profile.isShowOnLeaderboard());

        // Opt back in
        LeaderboardVisibilityRequest optInRequest = new LeaderboardVisibilityRequest(true);
        mockMvc.perform(patch("/api/volunteers/me/leaderboard-visibility")
                        .with(user(volUser.getEmail()).roles("VOLUNTEER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(optInRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showOnLeaderboard", is(true)));

        profile = volunteerProfileRepository.findByUserId(volUser.getId()).orElseThrow();
        assertTrue(profile.isShowOnLeaderboard());
    }

    @Test
    @DisplayName("Leaderboard is publicly accessible, excludes opted-out volunteers, and protects sensitive user data")
    void testLeaderboardExcludesOptedOutVolunteersAndPreservesPrivacy() throws Exception {
        User ngoUser = createNgoUser();

        // Volunteer A: opted-in with 20 approved hours
        String nameA = "Leaderboard Alpha " + UUID.randomUUID().toString().substring(0, 4);
        User volA = createVolunteerUser(nameA);
        Event eventA = createEvent(ngoUser, EventStatus.COMPLETED);
        Application appA = createApplication(volA, eventA, ApplicationStatus.APPROVED);
        Attendance attA = createAttendance(appA, ngoUser, AttendanceStatus.PRESENT, BigDecimal.valueOf(20.0), HoursStatus.APPROVED);
        createRating(attA, ngoUser, 5, 5, 5, "Star volunteer");

        // Volunteer B: opted-out with 50 approved hours
        String nameB = "Leaderboard Beta OptedOut " + UUID.randomUUID().toString().substring(0, 4);
        User volB = createVolunteerUser(nameB);
        VolunteerProfile profileB = volunteerProfileRepository.findByUserId(volB.getId()).orElseThrow();
        profileB.setShowOnLeaderboard(false);
        volunteerProfileRepository.save(profileB);

        Event eventB = createEvent(ngoUser, EventStatus.COMPLETED);
        Application appB = createApplication(volB, eventB, ApplicationStatus.APPROVED);
        createAttendance(appB, ngoUser, AttendanceStatus.PRESENT, BigDecimal.valueOf(50.0), HoursStatus.APPROVED);

        // Call public leaderboard (no auth required)
        mockMvc.perform(get("/api/leaderboard?page=0&size=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.content[*].displayName", hasItem(nameA)))
                .andExpect(jsonPath("$.content[*].displayName", not(hasItem(nameB))))
                // Privacy check: Verify sensitive properties do not exist anywhere in response
                .andExpect(jsonPath("$.content[*].email").doesNotExist())
                .andExpect(jsonPath("$.content[*].phone").doesNotExist())
                .andExpect(jsonPath("$.content[*].bio").doesNotExist())
                .andExpect(jsonPath("$.content[*].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[*].userId").doesNotExist());
    }

    @Test
    @DisplayName("Performance endpoints strictly forbid non-volunteers")
    void testPerformanceEndpointsRestrictedToVolunteers() throws Exception {
        User ngoUser = createNgoUser();

        mockMvc.perform(get("/api/volunteers/me/performance")
                        .with(user(ngoUser.getEmail()).roles("NGO")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/volunteers/me/performance/report.pdf")
                        .with(user(ngoUser.getEmail()).roles("NGO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Volunteer can download official PDF report with valid PDF magic bytes and content disposition")
    void testVolunteerCanDownloadOfficialPdfReport() throws Exception {
        User ngoUser = createNgoUser();
        User volUser = createVolunteerUser("PDF Volunteer " + UUID.randomUUID().toString().substring(0, 6));

        Event event = createEvent(ngoUser, EventStatus.COMPLETED);
        Application app = createApplication(volUser, event, ApplicationStatus.APPROVED);
        Attendance att = createAttendance(app, ngoUser, AttendanceStatus.PRESENT, BigDecimal.valueOf(15.0), HoursStatus.APPROVED);
        createRating(att, ngoUser, 5, 4, 5, "Superb volunteer leadership!");

        MvcResult result = mockMvc.perform(get("/api/volunteers/me/performance/report.pdf")
                        .with(user(volUser.getEmail()).roles("VOLUNTEER")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", is("application/pdf")))
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"volunteer-performance-report.pdf\"")))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF should contain valid rendered binary data");

        // Validate %PDF magic header
        String magicHeader = new String(pdfBytes, 0, 4);
        assertEquals("%PDF", magicHeader, "File must start with standard %PDF header");
    }

    // Helper methods
    private User createNgoUser() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = new User("ngo_" + unique + "@test.org", passwordEncoder.encode("password123"), Role.NGO, UserStatus.ACTIVE);
        user = userRepository.save(user);

        NgoProfile profile = new NgoProfile(
                user, "Test NGO " + unique, "Education", "A test NGO",
                "REG-" + unique, "Alice Representative", "555-0199",
                "123 Test Street", NgoVerificationStatus.VERIFIED
        );
        ngoProfileRepository.save(profile);
        return user;
    }

    private User createVolunteerUser(String fullName) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User user = new User("vol_" + unique + "@test.org", passwordEncoder.encode("password123"), Role.VOLUNTEER, UserStatus.ACTIVE);
        user = userRepository.save(user);

        VolunteerProfile profile = new VolunteerProfile(
                user, fullName, "555-0101", "Springfield",
                "Coordination, Logistics", "Community Service", "Passionate volunteer", true
        );
        volunteerProfileRepository.save(profile);
        return user;
    }

    private Event createEvent(User ngoUser, EventStatus status) {
        Event event = new Event(
                ngoUser,
                "Performance Event " + UUID.randomUUID().toString().substring(0, 6),
                "Event for performance testing",
                "Environment",
                "Downtown Center",
                LocalDate.now().minusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                "Tree planting",
                20,
                status
        );
        return eventRepository.save(event);
    }

    private Application createApplication(User volunteer, Event event, ApplicationStatus status) {
        Application app = new Application(volunteer, event, status);
        return applicationRepository.save(app);
    }

    private Attendance createAttendance(Application app, User markedBy, AttendanceStatus attStatus,
                                        BigDecimal hoursWorked, HoursStatus hoursStatus) {
        Attendance att = new Attendance(app, attStatus, hoursWorked, markedBy);
        att.setHoursStatus(hoursStatus);
        return attendanceRepository.save(att);
    }

    private VolunteerRating createRating(Attendance attendance, User ratedBy, int punctuality, int teamwork, int effort, String comment) {
        VolunteerRating rating = new VolunteerRating(attendance, punctuality, teamwork, effort, comment, ratedBy);
        return volunteerRatingRepository.save(rating);
    }
}
