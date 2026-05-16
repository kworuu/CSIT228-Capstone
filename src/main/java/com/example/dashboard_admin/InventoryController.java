package com.example.dashboard_admin;

import com.example.model.InventoryItem;
import com.example.util.SceneHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

public class InventoryController {

    @FXML private TableView<InventoryItem> mainTable;
    @FXML private TableColumn<InventoryItem, String> colItemName;
    @FXML private TableColumn<InventoryItem, String> colCategory;
    @FXML private TableColumn<InventoryItem, String> colUnit;
    @FXML private TableColumn<InventoryItem, InventoryItem.StockStatus> colStatus;
    @FXML private TableColumn<InventoryItem, Void> colAction;

    @FXML private Button btnNewItem;
    @FXML private Button navEvacuations;
    @FXML private Button navMap;
    @FXML private Button navActivity;
    @FXML private TextField searchItemField;

    private final ObservableList<InventoryItem> inventoryData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();

        // Navigation & Actions
        btnNewItem.setOnAction(event -> {
            SceneHelper.showModal("/com/example/dashboard_admin/modals/add-item.fxml", "Insert New Item", btnNewItem);
        });

        // Basic Search Filter Example
        searchItemField.textProperty().addListener((obs, oldVal, newVal) -> {
            // In a real app, use a FilteredList here
            System.out.println("Searching for: " + newVal);
        });

        // Scene Switching
        navEvacuations.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/evacuation.fxml", navEvacuations));
        navMap.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", navMap));
        navActivity.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/activity-log.fxml", navActivity));
    }

    private void setupTableColumns() {
        // Map columns to InventoryItem properties
        colItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

        // Custom rendering for the Status column
        colStatus.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getStatus()));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(InventoryItem.StockStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(status.toString());
                    // Apply CSS classes based on status
                    getStyleClass().removeAll("status-ok", "status-low", "status-critical");
                    switch (status) {
                        case OK -> getStyleClass().add("status-ok");
                        case LOW -> getStyleClass().add("status-low");
                        case CRITICAL -> getStyleClass().add("status-critical");
                    }
                }
            }
        });

        // Add Action Buttons (Edit/Delete)
        colAction.setCellFactory(column -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox container = new HBox(8, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-action-edit");
                deleteBtn.getStyleClass().add("btn-action-delete");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });

        mainTable.setItems(inventoryData);
    }


    private void addItem(){

    }
}