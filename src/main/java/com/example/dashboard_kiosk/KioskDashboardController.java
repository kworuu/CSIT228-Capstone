package com.example.dashboard_kiosk;

import com.example.map_logic_v2.BrgyMapHtmlProvider;
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

    @FXML private WebView webViewMiniMap; // This branch uses webViewMiniMap
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

    @FXML private TableView<EvacueeRecord>           kioskEvacueeTable;
    @FXML private TableColumn<EvacueeRecord, String> colEvacId;
    @FXML private TableColumn<EvacueeRecord, String> colEvacName;
    @FXML private TableColumn<EvacueeRecord, String> colEvacCenter;
    @FXML private TableColumn<EvacueeRecord, String> colEvacBrgy;
    @FXML private TableColumn<EvacueeRecord, String> colEvacRegisteredAt;
    @FXML private TextField searchField1;
    @FXML private Label lblEvacueeCount;
    @FXML private VBox  vboxEvacueeList;

    private String selectedCenterId = null;

    private final List<SimpleCenter> allCenters = new ArrayList<>();
    private final ObservableList<EvacueeRecord> evacueeData = FXCollections.observableArrayList();
    private KioskMapBridge mapBridge;

    private double currentBrgyLat = 10.3157;
    private double currentBrgyLng = 123.8854;
    private int currentBrgyZoom = 13;

    private final ObservableList<SimpleCenter> masterTableData = FXCollections.observableArrayList();

    public record EvacueeRecord(String id, String fullName, String assignedCenter, String barangay, String registeredAt) {}
    public record SimpleCenter(String id, String title, String address, String barangay, String status, String createdAt, double lat, double lng) {}


    @FXML
    public void initialize() {
        setupInventoryTable();
        setupSearch();
        loadCentersFromDB();
        loadEvacueesFromDB();
        loadEventsFromDB();
        setupMap();
        CenterEventManager.getInstance().addObserver(this);
        
        // Link the Event drawer clicks to Map interactions
        if (eventCellController != null) {
            eventCellController.setEventClickListener(centerId -> {
                Platform.runLater(() -> {
                    triggerMapMarkerHighlight(centerId);
                    allCenters.stream()
                            .filter(c -> c.id().equals(centerId))
                            .findFirst()
                            .ifPresent(this::showDetailModal);
                });
            });
        }
    }

    private void setupInventoryTable() {
        if (colId1       != null) colId1.setCellValueFactory(c       -> new SimpleStringProperty(c.getValue().id()));
        if (colName1     != null) colName1.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().title()));
        if (colCenter1   != null) colCenter1.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().address()));
        if (colBarangay1 != null) colBarangay1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().barangay()));
        if (colStatus1   != null) colStatus1.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().status()));
        if (colAction1   != null) colAction1.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().createdAt()));

        if (colEvacId          != null) colEvacId.setCellValueFactory(c          -> new SimpleStringProperty(c.getValue().id()));
        if (colEvacName        != null) colEvacName.setCellValueFactory(c        -> new SimpleStringProperty(c.getValue().fullName()));
        if (colEvacCenter      != null) colEvacCenter.setCellValueFactory(c      -> new SimpleStringProperty(c.getValue().assignedCenter()));
        if (colEvacBrgy        != null) colEvacBrgy.setCellValueFactory(c        -> new SimpleStringProperty(c.getValue().barangay()));
        if (colEvacRegisteredAt != null) colEvacRegisteredAt.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().registeredAt()));

        if (kioskEvacueeTable != null) kioskEvacueeTable.setItems(evacueeData);
    }

    private void loadCentersFromDB() {
        allCenters.clear();
        String sql = """
            SELECT ec.id, ec.name, ec.address, u.display_name as barangay, ec.created_at, ec.latitude, ec.longitude 
            FROM evacuation_centers ec
            JOIN users u ON ec.user_id = u.id
            ORDER BY ec.name ASC
        """;
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while(rs.next()) {
                allCenters.add(new SimpleCenter(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("name"),
                        rs.getString("address"),
                        rs.getString("barangay"),
                        "ACTIVE",
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
        String sql = """
            SELECT e.id, e.full_name_enc, ec.name AS center_name, u.display_name as barangay, e.created_at 
            FROM evacuees e 
            JOIN evacuation_centers ec ON e.evacuation_center_id = ec.id
            JOIN users u ON ec.user_id = u.id
            ORDER BY e.created_at DESC 
            LIMIT 200
        """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps =prepareStatement(conn, sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new EvacueeRecord(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("full_name_enc"),
                        rs.getString("center_name"),
                        rs.getString("barangay"),
                        formatTimestamp(rs.getString("created_at"))
                ));
            }
        } catch (SQLException e) {
            System.err.println("[KioskDashboard] DB Error loading evacuees: " + e.getMessage());
        }

        Platform.runLater(() -> evacueeData.setAll(rows));
    }
    
    // Helper method
    private PreparedStatement prepareStatement(Connection conn, String sql) throws SQLException {
         return conn.prepareStatement(sql);
    }

    @Override
    public void onCenterUpdated(CenterEvent event) {
        Platform.runLater(() -> {
            loadCentersFromDB();
            loadEvacueesFromDB();

            if (detailModal != null
                    && detailModal.isVisible()
                    && selectedCenterId != null
                    && String.valueOf(event.centerId()).equals(selectedCenterId)) {
                loadEvacueesForCenter(selectedCenterId);
            }

            if (eventCellController != null) {
                eventCellController.addEventToTop(event);
            }

        });
    }

    private void setupSearch() {
        if (searchField1 == null || mainTable1 == null) return;

        SearchTableUtility.setupSearch(
                searchField1,
                mainTable1,
                masterTableData,
                (center, searchText) -> {
                    if (searchText == null || searchText.isBlank()) return true;

                    String searchLower = searchText.toLowerCase();

                    boolean matchTitle = center.title() != null && center.title().toLowerCase().contains(searchLower);
                    boolean matchAddress = center.address() != null && center.address().toLowerCase().contains(searchLower);
                    boolean matchBarangay = center.barangay() != null && center.barangay().toLowerCase().contains(searchLower);

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

        selectedCenterId = c.id();

        if (lblModalTitle != null) lblModalTitle.setText(c.title());
        if (lblModalDesc  != null) lblModalDesc.setText(c.address() + " · " + c.barangay());

        loadEvacueesForCenter(c.id());

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

    // ── JavaScript Bridge Class (Must be a public class for JS reflection) ──
    public static class KioskMapBridge {
        private final KioskDashboardController ctrl;

        public KioskMapBridge(KioskDashboardController ctrl) {
            this.ctrl = ctrl;
        }

        public void onMarkerClick(String centerId) {
            Platform.runLater(() -> {
                ctrl.triggerMapMarkerHighlight(centerId);
                ctrl.allCenters.stream()
                        .filter(c -> c.id().equals(centerId))
                        .findFirst()
                        .ifPresent(ctrl::showDetailModal);
            });
        }

        public void toggleHomeButton(boolean show) {
            // No-op for Kiosk, but absolutely critical so Javascript doesn't crash on drag!
        }
    }

    private void setupMap() {
        if (webViewMiniMap == null) return;

        webViewMiniMap.getEngine().setJavaScriptEnabled(true);
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        webViewMiniMap.getEngine().setUserAgent("CivicGuard/1.0");

        webViewMiniMap.getEngine().setOnAlert(event ->
                System.out.println("[MAP-DEBUG] " + event.getData()));

        webViewMiniMap.getEngine().getLoadWorker().stateProperty()
                .addListener((_obs, _old, state) -> {
                    if (state == Worker.State.SUCCEEDED) {
                        JSObject win = (JSObject) webViewMiniMap.getEngine()
                                .executeScript("window");
                                
                        this.mapBridge = new KioskMapBridge(this);
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

        webViewMiniMap.getEngine().loadContent(
                BrgyMapHtmlProvider.getMapHTML(
                        buildCentersJson(),
                        currentBrgyLat,
                        currentBrgyLng,
                        currentBrgyZoom,
                        tilePort
                )
        );
    }
    
    private void triggerMapMarkerHighlight(String centerId) {
        if (webViewMiniMap == null) return;
        try {
            webViewMiniMap.getEngine()
                    .executeScript("if(window.highlightMarker) window.highlightMarker(" + centerId + ")");
        } catch (Exception ignored) {}
    }

    private String buildCentersJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < allCenters.size(); i++) {
            SimpleCenter c = allCenters.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format(
                    "{\"id\":\"%s\",\"name\":\"%s\",\"lat\":%s,\"lng\":%s,\"focal\":%b}",
                    esc(c.id()), esc(c.title()), c.lat(), c.lng(), false
            ));
        }
        return sb.append("]").toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ");
    }

    public interface MapBridge { 
        void onMarkerClick(String centerId); 
        void toggleHomeButton(boolean show); 
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