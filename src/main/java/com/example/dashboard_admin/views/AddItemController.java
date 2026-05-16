package com.example.dashboard_admin.views;


import com.example.dao.InventoryItemDao;
import com.example.model.InventoryItem;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

public class AddItemController {

    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField unitField;
    @FXML private TextField lowThresholdField;
    @FXML private TextField criticalThresholdField;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private final InventoryItemDao inventoryDao = new InventoryItemDao();

    @FXML
    public void initialize() {
        // Initialize Categories to match the inventory_items schema categories
        categoryCombo.getItems().addAll("food", "water", "non-food", "medical");

        // Use your utility to ensure thresholds only accept digits
        setupNumericValidation(lowThresholdField);
        setupNumericValidation(criticalThresholdField);

        // Action Handlers
        btnCancel.setOnAction(e -> closeStage());
        btnSave.setOnAction(e -> handleSave());
    }

    /**
     * Collects data from the UI and saves it using the InventoryItemDao.
     */
    private void handleSave() {
        if (!isInputValid()) {
            return;
        }

        try {
            // 1. Create a new model instance
            InventoryItem newItem = new InventoryItem();
            newItem.setName(nameField.getText().trim());
            newItem.setCategory(categoryCombo.getValue());
            newItem.setUnit(unitField.getText().trim());
            newItem.setLowThreshold(Integer.parseInt(lowThresholdField.getText()));
            newItem.setCriticalThreshold(Integer.parseInt(criticalThresholdField.getText()));

            // Initial stock is 0 for new items as per your schema
            newItem.setStockQuantity(0);

            // TODO: In a real scenario, get the current logged-in user's ID
            // newItem.setCreatedByUserId(currentUserId);

            // 2. Persist to database via DAO
            inventoryDao.save(newItem);

            // 3. Close modal on success
            closeStage();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save the item to the database.");
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please ensure thresholds are valid numbers.");
        }
    }

    /**
     * Validates that required fields are not empty.
     */
    private boolean isInputValid() {
        String errorMessage = "";

        if (nameField.getText() == null || nameField.getText().isEmpty()) {
            errorMessage += "Item Name is required.\n";
        }
        if (categoryCombo.getValue() == null) {
            errorMessage += "Category must be selected.\n";
        }
        if (unitField.getText() == null || unitField.getText().isEmpty()) {
            errorMessage += "Unit is required.\n";
        }
        if (lowThresholdField.getText().isEmpty() || criticalThresholdField.getText().isEmpty()) {
            errorMessage += "Threshold values are required.\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
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