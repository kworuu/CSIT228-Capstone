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

    public String toDb() { return dbValue; }
    public String displayLabel() { return displayLabel; }

    public static SupplyRequestStatus fromDb(String dbValue) {
        if (dbValue == null) return PENDING;
        for (SupplyRequestStatus status : values()) {
            if (status.dbValue.equalsIgnoreCase(dbValue)) {
                return status;
            }
        }
        return PENDING;
    }
}