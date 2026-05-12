package com.example.dashboard_barangay;

import com.example.util.DBConnectionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegisterEvacueeController {

    @FXML private TextField textFieldName;
    @FXML private ComboBox<CenterItem> comboBoxCenter;
    @FXML private TextField textFieldFamilySize;
    @FXML private TextField textFieldContact;
    @FXML private Label labelError;
    @FXML private Button buttonCancel;

    private String currentBarangay;
    private Runnable onRegistrationSuccess;

    // Helper class to store center ID and Name in the ComboBox
    private static class CenterItem {
        final long id;
        final String name;

        CenterItem(long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public void initData(String barangay, Runnable onSuccess) {
        this.currentBarangay = barangay;
        this.onRegistrationSuccess = onSuccess;
        loadCenters();
    }

    private void loadCenters() {
        ObservableList<CenterItem> centers = FXCollections.observableArrayList();
        String sql = "SELECT id, name FROM evacuation_centers WHERE barangay = ? ORDER BY name ASC";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currentBarangay);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                centers.add(new CenterItem(rs.getLong("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            System.err.println("DB error loading centers for registration: " + e.getMessage());
        }

        comboBoxCenter.setItems(centers);
    }

    @FXML private void handleRegister() {
        labelError.setVisible(false);

        String name = textFieldName.getText().trim();
        CenterItem selectedCenter = comboBoxCenter.getValue();
        String familySizeStr = textFieldFamilySize.getText().trim();
        String contact = textFieldContact.getText().trim();

        // Basic verification logic
        if (name.isEmpty() || selectedCenter == null || familySizeStr.isEmpty()) {
            showError("Please fill in all required fields (Name, Center, Family Size).");
            return;
        }

        int familySize;
        try {
            familySize = Integer.parseInt(familySizeStr);
            if (familySize <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Family size must be a positive number.");
            return;
        }

        // Insert into database
        String sql = "INSERT INTO evacuee_registrations (full_name, center_id, barangay, contact_number, family_size, verification_status) VALUES (?, ?, ?, ?, ?, 'Verified')";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setLong(2, selectedCenter.id);
            ps.setString(3, currentBarangay);
            ps.setString(4, contact);
            ps.setInt(5, familySize);

            ps.executeUpdate();

            if (onRegistrationSuccess != null) {
                onRegistrationSuccess.run();
            }
            closeModal();

        } catch (SQLException e) {
            System.err.println("Error saving evacuee registration: " + e.getMessage());
            showError("Database error occurred while saving.");
        }
    }

    private void showError(String message) {
        labelError.setText(message);
        labelError.setVisible(true);
    }

    @FXML private void handleCancel() {
        closeModal();
    }

    private void closeModal() {
        Stage stage = (Stage) buttonCancel.getScene().getWindow();
        stage.close();
    }
}