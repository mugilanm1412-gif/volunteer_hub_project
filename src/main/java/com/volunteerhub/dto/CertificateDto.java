package com.volunteerhub.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing an issued Certificate of Volunteer Service.
 */
public class CertificateDto {

    private Long id;
    private Long attendanceId;
    private String certificateNumber;
    private LocalDateTime issuedOn;
    private Long eventId;
    private String eventTitle;
    private String ngoName;
    private String volunteerName;
    private BigDecimal hoursWorked;
    private String downloadUrl;

    public CertificateDto() {
    }

    public CertificateDto(Long id, Long attendanceId, String certificateNumber, LocalDateTime issuedOn,
                          Long eventId, String eventTitle, String ngoName, String volunteerName,
                          BigDecimal hoursWorked, String downloadUrl) {
        this.id = id;
        this.attendanceId = attendanceId;
        this.certificateNumber = certificateNumber;
        this.issuedOn = issuedOn;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.ngoName = ngoName;
        this.volunteerName = volunteerName;
        this.hoursWorked = hoursWorked;
        this.downloadUrl = downloadUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Long attendanceId) {
        this.attendanceId = attendanceId;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public LocalDateTime getIssuedOn() {
        return issuedOn;
    }

    public void setIssuedOn(LocalDateTime issuedOn) {
        this.issuedOn = issuedOn;
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

    public String getVolunteerName() {
        return volunteerName;
    }

    public void setVolunteerName(String volunteerName) {
        this.volunteerName = volunteerName;
    }

    public BigDecimal getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(BigDecimal hoursWorked) {
        this.hoursWorked = hoursWorked;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
