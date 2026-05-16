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
        labelCenterName.setText(name);
        fieldAddress.setText(address);
    }

    private void handleSave() {
        try {
            // Uses exact 8-parameter record
            EvacuationCenter updatedCenter = new EvacuationCenter(
                    centerId, labelCenterName.getText(), fieldAddress.getText(), null, null, null, null, null
            );
            centerDao.updateCenterStatus(updatedCenter);
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}