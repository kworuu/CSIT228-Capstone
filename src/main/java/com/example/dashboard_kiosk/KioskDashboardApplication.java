package com.example.dashboard_kiosk;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class KioskDashboardApplication extends Application {

    private static final String WINDOW_TITLE = "CivicGuard — Disaster Response";

    private double dragOffsetX;
    private double dragOffsetY;

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(
                getClass().getResource(KioskConstants.FXML_ROOT));

        attachDragHandlers(root, stage);

        stage.setScene(new Scene(root));
        stage.setTitle(WINDOW_TITLE);
        stage.setMaximized(true);
        stage.show();
    }


    private void attachDragHandlers(Parent root, Stage stage) {
        root.setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });
        root.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });
    }
}