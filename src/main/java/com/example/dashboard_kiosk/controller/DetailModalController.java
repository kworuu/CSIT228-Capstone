package com.example.dashboard_kiosk.controller;

import com.example.dashboard_kiosk.KioskConstants;
import com.example.dashboard_kiosk.model.EvacuationSite;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for the floating bottom-left detail-modal overlay (read-only / kiosk).
 */
public final class DetailModalController {

    @FXML private AnchorPane modalRoot;
    @FXML private ImageView  imgOverlayCenter;
    @FXML private Label      labelOverlayName;
    @FXML private Label      labelOverlayAddress;
    @FXML private VBox       vboxOverlayItemsBox;
    @FXML private FlowPane   flowPaneOverlayPills;
    @FXML private Label      labelOverlayEvent;
    @FXML private Label      labelOverlayTimestamp;
    @FXML private Button     buttonViewRoster;
    @FXML private Button     buttonOverlayClose;

    private EvacuationSite currentSite;
    private Consumer<EvacuationSite> onViewRoster;

    public AnchorPane getRoot() { return modalRoot; }

    public void setOnViewRoster(Consumer<EvacuationSite> handler) {
        this.onViewRoster = handler;
    }

    public void show(EvacuationSite site) {
        if (site == null) return;
        this.currentSite = site;

        labelOverlayName.setText(site.title());
        labelOverlayAddress.setText(site.address());
        
        labelOverlayEvent.setText(site.eventLabel() != null ? site.eventLabel() : "No active event");
        labelOverlayTimestamp.setText("Updated: " + (site.updatedAt() != null ? site.updatedAt() : "—"));

        renderSupplies(site.supplies() != null ? site.supplies() : List.of());

        if (imgOverlayCenter != null) {
            if (site.photoPath() != null && !site.photoPath().isBlank()) {
                try {
                    imgOverlayCenter.setImage(new Image(getClass().getResourceAsStream(site.photoPath())));
                } catch (Exception e) {
                    imgOverlayCenter.setImage(null);
                    System.err.println("Modal could not load image: " + site.photoPath());
                }
            } else {
                imgOverlayCenter.setImage(null);
            }
        }

        modalRoot.setVisible(true);
        modalRoot.setManaged(true);
    }

    public void hide() {
        if (modalRoot == null) return;
        modalRoot.setVisible(false);
        modalRoot.setManaged(false);
    }

    @FXML
    private void handleClose() {
        hide();
    }

    @FXML
    private void handleViewRoster() {
        if (onViewRoster != null && currentSite != null) {
            onViewRoster.accept(currentSite);
        }
    }

    private void renderSupplies(List<String> supplies) {
        if (flowPaneOverlayPills == null) return;
        flowPaneOverlayPills.getChildren().clear();
        for (String supply : supplies) {
            Label pill = new Label(supply);
            pill.getStyleClass().add("supply-tag");
            flowPaneOverlayPills.getChildren().add(pill);
        }
    }
}
