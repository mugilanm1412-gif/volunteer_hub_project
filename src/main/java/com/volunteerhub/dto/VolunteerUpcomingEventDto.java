package com.volunteerhub.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Summary DTO of an approved upcoming event for the volunteer's dashboard.
 */
public class VolunteerUpcomingEventDto {

    private Long id;
    private String title;
    private String category;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private String ngoName;

    public VolunteerUpcomingEventDto() {
    }

    public VolunteerUpcomingEventDto(Long id, String title, String category, LocalDate eventDate,
                                     LocalTime startTime, LocalTime endTime, String location, String ngoName) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.ngoName = ngoName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNgoName() {
        return ngoName;
    }

    public void setNgoName(String ngoName) {
        this.ngoName = ngoName;
    }
}
