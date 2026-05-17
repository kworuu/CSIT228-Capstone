package com.example.dashboard_kiosk.observer;

import com.example.dashboard_kiosk.model.EvacuationSite;
import com.example.model.EvacueeRecord;
import com.example.util.CenterEvent;

import java.util.List;

/**
 * Observer contract implemented by every UI component that needs to react
 * to dashboard-wide data refreshes (the centers table, the evacuees table,
 * the events drawer, and the WebView map).
 *
 * <p>The {@link com.example.dashboard_kiosk.observer.KioskDataSubject}
 * fans events out to registered observers on the JavaFX Application Thread.
 * Default implementations are no-ops so each observer overrides only what
 * it cares about.</p>
 */
public interface DashboardViewObserver {

    /** Fired after the centers list is reloaded from the database. */
    default void onCentersRefreshed(List<EvacuationSite> centers) { }

    /** Fired after the evacuee list is reloaded from the database. */
    default void onEvacueesRefreshed(List<EvacueeRecord> evacuees) { }

    /** Fired after the event feed is reloaded from the database. */
    default void onEventsRefreshed(List<CenterEvent> events) { }

    /** Fired in real-time when a single new center-status event arrives. */
    default void onCenterEventReceived(CenterEvent event) { }
}