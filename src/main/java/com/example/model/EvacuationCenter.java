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
    private String photoPath;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Long managedBy;
    private boolean active;
    private LocalDateTime createdAt;

    public EvacuationCenter() {}

    public EvacuationCenter(Long id, String name, String address, String barangay, String photoPath,
                            BigDecimal latitude, BigDecimal longitude,
                            Long managedBy, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.barangay = barangay;
        this.photoPath = photoPath;
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
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
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


    @Override
    public String toString() {
        return name + " (" + barangay + ")";
    }

}