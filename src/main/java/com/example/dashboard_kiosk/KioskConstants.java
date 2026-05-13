package com.example.dashboard_kiosk;

import com.example.dashboard_kiosk.user.EmergencyAlert;

import java.time.format.DateTimeFormatter;

/**
 * Compile-time constants for the kiosk dashboard module.
 *
 * <p>Centralising every magic string and number here means:</p>
 * <ul>
 *   <li>Changing a CSS class name, anchor offset, or date format requires
 *       editing exactly one file.</li>
 *   <li>All values are self-documenting — the name explains the intent,
 *       the Javadoc explains the context.</li>
 *   <li>No {@code "EC-01"} or {@code "16.0"} scattered across controllers.</li>
 * </ul>
 *
 * <p>Constants are grouped by concern:</p>
 * <ol>
 *   <li>FXML / resource paths</li>
 *   <li>Map configuration</li>
 *   <li>Modal layout anchors</li>
 *   <li>Date / time formatting</li>
 *   <li>CSS class names — status pills</li>
 *   <li>CSS class names — alert severity</li>
 *   <li>CSS class names — alert card labels</li>
 *   <li>CSS class names — supply tags &amp; occupancy</li>
 *   <li>Filter tokens</li>
 *   <li>JSON serialisation</li>
 *   <li>Console / log prefixes</li>
 * </ol>
 */
public final class KioskConstants {

    /** Utility class — never instantiated. */
    private KioskConstants() {}

    // ══════════════════════════════════════════════════════════════════════
    // 1. FXML / RESOURCE PATHS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Classpath-relative path to the floating detail-modal FXML.
     * Resolved via {@code getClass().getResource(DETAIL_MODAL_FXML)} inside
     * {@code KioskDashboardController}.
     */
    public static final String DETAIL_MODAL_FXML = "detail-modal.fxml";

    // ══════════════════════════════════════════════════════════════════════
    // 2. MAP CONFIGURATION
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ID of the center that receives the large pulsing focal marker on the
     * Leaflet map. All other centers render as smaller secondary pins.
     * Change to {@code "EC-02"} etc. if the command center relocates.
     */
    public static final String FOCAL_CENTER_ID = "EC-01";

    /**
     * JavaScript call used to trigger the bounce animation on a map marker.
     * The {@code %s} placeholder is replaced with the center ID at runtime.
     *
     * <p>Example expanded form:
     * {@code if(window.highlightMarker) highlightMarker('EC-03')}</p>
     */
    public static final String JS_HIGHLIGHT_MARKER =
            "if(window.highlightMarker) highlightMarker('%s')";

    // ══════════════════════════════════════════════════════════════════════
    // 3. MODAL LAYOUT ANCHORS  (pixels from edge of mapOverlayPane)
    // ══════════════════════════════════════════════════════════════════════

    /** Distance in pixels from the bottom edge of the map pane to the modal. */
    public static final double MODAL_ANCHOR_BOTTOM = 16.0;

    /** Distance in pixels from the left edge of the map pane to the modal. */
    public static final double MODAL_ANCHOR_LEFT   = 16.0;

    // ══════════════════════════════════════════════════════════════════════
    // 4. DATE / TIME FORMATTING
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Pattern used to format alert timestamps in the alerts panel.
     * Example output: {@code "May 11, 3:45 PM"}.
     */
    public static final String ALERT_TIMESTAMP_PATTERN = "MMM d, h:mm a";

    /**
     * Pre-compiled {@link DateTimeFormatter} for alert timestamps.
     * Shared across all alert card builds — formatters are thread-safe
     * and expensive to create.
     */
    public static final DateTimeFormatter ALERT_FORMATTER =
            DateTimeFormatter.ofPattern(ALERT_TIMESTAMP_PATTERN);

    // ══════════════════════════════════════════════════════════════════════
    // 5. CSS CLASS NAMES — STATUS PILLS
    // ══════════════════════════════════════════════════════════════════════

    /** Base style class applied to every status pill label. */
    public static final String CSS_STATUS_TAG      = "status-tag";

    /**
     * Applied when center status is {@code "OPEN"}.
     * Renders a muted forest-green pill.
     */
    public static final String CSS_STATUS_OPEN     = "status-tag-open";

    /**
     * Applied when center status is {@code "FULL"}.
     * Renders a terracotta-red pill.
     */
    public static final String CSS_STATUS_FULL     = "status-tag-full";

    // ══════════════════════════════════════════════════════════════════════
    // 6. CSS CLASS NAMES — ALERT CARD SEVERITY
    // ══════════════════════════════════════════════════════════════════════

    /** Base class applied to every alert card VBox. */
    public static final String CSS_ALERT_ITEM      = "alert-item";

    /** Left-border terracotta accent for {@link EmergencyAlert.Severity#CRITICAL}. */
    public static final String CSS_ALERT_CRITICAL  = "alert-item-critical";

    /** Left-border amber accent for {@link EmergencyAlert.Severity#WARNING}. */
    public static final String CSS_ALERT_WARNING   = "alert-item-warning";

    /** Left-border sage accent for {@link EmergencyAlert.Severity#INFO}. */
    public static final String CSS_ALERT_INFO      = "alert-item-info";

    // ══════════════════════════════════════════════════════════════════════
    // 7. CSS CLASS NAMES — ALERT CARD LABELS
    // ══════════════════════════════════════════════════════════════════════

    /** Monospace timestamp label at the very top of each alert card. */
    public static final String CSS_ALERT_TIME      = "alert-time";

    /** Bold headline label inside an alert card. */
    public static final String CSS_ALERT_TITLE     = "alert-title";

    /** Muted location/area label inside an alert card. */
    public static final String CSS_ALERT_LOCATION  = "alert-location";

    /** Body / action guidance text label inside an alert card. */
    public static final String CSS_ALERT_BODY      = "alert-qty";   // keeps CSS file in sync

    // ══════════════════════════════════════════════════════════════════════
    // 8. CSS CLASS NAMES — SUPPLY TAGS & OCCUPANCY
    // ══════════════════════════════════════════════════════════════════════

    /** Applied to each supply pill Label inside the modal's FlowPane. */
    public static final String CSS_SUPPLY_TAG      = "supply-tag";

    /** Applied to the occupancy label in the modal (monospaced numeric style). */
    public static final String CSS_OCCUPANCY_LABEL = "occupancy-label";

    // ══════════════════════════════════════════════════════════════════════
    // 9. FILTER TOKENS  (used by search + segmented-control logic)
    // ══════════════════════════════════════════════════════════════════════

    /** Filter token meaning "show all centers regardless of status". */
    public static final String FILTER_ALL  = "ALL";

    /** Filter token that shows only centers whose status equals {@code "OPEN"}. */
    public static final String FILTER_OPEN = "OPEN";

    /** Filter token that shows only centers whose status equals {@code "FULL"}. */
    public static final String FILTER_FULL = "FULL";

    /** CSS class added to the active segment button in the filter control. */
    public static final String CSS_SEGMENT_ACTIVE = "segment-active";

    // ══════════════════════════════════════════════════════════════════════
    // 10. JSON SERIALISATION — map centers payload
    // ══════════════════════════════════════════════════════════════════════

    /**
     * {@link String#format} template for a single center object in the JSON
     * array passed to the Leaflet map via {@link com.example.map_logic_v2.MapHtmlProvider}.
     *
     * <p>Positional arguments:</p>
     * <ol>
     *   <li>{@code %s} — escaped center ID</li>
     *   <li>{@code %s} — escaped center title</li>
     *   <li>{@code %s} — latitude (double)</li>
     *   <li>{@code %s} — longitude (double)</li>
     *   <li>{@code %s} — status string ("OPEN" or "FULL")</li>
     *   <li>{@code %b} — {@code true} if this is the focal center</li>
     *   <li>{@code %d} — capacity integer</li>
     *   <li>{@code %d} — occupancy integer</li>
     * </ol>
     */
    public static final String JSON_CENTER_TEMPLATE =
            "{\"id\":\"%s\",\"name\":\"%s\",\"lat\":%s,\"lng\":%s,\"focus\":%b}";

    /**
     * Prefix for the map's JavaScript call to the location marker highlight
     * function. Prefix the center ID with a pin emoji in alert location labels.
     */
    public static final String LOCATION_ICON_PREFIX = "📍 ";

    // ══════════════════════════════════════════════════════════════════════
    // 11. CONSOLE / LOG PREFIXES
    // ══════════════════════════════════════════════════════════════════════

    /** Prefix for all console messages originating from the kiosk controller. */
    public static final String LOG_PREFIX = "[KioskDashboard] ";

    /** Message logged when the user clicks "Show Route" in the detail modal. */
    public static final String LOG_SHOW_ROUTE    = LOG_PREFIX + "Show route requested";

    /** Message logged when the user clicks "Full Details" in the detail modal. */
    public static final String LOG_VIEW_DETAILS  = LOG_PREFIX + "View details requested";

    /** Error message prefix when detail-modal.fxml cannot be loaded. */
    public static final String LOG_MODAL_ERROR   = LOG_PREFIX + "Could not load detail-modal.fxml: ";
}