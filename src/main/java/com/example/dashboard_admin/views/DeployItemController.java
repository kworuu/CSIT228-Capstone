package com.example.dashboard_admin.views;

import com.example.dao.InventoryItemDao;
import com.example.dao.SupplyRequestDao;
import com.example.dao.TransactionDao;
import com.example.model.InventoryItem;
import com.example.model.SupplyRequestStatus;
import com.example.model.Transaction;
import com.example.util.SceneHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class DeployItemController {

    @FXML private TextField itemNameField;
    @FXML private TextField brgyField;
    @FXML private TextField qtyField;
    @FXML private TextField deployField;
    @FXML private TextField notesField;

    @FXML private Button btnCancel;
    @FXML private Button btnDeploy;

    // DAOs
    private final TransactionDao transactionDao = new TransactionDao();
    private final InventoryItemDao inventoryDao = new InventoryItemDao();
    private final SupplyRequestDao requestDao = new SupplyRequestDao();

    private long currentItemId;
    private long currentRequestId;
    private Long currentBrgyUserId;

    @FXML
    public void initialize() {
        btnCancel.setOnAction(event -> handleCancel(event));
        btnDeploy.setOnAction(event -> handleDeploy(event));
    }

    /**
     * Call this when opening the modal from the Admin Request Table
     */
    public void setData(long requestId, long itemId, String itemName, Long brgyUserId, String brgyName, int requestedQty) {
        this.currentRequestId = requestId;
        this.currentItemId = itemId;
        this.currentBrgyUserId = brgyUserId;

        itemNameField.setText(itemName);
        brgyField.setText(brgyName);
        qtyField.setText(String.valueOf(requestedQty));

        // 1. Lock fields from user modification
        itemNameField.setEditable(false);
        brgyField.setEditable(false);
        qtyField.setEditable(false);

        // Inject custom style: light gray background (grayed out) with your green border accent
        String uneditableStyle = "-fx-background-color: #ecfdf5; " + // Light grayed-out background
//                "-fx-border-color: #064e3b; " +     // Keeps your signature Emergency Green border
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; " +
                "-fx-text-fill: #94a3b8; " +        // Muted gray text color matching "Deploy By"
                "-fx-opacity: 1.0;";                // Forces JavaFX to show our exact gray/green specs cleanly

        itemNameField.setStyle(uneditableStyle);
        brgyField.setStyle(uneditableStyle);
        qtyField.setStyle(uneditableStyle);
    }

    private void handleDeploy(ActionEvent event) {
        try {
            // FIXED: Read from deployField (user input) instead of qtyField (read-only reference value)
            String deployText = deployField.getText().trim();
            if (deployText.isEmpty()) {
                showError("Input Error", "Please specify the quantity you want to deploy.");
                return;
            }

            int amountToDeploy = Integer.parseInt(deployText);

            if (amountToDeploy <= 0) {
                showError("Input Error", "Deployment quantity must be greater than zero.");
                return;
            }

            // 1. Fetch current inventory to check if we have enough stock
            InventoryItem item = inventoryDao.findById(currentItemId)
                    .orElseThrow(() -> new SQLException("Item not found in inventory."));

            if (item.stockQuantity() < amountToDeploy) {
                showError("Stock Error", "Not enough items in warehouse. Available: " + item.stockQuantity());
                return;
            }

            // 2. Create Transaction Record matching your new SQL model schema
            // TODO: If deployField was intended for the quantity, make sure 'createdBy' passes the actual current admin's identifier/name string (e.g., "Admin")
            Transaction t = new Transaction(
                    0,
                    "outflow",
                    currentItemId,
                    amountToDeploy,
                    currentBrgyUserId,
                    "Admin",                // Hardcoded session placeholder or resolved name string
                    null,
                    notesField.getText()
            );
            transactionDao.recordTransaction(t);

            // 3. Update Inventory Stock (Subtract)
            InventoryItem updatedItem = new InventoryItem(
                    item.id(), item.name(), item.category(), item.unit(),
                    item.criticalThreshold(), item.lowThreshold(),
                    item.stockQuantity() - amountToDeploy,
                    item.createdAt(), item.createdByUserId()
            );
            inventoryDao.update(updatedItem);

            // 4. Update the Supply Request Status to APPROVED
            requestDao.updateStatus(currentRequestId, SupplyRequestStatus.APPROVED);

            showSuccess("Success", "Supplies deployed and stock updated.");
            SceneHelper.closeWindow(event);

        } catch (NumberFormatException e) {
            showError("Input Error", "Please enter a valid whole number for deployment quantity.");
        } catch (IllegalArgumentException e) {
            showError("Validation Error", e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database Error", e.getMessage());
        }
    }

    private void showSuccess(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void handleCancel(ActionEvent event) {
        SceneHelper.closeWindow(event);
    }
}