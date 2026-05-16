package com.example.model;

import java.time.LocalDateTime;

public record InventoryItem(
        long id,
        String name,
        String category,
        String unit,
        int minimumThreshold,
        int totalQuantity,
        LocalDateTime lastUpdated,
        Long createdByUserId
) {
    public enum StockStatus {
        OK, LOW_STOCK, OUT_OF_STOCK
    }

    public StockStatus getStockStatus() {
        if (totalQuantity <= 0) return StockStatus.OUT_OF_STOCK;
        if (totalQuantity <= minimumThreshold) return StockStatus.LOW_STOCK;
        return StockStatus.OK;
    }
}