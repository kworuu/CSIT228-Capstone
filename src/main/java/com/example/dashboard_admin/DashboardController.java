package com.example.dashboard_admin;

import com.example.dao.InventoryItemDao;
import com.example.dao.SupplyRequestDao;
import com.example.map_logic.MapHtmlProvider;
import com.example.model.InventoryItem;
import com.example.model.SupplyRequest;
import com.example.util.CardAlertHelper;
import com.example.util.SearchTableUtility;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.example.util.SceneHelper;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardController {

    @FXML private Button btnNewEvacCenter;
    @FXML private TextField searchEvacCenter; // You can rename this to searchRequest in your FXML
    @FXML private Button navInventory;
    @FXML private Button navMap;
    @FXML private Button btnExpandMap;
    @FXML private WebView webviewMiniMap;
    @FXML private Button navActivity;

    // Cards & Alerts
    @FXML private Label lblTotalEvacValue; // Can be repurposed to show "Total Requests"
    @FXML private Label lblCriticalItem;
    @FXML private VBox alertsContainer;

    // --- UPDATED TABLE COMPONENTS ---
    @FXML private TableView<SupplyRequest> mainTable;
    @FXML private TableColumn<SupplyRequest, String> colBrgy;
    @FXML private TableColumn<SupplyRequest, String> colDate;
    @FXML private TableColumn<SupplyRequest, String> colNotes;
    @FXML private TableColumn<SupplyRequest, String> colStatus;

    private final ObservableList<SupplyRequest> masterData = FXCollections.observableArrayList();

    // --- UPDATED DEPENDENCIES ---
    private final SupplyRequestDao requestDao = new SupplyRequestDao();
    private final InventoryItemDao inventoryDao = new InventoryItemDao();

    @FXML
    public void initialize() {
        // Navigation Logic
        navInventory.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/inventory.fxml", navInventory));
        navMap.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", navMap));

        // Minimap Logic
        if (webviewMiniMap != null) {
            webviewMiniMap.getEngine().setJavaScriptEnabled(true);
            webviewMiniMap.getEngine().loadContent(MapHtmlProvider.getMapHTML());
        }

        if (btnExpandMap != null) {
            btnExpandMap.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", btnExpandMap));
        }

        // Safe registration for Modals
        if (btnNewEvacCenter != null) {
            btnNewEvacCenter.setOnAction(event -> {
                SceneHelper.showModal("/com/example/dashboard_admin/modals/add-brgyReg.fxml", "Register Evacuation Center", btnNewEvacCenter);
            });
        }

        // Data Initialization
        setupTable();
        loadData();
        refreshStats();
        refreshAlerts(1L); // Adjust hardcoded ID as necessary for session management
    }

    private void setupTable() {
        // Map columns to SupplyRequest properties
        colBrgy.setCellValueFactory(new PropertyValueFactory<>("requestingBarangay"));

        // Format the date so it looks clean (e.g., "May 16, 2026 14:30")
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        colDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCreatedAt().format(dtf)));

        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        // Use the displayLabel from your SupplyRequestStatus enum
        colStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus().displayLabel()));

        mainTable.setItems(masterData);
    }

    private void loadData() {
        try {
            // Fetch all requests across the system for the Admin
            List<SupplyRequest> requestsList = requestDao.findAll();
            masterData.setAll(requestsList);
            setupSearch();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Database connection error: Could not fetch supply requests.");
        }
    }

    private void refreshStats() {
        try {
            // Repurpose the total card to show the number of requests
            int totalRequests = masterData.size();
            if (lblTotalEvacValue != null) {
                lblTotalEvacValue.setText(String.valueOf(totalRequests));
            }

            int crit = InventoryItemDao.getAdminCriticalCount();
            if (lblCriticalItem != null) {
                lblCriticalItem.setText(String.valueOf(crit));
            }
        } catch (SQLException e) {
            System.err.println("Error loading status metrics: " + e.getMessage());
        }
    }

    private void setupSearch() {
        if (searchEvacCenter == null || mainTable == null) return;

        // Search through the Requests by Barangay Name, Notes, or Status
        SearchTableUtility.setupSearch(
                searchEvacCenter,
                mainTable,
                masterData,
                (request, query) ->
                        (request.getRequestingBarangay() != null && request.getRequestingBarangay().toLowerCase().contains(query)) ||
                                (request.getNotes() != null && request.getNotes().toLowerCase().contains(query)) ||
                                (request.getStatus() != null && request.getStatus().name().toLowerCase().contains(query))
        );
    }

    public void refreshAlerts(Long userId) {
        if (alertsContainer == null) return;

        try {
            alertsContainer.getChildren().clear();
            List<InventoryItem> criticalItems = inventoryDao.findCriticalItemsByUser(userId);

            if (criticalItems.isEmpty()) {
                Label emptyLabel = new Label("All stocks are sufficient.");
                emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
                alertsContainer.getChildren().add(emptyLabel);
            } else {
                for (InventoryItem item : criticalItems) {
                    CardAlertHelper.addCriticalAlertCard(alertsContainer, item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}