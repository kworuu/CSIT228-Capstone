package com.example.dashboard_admin;

import com.example.dao.InventoryItemDao;
import com.example.model.InventoryItem;
import com.example.util.SceneHelper;
import com.example.util.SearchTableUtility;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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

    private final ObservableList<InventoryItem> masterData = FXCollections.observableArrayList();
    private final InventoryItemDao inventoryDao = new InventoryItemDao();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadData();

        btnNewItem.setOnAction(event -> {
            SceneHelper.showModal("/com/example/dashboard_admin/modals/add-item.fxml", "Insert New Item", btnNewItem);
            loadData();
        });

        SearchTableUtility.setupSearch(
                searchItemField,
                mainTable,
                masterData,
                (item, query) ->
                        (item.name() != null && item.name().toLowerCase().contains(query)) ||
                                (item.category() != null && item.category().toLowerCase().contains(query))
        );

        navEvacuations.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/evacuation.fxml", navEvacuations));
        navMap.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", navMap));
    }

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
        // FIXED: Replaced PropertyValueFactory with Record accessors
        colItemName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().name()));
        colCategory.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().category()));
        colUnit.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().unit()));

        colStatus.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getStockStatus()));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(InventoryItem.StockStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(status.toString());
                    getStyleClass().removeAll("status-ok", "status-low", "status-out-of-stock", "status-critical");
                    switch (status) {
                        case OK -> getStyleClass().add("status-ok");
                        case LOW_STOCK -> getStyleClass().add("status-low");
                        case CRITICAL -> getStyleClass().add("status-critical");
                        case OUT_OF_STOCK -> getStyleClass().add("status-out-of-stock");
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

        mainTable.setItems(masterData);
    }
}