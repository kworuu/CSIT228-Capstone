package com.example.model;

import java.time.LocalDateTime;

public record Transaction(
        long id,
        String direction,        // Encapsulates the ENUM('outflow') from schema
        long itemId,             // FK linking to inventory_items
        int quantity,
        Long destinationId,      // FK linking to users (can be null)
        String createdBy,          // FK linking to users (who authorized it)
        LocalDateTime createdAt,
        String notes
) {
    /**
     * Compact constructor to catch bad inputs before hitting the database
     */
    public Transaction {
        if (direction == null || !direction.equalsIgnoreCase("outflow")) {
            throw new IllegalArgumentException("Database constraints limit transaction direction to 'outflow'.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
    }
}