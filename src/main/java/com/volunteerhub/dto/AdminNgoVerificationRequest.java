package com.volunteerhub.dto;

import com.volunteerhub.entity.NgoVerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for administrator NGO verification.
 */
public class AdminNgoVerificationRequest {

    @NotNull(message = "Verification status is required (VERIFIED or REJECTED)")
    private NgoVerificationStatus status;

    @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
    private String rejectionReason;

    public AdminNgoVerificationRequest() {
    }

    public AdminNgoVerificationRequest(NgoVerificationStatus status, String rejectionReason) {
        this.status = status;
        this.rejectionReason = rejectionReason;
    }

    public NgoVerificationStatus getStatus() {
        return status;
    }

    public void setStatus(NgoVerificationStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
