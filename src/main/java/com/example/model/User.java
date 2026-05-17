package com.example.model;

import java.time.LocalDateTime;

public record User(
        long id,
        String username,
        String passwordHash,
        String displayName,
        UserRole role,
        Double latitude,
        Double longitude,
        Integer zoom,
        LocalDateTime lastLoginAt
) {
    public String assignedBarangay() {
        return displayName;
    }
}