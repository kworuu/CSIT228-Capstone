package com.example.dashboard_kiosk;

import com.example.dashboard_kiosk.user.SimpleCenter;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the floating detail-modal overlay that appears when a map
 * marker or table "View" button is activated.
 *
 * <p>The modal is loaded once at startup by {@link KioskDashboardController}
 * and kept {@code visible=false} / {@code managed=false} until
 * {@link #show(SimpleCenter)} is called.</p>
 *
 * <p>Supply icons are rendered as emoji-prefixed label tags inside the
 * {@code suppliesFlow} FlowPane, giving a clean pill appearance without
 * requiring external icon fonts.</p>
 */
public class DetailModalController {

    // ── FXML bindings ──────────────────────────────────────────────────────

    @FXML private VBox   modalRoot;
    @FXML private Label  lblTitle;
    @FXML private Label  lblMeta;
    @FXML private Label  lblStatus;
    @FXML private Label  lblAddress;
    @FXML private Label  lblOccupancy;
    @FXML private FlowPane suppliesFlow;
    @FXML private Button btnShowRoute;
    @FXML private Button btnViewDetails;

    // ── Callbacks wired by the parent controller ───────────────────────────

    private Runnable onViewDetails;
    private Runnable onShowRoute;

    // ── Public API ─────────────────────────────────────────────────────────

    /** Returns the root node so the parent can add it to an AnchorPane. */
    public VBox getRoot() { return modalRoot; }

    public void setOnViewDetails(Runnable cb) { onViewDetails = cb; }
    public void setOnShowRoute(Runnable cb)   { onShowRoute   = cb; }

    /**
     * Populates the modal with data from {@code center} and makes it visible.
     *
     * <p>Status handling:
     * <ul>
     *   <li>{@code OPEN} → muted forest-green pill ({@code status-tag-open})</li>
     *   <li>{@code FULL} → terracotta-red pill ({@code status-tag-full})</li>
     * </ul>
     * </p>
     *
     * @param center the selected evacuation center; must not be {@code null}
     */
    public void show(SimpleCenter center) {
        if (center == null) return;

        // ── Header ──────────────────────────────────────────────────────
        lblTitle.setText(center.getTitle());
        lblMeta.setText(center.getId() + " · " + abbreviateBarangay(center.getAddress()));

        // ── Status pill ─────────────────────────────────────────────────
        boolean isFull = center.isFull();
        lblStatus.setText(isFull ? "Full" : "Open");
        lblStatus.getStyleClass().removeAll("status-tag-open", "status-tag-full");
        lblStatus.getStyleClass().add(isFull ? "status-tag-full" : "status-tag-open");

        // ── Address & occupancy ─────────────────────────────────────────
        if (lblAddress != null)   lblAddress.setText(center.getAddress());
        if (lblOccupancy != null) {
            int pct = center.getCapacity() == 0
                    ? 0
                    : (int) Math.round(100.0 * center.getOccupancy() / center.getCapacity());
            lblOccupancy.setText(
                    center.getOccupancy() + " / " + center.getCapacity()
                            + " evacuees  (" + pct + "%)");
        }

        // ── Supply tags ─────────────────────────────────────────────────
        // In production, pull from DAO; here we derive from status/occupancy
        // as a reasonable placeholder.
        suppliesFlow.getChildren().clear();
        for (String supply : deriveSupplies(center)) {
            Label tag = new Label(supply);
            tag.getStyleClass().add("supply-tag");
            suppliesFlow.getChildren().add(tag);
        }

        // ── Show ────────────────────────────────────────────────────────
        modalRoot.setVisible(true);
        modalRoot.setManaged(true);
    }

    /** Hides the modal without destroying it. */
    public void hide() {
        modalRoot.setVisible(false);
        modalRoot.setManaged(false);
    }

    // ── FXML event handlers ────────────────────────────────────────────────

    @FXML private void handleClose()       { hide(); }
    @FXML private void handleShowRoute()   { if (onShowRoute   != null) onShowRoute.run(); }
    @FXML private void handleViewDetails() { if (onViewDetails != null) onViewDetails.run(); }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Derives plausible supply tags from the center's occupancy level.
     * Replace with a real inventory lookup in production.
     */
    private java.util.List<String> deriveSupplies(SimpleCenter center) {
        java.util.List<String> tags = new java.util.ArrayList<>();
        int occ = center.getOccupancy();

        tags.add("💧 Water");
        tags.add("🍱 Meals");

        if (occ > 50)  tags.add("🛏 Cots");
        if (occ > 80)  tags.add("🧰 First Aid");
        if (occ > 100) tags.add("👕 Clothing");
        if (!center.isFull()) tags.add("✅ Space Available");

        return tags;
    }

    /**
     * Extracts just the barangay portion from a full address string,
     * e.g. "Brgy. Poblacion, Argao, Cebu" → "Brgy. Poblacion".
     */
    private String abbreviateBarangay(String fullAddress) {
        if (fullAddress == null) return "";
        int comma = fullAddress.indexOf(',');
        return comma > 0 ? fullAddress.substring(0, comma).trim() : fullAddress;
    }
}