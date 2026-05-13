package com.example.dashboard_admin;

import com.example.util.SceneHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class ActivityLogController{
    @FXML private Button navInventory;

    @FXML private Button navMap;

    @FXML private Button navEvacuations;

    @FXML private Button btnNewEntry;

    @FXML private Button btnNewEntry1;

    @FXML private Button btnNewEntry11;

    public void initialize() {
        navEvacuations.setOnAction(event -> {
            SceneHelper.switchScene("/com/example/dashboard_admin/evacuation.fxml", navEvacuations);
        });

        navInventory.setOnAction(event->{
            SceneHelper.switchScene("/com/example/dashboard_admin/inventory.fxml", navInventory);
        });

        navMap.setOnAction(event->{
            SceneHelper.switchScene("/com/example/dashboard_admin/map.fxml", navMap);
        });
    }
}
