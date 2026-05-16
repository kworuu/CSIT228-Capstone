package com.example.model;

import java.time.LocalDateTime;

public record Transaction(
        long id,
        long itemId,
        int quantity,
        String direction,
        String destinationType,
        Long destinationId,
        Long userId,
        String notes,
        LocalDateTime createdAt
) {}