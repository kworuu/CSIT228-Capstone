package com.example.dashboard_barangay;

import com.example.auth.SessionContext;
import com.example.dao.EvacuationCenterDao;
import com.example.model.EvacuationCenter;
import com.example.util.SceneHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
    @FXML private TextField photoPathField;
    @FXML private TextField addressPathField;
    @FXML private Button btnOpenMap_extension;
    @FXML private ImageView photoPreview1;
    @FXML private Label placeholderLabel1; // Optional: to hide/show "No Image" text
    @FXML private Button btnCancel;
    @FXML private Button insertButton;

    private BigDecimal selectedLat;
    private BigDecimal selectedLon;
    private final EvacuationCenterDao centerDao = new EvacuationCenterDao();
    private Runnable onRegistrationSuccess;
    
    private Runnable onSaveCallback;

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void initialize() {
        btnCancel.setOnAction(SceneHelper::closeWindow);
    }
    
    public void setOnRegistrationSuccess(Runnable callback) {
        this.onRegistrationSuccess = callback;
    }

    public void setLocationData(String address, double lat, double lon) {
        addressPathField.setText(address);
        this.selectedLat = BigDecimal.valueOf(lat);
        this.selectedLon = BigDecimal.valueOf(lon);
        System.out.println("Data returned from map: " + lat + ", " + lon);
    }

    @FXML
    private void OpenMap_extension(ActionEvent event) {
        FXMLLoader loader = SceneHelper.openNestedModal(
                "/com/example/dashboard_barangay/modals/add-brgy_extension.fxml",
                "Select Location",
                btnOpenMap_extension
        );

        if (loader != null) {
            AddBrgyExtensionController extensionController = loader.getController();
            extensionController.setParentController(this);
        }
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
            if (photoPreview1 != null) {
                photoPreview1.setImage(image);
            }

            // Hide placeholder text if you added one
            if (placeholderLabel1 != null) {
                placeholderLabel1.setVisible(false);
            }
        }
    }

    @FXML
    private void handleInsert(ActionEvent event) {
        String name = centerNameField.getText().trim();
        String address = addressPathField.getText() != null ? addressPathField.getText().trim() : "";
        String photo = photoPathField.getText() != null ? photoPathField.getText().trim() : "";

        // NEW: Automatically grab the Barangay from the logged-in user!
        var session = SessionContext.current();
        if (session == null || session.getUser() == null || session.getUser().getAssignedBarangay() == null) {
             showAlert(Alert.AlertType.ERROR, "Session Error", "Could not identify your assigned Barangay. Please log in again.");
             return;
        }
        String brgy = session.getUser().getAssignedBarangay();

        if (name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Evacuation Center name is required.");
            return;
        }

        try {
            EvacuationCenter center = new EvacuationCenter();
            center.setName(name);
            center.setBarangay(brgy);
            center.setAddress(address); 
            center.setPhotoPath(photo); 
            center.setActive(true);
            center.setCreatedAt(LocalDateTime.now());
            
            // Note: capacity and currentOccupancy are completely gone!
            
            // If they didn't pick a location, default to the center of Cebu City or their Barangay
            center.setLatitude(selectedLat != null ? selectedLat : new BigDecimal("10.3157"));
            center.setLongitude(selectedLon != null ? selectedLon : new BigDecimal("123.8854"));

            centerDao.save(center);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Center '" + name + "' registered successfully!");
            
            // NEW: Trigger the map refresh on the main dashboard!
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            if (onRegistrationSuccess != null) {
                 onRegistrationSuccess.run();
            }
            
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