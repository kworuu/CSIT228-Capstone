package com.example.dashboard_kiosk.session;

import javafx.scene.Node;

public interface SessionMode {

    void applyTo(SessionContext context);

    String displayName();

    default void setVisible(Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }
}