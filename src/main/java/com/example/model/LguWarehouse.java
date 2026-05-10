package com.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an LGU supply warehouse.
 * Maps to the {@code lgu_warehouses} table.
 */
public class LguWarehouse {

    private Long id;
    private String name;
    private String address;
    private String lguCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime createdAt;

    public LguWarehouse() {}

    public LguWarehouse(Long id, String name, String address, String lguCode,
                        BigDecimal latitude, BigDecimal longitude,
                        LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.lguCode = lguCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getLguCode() { return lguCode; }
    public void setLguCode(String lguCode) { this.lguCode = lguCode; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return name; }
}