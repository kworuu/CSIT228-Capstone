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
    private final double lat;
    private final double lng;

    // ── Constructor ────────────────────────────────────────────────────────

    /**
     * Creates a fully-populated, immutable kiosk center record.
     *
     * @param id        unique identifier, e.g. {@code "EC-01"}
     * @param title     display name shown in the table and modal
     * @param address   full barangay address
     * @param lat       WGS-84 latitude for the Leaflet map marker
     * @param lng       WGS-84 longitude for the Leaflet map marker
     */
    public SimpleCenter(String id, String title, String address,
                        double lat, double lng) {
        this.id        = id;
        this.title     = title;
        this.address   = address;
        this.lat       = lat;
        this.lng       = lng;
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
    /** WGS-84 latitude used to place the Leaflet map marker. */
    public double getLat()      { return lat; }

    /** WGS-84 longitude used to place the Leaflet map marker. */
    public double getLng()      { return lng; }

    @Override
    public String toString() {
        return title + " [" + id + "] — ";
    }
}