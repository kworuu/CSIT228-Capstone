package com.example.dashboard_kiosk.model;


public record EvacuationSite(
        String id,
        String title,
        String address,
        String barangay,
        String status,
        String createdAt,
        double latitude,
        double longitude
) {
}