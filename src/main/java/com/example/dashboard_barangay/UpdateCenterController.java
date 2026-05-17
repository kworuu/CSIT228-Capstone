package com.example.dashboard_barangay;

import com.example.dao.EvacuationCenterDao;
import com.example.dao.InventoryItemDao;
import com.example.model.InventoryItem;
import com.example.util.DBConnectionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UpdateCenterController {

    @FXML private Label labelTitle;
    @FXML private Label labelOverlayAddress;
    @FXML private TextField textFieldEvent;
    @FXML private Button buttonSave;
    @FXML private Button buttonDelete;
    @FXML private Button buttonCancel;
    @FXML private ImageView imgModalCenter;
    @FXML private GridPane gridPaneSupplies;

    private long centerId;
    private final InventoryItemDao inventoryDao = new InventoryItemDao();
    private final EvacuationCenterDao centerDao = new EvacuationCenterDao();
    private final List<SupplyCheckBoxData> checkBoxes = new ArrayList<>();

    private record SupplyCheckBoxData(CheckBox checkBox, long itemId) {}

    @FXML
    public void initialize() {}

    public void initData(long id, String name, String address, String photoPath, String eventLabel, List<String> currentSupplies) {
        this.centerId = id;
        this.labelTitle.setText("Update " + name);
        this.labelOverlayAddress.setText(address);

        if (!eventLabel.equals("No active event")) {
            this.textFieldEvent.setText(eventLabel);
        }

        if (photoPath != null && !photoPath.isBlank()) {
            try {
                imgModalCenter.setImage(new Image(getClass().getResourceAsStream(photoPath)));
            } catch (Exception e) {
                System.err.println("Modal could not load image: " + photoPath);
            }
        }

        loadInventoryCheckboxes(currentSupplies);
    }

    private void loadInventoryCheckboxes(List<String> currentSupplies) {
        try {
            List<InventoryItem> items = inventoryDao.findAll();
            int row = 0;
            int col = 0;

            for (InventoryItem item : items) {
                CheckBox cb = new CheckBox(item.name());
                cb.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 13px;");

                if (currentSupplies.contains(item.name())) {
                    cb.setSelected(true);
                }

                gridPaneSupplies.add(cb, col, row);
                checkBoxes.add(new SupplyCheckBoxData(cb, item.id()));

                col++;
                if (col > 1) {
                    col = 0;
                    row++;
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load inventory for checkboxes: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to permanently delete this evacuation center?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    centerDao.delete(centerId);
                    closeWindow();
                } catch (SQLException e) {
                    // SQL Constraints will safely block deletion if evacuees are still inside!
                    Alert error = new Alert(Alert.AlertType.ERROR, "Cannot delete this center. Evacuees are currently registered here. Please delete or reassign them first.", ButtonType.OK);
                    error.showAndWait();
                }
            }
        });
    }

    @FXML
    private void handleSave() {
        List<Long> selectedIds = new ArrayList<>();
        for (SupplyCheckBoxData data : checkBoxes) {
            if (data.checkBox().isSelected()) {
                selectedIds.add(data.itemId());
            }
        }
        String itemsJson = selectedIds.toString();

        String sql = """
            INSERT INTO center_status_updates (center_id, event_label, available_item_ids, updated_at) 
            VALUES (?, ?, ?, NOW())
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, centerId);
            ps.setString(2, textFieldEvent.getText().trim());
            ps.setString(3, itemsJson);
            ps.executeUpdate();

            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) buttonCancel.getScene().getWindow();
        stage.close();
    }
}