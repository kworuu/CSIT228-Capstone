package com.example.model;

/**
 * Roles a user can have in the CivicGuard system.
 * Maps to the {@code users.role} ENUM column.
 */
public enum UserRole {
    ADMIN,
    STAFF;

    public static UserRole fromDb(String dbValue) {
        if (dbValue == null) return ADMIN;
        return switch (dbValue.toLowerCase()) {
            case "staff" -> STAFF;
            default -> ADMIN;
        };
    }

    public String toDb() {
        return this.name().toLowerCase();
    }
}