package com.example.dashboard_barangay;

import com.example.auth.SessionContext;
import com.example.dao.EvacuationCenterDao;
import com.example.dao.EvacueeDao;
import com.example.model.EvacuationCenter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;

public class RegisterEvacueeController {

    @FXML private TextField textFieldName;
    @FXML private ComboBox<EvacuationCenter> comboBoxCenter;
    @FXML private TextField textFieldContact;
    @FXML private Label labelError;
    @FXML private Button buttonRegister;
    @FXML private Button buttonCancel;

    private final EvacuationCenterDao centerDao = new EvacuationCenterDao();
    private final EvacueeDao evacueeDao = new EvacueeDao();

    private String currentBarangay;
    private Runnable onRegistrationSuccess;
    private boolean isEditMode = false;
    private long editingEvacueeId = 0;

    @FXML
    public void initialize() {
        labelError.setVisible(false);
        setupComboBox();
    }

    public void initData(String barangay, Runnable onSuccess) {
        this.currentBarangay = barangay;
        this.onRegistrationSuccess = onSuccess;
        loadEvacuationCenters();
    }
    public void initEditData(long evacueeId, String currentName, String barangay, Runnable onSuccess) {
        this.isEditMode = true;
        this.editingEvacueeId = evacueeId;
        this.currentBarangay = barangay;
        this.onRegistrationSuccess = onSuccess;

        // Change the UI text to look like an Edit Modal
        buttonRegister.setText("Save Changes");
        textFieldName.setText(currentName);

        loadEvacuationCenters();
    }

    private void setupComboBox() {
        comboBoxCenter.setConverter(new StringConverter<>() {
            @Override
            public String toString(EvacuationCenter center) {
                return center == null ? null : center.name();
            }

            @Override
            public EvacuationCenter fromString(String string) {
                return comboBoxCenter.getItems().stream()
                        .filter(c -> c.name().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
    }

    private void loadEvacuationCenters() {
        try {
            if (currentBarangay == null) {
                showError("Barangay context missing.");
                return;
            }

            List<EvacuationCenter> centers = centerDao.findByBarangay(currentBarangay);
            ObservableList<EvacuationCenter> observableCenters = FXCollections.observableArrayList(centers);
            comboBoxCenter.setItems(observableCenters);

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to load evacuation centers.");
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        labelError.setVisible(false);

        String name = textFieldName.getText().trim();
        String contact = textFieldContact.getText().trim();
        EvacuationCenter selectedCenter = comboBoxCenter.getValue();

        if (name.isEmpty()) {
            showError("Full Name is required.");
            return;
        }
        if (selectedCenter == null) {
            showError("Please assign an Evacuation Center.");
            return;
        }

        try {
            if (isEditMode) {
                // UPDATE EXISTING
                String finalContact = contact.isEmpty() ? null : contact;
                evacueeDao.updateEvacuee(editingEvacueeId, name, finalContact, selectedCenter.id());

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Evacuee updated successfully!", ButtonType.OK);
                alert.showAndWait();
            } else {
                // REGISTER NEW
                Long userId = SessionContext.current() != null && SessionContext.current().getUser() != null
                        ? SessionContext.current().getUser().id() : null;

                com.example.model.Evacuee newEvacuee = new com.example.model.Evacuee(
                        0, name, contact.isEmpty() ? null : contact, null, selectedCenter.id(), userId, null, null);
                evacueeDao.saveEvacuee(newEvacuee);

                Alert alert = new Alert(Alert.AlertType.INFORMATION, name + " assigned to " + selectedCenter.name() + ".", ButtonType.OK);
                alert.showAndWait();
            }

            if (onRegistrationSuccess != null) onRegistrationSuccess.run();
            closeWindow();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Database error: Failed to process request.");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void showError(String message) {
        labelError.setText(message);
        labelError.setVisible(true);
    }

    private void closeWindow() {
        Stage stage = (Stage) buttonCancel.getScene().getWindow();
        stage.close();
    }
}
