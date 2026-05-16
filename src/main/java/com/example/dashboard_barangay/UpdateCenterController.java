package com.example.dashboard_barangay;

import com.example.dao.EvacuationCenterDao;
import com.example.model.EvacuationCenter;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

public class UpdateCenterController {

    @FXML private Label labelCenterName;
    @FXML private TextField fieldAddress;
    @FXML private TextField fieldCapacity;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private long centerId;
    private final EvacuationCenterDao centerDao = new EvacuationCenterDao();

    @FXML
    public void initialize() {
        btnCancel.setOnAction(e -> closeWindow());
        btnSave.setOnAction(e -> handleSave());
    }

    public void initData(long id, String name, String address) {
        this.centerId = id;
        labelCenterName.setText("Update " + name);
        fieldAddress.setText(address);
    }

    private void handleSave() {
        try {
            // Update basic details
            EvacuationCenter updatedCenter = new EvacuationCenter(
                    centerId, labelCenterName.getText(), fieldAddress.getText(), "",
                    Integer.parseInt(fieldCapacity.getText()), 0, 0.0, 0.0, null, null, null
            );

            centerDao.updateCenterStatus(updatedCenter);
            closeWindow();
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}