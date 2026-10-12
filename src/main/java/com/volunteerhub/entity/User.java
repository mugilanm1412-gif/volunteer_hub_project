package com.volunteerhub.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Core User entity representing credentials and platform authentication.
 * Associated with either a VolunteerProfile or an NgoProfile depending on role.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private VolunteerProfile volunteerProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private NgoProfile ngoProfile;

    public User() {
    }

    public User(String email, String passwordHash, Role role, UserStatus status) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.createdAt = Instant.now();
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public VolunteerProfile getVolunteerProfile() {
        return volunteerProfile;
    }

    public void setVolunteerProfile(VolunteerProfile volunteerProfile) {
        this.volunteerProfile = volunteerProfile;
        if (volunteerProfile != null) {
            volunteerProfile.setUser(this);
        }
    }

    public NgoProfile getNgoProfile() {
        return ngoProfile;
    }

    public void setNgoProfile(NgoProfile ngoProfile) {
        this.ngoProfile = ngoProfile;
        if (ngoProfile != null) {
            ngoProfile.setUser(this);
        }
    }
}
