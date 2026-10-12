package com.volunteerhub.entity;

/**
 * Verification state for NGO accounts.
 * NGOs must be VERIFIED by an admin before they are authorized to publish events.
 */
public enum NgoVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
