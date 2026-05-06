package com.example.dashboard_admin;

import com.example.dashboard_admin.MapLogic.MapHtmlProvider;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

public class MapViewController implements Initializable {

    @FXML
    private WebView mapWebView;

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
    }

    private String loadMap() {
        return MapHtmlProvider.getMapHTML();
    }
}