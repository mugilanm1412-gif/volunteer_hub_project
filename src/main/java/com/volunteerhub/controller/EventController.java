package com.volunteerhub.controller;

import com.volunteerhub.dto.*;
import com.volunteerhub.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller providing endpoints for event management, search, and lifecycle transitions.
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Lists published events matching query parameters (Public / Authenticated).
     */
    @GetMapping
    public ResponseEntity<PageResponseDto<EventDto>> searchEvents(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "false") Boolean hasSlots,
            @PageableDefault(size = 10, sort = "eventDate", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponseDto<EventDto> result = eventService.searchEvents(q, category, location, from, to, hasSlots, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves events published by the authenticated NGO.
     */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<PageResponseDto<EventDto>> getMyEvents(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponseDto<EventDto> result = eventService.getNgoEvents(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves event details by ID (Public / Authenticated).
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getEventById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        EventDto dto = eventService.getEventById(id, email);
        return ResponseEntity.ok(dto);
    }

    /**
     * Creates a new volunteering event (Verified NGO only).
     */
    @PostMapping
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<EventDto> createEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EventCreateRequest request) {
        EventDto created = eventService.createEvent(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing event (Owner NGO only).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<EventDto> updateEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EventUpdateRequest request) {
        EventDto updated = eventService.updateEvent(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes an event if zero registrations exist (Owner NGO only).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        eventService.deleteEvent(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Cancels an event and notifies registered volunteers (Owner NGO only).
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<EventDto> cancelEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) EventCancelRequest request) {
        EventDto cancelled = eventService.cancelEvent(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(cancelled);
    }

    /**
     * Marks an event as COMPLETED (Owner NGO only).
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<EventDto> completeEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        EventDto completed = eventService.completeEvent(id, userDetails.getUsername());
        return ResponseEntity.ok(completed);
    }
}
