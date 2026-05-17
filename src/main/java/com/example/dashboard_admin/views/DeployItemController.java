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
    private long currentRequestId; // To update the status of the specific request
    private Long currentBrgyUserId; // Based on your DAO, barangay is the requesting_user_id

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
    }

    private void handleDeploy(ActionEvent event) {
        try {
            int amountToDeploy = Integer.parseInt(qtyField.getText());

            // 1. Fetch current inventory to check if we have enough stock
            InventoryItem item = inventoryDao.findById(currentItemId)
                    .orElseThrow(() -> new SQLException("Item not found in inventory."));

            if (item.stockQuantity() < amountToDeploy) {
                showError("Stock Error", "Not enough items in warehouse. Available: " + item.stockQuantity());
                return;
            }

            // 2. Create Transaction Record matching your new SQL model schema
            // Structure: id, direction, itemId, quantity, destinationId, createdBy, createdAt, notes
            Transaction t = new Transaction(
                    0,                      // id (auto-incremented)
                    "outflow",              // direction
                    currentItemId,          // item_id
                    amountToDeploy,         // quantity
                    currentBrgyUserId,      // destination_id (linked to users.id)
                    deployField.getText(),                     // created_by (1L for Admin session ID)
                    null,                   // created_at (null lets MySQL handle CURRENT_TIMESTAMP)
                    notesField.getText()    // notes
            );
            transactionDao.recordTransaction(t);

            // 3. Update Inventory Stock (Subtract)
            InventoryItem updatedItem = new InventoryItem(
                    item.id(), item.name(), item.category(), item.unit(),
                    item.criticalThreshold(), item.lowThreshold(),
                    item.stockQuantity() - amountToDeploy, // New Quantity
                    item.createdAt(), item.createdByUserId()
            );
            inventoryDao.update(updatedItem);

            // 4. Update the Supply Request Status to APPROVED
            requestDao.updateStatus(currentRequestId, SupplyRequestStatus.APPROVED);

            showSuccess("Success", "Supplies deployed and stock updated.");
            SceneHelper.closeWindow(event);

        } catch (NumberFormatException e) {
            showError("Input Error", "Please enter a valid number for quantity.");
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