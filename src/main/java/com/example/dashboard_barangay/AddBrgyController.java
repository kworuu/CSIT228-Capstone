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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class AddBrgyController {
    @FXML private TextField centerNameField;
    @FXML private ImageView imgPreview;
    @FXML private Label lblNoImage;
    @FXML private Button btnUploadPhoto;
    @FXML private Button btnCancel;

    private BigDecimal selectedLat;
    private BigDecimal selectedLon;
    private File selectedImageFile; // Temporarily holds the file they picked
    private final EvacuationCenterDao centerDao = new EvacuationCenterDao();

    public void initialize() {
        btnCancel.setOnAction(e -> closeWindow());
        lblNoImage.setVisible(true);
        imgPreview.setImage(null);
    }

    public void setLocationData(String barangayName, double lat, double lon) {
        this.selectedLat = BigDecimal.valueOf(lat);
        this.selectedLon = BigDecimal.valueOf(lon);
    }

    @FXML
    private void handleUploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Evacuation Center Photo");
        // Restrict them to only pick images
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(btnUploadPhoto.getScene().getWindow());

        if (file != null) {
            this.selectedImageFile = file;
            // Show a preview of the image in the UI
            Image image = new Image(file.toURI().toString());
            imgPreview.setImage(image);
            lblNoImage.setVisible(false); // Hide the "No Image" text
        }
    }

    @FXML
    private void handleInsert(ActionEvent event) {
        String name = centerNameField.getText().trim();
        String address = String.format("Lat: %.4f, Lng: %.4f", selectedLat.doubleValue(), selectedLon.doubleValue());

        if (name.isEmpty() || selectedLat == null || selectedLon == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Center Name and Location are required.");
            return;
        }
        
        // --- NEW: PHASE 2 FILE SAVING LOGIC ---
        String databasePhotoPath = null; // Default to null if no photo was uploaded

        if (selectedImageFile != null) {
            try {
                // 1. Point to your project's resources/images folder
                File destDir = new File("src/main/resources/images");
                if (!destDir.exists()) {
                    destDir.mkdirs(); // Create the folder if it doesn't exist
                }

                // 2. Give the file a unique name so it doesn't overwrite existing photos
                String uniqueFileName = System.currentTimeMillis() + "_" + selectedImageFile.getName();
                File destinationFile = new File(destDir, uniqueFileName);

                // 3. Physically copy the file from their computer into the project folder!
                Files.copy(selectedImageFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // 4. This is the exact string we save to the SQL database (e.g., "/images/123456_school.jpg")
                databasePhotoPath = "/images/" + uniqueFileName;

            } catch (Exception e) {
                System.err.println("Failed to copy image file: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "File Error", "Failed to save image: " + e.getMessage());
                return; // Stop saving if image fails
            }
        }

        try {
            SessionContext session = SessionContext.current();
            if (session == null || session.getUser() == null) {
                showAlert(Alert.AlertType.ERROR, "Session Error", "No active user session found.");
                return;
            }
            Long userId = session.getUser().id();

            EvacuationCenter center = new EvacuationCenter(
                    0, name, address, userId, databasePhotoPath, selectedLat.doubleValue(), selectedLon.doubleValue(), LocalDateTime.now()
            );
            centerDao.save(center);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Center '" + name + "' registered!");
            closeWindow();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    @FXML private void handleCancel(ActionEvent event) { closeWindow(); }
    private void closeWindow() { ((Stage) centerNameField.getScene().getWindow()).close(); }
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait();
    }
}
