package com.example.dashboard_kiosk.controller;

import com.example.dashboard_kiosk.KioskConstants;
import com.example.dashboard_kiosk.model.EvacuationSite;
import com.example.dashboard_kiosk.observer.DashboardViewObserver;
import com.example.dashboard_kiosk.observer.KioskDataSubject;
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
    @FXML private AnchorPane            detailModal;
    @FXML private DetailModalController detailModalController;

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
    private final ObservableList<EvacuationSite> centerData =
            FXCollections.observableArrayList();
    private final ObservableList<EvacueeRecord> evacueeData =
            FXCollections.observableArrayList();
    private String      selectedCenterId;
    private SessionMode sessionMode = new PublicSessionMode();

    // Strongly typed to prevent JavaFX garbage collecting the JS bridge
    private KioskMapBridge mapBridge;
    private RosterTabHelper rosterTabHelper;

    // ─── Map state ─────────────────────────────────────────────────────────
    private boolean mapInitialized = false; // True after WebView's SUCCEEDED state
    private boolean mapContentLoaded = false; // True after loadMapWithData() is called

    // ─── Lifecycle ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        configureCenterTableColumns();
        configureSearch();
        applySessionMode();
        registerObservers();
        setupMapShell(); // Prepare WebView, but don't load content yet
        wireEventDrawer();
        wireDetailModal();
        KioskDataSubject.getInstance().refreshAll(); // Triggers onCentersRefreshed, which will now load the map
    }

    public void setSessionMode(SessionMode mode) {
        if (mode == null) return;
        this.sessionMode = mode;
        applySessionMode();
    }

    // ─── Configuration ─────────────────────────────────────────────────────
    private void configureCenterTableColumns() {
        bindStringColumn(colCenterName,      EvacuationSite::title);
        bindStringColumn(colCenterAddress,   EvacuationSite::address);
        bindStringColumn(colCenterBarangay,  EvacuationSite::barangay);
        bindStringColumn(colCenterStatus,    EvacuationSite::status);
        bindStringColumn(colCenterCreatedAt, EvacuationSite::createdAt);
        if (centerTableView != null) centerTableView.setItems(centerData);
    }

    private void configureSearch() {
        if (searchField == null || centerTableView == null) return;
        SearchTableUtility.setupSearch(
                searchField, centerTableView, centerData,
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
        if (eventCellController != null) subject.registerObserver(eventCellController);
    }

    private void wireEventDrawer() {
        if (eventCellController == null) return;
        eventCellController.setOnEventSelected(this::focusCenterOnMap);
    }

    private void wireDetailModal() {
        rosterTabHelper = new RosterTabHelper(bottomTabPane, evacueeData);
        rosterTabHelper.initialize(tabCentersOverview, null);
        if (detailModal != null) {
            detailModal.setTranslateX(16.0);
            detailModal.setTranslateY(-16.0);
        }
        if (detailModalController != null) {
            detailModalController.setOnViewRoster(this::openRosterTab);
        }
    }

    // ─── DashboardViewObserver hooks ───────────────────────────────────────
    @Override
    public void onCentersRefreshed(List<EvacuationSite> centers) {
        centerData.setAll(centers);

        // --- FIX: Load map with data on first refresh, like the admin panel does ---
        if (!mapContentLoaded && !centers.isEmpty()) {
            loadMapWithData(centers);
            mapContentLoaded = true;
        } else if (mapContentLoaded) {
            // For subsequent updates, use the more efficient JS bridge
            pushMarkersToMap(centers);
        }
    }

    @Override
    public void onEvacueesRefreshed(List<EvacueeRecord> evacuees) {
        evacueeData.setAll(evacuees);
    }

    @Override
    public void onCenterEventReceived(CenterEvent event) {
        if (selectedCenterId != null
                && String.valueOf(event.centerId()).equals(selectedCenterId)) {
            findCenterById(selectedCenterId).ifPresent(this::openRosterTab);
        }
    }

    // ─── UI event handlers ─────────────────────────────────────────────────
    @FXML private void handleMenuClick() {
        if (eventCellController != null) eventCellController.toggleDrawer();
    }

    @FXML private void handleAdminLogin()    { Router.getInstance().navigate(Route.ADMIN_LOGIN);    }
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

    /** Prepares the WebView component but does not load any HTML content. */
    private void setupMapShell() {
        if (miniMapWebView == null) return;

        miniMapWebView.getEngine().setJavaScriptEnabled(true);
        miniMapWebView.getEngine().setUserAgent("CivicGuard/1.0");
        miniMapWebView.getEngine().setOnAlert(evt ->
                System.out.println(KioskConstants.LOG_MAP_DEBUG + evt.getData()));

        // CRITICAL FIX: JavaFX WebView missing mouse-up bug.
        miniMapWebView.setOnMouseExited(e -> {
            try {
                miniMapWebView.getEngine().executeScript(
                        "var evt = new MouseEvent('mouseup'," +
                                "{bubbles:true,cancelable:true,clientX:0,clientY:0});" +
                                "document.dispatchEvent(evt);"
                );
            } catch (Exception ex) { /* non-fatal */ }
        });
    }

    /**
     * Loads the map's HTML content, injecting the provided center data from the start.
     * This is called by the observer once data is available.
     */
    private void loadMapWithData(List<EvacuationSite> centers) {
        if (miniMapWebView == null) return;

        miniMapWebView.getEngine().getLoadWorker().stateProperty()
                .addListener((obs, oldS, newS) -> {
                    if (newS != Worker.State.SUCCEEDED) return;
                    installJsBridge();
                    mapInitialized = true;
                });

        int tilePort = startTileServerSafely();

        // Use the same city-wide boundaries as the admin panel for consistency
        double swLat = 10.250429;
        double swLng = 123.864302;
        double neLat = 10.503349;
        double neLng = 123.877159;
        double centerLat = (swLat + neLat) / 2.0;
        double centerLng = (swLng + neLng) / 2.0;
        int maxZoom = 17;

        String centersJson = buildCentersJson(centers);

        miniMapWebView.getEngine().loadContent(
                BrgyMapHtmlProvider.getCityMapHTML(
                        centersJson,
                        swLat, swLng, neLat, neLng,
                        centerLat, centerLng,
                        maxZoom, tilePort
                )
        );
    }

    private void installJsBridge() {
        try {
            JSObject window = (JSObject) miniMapWebView.getEngine().executeScript("window");
            this.mapBridge  = new KioskMapBridge(this);
            window.setMember(KioskConstants.JS_BRIDGE_MEMBER, this.mapBridge);
        } catch (Exception e) {
            System.err.println(KioskConstants.LOG_PREFIX + "Failed to install JS Bridge: " + e.getMessage());
        }
    }

    private int startTileServerSafely() {
        try {
            return TilePrefetchService.getInstance().startServer();
        } catch (Exception ex) {
            System.err.println(KioskConstants.LOG_PREFIX + "Tile server start failed: " + ex.getMessage());
            return -1;
        }
    }

    /** Executes window.updateMarkers(...) in the live page for subsequent data refreshes. */
    private void pushMarkersToMap(List<EvacuationSite> centers) {
        if (!mapInitialized) return; // Guard against calls before the map is ready

        String json   = buildCentersJson(centers);
        String script = "if(window.updateMarkers) window.updateMarkers(" + json + ");";
        Platform.runLater(() -> {
            try {
                miniMapWebView.getEngine().executeScript(script);
            } catch (Exception e) {
                System.err.println(KioskConstants.LOG_PREFIX + "updateMarkers failed: " + e.getMessage());
            }
        });
    }

    private void focusCenterOnMap(String centerId) {
        triggerMapMarkerHighlight(centerId);
        findCenterById(centerId).ifPresent(this::showDetailModal);
    }

    private void triggerMapMarkerHighlight(String centerId) {
        if (miniMapWebView == null || centerId == null || !mapInitialized) return;
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
            if (c.latitude() == 0.0 || c.longitude() == 0.0) continue; // Skip invalid locations
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
    public static class KioskMapBridge {
        private final KioskDashboardController controller;
        public KioskMapBridge(KioskDashboardController controller) {
            this.controller = controller;
        }
        public void onMarkerClick(String centerId) {
            Platform.runLater(() -> controller.focusCenterOnMap(centerId));
        }
        public void toggleHomeButton(boolean show) {
            // Intentionally empty for kiosk — prevents JS errors
        }
    }
}