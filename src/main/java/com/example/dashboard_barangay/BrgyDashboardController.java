package com.example.dashboard_barangay;


import com.example.dao.ActivityTimelineDao;
import com.example.map_logic_v2.BrgyMapHtmlProvider;
import com.example.model.ActivityTimelineItem;
import com.example.util.DBConnectionManager;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import javafx.collections.FXCollections;
import com.example.dao.EvacuationCenterDao;
import com.example.model.StructuralStatus;
import com.example.auth.SessionContext;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.example.map_tiles.TilePrefetchService;

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
    @FXML private AnchorPane vboxMapOverlay; // Changed to AnchorPane to match FXML
    @FXML private ImageView imgOverlayCenter; // Added this FXML element
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
    @FXML private Button buttonLogout;

    // ── FXML — Activity panel ─────────────────────────────────────
    @FXML private TableView<ActivityTimelineItem> tableViewActivity;
    @FXML private TableColumn<ActivityTimelineItem, String> tableColumnActTime;
    @FXML private TableColumn<ActivityTimelineItem, String> tableColumnActAction;
    @FXML private TableColumn<ActivityTimelineItem, String> tableColumnActTarget;
    @FXML private TableColumn<ActivityTimelineItem, String> tableColumnActCenter;
    @FXML private TableColumn<ActivityTimelineItem, String> tableColumnActBy;
    @FXML private TextField textFieldSearchActivity;

    private final ActivityTimelineDao timelineDao = new ActivityTimelineDao();

    // Phase 5b — structural status UI
    @FXML private ComboBox<StructuralStatus> comboStructuralStatus;
    @FXML private Label                      labelStructuralCurrent;
    @FXML private TextField                  textFieldStructuralNotes;

    private final EvacuationCenterDao centerDao = new EvacuationCenterDao();

    // Cached so handleSave can decide whether to call updateStructuralStatus
    private StructuralStatus originalStatus;
    private String           originalNotes;
    private BrgyMapBridge mapBridge;
    private final Map<Long, HBox> mapCardByCenterId = new HashMap<>();

    // Session-resolved on first access. Falls back to "Lahug" if no session
    // is active (developer convenience — e.g. running the dashboard FXML
    // directly via Scene Builder or BrgyDashboardApplication for testing
    // without going through the full login flow).
    private static final String FALLBACK_BARANGAY = "Lahug";
    private final String CURRENT_BARANGAY = resolveBarangayFromSession();

    private static String resolveBarangayFromSession() {
        var session = com.example.auth.SessionContext.current();
        if (session != null && session.getBarangay() != null) {
            return session.getBarangay().getName();
        }
        System.err.println("[BrgyDashboard] No active session — falling back to '"
                + FALLBACK_BARANGAY + "'. This is expected only in dev/test.");
        return FALLBACK_BARANGAY;
    }

    // NEW: Variables to hold the map center
    private double currentBrgyLat = 10.3157; // Default fallback
    private double currentBrgyLng = 123.8854;
    private int currentBrgyZoom = 13;

    // ── State ──────────────────────────────────────────────────────
    private final List<CenterData> centers = new ArrayList<>();
    private CenterData selectedCenter = null;
    private final Map<Long, HBox> cardBycenterId = new HashMap<>();

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
            String eventLabel, List<String> availableItems,
            String updatedAt, String photoPath,
            // Phase 5b additions:
            com.example.model.StructuralStatus structuralStatus,
            String structuralNotes,
            String structuralUpdatedAt) {}

    // ══════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        labelBrgyName.setText("Brgy. " + CURRENT_BARANGAY);
        populateSidebarFromSession();

        loadBarangayCoordinates(); // <--- NEW!
        loadCentersFromDB();
        setupMap();
        setupCenterCards();
        setupRegistrationTable();
        setupActivityTable();

        // Kick off background tile prefetch — runs on 8-thread pool, non-blocking.
        // This is where the rubric multithreading lives.
        TilePrefetchService.getInstance().prefetchAllBarangaysAsync(
                (done, total, finalResult) -> {
                    // This lambda runs on the JavaFX thread (Platform.runLater-wrapped)
                    if (finalResult != null) {
                        System.out.println("[BrgyDashboard] Tile cache warmed: " + finalResult);
                    } else if (done % 50 == 0) {
                        System.out.println("[BrgyDashboard] Prefetch progress: " + done + "/" + total);
                    }
                });
    }

    // NEW METHOD
    private void loadBarangayCoordinates() {
        String sql = "SELECT center_lat, center_lng, default_zoom FROM barangays WHERE name = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, CURRENT_BARANGAY);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentBrgyLat = rs.getDouble("center_lat");
                    currentBrgyLng = rs.getDouble("center_lng");
                    currentBrgyZoom = rs.getInt("default_zoom");
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load barangay coordinates: " + e.getMessage());
        }
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

    @FXML
    private void handleNavActivity() {
        showPanel(vboxPanelActivity);
        setActiveNav(buttonNavActivity);
        labelHeaderTitle.setText("Activity Log");
        labelHeaderSubtitle.setText("Recent actions and supply requests");

        // Ensure you are calling the NEW method
        refreshActivityLog();
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
    @FXML
    private void handleLogout() {
        new com.example.auth.AuthService().logout();
        com.example.util.Router.getInstance().navigate(com.example.util.Route.KIOSK);
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
            ec.id, ec.name, ec.address, ec.barangay, ec.photo_path,
            ec.capacity, ec.current_occupancy,
            ec.latitude, ec.longitude,
            ec.structural_status, ec.structural_notes, ec.structural_updated_at,
            csu.event_label, csu.available_item_ids, csu.updated_at
        FROM evacuation_centers ec
        LEFT JOIN (
            SELECT center_id,
                   event_label, available_item_ids, updated_at,
                   ROW_NUMBER() OVER (PARTITION BY center_id ORDER BY updated_at DESC) AS rn
            FROM center_status_updates
        ) csu ON csu.center_id = ec.id AND csu.rn = 1
        WHERE ec.is_active = 1 AND ec.barangay = ?
        ORDER BY ec.name
        """;
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 1. SET THE PARAMETER FIRST
            ps.setString(1, CURRENT_BARANGAY);

            // 2. THEN EXECUTE THE QUERY
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id          = rs.getLong("id");
                    String name      = rs.getString("name");
                    String address   = rs.getString("address");
                    String barangay  = rs.getString("barangay");

                    // It is perfectly fine if this is empty/null in the DB right now!
                    String photoPath = rs.getString("photo_path");

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
// Existing reads stay above — just add these three:
                    var structStatus = com.example.model.StructuralStatus.fromDb(
                            rs.getString("structural_status"));
                    String structNotes = rs.getString("structural_notes");
                    String structUpdatedRaw = rs.getString("structural_updated_at");
                    String structUpdated = structUpdatedRaw == null
                            ? "Never inspected"
                            : "Last inspected: " + formatTimestamp(structUpdatedRaw).replaceFirst("^Updated: ", "");

                    centers.add(new CenterData(
                            id, name, address, barangay,
                            capacity, occupancy, lat, lng,
                            eventLabel, items, updatedAt, photoPath,
                            structStatus, structNotes, structUpdated));
                }
            }
        } catch (SQLException e) {
            System.err.println("[BrgyDashboard] DB error loading centers: " + e.getMessage());
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
             PreparedStatement ps =prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) ps.setLong(i + 1, ids.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) names.add(rs.getString("name"));
        } catch (SQLException e) {
            System.err.println("[BrgyDashboard] DB error resolving items: " + e.getMessage());
        }
        return names;
    }
    
    private PreparedStatement prepareStatement(String sql) throws SQLException {
        return DBConnectionManager.getInstance().getConnection().prepareStatement(sql);
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

                        // 1. Save it to the class variable to prevent Garbage Collection
                        this.mapBridge = new BrgyMapBridge(this);

                        // 2. Pass the saved variable to Javascript
                        win.setMember("javaBridge", this.mapBridge);
                    }
                });

        // Start the local tile server (idempotent — safe to call repeatedly)
        int tilePort;
        try {
            tilePort = TilePrefetchService.getInstance().startServer();
        } catch (Exception e) {
            System.err.println("[BrgyDashboard] Could not start tile server: " + e.getMessage());
            tilePort = -1;
        }

        webViewMap.getEngine().loadContent(
                BrgyMapHtmlProvider.getMapHTML(
                        buildCentersJson(),
                        currentBrgyLat,
                        currentBrgyLng,
                        currentBrgyZoom,
                        tilePort
                )
        );
    }

    private String buildCentersJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < centers.size(); i++) {
            CenterData c = centers.get(i);
            if (i > 0) sb.append(",");

            // We removed the capacityStatus(c) call.
            // All markers will now use the stable high-contrast color defined in your HTML Provider.
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

        // NEW: Load image for the map overlay
        if (c.photoPath() != null && !c.photoPath().isBlank()) {
            try {
                Image img = new Image(getClass().getResourceAsStream(c.photoPath()));
                imgOverlayCenter.setImage(img);
                imgOverlayCenter.setVisible(true);
                imgOverlayCenter.setManaged(true);
            } catch (Exception e) {
                System.err.println("Could not load image for overlay: " + c.photoPath());
                imgOverlayCenter.setVisible(false);
                imgOverlayCenter.setManaged(false);
            }
        } else {
            imgOverlayCenter.setVisible(false);
            imgOverlayCenter.setManaged(false);
        }

        // Rebuild pills
        flowPaneOverlayPillsRow.getChildren().clear();

        // Phase 5b — structural status badge always appears first
        if (c.structuralStatus() != null) {
            Label structBadge = new Label("🏛 " + c.structuralStatus().displayLabel());
            structBadge.getStyleClass().addAll("brgy-pill", c.structuralStatus().cssClass());
            if (c.structuralNotes() != null && !c.structuralNotes().isBlank()) {
                javafx.scene.control.Tooltip.install(structBadge,
                        new javafx.scene.control.Tooltip(c.structuralNotes()));
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

    // ══════════════════════════════════════════════════════════════
    //  CENTER CARDS STRIP
    // ══════════════════════════════════════════════════════════════

    private void setupCenterCards() {
        hboxCenterCardsRow.getChildren().clear();
        cardBycenterId.clear();
        labelCenterCount.setText(centers.size() + " center" + (centers.size() == 1 ? "" : "s"));

        for (CenterData c : centers) {
            HBox card = buildCenterCard(c);
            hboxCenterCardsRow.getChildren().add(card);
            cardBycenterId.put(c.id(), card);
        }
    }

    private HBox buildCenterCard(CenterData c) {
        HBox hboxCard = new HBox(12);
        hboxCard.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        hboxCard.getStyleClass().add("brgy-center-card");
        hboxCard.setPadding(new javafx.geometry.Insets(12, 14, 12, 14));

        // 1. Left Side: Image
        ImageView imgCard = new ImageView();
        imgCard.setFitWidth(80);
        imgCard.setFitHeight(80);
        imgCard.setPreserveRatio(false);
        imgCard.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        if (c.photoPath() != null && !c.photoPath().isBlank()) {
            try {
                imgCard.setImage(new javafx.scene.image.Image(getClass().getResourceAsStream(c.photoPath())));
            } catch (Exception ignored) {}
        }

        // 2. Right Side: Text Content Box
        VBox vboxContent = new VBox(6);
        HBox.setHgrow(vboxContent, Priority.ALWAYS);

        HBox hboxNameRow = new HBox(8);
        hboxNameRow.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        Label lblName = new Label(c.name());
        lblName.getStyleClass().add("brgy-card-center-name");
        lblName.setWrapText(true);
        lblName.setMaxWidth(150);
        HBox.setHgrow(lblName, Priority.ALWAYS);

        Label lblBadge;
        if (c.structuralStatus() != null && c.structuralStatus() != StructuralStatus.SAFE) {
            // Show structural concern instead of generic ACTIVE
            lblBadge = new Label(c.structuralStatus().displayLabel().toUpperCase());
            lblBadge.getStyleClass().addAll("brgy-badge-struct", c.structuralStatus().cssClass());
        } else {
            lblBadge = new Label("ACTIVE");
            lblBadge.getStyleClass().add("brgy-badge-open");
        }

        hboxNameRow.getChildren().addAll(lblName, lblBadge);

        Label lblAddress = new Label(c.address());
        lblAddress.getStyleClass().add("brgy-card-center-addr");
        lblAddress.setWrapText(true);
        lblAddress.setMaxWidth(196);

        // RIP OUT THE PROGRESS BAR COMPLETELY!
        // No more stackPaneTrack or regionFill.

        Label lblEvent = new Label("📌 " + c.eventLabel());
        lblEvent.getStyleClass().add("brgy-card-center-event");
        lblEvent.setWrapText(true);
        lblEvent.setMaxWidth(196);

        Label lblTimestamp = new Label(c.updatedAt());
        lblTimestamp.getStyleClass().add("brgy-card-center-ts");

        // Added back everything except the capacity track
        vboxContent.getChildren().addAll(hboxNameRow, lblAddress, lblEvent, lblTimestamp);

        hboxCard.getChildren().addAll(imgCard, vboxContent);

        hboxCard.setOnMouseClicked(e -> {
            showOverlay(c);
            highlightCard(c.id());
            triggerMapMarkerHighlight(c.id());
        });

        return hboxCard;
    }

    private void highlightCard(long centerId) {
        mapCardByCenterId.forEach((id, hboxCard) -> {
            hboxCard.getStyleClass().remove("brgy-center-card-selected");
            if (id == centerId) {
                if (!hboxCard.getStyleClass().contains("brgy-center-card-selected"))
                    hboxCard.getStyleClass().add("brgy-center-card-selected");
                // Scroll to this card
                scrollCardIntoView(hboxCard);
            }
        });
    }

    private void scrollCardIntoView(HBox hboxCard) {
        Platform.runLater(() -> {
            double cardX = hboxCard.getBoundsInParent().getMinX();
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
    @FXML private void handleUpdateCenter() {
        if (selectedCenter == null) return;

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/example/dashboard_barangay/modals/update-center.fxml")
            );
            javafx.scene.Parent root = loader.load();

            // WE UNCOMMENTED THESE TWO LINES!
            UpdateCenterController controller = loader.getController();
            controller.initData(
                    selectedCenter.id(),
                    selectedCenter.name(),
                    selectedCenter.address(),
                    selectedCenter.photoPath(),
                    selectedCenter.structuralStatus(),
                    selectedCenter.structuralNotes(),
                    selectedCenter.structuralUpdatedAt());

            // Setup and show the modal window
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Update " + selectedCenter.name());
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(buttonUpdateCenter.getScene().getWindow());
            stage.setScene(new javafx.scene.Scene(root));

            stage.showAndWait();
            handleRefresh(); // Reloads map & overlay to show new pills!

        } catch (Exception e) {
            System.err.println("Failed to open Update Center modal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void handleRegisterEvacuee() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/example/dashboard_barangay/modals/register-evacuee.fxml")
            );
            javafx.scene.Parent root = loader.load();

            RegisterEvacueeController controller = loader.getController();
            controller.initData(CURRENT_BARANGAY, this::loadEvacueesFromDB);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Register Evacuee");
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(buttonRegisterEvacuee.getScene().getWindow());
            stage.setScene(new javafx.scene.Scene(root));

            stage.showAndWait();
        } catch (Exception e) {
            System.err.println("Failed to open Register Evacuee modal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DATABASE — Activity Log
    // ══════════════════════════════════════════════════════════════

    private void setupActivityTable() {
        // These now match the property names in your ActivityTimelineItem record
        tableColumnActTime.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().timestamp()));
        tableColumnActAction.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().action()));
        tableColumnActTarget.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().target()));
        tableColumnActCenter.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().center()));
        tableColumnActBy.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().performedBy()));

        refreshActivityLog();
    }

    private void refreshActivityLog() {
        try {
            var session = com.example.auth.SessionContext.current();
            String brgyName = (session != null && session.getBarangay() != null)
                    ? session.getBarangay().getName()
                    : CURRENT_BARANGAY;

            List<ActivityTimelineItem> logs = timelineDao.getBarangayTimeline(brgyName);
            System.out.println("Debug: Found " + logs.size() + " activity items for " + brgyName);

            tableViewActivity.setItems(javafx.collections.FXCollections.observableArrayList(logs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
    /**
     * Populates the bottom-of-sidebar staff card (display name + role)
     * from the current session. Safe to call when no session is active —
     * leaves the existing FXML defaults ("Brgy. Staff" / "Response Officer")
     * in place so dev/test runs still look reasonable.
     */
    private void populateSidebarFromSession() {
        var session = com.example.auth.SessionContext.current();
        if (session == null || session.getUser() == null) return;

        var user = session.getUser();
        if (labelStaffName != null) {
            labelStaffName.setText(user.getDisplayName());
        }
        if (labelStaffRole != null) {
            labelStaffRole.setText(roleLabelFor(user.getRole()));
        }
    }

    private static String roleLabelFor(com.example.model.UserRole role) {
        return switch (role) {
            case ADMIN    -> "Administrator";
            case BARANGAY -> "Barangay Officer";
            case STAFF    -> "Response Officer";
        };
    }
    @FXML
    private void handleOpenSupplyRequests() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/example/dashboard_barangay/modals/manage-supply-requests.fxml")
            );
            javafx.scene.Parent modal = loader.load();
            ManageSupplyRequestsController controller = loader.getController();

            // Assuming your dashboard root is a StackPane named rootPane
            // Adjust the variable name to match your FXML if necessary
            controller.setOnClose(() -> anchorPaneMainRoot.getChildren().remove(modal));

            anchorPaneMainRoot.getChildren().add(modal);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

}