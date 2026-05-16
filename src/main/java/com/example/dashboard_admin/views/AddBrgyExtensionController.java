package com.example.dashboard_admin.views;

import com.example.util.SceneHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;




public class AddBrgyExtensionController {
    private AddBrgyController parentController;

    @FXML
    private Button btnConfirm;
    @FXML
    private Button btnCancel;
    @FXML
    private TextField mapSearchField;
    @FXML
    private VBox rootPane;


    public void initialize() {
        btnCancel.setOnAction(SceneHelper::closeWindow);
    }

    public void setParentController(AddBrgyController parent) {
        this.parentController = parent;
    }

    @FXML
    private void handleSaveLocation(ActionEvent event) {
        String address = mapSearchField.getText();

        // Mock coordinates for now
        double lat = 14.5995;
        double lon = 120.9842;

        if (parentController != null && !address.isEmpty()) {
            parentController.setLocationData(address, lat, lon);
            SceneHelper.closeWindow(event);
        } else {
            // Optional: Show alert if they haven't picked a location
            System.out.println("Please select a location first.");
        }
    }

    // Helper for the parent to find the stage
    public Node getAnyNodeFromExtension() {
        return btnConfirm;
    }


    @FXML
    private void handleConfirmLocation(ActionEvent event) {
        // Pull these from wherever your map stores them (WebView JS callback,
        // text fields, marker state, etc.)
        String address = "...";
        double lat = 0.0;
        double lon = 0.0;

        if (parentController != null) {
            parentController.setLocationData(address, lat, lon);
        }

        // Close this modal — modal 1's setOnHidden will reshow it
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }



}