package com.volunteerhub.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * NgoProfile stores organizational and contact details for registered NGOs.
 * Requires administrator verification before events can be created or published.
 */
@Entity
@Table(name = "ngo_profiles")
public class NgoProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String cause;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "registration_number", nullable = false, length = 100)
    private String registrationNumber;

    @Column(name = "contact_person", nullable = false, length = 150)
    private String contactPerson;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(nullable = false, length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private NgoVerificationStatus verificationStatus = NgoVerificationStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public NgoProfile() {
    }

    public NgoProfile(User user, String name, String cause, String description, String registrationNumber,
                      String contactPerson, String contactPhone, String address, NgoVerificationStatus verificationStatus) {
        this.user = user;
        this.name = name;
        this.cause = cause;
        this.description = description;
        this.registrationNumber = registrationNumber;
        this.contactPerson = contactPerson;
        this.contactPhone = contactPhone;
        this.address = address;
        this.verificationStatus = verificationStatus != null ? verificationStatus : NgoVerificationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public NgoVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(NgoVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
