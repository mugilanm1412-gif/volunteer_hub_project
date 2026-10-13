package com.volunteerhub.service;

import com.volunteerhub.dto.*;
import com.volunteerhub.entity.*;
import com.volunteerhub.exception.BadRequestException;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing administrator operations including NGO verification,
 * user lifecycle status updates, event removal, audit log inspection, and platform statistics.
 */
@Service
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final EventRepository eventRepository;
    private final ApplicationRepository applicationRepository;
    private final AttendanceRepository attendanceRepository;
    private final CertificateRepository certificateRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public AdminService(UserRepository userRepository,
                        NgoProfileRepository ngoProfileRepository,
                        VolunteerProfileRepository volunteerProfileRepository,
                        EventRepository eventRepository,
                        ApplicationRepository applicationRepository,
                        AttendanceRepository attendanceRepository,
                        CertificateRepository certificateRepository,
                        AuditLogRepository auditLogRepository,
                        AuditLogService auditLogService,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.ngoProfileRepository = ngoProfileRepository;
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.eventRepository = eventRepository;
        this.applicationRepository = applicationRepository;
        this.attendanceRepository = attendanceRepository;
        this.certificateRepository = certificateRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    /**
     * Verifies or rejects an NGO organization application.
     */
    public AdminNgoVerificationResponse verifyNgo(Long ngoProfileId,
                                                  AdminNgoVerificationRequest request,
                                                  String adminEmail) {
        User adminUser = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with email: " + adminEmail));

        NgoProfile ngoProfile = ngoProfileRepository.findById(ngoProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found with ID: " + ngoProfileId));

        if (request.getStatus() == NgoVerificationStatus.REJECTED) {
            if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                throw new BadRequestException("Rejection reason is required when rejecting an NGO.");
            }
            ngoProfile.setRejectionReason(request.getRejectionReason().trim());
        } else if (request.getStatus() == NgoVerificationStatus.VERIFIED) {
            ngoProfile.setRejectionReason(null);
        }

        ngoProfile.setVerificationStatus(request.getStatus());
        ngoProfile = ngoProfileRepository.save(ngoProfile);

        // Record audit log entry
        String action = request.getStatus() == NgoVerificationStatus.VERIFIED ? "VERIFY_NGO" : "REJECT_NGO";
        String details = "Administrator " + adminUser.getEmail() + " set status to " + request.getStatus() +
                (request.getRejectionReason() != null ? ". Reason: " + request.getRejectionReason() : "");
        auditLogService.logAction(adminUser, action, "NGO_PROFILE", ngoProfile.getId(), details);

        // Dispatch in-app notification to the NGO organization user
        if (request.getStatus() == NgoVerificationStatus.VERIFIED) {
            notificationService.createNotification(
                    ngoProfile.getUser(),
                    "NGO_VERIFIED",
                    "Account Verified",
                    "Your NGO " + ngoProfile.getName() + " has been successfully verified by Administrator.",
                    "NGO_PROFILE",
                    ngoProfile.getId()
            );
        } else {
            notificationService.createNotification(
                    ngoProfile.getUser(),
                    "NGO_REJECTED",
                    "Verification Rejected",
                    "Your NGO " + ngoProfile.getName() + " verification was rejected. Reason: " + request.getRejectionReason(),
                    "NGO_PROFILE",
                    ngoProfile.getId()
            );
        }

        return new AdminNgoVerificationResponse(
                ngoProfile.getId(),
                ngoProfile.getName(),
                ngoProfile.getVerificationStatus(),
                "NGO verification status updated successfully."
        );
    }

    /**
     * Lists NGOs, optionally filtered by verification status.
     */
    @Transactional(readOnly = true)
    public List<NgoProfileDto> getNgos(NgoVerificationStatus status) {
        List<NgoProfile> profiles = (status != null)
                ? ngoProfileRepository.findByVerificationStatus(status)
                : ngoProfileRepository.findAll();

        return profiles.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Lists and searches platform users with pagination.
     */
    @Transactional(readOnly = true)
    public PageResponseDto<AdminUserDto> getUsers(String query, Role role, UserStatus status, Pageable pageable) {
        Page<User> page = userRepository.searchUsers(query, role, status, pageable);
        Page<AdminUserDto> dtoPage = page.map(this::toAdminUserDto);
        return PageResponseDto.of(dtoPage);
    }

    /**
     * Updates account lifecycle status (activate, suspend, deactivate).
     * Strictly prevents an administrator from suspending or deactivating their own account.
     */
    public AdminUserDto updateUserStatus(Long userId, UserStatusUpdateRequest request, String adminEmail) {
        User adminUser = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with email: " + adminEmail));

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Self-protection check
        if (adminUser.getId().equals(targetUser.getId())) {
            throw new BadRequestException("Administrators cannot change their own account status.");
        }

        UserStatus oldStatus = targetUser.getStatus();
        targetUser.setStatus(request.getStatus());
        targetUser = userRepository.save(targetUser);

        // Audit Log
        String details = String.format("Admin %s updated status of user %s (%s) from %s to %s.",
                adminUser.getEmail(), targetUser.getEmail(), targetUser.getRole(), oldStatus, targetUser.getStatus());
        auditLogService.logAction(adminUser, "USER_STATUS_CHANGE", "USER", targetUser.getId(), details);

        return toAdminUserDto(targetUser);
    }

    /**
     * Lists and searches platform events with pagination for moderation.
     */
    @Transactional(readOnly = true)
    public PageResponseDto<AdminEventItemDto> getEvents(String query, EventStatus status, Pageable pageable) {
        Page<Event> page = eventRepository.searchAdminEvents(status, query, pageable);
        Page<AdminEventItemDto> dtoPage = page.map(this::toAdminEventItemDto);
        return PageResponseDto.of(dtoPage);
    }

    /**
     * Soft-removes an inappropriate event with a mandatory reason.
     * Notifies all registered volunteers and the host NGO in-app.
     */
    public AdminEventItemDto removeEvent(Long eventId, AdminEventRemoveRequest request, String adminEmail) {
        User adminUser = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with email: " + adminEmail));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BadRequestException("Removal reason is mandatory when removing an event.");
        }

        String reason = request.getReason().trim();
        event.setStatus(EventStatus.REMOVED);
        event.setRemovedReason(reason);
        Event saved = eventRepository.save(event);

        // Notify registered volunteers (both APPROVED and PENDING registrations)
        List<Application> registrations = applicationRepository.findByEventIdOrderByAppliedAtAsc(event.getId());
        for (Application app : registrations) {
            if (app.getStatus() == ApplicationStatus.APPROVED || app.getStatus() == ApplicationStatus.PENDING) {
                notificationService.createNotification(
                        app.getVolunteer(),
                        "EVENT_REMOVED",
                        "Initiative Removed by Administration",
                        String.format("The initiative '%s' was removed by platform administration. Reason: %s",
                                event.getTitle(), reason),
                        "EVENT",
                        event.getId()
                );
            }
        }

        // Notify host NGO organizer
        if (event.getNgo() != null) {
            notificationService.createNotification(
                    event.getNgo(),
                    "EVENT_REMOVED",
                    "Your Initiative Was Removed by Administration",
                    String.format("Your initiative '%s' was removed by platform administration. Reason: %s",
                            event.getTitle(), reason),
                    "EVENT",
                    event.getId()
            );
        }

        // Record audit trail entry
        String details = String.format("Admin %s removed event '%s' (ID %d). Reason: %s",
                adminUser.getEmail(), event.getTitle(), event.getId(), reason);
        auditLogService.logAction(adminUser, "EVENT_REMOVED", "EVENT", event.getId(), details);

        return toAdminEventItemDto(saved);
    }

    /**
     * Retrieves platform audit logs with pagination and optional filtering.
     */
    @Transactional(readOnly = true)
    public PageResponseDto<AuditLogDto> getAuditLogs(String action, String entityType, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.searchAuditLogs(action, entityType, pageable);
        Page<AuditLogDto> dtoPage = page.map(this::toAuditLogDto);
        return PageResponseDto.of(dtoPage);
    }

    /**
     * Compiles platform-wide governance metrics for administrative oversight.
     */
    @Transactional(readOnly = true)
    public AdminStatsDto getPlatformStats() {
        long totalUsers = userRepository.count();
        long totalVolunteers = userRepository.countByRole(Role.VOLUNTEER);
        long totalNgos = userRepository.countByRole(Role.NGO);
        long totalAdmins = userRepository.countByRole(Role.ADMIN);

        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long suspendedUsers = userRepository.countByStatus(UserStatus.SUSPENDED);
        long deactivatedUsers = userRepository.countByStatus(UserStatus.DEACTIVATED);

        long totalEvents = eventRepository.count();
        long publishedEvents = eventRepository.countByStatus(EventStatus.PUBLISHED);
        long completedEvents = eventRepository.countByStatus(EventStatus.COMPLETED);
        long removedEvents = eventRepository.countByStatus(EventStatus.REMOVED);
        long draftEvents = eventRepository.countByStatus(EventStatus.DRAFT);

        long totalRegistrations = applicationRepository.count();
        BigDecimal totalVerifiedHours = attendanceRepository.sumTotalVerifiedHours();
        if (totalVerifiedHours == null) {
            totalVerifiedHours = BigDecimal.ZERO;
        }

        long totalCertificates = certificateRepository.count();
        long pendingNgos = ngoProfileRepository.countByVerificationStatus(NgoVerificationStatus.PENDING);

        return new AdminStatsDto(
                totalUsers,
                totalVolunteers,
                totalNgos,
                totalAdmins,
                activeUsers,
                suspendedUsers,
                deactivatedUsers,
                totalEvents,
                publishedEvents,
                completedEvents,
                removedEvents,
                draftEvents,
                totalRegistrations,
                totalVerifiedHours,
                totalCertificates,
                pendingNgos
        );
    }

    private AdminUserDto toAdminUserDto(User user) {
        String displayName = user.getEmail();
        if (user.getRole() == Role.VOLUNTEER) {
            displayName = volunteerProfileRepository.findByUserId(user.getId())
                    .map(p -> p.getFullName())
                    .orElse(user.getEmail());
        } else if (user.getRole() == Role.NGO) {
            displayName = ngoProfileRepository.findByUserId(user.getId())
                    .map(p -> p.getName())
                    .orElse(user.getEmail());
        } else if (user.getRole() == Role.ADMIN) {
            displayName = "Administrator";
        }

        return new AdminUserDto(
                user.getId(),
                user.getEmail(),
                displayName,
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    private AdminEventItemDto toAdminEventItemDto(Event event) {
        String ngoName = "NGO";
        Long ngoId = null;
        if (event.getNgo() != null) {
            ngoId = event.getNgo().getId();
            ngoName = ngoProfileRepository.findByUserId(ngoId)
                    .map(p -> p.getName())
                    .orElse(event.getNgo().getEmail());
        }

        int approvedCount = (int) applicationRepository.countByEventIdAndStatus(event.getId(), ApplicationStatus.APPROVED);

        return new AdminEventItemDto(
                event.getId(),
                ngoId,
                ngoName,
                event.getTitle(),
                event.getCategory(),
                event.getLocation(),
                event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getCapacity(),
                approvedCount,
                event.getStatus(),
                event.getRemovedReason(),
                event.getCreatedAt()
        );
    }

    private AuditLogDto toAuditLogDto(AuditLog a) {
        Long actorId = a.getActor() != null ? a.getActor().getId() : null;
        String actorEmail = a.getActor() != null ? a.getActor().getEmail() : "SYSTEM";

        return new AuditLogDto(
                a.getId(),
                actorId,
                actorEmail,
                a.getAction(),
                a.getEntityType(),
                a.getEntityId(),
                a.getDetails(),
                a.getCreatedAt()
        );
    }

    private NgoProfileDto toDto(NgoProfile profile) {
        return new NgoProfileDto(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getEmail(),
                profile.getName(),
                profile.getCause(),
                profile.getDescription(),
                profile.getRegistrationNumber(),
                profile.getContactPerson(),
                profile.getContactPhone(),
                profile.getAddress(),
                profile.getVerificationStatus(),
                profile.getRejectionReason(),
                profile.getCreatedAt()
        );
    }
}
