package com.example.dashboard_admin;

import com.example.map_logic_v2.BrgyMapHtmlProvider;
import com.example.map_tiles.TilePrefetchService;
import com.example.model.StructuralStatus;
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

/**
 * Controller for the admin "Map View" screen ({@code map.fxml}).
 *
 * <p>Renders every active evacuation center in the database as a marker on
 * a Cebu-City-bounded Leaflet map. The map factory, marker styling, click
 * bridge, and overlay panel are all shared with the per-barangay dashboard
 * and the kiosk login map — see
 * {@link com.example.map_logic_v2.BrgyMapHtmlProvider#getCityMapHTML}.</p>
 *
 * <p>Threading model (mirrors {@code BrgyDashboardController}):</p>
 * <ul>
 *   <li>The map shell is rendered synchronously with an empty center list,
 *       so the user sees a properly-framed Cebu City within milliseconds.</li>
 *   <li>The DB query (centers + their latest status update + inventory name
 *       resolution) runs on a background thread. When it completes, the
 *       centers are merged in and the map is re-rendered on the FX thread.</li>
 *   <li>The tile cache warmup runs on {@link TilePrefetchService}'s
 *       8-thread pool, completely off the FX thread.</li>
 * </ul>
 */
public class MapViewController implements Initializable {

    // ══════════════════════════════════════════════════════════════
    //  CEBU CITY BOUNDING BOX
    // ══════════════════════════════════════════════════════════════
    //
    // Same four corners the kiosk uses, kept in lock-step so the admin
    // and login maps frame the same area. Adjust here only — never
    // hardcode bounds in JS.
    private static final double CEBU_SW_LAT = 10.250;
    private static final double CEBU_SW_LNG = 123.650;
    private static final double CEBU_NE_LAT = 10.500;
    private static final double CEBU_NE_LNG = 123.950;

    private static final double CEBU_CENTER_LAT = (CEBU_SW_LAT + CEBU_NE_LAT) / 2.0;
    private static final double CEBU_CENTER_LNG = (CEBU_SW_LNG + CEBU_NE_LNG) / 2.0;

    private static final int CEBU_MAX_ZOOM = 17;   // street level
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    // ══════════════════════════════════════════════════════════════
    //  FXML — map + overlay
    // ══════════════════════════════════════════════════════════════
    @FXML private WebView    mapWebView;

    // Overlay (added in Phase 3's FXML patch — mirrors brgy dashboard layout)
    @FXML private AnchorPane vboxMapOverlay;
    @FXML private ImageView  imgOverlayCenter;
    @FXML private Label      labelOverlayName;
    @FXML private Label      labelOverlayAddress;
    @FXML private FlowPane   flowPaneOverlayPillsRow;
    @FXML private Label      labelOverlayEvent;
    @FXML private Label      labelOverlayTimestamp;
    @FXML private Button     buttonOverlayClose;
    @FXML private Button     btnReturnHome;

    // ── FXML — Sidebar nav buttons (existing) ─────────────────────
    @FXML private Button navEvacuations;
    @FXML private Button navInventory;
    @FXML private Button navActivity;
    
    // ── FXML — Header Bar (added) ─────────────────────────────────
    @FXML private Button btnRefresh;
    @FXML private Button buttonLogout;

    // ══════════════════════════════════════════════════════════════
    //  State
    // ══════════════════════════════════════════════════════════════
    private final List<CenterData> centers = new ArrayList<>();
    private CenterData selectedCenter;

    // Strong reference: JavaFX will GC the bridge object otherwise and
    // JS-side calls into Java will start throwing NullPointerException.
    private AdminMapBridge mapBridge;

    // ══════════════════════════════════════════════════════════════
    //  Center record (same shape as BrgyDashboardController.CenterData)
    // ══════════════════════════════════════════════════════════════
    public record CenterData(
            long id, String name, String address, String barangay,
            double lat, double lng,
            String eventLabel, List<String> availableItems,
            String updatedAt, String photoPath,
            StructuralStatus structuralStatus,
            String structuralNotes) {}

    // ══════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // 1. Wire navigation FIRST so a slow DB doesn't trap the admin
        //    on the map screen.
        navEvacuations.setOnAction(e -> SceneHelper.switchScene(
                "/com/example/dashboard_admin/evacuation.fxml", navEvacuations));
        navInventory.setOnAction(e -> SceneHelper.switchScene(
                "/com/example/dashboard_admin/inventory.fxml", navInventory));

        // 2. Render the map shell synchronously with an empty marker list.
        //    The Cebu City frame is visible within milliseconds — the user
        //    never sees a blank panel waiting on the DB.
        setupMap();

        // 3. Load every active evacuation center on a background thread.
        //    Markers appear when the DB returns; the FX thread stays free.
        loadCentersAsync();

        // 4. Warm the local tile cache on the prefetch service's
        //    dedicated 8-thread pool — same pattern as BrgyDashboard
        //    and KioskDashboard.
        TilePrefetchService.getInstance().prefetchAllBarangaysAsync(
                (done, total, finalResult) -> {
                    if (finalResult != null) {
                        System.out.println("[AdminMap] Tile cache warmed: " + finalResult);
                    } else if (done % 50 == 0) {
                        System.out.println("[AdminMap] Prefetch progress: "
                                + done + "/" + total);
                    }
                });
    }

    // ══════════════════════════════════════════════════════════════
    //  DATABASE — async load
    // ══════════════════════════════════════════════════════════════

    /**
     * Runs the centers query off the FX thread, then re-renders the map
     * on the FX thread with the loaded data.
     */
    private void loadCentersAsync() {
        Thread worker = new Thread(() -> {
            List<CenterData> loaded = queryCentersFromDB();

            Platform.runLater(() -> {
                centers.clear();
                centers.addAll(loaded);
                // Re-render: WebView is already initialised; this just
                // swaps the HTML and the marker layer comes back populated.
                setupMap();
            });
        }, "admin-map-centers-loader");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Same query the barangay dashboard uses, minus the
     * {@code WHERE barangay = ?} filter — the admin sees every center,
     * city-wide.
     */
    private List<CenterData> queryCentersFromDB() {
        List<CenterData> result = new ArrayList<>();

        String sql = """
            SELECT
                ec.id, ec.name, ec.address, ec.barangay, ec.photo_path,
                ec.latitude, ec.longitude,
                ec.structural_status, ec.structural_notes,
                csu.event_label, csu.available_item_ids, csu.updated_at
            FROM evacuation_centers ec
            LEFT JOIN (
                SELECT center_id,
                       event_label, available_item_ids, updated_at,
                       ROW_NUMBER() OVER (PARTITION BY center_id
                                          ORDER BY updated_at DESC) AS rn
                FROM center_status_updates
            ) csu ON csu.center_id = ec.id AND csu.rn = 1
            WHERE ec.is_active = 1
            ORDER BY ec.name
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long id           = rs.getLong("id");
                String name       = rs.getString("name");
                String address    = rs.getString("address");
                String barangay   = rs.getString("barangay");
                String photoPath  = rs.getString("photo_path");
                double lat        = rs.getDouble("latitude");
                double lng        = rs.getDouble("longitude");

                String eventLabel = rs.getString("event_label");
                if (eventLabel == null) eventLabel = "No active event";

                String itemJson   = rs.getString("available_item_ids");
                List<String> items = resolveItemNames(itemJson, conn);

                String updatedAt  = formatTimestamp(rs.getString("updated_at"));

                StructuralStatus structStatus =
                        StructuralStatus.fromDb(rs.getString("structural_status"));
                String structNotes = rs.getString("structural_notes");

                result.add(new CenterData(
                        id, name, address, barangay,
                        lat, lng,
                        eventLabel, items, updatedAt, photoPath,
                        structStatus, structNotes));
            }
        } catch (SQLException e) {
            System.err.println("[AdminMap] DB error loading centers: " + e.getMessage());
        }
        return result;
    }

    /**
     * Turns a JSON array string like {@code "[1,3,4]"} (stored in
     * {@code center_status_updates.available_item_ids}) into the matching
     * item names. Stays on the background thread by reusing the caller's
     * connection.
     */
    private List<String> resolveItemNames(String itemJson, Connection conn) {
        List<String> names = new ArrayList<>();
        if (itemJson == null || itemJson.isBlank()) return names;

        String stripped = itemJson.replaceAll("[\\[\\]\\s]", "");
        if (stripped.isEmpty()) return names;

        String[] parts = stripped.split(",");
        StringBuilder placeholders = new StringBuilder();
        List<Long> ids = new ArrayList<>();
        for (String p : parts) {
            try {
                ids.add(Long.parseLong(p));
                if (placeholders.length() > 0) placeholders.append(",");
                placeholders.append("?");
            } catch (NumberFormatException ignored) { /* skip junk */ }
        }
        if (ids.isEmpty()) return names;

        String sql = "SELECT name FROM inventory_items WHERE id IN (" + placeholders + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) ps.setLong(i + 1, ids.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) names.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("[AdminMap] Could not resolve item names: "
                    + e.getMessage());
        }
        return names;
    }

    private String formatTimestamp(String raw) {
        if (raw == null) return "Updated: —";
        try {
            // MySQL TIMESTAMP comes back as "yyyy-MM-dd HH:mm:ss"
            LocalDateTime dt = LocalDateTime.parse(raw.replace(' ', 'T'));
            return "Updated: " + dt.format(DISPLAY_FMT);
        } catch (Exception e) {
            return "Updated: " + raw;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  MAP
    // ══════════════════════════════════════════════════════════════

    private void setupMap() {
        if (mapWebView == null) return;

        mapWebView.getEngine().setJavaScriptEnabled(true);
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        mapWebView.getEngine().setUserAgent(
                "CivicGuard/1.0 (Contact: your_email@student.cit.edu)");

        // Forward JavaScript alert() to the Java console — handy for
        // debugging the Leaflet code inside the WebView.
        mapWebView.getEngine().setOnAlert(event ->
                System.out.println("[MAP-DEBUG] " + event.getData()));

        // (Re)install the JS-to-Java bridge every time the page reloads.
        // setupMap() can run twice (once with empty centers, once with
        // real centers) so we re-bind on each SUCCEEDED state.
        mapWebView.getEngine().getLoadWorker().stateProperty()
                .addListener((obs, old, state) -> {
                    if (state == Worker.State.SUCCEEDED) {
                        JSObject win = (JSObject) mapWebView.getEngine()
                                .executeScript("window");
                        this.mapBridge = new AdminMapBridge(this);
                        win.setMember("javaBridge", this.mapBridge);
                    }
                });

        int tilePort = -1;
        try {
            tilePort = TilePrefetchService.getInstance().startServer();
        } catch (Exception e) {
            System.err.println("[AdminMap] Could not start tile server: "
                    + e.getMessage());
        }

        mapWebView.getEngine().loadContent(
                BrgyMapHtmlProvider.getCityMapHTML(
                        buildCentersJson(),
                        CEBU_SW_LAT, CEBU_SW_LNG,
                        CEBU_NE_LAT, CEBU_NE_LNG,
                        CEBU_CENTER_LAT, CEBU_CENTER_LNG,
                        CEBU_MAX_ZOOM,
                        tilePort
                )
        );
    }

    /**
     * Serializes the in-memory center list into the JSON array the map
     * HTML expects. Shape per element: {@code {id, name, lat, lng}}.
     */
    private String buildCentersJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < centers.size(); i++) {
            CenterData c = centers.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format(
                    "{\"id\":%d,\"name\":\"%s\",\"lat\":%s,\"lng\":%s}",
                    c.id(),
                    esc(c.name()),
                    c.lat(),
                    c.lng()
            ));
        }
        return sb.append("]").toString();
    }

    /** Called from the bridge (already trampolined to the FX thread). */
    public void onMarkerClicked(String centerId) {
        long id;
        try { id = Long.parseLong(centerId); }
        catch (NumberFormatException e) { return; }

        centers.stream()
                .filter(c -> c.id() == id)
                .findFirst()
                .ifPresent(this::showOverlay);
    }

    // ══════════════════════════════════════════════════════════════
    //  OVERLAY  (mirrors BrgyDashboardController.showOverlay)
    // ══════════════════════════════════════════════════════════════

    private void showOverlay(CenterData c) {
        if (vboxMapOverlay == null) return;   // FXML not patched yet

        selectedCenter = c;

        labelOverlayName.setText(c.name());
        labelOverlayAddress.setText(c.address());
        labelOverlayEvent.setText(c.eventLabel());
        labelOverlayTimestamp.setText(c.updatedAt());

        // Header image — graceful fallback if path is null/blank/broken.
        if (c.photoPath() != null && !c.photoPath().isBlank()) {
            try {
                Image img = new Image(getClass().getResourceAsStream(c.photoPath()));
                imgOverlayCenter.setImage(img);
                imgOverlayCenter.setVisible(true);
                imgOverlayCenter.setManaged(true);
            } catch (Exception e) {
                System.err.println("[AdminMap] Could not load overlay image: "
                        + c.photoPath());
                imgOverlayCenter.setVisible(false);
                imgOverlayCenter.setManaged(false);
            }
        } else {
            imgOverlayCenter.setVisible(false);
            imgOverlayCenter.setManaged(false);
        }

        // Rebuild pills: structural badge first, then supplies.
        flowPaneOverlayPillsRow.getChildren().clear();

        if (c.structuralStatus() != null) {
            Label structBadge = new Label("🏛 " + c.structuralStatus().displayLabel());
            structBadge.getStyleClass().addAll("brgy-pill", c.structuralStatus().cssClass());
            if (c.structuralNotes() != null && !c.structuralNotes().isBlank()) {
                Tooltip.install(structBadge, new Tooltip(c.structuralNotes()));
            }
            flowPaneOverlayPillsRow.getChildren().add(structBadge);
        }

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

    /** Rough category detection for pill colour — matches BrgyDashboardController. */
    private String pillCategory(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("rice") || lower.contains("pancit") ||
                lower.contains("sardine") || lower.contains("food") ||
                lower.contains("canton"))
            return "brgy-pill-food";
        if (lower.contains("water") || lower.contains("drink"))
            return "brgy-pill-water";
        if (lower.contains("blanket") || lower.contains("hygiene") ||
                lower.contains("kit") || lower.contains("cloth"))
            return "brgy-pill-nonfood";
        return "brgy-pill-other";
    }

    @FXML
    private void handleOverlayClose() {
        if (vboxMapOverlay == null) return;
        vboxMapOverlay.setVisible(false);
        vboxMapOverlay.setManaged(false);
        selectedCenter = null;
    }

    // ══════════════════════════════════════════════════════════════
    //  EVENT HANDLERS
    // ══════════════════════════════════════════════════════════════
    
    @FXML
    private void handleRefresh() {
        loadCentersAsync();
        if (selectedCenter != null) {
            centers.stream()
                    .filter(c -> c.id() == selectedCenter.id())
                    .findFirst()
                    .ifPresent(this::showOverlay);
        }
    }
    
    @FXML
    private void handleLogout() {
        new com.example.auth.AuthService().logout();
        com.example.util.Router.getInstance().navigate(com.example.util.Route.KIOSK);
    }
    
    @FXML
    private void handleReturnHome() {
        btnReturnHome.setVisible(false); // Hide immediately on click
        try {
            // Tell the Leaflet map to fly back to the center
            mapWebView.getEngine().executeScript("if(window.flyHome) window.flyHome();");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ══════════════════════════════════════════════════════════════
    //  Utility
    // ══════════════════════════════════════════════════════════════

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");
    }

    // ══════════════════════════════════════════════════════════════
    //  JS → Java bridge
    // ══════════════════════════════════════════════════════════════

    /**
     * Public so JavaScript inside the WebView can see its methods via
     * {@link JSObject#setMember}. Methods are invoked on the WebKit
     * thread, so every callback hops back onto the FX thread via
     * {@link Platform#runLater}.
     */
    public static class AdminMapBridge {
        private final MapViewController ctrl;
        public AdminMapBridge(MapViewController ctrl) { this.ctrl = ctrl; }

        /** Called from JavaScript when a marker is clicked. */
        public void onMarkerClick(String centerId) {
            Platform.runLater(() -> ctrl.onMarkerClicked(centerId));
        }
        
        public void toggleHomeButton(boolean show) {
            Platform.runLater(() -> {
                if (ctrl.btnReturnHome != null) {
                    if (ctrl.btnReturnHome.isVisible() != show) {
                        ctrl.btnReturnHome.setVisible(show);
                    }
                }
            });
        }
    }
}