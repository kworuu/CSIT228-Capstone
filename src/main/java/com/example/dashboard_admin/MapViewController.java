package com.example.dashboard_admin;

import com.example.dashboard_admin.helper_classes.SceneHelper;
import com.example.map_logic.MapHtmlProvider;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

public class MapViewController implements Initializable {

    @FXML
    private WebView mapWebView;

    @FXML
    private Button navEvacuations;

    @FXML
    private Button navInventory;

    @FXML
    private Button navActivity;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Enable JavaScript and allow external resources
        mapWebView.getEngine().setJavaScriptEnabled(true);
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        mapWebView.getEngine().setUserAgent("CivicGuard/1.0 (Contact: your_email@student.cit.edu)");

        // THE MODERN BRIDGE: Catch JavaScript alert() calls and print them to Java console
        mapWebView.getEngine().setOnAlert(event -> {
            System.out.println("[MAP-DEBUG] " + event.getData());
        });

        // Load the map
        mapWebView.getEngine().setJavaScriptEnabled(true);
        mapWebView.getEngine().loadContent(loadMap());

        navActivity.setOnAction(event->{
            SceneHelper.switchScene("/com/example/dashboard_admin/activity-log.fxml", navActivity);
        });

        navInventory.setOnAction(event->{
            SceneHelper.switchScene("/com/example/dashboard_admin/inventory.fxml", navInventory);
        });

        navEvacuations.setOnAction(event->{
            SceneHelper.switchScene("/com/example/dashboard_admin/evacuation.fxml", navEvacuations);
        });
    }

    private String loadMap() {
        return MapHtmlProvider.getMapHTML();
    }
}