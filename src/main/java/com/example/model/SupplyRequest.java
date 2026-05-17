package com.example.model;

import java.time.LocalDateTime;

public record SupplyRequest(
        long id,
        long itemId,
        String itemName,
        Long targetCenterId,
        String targetCenterName,
        int quantity,
        SupplyRequestStatus status,
        String barangay,
        long userId,
        String notes,
        LocalDateTime createdAt
) {
}
