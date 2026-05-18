package com.example.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Application-wide navigation controller. Owns the primary {@link Stage}
 * and swaps its scene root when navigating between top-level routes.
 *
 * <p>Implements the Singleton design pattern (rubric criterion 7).
 * Singleton is appropriate because there is exactly one primary stage
 * per JVM in a desktop app, and centralizing route resolution keeps
 * FXML paths out of every controller.</p>
 *
 * <p>Typical usage from a controller:</p>
 * <pre>{@code
 *   Router.getInstance().navigate(Route.ADMIN_LOGIN);
 * }</pre>
 *
 * <p>The router uses scene-root swapping rather than creating new
 * {@code Stage} objects per navigation — this preserves stage state
 * (maximized, position, window decorations) across route changes.</p>
 */
public final class Router {

    private static volatile Router instance;

    public static Router getInstance() {
        Router local = instance;
        if (local == null) {
            synchronized (Router.class) {
                local = instance;
                if (local == null) {
                    local = new Router();
                    instance = local;
                }
            }
        }
        return local;
    }

    private Stage primaryStage;

    private Router() {}

    /** Must be called exactly once from {@code AppLauncher.start()}. */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Navigates to the given route. Loads the FXML, swaps the scene root
     * (or creates the scene on first call), and shows the stage if hidden.
     *
     * @throws IllegalStateException if {@link #setPrimaryStage(Stage)} was never called
     * @throws RuntimeException if the FXML cannot be loaded
     */
    public void navigate(Route route) {
        if (primaryStage == null) {
            throw new IllegalStateException(
                    "Router.setPrimaryStage(...) must be called before navigate()");
        }

        String fxmlPath = resolveFxml(route);
        URL fxmlUrl = Router.class.getResource(fxmlPath);
        if (fxmlUrl == null) {
            throw new RuntimeException("FXML not found on classpath: " + fxmlPath);
        }

        try {
            Parent root = FXMLLoader.load(fxmlUrl);

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root);
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML for route " + route, e);
        }
    }

    /**
     * Maps each route to its FXML resource path (classpath-relative,
     * absolute style with a leading slash).
     *
     * <p>Login FXMLs are placeholders — create them in Phase 3 step 2.
     * The router itself compiles fine without them; only an actual
     * navigation attempt will fail until they exist.</p>
     */
    private String resolveFxml(Route route) {
        return switch (route) {
            case KIOSK              -> "/com/example/dashboard_kiosk/controller/dashboard-user.fxml";
            case ADMIN_LOGIN        -> "/com/example/auth/admin-login.fxml";
            case BARANGAY_LOGIN     -> "/com/example/auth/barangay-login.fxml";
            case ADMIN_DASHBOARD    -> "/com/example/dashboard_admin/evacuation.fxml";
            case BARANGAY_DASHBOARD -> "/com/example/dashboard_barangay/dashboard-barangay.fxml";
        };
    }
}
