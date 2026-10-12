package com.volunteerhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteerhub.dto.*;
import com.volunteerhub.entity.Event;
import com.volunteerhub.entity.EventStatus;
import com.volunteerhub.entity.NgoProfile;
import com.volunteerhub.entity.NgoVerificationStatus;
import com.volunteerhub.entity.User;
import com.volunteerhub.repository.EventRepository;
import com.volunteerhub.repository.NgoProfileRepository;
import com.volunteerhub.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EventsAndVerificationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NgoProfileRepository ngoProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    private Event createFreshEvent(String title, Integer capacity, EventStatus status) {
        User ngoUser = userRepository.findByEmail("contact@greenearth.org").orElseThrow();
        Event event = new Event(
            ngoUser,
            title,
            "Description for " + title,
            "Environment",
            "Eco Park, Sector 5",
            LocalDate.now().plusDays(10),
            LocalTime.of(10, 0),
            LocalTime.of(14, 0),
            "Teamwork, Enthusiasm",
            capacity,
            status != null ? status : EventStatus.PUBLISHED
        );
        return eventRepository.save(event);
    }

    @Test
    @DisplayName("P3a-01: Admin can verify a pending NGO")
    @WithMockUser(username = "admin@volunteerhub.org", roles = {"ADMIN"})
    void testAdminVerifyNgo_Success() throws Exception {
        // Ensure NGO 2 is pending initially
        NgoProfile profile = ngoProfileRepository.findById(2L).orElseThrow();
        profile.setVerificationStatus(NgoVerificationStatus.PENDING);
        ngoProfileRepository.save(profile);

        AdminNgoVerificationRequest req = new AdminNgoVerificationRequest(NgoVerificationStatus.VERIFIED, null);

        mockMvc.perform(patch("/api/admin/ngos/2/verification")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(2)))
            .andExpect(jsonPath("$.verificationStatus", is("VERIFIED")))
            .andExpect(jsonPath("$.message", containsString("updated successfully")));
    }

    @Test
    @DisplayName("P3a-02: Admin rejecting NGO requires rejection reason")
    @WithMockUser(username = "admin@volunteerhub.org", roles = {"ADMIN"})
    void testAdminRejectNgo_MissingReason_ReturnsBadRequest() throws Exception {
        AdminNgoVerificationRequest req = new AdminNgoVerificationRequest(NgoVerificationStatus.REJECTED, "");

        mockMvc.perform(patch("/api/admin/ngos/2/verification")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("P3a-03: Non-admin user cannot verify NGO (returns 403)")
    @WithMockUser(username = "alice@example.com", roles = {"VOLUNTEER"})
    void testVolunteerCannotVerifyNgo_ReturnsForbidden() throws Exception {
        AdminNgoVerificationRequest req = new AdminNgoVerificationRequest(NgoVerificationStatus.VERIFIED, null);

        mockMvc.perform(patch("/api/admin/ngos/2/verification")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("P3a-04: Admin can list NGOs with optional verification status filter")
    @WithMockUser(username = "admin@volunteerhub.org", roles = {"ADMIN"})
    void testAdminListNgos() throws Exception {
        mockMvc.perform(get("/api/admin/ngos")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    @DisplayName("P3a-05: Unverified NGO cannot create an event (returns HTTP 403 Forbidden)")
    @WithMockUser(username = "help@oceanrescue.org", roles = {"NGO"})
    void testUnverifiedNgoCannotCreateEvent_ReturnsForbidden() throws Exception {
        // Ensure help@oceanrescue.org has PENDING verification status
        NgoProfile profile = ngoProfileRepository.findById(2L).orElseThrow();
        profile.setVerificationStatus(NgoVerificationStatus.PENDING);
        ngoProfileRepository.save(profile);

        EventCreateRequest req = new EventCreateRequest();
        req.setTitle("Unauthorized Coastal Event");
        req.setDescription("Attempting to create event without verification");
        req.setCategory("Environment");
        req.setLocation("Coast Beach");
        req.setEventDate(LocalDate.now().plusDays(10));
        req.setStartTime(LocalTime.of(10, 0));
        req.setEndTime(LocalTime.of(14, 0));
        req.setCapacity(20);
        req.setStatus(EventStatus.PUBLISHED);

        mockMvc.perform(post("/api/events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status", is(403)))
            .andExpect(jsonPath("$.message", containsString("Only verified NGOs can create events")));
    }

    @Test
    @DisplayName("P3a-06: Verified NGO creates event successfully (returns 201 Created)")
    @WithMockUser(username = "contact@greenearth.org", roles = {"NGO"})
    void testVerifiedNgoCreateEvent_Success() throws Exception {
        EventCreateRequest req = new EventCreateRequest();
        req.setTitle("Botanical Garden Seedling Workshop " + System.currentTimeMillis());
        req.setDescription("Hands-on learning and seed germination with botanists.");
        req.setCategory("Environment");
        req.setLocation("City Botanical Gardens");
        req.setEventDate(LocalDate.now().plusDays(15));
        req.setStartTime(LocalTime.of(9, 30));
        req.setEndTime(LocalTime.of(13, 0));
        req.setRequiredSkills("Gardening curiosity");
        req.setCapacity(25);
        req.setStatus(EventStatus.PUBLISHED);

        mockMvc.perform(post("/api/events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.capacity", is(25)))
            .andExpect(jsonPath("$.remainingSlots", is(25)))
            .andExpect(jsonPath("$.ngoName", is("Green Earth Foundation")));
    }

    @Test
    @DisplayName("P3a-07: NGO cannot edit another NGO's event (returns HTTP 403 Forbidden)")
    @WithMockUser(username = "help@oceanrescue.org", roles = {"NGO"})
    void testNgoCannotEditAnotherNgosEvent_ReturnsForbidden() throws Exception {
        Event event = createFreshEvent("Other NGO Event", 20, EventStatus.PUBLISHED);

        EventUpdateRequest req = new EventUpdateRequest();
        req.setTitle("Hacked Event Title");
        req.setDescription("Malicious edit attempt");
        req.setCategory("Environment");
        req.setLocation("Secret Location");
        req.setEventDate(LocalDate.now().plusDays(7));
        req.setStartTime(LocalTime.of(9, 0));
        req.setEndTime(LocalTime.of(13, 0));
        req.setCapacity(20);

        mockMvc.perform(put("/api/events/" + event.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status", is(403)))
            .andExpect(jsonPath("$.message", containsString("permission")));
    }

    @Test
    @DisplayName("P3a-08: Owner NGO edits event successfully (returns 200 OK)")
    @WithMockUser(username = "contact@greenearth.org", roles = {"NGO"})
    void testOwnerNgoEditsEvent_Success() throws Exception {
        Event event = createFreshEvent("Editable Event", 15, EventStatus.PUBLISHED);

        EventUpdateRequest req = new EventUpdateRequest();
        req.setTitle("Community Food Relief Packing - Expanded");
        req.setDescription("Updated schedule and higher capacity.");
        req.setCategory("Community Aid");
        req.setLocation("Central Food Depot, Hall 2");
        req.setEventDate(LocalDate.now().plusDays(5));
        req.setStartTime(LocalTime.of(10, 0));
        req.setEndTime(LocalTime.of(15, 0));
        req.setCapacity(18);

        mockMvc.perform(put("/api/events/" + event.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title", is("Community Food Relief Packing - Expanded")))
            .andExpect(jsonPath("$.capacity", is(18)));
    }

    @Test
    @DisplayName("P3a-09: Delete event with existing registrations is refused (returns 400 Bad Request)")
    @WithMockUser(username = "contact@greenearth.org", roles = {"NGO"})
    void testDeleteEventWithRegistrations_Refused() throws Exception {
        // Event 1 has seeded applications
        mockMvc.perform(delete("/api/events/1")
                .with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.message", containsString("existing registrations")));
    }

    @Test
    @DisplayName("P3a-10: Delete event with zero registrations succeeds (returns 204 No Content)")
    @WithMockUser(username = "contact@greenearth.org", roles = {"NGO"})
    void testDeleteEventWithZeroRegistrations_Success() throws Exception {
        Event event = createFreshEvent("Zero Registration Event To Delete", 10, EventStatus.PUBLISHED);

        mockMvc.perform(delete("/api/events/" + event.getId())
                .with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("P3a-11: Cancel event marks status CANCELLED and records reason (returns 200 OK)")
    @WithMockUser(username = "contact@greenearth.org", roles = {"NGO"})
    void testCancelEvent_Success() throws Exception {
        Event event = createFreshEvent("Event To Cancel", 12, EventStatus.PUBLISHED);
        EventCancelRequest req = new EventCancelRequest("Postponed due to facility maintenance.");

        mockMvc.perform(post("/api/events/" + event.getId() + "/cancel")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("CANCELLED")))
            .andExpect(jsonPath("$.removedReason", is("Postponed due to facility maintenance.")));
    }

    @Test
    @DisplayName("P3a-12: Complete event marks status COMPLETED (returns 200 OK)")
    @WithMockUser(username = "contact@greenearth.org", roles = {"NGO"})
    void testCompleteEvent_Success() throws Exception {
        Event event = createFreshEvent("Event To Complete", 12, EventStatus.PUBLISHED);

        mockMvc.perform(post("/api/events/" + event.getId() + "/complete")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    @DisplayName("P3a-13: Public user can view event details by ID (returns 200 OK)")
    void testPublicGetEventDetails() throws Exception {
        mockMvc.perform(get("/api/events/1")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.title", is("Coastal Beach Cleanup Drive")))
            .andExpect(jsonPath("$.ngoName", is("Green Earth Foundation")));
    }
}
