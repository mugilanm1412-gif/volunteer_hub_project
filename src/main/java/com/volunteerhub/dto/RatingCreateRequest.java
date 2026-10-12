package com.volunteerhub.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for submitting volunteer shift performance evaluation.
 */
public class RatingCreateRequest {

    @NotNull(message = "Punctuality score is required")
    @Min(value = 1, message = "Punctuality score must be between 1 and 5")
    @Max(value = 5, message = "Punctuality score must be between 1 and 5")
    private Integer punctuality;

    @NotNull(message = "Teamwork score is required")
    @Min(value = 1, message = "Teamwork score must be between 1 and 5")
    @Max(value = 5, message = "Teamwork score must be between 1 and 5")
    private Integer teamwork;

    @NotNull(message = "Effort score is required")
    @Min(value = 1, message = "Effort score must be between 1 and 5")
    @Max(value = 5, message = "Effort score must be between 1 and 5")
    private Integer effort;

    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    private String comment;

    public RatingCreateRequest() {
    }

    public RatingCreateRequest(Integer punctuality, Integer teamwork, Integer effort, String comment) {
        this.punctuality = punctuality;
        this.teamwork = teamwork;
        this.effort = effort;
        this.comment = comment;
    }

    public Integer getPunctuality() {
        return punctuality;
    }

    public void setPunctuality(Integer punctuality) {
        this.punctuality = punctuality;
    }

    public Integer getTeamwork() {
        return teamwork;
    }

    public void setTeamwork(Integer teamwork) {
        this.teamwork = teamwork;
    }

    public Integer getEffort() {
        return effort;
    }

    public void setEffort(Integer effort) {
        this.effort = effort;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
