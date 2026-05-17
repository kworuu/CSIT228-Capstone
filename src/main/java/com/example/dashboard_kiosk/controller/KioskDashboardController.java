package com.example.dashboard_kiosk.controller;

import com.example.dashboard_kiosk.KioskConstants;
import com.example.dashboard_kiosk.model.EvacuationSite;
import com.example.dashboard_kiosk.observer.DashboardViewObserver;
import com.example.dashboard_kiosk.observer.KioskDataSubject;
import com.example.dashboard_kiosk.service.KioskDataService;
import com.example.dashboard_kiosk.session.PublicSessionMode;
import com.example.dashboard_kiosk.session.SessionContext;
import com.example.dashboard_kiosk.session.SessionMode;
import com.example.map_logic_v2.BrgyMapHtmlProvider;
import com.example.map_tiles.TilePrefetchService;
import com.example.model.EvacueeRecord;
import com.example.util.CenterEvent;
import com.example.util.Route;
import com.example.util.Router;
import com.example.util.SearchTableUtility;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.List;
import java.util.Optional;

public final class KioskDashboardController implements DashboardViewObserver {

    // ─── Map ───────────────────────────────────────────────────────────────

    @FXML private WebView miniMapWebView;

    // ─── Detail modal (injected via fx:include) ────────────────────────────

    @FXML private AnchorPane            detailModal;           // root node from fx:include
    @FXML private DetailModalController detailModalController; // injected controller

    // ─── Bottom TabPane ────────────────────────────────────────────────────

    @FXML private TabPane bottomTabPane;
    @FXML private Tab     tabCentersOverview;

    // ─── Centers table ─────────────────────────────────────────────────────

    @FXML private TableView<EvacuationSite>           centerTableView;
    @FXML private TableColumn<EvacuationSite, String> colCenterName;
    @FXML private TableColumn<EvacuationSite, String> colCenterAddress;
    @FXML private TableColumn<EvacuationSite, String> colCenterBarangay;
    @FXML private TableColumn<EvacuationSite, String> colCenterStatus;
    @FXML private TableColumn<EvacuationSite, String> colCenterCreatedAt;

    // ─── Header controls ───────────────────────────────────────────────────

    @FXML private TextField searchField;
    @FXML private Button    btnBarangayLogin;
    @FXML private Button    btnAdminLogin;
    @FXML private Button    btnEventsToggle;

    // ─── Embedded controllers (via <fx:include>) ───────────────────────────

    @FXML private EventCellController eventCellController;

    // ─── Mutable state ─────────────────────────────────────────────────────

    private final ObservableList<EvacuationSite> centerData  = FXCollections.observableArrayList();
    private final ObservableList<EvacueeRecord>  evacueeData = FXCollections.observableArrayList();
    private boolean mapInitialized = false;
    private String selectedCenterId;

    private SessionMode sessionMode = new PublicSessionMode();

    // Strongly typed to prevent JavaFX garbage collecting the JS bridge
    private KioskMapBridge mapBridge;

    private RosterTabHelper rosterTabHelper;

    // ─── Lifecycle ─────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        configureCenterTableColumns();
        configureSearch();
        applySessionMode();
        registerObservers();
        loadInitialMap();
        wireEventDrawer();
        wireDetailModal();

        KioskDataSubject.getInstance().refreshAll();
    }

    public void setSessionMode(SessionMode mode) {
        if (mode == null) return;
        this.sessionMode = mode;
        applySessionMode();
    }

    // ─── Configuration ─────────────────────────────────────────────────────

    private void configureCenterTableColumns() {
        // colCenterId removed — not shown in kiosk read-only view
        bindStringColumn(colCenterName,      EvacuationSite::title);
        bindStringColumn(colCenterAddress,   EvacuationSite::address);
        bindStringColumn(colCenterBarangay,  EvacuationSite::barangay);
        bindStringColumn(colCenterStatus,    EvacuationSite::status);
        bindStringColumn(colCenterCreatedAt, EvacuationSite::createdAt);

        if (centerTableView != null) {
            centerTableView.setItems(centerData);
        }
    }

    private void configureSearch() {
        if (searchField == null || centerTableView == null) return;

        SearchTableUtility.setupSearch(
                searchField,
                centerTableView,
                centerData,
                (site, query) -> {
                    if (query == null || query.isBlank()) return true;
                    String q = query.toLowerCase();
                    return matches(site.title(),    q)
                            || matches(site.address(),  q)
                            || matches(site.barangay(), q);
                }
        );
    }

    private void applySessionMode() {
        sessionMode.applyTo(new SessionContext(
                btnBarangayLogin, btnAdminLogin, searchField, btnEventsToggle));
    }

    private void registerObservers() {
        KioskDataSubject subject = KioskDataSubject.getInstance();
        subject.registerObserver(this);
        if (eventCellController != null) {
            subject.registerObserver(eventCellController);
        }
    }

    private void wireEventDrawer() {
        if (eventCellController == null) return;
        eventCellController.setOnEventSelected(this::focusCenterOnMap);
    }

    private void wireDetailModal() {
        // Protect static tabs from being closed when tabClosingPolicy="ALL_TABS"
        rosterTabHelper = new RosterTabHelper(bottomTabPane, evacueeData);
        rosterTabHelper.initialize(tabCentersOverview, null);

        // Offset the modal from the bottom-left edge (replaces StackPane.margin
        // which SceneBuilder cannot apply across fx:include boundaries)
        if (detailModal != null) {
            detailModal.setTranslateX(16.0);
            detailModal.setTranslateY(-16.0);
        }

        // When "View Evacuee Roster" is clicked in the modal, open a closeable tab
        if (detailModalController != null) {
            detailModalController.setOnViewRoster(this::openRosterTab);
        }
    }

    // ─── DashboardViewObserver hooks ───────────────────────────────────────

    @Override
    public void onCentersRefreshed(List<EvacuationSite> centers) {
        centerData.setAll(centers);
        refreshMapMarkers(centers);
    }

    @Override
    public void onEvacueesRefreshed(List<EvacueeRecord> evacuees) {
        evacueeData.setAll(evacuees);
    }

    @Override
    public void onCenterEventReceived(CenterEvent event) {
        // Update the open roster tab for this center if one exists
        if (selectedCenterId != null
                && String.valueOf(event.centerId()).equals(selectedCenterId)) {
            findCenterById(selectedCenterId).ifPresent(this::openRosterTab);
        }
    }

    // ─── UI event handlers ─────────────────────────────────────────────────

    @FXML private void handleMenuClick() {
        if (eventCellController != null) {
            eventCellController.toggleDrawer();
        }
    }

    @FXML private void handleAdminLogin()    { Router.getInstance().navigate(Route.ADMIN_LOGIN); }
    @FXML private void handleBarangayLogin() { Router.getInstance().navigate(Route.BARANGAY_LOGIN); }

    // ─── Detail modal ──────────────────────────────────────────────────────

    private void showDetailModal(EvacuationSite site) {
        if (detailModalController == null) return;
        selectedCenterId = site.id();
        detailModalController.show(site);
    }

    private void openRosterTab(EvacuationSite site) {
        if (rosterTabHelper == null) return;
        rosterTabHelper.openRosterTab(site);
    }

    // ─── Map ───────────────────────────────────────────────────────────────

    private void loadInitialMap() {
        if (miniMapWebView == null) return;

        miniMapWebView.getEngine().setJavaScriptEnabled(true);
        miniMapWebView.getEngine().setUserAgent("CivicGuard/1.0");
        miniMapWebView.getEngine().setOnAlert(evt ->
                System.out.println(KioskConstants.LOG_MAP_DEBUG + evt.getData()));

        // CRITICAL FIX: JavaFX WebView missing mouse-up bug.
        // If the user drags the map downward the cursor enters the TabPane below,
        // JavaFX stops sending mouse events to the WebView, and Leaflet never
        // receives "mouseup" — the map locks in a dragging state permanently.
        // Force a synthetic mouseup when the cursor leaves the WebView.
        miniMapWebView.setOnMouseExited(e -> {
            try {
                miniMapWebView.getEngine().executeScript(
                        "var evt = new MouseEvent('mouseup', {bubbles:true,cancelable:true,clientX:0,clientY:0});" +
                                "document.dispatchEvent(evt);"
                );
            } catch (Exception ex) {
                // Non-fatal — highlight is a non-essential affordance
            }
        });

        miniMapWebView.getEngine().getLoadWorker().stateProperty()
                .addListener((obs, oldS, newS) -> {
                    if (newS == Worker.State.SUCCEEDED) {
                        installJsBridge();
                    }
                });

        refreshMapMarkers(List.of());
    }

    private void installJsBridge() {
        try {
            JSObject window = (JSObject) miniMapWebView.getEngine().executeScript("window");
            this.mapBridge = new KioskMapBridge(this);
            window.setMember(KioskConstants.JS_BRIDGE_MEMBER, this.mapBridge);
        } catch (Exception e) {
            System.err.println(KioskConstants.LOG_PREFIX + "Failed to install JS Bridge: " + e.getMessage());
        }
    }

    private int startTileServerSafely() {
        try {
            return TilePrefetchService.getInstance().startServer();
        } catch (Exception ex) {
            System.err.println(KioskConstants.LOG_PREFIX
                    + "Tile server start failed: " + ex.getMessage());
            return -1;
        }
    }

    private void refreshMapMarkers(List<EvacuationSite> centers) {
        if (miniMapWebView == null) return;
        if (!mapInitialized) {
            int tilePort = startTileServerSafely();
            miniMapWebView.getEngine().loadContent(
                    BrgyMapHtmlProvider.getMapHTML(
                            buildCentersJson(centers),
                            KioskConstants.DEFAULT_MAP_LAT,
                            KioskConstants.DEFAULT_MAP_LNG,
                            KioskConstants.DEFAULT_MAP_ZOOM,
                            tilePort));
            mapInitialized = true;
            return;
        }
        String json = buildCentersJson(centers);
        Platform.runLater(() ->
                miniMapWebView.getEngine().executeScript(
                        "if(window.updateMarkers) window.updateMarkers(" + json + ");"));
    }

    private void focusCenterOnMap(String centerId) {
        triggerMapMarkerHighlight(centerId);
        findCenterById(centerId).ifPresent(this::showDetailModal);
    }

    private void triggerMapMarkerHighlight(String centerId) {
        if (miniMapWebView == null || centerId == null) return;
        try {
            miniMapWebView.getEngine().executeScript(
                    String.format(KioskConstants.JS_HIGHLIGHT_MARKER_FMT, centerId));
        } catch (Exception ignored) { }
    }

    private Optional<EvacuationSite> findCenterById(String centerId) {
        return centerData.stream()
                .filter(s -> s.id().equals(centerId))
                .findFirst();
    }

    private static String buildCentersJson(List<EvacuationSite> centers) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < centers.size(); i++) {
            EvacuationSite c = centers.get(i);
            if (i > 0) sb.append(',');
            sb.append(String.format(
                    "{\"id\":\"%s\",\"name\":\"%s\",\"lat\":%s,\"lng\":%s,\"focal\":%b}",
                    escape(c.id()), escape(c.title()), c.latitude(), c.longitude(), false));
        }
        return sb.append(']').toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ");
    }

    private static boolean matches(String field, String lowerQuery) {
        return field != null && field.toLowerCase().contains(lowerQuery);
    }

    private static <T> void bindStringColumn(
            TableColumn<T, String> col,
            java.util.function.Function<T, String> getter) {
        if (col == null) return;
        col.setCellValueFactory(c -> new SimpleStringProperty(getter.apply(c.getValue())));
    }

    // ─── JavaScript bridge ─────────────────────────────────────────────────

    /**
     * Must be public with a public constructor so JavaFX WebEngine
     * reflection can invoke methods on it from JavaScript.
     */
    public static class KioskMapBridge {

        private final KioskDashboardController controller;

        public KioskMapBridge(KioskDashboardController controller) {
            this.controller = controller;
        }

        public void onMarkerClick(String centerId) {
            javafx.application.Platform.runLater(() ->
                    controller.focusCenterOnMap(centerId));
        }

        public void toggleHomeButton(boolean show) {
            // Intentionally empty for kiosk — prevents JS errors
        }
    }
}
