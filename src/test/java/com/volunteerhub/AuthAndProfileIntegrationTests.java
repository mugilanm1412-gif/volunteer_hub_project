package com.volunteerhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteerhub.dto.*;
import com.volunteerhub.entity.Role;
import com.volunteerhub.entity.User;
import com.volunteerhub.entity.UserStatus;
import com.volunteerhub.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthAndProfileIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("P2-01: Public volunteer registration succeeds and returns 201 Created")
    void testVolunteerRegistration_Success() throws Exception {
        String uniqueEmail = "newvolunteer_" + System.currentTimeMillis() + "@example.com";
        VolunteerRegisterRequest req = new VolunteerRegisterRequest();
        req.setEmail(uniqueEmail);
        req.setPassword("Password@123");
        req.setFullName("Test Volunteer");
        req.setCity("Eco City");
        req.setSkills("Gardening, First Aid");
        req.setShowOnLeaderboard(true);

        mockMvc.perform(post("/api/auth/register/volunteer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email", is(uniqueEmail)))
            .andExpect(jsonPath("$.role", is("VOLUNTEER")));
    }

    @Test
    @DisplayName("P2-02: Duplicate email registration returns HTTP 409 Conflict")
    void testVolunteerRegistration_DuplicateEmail_Conflict() throws Exception {
        // alice@example.com is seeded in Flyway V2
        VolunteerRegisterRequest req = new VolunteerRegisterRequest();
        req.setEmail("alice@example.com");
        req.setPassword("Password@123");
        req.setFullName("Duplicate Alice");

        mockMvc.perform(post("/api/auth/register/volunteer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", is(409)))
            .andExpect(jsonPath("$.error", is("Conflict")));
    }

    @Test
    @DisplayName("P2-03: Login with valid credentials establishes session and returns UserSummary")
    void testLogin_Success() throws Exception {
        LoginRequest req = new LoginRequest("alice@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", is("alice@example.com")))
            .andExpect(jsonPath("$.role", is("VOLUNTEER")))
            .andExpect(jsonPath("$.displayName", is("Alice Smith")));
    }

    @Test
    @DisplayName("P2-04: Login with bad credentials returns HTTP 401 Unauthorized")
    void testLogin_WrongPassword() throws Exception {
        LoginRequest req = new LoginRequest("alice@example.com", "wrongPassword@999");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("P2-05: Login with suspended account returns HTTP 401 Unauthorized")
    void testLogin_SuspendedUser() throws Exception {
        String suspendedEmail = "suspended_" + System.currentTimeMillis() + "@example.com";
        User suspendedUser = new User(suspendedEmail, passwordEncoder.encode("password123"), Role.VOLUNTEER, UserStatus.SUSPENDED);
        userRepository.save(suspendedUser);

        LoginRequest req = new LoginRequest(suspendedEmail, "password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message", containsString("suspended")));
    }

    @Test
    @DisplayName("P2-06: Unauthenticated API request returns JSON 401 instead of HTML redirect")
    void testUnauthenticatedAccess_ReturnsJson401() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("Content-Type", containsString("application/json")))
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("P2-07: Volunteer accessing NGO-only endpoint returns JSON 403 Forbidden")
    @WithMockUser(username = "alice@example.com", roles = {"VOLUNTEER"})
    void testVolunteerAccessingNgoEndpoint_ReturnsJson403() throws Exception {
        mockMvc.perform(get("/api/ngos/me/profile")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status", is(403)))
            .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("P2-08: Forgot password returns 200 OK without disclosing email existence")
    void testForgotPassword_GenericResponse() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest("nonexistent_user_9999@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message", containsString("If this email is registered")));
    }

    @Test
    @DisplayName("P2-09: Authenticated volunteer can retrieve their profile")
    @WithMockUser(username = "alice@example.com", roles = {"VOLUNTEER"})
    void testGetVolunteerProfile_Success() throws Exception {
        mockMvc.perform(get("/api/volunteers/me/profile")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName", is("Alice Smith")))
            .andExpect(jsonPath("$.email", is("alice@example.com")));
    }

    @Test
    @DisplayName("P2-10: Authenticated NGO can retrieve their profile")
    @WithMockUser(username = "contact@greenearth.org", roles = {"NGO"})
    void testGetNgoProfile_Success() throws Exception {
        mockMvc.perform(get("/api/ngos/me/profile")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", is("Green Earth Foundation")))
            .andExpect(jsonPath("$.verificationStatus", is("VERIFIED")));
    }
}
