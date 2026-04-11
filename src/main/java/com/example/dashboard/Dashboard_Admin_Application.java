package com.example.dashboard;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Dashboard_Admin_Application extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Dashboard_Admin_Application.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 734, 513);
        stage.setTitle("Dashboard-admin");
        stage.setScene(scene);
        stage.show();
    }
}
