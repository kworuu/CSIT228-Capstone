package com.example.dashboard_kiosk.session;

/**
 * Concrete {@link SessionMode} for an authenticated staff session.
 *
 * <p>Once the operator has logged in elsewhere, the kiosk no longer
 * needs to show the elevation buttons. All interactive controls stay
 * enabled so staff can search, browse events, and inspect centers.</p>
 */
public final class StaffSessionMode implements SessionMode {

    @Override
    public void applyTo(SessionContext context) {
        setVisible(context.getBarangayLoginButton(), false);
        setVisible(context.getAdminLoginButton(),    false);

        if (context.getSearchField()  != null) context.getSearchField().setDisable(false);
        if (context.getEventsButton() != null) context.getEventsButton().setDisable(false);
    }

    @Override
    public String displayName() { return "Staff Session"; }
}