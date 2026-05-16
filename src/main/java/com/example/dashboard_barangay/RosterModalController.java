package com.example.dashboard_barangay;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

public class RosterModalController {

    @FXML private StackPane modalRoot;
    @FXML private Label labelCenterName;
    @FXML private Label labelTotalCount;
    @FXML private TextField fieldSearch;

    @FXML private TableView<RosterItem> tableRoster;
    @FXML private TableColumn<RosterItem, String> colName;
    @FXML private TableColumn<RosterItem, String> colStatus;
    @FXML private TableColumn<RosterItem, String> colDate;

    // Lists for search filtering
    private final ObservableList<RosterItem> masterList = FXCollections.observableArrayList();
    private FilteredList<RosterItem> filteredList;
    private Runnable onCloseCallback;

    // Simple record to hold the table data
    public record RosterItem(String name, String status, String date) {}

    @FXML
    public void initialize() {
        // 1. Setup Table Columns
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().name()));
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().status()));
        colDate.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().date()));

        // 2. Setup Live Search Filter
        filteredList = new FilteredList<>(masterList, b -> true);

        fieldSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(item -> {
                if (newValue == null || newValue.isBlank()) return true; // Show all if search is empty
                return item.name().toLowerCase().contains(newValue.toLowerCase()); // Filter by name
            });
            updateCountLabel();
        });

        tableRoster.setItems(filteredList);
    }

    /**
     * Called by BrgyDashboardController right after opening the modal
     */
    public void initData(long centerId, String centerName) {
        labelCenterName.setText(centerName);

        // Load REAL data from the database
        try {
            com.example.dao.EvacueeDao dao = new com.example.dao.EvacueeDao();
            masterList.setAll(dao.getRosterByCenter(centerId));
        } catch (java.sql.SQLException e) {
            System.err.println("Failed to load center roster: " + e.getMessage());
        }

        updateCountLabel();
    }

    private void updateCountLabel() {
        labelTotalCount.setText(filteredList.size() + " Registered");
    }

    public void setOnClose(Runnable onCloseCallback) {
        this.onCloseCallback = onCloseCallback;
    }

    @FXML
    private void closeModal() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }
}