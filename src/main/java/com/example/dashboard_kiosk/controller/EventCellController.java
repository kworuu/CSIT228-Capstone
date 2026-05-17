package com.example.dashboard_kiosk.controller;

import com.example.dashboard_kiosk.KioskConstants;
import com.example.dashboard_kiosk.observer.DashboardViewObserver;
import com.example.util.CenterEvent;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller for the right-edge sliding events drawer.
 *
 * <p>Renders a scrollable list of recent center-status events. Each card
 * is clickable and forwards its {@code centerId} to the parent controller
 * via {@link #setOnEventSelected(Consumer)}.</p>
 */
public final class EventCellController implements Initializable, DashboardViewObserver {

    // ── FXML bindings ──────────────────────────────────────────────────────

    @FXML private VBox eventCellRoot;
    @FXML private VBox alertsContainerVBox;
    @FXML private TextField searchField;

    // ── Drawer animation state ─────────────────────────────────────────────

    public static final double DRAWER_WIDTH_PX = 300.0;
    public static final double SLIDE_DURATION_MS = 280.0;

    private boolean drawerOpen = false;
    
    // ── Events state ───────────────────────────────────────────────────────
    private final List<CenterEvent> allEvents = new ArrayList<>();

    // ── External callback ──────────────────────────────────────────────────

    /** Invoked with the {@code centerId} of any clicked event card. */
    private Consumer<String> onEventSelected;
    
    private Runnable onToggleComplete;

    // ── Initialisation ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (eventCellRoot != null) {
            eventCellRoot.setTranslateX(DRAWER_WIDTH_PX);
            eventCellRoot.setVisible(false); // hide bounds to prevent stealing clicks
        }
        
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> filterEvents());
        }
    }

    public void setOnEventSelected(Consumer<String> handler) {
        this.onEventSelected = handler;
    }

    public void setOnToggleComplete(Runnable handler) {
        this.onToggleComplete = handler;
    }

    public boolean isDrawerOpen() {
        return drawerOpen;
    }

    // ── Observer hooks ─────────────────────────────────────────────────────

    @Override
    public void onEventsRefreshed(List<CenterEvent> events) {
        this.allEvents.clear();
        if (events != null) {
            this.allEvents.addAll(events);
        }
        filterEvents();
    }

    @Override
    public void onCenterEventReceived(CenterEvent event) {
        if (event == null) return;
        // Remove older event for the same center to prevent duplicates in the UI
        this.allEvents.removeIf(e -> e.centerId() == event.centerId());
        this.allEvents.add(0, event);
        filterEvents();
    }

    // ── Public mutators (still usable from tests / non-observer callers) ──

    public void renderAll(List<CenterEvent> events) {
        this.allEvents.clear();
        if (events != null) {
            this.allEvents.addAll(events);
        }
        filterEvents();
    }

    public void prependEvent(CenterEvent event) {
        if (event == null) return;
        // Remove older event for the same center to prevent duplicates in the UI
        this.allEvents.removeIf(e -> e.centerId() == event.centerId());
        this.allEvents.add(0, event);
        filterEvents();
    }
    
    private void filterEvents() {
        if (alertsContainerVBox == null) return;
        
        String query = searchField != null ? searchField.getText() : "";
        final String q = query == null ? "" : query.toLowerCase();
        
        alertsContainerVBox.getChildren().clear();
        for (CenterEvent e : allEvents) {
            if (q.isEmpty() || e.centerName().toLowerCase().contains(q) || e.eventLabel().toLowerCase().contains(q)) {
                alertsContainerVBox.getChildren().add(buildEventCard(e));
            }
        }
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
            if (onToggleComplete != null) {
                onToggleComplete.run();
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
