package com.example.dashboard_admin.views;

import com.example.model.SupplyRequest;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class DeployItemController {

    @FXML private StackPane modalRoot; // Assuming your modal has a root StackPane

    private SupplyRequest currentRequest;
    private Runnable onCloseCallback;

    @FXML
    public void initialize() {
        // Initialization logic for your modal goes here
        // e.g., setup table views, buttons, etc.
    }

    public void initData(SupplyRequest request) {
        this.currentRequest = request;
        // Use the currentRequest data to populate your modal's UI elements
        System.out.println("DeployItemController received request: " + request.id() + " for " + request.itemName());
        // Example: lblRequestDetails.setText("Request for " + request.itemName() + " from " + request.requestingBarangay());
    }

    public void setOnClose(Runnable callback) {
        this.onCloseCallback = callback;
    }

    @FXML
    private void handleCloseModal() {
        // Logic to close the modal
        Stage stage = (Stage) modalRoot.getScene().getWindow();
        stage.close();
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    // Add other FXML handlers and logic for deployment here
}