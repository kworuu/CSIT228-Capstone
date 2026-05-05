package com.example.dashboard_admin;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import com.example.dashboard_admin.HelperClasses.SceneHelper;

public class DashboardController {
    @FXML
    private Button btnNewItem;

    @FXML
    private Button navInventory;

    @FXML
    public void initialize() {
        navInventory.setOnAction(event -> {
            SceneHelper.switchScene("/com/example/dashboard_admin/Inventory.fxml", navInventory);
        });
    }
}
