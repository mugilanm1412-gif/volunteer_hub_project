package com.volunteerhub.dto;

import java.math.BigDecimal;

/**
 * Publicly visible leaderboard entry ensuring privacy preservation.
 * Strictly exposes only display name, badge, hours, and ranking score.
 */
public class LeaderboardEntryDto {

    private int rank;
    private String displayName;
    private double score;
    private String badge;
    private BigDecimal verifiedHours;

    public LeaderboardEntryDto() {
    }

    public LeaderboardEntryDto(int rank, String displayName, double score, String badge, BigDecimal verifiedHours) {
        this.rank = rank;
        this.displayName = displayName;
        this.score = score;
        this.badge = badge;
        this.verifiedHours = verifiedHours;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public BigDecimal getVerifiedHours() {
        return verifiedHours;
    }

    public void setVerifiedHours(BigDecimal verifiedHours) {
        this.verifiedHours = verifiedHours;
    }
}
