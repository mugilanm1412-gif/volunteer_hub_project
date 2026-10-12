package com.volunteerhub.controller;

import com.volunteerhub.dto.ActivityDataPointDto;
import com.volunteerhub.dto.CategoryHoursDto;
import com.volunteerhub.entity.Role;
import com.volunteerhub.entity.User;
import com.volunteerhub.repository.UserRepository;
import com.volunteerhub.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller providing aggregated reports for category distributions and periodic activity trends.
 */
@RestController
@RequestMapping({"/api/reports", "/reports"})
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    public ReportController(ReportService reportService, UserRepository userRepository) {
        this.reportService = reportService;
        this.userRepository = userRepository;
    }

    /**
     * Retrieves aggregated volunteering hours grouped by cause category.
     *
     * @param ngoId       optional NGO identifier; if caller is NGO and parameter omitted, automatically scopes to caller
     * @param userDetails authenticated user details
     * @return list of category hour summaries
     */
    @GetMapping("/hours-by-category")
    public ResponseEntity<List<CategoryHoursDto>> getHoursByCategory(
            @RequestParam(required = false) Long ngoId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long effectiveNgoId = resolveNgoId(ngoId, userDetails);
        List<CategoryHoursDto> reports = reportService.getHoursByCategory(effectiveNgoId);
        return ResponseEntity.ok(reports);
    }

    /**
     * Retrieves aggregated activity metrics across time periods (monthly or weekly).
     *
     * @param granularity "MONTHLY" (default) or "WEEKLY"
     * @param ngoId       optional NGO identifier; if caller is NGO and parameter omitted, automatically scopes to caller
     * @param userDetails authenticated user details
     * @return list of activity data points
     */
    @GetMapping("/activity")
    public ResponseEntity<List<ActivityDataPointDto>> getActivity(
            @RequestParam(defaultValue = "MONTHLY") String granularity,
            @RequestParam(required = false) Long ngoId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long effectiveNgoId = resolveNgoId(ngoId, userDetails);
        List<ActivityDataPointDto> activity = reportService.getActivityReport(granularity, effectiveNgoId);
        return ResponseEntity.ok(activity);
    }

    private Long resolveNgoId(Long requestedNgoId, UserDetails userDetails) {
        if (requestedNgoId != null) {
            return requestedNgoId;
        }

        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
            if (user != null && user.getRole() == Role.NGO) {
                return user.getId();
            }
        }

        return null;
    }
}
