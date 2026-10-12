package com.volunteerhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating NGO organization details.
 */
public class NgoProfileUpdateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name cannot exceed 200 characters")
    private String name;

    @NotBlank(message = "Cause is required")
    @Size(max = 100, message = "Cause cannot exceed 100 characters")
    private String cause;

    private String description;

    @NotBlank(message = "Contact person is required")
    @Size(max = 150, message = "Contact person cannot exceed 150 characters")
    private String contactPerson;

    @NotBlank(message = "Contact phone is required")
    @Size(max = 20, message = "Contact phone cannot exceed 20 characters")
    private String contactPhone;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    public NgoProfileUpdateRequest() {
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
}
