package com.volunteerhub.service;

import com.volunteerhub.dto.ChangePasswordRequest;
import com.volunteerhub.dto.UserSummaryDto;
import com.volunteerhub.entity.User;
import com.volunteerhub.exception.BadRequestException;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service providing user profile retrieval and password management.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, AuthService authService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUserSummary(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return authService.buildUserSummary(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("The current password provided is incorrect.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("The new password cannot be the same as the current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
