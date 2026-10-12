package com.volunteerhub.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Summary DTO of a recent event attendance/activity for the volunteer dashboard.
 */
public class RecentActivityItemDto {

    private Long eventId;
    private String eventTitle;
    private LocalDate date;
    private BigDecimal hoursWorked;
    private String status;
    private Double rating;

    public RecentActivityItemDto() {
    }

    public RecentActivityItemDto(Long eventId, String eventTitle, LocalDate date,
                                 BigDecimal hoursWorked, String status, Double rating) {
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.date = date;
        this.hoursWorked = hoursWorked;
        this.status = status;
        this.rating = rating;
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(BigDecimal hoursWorked) {
        this.hoursWorked = hoursWorked;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }
}
