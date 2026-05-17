package com.example.dashboard_admin;

import com.example.auth.AuthService;
import com.example.dao.InventoryItemDao;
import com.example.dao.SupplyRequestDao;
import com.example.dashboard_admin.views.DeployItemController;
import com.example.map_logic.MapHtmlProvider;
import com.example.model.InventoryItem;
import com.example.model.SupplyRequest;
import com.example.model.SupplyRequestStatus;
import com.example.util.CardAlertHelper;
import com.example.util.Route;
import com.example.util.Router;
import com.example.util.SceneHelper;
import com.example.util.SearchTableUtility;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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

    // Toggle Tab Filters
    @FXML private Button filterAll;
    @FXML private Button filterPending;
    @FXML private Button filterApproved;

    // Cards & Alerts
    @FXML private Label lblTotalEvacValue;
    @FXML private Label lblCriticalItem;
    @FXML private VBox alertsContainer;

    // --- TABLE COMPONENTS ---
    @FXML private TableView<SupplyRequest> mainTable;
    @FXML private TableColumn<SupplyRequest, String> colBrgy;
    @FXML private TableColumn<SupplyRequest, String> colItem;
    @FXML private TableColumn<SupplyRequest, Integer> colQuantity;
    @FXML private TableColumn<SupplyRequest, String> colDate;
    @FXML private TableColumn<SupplyRequest, String> colNotes;
    @FXML private TableColumn<SupplyRequest, String> colStatus;
    @FXML private TableColumn<SupplyRequest, String> colAction;

    private final ObservableList<SupplyRequest> masterData =
            FXCollections.observableArrayList();

    // State tracking variables
    private final SupplyRequestDao requestDao = new SupplyRequestDao();
    private final InventoryItemDao inventoryDao = new InventoryItemDao();

    // FIX: Changed to an Observable Property so listeners update items automatically
    private final StringProperty selectedStatusFilter = new SimpleStringProperty("ALL");


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

        // Action listeners for tab switches
        if (filterAll != null && filterPending != null && filterApproved != null) {
            filterAll.setOnAction(e -> handleFilterChange("ALL", filterAll));
            filterPending.setOnAction(e -> handleFilterChange("PENDING", filterPending));
            filterApproved.setOnAction(e -> handleFilterChange("APPROVED", filterApproved));
        }

        // Data Initialization
        setupTable();
        loadData();
        refreshStats();
        refreshAlerts(1L);
    }

    private void setupTable() {
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
                new SimpleStringProperty(cellData.getValue().status().name())); // Using .name() is safer for debugging case matches

        // 1. THE STATUS PILL UPGRADE
        // Turns boring text into a beautifully colored, rounded badge
        colStatus.setCellFactory(column -> new TableCell<>() {
            private final Label badge = new Label();
            {
                // Base styling for the pill
                badge.setStyle("-fx-padding: 4px 12px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px;");
            }
            
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    badge.setText(status.toUpperCase());
                    
                    // Color code based on the status!
                    if (status.equalsIgnoreCase("Pending")) {
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #fef08a; -fx-text-fill: #854d0e;"); // Warning Yellow
                    } else if (status.equalsIgnoreCase("Approved") || status.equalsIgnoreCase("Dispatched")) {
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #bbf7d0; -fx-text-fill: #166534;"); // Success Green
                    } else {
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #e2e8f0; -fx-text-fill: #475569;"); // Default Gray
                    }
                    setGraphic(badge);
                }
            }
        });

        // 2. THE ACTION BUTTON UPGRADE
        // Injects a clickable "Deploy" button straight into the table row
        colAction.setCellFactory(column -> new TableCell<>() {
            private final Button actionBtn = new Button();
            {
                actionBtn.setOnAction(event -> {
                    SupplyRequest request = getTableView().getItems().get(getIndex());
                    handleDeployClick(request);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    SupplyRequest request = getTableView().getItems().get(getIndex());
                    
                    // If it needs attention, show a bright primary button
                    if (request.status() == com.example.model.SupplyRequestStatus.PENDING) {
                        actionBtn.setText("Deploy");
                        actionBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 6px 16px; -fx-font-weight: bold;");
                        actionBtn.setDisable(false);
                        setGraphic(actionBtn);
                    } 
                    // If it's already deployed, hide the button
                    else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void loadData() {
        try {
            List<SupplyRequest> requestsList = requestDao.findAllForAdmin();
            masterData.setAll(requestsList);

            // Only mount the layout filters if they aren't bound yet
            setupSearch();
            mainTable.refresh();
        } catch (SQLException e) {
            e.printStackTrace();
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

        // Use the newly upgraded utility overload method
        SearchTableUtility.setupSearch(
                searchEvacCenter,
                mainTable,
                masterData,
                (request, query) -> {
                    // --- STAGE 1: Tab Filter Logic ---
                    String activeFilter = selectedStatusFilter.get();
                    if (!activeFilter.equals("ALL")) {
                        SupplyRequestStatus currentStatus = request.status();

                        if (activeFilter.equals("PENDING") && currentStatus != SupplyRequestStatus.PENDING) {
                            return false;
                        }

                        if (activeFilter.equals("APPROVED") && currentStatus == SupplyRequestStatus.PENDING) {
                            return false;
                        }
                    }

                    // --- STAGE 2: Text Box Query Logic ---
                    if (query == null || query.trim().isEmpty()) {
                        return true; // Pass item if text query is empty (Stage 1 controls visibility)
                    }

                    return (request.barangay() != null && request.barangay().toLowerCase().contains(query)) ||
                            (request.notes() != null && request.notes().toLowerCase().contains(query)) ||
                            (request.itemName() != null && request.itemName().toLowerCase().contains(query));
                },
                selectedStatusFilter // <-- Pass the status tracking property here!
        );
    }

    private void handleFilterChange(String status, Button selectedButton) {
        this.selectedStatusFilter.set(status);

        if (filterAll != null && filterPending != null && filterApproved != null) {
            filterAll.getStyleClass().remove("segment-active");
            filterPending.getStyleClass().remove("segment-active");
            filterApproved.getStyleClass().remove("segment-active");
        }

        if (selectedButton != null) {
            selectedButton.getStyleClass().add("segment-active");
        }
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

    private void handleDeployClick(SupplyRequest request) {
        FXMLLoader loader = SceneHelper.showModalWithController(
                "/com/example/dashboard_admin/modals/deploy-items.fxml",
                "Deploy Supplies",
                mainTable
        );

        if (loader != null) {
            DeployItemController controller = loader.getController();
            controller.setData(
                    request.id(),
                    request.itemId(),
                    request.itemName(),
                    request.userId(),
                    request.barangay(),
                    request.quantity()
            );

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
        new AuthService().logout();
        Router.getInstance().navigate(Route.KIOSK);
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
