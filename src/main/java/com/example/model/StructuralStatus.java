package com.example.model;

/**
 * Building-integrity states a barangay can assign to an evacuation center.
 * Maps to the {@code evacuation_centers.structural_status} ENUM column
 * (introduced in V004 migration).
 *
 * <p>This is independent of {@code is_active} (whether the center accepts
 * evacuees) and {@code current_occupancy} (how full it is). A center can
 * be active and full while flagged as damaged — meaning "we're using it
 * but engineers should inspect it as soon as possible."</p>
 */
public enum StructuralStatus {

    /** Building integrity confirmed. No known damage. */
    SAFE("safe", "Safe"),

    /** Visible damage but center remains usable with caution. */
    DAMAGED("damaged", "Damaged"),

    /** Structurally compromised — should not accept evacuees. */
    UNSAFE("unsafe", "Unsafe");

    private final String dbValue;
    private final String displayLabel;

    StructuralStatus(String dbValue, String displayLabel) {
        this.dbValue = dbValue;
        this.displayLabel = displayLabel;
    }

    public String toDb()        { return dbValue; }
    public String displayLabel() { return displayLabel; }

    public static StructuralStatus fromDb(String dbValue) {
        if (dbValue == null) return SAFE;
        return switch (dbValue.toLowerCase()) {
            case "damaged" -> DAMAGED;
            case "unsafe"  -> UNSAFE;
            default        -> SAFE;
        };
    }

    /**
     * Returns the CSS class name used to style this status as a badge.
     * Matches the CSS rules added in Phase 5b.
     */
    public String cssClass() {
        return switch (this) {
            case SAFE    -> "brgy-struct-safe";
            case DAMAGED -> "brgy-struct-damaged";
            case UNSAFE  -> "brgy-struct-unsafe";
        };
    }
}