package com.example.dashboard_kiosk.controller;

import com.example.dashboard_kiosk.model.EvacuationSite;
import com.example.model.EvacueeRecord;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import java.util.Optional;

/**
 * Manages closeable evacuee-roster tabs inside the kiosk's bottom {@link TabPane}.
 */
public final class RosterTabHelper {

    private final TabPane tabPane;
    private final ObservableList<EvacueeRecord> allEvacuees;

    public RosterTabHelper(TabPane tabPane, ObservableList<EvacueeRecord> allEvacuees) {
        this.tabPane = tabPane;
        this.allEvacuees = allEvacuees;
    }

    public void initialize(Tab tabCentersOverview, Tab tabEvacuees) {
        if (tabCentersOverview != null) tabCentersOverview.setClosable(false);
        if (tabEvacuees        != null) tabEvacuees.setClosable(false);
    }

    public void openRosterTab(EvacuationSite site) {
        if (site == null) return;

        Optional<Tab> existing = tabPane.getTabs().stream()
                .filter(t -> site.id().equals(t.getUserData()))
                .findFirst();

        if (existing.isPresent()) {
            tabPane.getSelectionModel().select(existing.get());
            return;
        }

        Tab tab = new Tab();
        tab.setText("📋 " + site.title());
        tab.setUserData(site.id());
        tab.setClosable(true);
        tab.setContent(buildRosterContent(site));

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private VBox buildRosterContent(EvacuationSite site) {
        Label title = new Label("Evacuee Roster");
        title.getStyleClass().add("card-title");

        Label subtitle = new Label(site.title());
        subtitle.getStyleClass().add("detail-meta");

        VBox titleBox = new VBox(2.0, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search evacuee...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(220.0);

        HBox header = new HBox(12.0, titleBox, spacer, searchField);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.getStyleClass().add("card-header");
        header.setPadding(new Insets(10.0, 18.0, 10.0, 18.0));

        TableColumn<EvacueeRecord, String> colName = new TableColumn<>("FULL NAME");
        colName.setPrefWidth(280.0);
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));

        TableColumn<EvacueeRecord, String> colStatus = new TableColumn<>("STATUS");
        colStatus.setPrefWidth(160.0);
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        TableColumn<EvacueeRecord, String> colDate = new TableColumn<>("DATE REGISTERED");
        colDate.setPrefWidth(220.0);
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRegisteredAt()));

        TableView<EvacueeRecord> table = new TableView<>();
        table.getColumns().addAll(colName, colStatus, colDate);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No evacuees registered for this center."));
        
        FilteredList<EvacueeRecord> filteredData = new FilteredList<>(allEvacuees, e -> e.getAssignedCenter().equals(site.title()));
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(e -> {
                if (!e.getAssignedCenter().equals(site.title())) return false;
                if (newVal == null || newVal.isBlank()) return true;
                
                String q = newVal.toLowerCase();
                return (e.getFullName() != null && e.getFullName().toLowerCase().contains(q)) ||
                       (e.getStatus() != null && e.getStatus().toLowerCase().contains(q));
            });
        });
        
        table.setItems(filteredData);
        
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        VBox content = new VBox(header, table);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        return content;
    }
}
