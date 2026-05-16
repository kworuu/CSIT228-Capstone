package com.example.model;

import java.time.LocalDateTime;

public record User(
        long id,
        String username,
        String passwordHash,
        String displayName,
        UserRole role,
        String assignedBarangay,
        Double latitude,
        Double longitude,
        LocalDateTime createdAt,
        LocalDateTime lastLogin
) {}