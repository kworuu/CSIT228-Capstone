package com.example.dashboard_kiosk;

import java.time.format.DateTimeFormatter;

public final class KioskConstants {

    private KioskConstants() {}

    public static final String FXML_ROOT          = "BrgyUser.fxml";
    public static final String FXML_DETAIL_MODAL  = "detail-modal.fxml";
    public static final String FXML_EVENT_CELL    = "EventCell.fxml";
    public static final String STYLESHEET         = "style.css";

    public static final String JS_BRIDGE_MEMBER   = "javaBridge";

    public static final String JS_HIGHLIGHT_MARKER_FMT =
            "if(window.highlightMarker) window.highlightMarker('%s')";

    public static final String ALERT_TIMESTAMP_PATTERN = "MMM d, h:mm a";

    public static final DateTimeFormatter ALERT_FORMATTER =
            DateTimeFormatter.ofPattern(ALERT_TIMESTAMP_PATTERN);

    public static final String FULL_TIMESTAMP_PATTERN  = "MMM d, yyyy h:mm a";

    public static final DateTimeFormatter FULL_FORMATTER =
            DateTimeFormatter.ofPattern(FULL_TIMESTAMP_PATTERN);


    public static final String CSS_STATUS_TAG     = "status-tag";
    public static final String CSS_STATUS_OPEN    = "status-tag-open";
    public static final String CSS_STATUS_FULL    = "status-tag-full";
    public static final String CSS_STATUS_PENDING = "status-tag-pending";

    public static final String CSS_ALERT_ITEM     = "alert-item";
    public static final String CSS_ALERT_CRITICAL = "alert-item-critical";
    public static final String CSS_ALERT_WARNING  = "alert-item-warning";
    public static final String CSS_ALERT_INFO     = "alert-item-info";

    public static final String CSS_ALERT_TIME     = "alert-time";
    public static final String CSS_ALERT_TITLE    = "alert-title";
    public static final String CSS_ALERT_BODY     = "alert-qty";

    public static final String CSS_SUPPLY_TAG          = "supply-tag";
    public static final String CSS_DETAIL_META         = "detail-meta";
    public static final String CSS_EVACUEE_LIST_ROW    = "evacuee-list-row";

    public static final double DEFAULT_MAP_LAT  = 10.3157;
    public static final double DEFAULT_MAP_LNG  = 123.8854;
    public static final int    DEFAULT_MAP_ZOOM = 13;

    public static final String LOG_PREFIX        = "[KioskDashboard] ";
    public static final String LOG_DB_ERROR      = LOG_PREFIX + "DB error: ";
    public static final String LOG_MAP_DEBUG     = "[MAP-DEBUG] ";
}