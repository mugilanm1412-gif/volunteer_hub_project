package com.volunteerhub.exception;

/**
 * Thrown when an action conflicts with existing data, such as duplicate email registration.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
