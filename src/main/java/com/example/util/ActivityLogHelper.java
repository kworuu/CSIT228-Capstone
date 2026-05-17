package com.example.util;

import com.example.model.InventoryItem;
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

public class ActivityLogHelper {

    public static void addCriticalAlertCard(Pane container, InventoryItem item) {
        // 1. The Main Wrapper (VBox)
        VBox card = new VBox();
        card.getStyleClass().addAll("deploy-log", "alert-item-critical");

        // 2. The First Row (Header)
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label logTypeText = new Label("Deploy Item");
        logTypeText.getStyleClass().add("deploy-logText");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        // Assuming your InventoryItem has a method for the user or creator
        Label createdByLabel = new Label("Alleah Dela Pena"); // Replace with item.getUser() if applicable
        createdByLabel.getStyleClass().add("alert-qty");
        createdByLabel.setTextFill(Color.WHITE);

        topRow.getChildren().addAll(logTypeText, topSpacer, createdByLabel);

        // 3. The Separator
        Separator separator = new Separator();
        separator.setPrefWidth(200.0);
        VBox.setMargin(separator, new Insets(0, 0, 10, 0));

        // 4. The Second Row (Details)
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Label itemNameLabel = new Label(item.name());
        itemNameLabel.getStyleClass().add("deploy-logText");

        // Small fixed-width spacer
        Region midSpacer = new Region();
        midSpacer.setPrefSize(7.0, 15.0);

        Label qtyLabel = new Label(String.valueOf(item.stockQuantity()));
        qtyLabel.getStyleClass().add("alert-qty");
        qtyLabel.setTextFill(Color.WHITE);

        Region bottomSpring = new Region();
        HBox.setHgrow(bottomSpring, Priority.ALWAYS);

        // Date Label - using a placeholder or item date if available
        Label dateLabel = new Label("2026-05-15");
        dateLabel.getStyleClass().add("alert-qty");
        dateLabel.setTextFill(Color.WHITE);

        bottomRow.getChildren().addAll(itemNameLabel, midSpacer, qtyLabel, bottomSpring, dateLabel);

        // 5. Assembly
        card.getChildren().addAll(topRow, separator, bottomRow);

        // Add to the main container
        container.getChildren().add(card);
    }
}