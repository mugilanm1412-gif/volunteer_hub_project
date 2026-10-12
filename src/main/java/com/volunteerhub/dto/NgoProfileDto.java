package com.volunteerhub.dto;

import com.volunteerhub.entity.NgoVerificationStatus;
import java.time.Instant;

/**
 * DTO representing an NGO's profile and verification standing.
 */
public class NgoProfileDto {

    private Long id;
    private Long userId;
    private String email;
    private String name;
    private String cause;
    private String description;
    private String registrationNumber;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private NgoVerificationStatus verificationStatus;
    private String rejectionReason;
    private Instant createdAt;

    public NgoProfileDto() {
    }

    public NgoProfileDto(Long id, Long userId, String email, String name, String cause, String description,
                         String registrationNumber, String contactPerson, String contactPhone, String address,
                         NgoVerificationStatus verificationStatus, String rejectionReason, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.cause = cause;
        this.description = description;
        this.registrationNumber = registrationNumber;
        this.contactPerson = contactPerson;
        this.contactPhone = contactPhone;
        this.address = address;
        this.verificationStatus = verificationStatus;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
