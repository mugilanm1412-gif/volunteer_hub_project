package com.volunteerhub.dto;

import com.volunteerhub.entity.Role;
import com.volunteerhub.entity.UserStatus;

import java.time.Instant;

/**
 * DTO representing a platform user for administrative governance.
 */
public class AdminUserDto {

    private Long id;
    private String email;
    private String displayName;
    private Role role;
    private UserStatus status;
    private Instant createdAt;

    public AdminUserDto() {
    }

    public AdminUserDto(Long id, String email, String displayName, Role role, UserStatus status, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
