package com.volunteerhub.dto;

import com.volunteerhub.entity.ApplicationStatus;
import java.time.LocalDateTime;

/**
 * Detailed application view for NGO event organizers to review applicant details.
 */
public class EventApplicationItemDto {

    private Long id;
    private Long volunteerId;
    private String volunteerName;
    private String volunteerEmail;
    private String volunteerPhone;
    private String volunteerSkills;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;

    public EventApplicationItemDto() {
    }

    public EventApplicationItemDto(Long id, Long volunteerId, String volunteerName, String volunteerEmail,
                                  String volunteerPhone, String volunteerSkills,
                                  ApplicationStatus status, LocalDateTime appliedAt) {
        this.id = id;
        this.volunteerId = volunteerId;
        this.volunteerName = volunteerName;
        this.volunteerEmail = volunteerEmail;
        this.volunteerPhone = volunteerPhone;
        this.volunteerSkills = volunteerSkills;
        this.status = status;
        this.appliedAt = appliedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Long volunteerId) {
        this.volunteerId = volunteerId;
    }

    public String getVolunteerName() {
        return volunteerName;
    }

    public void setVolunteerName(String volunteerName) {
        this.volunteerName = volunteerName;
    }

    public String getVolunteerEmail() {
        return volunteerEmail;
    }

    public void setVolunteerEmail(String volunteerEmail) {
        this.volunteerEmail = volunteerEmail;
    }

    public String getVolunteerPhone() {
        return volunteerPhone;
    }

    public void setVolunteerPhone(String volunteerPhone) {
        this.volunteerPhone = volunteerPhone;
    }

    public String getVolunteerSkills() {
        return volunteerSkills;
    }

    public void setVolunteerSkills(String volunteerSkills) {
        this.volunteerSkills = volunteerSkills;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}
