package com.volunteerhub.service;

import com.volunteerhub.dto.AdminNgoVerificationRequest;
import com.volunteerhub.dto.AdminNgoVerificationResponse;
import com.volunteerhub.dto.NgoProfileDto;
import com.volunteerhub.entity.AuditLog;
import com.volunteerhub.entity.NgoProfile;
import com.volunteerhub.entity.NgoVerificationStatus;
import com.volunteerhub.entity.User;
import com.volunteerhub.exception.BadRequestException;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.repository.AuditLogRepository;
import com.volunteerhub.repository.NgoProfileRepository;
import com.volunteerhub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing administrator operations including NGO verification and audit logging.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;

    public AdminService(UserRepository userRepository,
                        NgoProfileRepository ngoProfileRepository,
                        AuditLogRepository auditLogRepository,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.ngoProfileRepository = ngoProfileRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
    }

    @Transactional
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
        auditLogRepository.save(new AuditLog(adminUser, action, "NGO_PROFILE", ngoProfile.getId(), details));

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

    @Transactional(readOnly = true)
    public List<NgoProfileDto> getNgos(NgoVerificationStatus status) {
        List<NgoProfile> profiles = (status != null)
            ? ngoProfileRepository.findByVerificationStatus(status)
            : ngoProfileRepository.findAll();

        return profiles.stream().map(this::toDto).collect(Collectors.toList());
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
