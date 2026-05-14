package com.example.dashboard_kiosk;

import com.example.map_tiles.TilePrefetchService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import com.example.util.Route;
import com.example.util.Router;
import java.util.ArrayList;
import java.util.List;

public class KioskDashboardController {

    // ── FXML Fields ────────────────────────────────────────────────────────
    @FXML private WebView webViewMiniMap;
    @FXML private VBox detailModal;
    @FXML private Label lblModalTitle;
    @FXML private Label lblModalDesc;
    @FXML private TableView<String> tableInventory1;
    @FXML private TableColumn<String, String> colItem1;
    @FXML private TableColumn<String, String> colStatus1;
    @FXML private TextField searchField1;

    // Filter Buttons
    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterSafe;
    @FXML private Button btnFilterFull;

    @FXML private VBox vboxCardsContainer;
    @FXML private TableView<SimpleCenter> mainTable1;
    @FXML private TableColumn<SimpleCenter, String> colId1;
    @FXML private TableColumn<SimpleCenter, String> colName1;

    // ── State ──────────────────────────────────────────────────────────────
    private final List<SimpleCenter> allCenters = new ArrayList<>();
    private final List<SimpleCenter> filteredCenters = new ArrayList<>();
    
    // Current Map Location
    private double currentBrgyLat = 9.8828;
    private double currentBrgyLng = 123.5953;
    private int currentBrgyZoom = 13;
    
    private String activeFilter = KioskConstants.FILTER_ALL;
    private MapBridge mapBridge;

    // ── Login navigation ──────────────────────────────────────────────────

    @FXML
    private void handleAdminLogin() {
        Router.getInstance().navigate(Route.ADMIN_LOGIN);
    }

    @FXML
    private void handleBarangayLogin() {
        Router.getInstance().navigate(Route.BARANGAY_LOGIN);
    }
    @FXML
    public void initialize() {
        // Load dummy data
        loadDummyData();
        
        // Setup initial map
        setupMap();
        setupInventoryTable();
    }

    // ── Data Loading ───────────────────────────────────────────────────────

    private void loadDummyData() {
        allCenters.clear();
        
        // Match dummy data from BrgyDashboard
        SimpleCenter c1 = new SimpleCenter(
            "1", "Poblacion Argao Elementary", 
            "M.L. Quezon St, Poblacion, Argao",
            9.8828, 123.5953
        );
        c1.getItems().add("Rice Sacks");
        c1.getItems().add("Canned Goods");
        
        SimpleCenter c2 = new SimpleCenter(
            "2", "Argao Sports Complex", 
            "Dr. T.S. Kintanar St, Bogo, Argao",
            9.8845, 123.6001
        );
        c2.getItems().add("Water Bottles");
        c2.getItems().add("Blankets");
        
        allCenters.add(c1);
        allCenters.add(c2);
        
        // Initial state: show all
        filteredCenters.addAll(allCenters);
    }

    // ── UI Setup ───────────────────────────────────────────────────────────

    private void setupInventoryTable() {
        if (colId1 != null) {
            colId1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        }
        if (colName1 != null) {
            colName1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAddress()));
        }
        if (colStatus1 != null) {
            colStatus1.setCellValueFactory(c -> new SimpleStringProperty("")); // Empty string for status column
            
            // Add a "View" button to the status column
            colStatus1.setCellFactory(col -> new TableCell<>() {
                private final Button btn = new Button("View");
                {
                    btn.getStyleClass().add("brgy-tbl-action");
                    btn.setOnAction(e -> {
                        // Do nothing for now
                    });
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(btn);
                    }
                }
            });
        }
        
        if (mainTable1 != null) {
            mainTable1.getItems().clear();
            mainTable1.getItems().addAll(filteredCenters);
        }
    }

    // ── Interaction Logic ──────────────────────────────────────────────────

    private void showDetailModal(SimpleCenter c) {
        if (lblModalTitle != null) lblModalTitle.setText(c.getTitle());
        if (lblModalDesc != null) lblModalDesc.setText(c.getAddress());
        
        if (tableInventory1 != null) {
            tableInventory1.getItems().clear();
            tableInventory1.getItems().addAll(c.getItems());
        }
        
        if (detailModal != null) {
            detailModal.setVisible(true);
            detailModal.setManaged(true);
        }
    }

    @FXML
    private void hideDetailModal() {
        if (detailModal != null) {
            detailModal.setVisible(false);
            detailModal.setManaged(false);
        }
    }

    // ── Filter Logic ───────────────────────────────────────────────────────

    @FXML
    private void filterAll() {
        updateSegmentStyle(btnFilterAll, KioskConstants.FILTER_ALL);
        filteredCenters.clear();
        filteredCenters.addAll(allCenters);
    }

    @FXML
    private void filterSafe() {
        updateSegmentStyle(btnFilterSafe, KioskConstants.FILTER_SAFE);
        filteredCenters.clear();
        // In dummy mode, safe = odd id
        for (SimpleCenter c : allCenters) {
            if (Integer.parseInt(c.getId()) % 2 != 0) {
                filteredCenters.add(c);
            }
        }
    }

    @FXML
    private void filterFull() {
        updateSegmentStyle(btnFilterFull, KioskConstants.FILTER_FULL);
        filteredCenters.clear();
        // In dummy mode, full = even id
        for (SimpleCenter c : allCenters) {
            if (Integer.parseInt(c.getId()) % 2 == 0) {
                filteredCenters.add(c);
            }
        }
    }

    private void updateSegmentStyle(Button btn, String filter) {
        // Simple mock implementation
        this.activeFilter = filter;
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

        // Use the map_logic_v2 provider matching testcase1
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

    // ── Helper Models ──────────────────────────────────────────────────────

    public interface MapBridge {
        void onMarkerClick(String centerId);
    }

    public static class SimpleCenter {
        private final String id;
        private final String title;
        private final String address;
        private final double lat;
        private final double lng;
        private final List<String> items = new ArrayList<>();

        public SimpleCenter(String id, String title, String address, double lat, double lng) {
            this.id = id;
            this.title = title;
            this.address = address;
            this.lat = lat;
            this.lng = lng;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getAddress() { return address; }
        public double getLat() { return lat; }
        public double getLng() { return lng; }
        public List<String> getItems() { return items; }
    }

    public static class KioskConstants {
        public static final String FILTER_ALL = "all";
        public static final String FILTER_SAFE = "safe";
        public static final String FILTER_FULL = "full";
        public static final String JSON_CENTER_TEMPLATE = "{\"id\":\"%s\",\"name\":\"%s\",\"lat\":%s,\"lng\":%s,\"isFocal\":%b}";
        public static final String FOCAL_CENTER_ID = "1";
    }
}
