package com.volunteerhub.dto;

import java.math.BigDecimal;

/**
 * Aggregate summary of volunteering hours and metrics grouped by cause category.
 */
public class CategoryHoursDto {

    private String category;
    private BigDecimal totalHours;
    private Long eventCount;
    private Long volunteerCount;

    public CategoryHoursDto() {
    }

    public CategoryHoursDto(String category, BigDecimal totalHours, Long eventCount, Long volunteerCount) {
        this.category = category;
        this.totalHours = totalHours != null ? totalHours : BigDecimal.ZERO;
        this.eventCount = eventCount != null ? eventCount : 0L;
        this.volunteerCount = volunteerCount != null ? volunteerCount : 0L;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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
