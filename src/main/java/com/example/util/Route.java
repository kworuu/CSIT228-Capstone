package com.example.util;

/**
 * Enumeration of the top-level navigation destinations in the app.
 *
 * <p>Each route maps to one FXML resource. The {@link Router} resolves
 * routes to FXML paths and swaps the scene root accordingly.</p>
 */
public enum Route {
    KIOSK,
    ADMIN_LOGIN,
    BARANGAY_LOGIN,
    ADMIN_DASHBOARD,
    BARANGAY_DASHBOARD
}