package com.example.dashboard_admin;

import com.example.dao.EvacuationCenterDao;
import com.example.dao.InventoryItemDao;
import com.example.map_logic.MapHtmlProvider;
import com.example.model.EvacuationCenter;
import com.example.model.InventoryItem;
import com.example.util.CardAlertHelper;
import com.example.util.SearchTableUtility;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.example.util.SceneHelper;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.sql.SQLException;
import java.util.List;

public class DashboardController {

    @FXML private Button btnNewEvacCenter;
    @FXML private TextField searchEvacCenter;
    @FXML private Button navInventory;
    @FXML private Button navMap;
    @FXML private Button btnExpandMap;
    @FXML private WebView webviewMiniMap;
    @FXML private Button navActivity;

    // Cards & Alerts
    @FXML private Label lblTotalEvacValue;
    @FXML private Label lblCriticalItem;
    @FXML private VBox alertsContainer;

    // Table components - Changed colAddress to String to match standard addresses
    @FXML private TableView<EvacuationCenter> mainTable;
    @FXML private TableColumn<EvacuationCenter, String> colEvacCenter;
    @FXML private TableColumn<EvacuationCenter, String> colBrgy;
    @FXML private TableColumn<EvacuationCenter, String> colAddress;
    @FXML private TableColumn<EvacuationCenter, String> colStatus;

    private final ObservableList<EvacuationCenter> masterData = FXCollections.observableArrayList();

    // Dependencies
    private final EvacuationCenterDao centerDao = new EvacuationCenterDao();
    private final InventoryItemDao inventoryDao = new InventoryItemDao();

    @FXML
    public void initialize() {
        // Navigation Logic
        navInventory.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/inventory.fxml", navInventory));
        navMap.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", navMap));
        navActivity.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/activity-log.fxml", navActivity));

        // Minimap Logic
        if (webviewMiniMap != null) {
            webviewMiniMap.getEngine().setJavaScriptEnabled(true);
            webviewMiniMap.getEngine().loadContent(MapHtmlProvider.getMapHTML());
        }

        if (btnExpandMap != null) {
            btnExpandMap.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", btnExpandMap));
        }

        // Safe registration for the Modal Button to handle multi-view loading gracefully
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
        colEvacCenter.setCellValueFactory(new PropertyValueFactory<>("name"));
        colBrgy.setCellValueFactory(new PropertyValueFactory<>("barangay"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item.toUpperCase()) {
                        case "FULL" -> setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                        case "ALMOST FULL" -> setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                        default -> setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void loadData() {
        try {
            List<EvacuationCenter> evacCenterList = centerDao.findAll();
            masterData.setAll(evacCenterList);
            searchEvac();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Database connection error: Could not fetch evacuation centers.");
        }
    }

    private void refreshStats() {
        try {
            int totEvacCenter = centerDao.getTotalCount();
            if (lblTotalEvacValue != null) {
                lblTotalEvacValue.setText(String.valueOf(totEvacCenter));
            }

            // Corrected static reference to match how your architecture access counts
            int crit = InventoryItemDao.getAdminCriticalCount();
            if (lblCriticalItem != null) {
                lblCriticalItem.setText(String.valueOf(crit));
            }
        } catch (SQLException e) {
            System.err.println("Error loading status metrics: " + e.getMessage());
        }
    }

    private void searchEvac() {
        if (searchEvacCenter == null || mainTable == null) return;

        SearchTableUtility.setupSearch(
                searchEvacCenter,
                mainTable,
                masterData,
                (center, query) -> center.getName().toLowerCase().contains(query) ||
                        center.getBarangay().toLowerCase().contains(query)
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