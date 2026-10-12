package com.volunteerhub.controller;

import com.volunteerhub.dto.*;
import com.volunteerhub.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller providing endpoints for event registration, application approval,
 * cancellation, and My Events listing.
 */
@RestController
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * Volunteer registers for an event.
     */
    @PostMapping("/api/events/{id}/applications")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<ApplicationDto> applyForEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        ApplicationDto dto = applicationService.applyForEvent(id, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * NGO retrieves all applicant registrations for their event.
     */
    @GetMapping("/api/events/{id}/applications")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<List<EventApplicationItemDto>> getEventApplications(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<EventApplicationItemDto> list = applicationService.getEventApplications(id, userDetails.getUsername());
        return ResponseEntity.ok(list);
    }

    /**
     * NGO approves or rejects a volunteer registration.
     */
    @PatchMapping("/api/applications/{id}/status")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<ApplicationDto> updateApplicationStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        ApplicationDto updated = applicationService.updateApplicationStatus(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Volunteer cancels their existing application.
     */
    @PostMapping("/api/applications/{id}/cancel")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<ApplicationDto> cancelApplication(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        ApplicationDto cancelled = applicationService.cancelApplication(id, userDetails.getUsername());
        return ResponseEntity.ok(cancelled);
    }

    /**
     * Volunteer retrieves their application history categorized by tab ("My Events").
     */
    @GetMapping("/api/volunteers/me/applications")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<PageResponseDto<VolunteerApplicationDto>> getMyApplications(
            @RequestParam(required = false, defaultValue = "upcoming") String tab,
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponseDto<VolunteerApplicationDto> result = applicationService.getVolunteerApplications(
            userDetails.getUsername(), tab, pageable
        );
        return ResponseEntity.ok(result);
    }
}
