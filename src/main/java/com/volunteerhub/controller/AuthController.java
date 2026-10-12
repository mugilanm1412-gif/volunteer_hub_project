package com.volunteerhub.controller;

import com.volunteerhub.dto.*;
import com.volunteerhub.service.AuthService;
import com.volunteerhub.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller handling unauthenticated user access, registration, session management,
 * and forgotten password recovery.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register/volunteer")
    public ResponseEntity<UserSummaryDto> registerVolunteer(@Valid @RequestBody VolunteerRegisterRequest request) {
        UserSummaryDto response = authService.registerVolunteer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/ngo")
    public ResponseEntity<UserSummaryDto> registerNgo(@Valid @RequestBody NgoRegisterRequest request) {
        UserSummaryDto response = authService.registerNgo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserSummaryDto> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse) {
        UserSummaryDto summary = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(httpRequest, httpResponse);
        return ResponseEntity.ok(new ApiResponseDto("Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDto> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok(new ApiResponseDto("If this email is registered in our platform, a password reset link has been dispatched."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDto> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(new ApiResponseDto("Your password has been successfully reset. You may now log in."));
    }
}
