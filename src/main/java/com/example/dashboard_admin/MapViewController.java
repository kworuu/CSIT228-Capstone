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

public class MapViewController implements Initializable {

    private static final double CEBU_SW_LAT = 10.250;
    private static final double CEBU_SW_LNG = 123.650;
    private static final double CEBU_NE_LAT = 10.500;
    private static final double CEBU_NE_LNG = 123.950;

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

    @FXML private Button navEvacuations;
    @FXML private Button navInventory;
    @FXML private Button navActivity;
    @FXML private Button btnRefresh;
    @FXML private Button buttonLogout;

    private final List<CenterData> centers = new ArrayList<>();
    private CenterData selectedCenter;
    private AdminMapBridge mapBridge;

    // FIXED: Removed Structural Status from the record to match our new schema
    public record CenterData(
            long id, String name, String address, String barangay,
            double lat, double lng,
            String eventLabel, List<String> availableItems,
            String updatedAt, String photoPath) {}

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        navEvacuations.setOnAction(e -> SceneHelper.switchScene("/com/example/dashboard_admin/evacuation.fxml", navEvacuations));
        navInventory.setOnAction(e -> SceneHelper.switchScene("/com/example/dashboard_admin/inventory.fxml", navInventory));

        setupMap();
        loadCentersAsync();

        TilePrefetchService.getInstance().prefetchAllBarangaysAsync((done, total, finalResult) -> {
            if (finalResult != null) {
                System.out.println("[AdminMap] Tile cache warmed: " + finalResult);
            }
        });
    }

    private void loadCentersAsync() {
        Thread worker = new Thread(() -> {
            List<CenterData> loaded = queryCentersFromDB();
            Platform.runLater(() -> {
                centers.clear();
                centers.addAll(loaded);
                setupMap();
            });
        }, "admin-map-centers-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private List<CenterData> queryCentersFromDB() {
        List<CenterData> result = new ArrayList<>();

        // FIXED: Updated query to use center_status_updates table and fetch correct columns!
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

                // FIXED: Actually calls the resolver to fetch supply names from the DB!
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

        // FIXED: Removed structural status rendering completely
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

    @FXML private void handleLogout() {
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