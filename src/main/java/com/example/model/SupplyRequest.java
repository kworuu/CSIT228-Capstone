package com.example.model;

import java.time.LocalDateTime;

public record SupplyRequest(
        long id,
        long itemId,
        String itemName,          // NEW: To display what item was requested
        Long targetCenterId,      // NEW: The actual DB column
        String targetCenterName,  // NEW: To display where it's going
        int quantity,
        SupplyRequestStatus status,
        String barangay,
        long userId,
        String notes,
        LocalDateTime createdAt
) {}