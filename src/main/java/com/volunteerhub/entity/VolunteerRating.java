package com.volunteerhub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing performance evaluations provided by NGOs for volunteer shifts.
 * Captures ratings for punctuality, teamwork, and effort (each on a 1-5 scale) and optional comments.
 */
@Entity
@Table(name = "volunteer_ratings")
public class VolunteerRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", nullable = false, unique = true)
    private Attendance attendance;

    @Column(nullable = false)
    private Integer punctuality;

    @Column(nullable = false)
    private Integer teamwork;

    @Column(nullable = false)
    private Integer effort;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rated_by", nullable = false)
    private User ratedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public VolunteerRating() {
    }

    public VolunteerRating(Attendance attendance, Integer punctuality, Integer teamwork, Integer effort,
                           String comment, User ratedBy) {
        this.attendance = attendance;
        this.punctuality = punctuality;
        this.teamwork = teamwork;
        this.effort = effort;
        this.comment = comment;
        this.ratedBy = ratedBy;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Calculates the unweighted mean score of punctuality, teamwork, and effort for this specific rating.
     */
    public double getAverageScore() {
        return (punctuality + teamwork + effort) / 3.0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Attendance getAttendance() {
        return attendance;
    }

    public void setAttendance(Attendance attendance) {
        this.attendance = attendance;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public User getRatedBy() {
        return ratedBy;
    }

    public void setRatedBy(User ratedBy) {
        this.ratedBy = ratedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
