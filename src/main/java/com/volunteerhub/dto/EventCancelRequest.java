package com.volunteerhub.dto;

import jakarta.validation.constraints.Size;

/**
 * Request payload when cancelling an event.
 */
public class EventCancelRequest {

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    public EventCancelRequest() {
    }

    public EventCancelRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
