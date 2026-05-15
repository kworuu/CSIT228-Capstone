package com.example.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SupplyRequest {
    private Long id;
    private String requestingBarangay;
    private Long requestingUserId;
    private Long evacuationCenterId;
    private SupplyRequestStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String adminNotes;

    private List<SupplyRequestItem> items = new ArrayList<>();

    // Constructor for creating a new request
    public SupplyRequest(String requestingBarangay, Long requestingUserId, Long evacuationCenterId, String notes) {
        this.requestingBarangay = requestingBarangay;
        this.requestingUserId = requestingUserId;
        this.evacuationCenterId = evacuationCenterId;
        this.notes = notes;
        this.status = SupplyRequestStatus.PENDING;
    }

    // Constructor for loading from DB
    public SupplyRequest(Long id, String requestingBarangay, Long requestingUserId, Long evacuationCenterId,
                         SupplyRequestStatus status, String notes, LocalDateTime createdAt,
                         Long reviewedBy, LocalDateTime reviewedAt, String adminNotes) {
        this.id = id;
        this.requestingBarangay = requestingBarangay;
        this.requestingUserId = requestingUserId;
        this.evacuationCenterId = evacuationCenterId;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.adminNotes = adminNotes;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestingBarangay() { return requestingBarangay; }
    public Long getRequestingUserId() { return requestingUserId; }
    public Long getEvacuationCenterId() { return evacuationCenterId; }
    public SupplyRequestStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<SupplyRequestItem> getItems() { return items; }
    public void setItems(List<SupplyRequestItem> items) { this.items = items; }
    public void addItem(SupplyRequestItem item) { this.items.add(item); }
}