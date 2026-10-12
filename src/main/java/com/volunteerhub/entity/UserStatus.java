package com.volunteerhub.entity;

/**
 * Account lifecycle statuses.
 * Suspended or deactivated users are rejected during authentication.
 */
public enum UserStatus {
    ACTIVE,
    SUSPENDED,
    DEACTIVATED
}
