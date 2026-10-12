package com.volunteerhub.dto;

import com.volunteerhub.entity.AttendanceStatus;
import com.volunteerhub.entity.HoursStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Detailed representation of an attendance record returned to the organizer.
 */
public class AttendanceResponseDto {

    private Long id;
    private Long applicationId;
    private Long volunteerId;
    private String volunteerName;
    private Long eventId;
    private String eventTitle;
    private AttendanceStatus attendanceStatus;
    private BigDecimal hoursWorked;
    private HoursStatus hoursStatus;
    private LocalDateTime markedAt;
    private String rejectReason;

    public AttendanceResponseDto() {
    }

    public AttendanceResponseDto(Long id, Long applicationId, Long volunteerId, String volunteerName,
                                 Long eventId, String eventTitle, AttendanceStatus attendanceStatus,
                                 BigDecimal hoursWorked, HoursStatus hoursStatus, LocalDateTime markedAt,
                                 String rejectReason) {
        this.id = id;
        this.applicationId = applicationId;
        this.volunteerId = volunteerId;
        this.volunteerName = volunteerName;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.attendanceStatus = attendanceStatus;
        this.hoursWorked = hoursWorked;
        this.hoursStatus = hoursStatus;
        this.markedAt = markedAt;
        this.rejectReason = rejectReason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
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

    public AttendanceStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(AttendanceStatus attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public BigDecimal getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(BigDecimal hoursWorked) {
        this.hoursWorked = hoursWorked;
    }

    public HoursStatus getHoursStatus() {
        return hoursStatus;
    }

    public void setHoursStatus(HoursStatus hoursStatus) {
        this.hoursStatus = hoursStatus;
    }

    public LocalDateTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(LocalDateTime markedAt) {
        this.markedAt = markedAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    private VolunteerRatingDto rating;

    public VolunteerRatingDto getRating() {
        return rating;
    }

    public void setRating(VolunteerRatingDto rating) {
        this.rating = rating;
    }
}
