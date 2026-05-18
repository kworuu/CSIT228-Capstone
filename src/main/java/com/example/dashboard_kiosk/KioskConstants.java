package com.example.dashboard_kiosk;

import java.time.format.DateTimeFormatter;

/**
 * Compile-time constants for the kiosk dashboard module.
 *
 * <p>Every CSS class name, FXML path, format string, and JavaScript snippet
 * lives here so cosmetic or wiring changes touch exactly one file.</p>
 *
 * <p>Constants are grouped by concern:</p>
 * <ol>
 *   <li>FXML / resource paths</li>
 *   <li>Map configuration &amp; JS interop</li>
 *   <li>Date / time formatting</li>
 *   <li>CSS class names — status pills</li>
 *   <li>CSS class names — alert severity</li>
 *   <li>CSS class names — alert card labels</li>
 *   <li>CSS class names — supply tags &amp; lists</li>
 *   <li>Defaults — map view</li>
 *   <li>Console / log prefixes</li>
 * </ol>
 */
public final class KioskConstants {

    /** Utility class — never instantiated. */
    private KioskConstants() {}

    // ─────────────────────────────────────────────────────────────────────
    // 1. FXML / RESOURCE PATHS
    // ─────────────────────────────────────────────────────────────────────

    public static final String FXML_ROOT          = "dashboard-user.fxml";
    public static final String FXML_DETAIL_MODAL  = "detail-modal.fxml";
    public static final String FXML_EVENT_CELL    = "event-cell.fxml";
    public static final String STYLESHEET         = "style.css";

    // ─────────────────────────────────────────────────────────────────────
    // 2. MAP CONFIGURATION & JS INTEROP
    // ─────────────────────────────────────────────────────────────────────

    /** JavaScript bridge member name installed on {@code window}. */
    public static final String JS_BRIDGE_MEMBER   = "javaBridge";

    /** Template for invoking the marker-highlight function in the WebView. */
    public static final String JS_HIGHLIGHT_MARKER_FMT =
            "if(window.highlightMarker) window.highlightMarker('%s')";

    // ─────────────────────────────────────────────────────────────────────
    // 3. DATE / TIME FORMATTING
    // ─────────────────────────────────────────────────────────────────────

    public static final String ALERT_TIMESTAMP_PATTERN = "MMM d, h:mm a";

    public static final DateTimeFormatter ALERT_FORMATTER =
            DateTimeFormatter.ofPattern(ALERT_TIMESTAMP_PATTERN);

    public static final String FULL_TIMESTAMP_PATTERN  = "MMM d, yyyy h:mm a";

    public static final DateTimeFormatter FULL_FORMATTER =
            DateTimeFormatter.ofPattern(FULL_TIMESTAMP_PATTERN);

    // ─────────────────────────────────────────────────────────────────────
    // 4. CSS — STATUS PILLS
    // ─────────────────────────────────────────────────────────────────────

    public static final String CSS_STATUS_TAG     = "status-tag";
    public static final String CSS_STATUS_OPEN    = "status-tag-open";
    public static final String CSS_STATUS_FULL    = "status-tag-full";
    public static final String CSS_STATUS_PENDING = "status-tag-pending";

    // ─────────────────────────────────────────────────────────────────────
    // 5. CSS — ALERT CARD SEVERITY
    // ─────────────────────────────────────────────────────────────────────

    public static final String CSS_ALERT_ITEM     = "alert-item";
    public static final String CSS_ALERT_CRITICAL = "alert-item-critical";
    public static final String CSS_ALERT_WARNING  = "alert-item-warning";
    public static final String CSS_ALERT_INFO     = "alert-item-info";

    // ─────────────────────────────────────────────────────────────────────
    // 6. CSS — ALERT CARD LABELS
    // ─────────────────────────────────────────────────────────────────────

    public static final String CSS_ALERT_TIME     = "alert-time";
    public static final String CSS_ALERT_TITLE    = "alert-title";
    public static final String CSS_ALERT_BODY     = "alert-qty";

    // ─────────────────────────────────────────────────────────────────────
    // 7. CSS — SUPPLY TAGS & LIST ROWS
    // ─────────────────────────────────────────────────────────────────────

    public static final String CSS_SUPPLY_TAG          = "supply-tag";
    public static final String CSS_DETAIL_META         = "detail-meta";
    public static final String CSS_EVACUEE_LIST_ROW    = "evacuee-list-row";

    // ─────────────────────────────────────────────────────────────────────
    // 8. DEFAULTS — MAP VIEW
    // ─────────────────────────────────────────────────────────────────────

    public static final double DEFAULT_MAP_LAT  = 10.3157;
    public static final double DEFAULT_MAP_LNG  = 123.8854;
    public static final int    DEFAULT_MAP_ZOOM = 13;

    // ─────────────────────────────────────────────────────────────────────
    // 9. CONSOLE / LOG PREFIXES
    // ─────────────────────────────────────────────────────────────────────

    public static final String LOG_PREFIX        = "[KioskDashboard] ";
    public static final String LOG_DB_ERROR      = LOG_PREFIX + "DB error: ";
    public static final String LOG_MAP_DEBUG     = "[MAP-DEBUG] ";
}