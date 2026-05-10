package com.example.dashboard_kiosk.user;

/**
 * Lightweight, read-only representation of an evacuation center used by the
 * kiosk dashboard.
 *
 * <p>This intentionally duplicates none of the DB-backed
 * {@link com.example.model.EvacuationCenter} entity. The kiosk view is
 * read-only and public-facing; keeping its model separate means changes to
 * the persistence layer never accidentally affect what the public screen
 * renders, and vice-versa.</p>
 *
 * <p>All fields are set once at construction and are immutable — the kiosk
 * does not allow editing.</p>
 */
public final class SimpleCenter {

    // ── Fields ─────────────────────────────────────────────────────────────

    private final String id;
    private final String title;
    private final String address;
    private final String status;   // "OPEN" | "FULL"
    private final double lat;
    private final double lng;
    private final int    capacity;
    private final int    occupancy;

    // ── Constructor ────────────────────────────────────────────────────────

    /**
     * Creates a fully-populated, immutable kiosk center record.
     *
     * @param id        unique identifier, e.g. {@code "EC-01"}
     * @param title     display name shown in the table and modal
     * @param address   full barangay address
     * @param status    {@code "OPEN"} or {@code "FULL"} (case-insensitive checks)
     * @param lat       WGS-84 latitude for the Leaflet map marker
     * @param lng       WGS-84 longitude for the Leaflet map marker
     * @param capacity  maximum number of evacuees the center can hold
     * @param occupancy current number of evacuees registered at this center
     */
    public SimpleCenter(String id, String title, String address,
                        String status, double lat, double lng,
                        int capacity, int occupancy) {
        this.id        = id;
        this.title     = title;
        this.address   = address;
        this.status    = status;
        this.lat       = lat;
        this.lng       = lng;
        this.capacity  = capacity;
        this.occupancy = occupancy;
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    /** Unique center identifier, e.g. {@code "EC-01"}. */
    public String getId()       { return id; }

    /** Human-readable display name shown in the table and modal header. */
    public String getTitle()    { return title; }

    /** Full address string, including barangay, municipality, and province. */
    public String getAddress()  { return address; }

    /**
     * Operational status.
     * @return {@code "OPEN"} when space is available, {@code "FULL"} otherwise
     */
    public String getStatus()   { return status; }

    /** WGS-84 latitude used to place the Leaflet map marker. */
    public double getLat()      { return lat; }

    /** WGS-84 longitude used to place the Leaflet map marker. */
    public double getLng()      { return lng; }

    /** Maximum evacuee capacity of this center. */
    public int getCapacity()    { return capacity; }

    /** Current number of registered evacuees. */
    public int getOccupancy()   { return occupancy; }

    // ── Derived helpers ────────────────────────────────────────────────────

    /**
     * Returns {@code true} when this center has reached or exceeded capacity.
     * Drives the map marker color (red) and the table status pill class.
     */
    public boolean isFull() {
        return "FULL".equalsIgnoreCase(status);
    }

    /**
     * Occupancy as a percentage of capacity, clamped to [0, 100].
     * Useful for displaying a progress indicator.
     */
    public int getOccupancyPercent() {
        if (capacity == 0) return 0;
        return (int) Math.min(100, Math.round(100.0 * occupancy / capacity));
    }

    // ── Object overrides ───────────────────────────────────────────────────

    @Override
    public String toString() {
        return title + " [" + id + "] — " + status +
                " (" + occupancy + "/" + capacity + ")";
    }
}