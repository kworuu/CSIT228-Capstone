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

public class UpdateCenterController {

    @FXML private Label labelTitle;
    @FXML private Label labelOverlayAddress;
    @FXML private ImageView imgModalCenter;
    @FXML private TextField textFieldEvent;
    @FXML private GridPane gridPaneSupplies;
    @FXML private Button buttonCancel;

    private long currentCenterId;
    private final List<CheckBox> listCheckBoxes = new ArrayList<>();

    /**
     * Initializes the modal with specific center data.
     */
    public void initData(long centerId, String name, String address, String photoPath) {
        this.currentCenterId = centerId;
        labelTitle.setText(name);
        labelOverlayAddress.setText(address);

        // Handle the dynamic image loading
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

        loadInventoryItems();
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
        List<Long> selectedIds = new ArrayList<>();
        for (CheckBox cb : listCheckBoxes) {
            if (cb.isSelected()) selectedIds.add((Long) cb.getUserData());
        }

        String sql = "INSERT INTO center_status_updates (center_id, event_label, available_item_ids) VALUES (?, ?, ?)";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, currentCenterId);
            ps.setString(2, textFieldEvent.getText().isEmpty() ? "No active event" : textFieldEvent.getText());
            ps.setString(3, selectedIds.toString());
            // existing code ...
            ps.executeUpdate();

            // NEW: Notify all Kiosk screens (Observers) that a center update happened
            String eventLabel = textFieldEvent.getText().isEmpty() ? "No active event" : textFieldEvent.getText();
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"));

            com.example.util.CenterEvent event = new com.example.util.CenterEvent(currentCenterId, labelTitle.getText(), eventLabel, timestamp);
            com.example.util.CenterEventManager.getInstance().notifyObservers(event);

            closeModal();
// existing code ...
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML private void handleCancel() {
        closeModal();
    }

    private void closeModal() {
        Stage stage = (Stage) buttonCancel.getScene().getWindow();
        stage.close();
    }
}