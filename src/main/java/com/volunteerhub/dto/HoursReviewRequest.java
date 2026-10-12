package com.volunteerhub.dto;

import com.volunteerhub.entity.HoursStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for approving or rejecting volunteer service hours.
 */
public class HoursReviewRequest {

    @NotNull(message = "Hours review status is required")
    private HoursStatus status;

    private String reason;

    public HoursReviewRequest() {
    }

    public HoursReviewRequest(HoursStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public HoursStatus getStatus() {
        return status;
    }

    public void setStatus(HoursStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
