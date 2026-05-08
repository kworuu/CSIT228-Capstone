package com.example.dashboard_admin;

import com.example.map_logic.MapHtmlProvider;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import com.example.dashboard_admin.helper_classes.SceneHelper;
import javafx.scene.web.WebView;

public class DashboardController {
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

    }


}
