package com.volunteerhub.dto;

import java.math.BigDecimal;

/**
 * Data point representing hours accumulated within a given time period (e.g., month or week).
 */
public class ProgressPointDto {

    private String period;
    private BigDecimal hours;

    public ProgressPointDto() {
    }

    public ProgressPointDto(String period, BigDecimal hours) {
        this.period = period;
        this.hours = hours;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }
}
