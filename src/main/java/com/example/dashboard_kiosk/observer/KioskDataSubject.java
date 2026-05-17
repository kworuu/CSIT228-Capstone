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

public final class KioskDataSubject implements CenterEventObserver {

    private static final KioskDataSubject INSTANCE = new KioskDataSubject();

    private final List<DashboardViewObserver> observers = new CopyOnWriteArrayList<>();

    private final KioskDataService dataService = KioskDataService.getInstance();

    private KioskDataSubject() {
        CenterEventManager.getInstance().addObserver(this);
    }

    public static KioskDataSubject getInstance() { return INSTANCE; }

    public void registerObserver(DashboardViewObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unregisterObserver(DashboardViewObserver observer) {
        observers.remove(observer);
    }

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

    @Override
    public void onCenterUpdated(CenterEvent event) {
        runOnFx(() -> {
            observers.forEach(o -> o.onCenterEventReceived(event));
        });
        refreshCenters();
        refreshEvacuees();
    }

    private static void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}