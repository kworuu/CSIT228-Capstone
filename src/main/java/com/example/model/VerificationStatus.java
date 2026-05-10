package com.example.model;

/**
 * Verification states an evacuee registration can be in.
 * Maps to the {@code evacuees.verification_status} ENUM column.
 */
public enum VerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED;

    public static VerificationStatus fromDb(String dbValue) {
        if (dbValue == null) return PENDING;
        return switch (dbValue.toLowerCase()) {
            case "verified" -> VERIFIED;
            case "rejected" -> REJECTED;
            default -> PENDING;
        };
    }

    public String toDb() {
        return this.name().toLowerCase();
    }
}