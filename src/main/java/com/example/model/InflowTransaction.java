package com.example.model;

import java.time.LocalDateTime;

/**
 * A transaction representing supplies arriving at the warehouse.
 * Has a {@code sourceLabel} describing where the stock came from.
 */
public class InflowTransaction extends Transaction {

    private String sourceLabel;

    public InflowTransaction() {}

    public InflowTransaction(Long id, Long itemId, int quantity, Long warehouseId,
                             String sourceLabel, Long createdBy,
                             LocalDateTime createdAt, String notes) {
        super(id, itemId, quantity, warehouseId, createdBy, createdAt, notes);
        this.sourceLabel = sourceLabel;
    }

    @Override
    public String getDirection() { return "inflow"; }

    @Override
    public int getSignedQuantity() { return quantity; }

    public String getSourceLabel() { return sourceLabel; }
    public void setSourceLabel(String sourceLabel) { this.sourceLabel = sourceLabel; }
}