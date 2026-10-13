package com.volunteerhub.service;

import com.volunteerhub.dto.LoginRequest;
import com.volunteerhub.dto.NgoRegisterRequest;
import com.volunteerhub.dto.UserSummaryDto;
import com.volunteerhub.dto.VolunteerRegisterRequest;
import com.volunteerhub.entity.*;
import com.volunteerhub.exception.ConflictException;
import com.volunteerhub.exception.UnauthorizedException;
import com.volunteerhub.repository.NgoProfileRepository;
import com.volunteerhub.repository.UserRepository;
import com.volunteerhub.repository.VolunteerProfileRepository;
import com.volunteerhub.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user authentication, registration, session persistence, and logout.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthService(UserRepository userRepository,
                       VolunteerProfileRepository volunteerProfileRepository,
                       NgoProfileRepository ngoProfileRepository,
                       PasswordEncoder passwordEncoder,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.ngoProfileRepository = ngoProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Transactional
    public UserSummaryDto registerVolunteer(VolunteerRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new ConflictException("An account with this email address already exists.");
        }

        User user = new User(
            request.getEmail().toLowerCase().trim(),
            passwordEncoder.encode(request.getPassword()),
            Role.VOLUNTEER,
            UserStatus.ACTIVE
        );
        user = userRepository.save(user);

        VolunteerProfile profile = new VolunteerProfile(
            user,
            request.getFullName().trim(),
            request.getPhone(),
            request.getCity(),
            request.getSkills(),
            request.getInterests(),
            request.getBio(),
            request.isShowOnLeaderboard()
        );
        volunteerProfileRepository.save(profile);

        // In-app welcome notification
        notificationService.createNotification(
            user,
            "WELCOME",
            "Welcome to Volunteer Hub!",
            "Your volunteer profile is active. Browse community events and register to start contributing verified hours.",
            "USER",
            user.getId()
        );

        return new UserSummaryDto(user.getId(), user.getEmail(), user.getRole(), user.getStatus(), profile.getFullName(), null);
    }

    @Transactional
    public UserSummaryDto registerNgo(NgoRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new ConflictException("An account with this email address already exists.");
        }
        if (ngoProfileRepository.existsByRegistrationNumber(request.getRegistrationNumber().trim())) {
            throw new ConflictException("An NGO with this registration number is already registered.");
        }

        User user = new User(
            request.getEmail().toLowerCase().trim(),
            passwordEncoder.encode(request.getPassword()),
            Role.NGO,
            UserStatus.ACTIVE
        );
        user = userRepository.save(user);

        NgoProfile profile = new NgoProfile(
            user,
            request.getName().trim(),
            request.getCause().trim(),
            request.getDescription(),
            request.getRegistrationNumber().trim(),
            request.getContactPerson().trim(),
            request.getContactPhone().trim(),
            request.getAddress().trim(),
            NgoVerificationStatus.PENDING
        );
        ngoProfileRepository.save(profile);

        // In-app welcome notification for NGO
        notificationService.createNotification(
            user,
            "NGO_REGISTERED",
            "Registration Received",
            "Welcome to Volunteer Hub! Your organization profile is pending administrator verification before you can publish events.",
            "NGO_PROFILE",
            profile.getId()
        );

        return new UserSummaryDto(user.getId(), user.getEmail(), user.getRole(), user.getStatus(), profile.getName(), profile.getVerificationStatus());
    }

    @Transactional(readOnly = true)
    public UserSummaryDto login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new UnauthorizedException("Your account has been suspended. Please contact platform administration.");
        }
        if (user.getStatus() == UserStatus.DEACTIVATED) {
            throw new UnauthorizedException("Your account is deactivated.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Establish Spring Security Authentication Context
        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Persist Security Context into the HTTP Session
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return buildUserSummary(user);
    }

    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public UserSummaryDto buildUserSummary(User user) {
        String displayName = user.getEmail();
        NgoVerificationStatus verificationStatus = null;

        if (user.getRole() == Role.VOLUNTEER) {
            displayName = volunteerProfileRepository.findByUserId(user.getId())
                .map(p -> p.getFullName())
                .orElse(user.getEmail());
        } else if (user.getRole() == Role.NGO) {
            NgoProfile ngo = ngoProfileRepository.findByUserId(user.getId()).orElse(null);
            if (ngo != null) {
                displayName = ngo.getName();
                verificationStatus = ngo.getVerificationStatus();
            }
        } else if (user.getRole() == Role.ADMIN) {
            displayName = "Administrator";
        }

        return new UserSummaryDto(user.getId(), user.getEmail(), user.getRole(), user.getStatus(), displayName, verificationStatus);
    }
}
