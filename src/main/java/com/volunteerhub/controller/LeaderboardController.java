package com.volunteerhub.controller;

import com.volunteerhub.dto.LeaderboardEntryDto;
import com.volunteerhub.dto.PageResponseDto;
import com.volunteerhub.service.PerformanceService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public REST controller for the volunteer leaderboard.
 * Preserves volunteer privacy and streams paginated ranking data efficiently.
 */
@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final PerformanceService performanceService;

    public LeaderboardController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    /**
     * Retrieves paginated public leaderboard rankings.
     * Default page = 0, size = 10.
     */
    @GetMapping
    public ResponseEntity<PageResponseDto<LeaderboardEntryDto>> getLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // Enforce maximum page size to protect backend performance
        int boundedSize = Math.max(1, Math.min(size, 50));
        int boundedPage = Math.max(0, page);

        Pageable pageable = PageRequest.of(boundedPage, boundedSize);
        PageResponseDto<LeaderboardEntryDto> leaderboard = performanceService.getLeaderboard(pageable);
        return ResponseEntity.ok(leaderboard);
    }
}
