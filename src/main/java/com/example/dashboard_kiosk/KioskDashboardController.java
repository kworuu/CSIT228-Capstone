package com.example.dashboard_kiosk;

import com.example.map_tiles.TilePrefetchService;
import com.example.util.CenterEvent;
import com.example.util.CenterEventManager;
import com.example.util.CenterEventObserver;
import com.example.util.DBConnectionManager;
import com.example.util.Route;
import com.example.util.Router;
import javafx.application.Platform;
import com.example.model.EvacueeRecord;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import com.example.util.SearchTableUtility;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class KioskDashboardController implements CenterEventObserver {

    @FXML private WebView webViewMiniMap;
    @FXML private VBox detailModal;
    @FXML private Label lblModalTitle;
    @FXML private Label lblModalDesc;
    @FXML private TableView<String> tableInventory1;
    @FXML private EventCellController eventCellController;
    @FXML private TableView<SimpleCenter> mainTable1;
    @FXML private TableColumn<SimpleCenter, String> colId1;
    @FXML private TableColumn<SimpleCenter, String> colName1;
    @FXML private TableColumn<SimpleCenter, String> colCenter1;
    @FXML private TableColumn<SimpleCenter, String> colBarangay1;
    @FXML private TableColumn<SimpleCenter, String> colStatus1;
    @FXML private TableColumn<SimpleCenter, String> colAction1;

    // Tab 2 — already added:
    @FXML private TableView<EvacueeRecord>           kioskEvacueeTable;
    @FXML private TableColumn<EvacueeRecord, String> colEvacId;
    @FXML private TableColumn<EvacueeRecord, String> colEvacName;
    @FXML private TableColumn<EvacueeRecord, String> colEvacCenter;
    @FXML private TableColumn<EvacueeRecord, String> colEvacBrgy;
    @FXML private TableColumn<EvacueeRecord, String> colEvacRegisteredAt;
    @FXML private TextField searchField1;
    @FXML private Label lblEvacueeCount;  // "2 registered" badge in detail modal
    @FXML private VBox  vboxEvacueeList;  // name list inside detail modal

    // Tracks which center the detail modal is currently showing.
    private String selectedCenterId = null;

    private final List<SimpleCenter> allCenters = new ArrayList<>();
    private final List<SimpleCenter> filteredCenters = new ArrayList<>();
    private final ObservableList<EvacueeRecord> evacueeData = FXCollections.observableArrayList();
    private MapBridge mapBridge;

    // Current Map Location Default
    private double currentBrgyLat = 10.3157;
    private double currentBrgyLng = 123.8854;
    private int currentBrgyZoom = 13;

    // ── State ──────────────────────────────────────────────────────────────
    private final ObservableList<SimpleCenter> masterTableData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupInventoryTable();   // wires both Tab 1 and Tab 2 columns
        setupSearch();
        loadCentersFromDB();
        loadEvacueesFromDB();    // ← new
        loadEventsFromDB();
        setupMap();
        CenterEventManager.getInstance().addObserver(this);
    }

    private void setupInventoryTable() {
        // ── Tab 1: Centers ───────────────────────────────────────────────────────
        if (colId1       != null) colId1.setCellValueFactory(c       -> new SimpleStringProperty(c.getValue().getId()));
        if (colName1     != null) colName1.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getTitle()));
        if (colCenter1   != null) colCenter1.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getAddress()));
        if (colBarangay1 != null) colBarangay1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBarangay()));
        if (colStatus1   != null) colStatus1.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getStatus()));
        if (colAction1   != null) colAction1.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getCreatedAt()));

        // ── Tab 2: Evacuees ───────────────────────────────────────────────────────
        if (colEvacId          != null) colEvacId.setCellValueFactory(c          -> new SimpleStringProperty(c.getValue().getId()));
        if (colEvacName        != null) colEvacName.setCellValueFactory(c        -> new SimpleStringProperty(c.getValue().getFullName()));
        if (colEvacCenter      != null) colEvacCenter.setCellValueFactory(c      -> new SimpleStringProperty(c.getValue().getAssignedCenter()));
        if (colEvacBrgy        != null) colEvacBrgy.setCellValueFactory(c        -> new SimpleStringProperty(c.getValue().getBarangay()));
        if (colEvacRegisteredAt != null) colEvacRegisteredAt.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRegisteredAt()));

        if (kioskEvacueeTable != null) kioskEvacueeTable.setItems(evacueeData);
    }

    // DB Query to load centers for Table and Map
    private void loadCentersFromDB() {
        allCenters.clear();
        String sql = "SELECT id, name, address, barangay, is_active, created_at, latitude, longitude FROM evacuation_centers ORDER BY name ASC";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while(rs.next()) {
                allCenters.add(new SimpleCenter(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("name"),
                        rs.getString("address"),
                        rs.getString("barangay"),
                        rs.getBoolean("is_active") ? "ACTIVE" : "INACTIVE",
                        formatTimestamp(rs.getString("created_at")),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                ));
            }
        } catch (SQLException e) {
            System.err.println("DB Error: " + e.getMessage());
        }

        Platform.runLater(() -> {
            masterTableData.setAll(allCenters);
        });
    }

    // DB Query to prepopulate events drawer
    private void loadEventsFromDB() {
        List<CenterEvent> events = new ArrayList<>();
        String sql = "SELECT csu.event_label, csu.updated_at, ec.name, ec.id " +
                "FROM center_status_updates csu " +
                "JOIN evacuation_centers ec ON csu.center_id = ec.id " +
                "ORDER BY csu.updated_at DESC LIMIT 15";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                events.add(new CenterEvent(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("event_label"),
                        formatTimestamp(rs.getString("updated_at"))
                ));
            }
        } catch (SQLException e) {
            System.err.println("DB Error loading events: " + e.getMessage());
        }

        if (eventCellController != null) {
            eventCellController.setEvents(events);
        }
    }

    private void loadEvacueesFromDB() {
        List<EvacueeRecord> rows = new ArrayList<>();

        // Exact column names from civicguard schema:
        //   evacuees.full_name_enc, evacuees.evacuation_center_id,
        //   evacuees.barangay, evacuees.created_at
        String sql =
                "SELECT e.id, e.full_name_enc, ec.name AS center_name, " +
                        "       e.barangay, e.created_at " +
                        "FROM evacuees e " +
                        "JOIN evacuation_centers ec ON e.evacuation_center_id = ec.id " +
                        "ORDER BY e.created_at DESC " +
                        "LIMIT 200";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new EvacueeRecord(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("full_name_enc"),   // encrypted field — plain text for now
                        rs.getString("center_name"),      // ec.name alias
                        rs.getString("barangay"),         // e.barangay (on evacuees row itself)
                        formatTimestamp(rs.getString("created_at"))
                ));
            }
        } catch (SQLException e) {
            System.err.println("[KioskDashboard] DB Error loading evacuees: " + e.getMessage());
        }

        Platform.runLater(() -> evacueeData.setAll(rows));
    }

    // Observer Callback - Triggered by UpdateCenterController
    @Override
    public void onCenterUpdated(CenterEvent event) {
        Platform.runLater(() -> {
            loadCentersFromDB();   // Tab 1
            loadEvacueesFromDB();  // Tab 2

            // If the detail modal is open in the center that just changed,
            // refresh its list immediately without any user interaction
            if (detailModal != null
                    && detailModal.isVisible()
                    && selectedCenterId != null
                    && String.valueOf(event.centerId()).equals(selectedCenterId)) {
                loadEvacueesForCenter(selectedCenterId);
            }

            if (eventCellController != null) {
                eventCellController.addEventToTop(event);
            }

            setupMap();
        });
    }

    // Paste this right below setupInventoryTable()
    private void setupSearch() {
        // Safety check to ensure UI elements are loaded
        if (searchField1 == null || mainTable1 == null) return;

        // Call your existing utility class
        SearchTableUtility.setupSearch(
                searchField1,
                mainTable1,
                masterTableData,
                (center, searchText) -> {
                    if (searchText == null || searchText.isBlank()) return true;

                    String searchLower = searchText.toLowerCase();

                    // Safe checks to prevent NullPointerExceptions if a field is null
                    boolean matchTitle = center.getTitle() != null && center.getTitle().toLowerCase().contains(searchLower);
                    boolean matchAddress = center.getAddress() != null && center.getAddress().toLowerCase().contains(searchLower);
                    boolean matchBarangay = center.getBarangay() != null && center.getBarangay().toLowerCase().contains(searchLower);

                    return matchTitle || matchAddress || matchBarangay;
                }
        );
    }

    private String formatTimestamp(String raw) {
        if (raw == null) return "—";
        try {
            LocalDateTime dt = LocalDateTime.parse(raw.replace(" ", "T").substring(0, 19));
            return dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"));
        } catch (Exception e) {
            return raw;
        }
    }

    @FXML private void handleMenuClick() {
        if (eventCellController != null) eventCellController.toggleDrawer();
    }

    @FXML private void handleAdminLogin() { Router.getInstance().navigate(Route.ADMIN_LOGIN); }
    @FXML private void handleBarangayLogin() { Router.getInstance().navigate(Route.BARANGAY_LOGIN); }

    private void showDetailModal(SimpleCenter c) {
        if (c == null) return;

        selectedCenterId = c.getId();

        if (lblModalTitle != null) lblModalTitle.setText(c.getTitle());
        if (lblModalDesc  != null) lblModalDesc.setText(c.getAddress() + " · " + c.getBarangay());

        loadEvacueesForCenter(c.getId());

        if (detailModal != null) {
            detailModal.setVisible(true);
            detailModal.setManaged(true);
        }
    }

    @FXML private void hideDetailModal() {
        if (detailModal != null) {
            detailModal.setVisible(false);
            detailModal.setManaged(false);
        }
    }

    // ── Map initialisation ─────────────────────────────────────────────────

    private void setupMap() {
        if (webViewMiniMap == null) return;

        webViewMiniMap.getEngine().setJavaScriptEnabled(true);
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        webViewMiniMap.getEngine().setUserAgent("CivicGuard/1.0");

        // Critical: without explicit max sizes the WebView hit-test surface
        // may not fill the StackPane, causing drag events to fall through
        webViewMiniMap.setMaxWidth(Double.MAX_VALUE);
        webViewMiniMap.setMaxHeight(Double.MAX_VALUE);

        webViewMiniMap.getEngine().getLoadWorker().stateProperty()
                .addListener((obs, old, state) -> {
                    if (state == Worker.State.SUCCEEDED) {
                        JSObject win = (JSObject) webViewMiniMap.getEngine().executeScript("window");

                        this.mapBridge = centerId -> Platform.runLater(() ->
                                allCenters.stream()
                                        .filter(c -> c.getId().equals(centerId))
                                        .findFirst()
                                        .ifPresent(this::showDetailModal));

                        win.setMember("javaBridge", this.mapBridge);

                        // Re-enable Leaflet drag after page load
                        webViewMiniMap.getEngine().executeScript(
                                "try {" +
                                        "  if (window._leafletMap) {" +
                                        "    window._leafletMap.dragging.enable();" +
                                        "    window._leafletMap.scrollWheelZoom.enable();" +
                                        "  }" +
                                        "} catch(e) {}"
                        );
                    }
                });

        int tilePort = -1;
        try {
            tilePort = TilePrefetchService.getInstance().startServer();
        } catch (Exception e) {
            System.err.println("[KioskDashboard] Tile server error: " + e.getMessage());
        }

        webViewMiniMap.getEngine().loadContent(
                com.example.map_logic_v2.BrgyMapHtmlProvider.getMapHTML(
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
        for (int i = 0; i < allCenters.size(); i++) {
            SimpleCenter c = allCenters.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format(
                    KioskConstants.JSON_CENTER_TEMPLATE,
                    esc(c.getId()), esc(c.getTitle()), c.getLat(), c.getLng(),
                    KioskConstants.FOCAL_CENTER_ID.equals(c.getId())
            ));
        }
        return sb.append("]").toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ");
    }




    public interface MapBridge { void onMarkerClick(String centerId); }

    // Updated SimpleCenter to match Real DB Schema for the TableView
    public static class SimpleCenter {
        private final String id;
        private final String title;
        private final String address;
        private final String barangay;
        private final String status;
        private final String createdAt;
        private final double lat;
        private final double lng;

        public SimpleCenter(String id, String title, String address, String barangay, String status, String createdAt, double lat, double lng) {
            this.id = id; this.title = title; this.address = address;
            this.barangay = barangay; this.status = status; this.createdAt = createdAt;
            this.lat = lat; this.lng = lng;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getAddress() { return address; }
        public String getBarangay() { return barangay; }
        public String getStatus() { return status; }
        public String getCreatedAt() { return createdAt; }
        public double getLat() { return lat; }
        public double getLng() { return lng; }
    }

    private void loadEvacueesForCenter(String centerId) {
        if (vboxEvacueeList == null || lblEvacueeCount == null) return;

        List<String> names = new ArrayList<>();

        String sql =
                "SELECT full_name_enc " +
                        "FROM evacuees " +
                        "WHERE evacuation_center_id = ? " +
                        "ORDER BY created_at DESC " +
                        "LIMIT 50";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, Long.parseLong(centerId));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("full_name_enc"));
                }
            }
        } catch (SQLException | NumberFormatException e) {
            System.err.println("[KioskDashboard] loadEvacueesForCenter: " + e.getMessage());
        }

        final List<String> finalNames = names;
        Platform.runLater(() -> {
            vboxEvacueeList.getChildren().clear();

            if (finalNames.isEmpty()) {
                Label empty = new Label("No evacuees registered at this center.");
                empty.getStyleClass().add("detail-meta");
                vboxEvacueeList.getChildren().add(empty);
            } else {
                for (String name : finalNames) {
                    Label row = new Label("👤  " + name);
                    row.getStyleClass().add("evacuee-list-row");
                    row.setMaxWidth(Double.MAX_VALUE);
                    vboxEvacueeList.getChildren().add(row);
                }
            }

            int count = finalNames.size();
            lblEvacueeCount.setText(count + " registered");
            lblEvacueeCount.getStyleClass().removeAll("status-tag-open", "status-tag-pending");
            lblEvacueeCount.getStyleClass().add(count > 0 ? "status-tag-open" : "status-tag-pending");
        });
    }
}