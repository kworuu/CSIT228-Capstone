package com.example.dashboard_barangay;

import com.example.dao.ActivityTimelineDao;
import com.example.dao.EvacuationCenterDao;
import com.example.dao.EvacueeDao;
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
import javafx.scene.web.WebView;
import javafx.util.StringConverter; // FIXED IMPORT
import netscape.javascript.JSObject;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.example.map_tiles.TilePrefetchService;

/**
 * Controller for BrgyDashboard.fxml
 */
public class BrgyDashboardController {

    // ── FXML — Sidebar ─────────────────────────────────────────────
    @FXML private AnchorPane anchorPaneMainRoot;
    @FXML private Button buttonNavMap;
    @FXML private Button buttonNavRegister;
    @FXML private Button buttonNavActivity;
    @FXML private Label  labelBrgyName;
    @FXML private Label  labelStaffName;
    @FXML private Label  labelStaffRole;

    // ── FXML — Header ──────────────────────────────────────────────
    @FXML private Label  labelHeaderTitle;
    @FXML private Label  labelHeaderSubtitle;
    @FXML private Button buttonAddCenter;

    // ── FXML — Content panels ──────────────────────────────────────
    @FXML private VBox   vboxPanelMap;
    @FXML private VBox   vboxPanelRegister;
    @FXML private VBox   vboxPanelActivity;

    // ── FXML — Map panel ───────────────────────────────────────────
    @FXML private WebView webViewMap;

    // Overlay
    @FXML private AnchorPane vboxMapOverlay;
    @FXML private ImageView imgOverlayCenter;
    @FXML private Label  labelOverlayName;
    @FXML private Label  labelOverlayAddress;
    @FXML private FlowPane flowPaneOverlayPillsRow;
    @FXML private Label  labelOverlayEvent;
    @FXML private Label  labelOverlayTimestamp;
    @FXML private Button buttonUpdateCenter;
    @FXML private Button buttonViewRoster;
    @FXML private Button buttonDeleteCenter; // NEW

    // Center cards strip
    @FXML private HBox   hboxCenterCardsRow;
    @FXML private Label  labelCenterCount;
    @FXML private ScrollPane scrollPaneCenterCards;

    // FXML — Register panel
    @FXML private TableView<EvacueeRow>    tableViewEvacuees;
    @FXML private TableColumn<EvacueeRow,String> tableColumnEvacId;
    @FXML private TableColumn<EvacueeRow,String> tableColumnEvacName;
    @FXML private TableColumn<EvacueeRow,String> tableColumnEvacCenter;
    @FXML private TableColumn<EvacueeRow,String> tableColumnEvacBrgy;
    @FXML private TableColumn<EvacueeRow, Void> tableColumnEvacAction; // NEW!
    @FXML private Button buttonRegisterEvacuee;
    @FXML private Button btnReturnHome;

    // ── FXML — Activity panel ─────────────────────────────────────
    @FXML private TableView<ActivityTimelineItem> tableViewActivity;
    @FXML private TableColumn<ActivityTimelineItem, String> tableColumnActTime;
    @FXML private TableColumn<ActivityTimelineItem, String> tableColumnActAction;
    @FXML private TableColumn<ActivityTimelineItem, String> tableColumnActTarget;
    @FXML private TableColumn<ActivityTimelineItem, String> tableColumnActCenter;
    @FXML private TableColumn<ActivityTimelineItem, String> tableColumnActBy;

    private final ActivityTimelineDao timelineDao = new ActivityTimelineDao();
    private BrgyMapBridge mapBridge;
    private final Map<Long, HBox> mapCardByCenterId = new HashMap<>();

    private static final String FALLBACK_BARANGAY = "Lahug";
    private final String CURRENT_BARANGAY = resolveBarangayFromSession();

    private static String resolveBarangayFromSession() {
        var session = com.example.auth.SessionContext.current();
        if (session != null && session.getUser() != null) {
            return session.getUser().displayName();
        }
        System.err.println("[BrgyDashboard] No active session — falling back to '"
                + FALLBACK_BARANGAY + "'. This is expected only in dev/test.");
        return FALLBACK_BARANGAY;
    }

    private double currentBrgyLat = 10.3157;
    private double currentBrgyLng = 123.8854;
    private int currentBrgyZoom = 13;

    private final List<CenterData> centers = new ArrayList<>();
    private CenterData selectedCenter = null;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    // ── Simple row models ──────────────────────────────────────────

    public record EvacueeRow(
            String id, String name, String center, String barangay) {}

    public record CenterData(
            long id, String name, String address, String barangay,
            double lat, double lng, String photoPath,
            String eventLabel, List<String> availableItems, String updatedAt) {
        @Override
        public String toString() {
            return name;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        labelBrgyName.setText(CURRENT_BARANGAY);
        populateSidebarFromSession();

        loadBarangayCoordinates();
        loadCentersFromDB();
        setupMap();
        setupCenterCards();
        setupRegistrationTable();
        setupActivityTable();

        TilePrefetchService.getInstance().prefetchAllBarangaysAsync(
                (done, total, finalResult) -> {
                    if (finalResult != null) {
                        System.out.println("[BrgyDashboard] Tile cache warmed: " + finalResult);
                    } else if (done % 50 == 0) {
                        System.out.println("[BrgyDashboard] Prefetch progress: " + done + "/" + total);
                    }
                });
    }

    private void loadBarangayCoordinates() {
        String sql = "SELECT display_name AS name, latitude, longitude, zoom FROM users WHERE role = 'barangay' AND latitude IS NOT NULL";

        try (java.sql.Connection conn = com.example.util.DBConnectionManager.getInstance().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                double lat = rs.getDouble("latitude");
                double lng = rs.getDouble("longitude");

                // Keep whatever your code does with these variables next!
                // For example: mapBridge.addBarangayMarker(name, lat, lng);
            }
        } catch (java.sql.SQLException e) {
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
        refreshActivityLog();
    }

    @FXML private void handleRefresh() {
        loadCentersFromDB();
        setupCenterCards();
        setupMap();
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
        btnReturnHome.setVisible(false);
        try {
            webViewMap.getEngine().executeScript("if(window.flyHome) window.flyHome();");
        } catch (Exception e) {
            System.err.println("Error returning home: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddCenter() {
        // PHASE 1: Turn on Pinning Mode instead of opening the modal immediately
        try {
            webViewMap.getEngine().executeScript("window.enablePinningMode();");
            
            // Show a quick instruction to the user
            Alert info = new Alert(Alert.AlertType.INFORMATION, "Click anywhere on the map to drop the pin for the new evacuation center.", ButtonType.OK);
            info.setTitle("Pin Location");
            info.setHeaderText("Map Pinning Mode Active");
            info.show();
        } catch (Exception e) {
            System.err.println("Map script error: " + e.getMessage());
        }
    }

    // PHASE 1: Opens the modal AFTER they click the map
    public void openAddCenterModal(double lat, double lng) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/example/dashboard_barangay/modals/add-brgyReg.fxml")
            );
            javafx.scene.Parent root = loader.load();

            // Pass the coordinates to the old modal controller
            AddBrgyController controller = loader.getController();
            if(controller != null) {
                controller.setLocationData(CURRENT_BARANGAY, lat, lng);
            }

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Register Evacuation Center");
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(buttonAddCenter.getScene().getWindow());
            stage.setScene(new javafx.scene.Scene(root));

            stage.showAndWait();
            handleRefresh(); // Refresh map after saving
        } catch (Exception e) {
            System.err.println("Failed to open Register Center modal: " + e.getMessage());
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
            ec.id, ec.name, ec.address, u.display_name as barangay, ec.photo_path,
            ec.latitude, ec.longitude,
            csu.event_label, csu.available_item_ids, csu.updated_at
        FROM evacuation_centers ec
        JOIN users u ON ec.user_id = u.id
        LEFT JOIN (
            SELECT center_id,
                   event_label, available_item_ids, updated_at,
                   ROW_NUMBER() OVER (PARTITION BY center_id ORDER BY updated_at DESC) AS rn
            FROM center_status_updates
        ) csu ON csu.center_id = ec.id AND csu.rn = 1
        WHERE u.display_name = ?
        ORDER BY ec.name
        """;
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, CURRENT_BARANGAY);

            try (ResultSet rs = ps.executeQuery()) {
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
                    List<String> items = resolveItemNames(itemJson);

                    String updatedAtRaw = rs.getString("updated_at");
                    String updatedAt = formatTimestamp(updatedAtRaw);

                    centers.add(new CenterData(
                            id, name, address, barangay,
                            lat, lng, photoPath,
                            eventLabel, items, updatedAt));
                }
            }
        } catch (SQLException e) {
            System.err.println("[BrgyDashboard] DB error loading centers: " + e.getMessage());
        }
    }

    private List<String> resolveItemNames(String jsonArray) {
        if (jsonArray == null || jsonArray.isBlank() || jsonArray.equals("[]"))
            return List.of();

        String stripped = jsonArray.trim().replaceAll("[\\[\\]\\s]", "");
        if (stripped.isEmpty()) return List.of();

        String[] parts = stripped.split(",");
        List<Long> ids = new ArrayList<>();
        for (String p : parts) {
            try { ids.add(Long.parseLong(p.trim())); }
            catch (NumberFormatException ignored) {}
        }
        if (ids.isEmpty()) return List.of();

        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT name FROM inventory_items WHERE id IN (" + placeholders + ")";

        List<String> names = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) ps.setLong(i + 1, ids.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) names.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("[BrgyDashboard] DB error resolving items: " + e.getMessage());
        }
        return names;
    }

    private String formatTimestamp(String raw) {
        if (raw == null) return "—";
        try {
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
                .addListener((_obs, _old, state) -> {
                    if (state == Worker.State.SUCCEEDED) {
                        JSObject win = (JSObject) webViewMap.getEngine()
                                .executeScript("window");
                        this.mapBridge = new BrgyMapBridge(this);
                        win.setMember("javaBridge", this.mapBridge);
                    }
                });

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

        buttonUpdateCenter.setOnAction(_e -> handleUpdateCenter());

        buttonViewRoster.setOnAction(_e -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/com/example/dashboard_barangay/modals/roster-modal.fxml")
                );
                javafx.scene.Parent modal = loader.load();

                RosterModalController controller = loader.getController();
                controller.initData(c.id(), c.name());

                controller.setOnClose(() -> anchorPaneMainRoot.getChildren().remove(modal));

                anchorPaneMainRoot.getChildren().add(modal);
            } catch (Exception ex) {
                System.err.println("Failed to open Roster Modal: " + ex.getMessage());
            }
        });

        buttonDeleteCenter.setOnAction(_e -> handleDeleteCenter(c.id()));
    }

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
        mapCardByCenterId.values().forEach(v ->
                v.getStyleClass().remove("brgy-center-card-selected"));
    }

    // ══════════════════════════════════════════════════════════════
    //  CENTER CARDS STRIP
    // ══════════════════════════════════════════════════════════════

    private void setupCenterCards() {
        hboxCenterCardsRow.getChildren().clear();
        mapCardByCenterId.clear();
        labelCenterCount.setText(centers.size() + " center" + (centers.size() == 1 ? "" : "s"));

        for (CenterData c : centers) {
            HBox card = buildCenterCard(c);
            hboxCenterCardsRow.getChildren().add(card);
            mapCardByCenterId.put(c.id(), card);
        }
    }

    private HBox buildCenterCard(CenterData c) {
        HBox hboxCard = new HBox(12);
        hboxCard.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        hboxCard.getStyleClass().add("brgy-center-card");
        hboxCard.setPadding(new javafx.geometry.Insets(12, 14, 12, 14));

        ImageView imgCard = new ImageView();
        imgCard.setFitWidth(80);
        imgCard.setFitHeight(80);
        imgCard.setPreserveRatio(false);
        imgCard.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        if (c.photoPath() != null && !c.photoPath().isBlank()) {
            try {
                imgCard.setImage(new Image(getClass().getResourceAsStream(c.photoPath())));
            } catch (Exception ignored) {}
        }

        VBox vboxContent = new VBox(6);
        HBox.setHgrow(vboxContent, Priority.ALWAYS);

        HBox hboxNameRow = new HBox(8);
        hboxNameRow.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        Label lblName = new Label(c.name());
        lblName.getStyleClass().add("brgy-card-center-name");
        lblName.setWrapText(true);
        lblName.setMaxWidth(150);
        HBox.setHgrow(lblName, Priority.ALWAYS);

        Label lblBadge = new Label("ACTIVE");
        lblBadge.getStyleClass().add("brgy-badge-open");

        hboxNameRow.getChildren().addAll(lblName, lblBadge);

        Label lblAddress = new Label(c.address());
        lblAddress.getStyleClass().add("brgy-card-center-addr");
        lblAddress.setWrapText(true);
        lblAddress.setMaxWidth(196);

        Label lblEvent = new Label("📌 " + c.eventLabel());
        lblEvent.getStyleClass().add("brgy-card-center-event");
        lblEvent.setWrapText(true);
        lblEvent.setMaxWidth(196);

        Label lblTimestamp = new Label(c.updatedAt());
        lblTimestamp.getStyleClass().add("brgy-card-center-ts");

        vboxContent.getChildren().addAll(hboxNameRow, lblAddress, lblEvent, lblTimestamp);
        hboxCard.getChildren().addAll(imgCard, vboxContent);

        hboxCard.setOnMouseClicked(_e -> {
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

    private void triggerMapMarkerHighlight(long centerId) {
        try {
            webViewMap.getEngine()
                    .executeScript("window.highlightMarker(" + centerId + ")");
        } catch (Exception ignored) {}
    }

    // ══════════════════════════════════════════════════════════════
    //  DATABASE — Evacuees
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

        // NEW: Inline Action Buttons (Edit / Delete)
        tableColumnEvacAction.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(8, editBtn, deleteBtn);

            {
                // Basic styling for the buttons so they match the theme
                editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");
                pane.setAlignment(javafx.geometry.Pos.CENTER);

                editBtn.setOnAction(event -> {
                    EvacueeRow rowData = getTableView().getItems().get(getIndex());
                    handleEditEvacuee(rowData);
                });

                deleteBtn.setOnAction(event -> {
                    EvacueeRow rowData = getTableView().getItems().get(getIndex());
                    handleDeleteEvacuee(rowData);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void handleDeleteEvacuee(EvacueeRow rowData) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete registration for " + rowData.name() + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                try {
                    new com.example.dao.EvacueeDao().delete(Long.parseLong(rowData.id()));
                    loadEvacueesFromDB(); // Refresh table instantly
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void handleEditEvacuee(EvacueeRow rowData) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/example/dashboard_barangay/modals/register-evacuee.fxml")
            );
            javafx.scene.Parent root = loader.load();

            RegisterEvacueeController controller = loader.getController();

            // TRIGGER EDIT MODE
            long evacueeId = Long.parseLong(rowData.id());
            controller.initEditData(evacueeId, rowData.name(), CURRENT_BARANGAY, this::loadEvacueesFromDB);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Edit Evacuee");
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(tableViewEvacuees.getScene().getWindow());
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            System.err.println("Failed to open Edit Evacuee modal: " + e.getMessage());
        }
    }

    private void loadEvacueesFromDB() {
        String sql = """
            SELECT e.id, e.full_name_enc, ec.name AS center_name, u.display_name AS barangay
            FROM evacuees e
            JOIN evacuation_centers ec ON e.evacuation_center_id = ec.id
            -- FIXED: We now check who owns the CENTER, not who registered the evacuee!
            JOIN users u ON ec.user_id = u.id 
            WHERE u.display_name = ?
            ORDER BY e.created_at DESC
            LIMIT 200
            """;

        javafx.collections.ObservableList<EvacueeRow> rows =
                javafx.collections.FXCollections.observableArrayList();

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, CURRENT_BARANGAY);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new EvacueeRow(
                            String.valueOf(rs.getLong("id")),
                            rs.getString("full_name_enc"),
                            rs.getString("center_name"),
                            rs.getString("barangay")));
                }
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

            UpdateCenterController controller = loader.getController();
            // PASS THE EXTRA DATA SO THE MODAL CAN LOAD IT!
            controller.initData(
                    selectedCenter.id(),
                    selectedCenter.name(),
                    selectedCenter.address(),
                    selectedCenter.photoPath(),
                    selectedCenter.eventLabel(),
                    selectedCenter.availableItems()
            );

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Update " + selectedCenter.name());
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(buttonUpdateCenter.getScene().getWindow());
            stage.setScene(new javafx.scene.Scene(root));

            stage.showAndWait();
            handleRefresh();
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
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DATABASE — Activity Log
    // ══════════════════════════════════════════════════════════════

    private void setupActivityTable() {
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
            String brgyName = (session != null && session.getUser() != null)
                    ? session.getUser().displayName()
                    : CURRENT_BARANGAY;

            List<ActivityTimelineItem> logs = timelineDao.getBarangayTimeline(brgyName);
            tableViewActivity.setItems(javafx.collections.FXCollections.observableArrayList(logs));
        } catch (SQLException e) {
            System.err.println("Failed to refresh activity log: " + e.getMessage());
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
    //  JS → Java bridge
    // ══════════════════════════════════════════════════════════════

    public static class BrgyMapBridge {
        private final BrgyDashboardController ctrl;
        public BrgyMapBridge(BrgyDashboardController ctrl) { this.ctrl = ctrl; }

        public void onMarkerClick(String centerId) {
            Platform.runLater(() -> ctrl.onMarkerClicked(centerId));
        }

        // NEW: Receives the lat/lng from Phase 1
        public void onMapClicked(double lat, double lng) {
            Platform.runLater(() -> ctrl.openAddCenterModal(lat, lng));
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

    private void populateSidebarFromSession() {
        var session = com.example.auth.SessionContext.current();
        if (session == null || session.getUser() == null) return;

        var user = session.getUser();
        if (labelStaffName != null) {
            labelStaffName.setText(user.displayName());
        }
        if (labelStaffRole != null) {
            labelStaffRole.setText(roleLabelFor(user.role()));
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

            controller.setOnClose(() -> anchorPaneMainRoot.getChildren().remove(modal));

            anchorPaneMainRoot.getChildren().add(modal);
        } catch (java.io.IOException e) {
            System.err.println("Failed to open supply requests modal: " + e.getMessage());
        }
    }
    private void handleDeleteCenter(long centerId) {
        try {
            EvacueeDao evacDao = new EvacueeDao();
            int evacueeCount = evacDao.countByCenter(centerId);

            if (evacueeCount > 0) {
                // 1. If there are evacuees, grab all OTHER centers in the barangay
                List<CenterData> otherCenters = centers.stream()
                        .filter(c -> c.id() != centerId)
                        .toList();

                if (otherCenters.isEmpty()) {
                    Alert err = new Alert(Alert.AlertType.ERROR, "Cannot delete! There are " + evacueeCount + " evacuees here, and you have no other centers to transfer them to. Please register a new center first.", ButtonType.OK);
                    err.showAndWait();
                    return;
                }

                // 2. Ask the user where to move them using a native JavaFX ChoiceDialog
                ChoiceDialog<CenterData> dialog = new ChoiceDialog<>(otherCenters.get(0), otherCenters);
                dialog.setTitle("Evacuee Mass Reassignment");
                dialog.setHeaderText("This center currently houses " + evacueeCount + " evacuees.\nYou must reassign them to another center before demolishing this one.");
                dialog.setContentText("Select destination center:");

                // 3. If they click OK, run the mass migration and delete!
                dialog.showAndWait().ifPresent(chosenCenter -> {
                    try {
                        evacDao.reassignAll(centerId, chosenCenter.id());
                        new EvacuationCenterDao().delete(centerId);

                        handleOverlayClose();
                        handleRefresh();       // Refresh the map markers
                        loadEvacueesFromDB();  // Refresh the main registration table

                        Alert success = new Alert(Alert.AlertType.INFORMATION, "Center deleted! " + evacueeCount + " evacuees were safely transferred to " + chosenCenter.name() + ".", ButtonType.OK);
                        success.showAndWait();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

            } else {
                // Standard deletion if the building is completely empty
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to permanently delete this empty evacuation center?", ButtonType.YES, ButtonType.NO);
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        try {
                            new EvacuationCenterDao().delete(centerId);
                            handleOverlayClose();
                            handleRefresh();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
