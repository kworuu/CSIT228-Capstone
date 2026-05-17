package com.example.dashboard_kiosk.model;

/**
 * Immutable, read-only representation of an evacuation center as displayed
 * on the public kiosk dashboard.
 *
 * <p>This record is intentionally decoupled from the persistence-layer
 * {@code EvacuationCenter} entity. The kiosk view is public-facing and
 * read-only, so changes to the database schema never affect what the
 * public screen renders, and vice-versa.</p>
 *
 * <p>Fields:</p>
 * <ul>
 *   <li>{@code id}        — unique center identifier (string form of DB primary key).</li>
 *   <li>{@code title}     — human-readable display name shown in the table and modal.</li>
 *   <li>{@code address}   — full address line.</li>
 *   <li>{@code barangay}  — administering barangay name.</li>
 *   <li>{@code status}    — operational status ({@code "OPEN"} / {@code "FULL"} / {@code "ACTIVE"}).</li>
 *   <li>{@code createdAt} — human-formatted creation timestamp.</li>
 *   <li>{@code latitude}  — WGS-84 latitude for map placement.</li>
 *   <li>{@code longitude} — WGS-84 longitude for map placement.</li>
 * </ul>
 */
public record EvacuationSite(
        String id,
        String title,
        String address,
        String barangay,
        String status,
        String createdAt,
        double latitude,
        double longitude
) {
}