package com.example.dashboard_kiosk;

//import com.example.model.CenterStatus;
//import com.example.model.EvacuationCenter;
//import com.example.model.Supply;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class DetailModalController {

    @FXML private VBox modalRoot;
    @FXML private Label lblTitle, lblMeta, lblStatus, lblAddress;
    @FXML private FlowPane suppliesFlow;
    @FXML private Button btnShowRoute, btnViewDetails;

    private Runnable onViewDetails;
    private Runnable onShowRoute;

    public VBox getRoot() { return modalRoot; }

    public void setOnViewDetails(Runnable cb) { onViewDetails = cb; }
    public void setOnShowRoute(Runnable cb)   { onShowRoute   = cb; }

//    public void show(EvacuationCenter center) {
//        lblTitle.setText(center.getTitle());
//        lblMeta.setText(center.getId() + " · " + center.getAddress());
//        lblAddress.setText(center.getAddress());
//
//        lblStatus.setText(center.getStatus().getLabel());
//        lblStatus.getStyleClass().removeAll("status-tag-open", "status-tag-full");
//        lblStatus.getStyleClass().add(
//                center.getStatus() == CenterStatus.OPEN ? "status-tag-open" : "status-tag-full");
//
//        suppliesFlow.getChildren().clear();
//        for (Supply s : center.getSupplies()) {
//            Label tag = new Label(s.name());
//            tag.getStyleClass().add("supply-tag");
//            suppliesFlow.getChildren().add(tag);
//        }
//
//        modalRoot.setVisible(true);
//        modalRoot.setManaged(true);
//    }

    public void hide() {
        modalRoot.setVisible(false);
        modalRoot.setManaged(false);
    }

    @FXML private void handleClose()       { hide(); }
    @FXML private void handleShowRoute()   { if (onShowRoute   != null) onShowRoute.run(); }
    @FXML private void handleViewDetails() { if (onViewDetails != null) onViewDetails.run(); }
}