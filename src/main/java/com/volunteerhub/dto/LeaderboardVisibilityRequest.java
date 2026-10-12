package com.volunteerhub.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for toggling public leaderboard visibility.
 */
public class LeaderboardVisibilityRequest {

    @NotNull(message = "showOnLeaderboard flag is required")
    private Boolean showOnLeaderboard;

    public LeaderboardVisibilityRequest() {
    }

    public LeaderboardVisibilityRequest(Boolean showOnLeaderboard) {
        this.showOnLeaderboard = showOnLeaderboard;
    }

    public Boolean getShowOnLeaderboard() {
        return showOnLeaderboard;
    }

    public void setShowOnLeaderboard(Boolean showOnLeaderboard) {
        this.showOnLeaderboard = showOnLeaderboard;
    }
}
