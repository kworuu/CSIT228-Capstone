package com.example.dashboard_admin;

import com.example.dao.EvacuationCenterDao;
import com.example.model.EvacuationCenter;
import com.example.util.SceneHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class AddBrgyController {

    @FXML private TextField centerNameField;
    @FXML private TextField barangayField;
    @FXML private TextField photoPathField;
    @FXML private TextField addressPathField;
    @FXML private Button btnOpenMap_extension;
    @FXML private ImageView photoPreview;
    @FXML private Label placeholderLabel; // Optional: to hide/show "No Image" text

    private final EvacuationCenterDao centerDao = new EvacuationCenterDao();


    @FXML
    private void handleOpenMap(ActionEvent event) {
        SceneHelper.nestedModal("/com/example/dashboard_admin/views/add-brgy_extension.fxml",
                "Select Location",
                btnOpenMap_extension);
    }

    @FXML
    private void handleBrowsePhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Center Photo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(((Button) event.getSource()).getScene().getWindow());

        if (selectedFile != null) {
            // Update the text field with the absolute path
            photoPathField.setText(selectedFile.getAbsolutePath());

            // Update the preview image
            Image image = new Image(selectedFile.toURI().toString());
            photoPreview.setImage(image);

            // Hide placeholder text if you added one
            if (placeholderLabel != null) {
                placeholderLabel.setVisible(false);
            }
        }
    }

    @FXML
    private void handleInsert(ActionEvent event) {
        String name = centerNameField.getText().trim();
        String brgy = barangayField.getText().trim();

        if (name.isEmpty() || brgy.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Name and Barangay are required.");
            return;
        }

        try {
            EvacuationCenter center = new EvacuationCenter();
            center.setName(name);
            center.setBarangay(brgy);
            center.setAddress(brgy);
            center.setCapacity(500);
            center.setCurrentOccupancy(0);
            center.setActive(true);
            center.setCreatedAt(LocalDateTime.now());
            center.setLatitude(new BigDecimal("14.5995"));
            center.setLongitude(new BigDecimal("120.9842"));

            centerDao.save(center);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Center '" + name + "' registered!");
            closeWindow();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    @FXML private void handleCancel(ActionEvent event) { closeWindow(); }

    private void closeWindow() {
        Stage stage = (Stage) centerNameField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}