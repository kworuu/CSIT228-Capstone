package com.example.dashboard_admin;

import com.example.map_logic_v2.BrgyMapHtmlProvider;
import com.example.map_tiles.TilePrefetchService;
import com.example.util.DBConnectionManager;
import com.example.util.SceneHelper;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MapViewController implements Initializable, com.example.util.CenterEventObserver {

    private static final double CEBU_SW_LAT = 10.250429;
    private static final double CEBU_SW_LNG = 123.864302;
    private static final double CEBU_NE_LAT = 10.503349;
    private static final double CEBU_NE_LNG = 123.877159;

    private static final double CEBU_CENTER_LAT = (CEBU_SW_LAT + CEBU_NE_LAT) / 2.0;
    private static final double CEBU_CENTER_LNG = (CEBU_SW_LNG + CEBU_NE_LNG) / 2.0;

    private static final int CEBU_MAX_ZOOM = 17;
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    @FXML private WebView mapWebView;
    @FXML private AnchorPane vboxMapOverlay;
    @FXML private ImageView imgOverlayCenter;
    @FXML private Label labelOverlayName;
    @FXML private Label labelOverlayAddress;
    @FXML private FlowPane flowPaneOverlayPillsRow;
    @FXML private Label labelOverlayEvent;
    @FXML private Label labelOverlayTimestamp;
    @FXML private Button buttonOverlayClose;
    @FXML private Button btnReturnHome;
    @FXML private VBox eventsContainer;

    @FXML private Button navEvacuations;
    @FXML private Button navInventory;
    @FXML private Button navActivity;
    @FXML private Button btnRefresh;
    @FXML private Button buttonLogout; // Added for logout functionality

    private final List<CenterData> centers = new ArrayList<>();
    private CenterData selectedCenter;
    private AdminMapBridge mapBridge;
    private boolean isMapLoaded = false; // Flag to check if Leaflet needs initialization

    public record CenterData(
            long id, String name, String address, String barangay,
            double lat, double lng,
            String eventLabel, List<String> availableItems,
            String updatedAt, String photoPath) {}


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        navEvacuations.setOnAction(e -> SceneHelper.switchScene("/com/example/dashboard_admin/evacuation.fxml", navEvacuations));
        navInventory.setOnAction(e -> SceneHelper.switchScene("/com/example/dashboard_admin/inventory.fxml", navInventory));

        // Start pulling standard database logs right away
        loadCentersAsync();

        // Register to receive live barangay updates instantly!
        com.example.util.CenterEventManager.getInstance().addObserver(this);

        TilePrefetchService.getInstance().prefetchAllBarangaysAsync((done, total, finalResult) -> {
            if (finalResult != null) {
                System.out.println("[AdminMap] Tile cache warmed: " + finalResult);
            }
        });
    }

    private void loadCentersAsync() {
        Thread worker = new Thread(() -> {
            List<CenterData> loadedCenters = queryCentersFromDB();
            List<com.example.util.CenterEvent> recentLogs = queryRecentLogsFromDB();

            Platform.runLater(() -> {
                centers.clear();
                centers.addAll(loadedCenters);

                // Only load the Leaflet HTML container if it hasn't been rendered yet
                if (!isMapLoaded) {
                    setupMap();
                    isMapLoaded = true;
                } else {
                    // Optional fallback: If BrgyMapHtmlProvider supports hot-swapping JSON, push data here:
                    // mapWebView.getEngine().executeScript("if(window.updateMarkers){ window.updateMarkers(" + buildCentersJson() + "); }");
                }

                // Populates the chronological activity stream independently of map state
                populateRecentActivityLog(recentLogs);
            });
        }, "admin-map-centers-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private List<com.example.util.CenterEvent> queryRecentLogsFromDB() {
        List<com.example.util.CenterEvent> events = new ArrayList<>();
        String sql = """
            SELECT csu.center_id, csu.event_label, csu.updated_at, ec.name 
            FROM center_status_updates csu 
            JOIN evacuation_centers ec ON csu.center_id = ec.id 
            ORDER BY csu.updated_at DESC LIMIT 15
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                events.add(new com.example.util.CenterEvent(
                        rs.getLong("center_id"),
                        rs.getString("name"),
                        rs.getString("event_label"),
                        formatTimestamp(rs.getString("updated_at")).replace("Updated: ", "")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[AdminMap] Error loading log feed: " + e.getMessage());
        }
        return events;
    }


    private List<CenterData> queryCentersFromDB() {
        List<CenterData> result = new ArrayList<>();
        String sql = """
            SELECT
                ec.id, ec.name, ec.address, u.display_name as barangay, ec.photo_path,
                ec.latitude, ec.longitude,
                csu.event_label, csu.available_item_ids, csu.updated_at
            FROM evacuation_centers ec
            LEFT JOIN users u ON ec.user_id = u.id
            LEFT JOIN (
                SELECT center_id,
                       event_label, available_item_ids, updated_at,
                       ROW_NUMBER() OVER (PARTITION BY center_id ORDER BY updated_at DESC) AS rn
                FROM center_status_updates
            ) csu ON csu.center_id = ec.id AND csu.rn = 1
            ORDER BY ec.name
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long id = rs.getLong("id");
                String name = rs.getString("name");
                String address = rs.getString("address");
                String barangay = rs.getString("barangay");
                String photoPath = rs.getString("photo_path");
                double lat = rs.getDouble("latitude");
                double lng = rs.getDouble("longitude");

                String eventLabel = rs.getString("event_label");
                if (eventLabel == null) eventLabel = "No active event";

                String itemJson = rs.getString("available_item_ids");
                List<String> items = resolveItemNames(itemJson, conn);

                String updatedAt = formatTimestamp(rs.getString("updated_at"));

                result.add(new CenterData(id, name, address, barangay, lat, lng, eventLabel, items, updatedAt, photoPath));
            }
        } catch (SQLException e) {
            System.err.println("[AdminMap] DB error loading centers: " + e.getMessage());
        }
        return result;
    }

    private List<String> resolveItemNames(String itemJson, Connection conn) {
        List<String> names = new ArrayList<>();
        if (itemJson == null || itemJson.isBlank() || itemJson.equals("[]")) return names;

        String stripped = itemJson.replaceAll("[\\[\\]\\s]", "");
        if (stripped.isEmpty()) return names;

        String[] parts = stripped.split(",");
        StringBuilder placeholders = new StringBuilder();
        List<Long> ids = new ArrayList<>();

        for (String p : parts) {
            try {
                ids.add(Long.parseLong(p.trim()));
                if (placeholders.length() > 0) placeholders.append(",");
                placeholders.append("?");
            } catch (NumberFormatException ignored) {}
        }

        if (ids.isEmpty()) return names;

        String sql = "SELECT name FROM inventory_items WHERE id IN (" + placeholders + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) ps.setLong(i + 1, ids.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) names.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("[AdminMap] Could not resolve item names: " + e.getMessage());
        }
        return names;
    }

    private String formatTimestamp(String raw) {
        if (raw == null) return "Updated: —";
        try {
            LocalDateTime dt = LocalDateTime.parse(raw.replace(' ', 'T').substring(0, 19));
            return "Updated: " + dt.format(DISPLAY_FMT);
        } catch (Exception e) {
            return "Updated: " + raw;
        }
    }

    private void setupMap() {
        if (mapWebView == null) return;
        mapWebView.getEngine().setJavaScriptEnabled(true);
        mapWebView.getEngine().setUserAgent("CivicGuard/1.0");

        mapWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject win = (JSObject) mapWebView.getEngine().executeScript("window");
                this.mapBridge = new AdminMapBridge(this);
                win.setMember("javaBridge", this.mapBridge);
            }
        });

        int tilePort = -1;
        try { tilePort = TilePrefetchService.getInstance().startServer(); } catch (Exception ignored) {}

        mapWebView.getEngine().loadContent(
                BrgyMapHtmlProvider.getCityMapHTML(
                        buildCentersJson(), CEBU_SW_LAT, CEBU_SW_LNG, CEBU_NE_LAT, CEBU_NE_LNG,
                        CEBU_CENTER_LAT, CEBU_CENTER_LNG, CEBU_MAX_ZOOM, tilePort
                )
        );
    }

    private String buildCentersJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < centers.size(); i++) {
            CenterData c = centers.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"id\":%d,\"name\":\"%s\",\"lat\":%s,\"lng\":%s}",
                    c.id(), esc(c.name()), c.lat(), c.lng()));
        }
        return sb.append("]").toString();
    }

    public void onMarkerClicked(String centerId) {
        try {
            long id = Long.parseLong(centerId);
            centers.stream().filter(c -> c.id() == id).findFirst().ifPresent(this::showOverlay);
        } catch (NumberFormatException ignored) {}
    }

    private void showOverlay(CenterData c) {
        if (vboxMapOverlay == null) return;
        vboxMapOverlay.setMinHeight(Region.USE_PREF_SIZE);
        vboxMapOverlay.setPrefHeight(Region.USE_COMPUTED_SIZE);
        vboxMapOverlay.setMaxHeight(Region.USE_PREF_SIZE);

        selectedCenter = c;
        labelOverlayName.setText(c.name());
        labelOverlayAddress.setText(c.address());
        labelOverlayEvent.setText(c.eventLabel());
        labelOverlayTimestamp.setText(c.updatedAt());

        if (c.photoPath() != null && !c.photoPath().isBlank()) {
            try {
                imgOverlayCenter.setImage(new Image(getClass().getResourceAsStream(c.photoPath())));
                imgOverlayCenter.setVisible(true);
                imgOverlayCenter.setManaged(true);
            } catch (Exception e) {
                imgOverlayCenter.setVisible(false);
                imgOverlayCenter.setManaged(false);
            }
        } else {
            imgOverlayCenter.setVisible(false);
            imgOverlayCenter.setManaged(false);
        }

        flowPaneOverlayPillsRow.getChildren().clear();

        if (c.availableItems().isEmpty()) {
            Label none = new Label("No supplies listed");
            none.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11;");
            flowPaneOverlayPillsRow.getChildren().add(none);
        } else {
            for (String item : c.availableItems()) {
                Label pill = new Label(item);
                pill.getStyleClass().addAll("brgy-pill", pillCategory(item));
                flowPaneOverlayPillsRow.getChildren().add(pill);
            }
        }

        vboxMapOverlay.setVisible(true);
        vboxMapOverlay.setManaged(true);
    }

    private void populateRecentActivityLog(List<com.example.util.CenterEvent> events) {
        if (eventsContainer == null) return;
        eventsContainer.getChildren().clear();

        if (events.isEmpty()) {
            Label noEventsLabel = new Label("No recent updates or events logged.");
            noEventsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic; -fx-font-size: 13px; -fx-padding: 10px;");
            eventsContainer.getChildren().add(noEventsLabel);
            return;
        }

        for (com.example.util.CenterEvent event : events) {
            VBox card = new VBox();
            card.setSpacing(6.0);
            card.getStyleClass().add("alert-item");
            card.setStyle("-fx-cursor: hand;");

            // Clicking an alert shifts the view focus layout drawer directly onto the map pin!
            card.setOnMouseClicked(e -> onMarkerClicked(String.valueOf(event.centerId())));

            VBox titleVBox = new VBox();
            titleVBox.setSpacing(2.0);
            Label title = new Label(event.centerName());
            title.getStyleClass().add("alert-title");
            title.setWrapText(true);

            Label eventLabel = new Label("⚠️ " + event.eventLabel());
            eventLabel.getStyleClass().add("alert-location");
            eventLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
            eventLabel.setWrapText(true);
            titleVBox.getChildren().addAll(title, eventLabel);

            HBox detailsBox = new HBox();
            detailsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            Label timestampLabel = new Label(event.timestamp());
            timestampLabel.getStyleClass().add("alert-location");

            detailsBox.getChildren().addAll(spacer, timestampLabel);
            card.getChildren().addAll(titleVBox, detailsBox);

            eventsContainer.getChildren().add(card);
        }
    }

    @Override
    public void onCenterUpdated(com.example.util.CenterEvent event) {
        Platform.runLater(this::loadCentersAsync);
    }

    private String pillCategory(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("rice") || lower.contains("pancit") || lower.contains("sardine") || lower.contains("food")) return "brgy-pill-food";
        if (lower.contains("water") || lower.contains("drink")) return "brgy-pill-water";
        if (lower.contains("kit") || lower.contains("hygiene")) return "brgy-pill-nonfood";
        return "brgy-pill-other";
    }

    @FXML private void handleOverlayClose() {
        if (vboxMapOverlay != null) {
            vboxMapOverlay.setVisible(false);
            vboxMapOverlay.setManaged(false);
        }
        selectedCenter = null;
    }

    @FXML private void handleRefresh() {
        loadCentersAsync();
        if (selectedCenter != null) {
            centers.stream().filter(c -> c.id() == selectedCenter.id()).findFirst().ifPresent(this::showOverlay);
        }
    }

    @FXML
    private void handleLogout() {
        new com.example.auth.AuthService().logout();
        com.example.util.Router.getInstance().navigate(com.example.util.Route.KIOSK);
    }

    @FXML private void handleReturnHome() {
        btnReturnHome.setVisible(false);
        try { mapWebView.getEngine().executeScript("if(window.flyHome) window.flyHome();"); }
        catch (Exception ignored) {}
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    public static class AdminMapBridge {
        private final MapViewController ctrl;
        public AdminMapBridge(MapViewController ctrl) { this.ctrl = ctrl; }
        public void onMarkerClick(String centerId) { Platform.runLater(() -> ctrl.onMarkerClicked(centerId)); }
        public void toggleHomeButton(boolean show) {
            Platform.runLater(() -> {
                if (ctrl.btnReturnHome != null && ctrl.btnReturnHome.isVisible() != show) {
                    ctrl.btnReturnHome.setVisible(show);
                }
            });
        }
    }
}