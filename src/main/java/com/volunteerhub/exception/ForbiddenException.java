package com.volunteerhub.exception;

/**
 * Thrown when an authenticated user attempts to access or modify a resource they do not own.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
