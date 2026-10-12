package com.volunteerhub.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Consolidated dashboard metrics and activity analytics for the authenticated NGO.
 */
public class NgoDashboardDto {

    private String ngoName;
    private String verificationStatus;
    private Long eventsCreated;
    private Long upcomingEventsCount;
    private Long completedEventsCount;
    private Long totalApplicants;
    private Long approvedVolunteersCount;
    private BigDecimal totalVerifiedHours;
    private List<CategoryHoursDto> hoursByCategory = new ArrayList<>();
    private List<ActivityDataPointDto> activity = new ArrayList<>();

    public NgoDashboardDto() {
    }

    public NgoDashboardDto(String ngoName, String verificationStatus, Long eventsCreated,
                           Long upcomingEventsCount, Long completedEventsCount, Long totalApplicants,
                           Long approvedVolunteersCount, BigDecimal totalVerifiedHours,
                           List<CategoryHoursDto> hoursByCategory, List<ActivityDataPointDto> activity) {
        this.ngoName = ngoName;
        this.verificationStatus = verificationStatus;
        this.eventsCreated = eventsCreated != null ? eventsCreated : 0L;
        this.upcomingEventsCount = upcomingEventsCount != null ? upcomingEventsCount : 0L;
        this.completedEventsCount = completedEventsCount != null ? completedEventsCount : 0L;
        this.totalApplicants = totalApplicants != null ? totalApplicants : 0L;
        this.approvedVolunteersCount = approvedVolunteersCount != null ? approvedVolunteersCount : 0L;
        this.totalVerifiedHours = totalVerifiedHours != null ? totalVerifiedHours : BigDecimal.ZERO;
        this.hoursByCategory = hoursByCategory != null ? hoursByCategory : new ArrayList<>();
        this.activity = activity != null ? activity : new ArrayList<>();
    }

    public String getNgoName() {
        return ngoName;
    }

    public void setNgoName(String ngoName) {
        this.ngoName = ngoName;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Long getEventsCreated() {
        return eventsCreated;
    }

    public void setEventsCreated(Long eventsCreated) {
        this.eventsCreated = eventsCreated;
    }

    public Long getUpcomingEventsCount() {
        return upcomingEventsCount;
    }

    public void setUpcomingEventsCount(Long upcomingEventsCount) {
        this.upcomingEventsCount = upcomingEventsCount;
    }

    public Long getCompletedEventsCount() {
        return completedEventsCount;
    }

    public void setCompletedEventsCount(Long completedEventsCount) {
        this.completedEventsCount = completedEventsCount;
    }

    public Long getTotalApplicants() {
        return totalApplicants;
    }

    public void setTotalApplicants(Long totalApplicants) {
        this.totalApplicants = totalApplicants;
    }

    public Long getApprovedVolunteersCount() {
        return approvedVolunteersCount;
    }

    public void setApprovedVolunteersCount(Long approvedVolunteersCount) {
        this.approvedVolunteersCount = approvedVolunteersCount;
    }

    public BigDecimal getTotalVerifiedHours() {
        return totalVerifiedHours;
    }

    public void setTotalVerifiedHours(BigDecimal totalVerifiedHours) {
        this.totalVerifiedHours = totalVerifiedHours;
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
