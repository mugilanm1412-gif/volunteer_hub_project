package com.volunteerhub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for NGO registration.
 */
public class NgoRegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "NGO Name is required")
    @Size(max = 200, message = "NGO Name cannot exceed 200 characters")
    private String name;

    @NotBlank(message = "Primary cause / category is required")
    @Size(max = 100, message = "Cause cannot exceed 100 characters")
    private String cause;

    private String description;

    @NotBlank(message = "Official registration number is required")
    @Size(max = 100, message = "Registration number cannot exceed 100 characters")
    private String registrationNumber;

    @NotBlank(message = "Contact person name is required")
    @Size(max = 150, message = "Contact person cannot exceed 150 characters")
    private String contactPerson;

    @NotBlank(message = "Contact phone number is required")
    @Size(max = 20, message = "Contact phone cannot exceed 20 characters")
    private String contactPhone;

    @NotBlank(message = "Physical address is required")
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    public NgoRegisterRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
