package com.volunteerhub.dto;

import com.volunteerhub.entity.ApplicationStatus;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for an individual application confirmation or status.
 */
public class ApplicationDto {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private Long volunteerId;
    private String volunteerName;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private String message;

    public ApplicationDto() {
    }

    public ApplicationDto(Long id, Long eventId, String eventTitle, Long volunteerId,
                          String volunteerName, ApplicationStatus status, LocalDateTime appliedAt, String message) {
        this.id = id;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.volunteerId = volunteerId;
        this.volunteerName = volunteerName;
        this.status = status;
        this.appliedAt = appliedAt;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
