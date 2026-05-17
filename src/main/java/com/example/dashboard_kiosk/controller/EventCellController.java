package com.example.dashboard_kiosk.controller;

import com.example.dashboard_kiosk.KioskConstants;
import com.example.dashboard_kiosk.observer.DashboardViewObserver;
import com.example.util.CenterEvent;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller for the right-edge sliding events drawer.
 *
 * <p>Renders a scrollable list of recent center-status events. Each card
 * is clickable and forwards its {@code centerId} to the parent controller
 * via {@link #setOnEventSelected(Consumer)}.</p>
 *
 * <p>Implements {@link DashboardViewObserver} so it can be registered
 * directly against {@link com.example.dashboard_kiosk.observer.KioskDataSubject}
 * — the parent controller no longer needs to forward refreshes manually.</p>
 */
public final class EventCellController implements Initializable, DashboardViewObserver {

    // ── FXML bindings ──────────────────────────────────────────────────────

    @FXML private VBox eventCellRoot;
    @FXML private VBox alertsContainerVBox;

    // ── Drawer animation state ─────────────────────────────────────────────

    private static final double DRAWER_WIDTH_PX = 300.0;
    private static final double SLIDE_DURATION_MS = 280.0;

    private boolean drawerOpen = false;

    // ── External callback ──────────────────────────────────────────────────

    /** Invoked with the {@code centerId} of any clicked event card. */
    private Consumer<String> onEventSelected;

    // ── Initialisation ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (eventCellRoot != null) {
            eventCellRoot.setTranslateX(DRAWER_WIDTH_PX);
            eventCellRoot.setVisible(false); // Fix: hide bounds to prevent stealing clicks
        }
    }

    public void setOnEventSelected(Consumer<String> handler) {
        this.onEventSelected = handler;
    }

    // ── Observer hooks ─────────────────────────────────────────────────────

    @Override
    public void onEventsRefreshed(List<CenterEvent> events) {
        renderAll(events);
    }

    @Override
    public void onCenterEventReceived(CenterEvent event) {
        prependEvent(event);
    }

    // ── Public mutators (still usable from tests / non-observer callers) ──

    public void renderAll(List<CenterEvent> events) {
        if (alertsContainerVBox == null) return;
        alertsContainerVBox.getChildren().clear();
        for (CenterEvent e : events) {
            alertsContainerVBox.getChildren().add(buildEventCard(e));
        }
    }

    public void prependEvent(CenterEvent event) {
        if (alertsContainerVBox == null || event == null) return;
        alertsContainerVBox.getChildren().add(0, buildEventCard(event));
    }

    // ── Drawer control ─────────────────────────────────────────────────────

    public void toggleDrawer() {
        if (eventCellRoot == null) return;
        
        if (!drawerOpen) {
            eventCellRoot.setVisible(true); // make visible before sliding in
        }

        TranslateTransition slide = new TranslateTransition(
                Duration.millis(SLIDE_DURATION_MS), eventCellRoot);
        slide.setToX(drawerOpen ? DRAWER_WIDTH_PX : 0);
        
        boolean wasOpen = drawerOpen;
        slide.setOnFinished(e -> {
            if (wasOpen) {
                eventCellRoot.setVisible(false); // make invisible after sliding out
            }
        });

        drawerOpen = !drawerOpen;
        slide.play();
    }

    @FXML
    private void handleClose() {
        if (drawerOpen) toggleDrawer();
    }

    // ── Card builder ───────────────────────────────────────────────────────

    /**
     * Builds a single warning-styled alert card for an incoming
     * {@link CenterEvent}. The whole card is clickable.
     */
    private VBox buildEventCard(CenterEvent event) {
        VBox card = new VBox(5);
        card.getStyleClass().addAll(
                KioskConstants.CSS_ALERT_ITEM,
                KioskConstants.CSS_ALERT_WARNING);

        Label timeLabel = new Label(event.timestamp());
        timeLabel.getStyleClass().add(KioskConstants.CSS_ALERT_TIME);

        Label titleLabel = new Label(event.centerName());
        titleLabel.getStyleClass().add(KioskConstants.CSS_ALERT_TITLE);

        Label bodyLabel = new Label(event.eventLabel());
        bodyLabel.getStyleClass().add(KioskConstants.CSS_ALERT_BODY);

        card.getChildren().addAll(timeLabel, titleLabel, bodyLabel);

        card.setStyle("-fx-cursor: hand;");
        card.setOnMouseClicked(e -> {
            if (onEventSelected != null) {
                onEventSelected.accept(String.valueOf(event.centerId()));
            }
        });

        return card;
    }
}