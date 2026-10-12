package com.volunteerhub.controller;

import com.volunteerhub.dto.LeaderboardVisibilityRequest;
import com.volunteerhub.dto.ProgressPointDto;
import com.volunteerhub.dto.VolunteerPerformanceDto;
import com.volunteerhub.service.PerformanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for volunteer performance analytics, time-series progress,
 * privacy preferences, and OpenPDF report downloads.
 */
@RestController
@RequestMapping("/api/volunteers/me")
@PreAuthorize("hasRole('VOLUNTEER')")
public class PerformanceController {

    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    /**
     * Retrieves computed performance metrics, scores, and badge level for the authenticated volunteer.
     */
    @GetMapping("/performance")
    public ResponseEntity<VolunteerPerformanceDto> getPerformance(@AuthenticationPrincipal UserDetails userDetails) {
        VolunteerPerformanceDto performance = performanceService.getVolunteerPerformance(userDetails.getUsername());
        return ResponseEntity.ok(performance);
    }

    /**
     * Retrieves historical service hours grouped by month or week.
     */
    @GetMapping("/performance/progress")
    public ResponseEntity<List<ProgressPointDto>> getProgress(
            @RequestParam(defaultValue = "month") String granularity,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ProgressPointDto> progress = performanceService.getProgress(userDetails.getUsername(), granularity);
        return ResponseEntity.ok(progress);
    }

    /**
     * Toggles whether the volunteer is displayed on public leaderboards.
     */
    @PatchMapping("/leaderboard-visibility")
    public ResponseEntity<Map<String, Object>> updateLeaderboardVisibility(
            @Valid @RequestBody LeaderboardVisibilityRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = performanceService.updateLeaderboardVisibility(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Generates and downloads an official OpenPDF performance report and transcript.
     */
    @GetMapping("/performance/report.pdf")
    public ResponseEntity<byte[]> downloadPerformanceReportPdf(@AuthenticationPrincipal UserDetails userDetails) {
        byte[] pdfBytes = performanceService.generatePerformancePdf(userDetails.getUsername());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"volunteer-performance-report.pdf\"");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
