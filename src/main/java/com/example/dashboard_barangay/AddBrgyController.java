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
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.SQLException;

public class AddBrgyController {

    @FXML private TextField centerNameField;
    @FXML private TextField addressPathField;
    @FXML private Button btnOpenMap_extension;
    @FXML private Button btnCancel;
    @FXML private Button btnSaveLoc;

    private BigDecimal selectedLat;
    private BigDecimal selectedLon;
    private final EvacuationCenterDao centerDao = new EvacuationCenterDao();

    public void initialize() {
        btnCancel.setOnAction(e -> closeWindow());
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
                "Select Location",
                btnOpenMap_extension
        );

        if (loader != null) {
            AddBrgyExtensionController extensionController = loader.getController();
            extensionController.setParentController(this);
        }
    }

    @FXML
    private void handleInsert(ActionEvent event) {
        String name = centerNameField.getText().trim();
        String address = addressPathField.getText().trim();

        if (name.isEmpty() || address.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Name and Address are required.");
            return;
        }

        try {
            SessionContext session = SessionContext.current();
            if (session == null || session.getUser() == null) return;

            // Fixed the getter error:
            String userBrgy = session.getUser().assignedBarangay();
            Long userId = session.getUser().id();

            Double lat = selectedLat != null ? selectedLat.doubleValue() : 14.5995;
            Double lon = selectedLon != null ? selectedLon.doubleValue() : 120.9842;

            // Use the new 11-parameter constructor (no structural status or active flags)
            EvacuationCenter center = new EvacuationCenter(
                    0, name, address, userBrgy, 100, 0, lat, lon, userId, null, null
            );

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