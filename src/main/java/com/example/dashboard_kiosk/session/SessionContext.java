package com.example.dashboard_kiosk.session;

import javafx.scene.Node;

/**
 * Bag of node references that {@link SessionMode} strategies operate on.
 *
 * <p>Keeping these references in a dedicated context object means
 * the strategy implementations stay free of FXML coupling and the
 * {@link com.example.dashboard_kiosk.controller.KioskDashboardController}
 * exposes only what the strategy actually needs.</p>
 */
public final class SessionContext {

    private final Node barangayLoginButton;
    private final Node adminLoginButton;
    private final Node searchField;
    private final Node eventsButton;

    public SessionContext(Node barangayLoginButton,
                          Node adminLoginButton,
                          Node searchField,
                          Node eventsButton) {
        this.barangayLoginButton = barangayLoginButton;
        this.adminLoginButton    = adminLoginButton;
        this.searchField         = searchField;
        this.eventsButton        = eventsButton;
    }

    public Node getBarangayLoginButton() { return barangayLoginButton; }
    public Node getAdminLoginButton()    { return adminLoginButton; }
    public Node getSearchField()         { return searchField; }
    public Node getEventsButton()        { return eventsButton; }
}