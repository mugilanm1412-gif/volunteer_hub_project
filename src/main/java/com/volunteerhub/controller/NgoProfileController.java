package com.volunteerhub.controller;

import com.volunteerhub.dto.NgoProfileDto;
import com.volunteerhub.dto.NgoProfileUpdateRequest;
import com.volunteerhub.service.NgoProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller allowing NGOs to view and update their organization profile.
 */
@RestController
@RequestMapping("/api/ngos/me/profile")
@PreAuthorize("hasRole('NGO')")
public class NgoProfileController {

    private final NgoProfileService ngoProfileService;

    public NgoProfileController(NgoProfileService ngoProfileService) {
        this.ngoProfileService = ngoProfileService;
    }

    @GetMapping
    public ResponseEntity<NgoProfileDto> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        NgoProfileDto profile = ngoProfileService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<NgoProfileDto> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                                       @Valid @RequestBody NgoProfileUpdateRequest request) {
        NgoProfileDto updated = ngoProfileService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(updated);
    }
}
