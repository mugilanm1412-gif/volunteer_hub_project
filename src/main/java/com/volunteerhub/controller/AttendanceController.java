package com.volunteerhub.controller;

import com.volunteerhub.dto.AttendanceResponseDto;
import com.volunteerhub.dto.BatchAttendanceRequest;
import com.volunteerhub.dto.VolunteerAttendanceDto;
import com.volunteerhub.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller providing endpoints for recording event attendance and querying volunteer hours.
 */
@RestController
@RequestMapping("/api")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /**
     * Batch marks attendance and logs service hours for an event's approved registrations.
     * Accessible only by the owning NGO.
     */
    @PostMapping("/events/{id}/attendance")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<List<AttendanceResponseDto>> markAttendance(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BatchAttendanceRequest request) {
        List<AttendanceResponseDto> result = attendanceService.markAttendance(id, userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Retrieves all recorded attendance entries for an event.
     * Accessible only by the hosting NGO.
     */
    @GetMapping("/events/{id}/attendance")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<List<AttendanceResponseDto>> getEventAttendance(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<AttendanceResponseDto> result = attendanceService.getEventAttendance(id, userDetails.getUsername());
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves attendance history and verified hours for the authenticated volunteer.
     */
    @GetMapping("/volunteers/me/attendance")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<List<VolunteerAttendanceDto>> getMyAttendance(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<VolunteerAttendanceDto> result = attendanceService.getVolunteerAttendance(userDetails.getUsername());
        return ResponseEntity.ok(result);
    }

    /**
     * Reviews and approves or rejects volunteer service hours.
     * Accessible by the hosting NGO or by an Administrator.
     */
    @PatchMapping("/attendance/{id}/hours-review")
    @PreAuthorize("hasAnyRole('NGO', 'ADMIN')")
    public ResponseEntity<AttendanceResponseDto> reviewHours(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody com.volunteerhub.dto.HoursReviewRequest request) {
        AttendanceResponseDto result = attendanceService.reviewHours(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(result);
    }
}
