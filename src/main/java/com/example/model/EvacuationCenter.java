package com.example.model;

import java.time.LocalDateTime;

public record EvacuationCenter(
        long id,
        String name,
        String address,
        Long userId,
        String photoPath,
        Double latitude,
        Double longitude,
        LocalDateTime createdAt
) {}