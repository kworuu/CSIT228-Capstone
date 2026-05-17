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
    @FXML private TextField addressPathField;
    @FXML private TextField photoPathField;
    @FXML private ImageView photoPreview1;
    @FXML private Label placeholderLabel1;
    @FXML private Button btnOpenMap_extension;
    @FXML private Button btnCancel;

    private BigDecimal selectedLat;
    private BigDecimal selectedLon;
    private File selectedPhotoFile;
    private final EvacuationCenterDao centerDao = new EvacuationCenterDao();

    public void initialize() {
        btnCancel.setOnAction(e -> closeWindow());
        placeholderLabel1.setVisible(true);
        photoPreview1.setImage(null);
    }

    public void setLocationData(String address, double lat, double lon) {
        addressPathField.setText(address);
        this.selectedLat = BigDecimal.valueOf(lat);
        this.selectedLon = BigDecimal.valueOf(lon);
    }

    @FXML
    private void OpenMap_extension(ActionEvent event) {
        FXMLLoader loader = SceneHelper.openNestedModal(
                "/com/example/dashboard_barangay/modals/add-brgy_extension.fxml",
                "Select Location", btnOpenMap_extension);
        if (loader != null) {
            AddBrgyExtensionController extensionController = loader.getController();
            extensionController.setParentController(this);
        }
    }

    @FXML
    private void handleBrowsePhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Center Photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(centerNameField.getScene().getWindow());
        if (selectedFile != null) {
            selectedPhotoFile = selectedFile;
            photoPathField.setText(selectedFile.getAbsolutePath());
            Image image = new Image(selectedFile.toURI().toString());
            photoPreview1.setImage(image);
            placeholderLabel1.setVisible(false);
        }
    }

    @FXML
    private void handleInsert(ActionEvent event) {
        String name = centerNameField.getText().trim();
        String address = addressPathField.getText().trim();
        String photoPath = (selectedPhotoFile != null) ? selectedPhotoFile.getAbsolutePath() : null;

        if (name.isEmpty() || address.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Name and Address are required.");
            return;
        }
        try {
            SessionContext session = SessionContext.current();
            if (session == null || session.getUser() == null) return;
            Long userId = session.getUser().id();

            Double lat = selectedLat != null ? selectedLat.doubleValue() : 10.3157;
            Double lon = selectedLon != null ? selectedLon.doubleValue() : 123.8854;

            EvacuationCenter center = new EvacuationCenter(
                    0, name, address, userId, photoPath, lat, lon, LocalDateTime.now()
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
