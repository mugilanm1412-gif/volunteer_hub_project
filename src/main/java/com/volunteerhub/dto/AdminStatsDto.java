package com.volunteerhub.dto;

import java.math.BigDecimal;

/**
 * Platform aggregate statistics for administrative dashboard metrics.
 */
public class AdminStatsDto {

    private long totalUsers;
    private long totalVolunteers;
    private long totalNgos;
    private long totalAdmins;
    private long activeUsers;
    private long suspendedUsers;
    private long deactivatedUsers;

    private long totalEvents;
    private long publishedEvents;
    private long completedEvents;
    private long removedEvents;
    private long draftEvents;

    private long totalRegistrations;
    private BigDecimal totalVerifiedHours;
    private long totalCertificatesIssued;
    private long pendingNgoVerifications;

    public AdminStatsDto() {
    }

    public AdminStatsDto(long totalUsers, long totalVolunteers, long totalNgos, long totalAdmins,
                         long activeUsers, long suspendedUsers, long deactivatedUsers,
                         long totalEvents, long publishedEvents, long completedEvents,
                         long removedEvents, long draftEvents, long totalRegistrations,
                         BigDecimal totalVerifiedHours, long totalCertificatesIssued,
                         long pendingNgoVerifications) {
        this.totalUsers = totalUsers;
        this.totalVolunteers = totalVolunteers;
        this.totalNgos = totalNgos;
        this.totalAdmins = totalAdmins;
        this.activeUsers = activeUsers;
        this.suspendedUsers = suspendedUsers;
        this.deactivatedUsers = deactivatedUsers;
        this.totalEvents = totalEvents;
        this.publishedEvents = publishedEvents;
        this.completedEvents = completedEvents;
        this.removedEvents = removedEvents;
        this.draftEvents = draftEvents;
        this.totalRegistrations = totalRegistrations;
        this.totalVerifiedHours = totalVerifiedHours;
        this.totalCertificatesIssued = totalCertificatesIssued;
        this.pendingNgoVerifications = pendingNgoVerifications;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalVolunteers() {
        return totalVolunteers;
    }

    public void setTotalVolunteers(long totalVolunteers) {
        this.totalVolunteers = totalVolunteers;
    }

    public long getTotalNgos() {
        return totalNgos;
    }

    public void setTotalNgos(long totalNgos) {
        this.totalNgos = totalNgos;
    }

    public long getTotalAdmins() {
        return totalAdmins;
    }

    public void setTotalAdmins(long totalAdmins) {
        this.totalAdmins = totalAdmins;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getSuspendedUsers() {
        return suspendedUsers;
    }

    public void setSuspendedUsers(long suspendedUsers) {
        this.suspendedUsers = suspendedUsers;
    }

    public long getDeactivatedUsers() {
        return deactivatedUsers;
    }

    public void setDeactivatedUsers(long deactivatedUsers) {
        this.deactivatedUsers = deactivatedUsers;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public long getPublishedEvents() {
        return publishedEvents;
    }

    public void setPublishedEvents(long publishedEvents) {
        this.publishedEvents = publishedEvents;
    }

    public long getCompletedEvents() {
        return completedEvents;
    }

    public void setCompletedEvents(long completedEvents) {
        this.completedEvents = completedEvents;
    }

    public long getRemovedEvents() {
        return removedEvents;
    }

    public void setRemovedEvents(long removedEvents) {
        this.removedEvents = removedEvents;
    }

    public long getDraftEvents() {
        return draftEvents;
    }

    public void setDraftEvents(long draftEvents) {
        this.draftEvents = draftEvents;
    }

    public long getTotalRegistrations() {
        return totalRegistrations;
    }

    public void setTotalRegistrations(long totalRegistrations) {
        this.totalRegistrations = totalRegistrations;
    }

    public BigDecimal getTotalVerifiedHours() {
        return totalVerifiedHours;
    }

    public void setTotalVerifiedHours(BigDecimal totalVerifiedHours) {
        this.totalVerifiedHours = totalVerifiedHours;
    }

    public long getTotalCertificatesIssued() {
        return totalCertificatesIssued;
    }

    public void setTotalCertificatesIssued(long totalCertificatesIssued) {
        this.totalCertificatesIssued = totalCertificatesIssued;
    }

    public long getPendingNgoVerifications() {
        return pendingNgoVerifications;
    }

    public void setPendingNgoVerifications(long pendingNgoVerifications) {
        this.pendingNgoVerifications = pendingNgoVerifications;
    }
}
