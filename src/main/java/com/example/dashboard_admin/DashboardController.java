package com.example.dashboard_admin;

import com.example.dao.InventoryItemDao;
import com.example.dao.SupplyRequestDao;
import com.example.dashboard_admin.views.DeployItemController;
import com.example.map_logic.MapHtmlProvider;
import com.example.model.InventoryItem;
import com.example.model.SupplyRequest;
import com.example.model.SupplyRequestStatus;
import com.example.util.CardAlertHelper;
import com.example.util.SceneHelper;
import com.example.util.SearchTableUtility;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
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

    // --- UPDATED TABLE COMPONENTS ---
    @FXML private TableView<SupplyRequest> mainTable;
    @FXML private TableColumn<SupplyRequest, String> colBrgy;
    @FXML private TableColumn<SupplyRequest, String> colItem;
    @FXML private TableColumn<SupplyRequest, Integer> colQuantity;
    @FXML private TableColumn<SupplyRequest, String> colDate;
    @FXML private TableColumn<SupplyRequest, String> colNotes;
    @FXML private TableColumn<SupplyRequest, String> colStatus;
    @FXML private TableColumn<SupplyRequest, Void> colAction;

    private final ObservableList<SupplyRequest> masterData =
            FXCollections.observableArrayList();

    // --- UPDATED DEPENDENCIES ---
    private final SupplyRequestDao requestDao = new SupplyRequestDao();
    private final InventoryItemDao inventoryDao = new InventoryItemDao();


    @FXML
    public void initialize() {
        // Navigation
        navInventory.setOnAction(event ->
                SceneHelper.switchScene("/com/example/dashboard_admin/inventory.fxml", navInventory));
        navMap.setOnAction(event ->
                SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", navMap));

        // Minimap
        if (webviewMiniMap != null) {
            webviewMiniMap.getEngine().setJavaScriptEnabled(true);
            webviewMiniMap.getEngine().loadContent(MapHtmlProvider.getMapHTML());
        }

        if (btnExpandMap != null) {
            btnExpandMap.setOnAction(event ->
                    SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", btnExpandMap));
        }

        // Safe registration for Modals
        if (btnNewEvacCenter != null) {
            btnNewEvacCenter.setOnAction(event ->
                    SceneHelper.showModal(
                            "/com/example/dashboard_admin/modals/add-brgyReg.fxml",
                            "Register Evacuation Center",
                            btnNewEvacCenter));
        }

        // Data Initialization
        setupTable();
        loadData();
        refreshStats();
        refreshAlerts(1L); // Adjust hardcoded ID as necessary for session management
    }

    private void setupTable() {
        // NEW: Replaced PropertyValueFactory with safe lambda accessors for records
        colBrgy.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().barangay()));

        colItem.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().itemName()));

        colQuantity.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().quantity()).asObject());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        colDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().createdAt() != null
                        ? cellData.getValue().createdAt().format(dtf)
                        : ""));

        colNotes.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().notes()));

        colStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().status().displayLabel()));

        setupActionColumn();
        mainTable.setItems(masterData);
    }

    /**
     * Installs a custom TableCell rendering a Deploy button on pending rows.
     * Already-processed rows show no button.
     */
    private void setupActionColumn() {
        if (colAction == null) return;

        colAction.setCellFactory(column -> new TableCell<SupplyRequest, Void>() {
            private final Button actionBtn = new Button("Deploy");

            {
                actionBtn.getStyleClass().add("btn-primary");
                actionBtn.setMaxWidth(Double.MAX_VALUE);
                actionBtn.setOnAction(event -> {
                    SupplyRequest rowData = getTableView().getItems().get(getIndex());
                    handleDeployClick(rowData);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                SupplyRequest request = getTableView().getItems().get(getIndex());

                if (request.status() == SupplyRequestStatus.PENDING) {
                    setGraphic(actionBtn);
                } else {
                    setGraphic(null);
                }
            }
        });
    }

    private void loadData() {
        try {
            // Fetch all requests across the system for the Admin
            List<SupplyRequest> requestsList = requestDao.findAllForAdmin();
            masterData.setAll(requestsList);
            setupSearch();
            mainTable.refresh();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Database connection error: Could not fetch supply requests.");
        }
    }

    private void refreshStats() {
        try {
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

        // NEW: Used record accessor methods without "get"
        SearchTableUtility.setupSearch(
                searchEvacCenter,
                mainTable,
                masterData,
                (request, query) ->
                        (request.barangay() != null && request.barangay().toLowerCase().contains(query)) ||
                                (request.notes() != null && request.notes().toLowerCase().contains(query)) ||
                                (request.status() != null && request.status().name().toLowerCase().contains(query))
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

    /**
     * Called when admin clicks Deploy on a pending request.
     * Shows confirmation, updates DB status to FULFILLED, refreshes table.
     */
    private void handleDeployClick(SupplyRequest request) {
        // 1. Open the modal and get the loader back
        FXMLLoader loader = SceneHelper.showModalWithController(
                "/com/example/dashboard_admin/modals/deploy-items.fxml",
                "Deploy Supplies",
                mainTable
        );

        if (loader != null) {
            // 2. Get the controller from the loader
            DeployItemController controller = loader.getController();

            // 3. Pass your data
            controller.setData(
                    request.id(),
                    request.itemId(),
                    request.itemName(),
                    request.userId(), // Barangay User ID
                    request.barangay(),
                    request.quantity()
            );

            // 4. Refresh data when the modal is closed
            Parent root = loader.getRoot();
            Stage stage = (Stage) root.getScene().getWindow();

            stage.setOnHiding(event -> {
                javafx.application.Platform.runLater(() -> {
                    loadData();
                    refreshStats();
                });
            });

            stage.show();
        }
    }


    @FXML
    private void handleRefresh() {
        loadData();
        refreshStats();
    }

    @FXML
    private void handleLogout() {
        // Implement logout logic
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}