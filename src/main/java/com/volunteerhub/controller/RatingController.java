package com.volunteerhub.controller;

import com.volunteerhub.dto.RatingCreateRequest;
import com.volunteerhub.dto.VolunteerRatingDto;
import com.volunteerhub.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing endpoints for volunteer performance evaluations and rating history.
 */
@RestController
@RequestMapping("/api")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    /**
     * Submits a performance rating for a volunteer's attendance record.
     * Accessible only by the owning NGO organizer.
     */
    @PostMapping("/attendance/{id}/rating")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<VolunteerRatingDto> createRating(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RatingCreateRequest request) {
        VolunteerRatingDto created = ratingService.createRating(id, userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Retrieves all performance ratings received by the authenticated volunteer.
     */
    @GetMapping("/volunteers/me/ratings")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<List<VolunteerRatingDto>> getMyRatings(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<VolunteerRatingDto> ratings = ratingService.getVolunteerRatings(userDetails.getUsername());
        return ResponseEntity.ok(ratings);
    }
}
