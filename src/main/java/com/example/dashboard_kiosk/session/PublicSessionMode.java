package com.example.dashboard_kiosk.session;

public final class PublicSessionMode implements SessionMode {

    @Override
    public void applyTo(SessionContext context) {

        setVisible(context.getBarangayLoginButton(), true);
        setVisible(context.getAdminLoginButton(),    true);

        if (context.getSearchField()  != null) context.getSearchField().setDisable(false);
        if (context.getEventsButton() != null) context.getEventsButton().setDisable(false);
    }

    @Override
    public String displayName() { return "Public Kiosk"; }
}