package com.volunteerhub.dto;

import java.math.BigDecimal;

/**
 * Interface projection for aggregating leaderboard metrics at database query time.
 * Prevents loading JPA entities into JVM memory.
 */
public interface LeaderboardRowProjection {

    Long getUserId();

    String getDisplayName();

    BigDecimal getVerifiedHours();

    int getAttendedEvents();

    int getRegisteredEvents();

    Double getAvgRating();

    Double getScore();
}
