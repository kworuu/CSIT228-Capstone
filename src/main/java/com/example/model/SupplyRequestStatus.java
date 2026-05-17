package com.example.model;

public enum SupplyRequestStatus {
    PENDING("pending", "Pending"),
    APPROVED("approved", "Approved"),
    PARTIALLY_FULFILLED("partially_fulfilled", "Partially Fulfilled"),
    FULFILLED("fulfilled", "Fulfilled"),
    REJECTED("rejected", "Rejected");

    private final String dbValue;
    private final String displayLabel;

    SupplyRequestStatus(String dbValue, String displayLabel) {
        this.dbValue = dbValue;
        this.displayLabel = displayLabel;
    }

    public String dbValue() {
        return dbValue;
    }

    public String displayLabel() {
        return displayLabel;
    }


    /**
     * Safely converts lowercase strings from the database into the correct Enum constant.
     * Drop this static method inside your SupplyRequestStatus enum class.
     */
    public static SupplyRequestStatus fromString(String statusStr) {
        if (statusStr == null) return PENDING;
        try {
            return SupplyRequestStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Warning: Unknown database status string received: " + statusStr);
            return PENDING; // Safe default fallback
        }
    }

}