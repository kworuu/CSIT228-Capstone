package com.example.map_logic_v2;

import javafx.application.Platform;

/**
 * A bridge to receive JavaScript events from the map's WebView.
 * This version is simplified to have no external dependencies.
 */
public class MapBridge {

    /**
     * Called from JavaScript when a map marker is clicked.
     * This method now only prints to the console to avoid errors.
     *
     * @param centerId The ID of the center that was clicked.
     */
    public void onMarkerClick(String centerId) {
        // The Platform.runLater is important because this is called from a JS thread.
        Platform.runLater(() -> {
            System.out.println("[MapBridge] Map marker clicked for location ID: " + centerId);
            // The original logic to open a detail modal was removed because
            // the required controller (CenterDetailController) is not present on this branch.
        });
    }
}
