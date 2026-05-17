package com.example.dashboard_kiosk.session;

/**
 * Concrete {@link SessionMode} for the public kiosk: search and event
 * browsing remain available, while both login buttons are shown so any
 * authorised staff member can elevate to a privileged session.
 *
 * <p>This is the default mode applied by
 * {@link com.example.dashboard_kiosk.controller.KioskDashboardController}
 * at startup. Override the controller's strategy assignment to swap in a
 * different mode for kiosks deployed in restricted areas.</p>
 */
public final class PublicSessionMode implements SessionMode {

    @Override
    public void applyTo(SessionContext context) {
        // Login affordances stay visible — kiosks are placed in public spaces
        // but still need to allow authorised staff to elevate.
        setVisible(context.getBarangayLoginButton(), true);
        setVisible(context.getAdminLoginButton(),    true);

        // Read-only kiosk: search is enabled, the events drawer can be opened.
        if (context.getSearchField()  != null) context.getSearchField().setDisable(false);
        if (context.getEventsButton() != null) context.getEventsButton().setDisable(false);
    }

    @Override
    public String displayName() { return "Public Kiosk"; }
}