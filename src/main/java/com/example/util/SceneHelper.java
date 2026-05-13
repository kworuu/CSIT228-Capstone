package com.example.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class SceneHelper {

    /**
     * Opens a new window as a Modal (blocks the main window)
     */
    public static void showModal(String fxmlPath, String title, Button ownerButton) {


        try {
            FXMLLoader loader = new FXMLLoader(SceneHelper.class.getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();
            dragWindow(root, stage, 1);

            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(ownerButton.getScene().getWindow());
            stage.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            stage.sizeToScene();
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);


            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            System.err.println("Error loading modal: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Switches the current screen (Full screen transfer)
     */
    public static void switchScene(String fxmlPath, Button triggerButton) {
        try {

            Parent root = FXMLLoader.load(SceneHelper.class.getResource(fxmlPath));
            Stage stage = (Stage) triggerButton.getScene().getWindow();

            dragWindow(root, stage, 0);
            stage.setMaximized(true);
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("Error switching scene: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private static void dragWindow(Parent root, Stage stage, int xy){
        final double[] xOffset;
        final double[] yOffset;

        if(xy == 1){
            xOffset = new double[]{0};
            yOffset = new double[]{0};
        }else{
            xOffset = new double[]{0};
            yOffset = new double[]{0};
        }

        root.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset[0]);
            stage.setY(event.getScreenY() - yOffset[0]);
        });
    }

    public static FXMLLoader nestedModal(String fxmlPath, String title, Button ownerButton){
        FXMLLoader loader = new FXMLLoader(SceneHelper.class.getResource(fxmlPath));

        try {
            Parent root = loader.load();
            Stage stage = new Stage();
            dragWindow(root, stage, 1);

            stage.setTitle(title);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(ownerButton.getScene().getWindow());
            stage.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            stage.sizeToScene();
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);


            stage.setScene(scene);
            stage.show();
            return loader;
        } catch (IOException e) {
            System.err.println("Error loading modal: " + fxmlPath);
            e.printStackTrace();
        }

        return loader;
    }

}