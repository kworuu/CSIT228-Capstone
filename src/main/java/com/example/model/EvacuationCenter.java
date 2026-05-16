package com.example.model;

import java.time.LocalDateTime;

public record EvacuationCenter(
        long id,
        String name,
        String address,
        String barangay,
        int capacity,
        int currentOccupancy,
        Double latitude,
        Double longitude,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}