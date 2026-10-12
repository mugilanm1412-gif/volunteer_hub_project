package com.volunteerhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteerhub.dto.RatingCreateRequest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 5a: Volunteer Ratings Backend Integration Tests")
public class RatingIntegrationTests {

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
    private VolunteerRatingRepository volunteerRatingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NgoProfileRepository ngoProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private Event createTestEvent(User ngoUser, EventStatus status) {
        Event event = new Event(
                ngoUser,
                "Evaluation Drive " + UUID.randomUUID().toString().substring(0, 6),
                "Event for rating evaluation tests",
                "Environment",
                "Community Park",
                LocalDate.now().minusDays(2),
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                "Gardening",
                10,
                status
        );
        return eventRepository.save(event);
    }

    private Attendance createTestAttendance(User ngoUser, User volunteerUser, AttendanceStatus attendanceStatus, BigDecimal hours) {
        Event event = createTestEvent(ngoUser, EventStatus.COMPLETED);
        Application app = new Application(volunteerUser, event, ApplicationStatus.APPROVED);
        applicationRepository.save(app);

        Attendance att = new Attendance(app, attendanceStatus, hours, ngoUser);
        att.setHoursStatus(HoursStatus.APPROVED);
        return attendanceRepository.save(att);
    }

    @Test
    @DisplayName("1. Successful volunteer rating creation creates record and dispatches in-app notification")
    void testCreateRating_Success() throws Exception {
        User ngoUser = createUniqueUser("ngo_rater", Role.NGO);
        NgoProfile ngoProfile = createUniqueNgo(ngoUser, "Green Earth Alliance");
        User volunteer = createUniqueUser("vol_rated", Role.VOLUNTEER);

        Attendance att = createTestAttendance(ngoUser, volunteer, AttendanceStatus.PRESENT, BigDecimal.valueOf(4.0));

        RatingCreateRequest request = new RatingCreateRequest(5, 4, 5, "Outstanding teamwork and energy!");

        mockMvc.perform(post("/api/attendance/" + att.getId() + "/rating")
                        .with(csrf())
                        .with(user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.attendanceId").value(att.getId()))
                .andExpect(jsonPath("$.punctuality").value(5))
                .andExpect(jsonPath("$.teamwork").value(4))
                .andExpect(jsonPath("$.effort").value(5))
                .andExpect(jsonPath("$.averageScore").value(4.67))
                .andExpect(jsonPath("$.comment").value("Outstanding teamwork and energy!"))
                .andExpect(jsonPath("$.ngoName").value(ngoProfile.getName()));

        // Verify DB entity
        assertTrue(volunteerRatingRepository.existsByAttendanceId(att.getId()));

        // Verify in-app notification dispatched
        List<Notification> notifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(volunteer.getId(), null).getContent();
        assertTrue(notifs.stream().anyMatch(n -> "RATING_RECEIVED".equals(n.getType()) &&
                n.getMessage().contains("Green Earth Alliance")));
    }

    @Test
    @DisplayName("2. Volunteer cannot submit rating (Expect 403 Forbidden)")
    void testCreateRating_VolunteerCannotRate_Returns403() throws Exception {
        User ngoUser = createUniqueUser("ngo_owner", Role.NGO);
        createUniqueNgo(ngoUser, "Nature Corp");
        User volunteer = createUniqueUser("vol_test", Role.VOLUNTEER);

        Attendance att = createTestAttendance(ngoUser, volunteer, AttendanceStatus.PRESENT, BigDecimal.valueOf(4.0));
        RatingCreateRequest request = new RatingCreateRequest(5, 5, 5, "Self praise");

        mockMvc.perform(post("/api/attendance/" + att.getId() + "/rating")
                        .with(csrf())
                        .with(user(volunteer.getEmail()).roles("VOLUNTEER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. Different NGO cannot rate another NGO's event attendance (Expect 403 Forbidden)")
    void testCreateRating_DifferentNgoCannotRate_Returns403() throws Exception {
        User hostNgo = createUniqueUser("host_ngo", Role.NGO);
        createUniqueNgo(hostNgo, "Host NGO");

        User intruderNgo = createUniqueUser("intruder_ngo", Role.NGO);
        createUniqueNgo(intruderNgo, "Intruder NGO");

        User volunteer = createUniqueUser("vol_cand", Role.VOLUNTEER);

        Attendance att = createTestAttendance(hostNgo, volunteer, AttendanceStatus.PRESENT, BigDecimal.valueOf(4.0));
        RatingCreateRequest request = new RatingCreateRequest(4, 4, 4, "Good job");

        mockMvc.perform(post("/api/attendance/" + att.getId() + "/rating")
                        .with(csrf())
                        .with(user(intruderNgo.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to evaluate attendance for another organization's event"));
    }

    @Test
    @DisplayName("4. Rating for an ABSENT volunteer is refused (Expect 400 Bad Request)")
    void testCreateRating_AbsentVolunteer_Returns400() throws Exception {
        User ngoUser = createUniqueUser("ngo_abs", Role.NGO);
        createUniqueNgo(ngoUser, "Eco Hub");
        User volunteer = createUniqueUser("vol_abs", Role.VOLUNTEER);

        Attendance att = createTestAttendance(ngoUser, volunteer, AttendanceStatus.ABSENT, BigDecimal.ZERO);
        RatingCreateRequest request = new RatingCreateRequest(3, 3, 3, "Did not show up");

        mockMvc.perform(post("/api/attendance/" + att.getId() + "/rating")
                        .with(csrf())
                        .with(user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot submit performance evaluation for an absent volunteer"));
    }

    @Test
    @DisplayName("5. Duplicate rating for the same attendance is refused (Expect 409 Conflict)")
    void testCreateRating_DuplicateRating_Returns409() throws Exception {
        User ngoUser = createUniqueUser("ngo_dup", Role.NGO);
        createUniqueNgo(ngoUser, "Clean Waters");
        User volunteer = createUniqueUser("vol_dup", Role.VOLUNTEER);

        Attendance att = createTestAttendance(ngoUser, volunteer, AttendanceStatus.PRESENT, BigDecimal.valueOf(3.0));
        RatingCreateRequest req1 = new RatingCreateRequest(5, 5, 5, "First review");

        // First rating succeeds
        mockMvc.perform(post("/api/attendance/" + att.getId() + "/rating")
                        .with(csrf())
                        .with(user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Duplicate submission fails with 409
        RatingCreateRequest req2 = new RatingCreateRequest(4, 4, 4, "Second review");
        mockMvc.perform(post("/api/attendance/" + att.getId() + "/rating")
                        .with(csrf())
                        .with(user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A performance rating has already been submitted for this attendance record"));
    }

    @Test
    @DisplayName("6. Out-of-range rating scores (< 1 or > 5) fail validation (Expect 400 Bad Request)")
    void testCreateRating_OutOfRangeScore_Returns400() throws Exception {
        User ngoUser = createUniqueUser("ngo_val", Role.NGO);
        createUniqueNgo(ngoUser, "Wildlife Care");
        User volunteer = createUniqueUser("vol_val", Role.VOLUNTEER);

        Attendance att = createTestAttendance(ngoUser, volunteer, AttendanceStatus.PRESENT, BigDecimal.valueOf(3.0));

        // Score 6 is invalid
        RatingCreateRequest invalidReq = new RatingCreateRequest(6, 4, 5, "Too high");

        mockMvc.perform(post("/api/attendance/" + att.getId() + "/rating")
                        .with(csrf())
                        .with(user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.punctuality").exists());

        // Score 0 is invalid
        RatingCreateRequest zeroReq = new RatingCreateRequest(0, 4, 5, "Too low");

        mockMvc.perform(post("/api/attendance/" + att.getId() + "/rating")
                        .with(csrf())
                        .with(user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.punctuality").exists());
    }

    @Test
    @DisplayName("7. Volunteer retrieves their received ratings history (Expect 200 OK)")
    void testGetVolunteerRatings_Success() throws Exception {
        User ngoUser = createUniqueUser("ngo_list", Role.NGO);
        createUniqueNgo(ngoUser, "Urban Trees");
        User volunteer = createUniqueUser("vol_hist", Role.VOLUNTEER);

        Attendance att = createTestAttendance(ngoUser, volunteer, AttendanceStatus.PRESENT, BigDecimal.valueOf(4.0));
        RatingCreateRequest req = new RatingCreateRequest(5, 5, 4, "Great enthusiasm");

        mockMvc.perform(post("/api/attendance/" + att.getId() + "/rating")
                        .with(csrf())
                        .with(user(ngoUser.getEmail()).roles("NGO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/volunteers/me/ratings")
                        .with(user(volunteer.getEmail()).roles("VOLUNTEER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].punctuality").value(5))
                .andExpect(jsonPath("$[0].teamwork").value(5))
                .andExpect(jsonPath("$[0].effort").value(4))
                .andExpect(jsonPath("$[0].averageScore").value(4.67))
                .andExpect(jsonPath("$[0].comment").value("Great enthusiasm"));
    }
}
