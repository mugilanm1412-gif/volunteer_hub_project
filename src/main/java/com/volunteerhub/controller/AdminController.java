package com.volunteerhub.controller;

import com.volunteerhub.dto.*;
import com.volunteerhub.entity.EventStatus;
import com.volunteerhub.entity.NgoVerificationStatus;
import com.volunteerhub.entity.Role;
import com.volunteerhub.entity.UserStatus;
import com.volunteerhub.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller providing administrative governance endpoints for users, events,
 * NGO verification, audit logs, and platform statistics.
 */
@RestController
@RequestMapping({"/api/admin", "/admin"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final com.volunteerhub.service.EventReminderService eventReminderService;

    public AdminController(AdminService adminService,
                           com.volunteerhub.service.EventReminderService eventReminderService) {
        this.adminService = adminService;
        this.eventReminderService = eventReminderService;
    }

    // ==========================================
    // 1. NGO Verification & Listing
    // ==========================================

    @PatchMapping("/ngos/{id}/verification")
    public ResponseEntity<AdminNgoVerificationResponse> verifyNgo(
            @PathVariable Long id,
            @Valid @RequestBody AdminNgoVerificationRequest request,
            @AuthenticationPrincipal UserDetails adminUserDetails) {
        AdminNgoVerificationResponse response = adminService.verifyNgo(id, request, adminUserDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ngos")
    public ResponseEntity<List<NgoProfileDto>> listNgos(
            @RequestParam(required = false) NgoVerificationStatus status) {
        List<NgoProfileDto> ngos = adminService.getNgos(status);
        return ResponseEntity.ok(ngos);
    }

    // ==========================================
    // 2. User Lifecycle Management
    // ==========================================

    @GetMapping("/users")
    public ResponseEntity<PageResponseDto<AdminUserDto>> getUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponseDto<AdminUserDto> users = adminService.getUsers(query, role, status, pageable);
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<AdminUserDto> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetails adminUserDetails) {
        AdminUserDto updated = adminService.updateUserStatus(id, request, adminUserDetails.getUsername());
        return ResponseEntity.ok(updated);
    }

    // ==========================================
    // 3. Event Moderation & Governance
    // ==========================================

    @GetMapping("/events")
    public ResponseEntity<PageResponseDto<AdminEventItemDto>> getEvents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) EventStatus status,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponseDto<AdminEventItemDto> events = adminService.getEvents(query, status, pageable);
        return ResponseEntity.ok(events);
    }

    @PostMapping("/events/{id}/remove")
    public ResponseEntity<AdminEventItemDto> removeEvent(
            @PathVariable Long id,
            @Valid @RequestBody AdminEventRemoveRequest request,
            @AuthenticationPrincipal UserDetails adminUserDetails) {
        AdminEventItemDto removed = adminService.removeEvent(id, request, adminUserDetails.getUsername());
        return ResponseEntity.ok(removed);
    }

    // ==========================================
    // 4. Audit Trail Inspection
    // ==========================================

    @GetMapping("/audit-logs")
    public ResponseEntity<PageResponseDto<AuditLogDto>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponseDto<AuditLogDto> logs = adminService.getAuditLogs(action, entityType, pageable);
        return ResponseEntity.ok(logs);
    }

    // ==========================================
    // 5. Platform Summary Statistics
    // ==========================================

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        AdminStatsDto stats = adminService.getPlatformStats();
        return ResponseEntity.ok(stats);
    }

    // ==========================================
    // 6. Automated Reminders Trigger
    // ==========================================

    @PostMapping("/reminders/trigger")
    public ResponseEntity<ReminderTriggerResponse> triggerReminders(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        java.time.LocalDate targetDate = date != null ? date : java.time.LocalDate.now().plusDays(1);
        int sent = eventReminderService.sendUpcomingEventReminders(targetDate);
        String message = String.format("Sent %d reminder notifications for events on %s.", sent, targetDate);
        return ResponseEntity.ok(new ReminderTriggerResponse(sent, targetDate, message));
    }
}
