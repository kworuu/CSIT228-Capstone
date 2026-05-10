package com.example.dashboard_kiosk;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class KioskDashboardApplication extends Application {

    private double xOffset, yOffset;

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(
                getClass().getResource("BrgyUser.fxml"));

        root.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
        root.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });

        stage.setScene(new Scene(root));
        stage.setTitle("CivicGuard — Disaster Response");
        stage.setMaximized(true);
        stage.show();
    }
}