package com.example;

import com.example.map_tiles.TilePrefetchService;
import com.example.util.Route;
import com.example.util.Router;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class AppLauncher extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // 1. Start the local tile HTTP server (idempotent — singleton handles re-entry).
        try {
            int port = TilePrefetchService.getInstance().startServer();
            System.out.println("[AppLauncher] Local tile server listening on port " + port);
        } catch (Exception e) {
            System.err.println("[AppLauncher] Failed to start local tile server: " + e.getMessage());
            // Non-fatal: the map will fall back to live OSM. Continue.
        }

        // 2. Kick off background tile prefetch. This is the multithreaded
        TilePrefetchService.getInstance().prefetchAllBarangaysAsync(
                (done, total, finalResult) -> {
                    if (finalResult != null) {
                        System.out.println("[AppLauncher] Tile prefetch complete: " + finalResult);
                    } else if (done % 100 == 0) {
                        System.out.println("[AppLauncher] Prefetch progress: " + done + "/" + total);
                    }
                });

        // 3. Wire up the router and navigate to the default screen.
        Router.getInstance().setPrimaryStage(stage);
        stage.setTitle("CivicGuard — Disaster Response");
        stage.setMaximized(true);
        
        // Ensure the application stops running when the primary stage is closed
        stage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });

        Router.getInstance().navigate(Route.KIOSK);
    }
}