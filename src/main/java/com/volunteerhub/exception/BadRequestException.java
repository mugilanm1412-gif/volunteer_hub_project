package com.volunteerhub.exception;

/**
 * Thrown when business rules or parameters are invalid.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
