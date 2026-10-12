package com.volunteerhub.service;

import com.volunteerhub.dto.ForgotPasswordRequest;
import com.volunteerhub.dto.ResetPasswordRequest;
import com.volunteerhub.entity.PasswordResetToken;
import com.volunteerhub.entity.User;
import com.volunteerhub.exception.BadRequestException;
import com.volunteerhub.repository.PasswordResetTokenRepository;
import com.volunteerhub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Service managing secure, single-use, time-limited password reset tokens.
 * Plaintext tokens are sent/logged but only SHA-256 hashes are stored.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${server.port:8080}")
    private String serverPort;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        Optional<User> userOptional = userRepository.findByEmail(email);

        // Security rule: Always return generic response; never reveal whether email exists
        if (userOptional.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOptional.get();

        // Generate 32 bytes of cryptographic randomness (64-character hex string)
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = HexFormat.of().formatHex(randomBytes);

        // Compute SHA-256 hash for storage
        String tokenHash = hashToken(rawToken);

        // Remove any existing active reset tokens for this user
        tokenRepository.deleteByUserId(user.getId());

        // Token expires in 30 minutes
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(30));
        PasswordResetToken token = new PasswordResetToken(user, tokenHash, expiresAt);
        tokenRepository.save(token);

        String resetUrl = "http://localhost:" + serverPort + "/reset-password.html?token=" + rawToken;

        // In development or when mail is disabled, log the reset URL to console
        log.info("================================================================================");
        log.info("[PASSWORD RESET LINK] For user {}:", user.getEmail());
        log.info("URL: {}", resetUrl);
        log.info("Token expires in 30 minutes. Single-use only.");
        log.info("================================================================================");
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashToken(request.getToken().trim());

        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new BadRequestException("Invalid or non-existent password reset token."));

        if (token.isUsed()) {
            throw new BadRequestException("This password reset token has already been used.");
        }

        if (token.isExpired()) {
            throw new BadRequestException("This password reset token has expired. Please request a new one.");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate token
        token.setUsedAt(Instant.now());
        tokenRepository.save(token);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
