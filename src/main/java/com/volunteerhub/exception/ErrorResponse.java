package com.volunteerhub.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized error payload returned across all /api/** endpoints.
 */
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, String> fieldErrors;

    public ErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ErrorResponse(int status, String error, String message, Map<String, String> fieldErrors) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
}
