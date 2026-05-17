package com.example.model;

import java.time.LocalDateTime;

public record InventoryItem(
        long id,
        String name,
        String category,
        String unit,
        int lowThreshold,
        int criticalThreshold,
        int stockQuantity,
        Long createdByUserId
) {
    public int totalQuantity() {
        return stockQuantity;

    }

    public int minimumThreshold() {
        return lowThreshold;
    }

    public enum StockStatus {
        OK, LOW_STOCK, CRITICAL, OUT_OF_STOCK
    }

    public StockStatus getStockStatus() {
        if (stockQuantity <= 0) return StockStatus.OUT_OF_STOCK;
        if (stockQuantity <= criticalThreshold) return StockStatus.CRITICAL;
        if (stockQuantity <= lowThreshold) return StockStatus.LOW_STOCK;
        return StockStatus.OK;
    }
}