package com.example.dashboard_barangay;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import com.example.map_logic.MapHtmlProvider;

public class BrgyDashboardController {

    @FXML
    private WebView webViewMiniMap;

    @FXML
    public void initialize() {
        // Wire the minimap
        if (webViewMiniMap != null) {
            webViewMiniMap.getEngine().setJavaScriptEnabled(true);
            webViewMiniMap.getEngine().loadContent(MapHtmlProvider.getMapHTML());
        }
    }
}
