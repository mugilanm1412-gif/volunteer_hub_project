package com.volunteerhub.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Consolidated administrative dashboard metrics, category distributions, and activity trends.
 */
public class AdminDashboardDto {

    private Long totalUsers;
    private Long totalVolunteers;
    private Long verifiedNgos;
    private Long pendingNgoVerifications;
    private Long activeEvents;
    private Long completedEvents;
    private BigDecimal totalVerifiedHours;
    private Long recentRegistrationsCount;
    private List<CategoryHoursDto> hoursByCategory = new ArrayList<>();
    private List<ActivityDataPointDto> activity = new ArrayList<>();

    public AdminDashboardDto() {
    }

    public AdminDashboardDto(Long totalUsers, Long totalVolunteers, Long verifiedNgos,
                             Long pendingNgoVerifications, Long activeEvents, Long completedEvents,
                             BigDecimal totalVerifiedHours, Long recentRegistrationsCount,
                             List<CategoryHoursDto> hoursByCategory, List<ActivityDataPointDto> activity) {
        this.totalUsers = totalUsers != null ? totalUsers : 0L;
        this.totalVolunteers = totalVolunteers != null ? totalVolunteers : 0L;
        this.verifiedNgos = verifiedNgos != null ? verifiedNgos : 0L;
        this.pendingNgoVerifications = pendingNgoVerifications != null ? pendingNgoVerifications : 0L;
        this.activeEvents = activeEvents != null ? activeEvents : 0L;
        this.completedEvents = completedEvents != null ? completedEvents : 0L;
        this.totalVerifiedHours = totalVerifiedHours != null ? totalVerifiedHours : BigDecimal.ZERO;
        this.recentRegistrationsCount = recentRegistrationsCount != null ? recentRegistrationsCount : 0L;
        this.hoursByCategory = hoursByCategory != null ? hoursByCategory : new ArrayList<>();
        this.activity = activity != null ? activity : new ArrayList<>();
    }

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Long getTotalVolunteers() {
        return totalVolunteers;
    }

    public void setTotalVolunteers(Long totalVolunteers) {
        this.totalVolunteers = totalVolunteers;
    }

    public Long getVerifiedNgos() {
        return verifiedNgos;
    }

    public void setVerifiedNgos(Long verifiedNgos) {
        this.verifiedNgos = verifiedNgos;
    }

    public Long getPendingNgoVerifications() {
        return pendingNgoVerifications;
    }

    public void setPendingNgoVerifications(Long pendingNgoVerifications) {
        this.pendingNgoVerifications = pendingNgoVerifications;
    }

    public Long getActiveEvents() {
        return activeEvents;
    }

    public void setActiveEvents(Long activeEvents) {
        this.activeEvents = activeEvents;
    }

    public Long getCompletedEvents() {
        return completedEvents;
    }

    public void setCompletedEvents(Long completedEvents) {
        this.completedEvents = completedEvents;
    }

    public BigDecimal getTotalVerifiedHours() {
        return totalVerifiedHours;
    }

    public void setTotalVerifiedHours(BigDecimal totalVerifiedHours) {
        this.totalVerifiedHours = totalVerifiedHours;
    }

    public Long getRecentRegistrationsCount() {
        return recentRegistrationsCount;
    }

    public void setRecentRegistrationsCount(Long recentRegistrationsCount) {
        this.recentRegistrationsCount = recentRegistrationsCount;
    }

    public List<CategoryHoursDto> getHoursByCategory() {
        return hoursByCategory;
    }

    public void setHoursByCategory(List<CategoryHoursDto> hoursByCategory) {
        this.hoursByCategory = hoursByCategory;
    }

    public List<ActivityDataPointDto> getActivity() {
        return activity;
    }

    public void setActivity(List<ActivityDataPointDto> activity) {
        this.activity = activity;
    }
}
