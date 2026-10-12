package com.volunteerhub.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * VolunteerProfile stores biographical and participation preferences for volunteers.
 */
@Entity
@Table(name = "volunteer_profiles")
public class VolunteerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String city;

    @Column(length = 500)
    private String skills;

    @Column(length = 500)
    private String interests;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "show_on_leaderboard", nullable = false)
    private boolean showOnLeaderboard = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public VolunteerProfile() {
    }

    public VolunteerProfile(User user, String fullName, String phone, String city, String skills, String interests, String bio, boolean showOnLeaderboard) {
        this.user = user;
        this.fullName = fullName;
        this.phone = phone;
        this.city = city;
        this.skills = skills;
        this.interests = interests;
        this.bio = bio;
        this.showOnLeaderboard = showOnLeaderboard;
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
