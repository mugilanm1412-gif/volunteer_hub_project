package com.volunteerhub.dto;

import com.volunteerhub.entity.ApplicationStatus;
import com.volunteerhub.entity.EventStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Data Transfer Object for a volunteer's application history ("My Events" tabs).
 */
public class VolunteerApplicationDto {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private String eventDescription;
    private String ngoName;
    private String category;
    private String location;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private ApplicationStatus status;
    private EventStatus eventStatus;
    private LocalDateTime appliedAt;

    public VolunteerApplicationDto() {
    }

    public VolunteerApplicationDto(Long id, Long eventId, String eventTitle, String eventDescription,
                                  String ngoName, String category, String location,
                                  LocalDate eventDate, LocalTime startTime, LocalTime endTime,
                                  ApplicationStatus status, EventStatus eventStatus, LocalDateTime appliedAt) {
        this.id = id;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.eventDescription = eventDescription;
        this.ngoName = ngoName;
        this.category = category;
        this.location = location;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.eventStatus = eventStatus;
        this.appliedAt = appliedAt;
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

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    public String getNgoName() {
        return ngoName;
    }

    public void setNgoName(String ngoName) {
        this.ngoName = ngoName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public EventStatus getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(EventStatus eventStatus) {
        this.eventStatus = eventStatus;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}
