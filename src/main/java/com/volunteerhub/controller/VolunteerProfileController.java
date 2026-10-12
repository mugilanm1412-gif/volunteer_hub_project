package com.volunteerhub.controller;

import com.volunteerhub.dto.VolunteerProfileDto;
import com.volunteerhub.dto.VolunteerProfileUpdateRequest;
import com.volunteerhub.service.VolunteerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller allowing volunteers to view and edit their biographical profile.
 */
@RestController
@RequestMapping("/api/volunteers/me/profile")
@PreAuthorize("hasRole('VOLUNTEER')")
public class VolunteerProfileController {

    private final VolunteerProfileService volunteerProfileService;

    public VolunteerProfileController(VolunteerProfileService volunteerProfileService) {
        this.volunteerProfileService = volunteerProfileService;
    }

    @GetMapping
    public ResponseEntity<VolunteerProfileDto> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        VolunteerProfileDto profile = volunteerProfileService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<VolunteerProfileDto> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                                             @Valid @RequestBody VolunteerProfileUpdateRequest request) {
        VolunteerProfileDto updated = volunteerProfileService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(updated);
    }
}
