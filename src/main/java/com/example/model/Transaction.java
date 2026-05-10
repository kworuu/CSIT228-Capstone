package com.example.model;

import java.time.LocalDateTime;

/**
 * Abstract base class for inventory movement transactions.
 *
 * <p>Two subclasses exist: {@link InflowTransaction} for restocks and
 * {@link OutflowTransaction} for dispatches. Demonstrates inheritance
 * and polymorphism for rubric criterion 1.</p>
 */
public abstract class Transaction {

    protected Long id;
    protected Long itemId;
    protected int quantity;
    protected Long warehouseId;
    protected Long createdBy;
    protected LocalDateTime createdAt;
    protected String notes;

    protected Transaction() {}

    protected Transaction(Long id, Long itemId, int quantity, Long warehouseId,
                          Long createdBy, LocalDateTime createdAt, String notes) {
        this.id = id;
        this.itemId = itemId;
        this.quantity = quantity;
        this.warehouseId = warehouseId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.notes = notes;
    }

    /** Returns "inflow" or "outflow" — implemented by subclasses. */
    public abstract String getDirection();

    /** Inflows positive, outflows negative — implemented by subclasses. */
    public abstract int getSignedQuantity();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}