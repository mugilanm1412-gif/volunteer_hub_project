package com.volunteerhub.dto;

import com.volunteerhub.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for approving or rejecting a volunteer application.
 */
public class ApplicationStatusUpdateRequest {

    @NotNull(message = "Application status is required (APPROVED or REJECTED)")
    private ApplicationStatus status;

    public ApplicationStatusUpdateRequest() {
    }

    public ApplicationStatusUpdateRequest(ApplicationStatus status) {
        this.status = status;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }
}
