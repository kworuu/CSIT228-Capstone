package com.example.dashboard_admin.views;

import com.example.dao.InventoryItemDao;
import com.example.model.InventoryItem;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.sql.SQLException;

public class EditItemController {

    @FXML private TextField itemField;
    @FXML private TextField initialField; // Shows current stock
    @FXML private TextField updatedField; // User enters amount to ADD
    @FXML private Button btnCancel;
    @FXML private Button btnUpdated;

    private final InventoryItemDao inventoryDao = new InventoryItemDao();
    private InventoryItem currentItem;

    @FXML
    public void initialize() {
        btnCancel.setOnAction(event -> closeWindow());
        btnUpdated.setOnAction(event -> processUpdate());
    }

    public void setItemData(InventoryItem item) {
        this.currentItem = item;
        itemField.setText(item.name());
        initialField.setText(String.valueOf(item.stockQuantity()));
        initialField.setEditable(false); // User shouldn't manually edit "initial"
    }

    private void processUpdate() {
        try {

            int adjustment = Integer.parseInt(updatedField.getText().replace("+", "").trim());
            int newTotal = currentItem.stockQuantity() + adjustment;


            InventoryItem updatedItem = new InventoryItem(
                    currentItem.id(),
                    itemField.getText(),
                    currentItem.category(),
                    currentItem.unit(),
                    currentItem.criticalThreshold(),
                    currentItem.lowThreshold(),
                    newTotal,
                    currentItem.createdAt(),
                    currentItem.createdByUserId()
            );


            inventoryDao.update(updatedItem);
            closeWindow();

        } catch (NumberFormatException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}