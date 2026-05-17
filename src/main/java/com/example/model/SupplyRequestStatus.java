package com.example.model;

public enum SupplyRequestStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    PARTIALLY_FULFILLED("Partially Fulfilled"),
    FULFILLED("Fulfilled"),
    REJECTED("Rejected");

    private final String displayLabel;

    SupplyRequestStatus(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String displayLabel() {
        return displayLabel;
    }

    public static SupplyRequestStatus fromDb(String dbValue) {
        if (dbValue == null) return PENDING;
        return switch (dbValue.toLowerCase()) {
            case "approved" -> APPROVED;
            case "partially_fulfilled" -> PARTIALLY_FULFILLED;
            case "fulfilled" -> FULFILLED;
            case "rejected" -> REJECTED;
            default -> PENDING;
        };
    }
}