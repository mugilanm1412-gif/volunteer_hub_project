package com.volunteerhub.exception;

/**
 * Thrown when authentication credentials or token are invalid or unauthorized.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
