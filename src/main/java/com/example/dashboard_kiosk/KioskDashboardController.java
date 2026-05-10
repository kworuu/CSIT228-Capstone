package com.example.dashboard_kiosk;

import com.example.map_logic_v2.MapHtmlProvider;
import com.example.map_logic_v2.MapBridge;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * Simplified controller that removes all missing dependencies (like EvacuationCenter
 * and DetailModalController) so the application can run independently.
 */
public class KioskDashboardController {

    // ── FXML fields ────────────────────────────────────────────────────────

    @FXML private WebView webViewMiniMap;
    @FXML private AnchorPane mapOverlayPane;

    @FXML private TableView<SimpleCenter> mainTable1;
    @FXML private TableColumn<SimpleCenter, String> colId1;
    @FXML private TableColumn<SimpleCenter, String> colName1;
    @FXML private TableColumn<SimpleCenter, String> colCenter1;
    @FXML private TableColumn<SimpleCenter, String> colStatus1;

    @FXML private TextField searchField1;
    @FXML private Button filterAll1, filterOpen1, filterFull1;

    // ── State ──────────────────────────────────────────────────────────────

    private final ObservableList<SimpleCenter> allCenters = FXCollections.observableArrayList();

    // Local simplified model to avoid missing class errors
    public static class SimpleCenter {
        private final String id, title, address, status;
        private final double lat, lng;
        
        public SimpleCenter(String id, String title, String address, String status, double lat, double lng) {
            this.id = id; this.title = title; this.address = address; this.status = status;
            this.lat = lat; this.lng = lng;
        }
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getAddress() { return address; }
        public String getStatus() { return status; }
        public double getLat() { return lat; }
        public double getLng() { return lng; }
    }

    // ── Init ───────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        seedSampleData();
        setupCentersTable();
        setupMap();
    }

    // ── Sample data ────────────────────────────────────────────────────────

    private void seedSampleData() {
        allCenters.addAll(
            new SimpleCenter("EC-01", "Argao Command Center", "Brgy. Poblacion, Argao, Cebu", "OPEN", 9.8828, 123.5953),
            new SimpleCenter("EC-02", "Argao Central School", "Brgy. Poblacion", "OPEN", 9.8810, 123.5980),
            new SimpleCenter("EC-03", "Mabolo Elementary School", "Brgy. Mabolo", "FULL", 9.8870, 123.6010)
        );
    }

    // ── Centers table ──────────────────────────────────────────────────────

    private void setupCentersTable() {
        if (mainTable1 == null) return;
        
        colId1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        colName1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAddress()));
        colCenter1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        
        colCenter1.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label pill = new Label(status);
                pill.getStyleClass().addAll("status-tag", "OPEN".equals(status) ? "status-tag-open" : "status-tag-full");
                setGraphic(pill);
                setText(null);
            }
        });

        colStatus1.setCellValueFactory(c -> new SimpleStringProperty("View"));
        colStatus1.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("View");
            { btn.getStyleClass().add("btn-table-action"); }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setText(null);
            }
        });

        mainTable1.setItems(allCenters);
    }

    // ── Map ────────────────────────────────────────────────────────────────

    private void setupMap() {
        if (webViewMiniMap == null) return;

        webViewMiniMap.getEngine().setJavaScriptEnabled(true);
        webViewMiniMap.getEngine().getLoadWorker().stateProperty()
                .addListener((obs, old, state) -> {
                    if (state == Worker.State.SUCCEEDED) {
                        JSObject win = (JSObject) webViewMiniMap.getEngine().executeScript("window");
                        // Inject our dependency-free MapBridge
                        win.setMember("javaBridge", new MapBridge());
                    }
                });

        webViewMiniMap.getEngine().loadContent(MapHtmlProvider.getMapHTML(buildCentersJson()));
    }

    private String buildCentersJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < allCenters.size(); i++) {
            SimpleCenter c = allCenters.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format(
                    "{\"id\":\"%s\",\"title\":\"%s\",\"lat\":%s,\"lng\":%s,\"status\":\"%s\",\"focus\":%b}",
                    esc(c.getId()), esc(c.getTitle()), c.getLat(), c.getLng(),
                    c.getStatus(), "EC-01".equals(c.getId())
            ));
        }
        return sb.append("]").toString();
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}