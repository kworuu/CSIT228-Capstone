package com.example.model;

public class SupplyRequestItem {
    private Long id;
    private Long requestId;
    private Long itemId;
    private int quantityRequested;
    private int quantityApproved;

    // Transient field for UI display purposes (joined from inventory_items)
    private String itemName;
    private String unit;

    public SupplyRequestItem(Long itemId, int quantityRequested, String itemName, String unit) {
        this.itemId = itemId;
        this.quantityRequested = quantityRequested;
        this.itemName = itemName;
        this.unit = unit;
        this.quantityApproved = 0;
    }

    public SupplyRequestItem(Long id, Long requestId, Long itemId, int quantityRequested, int quantityApproved, String itemName, String unit) {
        this.id = id;
        this.requestId = requestId;
        this.itemId = itemId;
        this.quantityRequested = quantityRequested;
        this.quantityApproved = quantityApproved;
        this.itemName = itemName;
        this.unit = unit;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Long getItemId() { return itemId; }
    public int getQuantityRequested() { return quantityRequested; }
    public int getQuantityApproved() { return quantityApproved; }
    public String getItemName() { return itemName; }
    public String getUnit() { return unit; }
}