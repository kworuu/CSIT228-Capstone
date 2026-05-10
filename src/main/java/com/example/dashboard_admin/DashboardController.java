package com.example.dashboard_admin;

import com.example.dashboard_admin.helper_classes.SearchUtils;
import com.example.database.DBQUERY.DatabaseManager;
import com.example.database.request;
import com.example.map_logic.MapHtmlProvider;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import com.example.dashboard_admin.helper_classes.SceneHelper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.web.WebView;

public class DashboardController {
    @FXML
    private TableView<request> tableRequest;
    @FXML
    private TableColumn<request, String> colID;
    @FXML
    private TableColumn<request, String> colCenter;
    @FXML
    private TableColumn<request, String> colItem;
    @FXML
    private TableColumn<request, Integer> colQty;
    @FXML
    private TableColumn<request, String> colDate;
    @FXML
    private TableColumn<request, String> colStatus;
    @FXML
    private TextField searchRequest;

    @FXML
    private Button btnNewItem;

    @FXML
    private Button navInventory;

    @FXML Button navMap;

    @FXML
    private Button btnExpandMap;
    @FXML
    private WebView webviewMiniMap;

    @FXML
    private Button navActivity;
    private ObservableList<request> masterData;


    public void setTableRequest(){
        colID.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCenter.setCellValueFactory(new PropertyValueFactory<>("center"));
        colItem.setCellValueFactory(new PropertyValueFactory<>("item"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        loadTableData();
    }

    private void loadTableData(){
        DatabaseManager db = new DatabaseManager();
        masterData = FXCollections.observableArrayList(db.fetchRequests());
        SearchUtils.setupSearch(searchRequest, tableRequest, masterData, (req, searchText) -> {

            String id = req.getId() != null ? req.getId().toLowerCase() : "";
            String center = req.getCenter() != null ? req.getCenter().toLowerCase() : "";
            String item = req.getItem() != null ? req.getItem().toLowerCase() : "";

            return id.contains(searchText) ||
                    center.contains(searchText) ||
                    item.contains(searchText);
        });

    }





    @FXML
    public void initialize() {
        navInventory.setOnAction(event -> {
            SceneHelper.switchScene("/com/example/dashboard_admin/inventory.fxml", navInventory);
        });

        // Wire the minimap
        webviewMiniMap.getEngine().setJavaScriptEnabled(true);
        webviewMiniMap.getEngine().loadContent(MapHtmlProvider.getMapHTML());

        // Wire the expand button for the minimap
        btnExpandMap.setOnAction(event -> {
            SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", btnExpandMap);
        });

        navMap.setOnAction(event-> {
            SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", navMap);
        });

        navActivity.setOnAction(event->{
            SceneHelper.switchScene("/com/example/dashboard_admin/activity-log.fxml", navActivity);
        });

        setTableRequest();
    }


}
