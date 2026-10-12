package com.volunteerhub.dto;

import com.volunteerhub.entity.NgoVerificationStatus;
import com.volunteerhub.entity.Role;
import com.volunteerhub.entity.UserStatus;

/**
 * Summary DTO returned by /api/users/me and /api/auth/login.
 * Never exposes passwords or password hashes.
 */
public class UserSummaryDto {

    private Long id;
    private String email;
    private Role role;
    private UserStatus status;
    private String displayName;
    private NgoVerificationStatus verificationStatus;

    public UserSummaryDto() {
    }

    public UserSummaryDto(Long id, String email, Role role, UserStatus status, String displayName, NgoVerificationStatus verificationStatus) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.status = status;
        this.displayName = displayName;
        this.verificationStatus = verificationStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public NgoVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(NgoVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
}
