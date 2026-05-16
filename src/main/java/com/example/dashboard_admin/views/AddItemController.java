package com.example.dashboard_admin.views;

import com.example.dao.InventoryItemDao;
import com.example.model.InventoryItem;
import com.example.auth.SessionContext;
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
        categoryCombo.getItems().addAll("food", "water", "non-food", "medical");
        setupNumericValidation(lowThresholdField);
        setupNumericValidation(criticalThresholdField);

        btnCancel.setOnAction(e -> closeStage());
        btnSave.setOnAction(e -> handleSave());
    }

    private void handleSave() {
        if (!isInputValid()) {
            return;
        }

        try {
            SessionContext session = SessionContext.current();
            Long userId = (session != null && session.getUser() != null) ? session.getUser().id() : null;

            // NEW: Using the 8-parameter InventoryItem record constructor
            InventoryItem newItem = new InventoryItem(
                    0,
                    nameField.getText().trim(),
                    categoryCombo.getValue(),
                    unitField.getText().trim(),
                    Integer.parseInt(lowThresholdField.getText()),
                    0,
                    null,
                    userId
            );

            inventoryDao.save(newItem);
            closeStage();

        } catch (Exception e) { // Updated to catch broader exceptions due to DAO variations
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save the item to the database.");
        }
    }

    private boolean isInputValid() {
        String errorMessage = "";
        if (nameField.getText() == null || nameField.getText().isEmpty()) errorMessage += "Item Name is required.\n";
        if (categoryCombo.getValue() == null) errorMessage += "Category must be selected.\n";
        if (unitField.getText() == null || unitField.getText().isEmpty()) errorMessage += "Unit is required.\n";
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