package com.volunteerhub.controller;

import com.volunteerhub.dto.AdminDashboardDto;
import com.volunteerhub.dto.NgoDashboardDto;
import com.volunteerhub.dto.VolunteerDashboardDto;
import com.volunteerhub.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing role-scoped consolidated dashboard telemetry for Volunteers, NGOs, and Administrators.
 */
@RestController
@RequestMapping({"/api/dashboard", "/dashboard"})
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Retrieves personalized dashboard metrics, upcoming initiatives, and recent activity for the volunteer.
     */
    @GetMapping("/volunteer")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<VolunteerDashboardDto> getVolunteerDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        VolunteerDashboardDto dto = dashboardService.getVolunteerDashboard(userDetails.getUsername());
        return ResponseEntity.ok(dto);
    }

    /**
     * Retrieves organization metrics, applicant figures, and activity analytics for the NGO.
     */
    @GetMapping("/ngo")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<NgoDashboardDto> getNgoDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        NgoDashboardDto dto = dashboardService.getNgoDashboard(userDetails.getUsername());
        return ResponseEntity.ok(dto);
    }

    /**
     * Retrieves platform-wide governance metrics and activity analytics for administrators.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardDto> getAdminDashboard() {
        AdminDashboardDto dto = dashboardService.getAdminDashboard();
        return ResponseEntity.ok(dto);
    }
}
