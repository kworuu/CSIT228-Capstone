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

    public InventoryItem() {}

    public InventoryItem(Long id, String name, String category, String unit,
                         int criticalThreshold, int lowThreshold,
                         LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.criticalThreshold = criticalThreshold;
        this.lowThreshold = lowThreshold;
        this.createdAt = createdAt;
    }

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

    /**
     * Returns the stock status for a given quantity against this item's thresholds.
     */
    public StockStatus getStatusFor(int quantity) {
        if (quantity <= criticalThreshold) return StockStatus.CRITICAL;
        if (quantity <= lowThreshold) return StockStatus.LOW;
        return StockStatus.OK;
    }

    @Override
    public String toString() { return name + " (" + unit + ")"; }
}