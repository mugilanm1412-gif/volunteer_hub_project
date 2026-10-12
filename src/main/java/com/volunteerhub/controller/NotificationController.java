package com.volunteerhub.controller;

import com.volunteerhub.dto.ApiResponseDto;
import com.volunteerhub.dto.NotificationDto;
import com.volunteerhub.dto.PageResponseDto;
import com.volunteerhub.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller providing in-app notifications and read-state management.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<PageResponseDto<NotificationDto>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponseDto<NotificationDto> notifications = notificationService.getNotifications(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        long count = notificationService.getUnreadCount(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponseDto> markAsRead(@AuthenticationPrincipal UserDetails userDetails,
                                                     @PathVariable Long id) {
        notificationService.markAsRead(id, userDetails.getUsername());
        return ResponseEntity.ok(new ApiResponseDto("Notification marked as read"));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponseDto> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getUsername());
        return ResponseEntity.ok(new ApiResponseDto("All notifications marked as read"));
    }
}
