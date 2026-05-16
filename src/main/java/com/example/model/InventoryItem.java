package com.example.model;

import java.time.LocalDateTime;

public record InventoryItem(
        long id,
        String name,
        String category,
        String unit,
        int criticalThreshold,  // Added
        int lowThreshold,       // Added
        int stockQuantity,
        LocalDateTime createdAt,
        Long createdByUserId
) {
    // Optional: Helper to determine status based on thresholds
    public StockStatus getStockStatus() {
        if (stockQuantity <= criticalThreshold) return StockStatus.CRITICAL;
        if (stockQuantity <= lowThreshold) return StockStatus.LOW;
        return StockStatus.GREAT;
    }

    public enum StockStatus { GREAT, LOW, CRITICAL }
}