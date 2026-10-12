package com.volunteerhub.dto;

import java.math.BigDecimal;

/**
 * Aggregate metrics data point across a specific time period (monthly or weekly).
 */
public class ActivityDataPointDto {

    private String period;
    private String label;
    private BigDecimal totalHours;
    private Long eventCount;
    private Long volunteerCount;

    public ActivityDataPointDto() {
    }

    public ActivityDataPointDto(String period, String label, BigDecimal totalHours, Long eventCount, Long volunteerCount) {
        this.period = period;
        this.label = label;
        this.totalHours = totalHours != null ? totalHours : BigDecimal.ZERO;
        this.eventCount = eventCount != null ? eventCount : 0L;
        this.volunteerCount = volunteerCount != null ? volunteerCount : 0L;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(BigDecimal totalHours) {
        this.totalHours = totalHours;
    }

    public Long getEventCount() {
        return eventCount;
    }

    public void setEventCount(Long eventCount) {
        this.eventCount = eventCount;
    }

    public Long getVolunteerCount() {
        return volunteerCount;
    }

    public void setVolunteerCount(Long volunteerCount) {
        this.volunteerCount = volunteerCount;
    }
}
