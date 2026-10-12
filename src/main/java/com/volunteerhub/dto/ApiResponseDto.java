package com.volunteerhub.dto;

/**
 * Standard simple response payload returning an operational message.
 */
public class ApiResponseDto {

    private String message;

    public ApiResponseDto() {
    }

    public ApiResponseDto(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
