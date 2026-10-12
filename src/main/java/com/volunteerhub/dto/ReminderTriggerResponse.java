package com.volunteerhub.dto;

import java.time.LocalDate;

/**
 * Response payload when event reminders are triggered administratively or via scheduler.
 */
public class ReminderTriggerResponse {

    private int sentCount;
    private LocalDate targetDate;
    private String message;

    public ReminderTriggerResponse() {
    }

    public ReminderTriggerResponse(int sentCount, LocalDate targetDate, String message) {
        this.sentCount = sentCount;
        this.targetDate = targetDate;
        this.message = message;
    }

    public int getSentCount() {
        return sentCount;
    }

    public void setSentCount(int sentCount) {
        this.sentCount = sentCount;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
