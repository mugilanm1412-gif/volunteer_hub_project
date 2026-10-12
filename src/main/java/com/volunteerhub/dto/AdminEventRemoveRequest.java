package com.volunteerhub.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for administrative soft-removal of an event.
 */
public class AdminEventRemoveRequest {

    @NotBlank(message = "Removal reason is required")
    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    @JsonAlias({"removedReason", "removalReason"})
    private String reason;

    public AdminEventRemoveRequest() {
    }

    public AdminEventRemoveRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
