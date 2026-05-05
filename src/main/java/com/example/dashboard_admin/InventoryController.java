package com.example.dashboard_admin;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import com.example.dashboard_admin.HelperClasses.SceneHelper;


public class InventoryController {

    @FXML
    private Button btnNewItem;

    @FXML
    private Button navEvacuations;

    @FXML
    public void initialize() {
        btnNewItem.setOnAction(event -> {
            SceneHelper.showModal("/com/example/dashboard_admin/Modals/add_item.fxml", "Insert New Item", btnNewItem);
        });

        navEvacuations.setOnAction(event -> {
            SceneHelper.switchScene("/com/example/dashboard_admin/Evacuation.fxml", navEvacuations);
        });
    }
}