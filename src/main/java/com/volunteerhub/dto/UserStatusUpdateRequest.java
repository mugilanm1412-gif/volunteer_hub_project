package com.volunteerhub.dto;

import com.volunteerhub.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating a user's account lifecycle status.
 */
public class UserStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private UserStatus status;

    public UserStatusUpdateRequest() {
    }

    public UserStatusUpdateRequest(UserStatus status) {
        this.status = status;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
