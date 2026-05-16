package com.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "center_status_updates")
public class CenterStatusUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false)
    private EvacuationCenter center;

    @Column(name = "event_label", nullable = false, length = 255)
    private String eventLabel = "No active event";

    @Column(name = "available_item_ids", columnDefinition = "LONGTEXT")
    private String availableItemIds; // JSON Array containing available item IDs

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "updated_at", nullable = false, updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    // Constructors
    public CenterStatusUpdate() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EvacuationCenter getCenter() { return center; }
    public void setCenter(EvacuationCenter center) { this.center = center; }

    public String getEventLabel() { return eventLabel; }
    public void setEventLabel(String eventLabel) { this.eventLabel = eventLabel; }

    public String getAvailableItemIds() { return availableItemIds; }
    public void setAvailableItemIds(String availableItemIds) { this.availableItemIds = availableItemIds; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(User updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}