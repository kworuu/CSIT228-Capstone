package com.example.model;

import java.time.LocalDateTime;

/**
 * Represents the quantity of one inventory item at one warehouse.
 * Maps to the {@code warehouse_stock} table.
 *
 * <p>Uses a composite primary key — there's no auto-generated ID.
 * The combination of {@code warehouseId} and {@code itemId} is the identity.</p>
 */
public class WarehouseStock {

    private Long warehouseId;
    private Long itemId;
    private int quantity;
    private LocalDateTime lastUpdated;

    public WarehouseStock() {}

    public WarehouseStock(Long warehouseId, Long itemId, int quantity, LocalDateTime lastUpdated) {
        this.warehouseId = warehouseId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.lastUpdated = lastUpdated;
    }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}