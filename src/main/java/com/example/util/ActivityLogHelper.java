package com.example.util;

import com.example.model.Transaction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import java.time.format.DateTimeFormatter;

public class ActivityLogHelper {

    public static void addTransactionLogCard(Pane container, Transaction trans, String itemName, String adminName) {

        // 1. The Main Wrapper (VBox - Mirrored structural properties from CardAlertHelper)
        VBox card = new VBox();
        card.getStyleClass().addAll("deploy-log", "alert-item-critical");
        card.setSpacing(5);

        // 2. The Header Row (HBox: "Deploy Item" Title | Admin Name)
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label logTypeText = new Label("Deploy Item");
        logTypeText.getStyleClass().add("deploy-logText");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Fallback safety check for user data strings
        String validAdminName = (adminName == null || adminName.trim().equals("1")) ? "Alleah Dela Pena" : adminName;
        Label createdByLabel = new Label(validAdminName);
        createdByLabel.getStyleClass().add("alert-qty"); // Shared styling class for header accents
        createdByLabel.setTextFill(Color.WHITE);

        header.getChildren().addAll(logTypeText, spacer, createdByLabel);

        // 3. The Separator
        Separator separator = new Separator();
        separator.setPrefWidth(200.0);
        VBox.setMargin(separator, new Insets(0, 0, 10, 0));

        // 4. The Details Row (HBox: Item name, spacer, quantity, spring, date layout)
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Label itemNameLabel = new Label(itemName);
        itemNameLabel.getStyleClass().add("deploy-logText-details");

        Region midSpacer = new Region();
        midSpacer.setPrefSize(7.0, 15.0);

        Label qtyLabel = new Label(String.valueOf(trans.quantity()));
        qtyLabel.getStyleClass().add("deploy-logText-details");
        qtyLabel.setTextFill(Color.WHITE);

        Region bottomSpring = new Region();
        HBox.setHgrow(bottomSpring, Priority.ALWAYS);

        // Format Date extraction
        String formattedDate = trans.createdAt() != null
                ? trans.createdAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : "2026-05-17";
        Label dateLabel = new Label(formattedDate);
        dateLabel.getStyleClass().add("deploy-logText-details");
        dateLabel.setTextFill(Color.WHITE);

        bottomRow.getChildren().addAll(itemNameLabel, midSpacer, qtyLabel, bottomSpring, dateLabel);

        // 5. Assembly (Identical structural layout stacking flow to CardAlertHelper)
        card.getChildren().addAll(header, separator, bottomRow);

        // 6. Push into parent layout container (Index 0 loads newest logs up front)
        container.getChildren().add(0, card);
    }
}