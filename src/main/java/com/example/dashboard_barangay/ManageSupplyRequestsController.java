package com.example.dashboard_barangay;

import com.example.auth.SessionContext;
import com.example.dao.EvacuationCenterDao;
import com.example.dao.InventoryItemDao;
import com.example.dao.SupplyRequestDao;
import com.example.model.EvacuationCenter;
import com.example.model.InventoryItem;
import com.example.model.SupplyRequest;
import com.example.model.SupplyRequestStatus;
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

    @FXML private ComboBox<EvacuationCenter> comboBoxCenter;
    @FXML private ComboBox<InventoryItem> comboItems;
    @FXML private TextField fieldQuantity;
    @FXML private TextField fieldNotes;
    @FXML private Button btnSubmit;

    @FXML private TableView<SupplyRequestItem> tableStaging;
    @FXML private TableColumn<SupplyRequestItem, String> colStagingItem;
    @FXML private TableColumn<SupplyRequestItem, Integer> colStagingQty;
    @FXML private TableColumn<SupplyRequestItem, String> colStagingUnit;

    @FXML private TableView<SupplyRequest> tableHistory;
    @FXML private TableColumn<SupplyRequest, String> colHistoryDate;
    @FXML private TableColumn<SupplyRequest, String> colHistoryStatus;
    @FXML private TableColumn<SupplyRequest, String> colHistoryNotes;

    private final EvacuationCenterDao centerDao = new EvacuationCenterDao();
    private final InventoryItemDao inventoryDao = new InventoryItemDao();
    private final SupplyRequestDao requestDao = new SupplyRequestDao();

    private final ObservableList<SupplyRequestItem> stagingItems = FXCollections.observableArrayList();
    private final ObservableList<SupplyRequest> historyItems = FXCollections.observableArrayList();
    private Runnable onCloseCallback;

    @FXML
    public void initialize() {
        setupTables();
        loadEvacuationCenters();
        loadInventoryItems();
        loadRequestHistory();
    }

    public void setOnClose(Runnable callback) {
        this.onCloseCallback = callback;
    }

    private void setupTables() {
        tableStaging.setItems(stagingItems);
        colStagingItem.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colStagingQty.setCellValueFactory(new PropertyValueFactory<>("quantityRequested"));
        colStagingUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

        tableHistory.setItems(historyItems);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

        // NEW: Uses record accessors to prevent crashes
        colHistoryDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().createdAt() != null ? cellData.getValue().createdAt().format(dtf) : ""));

        colHistoryStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().status().displayLabel()));

        colHistoryNotes.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().notes()));
    }

    private void loadEvacuationCenters() {
        try {
            if (SessionContext.current() == null || SessionContext.current().getBarangay() == null) return;
            String brgyName = SessionContext.current().getBarangay().getName();

            comboBoxCenter.setItems(FXCollections.observableArrayList(centerDao.findByBarangay(brgyName)));
            comboBoxCenter.setConverter(new StringConverter<>() {
                @Override public String toString(EvacuationCenter center) {
                    return center == null ? "" : center.name();
                }
                @Override public EvacuationCenter fromString(String string) { return null; }
            });
        } catch (SQLException e) {
            System.err.println("Failed to load evacuation centers: " + e.getMessage());
        }
    }

    private void loadInventoryItems() {
        try {
            comboItems.setItems(FXCollections.observableArrayList(inventoryDao.findAll()));
            comboItems.setConverter(new StringConverter<>() {
                @Override public String toString(InventoryItem item) {
                    return item == null ? "" : item.name() + " (" + item.unit() + ")";
                }
                @Override public InventoryItem fromString(String string) { return null; }
            });
        } catch (SQLException e) {
            System.err.println("Failed to load inventory items: " + e.getMessage());
        }
    }

    private void loadRequestHistory() {
        if (SessionContext.current() == null || SessionContext.current().getBarangay() == null) return;
        try {
            String brgyName = SessionContext.current().getBarangay().getName();
            historyItems.setAll(requestDao.getRequestsByBarangay(brgyName));
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

            for (SupplyRequestItem item : stagingItems) {
                if (item.getItemId().equals(selected.id())) {
                    stagingItems.remove(item);
                    stagingItems.add(new SupplyRequestItem(selected.id(), item.getQuantityRequested() + qty, selected.name(), selected.unit()));
                    fieldQuantity.clear();
                    return;
                }
            }

            stagingItems.add(new SupplyRequestItem(selected.id(), qty, selected.name(), selected.unit()));
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
            Long userId = SessionContext.current().getUser().id();
            String brgyName = SessionContext.current().getBarangay().getName();

            // NEW: Save each staged item as an individual supply request natively
            for (SupplyRequestItem stagedItem : stagingItems) {
                SupplyRequest newRequest = new SupplyRequest(
                        0,
                        stagedItem.getItemId(),
                        stagedItem.getQuantityRequested(),
                        SupplyRequestStatus.PENDING,
                        brgyName,
                        userId,
                        fieldNotes.getText(),
                        null,
                        null
                );
                requestDao.saveRequest(newRequest);
            }

            stagingItems.clear();
            fieldNotes.clear();
            comboBoxCenter.getSelectionModel().clearSelection();
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