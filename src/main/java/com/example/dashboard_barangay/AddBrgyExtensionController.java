package com.example.dashboard_barangay;

import com.example.map_logic_v2.PickerMapHtmlProvider;
import com.example.map_tiles.TilePrefetchService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import javafx.stage.Stage;

public class AddBrgyExtensionController {

    @FXML private WebView extensionWebViewMap;
    @FXML private Button btnCancelLoc;
    @FXML private Button btnConfirmLoc;

    private AddBrgyController parentController;
    private double selectedLat = 0.0;
    private double selectedLng = 0.0;
    private boolean hasSelected = false;

    public void setParentController(AddBrgyController parentController) {
        this.parentController = parentController;
    }

    @FXML
    public void initialize() {
        setupMap();
    }

    private void setupMap() {
        extensionWebViewMap.getEngine().setJavaScriptEnabled(true);
        
        // Setup the JS Bridge
        extensionWebViewMap.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject win = (JSObject) extensionWebViewMap.getEngine().executeScript("window");
                win.setMember("javaBridge", new PickerBridge());
            }
        });

        // Use the Barangay's center coordinates to start (example: Lahug)
        double startLat = 10.3340; 
        double startLng = 123.8950;
        
        int tilePort = -1;
        try { tilePort = TilePrefetchService.getInstance().startServer(); } catch (Exception ignored) {}

        // Load the new Picker HTML!
        extensionWebViewMap.getEngine().loadContent(
                PickerMapHtmlProvider.getMapHTML(startLat, startLng, 15, tilePort)
        );
    }

    @FXML
    private void handleConfirmLocation(ActionEvent event) {
        if (!hasSelected) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please click on the map to pin a location first.");
            alert.showAndWait();
            return;
        }
        
        // Pass the coordinates back to the main form!
        // We will pass a generic address string for now, but the coordinates are the real prize.
        if (parentController != null) {
            parentController.setLocationData("Pinned from Map", selectedLat, selectedLng);
        }
        closeWindow();
    }

    @FXML
    private void handleCancelLocation(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnCancelLoc.getScene().getWindow();
        stage.close();
    }

    // --- Inner class for JS Bridge ---
    public class PickerBridge {
        public void setCoordinates(double lat, double lng) {
            Platform.runLater(() -> {
                selectedLat = lat;
                selectedLng = lng;
                hasSelected = true;
                // Optional: You could enable/highlight the confirm button here
            });
        }
    }
}