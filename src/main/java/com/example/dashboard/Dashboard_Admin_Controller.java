package com.example.dashboard;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class Dashboard_Admin_Controller {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}
