package com.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a physical evacuation center where evacuees are sheltered.
 * Maps to the {@code evacuation_centers} table.
 */
public class EvacuationCenter {

    private Long id;
    private String name;
    private String address;
    private String barangay;
    private int capacity;
    private int currentOccupancy;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Long managedBy;
    private boolean active;
    private LocalDateTime createdAt;

    public EvacuationCenter() {}

    public EvacuationCenter(Long id, String name, String address, String barangay,
                            int capacity, int currentOccupancy,
                            BigDecimal latitude, BigDecimal longitude,
                            Long managedBy, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.barangay = barangay;
        this.capacity = capacity;
        this.currentOccupancy = currentOccupancy;
        this.latitude = latitude;
        this.longitude = longitude;
        this.managedBy = managedBy;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBarangay() { return barangay; }
    public void setBarangay(String barangay) { this.barangay = barangay; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public int getCurrentOccupancy() { return currentOccupancy; }
    public void setCurrentOccupancy(int currentOccupancy) { this.currentOccupancy = currentOccupancy; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public Long getManagedBy() { return managedBy; }
    public void setManagedBy(Long managedBy) { this.managedBy = managedBy; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Returns occupancy as a fraction of capacity (0.0 to 1.0+). */
    public double getOccupancyRatio() {
        return capacity == 0 ? 0.0 : (double) currentOccupancy / capacity;
    }

    public boolean isAtCapacity() {
        return currentOccupancy >= capacity;
    }

    @Override
    public String toString() {
        return name + " (" + barangay + ")";
    }
}