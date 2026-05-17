package com.example.dashboard_kiosk.session;

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