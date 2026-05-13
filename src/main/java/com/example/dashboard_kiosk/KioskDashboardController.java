package com.example.dashboard_kiosk;

import com.example.dashboard_kiosk.sample.KioskSampleData;
import com.example.dashboard_kiosk.user.EmergencyAlert;
import com.example.dashboard_kiosk.user.SimpleCenter;
import com.example.map_logic_v2.MapHtmlProvider;
import com.example.map_logic_v2.MapBridge;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Controller for the public-facing Kiosk / Barangay dashboard.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Seeds and displays evacuation center data in the TableView.</li>
 *   <li>Populates the Emergency Alerts panel, sorted newest-first.</li>
 *   <li>Bridges JavaScript map-marker clicks to the floating DetailModal
 *       overlay via {@link MapBridge}.</li>
 *   <li>Applies OPEN/FULL status styling consistently to table pills and
 *       map marker colors.</li>
 * </ul>
 * </p>
 *
 * <p>All view models use the top-level {@link SimpleCenter} and
 * {@link EmergencyAlert} classes in this package. Seed data comes from
 * {@link KioskSampleData}; replace those calls with a DAO/service for
 * production. All magic strings and numbers are sourced from
 * {@link KioskConstants}.</p>
 */
public class KioskDashboardController {

    // ── FXML injections ────────────────────────────────────────────────────

    @FXML private WebView    webViewMiniMap;
    @FXML private AnchorPane mapOverlayPane;

    @FXML private TableView<SimpleCenter>           mainTable1;
    @FXML private TableColumn<SimpleCenter, String> colId1;      // Center name
    @FXML private TableColumn<SimpleCenter, String> colName1;    // Address
    @FXML private TableColumn<SimpleCenter, String> colStatus1;  // Occupancy + View btn

    @FXML private TextField searchField1;

    /** Container for dynamically generated alert cards. */
    @FXML private VBox alertsContainer;

    // Coordinates for Argao
    private double currentBrgyLat = 9.8828;
    private double currentBrgyLng = 123.5953;
    private int currentBrgyZoom = 13;

    // Crucial: Store the bridge as a class member so it isn't garbage collected!
    private MapBridge mapBridge;

    // ── Internal state ─────────────────────────────────────────────────────

    private final ObservableList<SimpleCenter> allCenters   = FXCollections.observableArrayList();
    private final ObservableList<SimpleCenter> shownCenters = FXCollections.observableArrayList();

    /** Tracks the active filter token (ALL / OPEN / FULL). */
    private String activeFilter = KioskConstants.FILTER_ALL;

    /** Reference kept so we can call show() from the map bridge. */
    private DetailModalController detailModalController;

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        seedSampleData();
        setupCentersTable();
        setupAlerts();
        setupMap();
        loadDetailModal();
    }

    // ── Sample data ────────────────────────────────────────────────────────

    /**
     * Loads seed evacuation centers from {@link KioskSampleData}.
     * Replace with a DAO/service call in production.
     */
    private void seedSampleData() {
        allCenters.addAll(KioskSampleData.getSampleCenters());
        shownCenters.setAll(allCenters);
    }

    // ── Centers TableView ──────────────────────────────────────────────────

    private void setupCentersTable() {
        if (mainTable1 == null) return;

        // Center Name column
        colId1.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTitle()));

        // Address column
        colName1.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getAddress()));

        colStatus1.setCellValueFactory(c -> new SimpleStringProperty(""));
        colStatus1.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("View");
            {
                btn.getStyleClass().add("btn-table-action");
                btn.setOnAction(e -> {
                    SimpleCenter center = getTableView().getItems().get(getIndex());
                    showDetailModal(center);
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox cell = new HBox(btn);
                cell.setAlignment(Pos.CENTER_LEFT);
                setGraphic(cell);
            }
        });

        colStatus1.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("View");
            {
                btn.getStyleClass().add("btn-table-action");
                btn.setOnAction(e -> {
                    SimpleCenter center = getTableView().getItems().get(getIndex());
                    showDetailModal(center);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); setText(null); return; }

                Label occ = new Label(item);
                occ.getStyleClass().add(KioskConstants.CSS_OCCUPANCY_LABEL);

                HBox cell = new HBox(8, occ, btn);
                cell.setAlignment(Pos.CENTER_LEFT);
                setGraphic(cell);
                setText(null);
            }
        });

        mainTable1.setItems(shownCenters);
        mainTable1.setPlaceholder(new Label("No evacuation centers match the current filter."));
    }




    private void updateSegmentStyle(Button btn, String filter) {
        if (btn == null) return;
        btn.getStyleClass().removeAll(KioskConstants.CSS_SEGMENT_ACTIVE);
        if (filter.equals(activeFilter)) btn.getStyleClass().add(KioskConstants.CSS_SEGMENT_ACTIVE);
    }

    // ── Emergency Alerts ───────────────────────────────────────────────────

    /**
     * Populates the alerts panel from {@link KioskSampleData}.
     * Alerts are sorted newest-first so responders see the most urgent
     * information at the top without scrolling.
     */
    private void setupAlerts() {
        if (alertsContainer == null) return;

        List<EmergencyAlert> alerts = new ArrayList<>(KioskSampleData.getSampleAlerts());

        // Sort: newest timestamp first
        alerts.sort(Comparator.comparing(EmergencyAlert::getIssuedAt).reversed());

        alertsContainer.getChildren().clear();
        for (EmergencyAlert alert : alerts) {
            alertsContainer.getChildren().add(buildAlertCard(alert));
        }
    }

    /**
     * Builds a styled VBox card for a single alert.
     * Severity determines the left-border accent color via CSS class.
     */
    private VBox buildAlertCard(EmergencyAlert alert) {
        VBox card = new VBox(4);
        card.getStyleClass().add(KioskConstants.CSS_ALERT_ITEM);

        switch (alert.getSeverity()) {
            case CRITICAL -> card.getStyleClass().add(KioskConstants.CSS_ALERT_CRITICAL);
            case WARNING  -> card.getStyleClass().add(KioskConstants.CSS_ALERT_WARNING);
            case INFO     -> card.getStyleClass().add(KioskConstants.CSS_ALERT_INFO);
        }

        Label time = new Label(alert.getIssuedAt().format(KioskConstants.ALERT_FORMATTER));
        time.getStyleClass().add(KioskConstants.CSS_ALERT_TIME);

        Label title = new Label(alert.getTitle());
        title.getStyleClass().add(KioskConstants.CSS_ALERT_TITLE);

        Label loc = new Label(KioskConstants.LOCATION_ICON_PREFIX + alert.getLocation());
        loc.getStyleClass().add(KioskConstants.CSS_ALERT_LOCATION);

        Label body = new Label(alert.getBody());
        body.getStyleClass().add(KioskConstants.CSS_ALERT_BODY);
        body.setWrapText(true);

        card.getChildren().addAll(time, title, loc, body);
        return card;
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

        // Use the map_logic_v2 provider matching testcase1
        webViewMiniMap.getEngine().loadContent(
                com.example.map_logic_v2.BrgyMapHtmlProvider.getMapHTML(
                        buildCentersJson(),
                        currentBrgyLat,
                        currentBrgyLng,
                        currentBrgyZoom
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

    // ── Detail modal (overlay) ─────────────────────────────────────────────

    /**
     * Loads the detail-modal FXML, positions it as an AnchorPane overlay,
     * and wires the controller reference so the map bridge can call show().
     */
    private void loadDetailModal() {
        if (mapOverlayPane == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(KioskConstants.DETAIL_MODAL_FXML));
            VBox modal = loader.load();
            detailModalController = loader.getController();

            // Anchor modal to bottom-left of the map overlay pane
            AnchorPane.setBottomAnchor(modal, KioskConstants.MODAL_ANCHOR_BOTTOM);
            AnchorPane.setLeftAnchor(modal,   KioskConstants.MODAL_ANCHOR_LEFT);

            mapOverlayPane.getChildren().add(modal);

            detailModalController.setOnShowRoute(() ->
                    System.out.println(KioskConstants.LOG_SHOW_ROUTE));
            detailModalController.setOnViewDetails(() ->
                    System.out.println(KioskConstants.LOG_VIEW_DETAILS));

        } catch (IOException ex) {
            System.err.println(KioskConstants.LOG_MODAL_ERROR + ex.getMessage());
        }
    }

    /**
     * Delegates to the modal controller to display center info.
     * Also bounces the corresponding map marker via JavaScript.
     */
    private void showDetailModal(SimpleCenter center) {
        if (detailModalController == null) return;
        detailModalController.show(center);

        // Bounce matching marker on the map
        if (webViewMiniMap != null) {
            try {
                webViewMiniMap.getEngine().executeScript(
                        String.format(KioskConstants.JS_HIGHLIGHT_MARKER, esc(center.getId())));
            } catch (Exception ignored) { /* map may still be loading */ }
        }
    }

    // ── Utilities ──────────────────────────────────────────────────────────

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("'", "\\'");
    }
}