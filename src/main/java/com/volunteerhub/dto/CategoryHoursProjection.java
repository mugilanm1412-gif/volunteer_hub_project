package com.volunteerhub.dto;

import java.math.BigDecimal;

/**
 * Spring Data JPA projection for hours by category aggregations.
 */
public interface CategoryHoursProjection {
    String getCategory();
    BigDecimal getTotalHours();
    Long getEventCount();
    Long getVolunteerCount();
}
