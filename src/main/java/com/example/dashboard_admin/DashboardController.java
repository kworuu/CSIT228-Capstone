package com.example.dashboard_admin;

import com.example.auth.AuthService;
import com.example.dao.InventoryItemDao;
import com.example.dao.SupplyRequestDao;
import com.example.dao.TransactionDao;
import com.example.dashboard_admin.views.DeployItemController;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.example.model.Transaction;
import com.example.util.*;
import javafx.application.Platform;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardController {


    @FXML private TextField searchEvacCenter;
    @FXML private Button navInventory;
    @FXML private Button navMap;

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

    // container for logs
    @FXML private VBox containerTransactionLog;

    private final ObservableList<SupplyRequest> masterData = FXCollections.observableArrayList();

    // State tracking variables
    private final SupplyRequestDao requestDao = new SupplyRequestDao();
    private final InventoryItemDao inventoryDao = new InventoryItemDao();
    private final TransactionDao transactionDao = new TransactionDao();

    //Observable Property so listeners update items automatically
    private final StringProperty selectedStatusFilter = new SimpleStringProperty("ALL");


    @FXML
    public void initialize() {
        // Navigation
        navInventory.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/inventory.fxml", navInventory));
        navMap.setOnAction(event -> SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", navMap));

        // Tab switches
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
        refreshTransactionLogs();
    }

    private void setupTable() {
        colBrgy.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().barangay()));
        colItem.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().itemName()));
        colQuantity.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().quantity()).asObject());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        colDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().createdAt() != null ? cellData.getValue().createdAt().format(dtf) : ""));
        colNotes.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().notes()));
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().status().name()));


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
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    SupplyRequest request = getTableView().getItems().get(getIndex());


                    if (request.status() == com.example.model.SupplyRequestStatus.PENDING) {
                        actionBtn.setText("Deploy");
                        actionBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 6px 16px; -fx-font-weight: bold;");
                        actionBtn.setDisable(false);
                        setGraphic(actionBtn);
                    }
                    else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void loadData() {
        try {
            masterData.setAll(requestDao.findAllForAdmin());
            setupSearch();
            mainTable.refresh();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshStats() {
        try {
            if (lblTotalEvacValue != null) lblTotalEvacValue.setText(String.valueOf(masterData.size()));
            if (lblCriticalItem != null) lblCriticalItem.setText(String.valueOf(InventoryItemDao.getAdminCriticalCount()));
        } catch (SQLException e) {
            System.err.println("Error loading stats: " + e.getMessage());
        }
    }

    private void setupSearch() {
        if (searchEvacCenter == null || mainTable == null) return;
        SearchTableUtility.setupSearch(searchEvacCenter, mainTable, masterData, (request, query) -> {
            String activeFilter = selectedStatusFilter.get();
            if (!activeFilter.equals("ALL")) {
                if (activeFilter.equals("PENDING") && request.status() != SupplyRequestStatus.PENDING) return false;
                if (activeFilter.equals("APPROVED") && request.status() == SupplyRequestStatus.PENDING) return false;
            }
            if (query == null || query.trim().isEmpty()) return true;
            String lowerQuery = query.toLowerCase();
            return (request.barangay() != null && request.barangay().toLowerCase().contains(lowerQuery)) ||
                    (request.itemName() != null && request.itemName().toLowerCase().contains(lowerQuery));
        }, selectedStatusFilter);
    }

    private void handleFilterChange(String status, Button selectedButton) {
        selectedStatusFilter.set(status);
        List.of(filterAll, filterPending, filterApproved).forEach(btn -> btn.getStyleClass().remove("segment-active"));
        if (selectedButton != null) selectedButton.getStyleClass().add("segment-active");
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
                criticalItems.forEach(item -> CardAlertHelper.addCriticalAlertCard(alertsContainer, item));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleDeployClick(SupplyRequest request) {
        FXMLLoader loader = SceneHelper.showModalWithController("/com/example/dashboard_admin/modals/deploy-items.fxml", "Deploy Supplies", mainTable);
        if (loader != null) {
            DeployItemController controller = loader.getController();
            controller.setData(request.id(), request.itemId(), request.itemName(), request.userId(), request.barangay(), request.quantity());

            Parent root = loader.getRoot();
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setOnHiding(event -> Platform.runLater(this::handleRefresh));
            stage.show();
        }
    }

    @FXML private void handleRefresh() {
        loadData();
        refreshStats();
        refreshTransactionLogs();
    }

    @FXML private void handleLogout() {
        new AuthService().logout();
        Router.getInstance().navigate(Route.KIOSK);
    }

    private void refreshTransactionLogs() {
        if (containerTransactionLog == null) return;
        try {
            containerTransactionLog.getChildren().clear();
            containerTransactionLog.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-border-color: transparent;" +
                            "-fx-padding: 0px;"
            );

            containerTransactionLog.setSpacing(8);

            List<Transaction> transactions = transactionDao.findAll();

            if (transactions.isEmpty()) {
                showEmptyMessage();
            } else {
                for (Transaction t : transactions) {
                    String itemName = inventoryDao.findById(t.itemId())
                            .map(InventoryItem::name)
                            .orElse("Unknown Item");

                    ActivityLogHelper.addTransactionLogCard(
                            containerTransactionLog,
                            t,
                            itemName,
                            t.createdBy()
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showEmptyMessage() {
        Label empty = new Label("No transactions recorded.");
        empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        containerTransactionLog.getChildren().add(empty);
    }
}
