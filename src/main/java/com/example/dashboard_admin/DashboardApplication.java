package com.example.dashboard_admin;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class DashboardApplication extends Application {
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage stage) throws IOException {
        com.example.util.Router.getInstance().setPrimaryStage(stage);
        com.example.util.Router.getInstance().navigate(com.example.util.Route.ADMIN_LOGIN);

        FXMLLoader fxmlLoader = new FXMLLoader(DashboardApplication.class.getResource("evacuation.fxml"));
        Parent root = fxmlLoader.load();

        //stage.initStyle(StageStyle.UNDECORATED);

        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        Scene scene = new Scene(root);
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.show();

        stage.show();
    }



}