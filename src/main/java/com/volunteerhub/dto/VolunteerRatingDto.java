package com.volunteerhub.dto;

import java.time.LocalDateTime;

/**
 * Representation of a volunteer rating and review details.
 */
public class VolunteerRatingDto {

    private Long id;
    private Long attendanceId;
    private Long eventId;
    private String eventTitle;
    private String ngoName;
    private Integer punctuality;
    private Integer teamwork;
    private Integer effort;
    private Double averageScore;
    private String comment;
    private LocalDateTime createdAt;

    public VolunteerRatingDto() {
    }

    public VolunteerRatingDto(Long id, Long attendanceId, Long eventId, String eventTitle, String ngoName,
                              Integer punctuality, Integer teamwork, Integer effort, Double averageScore,
                              String comment, LocalDateTime createdAt) {
        this.id = id;
        this.attendanceId = attendanceId;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.ngoName = ngoName;
        this.punctuality = punctuality;
        this.teamwork = teamwork;
        this.effort = effort;
        this.averageScore = averageScore;
        this.comment = comment;
        this.createdAt = createdAt;
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

    public Integer getPunctuality() {
        return punctuality;
    }

    public void setPunctuality(Integer punctuality) {
        this.punctuality = punctuality;
    }

    public Integer getTeamwork() {
        return teamwork;
    }

    public void setTeamwork(Integer teamwork) {
        this.teamwork = teamwork;
    }

    public Integer getEffort() {
        return effort;
    }

    public void setEffort(Integer effort) {
        this.effort = effort;
    }

    public Double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(Double averageScore) {
        this.averageScore = averageScore;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
