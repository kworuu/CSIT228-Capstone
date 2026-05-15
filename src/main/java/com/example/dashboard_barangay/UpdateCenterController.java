package com.example.dashboard_barangay;

import com.example.util.DBConnectionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import com.example.dao.EvacuationCenterDao;
import com.example.model.StructuralStatus;
import com.example.auth.SessionContext;

public class UpdateCenterController {

    @FXML private Label labelTitle;
    @FXML private Label labelOverlayAddress;
    @FXML private ImageView imgModalCenter;
    @FXML private TextField textFieldEvent;
    @FXML private GridPane gridPaneSupplies;
    @FXML private Button buttonCancel;
    // Phase 5b — structural status UI
    @FXML private ComboBox<StructuralStatus> comboStructuralStatus;
    @FXML private Label                      labelStructuralCurrent;
    @FXML private TextField                  textFieldStructuralNotes;

    private final EvacuationCenterDao centerDao = new EvacuationCenterDao();

    // Cached so handleSave can decide whether to call updateStructuralStatus
    private StructuralStatus originalStatus;
    private String           originalNotes;

    private long currentCenterId;
    private final List<CheckBox> listCheckBoxes = new ArrayList<>();

    /**
     * Initializes the modal with specific center data.
     */
    public void initData(long centerId, String name, String address, String photoPath,
                         StructuralStatus currentStatus, String currentNotes,
                         String currentUpdatedDisplay) {
        this.currentCenterId = centerId;
        this.originalStatus  = currentStatus != null ? currentStatus : StructuralStatus.SAFE;
        this.originalNotes   = currentNotes != null ? currentNotes : "";

        labelTitle.setText(name);
        labelOverlayAddress.setText(address);

        // Existing image-loading logic stays unchanged...
        if (photoPath != null && !photoPath.isBlank()) {
            try {
                Image img = new Image(getClass().getResourceAsStream(photoPath));
                imgModalCenter.setImage(img);
                imgModalCenter.setVisible(true);
                imgModalCenter.setManaged(true);
            } catch (Exception e) {
                System.err.println("Could not load image: " + photoPath);
                hideImage();
            }
        } else {
            hideImage();
        }

        setupStructuralUI(currentUpdatedDisplay);
        loadInventoryItems();
    }

    private void setupStructuralUI(String currentUpdatedDisplay) {
        comboStructuralStatus.setItems(FXCollections.observableArrayList(
                StructuralStatus.values()));
        comboStructuralStatus.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(StructuralStatus s) {
                return s == null ? "" : s.displayLabel();
            }
            @Override public StructuralStatus fromString(String s) {
                return StructuralStatus.valueOf(s.toUpperCase());
            }
        });
        comboStructuralStatus.setValue(originalStatus);

        textFieldStructuralNotes.setText(originalNotes);

        labelStructuralCurrent.setText(
                currentUpdatedDisplay == null || currentUpdatedDisplay.isBlank()
                        ? "Never inspected"
                        : currentUpdatedDisplay);
    }

    private void hideImage() {
        imgModalCenter.setVisible(false);
        imgModalCenter.setManaged(false);
    }

    private void loadInventoryItems() {
        gridPaneSupplies.getChildren().clear();
        listCheckBoxes.clear();

        String sql = "SELECT id, name FROM inventory_items ORDER BY name ASC";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int row = 0;
            int col = 0;
            while (rs.next()) {
                CheckBox cb = new CheckBox(rs.getString("name"));
                cb.setUserData(rs.getLong("id"));
                cb.setStyle("-fx-text-fill: #0f172a;"); // Use dark text instead of white to be visible

                listCheckBoxes.add(cb);
                gridPaneSupplies.add(cb, col, row);
                
                col++;
                if (col > 1) { // 2 columns
                    col = 0;
                    row++;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML private void handleSave() {
        // ── 1. Save the supplies / event update (existing logic) ──
        List<Long> selectedIds = new ArrayList<>();
        for (CheckBox cb : listCheckBoxes) {
            if (cb.isSelected()) selectedIds.add((Long) cb.getUserData());
        }

        String suppliesSql = "INSERT INTO center_status_updates " +
                "(center_id, event_label, available_item_ids) VALUES (?, ?, ?)";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(suppliesSql)) {
            ps.setLong(1, currentCenterId);
            ps.setString(2, textFieldEvent.getText().isEmpty()
                    ? "No active event" : textFieldEvent.getText());
            ps.setString(3, selectedIds.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return;  // bail before touching structural
        }

        // ── 2. Save structural status — only if it actually changed ──
        StructuralStatus newStatus = comboStructuralStatus.getValue();
        String           newNotes  = textFieldStructuralNotes.getText();

        boolean statusChanged = newStatus != originalStatus;
        boolean notesChanged  = !java.util.Objects.equals(
                originalNotes == null ? "" : originalNotes,
                newNotes == null ? "" : newNotes);

        if (statusChanged || notesChanged) {
            var session = SessionContext.current();
            if (session == null || session.getUser() == null) {
                System.err.println("[UpdateCenter] No session — skipping structural save");
            } else {
                try {
                    centerDao.updateStructuralStatus(
                            currentCenterId,
                            newStatus,
                            newNotes,
                            session.getUser().getId());
                    System.out.println("[UpdateCenter] Structural status saved: "
                            + newStatus + " by user " + session.getUser().getUsername());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        closeModal();
    }

    @FXML private void handleCancel() {
        closeModal();
    }

    private void closeModal() {
        Stage stage = (Stage) buttonCancel.getScene().getWindow();
        stage.close();
    }
}