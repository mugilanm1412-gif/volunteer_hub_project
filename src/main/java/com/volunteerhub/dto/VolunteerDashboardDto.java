package com.volunteerhub.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Consolidated dashboard metrics and event feeds for the authenticated volunteer.
 */
public class VolunteerDashboardDto {

    private String volunteerName;
    private BigDecimal verifiedHours;
    private Integer completedEvents;
    private List<VolunteerUpcomingEventDto> upcomingApprovedEvents = new ArrayList<>();
    private Integer upcomingEventsCount;
    private Integer pendingRegistrationsCount;
    private Double attendanceRate;
    private Double averageRating;
    private Double performanceScore;
    private String badge;
    private Integer certificatesCount;
    private List<RecentActivityItemDto> recentActivity = new ArrayList<>();
    private List<CategoryHoursDto> hoursByCategory = new ArrayList<>();

    public VolunteerDashboardDto() {
    }

    public VolunteerDashboardDto(String volunteerName, BigDecimal verifiedHours, Integer completedEvents,
                                 List<VolunteerUpcomingEventDto> upcomingApprovedEvents, Integer upcomingEventsCount,
                                 Integer pendingRegistrationsCount, Double attendanceRate, Double averageRating,
                                 Double performanceScore, String badge, Integer certificatesCount,
                                 List<RecentActivityItemDto> recentActivity, List<CategoryHoursDto> hoursByCategory) {
        this.volunteerName = volunteerName;
        this.verifiedHours = verifiedHours != null ? verifiedHours : BigDecimal.ZERO;
        this.completedEvents = completedEvents != null ? completedEvents : 0;
        this.upcomingApprovedEvents = upcomingApprovedEvents != null ? upcomingApprovedEvents : new ArrayList<>();
        this.upcomingEventsCount = upcomingEventsCount != null ? upcomingEventsCount : 0;
        this.pendingRegistrationsCount = pendingRegistrationsCount != null ? pendingRegistrationsCount : 0;
        this.attendanceRate = attendanceRate != null ? attendanceRate : 0.0;
        this.averageRating = averageRating;
        this.performanceScore = performanceScore != null ? performanceScore : 0.0;
        this.badge = badge;
        this.certificatesCount = certificatesCount != null ? certificatesCount : 0;
        this.recentActivity = recentActivity != null ? recentActivity : new ArrayList<>();
        this.hoursByCategory = hoursByCategory != null ? hoursByCategory : new ArrayList<>();
    }

    public String getVolunteerName() {
        return volunteerName;
    }

    public void setVolunteerName(String volunteerName) {
        this.volunteerName = volunteerName;
    }

    public BigDecimal getVerifiedHours() {
        return verifiedHours;
    }

    public void setVerifiedHours(BigDecimal verifiedHours) {
        this.verifiedHours = verifiedHours;
    }

    public Integer getCompletedEvents() {
        return completedEvents;
    }

    public void setCompletedEvents(Integer completedEvents) {
        this.completedEvents = completedEvents;
    }

    public List<VolunteerUpcomingEventDto> getUpcomingApprovedEvents() {
        return upcomingApprovedEvents;
    }

    public void setUpcomingApprovedEvents(List<VolunteerUpcomingEventDto> upcomingApprovedEvents) {
        this.upcomingApprovedEvents = upcomingApprovedEvents;
    }

    public Integer getUpcomingEventsCount() {
        return upcomingEventsCount;
    }

    public void setUpcomingEventsCount(Integer upcomingEventsCount) {
        this.upcomingEventsCount = upcomingEventsCount;
    }

    public Integer getPendingRegistrationsCount() {
        return pendingRegistrationsCount;
    }

    public void setPendingRegistrationsCount(Integer pendingRegistrationsCount) {
        this.pendingRegistrationsCount = pendingRegistrationsCount;
    }

    public Double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(Double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Double getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(Double performanceScore) {
        this.performanceScore = performanceScore;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public Integer getCertificatesCount() {
        return certificatesCount;
    }

    public void setCertificatesCount(Integer certificatesCount) {
        this.certificatesCount = certificatesCount;
    }

    public List<RecentActivityItemDto> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<RecentActivityItemDto> recentActivity) {
        this.recentActivity = recentActivity;
    }

    public List<CategoryHoursDto> getHoursByCategory() {
        return hoursByCategory;
    }

    public void setHoursByCategory(List<CategoryHoursDto> hoursByCategory) {
        this.hoursByCategory = hoursByCategory;
    }
}
