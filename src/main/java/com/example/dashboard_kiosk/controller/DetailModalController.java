package com.example.dashboard_kiosk.controller;

import com.example.dashboard_kiosk.KioskConstants;
import com.example.dashboard_kiosk.model.EvacuationSite;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for the floating bottom-left detail-modal overlay (read-only / kiosk).
 *
 * <p>Mirrors the structure of {@code vboxMapOverlay} in BrgyDashboard.fxml.
 * Users cannot edit, delete, or update — the only permitted action is
 * opening the Evacuee Roster via {@link #handleViewRoster()}.</p>
 *
 * <p>The modal is loaded once at startup by the parent
 * {@link KioskDashboardController} and kept {@code visible=false} /
 * {@code managed=false} until {@link #show(EvacuationSite)} is called.</p>
 */
public final class DetailModalController {

    // ── FXML bindings ──────────────────────────────────────────────────────

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

    // ── State ──────────────────────────────────────────────────────────────

    /** The site currently displayed; retained so roster callback can pass it along. */
    private EvacuationSite currentSite;

    // ── Callback ──────────────────────────────────────────────────────────

    /**
     * Invoked when the user clicks "View Evacuee Roster".
     * Receives the currently displayed {@link EvacuationSite} so the parent
     * controller can open a closeable roster tab labelled with the center name.
     */
    private Consumer<EvacuationSite> onViewRoster;

    // ── Public API ─────────────────────────────────────────────────────────

    /** @return the root node so the parent can attach it to its scene graph. */
    public AnchorPane getRoot() { return modalRoot; }

    /**
     * Registers the callback invoked when "View Evacuee Roster" is clicked.
     *
     * @param handler receives the currently shown {@link EvacuationSite}
     */
    public void setOnViewRoster(Consumer<EvacuationSite> handler) {
        this.onViewRoster = handler;
    }

    /**
     * Populates the modal with the given site and makes it visible.
     *
     * @param site the selected evacuation center; ignored if {@code null}
     */
    public void show(EvacuationSite site) {
        if (site == null) return;
        this.currentSite = site;

        labelOverlayName.setText(site.title());
        labelOverlayAddress.setText(site.address());
        labelOverlayEvent.setText("No active event");
        labelOverlayTimestamp.setText("Updated: —");

        renderSupplies(deriveSupplies(site));

        modalRoot.setVisible(true);
        modalRoot.setManaged(true);
    }

    /** Hides the modal without destroying it. */
    public void hide() {
        if (modalRoot == null) return;
        modalRoot.setVisible(false);
        modalRoot.setManaged(false);
    }

    // ── FXML event handlers ────────────────────────────────────────────────

    @FXML
    private void handleClose() {
        hide();
    }

    /**
     * Fired when the user clicks "View Evacuee Roster".
     * Delegates to the parent controller via {@link #onViewRoster} so it can
     * open a new closeable tab in the TabPane, labelled with this center's name.
     */
    @FXML
    private void handleViewRoster() {
        if (onViewRoster != null && currentSite != null) {
            onViewRoster.accept(currentSite);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void renderSupplies(List<String> supplies) {
        if (flowPaneOverlayPills == null) return;
        flowPaneOverlayPills.getChildren().clear();
        for (String supply : supplies) {
            Label pill = new Label(supply);
            pill.getStyleClass().add(KioskConstants.CSS_SUPPLY_TAG);
            flowPaneOverlayPills.getChildren().add(pill);
        }
    }

    /**
     * Derives plausible supply tags from a center.
     * Replace with a real inventory join in production.
     */
    private List<String> deriveSupplies(EvacuationSite site) {
        return List.of(
                "💧 Water",
                "🍱 Meals",
                "🛏 Cots",
                "🧰 First Aid",
                "👕 Clothing"
        );
    }


}