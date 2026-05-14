package com.example.model;

/**
 * Represents one row of the {@code barangays} table (V002 migration).
 *
 * <p>Holds the geographic identity of a barangay — its map center and
 * default zoom — used to bound the Barangay dashboard's map view.</p>
 *
 * <p>This class is a plain POJO. It does <i>not</i> hold credentials;
 * authentication lives on the {@code users} table, linked via
 * {@code users.assigned_barangay = barangays.name}.</p>
 */
public class Barangay {

    private Long id;
    private String name;
    private double centerLat;
    private double centerLng;
    private int defaultZoom;

    public Barangay() {}

    public Barangay(Long id, String name,
                    double centerLat, double centerLng, int defaultZoom) {
        this.id = id;
        this.name = name;
        this.centerLat = centerLat;
        this.centerLng = centerLng;
        this.defaultZoom = defaultZoom;
    }

    public Long getId()                  { return id; }
    public void setId(Long id)           { this.id = id; }
    public String getName()              { return name; }
    public void setName(String name)     { this.name = name; }
    public double getCenterLat()         { return centerLat; }
    public void setCenterLat(double v)   { this.centerLat = v; }
    public double getCenterLng()         { return centerLng; }
    public void setCenterLng(double v)   { this.centerLng = v; }
    public int getDefaultZoom()          { return defaultZoom; }
    public void setDefaultZoom(int z)    { this.defaultZoom = z; }

    @Override
    public String toString() {
        return "Brgy. " + name + " (" + centerLat + ", " + centerLng + " @ z" + defaultZoom + ")";
    }
}