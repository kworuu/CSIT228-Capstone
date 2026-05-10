package com.example.dashboard_barangay;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebView;
import com.example.map_logic.MapHtmlProvider;

public class BrgyDashboardController {

    @FXML
    private WebView webViewMiniMap;
    
    @FXML
    private ImageView offlineMapImage;

    @FXML
    public void initialize() {
        // Wire the minimap
        if (webViewMiniMap != null) {
            webViewMiniMap.getEngine().setJavaScriptEnabled(true);
            
            if (NetworkUtils.isInternetAvailable()) {
                webViewMiniMap.getEngine().loadContent(MapHtmlProvider.getMapHTML());
                
                // Save a snapshot of the map once it's loaded
                webViewMiniMap.getEngine().getLoadWorker().stateProperty().addListener(
                        (observable, oldValue, newValue) -> {
                            if (newValue == Worker.State.SUCCEEDED) {
                                // Add a slight delay to allow the map to render completely
                                new java.util.Timer().schedule(
                                    new java.util.TimerTask() {
                                        @Override
                                        public void run() {
                                            javafx.application.Platform.runLater(() -> {
//                                                 MapCache.saveMapImage(webViewMiniMap.snapshot(null, null));
                                            });
                                        }
                                    }, 
                                    2000 // Wait 2 seconds
                                );
                            }
                        }
                );
            } else {
                // Try to load cached map
////                Image cachedMap = MapCache.loadMapImage();
//                if (cachedMap != null && offlineMapImage != null) {
//                    webViewMiniMap.setVisible(false);
//                    offlineMapImage.setImage(cachedMap);
//
//                    // Bind ImageView size to its parent StackPane
//                    offlineMapImage.fitWidthProperty().bind(webViewMiniMap.widthProperty());
//                    offlineMapImage.fitHeightProperty().bind(webViewMiniMap.heightProperty());
//
//                    offlineMapImage.setVisible(true);
//                } else {
//                    // Fallback to error message if no cache exists
//                    webViewMiniMap.getEngine().loadContent(getOfflineHTML());
//                }
            }
        }
    }
    
    private String getOfflineHTML() {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                html, body {
                    width: 100vw;
                    height: 100vh;
                    margin: 0;
                    padding: 0;
                    background-color: #f3f4f6;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    color: #4b5563;
                }
                .container {
                    text-align: center;
                    padding: 20px;
                    border: 1px solid #d1d5db;
                    border-radius: 8px;
                    background-color: #ffffff;
                    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
                }
                h2 {
                    margin-top: 0;
                    color: #ef4444;
                }
                p {
                    margin-bottom: 0;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <h2>Map Unavailable</h2>
                <p>No internet connection detected.</p>
                <p>Please check your network and restart the application.</p>
            </div>
        </body>
        </html>
        """;
    }
}
