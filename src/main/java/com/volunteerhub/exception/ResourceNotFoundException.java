package com.volunteerhub.exception;

/**
 * Thrown when an entity (User, Profile, Event, etc.) cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
