package com.example.model;

import java.time.LocalDateTime;

/**
 * A transaction representing supplies dispatched from warehouse to
 * an evacuation center.
 */
public class OutflowTransaction extends Transaction {

    private Long destinationCenterId;

    public OutflowTransaction() {}

    public OutflowTransaction(Long id, Long itemId, int quantity, Long warehouseId,
                              Long destinationCenterId, Long createdBy,
                              LocalDateTime createdAt, String notes) {
        super(id, itemId, quantity, warehouseId, createdBy, createdAt, notes);
        this.destinationCenterId = destinationCenterId;
    }

    @Override
    public String getDirection() { return "outflow"; }

    @Override
    public int getSignedQuantity() { return -quantity; }

    public Long getDestinationCenterId() { return destinationCenterId; }
    public void setDestinationCenterId(Long destinationCenterId) { this.destinationCenterId = destinationCenterId; }
}