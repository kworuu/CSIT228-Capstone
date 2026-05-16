package com.example.model;

public enum SupplyRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    FULFILLED;

    public static SupplyRequestStatus fromDb(String status) {
        if (status == null) return PENDING;
        try {
            return valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }

    public String displayLabel() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}