package com.example.util;

import com.example.model.InventoryItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class CardAlertHelper {

    public static void addCriticalAlertCard(Pane container, InventoryItem item) {
        // 1. The Main Wrapper (VBox)
        VBox card = new VBox();
        // Matching your FXML: styleClass="alert-item, alert-item-critical"
        card.getStyleClass().addAll("alert-item", "alert-item-critical");
        card.setSpacing(5); // Adjust spacing as needed

        // 2. The Header Row (HBox)
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(item.name());
        nameLabel.getStyleClass().add("alert-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label qtyLabel = new Label(item.stockQuantity() + " left");
        qtyLabel.getStyleClass().add("alert-qty");

        header.getChildren().addAll(nameLabel, spacer, qtyLabel);

        // 3. The Separator
        Separator separator = new Separator();
        separator.setPrefWidth(200.0);
        // Matching your FXML: padding bottom 10
        VBox.setMargin(separator, new Insets(0, 0, 10, 0));

        // 4. The Action Button
        Button restockBtn = new Button("Restock necessary");
        restockBtn.setMnemonicParsing(false);
        restockBtn.setPrefHeight(22.0);
        restockBtn.setPrefWidth(252.0); // Match your FXML width
        restockBtn.getStyleClass().add("btn-link-sm");

        restockBtn.setOnAction(event -> {
            // Using your existing SceneHelper to switch to the inventory view
            SceneHelper.switchScene("/com/example/dashboard_admin/inventory.fxml", restockBtn);
        });

        // 5. Assembly
        card.getChildren().addAll(header, separator, restockBtn);

        // Add to the main container
        container.getChildren().add(card);
    }
}