package com.example.dashboard_barangay;

import com.example.auth.SessionContext;
import com.example.dao.EvacuationCenterDao;
import com.example.dao.EvacueeDao;
import com.example.model.EvacuationCenter;
import com.example.model.Evacuee;
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

    private void setupComboBox() {
        comboBoxCenter.setConverter(new StringConverter<EvacuationCenter>() {
            @Override
            public String toString(EvacuationCenter center) {
                return center == null ? null : center.name();
            }

            @Override
            public EvacuationCenter fromString(String string) {
                return null;
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
            Long userId = SessionContext.current() != null && SessionContext.current().getUser() != null
                    ? SessionContext.current().getUser().id() : null;

            // NEW: Using the 9-parameter Evacuee record constructor
            Evacuee newEvacuee = new Evacuee(
                    0,
                    name,
                    contact.isEmpty() ? null : contact,
                    currentBarangay,
                    null,
                    selectedCenter.id(),
                    userId,
                    null,
                    null
            );

            evacueeDao.saveEvacuee(newEvacuee);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Registration Successful");
            alert.setHeaderText(null);
            alert.setContentText(name + " has been successfully assigned to " + selectedCenter.name() + ".");
            alert.showAndWait();

            if (onRegistrationSuccess != null) {
                onRegistrationSuccess.run();
            }

            closeWindow();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Database error: Failed to register evacuee.");
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