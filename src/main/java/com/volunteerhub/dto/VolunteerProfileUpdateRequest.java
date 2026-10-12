package com.volunteerhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating volunteer biographical details.
 */
public class VolunteerProfileUpdateRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name cannot exceed 150 characters")
    private String fullName;

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String phone;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 500, message = "Skills cannot exceed 500 characters")
    private String skills;

    @Size(max = 500, message = "Interests cannot exceed 500 characters")
    private String interests;

    private String bio;

    private boolean showOnLeaderboard = true;

    public VolunteerProfileUpdateRequest() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isShowOnLeaderboard() {
        return showOnLeaderboard;
    }

    public void setShowOnLeaderboard(boolean showOnLeaderboard) {
        this.showOnLeaderboard = showOnLeaderboard;
    }
}
