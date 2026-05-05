package com.example.dashboard_admin.Views;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

public class AddItemController {

    @FXML
    private TextField itemIdField;

    @FXML
    private TextField itemNameField;

    @FXML
    private TextField quantityField;

    @FXML
    private Button insertButton;

    private static int nextId = 1;

    @FXML
    public void initialize() {
        // Generate initial ID
        updateIdField();

        // Listeners for validation
        itemNameField.textProperty().addListener((obs, oldVal, newVal) -> validateFields());
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Force numeric only
            if (!newVal.matches("\\d*")) {
                quantityField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            validateFields();
        });

        validateFields();
    }

    private void updateIdField() {
        itemIdField.setText(String.format("INV-2023-%04d", nextId));
    }

    private void validateFields() {
        boolean isNameEmpty = itemNameField.getText().trim().isEmpty();
        boolean isQtyEmpty = quantityField.getText().trim().isEmpty();
        boolean isQtyValid = false;

        try {
            if (!isQtyEmpty) {
                int qty = Integer.parseInt(quantityField.getText());
                isQtyValid = qty > 0;
            }
        } catch (NumberFormatException e) {
            isQtyValid = false;
        }

        insertButton.setDisable(isNameEmpty || isQtyEmpty || !isQtyValid);
    }

    @FXML
    void handleInsert(ActionEvent event) {
        System.out.println("Inserting Item: " + itemNameField.getText() + " (Qty: " + quantityField.getText() + ")");

        // Business logic would go here
        nextId++;

        // Close modal
        closeStage();
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) itemNameField.getScene().getWindow();
        stage.close();
    }
}
