package com.example.dashboard_admin;

import com.example.dao.EvacuationCenterDao;
import com.example.dao.InventoryItemDao;
import com.example.map_logic.MapHtmlProvider;
import com.example.model.EvacuationCenter;
import com.example.util.SearchTableUtility;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.example.util.SceneHelper;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.web.WebView;

import java.sql.SQLException;
import java.util.List;

public class DashboardController {

    @FXML
    private Button btnNewEvacCenter;
    @FXML
    private TextField searchEvacCenter;
    @FXML
    private Button navInventory;
    @FXML
    private Button navMap;
    @FXML
    private Button btnExpandMap;
    @FXML
    private WebView webviewMiniMap;
    @FXML
    private Button navActivity;



    // Cards
    @FXML
    private Label lblTotalEvacValue;
    @FXML
    private Label lblCriticalItem;


    // Table components
    @FXML private TableView<EvacuationCenter> mainTable;
    @FXML private TableColumn<EvacuationCenter, String> colEvacCenter;
    @FXML private TableColumn<EvacuationCenter, String> colBrgy;
    @FXML private TableColumn<EvacuationCenter, Integer> colAddress;
    @FXML private TableColumn<EvacuationCenter, String> colStatus;

    private final ObservableList<EvacuationCenter> masterData = FXCollections.observableArrayList();

    //Classes Declaration
    private final EvacuationCenterDao centerDao = new EvacuationCenterDao();

    @FXML
    public void initialize() {
        // Navigation Logic
        navInventory.setOnAction(event -> {
            SceneHelper.switchScene("/com/example/dashboard_admin/inventory.fxml", navInventory);
        });

        navMap.setOnAction(event -> {
            SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", navMap);
        });

        navActivity.setOnAction(event -> {
            SceneHelper.switchScene("/com/example/dashboard_admin/activity-log.fxml", navActivity);
        });

        // Minimap Logic
        webviewMiniMap.getEngine().setJavaScriptEnabled(true);
        webviewMiniMap.getEngine().loadContent(MapHtmlProvider.getMapHTML());

        btnExpandMap.setOnAction(event -> {
            SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", btnExpandMap);
        });

        // Modal Logic
        btnNewEvacCenter.setOnAction(event ->{
            SceneHelper.showModal("/com/example/dashboard_admin/modals/add-brgyReg.fxml", "Register Evacuation Center", btnNewEvacCenter);
        });

        // Data Initialization
        setupTable();
        loadData();
        refreshStats();
    }

    private void setupTable() {
        // Link columns to Model fields
        colEvacCenter.setCellValueFactory(new PropertyValueFactory<>("name"));
        colBrgy.setCellValueFactory(new PropertyValueFactory<>("barangay"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Add visual feedback to the Status column
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Match the color to the severity of the occupancy
                    switch (item) {
                        case "FULL" -> setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");   // Red
                        case "ALMOST FULL" -> setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;"); // Orange
                        default -> setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");     // Green
                    }
                }
            }
        });
    }

    private void loadData() {
        try {
            // 1. Fetch records from DB
            List<EvacuationCenter> evacCenterList = centerDao.findAll();

            // 2. Clear and update the class-level masterData
            // This is critical because SearchTableUtility watches this specific list
            masterData.setAll(evacCenterList);

            // 3. Initialize the search logic
            // We call this here to ensure the TableView is bound to the FilteredList
            searchEvac();

            System.out.println("DEBUG: Rows fetched and search initialized: " + masterData.size());
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Database connection error: Could not fetch evacuation centers.");
        }
    }

    private void refreshStats(){
        try{
            //EvacCenter
            int totEvacCenter = centerDao.getTotalCount();
            lblTotalEvacValue.setText(String.valueOf(totEvacCenter));

            //Critical Items
            int crit = InventoryItemDao.getAdminCriticalCount();
            lblCriticalItem.setText(String.valueOf(crit));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }



    private void searchEvac(){
        SearchTableUtility.setupSearch(
                searchEvacCenter,
                mainTable,
                masterData,
                (center, query) -> {
                    return center.getName().toLowerCase().contains(query) ||
                            center.getBarangay().toLowerCase().contains(query);
                }
        );
    }
}