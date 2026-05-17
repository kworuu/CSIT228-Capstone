package com.example.dashboard_kiosk.session;

import javafx.scene.Node;

/**
 * Strategy interface defining how the kiosk dashboard behaves under
 * different access levels.
 *
 * <p>Two concrete strategies are provided out of the box:</p>
 * <ul>
 *   <li>{@link PublicSessionMode} — read-only, hides login &amp; edit affordances.</li>
 *   <li>{@link StaffSessionMode}  — shows admin login buttons and enables
 *       interactive controls.</li>
 * </ul>
 *
 * <p>The strategy is applied once at controller initialization via
 * {@link #applyTo(SessionContext)}. Switching modes at runtime is supported
 * by re-applying a different strategy to the same context.</p>
 */
public interface SessionMode {

    /**
     * Configures the dashboard's interactive nodes for this mode.
     *
     * @param context bag of references to the FXML nodes whose visibility
     *                or interactivity varies by mode
     */
    void applyTo(SessionContext context);

    /** Human-readable identifier — used for diagnostics and logging. */
    String displayName();

    // ── Helper: bulk show/hide ─────────────────────────────────────────────

    /** Show and lay out a node, or hide and skip layout. */
    default void setVisible(Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }
}