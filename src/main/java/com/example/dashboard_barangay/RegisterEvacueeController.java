package com.example.dashboard_barangay;

import com.example.auth.SessionContext;
import com.example.dao.EvacuationCenterDao;
import com.example.dao.EvacueeDao;
import com.example.model.EvacuationCenter;
import com.example.model.Evacuee;
import com.example.model.User;
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

    // Data Access Objects for database operations
    private final EvacuationCenterDao centerDao = new EvacuationCenterDao();
    private final EvacueeDao evacueeDao = new EvacueeDao();
    
    private String currentBarangay;
    private Runnable onRegistrationSuccess;

    @FXML
    public void initialize() {
        labelError.setVisible(false);
        setupComboBox();
    }
    
    /**
     * Initializes the modal with the current barangay and a callback to refresh the table.
     */
    public void initData(String barangay, Runnable onSuccess) {
        this.currentBarangay = barangay;
        this.onRegistrationSuccess = onSuccess;
        loadEvacuationCenters();
    }

    private void setupComboBox() {
        // This tells the ComboBox to display the Center's Name, not its Java memory address
        comboBoxCenter.setConverter(new StringConverter<EvacuationCenter>() {
            @Override
            public String toString(EvacuationCenter center) {
                return center == null ? null : center.getName();
            }

            @Override
            public EvacuationCenter fromString(String string) {
                return null; // Not needed for our use case
            }
        });
    }

    private void loadEvacuationCenters() {
        try {
            if (currentBarangay == null) {
                showError("Barangay context missing.");
                return;
            }

            // Fetch only centers belonging to this specific barangay
            List<EvacuationCenter> centers = centerDao.findByBarangay(currentBarangay);

            // Populate the dropdown
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

        // 1. Validation
        if (name.isEmpty()) {
            showError("Full Name is required.");
            return;
        }
        if (selectedCenter == null) {
            showError("Please assign an Evacuation Center.");
            return;
        }

        // 2. Save to Database
        try {
            Evacuee newEvacuee = new Evacuee();
            newEvacuee.setFullNameEnc(name);
            newEvacuee.setContactEnc(contact.isEmpty() ? null : contact);
            newEvacuee.setBarangay(currentBarangay);
            newEvacuee.setEvacuationCenterId(selectedCenter.getId());

            // Assuming verification_status defaults to 'pending' in your DB/Model

            evacueeDao.save(newEvacuee); // Saves to the database!

            // 3. Show Success & Close
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Registration Successful");
            alert.setHeaderText(null);
            alert.setContentText(name + " has been successfully assigned to " + selectedCenter.getName() + ".");
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