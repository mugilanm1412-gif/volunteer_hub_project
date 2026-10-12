package com.volunteerhub.controller;

import com.volunteerhub.dto.AdminNgoVerificationRequest;
import com.volunteerhub.dto.AdminNgoVerificationResponse;
import com.volunteerhub.dto.NgoProfileDto;
import com.volunteerhub.entity.NgoVerificationStatus;
import com.volunteerhub.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller providing administrative management endpoints for NGOs.
 */
@RestController
@RequestMapping("/api/admin/ngos")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Verifies or rejects an NGO organization application.
     */
    @PatchMapping("/{id}/verification")
    public ResponseEntity<AdminNgoVerificationResponse> verifyNgo(
            @PathVariable Long id,
            @Valid @RequestBody AdminNgoVerificationRequest request,
            @AuthenticationPrincipal UserDetails adminUserDetails) {
        AdminNgoVerificationResponse response = adminService.verifyNgo(id, request, adminUserDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Lists NGOs, optionally filtered by verification status.
     */
    @GetMapping
    public ResponseEntity<List<NgoProfileDto>> listNgos(
            @RequestParam(required = false) NgoVerificationStatus status) {
        List<NgoProfileDto> ngos = adminService.getNgos(status);
        return ResponseEntity.ok(ngos);
    }
}
