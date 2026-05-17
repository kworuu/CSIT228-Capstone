package com.example.dashboard_admin.views;

import com.example.dao.InventoryItemDao;
import com.example.model.InventoryItem;
import com.example.auth.SessionContext;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class AddItemController {

    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField unitField;
    @FXML private TextField quantityField; // Added field connection
    @FXML private TextField lowThresholdField;
    @FXML private TextField criticalThresholdField;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private final InventoryItemDao inventoryDao = new InventoryItemDao();

    @FXML
    public void initialize() {
        categoryCombo.getItems().addAll("food", "water", "non-food", "medical");

        // Apply numeric validation to the new quantity field along with thresholds
        setupNumericValidation(quantityField);
        setupNumericValidation(lowThresholdField);
        setupNumericValidation(criticalThresholdField);

        btnCancel.setOnAction(e -> closeStage());
        btnSave.setOnAction(e -> handleSave());
    }

    private void handleSave() {
        if (!isInputValid()) return;

        try {
            SessionContext session = SessionContext.current();
            Long userId = (session != null && session.getUser() != null) ? session.getUser().id() : null;

            // Instantiating item with user-defined initial quantity
            InventoryItem newItem = new InventoryItem(
                    0,
                    nameField.getText().trim(),
                    categoryCombo.getValue(),
                    unitField.getText().trim(),
                    Integer.parseInt(criticalThresholdField.getText()),
                    Integer.parseInt(lowThresholdField.getText()),
                    Integer.parseInt(quantityField.getText()), // Changed from hardcoded 0
                    LocalDateTime.now(),
                    userId
            );

            inventoryDao.save(newItem);
            closeStage();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save.");
        }
    }

    private boolean isInputValid() {
        String errorMessage = "";
        if (nameField.getText() == null || nameField.getText().isEmpty()) errorMessage += "Item Name is required.\n";
        if (categoryCombo.getValue() == null) errorMessage += "Category must be selected.\n";
        if (unitField.getText() == null || unitField.getText().isEmpty()) errorMessage += "Unit is required.\n";

        // Added validation check for initial stock input
        if (quantityField.getText() == null || quantityField.getText().isEmpty()) errorMessage += "Initial Stock Quantity is required.\n";
        if (lowThresholdField.getText().isEmpty() || criticalThresholdField.getText().isEmpty()) errorMessage += "Threshold values are required.\n";

        if (errorMessage.isEmpty()) return true;
        else {
            showAlert(Alert.AlertType.WARNING, "Invalid Fields", errorMessage);
            return false;
        }
    }

    private void setupNumericValidation(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                field.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }

    private void closeStage() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
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