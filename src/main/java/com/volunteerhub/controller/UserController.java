package com.volunteerhub.controller;

import com.volunteerhub.dto.ApiResponseDto;
import com.volunteerhub.dto.ChangePasswordRequest;
import com.volunteerhub.dto.UserSummaryDto;
import com.volunteerhub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller providing current user information and credential updates.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummaryDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserSummaryDto summary = userService.getCurrentUserSummary(userDetails.getUsername());
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/me/password")
    public ResponseEntity<ApiResponseDto> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                                         @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(new ApiResponseDto("Password changed successfully"));
    }
}
