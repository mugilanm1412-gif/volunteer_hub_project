package com.volunteerhub.dto;

import com.volunteerhub.entity.NgoVerificationStatus;

/**
 * Response payload returned after an admin verifies or rejects an NGO.
 */
public class AdminNgoVerificationResponse {

    private Long id;
    private String ngoName;
    private NgoVerificationStatus verificationStatus;
    private String message;

    public AdminNgoVerificationResponse() {
    }

    public AdminNgoVerificationResponse(Long id, String ngoName, NgoVerificationStatus verificationStatus, String message) {
        this.id = id;
        this.ngoName = ngoName;
        this.verificationStatus = verificationStatus;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNgoName() {
        return ngoName;
    }

    public void setNgoName(String ngoName) {
        this.ngoName = ngoName;
    }

    public NgoVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(NgoVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
