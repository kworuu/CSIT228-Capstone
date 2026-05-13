package com.example.dashboard_kiosk.sample;

import com.example.dashboard_kiosk.user.EmergencyAlert;
import com.example.dashboard_kiosk.KioskConstants;
import com.example.dashboard_kiosk.user.SimpleCenter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Static seed data used by the kiosk dashboard while a live database
 * connection or REST endpoint is not yet wired up.
 *
 * <h2>How to replace with real data</h2>
 * <ol>
 *   <li>Implement a service / DAO that fetches {@link SimpleCenter} and
 *       {@link EmergencyAlert} records from the {@code evacuation_centers} and
 *       {@code alerts} tables respectively.</li>
 *   <li>In {@code KioskDashboardController.initialize()}, replace the calls to
 *       {@link #getSampleCenters()} / {@link #getSampleAlerts()} with calls to
 *       your service, e.g.:
 *       <pre>
 *           allCenters.addAll(evacuationCenterService.getActiveCenters());
 *       </pre>
 *   </li>
 *   <li>Delete or archive this file — it has no role in production.</li>
 * </ol>
 *
 * <p>All coordinates are WGS-84 and fall within the Argao, Cebu area
 * (approx. 9.876 – 9.890 N, 123.585 – 123.605 E).</p>
 */
public final class KioskSampleData {

    /** Utility class — no instances. */
    private KioskSampleData() {}

    // ── Evacuation Centers ─────────────────────────────────────────────────

    /**
     * Returns the five seed evacuation centers used during development.
     *
     * <p>Center fields: id · title · address · status · lat · lng ·
     * capacity · occupancy</p>
     *
     * <p>EC-01 is flagged as the focal / command-center pin in the Leaflet map
     * (see {@link KioskConstants#FOCAL_CENTER_ID}).</p>
     *
     * @return unmodifiable list of sample {@link SimpleCenter} records
     */
    public static List<SimpleCenter> getSampleCenters() {
        return List.of(
                new SimpleCenter(
                        "EC-01",
                        "Argao Command Center",
                        "Brgy. Poblacion, Argao, Cebu",
                        9.8828, 123.5953
                ),
                new SimpleCenter(
                        "EC-02",
                        "Argao Central School",
                        "Brgy. Poblacion, Argao, Cebu",
                        9.8810, 123.5980
                ),
                new SimpleCenter(
                        "EC-03",
                        "Mabolo Elementary School",
                        "Brgy. Mabolo, Argao, Cebu",
                        9.8870, 123.6010
                ),
                new SimpleCenter(
                        "EC-04",
                        "Conalum Barangay Hall",
                        "Brgy. Conalum, Argao, Cebu",
                        9.8760, 123.5900
                ),
                new SimpleCenter(
                        "EC-05",
                        "Lamacan Multi-purpose Hall",
                        "Brgy. Lamacan, Argao, Cebu",
                        9.8850, 123.5870
                )
        );
    }

    // ── Emergency Alerts ───────────────────────────────────────────────────

    /**
     * Returns three seed emergency alerts used during development.
     *
     * <p>Timestamps are expressed relative to {@code LocalDateTime.now()} at
     * call-time so they always appear recent in the UI. In production, replace
     * with absolute timestamps fetched from the database.</p>
     *
     * <p>The controller sorts these newest-first before rendering; the ordering
     * here does not matter.</p>
     *
     * @return mutable list of sample {@link EmergencyAlert} records
     *         (mutable so the controller can sort in place)
     */
    public static List<EmergencyAlert> getSampleAlerts() {
        LocalDateTime now = LocalDateTime.now();

        return List.of(
                new EmergencyAlert(
                        "Heavy Rainfall Warning",
                        "Cebu City & Surrounding Areas",
                        "Evacuate if in low-lying or flood-prone zones immediately.",
                        EmergencyAlert.Severity.CRITICAL,
                        now.minusMinutes(12)
                ),
                new EmergencyAlert(
                        "Lamacan & Mabolo Centers Full",
                        "Brgy. Lamacan, Brgy. Mabolo",
                        "Redirect evacuees to Argao Command Center (EC-01).",
                        EmergencyAlert.Severity.WARNING,
                        now.minusHours(1).minusMinutes(5)
                ),
                new EmergencyAlert(
                        "Relief Goods Dispatch",
                        "EC-01 — Argao Command Center",
                        "Water and food packs en route. ETA 45 minutes.",
                        EmergencyAlert.Severity.INFO,
                        now.minusHours(3)
                )
        );
    }
}