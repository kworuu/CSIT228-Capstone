package com.example.model;

import java.time.LocalDateTime;

public record SupplyRequest(
        long id,
        long itemId,
        int quantity,
        SupplyRequestStatus status,
        String requestingBarangay, // Populated dynamically via JOIN
        Long requestingUserId,
        String notes,
        LocalDateTime createdAt
) {}