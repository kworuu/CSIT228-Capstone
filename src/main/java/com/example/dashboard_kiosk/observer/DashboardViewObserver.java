package com.example.dashboard_kiosk.observer;

import com.example.dashboard_kiosk.model.EvacuationSite;
import com.example.model.EvacueeRecord;
import com.example.util.CenterEvent;

import java.util.List;

public interface DashboardViewObserver {

    default void onCentersRefreshed(List<EvacuationSite> centers) { }

    default void onEvacueesRefreshed(List<EvacueeRecord> evacuees) { }

    default void onEventsRefreshed(List<CenterEvent> events) { }

    default void onCenterEventReceived(CenterEvent event) { }
}