package com.volunteerhub.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object delivering computed performance metrics and badges for a volunteer.
 */
public class VolunteerPerformanceDto {

    private BigDecimal totalVerifiedHours;
    private int completedEvents;
    private int registeredCompletedEvents;
    private double attendanceRate;
    private Double averageRating;
    private double hoursScore;
    private double attendanceScore;
    private Double ratingScore;
    private double performanceScore;
    private String badge;
    private boolean showOnLeaderboard;

    public VolunteerPerformanceDto() {
    }

    public VolunteerPerformanceDto(BigDecimal totalVerifiedHours, int completedEvents, int registeredCompletedEvents,
                                   double attendanceRate, Double averageRating, double hoursScore,
                                   double attendanceScore, Double ratingScore, double performanceScore,
                                   String badge, boolean showOnLeaderboard) {
        this.totalVerifiedHours = totalVerifiedHours;
        this.completedEvents = completedEvents;
        this.registeredCompletedEvents = registeredCompletedEvents;
        this.attendanceRate = attendanceRate;
        this.averageRating = averageRating;
        this.hoursScore = hoursScore;
        this.attendanceScore = attendanceScore;
        this.ratingScore = ratingScore;
        this.performanceScore = performanceScore;
        this.badge = badge;
        this.showOnLeaderboard = showOnLeaderboard;
    }

    public BigDecimal getTotalVerifiedHours() {
        return totalVerifiedHours;
    }

    public void setTotalVerifiedHours(BigDecimal totalVerifiedHours) {
        this.totalVerifiedHours = totalVerifiedHours;
    }

    public int getCompletedEvents() {
        return completedEvents;
    }

    public void setCompletedEvents(int completedEvents) {
        this.completedEvents = completedEvents;
    }

    public int getRegisteredCompletedEvents() {
        return registeredCompletedEvents;
    }

    public void setRegisteredCompletedEvents(int registeredCompletedEvents) {
        this.registeredCompletedEvents = registeredCompletedEvents;
    }

    public double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public double getHoursScore() {
        return hoursScore;
    }

    public void setHoursScore(double hoursScore) {
        this.hoursScore = hoursScore;
    }

    public double getAttendanceScore() {
        return attendanceScore;
    }

    public void setAttendanceScore(double attendanceScore) {
        this.attendanceScore = attendanceScore;
    }

    public Double getRatingScore() {
        return ratingScore;
    }

    public void setRatingScore(Double ratingScore) {
        this.ratingScore = ratingScore;
    }

    public double getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(double performanceScore) {
        this.performanceScore = performanceScore;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public boolean isShowOnLeaderboard() {
        return showOnLeaderboard;
    }

    public void setShowOnLeaderboard(boolean showOnLeaderboard) {
        this.showOnLeaderboard = showOnLeaderboard;
    }
}
