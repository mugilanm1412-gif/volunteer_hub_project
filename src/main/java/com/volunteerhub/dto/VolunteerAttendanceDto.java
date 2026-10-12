package com.volunteerhub.dto;

import com.volunteerhub.entity.AttendanceStatus;
import com.volunteerhub.entity.HoursStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representation of an attendance and verified hours record for a volunteer's schedule.
 */
public class VolunteerAttendanceDto {

    private Long id;
    private Long applicationId;
    private Long eventId;
    private String eventTitle;
    private String ngoName;
    private LocalDate eventDate;
    private AttendanceStatus attendanceStatus;
    private BigDecimal hoursWorked;
    private HoursStatus hoursStatus;
    private LocalDateTime reviewedAt;
    private String rejectReason;

    public VolunteerAttendanceDto() {
    }

    public VolunteerAttendanceDto(Long id, Long applicationId, Long eventId, String eventTitle,
                                  String ngoName, LocalDate eventDate, AttendanceStatus attendanceStatus,
                                  BigDecimal hoursWorked, HoursStatus hoursStatus, LocalDateTime reviewedAt,
                                  String rejectReason) {
        this.id = id;
        this.applicationId = applicationId;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.ngoName = ngoName;
        this.eventDate = eventDate;
        this.attendanceStatus = attendanceStatus;
        this.hoursWorked = hoursWorked;
        this.hoursStatus = hoursStatus;
        this.reviewedAt = reviewedAt;
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

    public String getNgoName() {
        return ngoName;
    }

    public void setNgoName(String ngoName) {
        this.ngoName = ngoName;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
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

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
