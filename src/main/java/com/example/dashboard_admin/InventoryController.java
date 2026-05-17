package com.example.dashboard_admin;

import com.example.dao.InventoryItemDao;
import com.example.dashboard_admin.views.EditItemController;
import com.example.model.InventoryItem;
import com.example.util.SceneHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class InventoryController {

    @FXML private TableView<InventoryItem> mainTable;
    @FXML private TableColumn<InventoryItem, String> colItemName;
    @FXML private TableColumn<InventoryItem, String> colCategory;
    @FXML private TableColumn<InventoryItem, String> colUnit;
    @FXML private TableColumn<InventoryItem, String> colQty;
    @FXML private TableColumn<InventoryItem, InventoryItem.StockStatus> colStatus;
    @FXML private TableColumn<InventoryItem, Void> colAction;

    @FXML private Button btnNewItem;
    @FXML private Button navEvacuations;
    @FXML private Button navMap;
    @FXML private TextField searchItemField;

    @FXML private Button filterAll;
    @FXML private Button filterCrit;
    @FXML private Button filterLow;

    private final ObservableList<InventoryItem> masterData = FXCollections.observableArrayList();
    private FilteredList<InventoryItem> filteredData;
    private final InventoryItemDao inventoryDao = new InventoryItemDao();

    private String currentFilterMode = "ALL";

    @FXML
    public void initialize() {
        filteredData = new FilteredList<>(masterData, p -> true);

        setupTableColumns();
        loadData();

        filterAll.setOnAction(e -> changeFilterMode("ALL", filterAll));
        filterCrit.setOnAction(e -> changeFilterMode("CRITICAL", filterCrit));
        filterLow.setOnAction(e -> changeFilterMode("LOW", filterLow));

        searchItemField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyCombinedFilter();
        });

        btnNewItem.setOnAction(event -> {
            SceneHelper.showModal("/com/example/dashboard_admin/modals/add-item.fxml", "Insert New Item", btnNewItem);
            loadData();
        });

        navEvacuations.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/evacuation.fxml", navEvacuations));
        navMap.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", navMap));
    }

    private void loadData() {
        try {
            List<InventoryItem> items = inventoryDao.findAll();
            masterData.setAll(items);
            applyCombinedFilter();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to load inventory data.");
        }
    }

    private void changeFilterMode(String mode, Button selectedButton) {
        this.currentFilterMode = mode;

        filterAll.getStyleClass().remove("segment-active");
        filterCrit.getStyleClass().remove("segment-active");
        filterLow.getStyleClass().remove("segment-active");

        selectedButton.getStyleClass().add("segment-active");
        applyCombinedFilter();
    }

    private void applyCombinedFilter() {
        String searchQuery = searchItemField.getText() == null ? "" : searchItemField.getText().toLowerCase().trim();

        filteredData.setPredicate(item -> {
            boolean matchesSearch = searchQuery.isEmpty() ||
                    (item.name() != null && item.name().toLowerCase().contains(searchQuery)) ||
                    (item.category() != null && item.category().toLowerCase().contains(searchQuery));

            if (!matchesSearch) return false;

            return switch (currentFilterMode) {
                case "CRITICAL" -> item.getStockStatus() == InventoryItem.StockStatus.CRITICAL;
                case "LOW"      -> item.getStockStatus() == InventoryItem.StockStatus.LOW;
                default         -> true;
            };
        });
    }

    private void setupTableColumns() {
        colItemName.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().name()));
        colCategory.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().category()));
        colUnit.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().unit()));
        colQty.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().stockQuantity())));

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
                    setText(status == InventoryItem.StockStatus.CRITICAL ? "CRITICAL" : status.toString());

                    getStyleClass().removeAll("status-ok", "status-low", "status-out-of-stock");
                    switch (status) {
                        case GREAT -> getStyleClass().add("status-ok");
                        case LOW -> getStyleClass().add("status-low");
                        case CRITICAL -> getStyleClass().add("status-out-of-stock");
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

                editBtn.setOnAction(event -> {
                    InventoryItem item = getTableRow().getItem();
                    handleEdit(item);
                });

                deleteBtn.setOnAction(event -> {
                    InventoryItem item = getTableRow().getItem();
                    handleDelete(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });

        mainTable.setItems(filteredData);
    }

    private void handleEdit(InventoryItem item) {
        if (item == null) return;

        FXMLLoader loader = SceneHelper.showModalWithController(
                "/com/example/dashboard_admin/modals/edit-inventory.fxml",
                "Update Item",
                mainTable
        );

        if (loader != null) {
            EditItemController controller = loader.getController();
            controller.setItemData(item);
            Parent root = loader.getRoot();
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setOnHiding(event -> loadData());
            stage.show();
        }
    }

    private void handleDelete(InventoryItem item) {
        if (item == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + item.name() + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    if (inventoryDao.deleteById(item.id())) {
                        loadData();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}