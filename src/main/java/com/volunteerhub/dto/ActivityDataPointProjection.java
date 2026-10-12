package com.volunteerhub.dto;

import java.math.BigDecimal;

/**
 * Spring Data JPA projection for periodic activity aggregations.
 */
public interface ActivityDataPointProjection {
    String getPeriod();
    String getLabel();
    BigDecimal getTotalHours();
    Long getEventCount();
    Long getVolunteerCount();
}
