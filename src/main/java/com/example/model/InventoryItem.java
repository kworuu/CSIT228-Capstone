package com.example.model;

import java.time.LocalDateTime;

/**
 * Represents a type of supply item tracked in inventory.
 * Maps to the {@code inventory_items} table.
 */
public class InventoryItem {

    /** Status levels for an item's current stock. */
    public enum StockStatus {
        OK, LOW, CRITICAL
    }

    private Long id;
    private String name;
    private String category;
    private String unit;
    private int criticalThreshold;
    private int lowThreshold;
    private LocalDateTime createdAt;

    // Integrated Stock Column
    private int stockQuantity;

    // Tracks which user created/owns this record
    private Long createdByUserId;

    // Default Constructor
    public InventoryItem() {}

    // Full Constructor for DAO mapping
    public InventoryItem(Long id, String name, String category,
                         String unit, int criticalThreshold,
                         int lowThreshold, LocalDateTime createdAt,
                         int stockQuantity, Long createdByUserId) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.criticalThreshold = criticalThreshold;
        this.lowThreshold = lowThreshold;
        this.createdAt = createdAt;
        this.stockQuantity = stockQuantity;
        this.createdByUserId = createdByUserId;
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getCriticalThreshold() { return criticalThreshold; }
    public void setCriticalThreshold(int criticalThreshold) { this.criticalThreshold = criticalThreshold; }

    public int getLowThreshold() { return lowThreshold; }
    public void setLowThreshold(int lowThreshold) { this.lowThreshold = lowThreshold; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }

    /**
     * Business Logic: Returns the status based on internal thresholds.
     * Useful for UI coloring (e.g., Red for Critical).
     */
    public StockStatus getStatus() {
        if (stockQuantity <= criticalThreshold) return StockStatus.CRITICAL;
        if (stockQuantity <= lowThreshold) return StockStatus.LOW;
        return StockStatus.OK;
    }

    @Override
    public String toString() {
        return String.format("%s [%s] - Stock: %d %s", name, category, stockQuantity, unit);
    }
}