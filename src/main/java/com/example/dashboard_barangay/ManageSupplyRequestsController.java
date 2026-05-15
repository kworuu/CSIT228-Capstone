package com.example.dashboard_barangay;

import com.example.auth.SessionContext;
import com.example.dao.InventoryItemDao;
import com.example.dao.SupplyRequestDao;
import com.example.model.InventoryItem;
import com.example.model.SupplyRequest;
import com.example.model.SupplyRequestItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class ManageSupplyRequestsController {

    @FXML private StackPane modalRoot;

    // Draft Request Fields
    @FXML private ComboBox<InventoryItem> comboItems;
    @FXML private TextField fieldQuantity;
    @FXML private TextField fieldNotes;
    @FXML private Button btnSubmit;

    // Staging Table
    @FXML private TableView<SupplyRequestItem> tableStaging;
    @FXML private TableColumn<SupplyRequestItem, String> colStagingItem;
    @FXML private TableColumn<SupplyRequestItem, Integer> colStagingQty;
    @FXML private TableColumn<SupplyRequestItem, String> colStagingUnit;

    // History Table
    @FXML private TableView<SupplyRequest> tableHistory;
    @FXML private TableColumn<SupplyRequest, String> colHistoryDate;
    @FXML private TableColumn<SupplyRequest, String> colHistoryStatus;
    @FXML private TableColumn<SupplyRequest, String> colHistoryNotes;

    private final InventoryItemDao inventoryDao = new InventoryItemDao();
    private final SupplyRequestDao requestDao = new SupplyRequestDao();

    private final ObservableList<SupplyRequestItem> stagingItems = FXCollections.observableArrayList();
    private final ObservableList<SupplyRequest> historyItems = FXCollections.observableArrayList();
    private Runnable onCloseCallback;

    @FXML
    public void initialize() {
        setupTables();
        loadInventoryItems();
        loadRequestHistory();
    }

    public void setOnClose(Runnable callback) {
        this.onCloseCallback = callback;
    }

    private void setupTables() {
        // Staging Table
        tableStaging.setItems(stagingItems);
        colStagingItem.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colStagingQty.setCellValueFactory(new PropertyValueFactory<>("quantityRequested"));
        colStagingUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

        // History Table
        tableHistory.setItems(historyItems);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

        colHistoryDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCreatedAt().format(dtf)));

        colHistoryStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus().displayLabel()));

        colHistoryNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));
    }

    private void loadInventoryItems() {
        try {
            comboItems.setItems(FXCollections.observableArrayList(inventoryDao.findAll()));
            comboItems.setConverter(new StringConverter<>() {
                @Override public String toString(InventoryItem item) {
                    return item == null ? "" : item.getName() + " (" + item.getUnit() + ")";
                }
                @Override public InventoryItem fromString(String string) { return null; } // Not needed
            });
        } catch (SQLException e) {
            System.err.println("Failed to load inventory items: " + e.getMessage());
        }
    }

    private void loadRequestHistory() {
        if (SessionContext.current() == null || SessionContext.current().getBarangay() == null) return;
        try {
            String brgyName = SessionContext.current().getBarangay().getName();
            historyItems.setAll(requestDao.findByBarangay(brgyName));
        } catch (SQLException e) {
            System.err.println("Failed to load history: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddLineItem() {
        InventoryItem selected = comboItems.getValue();
        String qtyText = fieldQuantity.getText();

        if (selected == null || qtyText.isBlank()) return;

        try {
            int qty = Integer.parseInt(qtyText.trim());
            if (qty <= 0) return;

            // Check if item is already in staging; if so, add to existing qty
            for (SupplyRequestItem item : stagingItems) {
                if (item.getItemId().equals(selected.getId())) {
                    // Quick replace to update table
                    stagingItems.remove(item);
                    stagingItems.add(new SupplyRequestItem(selected.getId(), item.getQuantityRequested() + qty, selected.getName(), selected.getUnit()));
                    fieldQuantity.clear();
                    return;
                }
            }

            stagingItems.add(new SupplyRequestItem(selected.getId(), qty, selected.getName(), selected.getUnit()));
            fieldQuantity.clear();
            comboItems.getSelectionModel().clearSelection();

        } catch (NumberFormatException e) {
            System.err.println("Invalid quantity");
        }
    }

    @FXML
    private void handleSubmitRequest() {
        if (stagingItems.isEmpty()) return;
        if (SessionContext.current() == null) return;

        try {
            SupplyRequest request = new SupplyRequest(
                    SessionContext.current().getBarangay().getName(),
                    SessionContext.current().getUser().getId(),
                    null, // Can be linked to a specific center later if you expand the UI
                    fieldNotes.getText()
            );

            // Move items from staging to request
            request.getItems().addAll(stagingItems);

            // Transact to DB
            requestDao.saveRequestWithItems(request);

            // Clean up UI and refresh history
            stagingItems.clear();
            fieldNotes.clear();
            loadRequestHistory();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void closeModal() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }
}