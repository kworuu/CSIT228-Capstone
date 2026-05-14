package com.example.model;

/**
 * Roles a user can have in the CivicGuard system.
 * Maps to the {@code users.role} ENUM column.
 *
 * <p>V004 migration extended the DB enum to include {@code 'barangay'}.
 * {@code STAFF} is retained for backward compatibility with V001 data
 * and is treated as equivalent to {@code BARANGAY} in routing decisions.</p>
 */
public enum UserRole {
    ADMIN,
    BARANGAY,
    STAFF;  // legacy — kept so old rows still deserialize

    public static UserRole fromDb(String dbValue) {
        if (dbValue == null) return ADMIN;
        return switch (dbValue.toLowerCase()) {
            case "barangay" -> BARANGAY;
            case "staff"    -> STAFF;
            default         -> ADMIN;
        };
    }

    public String toDb() {
        return this.name().toLowerCase();
    }

    /**
     * Convenience for the router — both BARANGAY and legacy STAFF
     * route to the barangay dashboard.
     */
    public boolean isBarangayRole() {
        return this == BARANGAY || this == STAFF;
    }
}