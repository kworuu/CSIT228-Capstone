package com.example.dashboard_admin;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import com.example.dashboard_admin.helper_classes.SceneHelper;


public class InventoryController {

    @FXML
    private Button btnNewItem;

    @FXML
    private Button navEvacuations;

    @FXML
    private Button navMap;

    @FXML
    private Button navActivity;

    @FXML
    public void initialize() {
        btnNewItem.setOnAction(event -> {
            SceneHelper.showModal("/com/example/dashboard_admin/modals/add-item.fxml", "Insert New Item", btnNewItem);
        });

        navEvacuations.setOnAction(event -> {
            SceneHelper.switchScene("/com/example/dashboard_admin/evacuation.fxml", navEvacuations);
        });

        navMap.setOnAction(event-> {
            SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", navMap);
        });

        navActivity.setOnAction(event->{
            SceneHelper.switchScene("/com/example/dashboard_admin/activity-log.fxml", navActivity);
        });
    }
}