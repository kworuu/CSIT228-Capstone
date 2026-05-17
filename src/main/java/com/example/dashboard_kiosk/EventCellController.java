package com.example.dashboard_kiosk;

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

public class EventCellController implements Initializable {

    @FXML private VBox eventCellRoot;
    @FXML private VBox alertsContainer; // Ensure this matches fx:id in EventCell.fxml

    private static final double DRAWER_WIDTH = 300.0;
    private static final double ANIM_MS = 280.0;
    private boolean isOpen = false;

    // Callback interface so KioskDashboardController knows when an event is clicked
    private EventClickListener eventClickListener;

    public interface EventClickListener {
        void onEventClicked(String centerId);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (eventCellRoot != null) {
            eventCellRoot.setTranslateX(DRAWER_WIDTH);
        }
    }

    public void setEventClickListener(EventClickListener listener) {
        this.eventClickListener = listener;
    }

    public void setEvents(List<CenterEvent> events) {
        if (alertsContainer == null) return;
        alertsContainer.getChildren().clear();
        for (CenterEvent e : events) {
            alertsContainer.getChildren().add(createEventCard(e));
        }
    }

    public void addEventToTop(CenterEvent event) {
        if (alertsContainer != null) {
            alertsContainer.getChildren().add(0, createEventCard(event));
        }
    }

    // Creates the visual card for an Event dynamically
    private VBox createEventCard(CenterEvent event) {
        VBox card = new VBox(5);
        card.getStyleClass().addAll("alert-item", "alert-item-warning");

        Label timeLbl = new Label(event.timestamp());
        timeLbl.getStyleClass().add("alert-time");

        Label titleLbl = new Label(event.centerName());
        titleLbl.getStyleClass().add("alert-title");

        Label bodyLbl = new Label(event.eventLabel());
        bodyLbl.getStyleClass().add("alert-qty");

        card.getChildren().addAll(timeLbl, titleLbl, bodyLbl);

        // Make the card clickable!
        card.setStyle("-fx-cursor: hand;");
        card.setOnMouseClicked(e -> {
            if (eventClickListener != null) {
                eventClickListener.onEventClicked(String.valueOf(event.centerId()));
            }
        });

        return card;
    }

    public void toggleDrawer() {
        if (eventCellRoot == null) return;
        TranslateTransition slide = new TranslateTransition(Duration.millis(ANIM_MS), eventCellRoot);
        slide.setToX(isOpen ? DRAWER_WIDTH : 0);
        isOpen = !isOpen;
        slide.play();
    }

    @FXML private void handleClose() {
        if (isOpen) toggleDrawer();
    }
}