package com.example.dashboard_barangay;

import com.example.map_logic_v2.BrgyMapHtmlProvider;
import com.example.util.DBConnectionManager;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for BrgyDashboard.fxml
 *
 * Phase 1 + 2 implementation:
 *  - Sidebar nav switching (Map / Registrations / Activity)
 *  - Map loads with centers from DB, zoomed to fit
 *  - Marker click → overlay panel with center details, supply pills, event
 *  - Center cards strip below map, synced with marker selection
 *  - "Update Center Info" button wired (modal in Phase 3)
 *  - Registration + Activity panels stubbed (Phase 3/4)
 */
public class BrgyDashboardController {

    // ── FXML — Sidebar ─────────────────────────────────────────────
    @FXML private AnchorPane anchorPaneMainRoot;
    @FXML private VBox vboxSidebar;
    @FXML private Button buttonNavMap;
    @FXML private Button buttonNavRegister;
    @FXML private Button buttonNavActivity;
    @FXML private Label  labelBrgyName;
    @FXML private Label  labelStaffName;
    @FXML private Label  labelStaffRole;
    @FXML private Label  labelSystemStatus;
    @FXML private Circle circleStatusDot;

    // ── FXML — Header ──────────────────────────────────────────────
    @FXML private HBox   hboxHeaderBar;
    @FXML private Label  labelHeaderTitle;
    @FXML private Label  labelHeaderSubtitle;
    @FXML private Button buttonRefresh;

    // ── FXML — Content panels ──────────────────────────────────────
    @FXML private VBox   vboxPanelMap;
    @FXML private VBox   vboxPanelRegister;
    @FXML private VBox   vboxPanelActivity;

    // ── FXML — Map panel ───────────────────────────────────────────
    @FXML private WebView webViewMap;
    @FXML private StackPane stackPaneMapContainer;

    // Overlay
    @FXML private VBox   vboxMapOverlay;
    @FXML private Label  labelOverlayName;
    @FXML private Label  labelOverlayAddress;
    @FXML private VBox   vboxOverlayItemsBox;
    @FXML private FlowPane flowPaneOverlayPillsRow;
    @FXML private Label  labelOverlayEvent;
    @FXML private Label  labelOverlayTimestamp;
    @FXML private Button buttonUpdateCenter;
    @FXML private Button buttonOverlayClose;

    // Center cards strip
    @FXML private HBox   hboxCenterCardsRow;
    @FXML private Label  labelCenterCount;
    @FXML private ScrollPane scrollPaneCenterCards;

    // ── FXML — Register panel ──────────────────────────────────────
    @FXML private TableView<EvacueeRow>    tableViewEvacuees;
    @FXML private TableColumn<EvacueeRow,String> tableColumnEvacId;
    @FXML private TableColumn<EvacueeRow,String> tableColumnEvacName;
    @FXML private TableColumn<EvacueeRow,String> tableColumnEvacCenter;
    @FXML private TableColumn<EvacueeRow,String> tableColumnEvacBrgy;
    @FXML private TableColumn<EvacueeRow,String> tableColumnEvacFamily;
    @FXML private TableColumn<EvacueeRow,String> tableColumnEvacStatus;
    @FXML private TableColumn<EvacueeRow,String> tableColumnEvacAction;
    @FXML private Button buttonRegisterEvacuee;
    @FXML private Button buttonExportCsv;
    @FXML private TextField textFieldSearchEvacuees;
    @FXML private Button buttonFilterRegAll;
    @FXML private Button buttonFilterRegVerified;
    @FXML private Button buttonFilterRegPending;

    // ── FXML — Activity panel ─────────────────────────────────────
    @FXML private TableView<ActivityRow>   tableViewActivity;
    @FXML private TableColumn<ActivityRow,String> tableColumnActTime;
    @FXML private TableColumn<ActivityRow,String> tableColumnActAction;
    @FXML private TableColumn<ActivityRow,String> tableColumnActTarget;
    @FXML private TableColumn<ActivityRow,String> tableColumnActCenter;
    @FXML private TableColumn<ActivityRow,String> tableColumnActBy;
    @FXML private TextField textFieldSearchActivity;

    // ── State ──────────────────────────────────────────────────────
    private final List<CenterData> centers = new ArrayList<>();
    private CenterData selectedCenter = null;
    private final Map<Long, VBox> cardBycenterId = new HashMap<>();

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    // ── Simple row models ──────────────────────────────────────────

    public record EvacueeRow(
            String id, String name, String center,
            String barangay, String familySize, String status) {}

    public record ActivityRow(
            String time, String action, String target,
            String center, String by) {}

    public record CenterData(
            long id, String name, String address, String barangay,
            int capacity, int occupancy, double lat, double lng,
            // from center_status_updates (latest row)
            String eventLabel, List<String> availableItems,
            String updatedAt) {}

    // ══════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        loadCentersFromDB();
        setupMap();
        setupCenterCards();
        setupRegistrationTable();
        setupActivityTable();
        // Map is the default panel — already visible in FXML
    }

    // ══════════════════════════════════════════════════════════════
    //  SIDEBAR NAV
    // ══════════════════════════════════════════════════════════════

    @FXML private void handleNavMap() {
        showPanel(vboxPanelMap);
        setActiveNav(buttonNavMap);
        labelHeaderTitle.setText("Map View");
        labelHeaderSubtitle.setText("Live evacuation center status");
    }

    @FXML private void handleNavRegister() {
        showPanel(vboxPanelRegister);
        setActiveNav(buttonNavRegister);
        labelHeaderTitle.setText("Evacuee Registrations");
        labelHeaderSubtitle.setText("Register and manage evacuees in your barangay");
        loadEvacueesFromDB();
    }

    @FXML private void handleNavActivity() {
        showPanel(vboxPanelActivity);
        setActiveNav(buttonNavActivity);
        labelHeaderTitle.setText("Activity Log");
        labelHeaderSubtitle.setText("Recent actions and updates");
        loadActivityFromDB();
    }

    @FXML private void handleRefresh() {
        loadCentersFromDB();
        setupCenterCards();
        setupMap();
        if (selectedCenter != null) {
            // Re-find updated center and refresh overlay
            centers.stream()
                    .filter(c -> c.id() == selectedCenter.id())
                    .findFirst()
                    .ifPresent(this::showOverlay);
        }
    }

    private void showPanel(VBox target) {
        for (VBox p : List.of(vboxPanelMap, vboxPanelRegister, vboxPanelActivity)) {
            p.setVisible(p == target);
            p.setManaged(p == target);
        }
    }

    private void setActiveNav(Button active) {
        for (Button b : List.of(buttonNavMap, buttonNavRegister, buttonNavActivity)) {
            b.getStyleClass().remove("brgy-nav-active");
        }
        if (!active.getStyleClass().contains("brgy-nav-active"))
            active.getStyleClass().add("brgy-nav-active");
    }

    // ══════════════════════════════════════════════════════════════
    //  DATABASE — Centers
    // ══════════════════════════════════════════════════════════════

    private void loadCentersFromDB() {
        centers.clear();
        String sql = """
            SELECT
                ec.id, ec.name, ec.address, ec.barangay,
                ec.capacity, ec.current_occupancy,
                ec.latitude, ec.longitude,
                csu.event_label, csu.available_item_ids, csu.updated_at
            FROM evacuation_centers ec
            LEFT JOIN (
                SELECT center_id,
                       event_label, available_item_ids, updated_at,
                       ROW_NUMBER() OVER (PARTITION BY center_id ORDER BY updated_at DESC) AS rn
                FROM center_status_updates
            ) csu ON csu.center_id = ec.id AND csu.rn = 1
            WHERE ec.is_active = 1
            ORDER BY ec.name
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long id          = rs.getLong("id");
                String name      = rs.getString("name");
                String address   = rs.getString("address");
                String barangay  = rs.getString("barangay");
                int capacity     = rs.getInt("capacity");
                int occupancy    = rs.getInt("current_occupancy");
                double lat       = rs.getDouble("latitude");
                double lng       = rs.getDouble("longitude");

                String eventLabel     = rs.getString("event_label");
                if (eventLabel == null) eventLabel = "No active event";

                String itemJson       = rs.getString("available_item_ids");
                List<String> items    = resolveItemNames(itemJson);

                String updatedAtRaw   = rs.getString("updated_at");
                String updatedAt      = formatTimestamp(updatedAtRaw);

                centers.add(new CenterData(
                        id, name, address, barangay,
                        capacity, occupancy, lat, lng,
                        eventLabel, items, updatedAt));
            }
        } catch (SQLException e) {
            System.err.println("[BrgyDashboard] DB error loading centers: " + e.getMessage());
            // Graceful fallback — show empty state, don't crash
        }
    }

    /**
     * Given a JSON array string like "[1,3,4]", queries inventory_items
     * and returns the matching item names.
     */
    private List<String> resolveItemNames(String jsonArray) {
        if (jsonArray == null || jsonArray.isBlank() || jsonArray.equals("[]"))
            return List.of();

        // Simple parse: strip brackets, split by comma
        String stripped = jsonArray.trim().replaceAll("[\\[\\]\\s]", "");
        if (stripped.isEmpty()) return List.of();

        String[] parts = stripped.split(",");
        List<Long> ids = new ArrayList<>();
        for (String p : parts) {
            try { ids.add(Long.parseLong(p.trim())); }
            catch (NumberFormatException ignored) {}
        }
        if (ids.isEmpty()) return List.of();

        // Build IN clause
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT name, category FROM inventory_items WHERE id IN (" + placeholders + ")";

        List<String> names = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) ps.setLong(i + 1, ids.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) names.add(rs.getString("name"));
        } catch (SQLException e) {
            System.err.println("[BrgyDashboard] DB error resolving items: " + e.getMessage());
        }
        return names;
    }

    private String formatTimestamp(String raw) {
        if (raw == null) return "—";
        try {
            // MariaDB TIMESTAMP format: "2026-05-10 10:42:00"
            LocalDateTime dt = LocalDateTime.parse(raw.replace(" ", "T").substring(0, 19));
            return "Updated: " + dt.format(DISPLAY_FMT);
        } catch (Exception e) {
            return "Updated: " + raw;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  MAP
    // ══════════════════════════════════════════════════════════════

    private void setupMap() {
        webViewMap.getEngine().setJavaScriptEnabled(true);
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        webViewMap.getEngine().setUserAgent("CivicGuard/1.0");

        webViewMap.getEngine().setOnAlert(event ->
                System.out.println("[MAP-DEBUG] " + event.getData()));

        webViewMap.getEngine().getLoadWorker().stateProperty()
                .addListener((obs, old, state) -> {
                    if (state == Worker.State.SUCCEEDED) {
                        JSObject win = (JSObject) webViewMap.getEngine()
                                .executeScript("window");
                        // Install the bridge so JS can call back into Java
                        win.setMember("javaBridge", new BrgyMapBridge(this));
                    }
                });

        webViewMap.getEngine().loadContent(
                BrgyMapHtmlProvider.getMapHTML(buildCentersJson()));
    }

    private String buildCentersJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < centers.size(); i++) {
            CenterData c = centers.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format(
                    "{\"id\":%d,\"name\":\"%s\",\"lat\":%s,\"lng\":%s,\"status\":\"%s\"}",
                    c.id(), esc(c.name()), c.lat(), c.lng(),
                    capacityStatus(c)));
        }
        return sb.append("]").toString();
    }

    private String capacityStatus(CenterData c) {
        if (c.capacity() <= 0) return "OPEN";
        return ((double) c.occupancy() / c.capacity()) >= 1.0 ? "FULL" : "OPEN";
    }

    /** Called by the JS bridge (from FX thread via Platform.runLater) */
    public void onMarkerClicked(String centerId) {
        long id;
        try { id = Long.parseLong(centerId); }
        catch (NumberFormatException e) { return; }

        centers.stream()
                .filter(c -> c.id() == id)
                .findFirst()
                .ifPresent(this::showOverlay);

        highlightCard(id);
    }

    // ══════════════════════════════════════════════════════════════
    //  MAP OVERLAY
    // ══════════════════════════════════════════════════════════════

    private void showOverlay(CenterData c) {
        selectedCenter = c;

        labelOverlayName.setText(c.name());
        labelOverlayAddress.setText(c.address());
        labelOverlayEvent.setText(c.eventLabel());
        labelOverlayTimestamp.setText(c.updatedAt());

        // Rebuild pills
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

        // Wire update button to carry the selected center
        buttonUpdateCenter.setOnAction(e -> handleUpdateCenter());
    }

    /** Rough category detection from item name for pill colour */
    private String pillCategory(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("rice") || lower.contains("pancit") ||
                lower.contains("sardine") || lower.contains("food") || lower.contains("canton"))
            return "brgy-pill-food";
        if (lower.contains("water") || lower.contains("drink"))
            return "brgy-pill-water";
        if (lower.contains("blanket") || lower.contains("hygiene") ||
                lower.contains("kit") || lower.contains("cloth"))
            return "brgy-pill-nonfood";
        return "brgy-pill-other";
    }

    @FXML private void handleOverlayClose() {
        vboxMapOverlay.setVisible(false);
        vboxMapOverlay.setManaged(false);
        selectedCenter = null;
        // Deselect card
        cardBycenterId.values().forEach(v ->
                v.getStyleClass().remove("brgy-center-card-selected"));
    }

    @FXML private void handleUpdateCenter() {
        if (selectedCenter == null) return;
        // Phase 3: open "Update Center Info" modal
        // For now, print to console so you can see it's wired
        System.out.println("[BrgyDashboard] Update center: " + selectedCenter.name()
                + " (id=" + selectedCenter.id() + ")");
        // TODO: SceneHelper.showModal(...) with UpdateCenterController
    }

    // ══════════════════════════════════════════════════════════════
    //  CENTER CARDS STRIP
    // ══════════════════════════════════════════════════════════════

    private void setupCenterCards() {
        hboxCenterCardsRow.getChildren().clear();
        cardBycenterId.clear();
        labelCenterCount.setText(centers.size() + " center" + (centers.size() == 1 ? "" : "s"));

        for (CenterData c : centers) {
            VBox card = buildCenterCard(c);
            hboxCenterCardsRow.getChildren().add(card);
            cardBycenterId.put(c.id(), card);
        }
    }

    private VBox buildCenterCard(CenterData c) {
        VBox card = new VBox(6);
        card.getStyleClass().add("brgy-center-card");
        card.setPadding(new javafx.geometry.Insets(12, 14, 12, 14));

        // Name + badge row
        HBox nameRow = new HBox(8);
        nameRow.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        Label nameLabel = new Label(c.name());
        nameLabel.getStyleClass().add("brgy-card-center-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(150);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        String status = capacityStatus(c);
        Label badge = new Label(status);
        badge.getStyleClass().add("OPEN".equals(status) ? "brgy-badge-open" : "brgy-badge-full");

        nameRow.getChildren().addAll(nameLabel, badge);

        // Address
        Label addr = new Label(c.address());
        addr.getStyleClass().add("brgy-card-center-addr");
        addr.setWrapText(true);
        addr.setMaxWidth(196);

        // Capacity mini-bar
        StackPane track = new StackPane();
        track.getStyleClass().add("brgy-cap-track");
        track.setMinHeight(5); track.setMaxHeight(5);

        double pct = c.capacity() > 0 ? (double) c.occupancy() / c.capacity() : 0;
        pct = Math.min(pct, 1.0);
        Region fill = new Region();
        fill.getStyleClass().add(pct >= 1.0 ? "brgy-cap-fill-full"
                : pct >= 0.75 ? "brgy-cap-fill-warn" : "brgy-cap-fill-ok");
        fill.setPrefWidth(196 * pct);
        fill.setMinWidth(0);
        track.getChildren().add(fill);
        StackPane.setAlignment(fill, javafx.geometry.Pos.CENTER_LEFT);

        Label capLabel = new Label(c.occupancy() + " / " + c.capacity() + " evacuees");
        capLabel.setStyle("-fx-font-size: 9; -fx-text-fill: #94a3b8;");

        // Event
        Label eventLabel = new Label("📌 " + c.eventLabel());
        eventLabel.getStyleClass().add("brgy-card-center-event");
        eventLabel.setWrapText(true);
        eventLabel.setMaxWidth(196);

        // Timestamp
        Label ts = new Label(c.updatedAt());
        ts.getStyleClass().add("brgy-card-center-ts");

        card.getChildren().addAll(nameRow, addr, track, capLabel, eventLabel, ts);

        // Click — show overlay + highlight marker in JS
        card.setOnMouseClicked(e -> {
            showOverlay(c);
            highlightCard(c.id());
            triggerMapMarkerHighlight(c.id());
        });

        return card;
    }

    private void highlightCard(long centerId) {
        cardBycenterId.forEach((id, card) -> {
            card.getStyleClass().remove("brgy-center-card-selected");
            if (id == centerId) {
                if (!card.getStyleClass().contains("brgy-center-card-selected"))
                    card.getStyleClass().add("brgy-center-card-selected");
                // Scroll to this card
                scrollCardIntoView(card);
            }
        });
    }

    private void scrollCardIntoView(VBox card) {
        // Compute rough horizontal scroll position
        Platform.runLater(() -> {
            double cardX = card.getBoundsInParent().getMinX();
            double scrollWidth = scrollPaneCenterCards.getContent().getBoundsInLocal().getWidth()
                    - scrollPaneCenterCards.getViewportBounds().getWidth();
            if (scrollWidth > 0) {
                double h = Math.max(0, Math.min(1.0, (cardX - 20) / scrollWidth));
                scrollPaneCenterCards.setHvalue(h);
            }
        });
    }

    /** Calls window.highlightMarker(id) in the Leaflet map */
    private void triggerMapMarkerHighlight(long centerId) {
        try {
            webViewMap.getEngine()
                    .executeScript("window.highlightMarker(" + centerId + ")");
        } catch (Exception ignored) {}
    }

    // ══════════════════════════════════════════════════════════════
    //  DATABASE — Evacuees (Phase 3 will flesh out the form)
    // ══════════════════════════════════════════════════════════════

    private void setupRegistrationTable() {
        tableColumnEvacId.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().id()));
        tableColumnEvacName.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().name()));
        tableColumnEvacCenter.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().center()));
        tableColumnEvacBrgy.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().barangay()));
        tableColumnEvacFamily.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().familySize()));
        tableColumnEvacStatus.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().status()));
        tableColumnEvacStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label lbl = new Label(s);
                lbl.getStyleClass().add(switch (s.toLowerCase()) {
                    case "verified" -> "brgy-status-verified";
                    case "rejected" -> "brgy-status-rejected";
                    default         -> "brgy-status-pending";
                });
                setGraphic(lbl); setText(null);
            }
        });
        tableColumnEvacAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("View");
            { btn.getStyleClass().add("brgy-tbl-action"); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setText(null);
            }
        });
    }

    private void loadEvacueesFromDB() {
        String sql = """
            SELECT e.id,
                   e.full_name_enc,
                   ec.name AS center_name,
                   e.barangay,
                   COALESCE(fg.member_count, 1) AS family_size,
                   e.verification_status
            FROM evacuees e
            LEFT JOIN evacuation_centers ec ON ec.id = e.evacuation_center_id
            LEFT JOIN family_groups fg ON fg.id = e.family_group_id
            ORDER BY e.created_at DESC
            LIMIT 200
            """;

        javafx.collections.ObservableList<EvacueeRow> rows =
                javafx.collections.FXCollections.observableArrayList();

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new EvacueeRow(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("full_name_enc"),
                        rs.getString("center_name"),
                        rs.getString("barangay"),
                        String.valueOf(rs.getInt("family_size")),
                        rs.getString("verification_status")));
            }
        } catch (SQLException e) {
            System.err.println("[BrgyDashboard] DB error loading evacuees: " + e.getMessage());
        }
        tableViewEvacuees.setItems(rows);
    }

    @FXML private void handleRegisterEvacuee() {
        // Phase 3: open registration modal
        System.out.println("[BrgyDashboard] Open register evacuee modal");
        // TODO: SceneHelper.showModal(...)
    }

    // ══════════════════════════════════════════════════════════════
    //  DATABASE — Activity Log
    // ══════════════════════════════════════════════════════════════

    private void setupActivityTable() {
        tableColumnActTime.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().time()));
        tableColumnActAction.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().action()));
        tableColumnActTarget.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().target()));
        tableColumnActCenter.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().center()));
        tableColumnActBy.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().by()));
    }

    private void loadActivityFromDB() {
        String sql = """
            SELECT
                al.timestamp,
                al.action,
                al.target_type,
                al.target_id,
                u.display_name
            FROM activity_log al
            LEFT JOIN users u ON u.id = al.user_id
            ORDER BY al.timestamp DESC
            LIMIT 100
            """;

        javafx.collections.ObservableList<ActivityRow> rows =
                javafx.collections.FXCollections.observableArrayList();

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new ActivityRow(
                        formatTimestamp(rs.getString("timestamp")),
                        rs.getString("action"),
                        rs.getString("target_type") + " #" + rs.getLong("target_id"),
                        "—",  // center resolution in a follow-up query if needed
                        rs.getString("display_name")));
            }
        } catch (SQLException e) {
            System.err.println("[BrgyDashboard] DB error loading activity: " + e.getMessage());
        }
        tableViewActivity.setItems(rows);
    }

    // ══════════════════════════════════════════════════════════════
    //  UTILITY
    // ══════════════════════════════════════════════════════════════

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");
    }

    // ══════════════════════════════════════════════════════════════
    //  JS → Java bridge (inner class — must be public for JSObject)
    // ══════════════════════════════════════════════════════════════

    public static class BrgyMapBridge {
        private final BrgyDashboardController ctrl;
        public BrgyMapBridge(BrgyDashboardController ctrl) { this.ctrl = ctrl; }

        /** Called from JavaScript when a marker is clicked. */
        public void onMarkerClick(String centerId) {
            Platform.runLater(() -> ctrl.onMarkerClicked(centerId));
        }
    }
}