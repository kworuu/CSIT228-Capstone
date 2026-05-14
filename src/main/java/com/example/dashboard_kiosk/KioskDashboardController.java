package com.example.dashboard_kiosk;

import com.example.map_tiles.TilePrefetchService;
import com.example.util.CenterEvent;
import com.example.util.CenterEventManager;
import com.example.util.CenterEventObserver;
import com.example.util.DBConnectionManager;
import com.example.util.Route;
import com.example.util.Router;
import javafx.application.Platform;
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
    @FXML private TextField searchField1;

    private final List<SimpleCenter> allCenters = new ArrayList<>();
    private final List<SimpleCenter> filteredCenters = new ArrayList<>();
    private MapBridge mapBridge;

    // Current Map Location Default
    private double currentBrgyLat = 10.3157;
    private double currentBrgyLng = 123.8854;
    private int currentBrgyZoom = 13;

    // ── State ──────────────────────────────────────────────────────────────
    private final ObservableList<SimpleCenter> masterTableData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupInventoryTable();

        // ADD THIS LINE HERE:
        setupSearch();

        loadCentersFromDB();
        loadEventsFromDB(); // Populates drawer at startup
        setupMap();

        // Subscribe to real-time events
        CenterEventManager.getInstance().addObserver(this);
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

    // Observer Callback - Triggered by UpdateCenterController
    @Override
    public void onCenterUpdated(CenterEvent event) {
        // Must execute on JavaFX Thread
        Platform.runLater(() -> {
            loadCentersFromDB(); // Refresh Table
            if (eventCellController != null) {
                eventCellController.addEventToTop(event); // Push new event to drawer
            }
            setupMap();
        });
    }

    private void setupInventoryTable() {
        if (colId1 != null) colId1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        if (colName1 != null) colName1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        if (colCenter1 != null) colCenter1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAddress()));
        if (colBarangay1 != null) colBarangay1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBarangay()));
        if (colStatus1 != null) colStatus1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        if (colAction1 != null) colAction1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCreatedAt()));
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
        if (lblModalTitle != null) lblModalTitle.setText(c.getTitle());
        if (lblModalDesc != null) lblModalDesc.setText(c.getAddress());
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

        webViewMiniMap.getEngine().getLoadWorker().stateProperty()
                .addListener((obs, old, state) -> {
                    if (state == Worker.State.SUCCEEDED) {
                        JSObject win = (JSObject) webViewMiniMap.getEngine().executeScript("window");

                        // 1. Assign to class variable
                        this.mapBridge = new MapBridge() {
                            @Override
                            public void onMarkerClick(String centerId) {
                                Platform.runLater(() ->
                                        allCenters.stream()
                                                .filter(c -> c.getId().equals(centerId))
                                                .findFirst()
                                                .ifPresent(KioskDashboardController.this::showDetailModal));
                            }
                        };

                        // 2. Pass to JS window
                        win.setMember("javaBridge", this.mapBridge);
                    }
                });

        // Ensure the local tile server is started
        int tilePort = -1;
        try {
            tilePort = TilePrefetchService.getInstance().startServer();
        } catch (Exception e) {
            System.err.println("Could not start local tile server for Kiosk: " + e.getMessage());
        }

        // Load the map
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
}