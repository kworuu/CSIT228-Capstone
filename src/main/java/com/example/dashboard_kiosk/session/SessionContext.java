package com.example.dashboard_kiosk.session;

import javafx.scene.Node;

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