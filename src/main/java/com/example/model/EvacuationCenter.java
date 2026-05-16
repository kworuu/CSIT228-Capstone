package com.example.model;

import java.time.LocalDateTime;

public record EvacuationCenter(
        long id,
        String name,
        String address,
        String barangay,
        String photoPath,
        int capacity,
        int currentOccupancy,
        Double latitude,
        Double longitude,
        StructuralStatus structuralStatus,
        String structuralNotes,
        LocalDateTime structuralUpdatedAt,
        Long userId,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}