package com.example.dashboard_admin;

import com.example.dao.InventoryItemDao;
import com.example.model.InventoryItem;
import com.example.util.SceneHelper;
import com.example.util.SearchTableUtility;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.util.List;

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

    // 1. Rename to masterData to match your utility pattern
    private final ObservableList<InventoryItem> masterData = FXCollections.observableArrayList();

    // 2. Instantiate your DAO to fetch the data
    private final InventoryItemDao inventoryDao = new InventoryItemDao();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadData(); // Load the data from the database

        // Navigation & Actions
        btnNewItem.setOnAction(event -> {
            SceneHelper.showModal("/com/example/dashboard_admin/modals/add-item.fxml", "Insert New Item", btnNewItem);
            // Optional: Call loadData() here again if you want the table to refresh after the modal closes
            loadData();
        });

        // 3. Implement Search using your SearchTableUtility
        SearchTableUtility.setupSearch(
                searchItemField,
                mainTable,
                masterData,
                (item, query) ->
                        (item.getName() != null && item.getName().toLowerCase().contains(query)) ||
                                (item.getCategory() != null && item.getCategory().toLowerCase().contains(query))
        );

        // Scene Switching
        navEvacuations.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/evacuation.fxml", navEvacuations));
        navMap.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", navMap));
    }

    // 4. Method to pull data from your database into the table
    private void loadData() {
        try {
            List<InventoryItem> items = inventoryDao.findAll();
            masterData.setAll(items);
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to load inventory data.");
        }
    }

    private void setupTableColumns() {
        colItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

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
                    getStyleClass().removeAll("status-ok", "status-low", "status-critical");
                    switch (status) {
                        case OK -> getStyleClass().add("status-ok");
                        case LOW -> getStyleClass().add("status-low");
                        case CRITICAL -> getStyleClass().add("status-critical");
                    }
                }
            }
        });

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

        // The SearchTableUtility handles setting the items to the table automatically,
        // so we just initialize it with our masterData here just in case.
        mainTable.setItems(masterData);
    }
}