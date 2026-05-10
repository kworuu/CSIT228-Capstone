package com.example.dashboard_kiosk.user;

import com.example.dashboard_kiosk.KioskConstants;
import com.example.dashboard_kiosk.sample.KioskSampleData;

import java.time.LocalDateTime;

/**
 * Represents a single emergency alert displayed in the kiosk dashboard's
 * alert panel.
 *
 * <p>Alerts are immutable once created. The {@link Severity} level controls
 * both the sort priority (handled by the controller) and the CSS accent
 * class applied to the rendered card.</p>
 *
 * <p>In production this object would be hydrated from a {@code alerts} database
 * table or an external notification service. For now,
 * {@link KioskSampleData#getSampleAlerts()} provides seed records.</p>
 */
public final class EmergencyAlert {

    // ── Severity ───────────────────────────────────────────────────────────

    /**
     * Operational severity level of the alert.
     *
     * <ul>
     *   <li>{@link #CRITICAL} — immediate danger; terracotta left-border accent</li>
     *   <li>{@link #WARNING}  — elevated risk; amber left-border accent</li>
     *   <li>{@link #INFO}     — situational update; sage left-border accent</li>
     * </ul>
     *
     * The CSS class names that correspond to each level are defined in
     * {@link KioskConstants}: {@code ALERT_CSS_CRITICAL}, {@code ALERT_CSS_WARNING},
     * {@code ALERT_CSS_INFO}.
     */
    public enum Severity {
        CRITICAL,
        WARNING,
        INFO
    }

    // ── Fields ─────────────────────────────────────────────────────────────

    private final String        title;
    private final String        location;
    private final String        body;
    private final Severity      severity;
    private final LocalDateTime issuedAt;

    // ── Constructor ────────────────────────────────────────────────────────

    /**
     * Creates a fully-populated, immutable emergency alert.
     *
     * @param title     short headline, e.g. {@code "Heavy Rainfall Warning"}
     * @param location  affected area, e.g. {@code "Cebu City & Surrounding Areas"}
     * @param body      descriptive action guidance for evacuees / responders
     * @param severity  one of {@link Severity#CRITICAL}, {@link Severity#WARNING},
     *                  or {@link Severity#INFO}
     * @param issuedAt  timestamp when the alert was issued; used to sort
     *                  newest-first in the dashboard panel
     */
    public EmergencyAlert(String title, String location, String body,
                          Severity severity, LocalDateTime issuedAt) {
        this.title    = title;
        this.location = location;
        this.body     = body;
        this.severity = severity;
        this.issuedAt = issuedAt;
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    /** Short headline text displayed in bold at the top of the card. */
    public String getTitle()           { return title; }

    /** Affected area or location identifier. */
    public String getLocation()        { return location; }

    /** Full descriptive text / action guidance shown below the location. */
    public String getBody()            { return body; }

    /** Severity level that drives CSS styling and visual priority. */
    public Severity getSeverity()      { return severity; }

    /**
     * Timestamp when this alert was issued.
     * The controller sorts by this field (descending) so the newest alert
     * always appears at the top of the panel.
     */
    public LocalDateTime getIssuedAt() { return issuedAt; }

    // ── Object overrides ───────────────────────────────────────────────────

    @Override
    public String toString() {
        return "[" + severity + "] " + title + " @ " + issuedAt;
    }
}
