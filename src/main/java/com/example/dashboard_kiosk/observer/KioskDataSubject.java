package com.example.dashboard_kiosk.observer;

import com.example.dashboard_kiosk.model.EvacuationSite;
import com.example.dashboard_kiosk.service.KioskDataService;
import com.example.model.EvacueeRecord;
import com.example.util.CenterEvent;
import com.example.util.CenterEventManager;
import com.example.util.CenterEventObserver;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton Observer-pattern subject for the kiosk dashboard.
 *
 * <p>This class decouples view components from both the database layer
 * ({@link KioskDataService}) and the underlying event bus
 * ({@link CenterEventManager}). View components register here through
 * {@link #registerObserver(DashboardViewObserver)} and the subject
 * fans every refresh out to all registered listeners on the JavaFX
 * Application Thread.</p>
 *
 * <h3>Wiring</h3>
 * <pre>{@code
 *  KioskDataSubject subject = KioskDataSubject.getInstance();
 *  subject.registerObserver(centerTableObserver);
 *  subject.registerObserver(evacueeTableObserver);
 *  subject.registerObserver(mapObserver);
 *  subject.refreshAll();        // pull from DB, fan out to everyone
 * }</pre>
 */
public final class KioskDataSubject implements CenterEventObserver {

    private static final KioskDataSubject INSTANCE = new KioskDataSubject();

    /** Thread-safe observer list — writes are rare, reads are frequent. */
    private final List<DashboardViewObserver> observers = new CopyOnWriteArrayList<>();

    private final KioskDataService dataService = KioskDataService.getInstance();

    private KioskDataSubject() {
        // Bridge upstream event bus → this subject
        CenterEventManager.getInstance().addObserver(this);
    }

    /** @return the process-wide subject instance. */
    public static KioskDataSubject getInstance() { return INSTANCE; }

    // ── Observer registration ──────────────────────────────────────────────

    public void registerObserver(DashboardViewObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unregisterObserver(DashboardViewObserver observer) {
        observers.remove(observer);
    }

    // ── Refresh API ────────────────────────────────────────────────────────

    /** Reloads centers, evacuees, and events, then fans each refresh out. */
    public void refreshAll() {
        refreshCenters();
        refreshEvacuees();
        refreshEvents();
    }

    public void refreshCenters() {
        List<EvacuationSite> sites = dataService.loadAllCenters();
        runOnFx(() -> observers.forEach(o -> o.onCentersRefreshed(sites)));
    }

    public void refreshEvacuees() {
        List<EvacueeRecord> rows = dataService.loadAllEvacuees();
        runOnFx(() -> observers.forEach(o -> o.onEvacueesRefreshed(rows)));
    }

    public void refreshEvents() {
        List<CenterEvent> events = dataService.loadRecentEvents();
        runOnFx(() -> observers.forEach(o -> o.onEventsRefreshed(events)));
    }

    // ── CenterEventObserver bridge ─────────────────────────────────────────

    /**
     * Invoked by {@link CenterEventManager} on any center status change.
     * The subject refreshes the centers + evacuees tables and notifies
     * observers of the specific incoming event so the drawer can prepend it.
     */
    @Override
    public void onCenterUpdated(CenterEvent event) {
        runOnFx(() -> {
            observers.forEach(o -> o.onCenterEventReceived(event));
        });
        refreshCenters();
        refreshEvacuees();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}