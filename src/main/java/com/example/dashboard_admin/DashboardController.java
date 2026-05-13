package com.example.dashboard_admin;

import com.example.dao.EvacuationCenterDao;
import com.example.map_logic.MapHtmlProvider;
import com.example.model.EvacuationCenter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.example.dashboard_admin.helper_classes.SceneHelper;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.web.WebView;

import java.sql.SQLException;
import java.util.List;

public class DashboardController {

    @FXML
    private Button btnNewEvacCenter;
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

    // Table components
    @FXML private TableView<EvacuationCenter> mainTable;
    @FXML private TableColumn<EvacuationCenter, String> colEvacCenter;
    @FXML private TableColumn<EvacuationCenter, String> colBrgy;
    @FXML private TableColumn<EvacuationCenter, Integer> colPopulation;
    @FXML private TableColumn<EvacuationCenter, String> colStatus;

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

        // Data Initialization
        setupTable();
        loadData();
        refreshStats();
    }

    private void setupTable() {
        // Link columns to Model fields
        colEvacCenter.setCellValueFactory(new PropertyValueFactory<>("name"));
        colBrgy.setCellValueFactory(new PropertyValueFactory<>("barangay"));
        colPopulation.setCellValueFactory(new PropertyValueFactory<>("currentOccupancy"));
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
            EvacuationCenterDao evacDao = new EvacuationCenterDao();

            // Fetch records from DB
            List<EvacuationCenter> evacCenterList = evacDao.findAll();
            System.out.println("DEBUG: Rows fetched: " + evacCenterList.size()); // Check your console!

            // Wrap in ObservableList for JavaFX
            ObservableList<EvacuationCenter> data = FXCollections.observableArrayList(evacCenterList);

            // Populate Table
            mainTable.setItems(data);

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Database connection error: Could not fetch evacuation centers.");
        }
    }

    private void refreshStats(){
        try{
            int totEvacCenter = centerDao.getTotalCount();

            lblTotalEvacValue.setText(String.valueOf(totEvacCenter));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}