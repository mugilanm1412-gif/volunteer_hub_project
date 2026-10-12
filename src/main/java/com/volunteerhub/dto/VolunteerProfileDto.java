package com.volunteerhub.dto;

import java.time.Instant;

/**
 * DTO representing a volunteer's detailed profile.
 */
public class VolunteerProfileDto {

    private Long id;
    private Long userId;
    private String email;
    private String fullName;
    private String phone;
    private String city;
    private String skills;
    private String interests;
    private String bio;
    private boolean showOnLeaderboard;
    private Instant createdAt;

    public VolunteerProfileDto() {
    }

    public VolunteerProfileDto(Long id, Long userId, String email, String fullName, String phone,
                               String city, String skills, String interests, String bio,
                               boolean showOnLeaderboard, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.city = city;
        this.skills = skills;
        this.interests = interests;
        this.bio = bio;
        this.showOnLeaderboard = showOnLeaderboard;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
