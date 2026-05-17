package com.example.util;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import java.util.function.BiPredicate;

public class SearchTableUtility {

    // Keep your original method exactly as it was so your other classes don't break!
    public static <T> void setupSearch(TextField searchField, TableView<T> tableView,
                                       ObservableList<T> masterData,
                                       BiPredicate<T, String> filterLogic) {
        setupSearch(searchField, tableView, masterData, filterLogic, null);
    }

    /**
     * UPGRADED OVERLOAD: Accepts an extra observable trigger (like a status property)
     * to force re-filtering even when the search text field is completely empty.
     */
    public static <T> void setupSearch(TextField searchField, TableView<T> tableView,
                                       ObservableList<T> masterData,
                                       BiPredicate<T, String> filterLogic,
                                       ObservableValue<?> extraTrigger) {

        FilteredList<T> filteredData = new FilteredList<>(masterData, p -> true);

        // Moving the unified predicate logic out so both listeners can share it
        Runnable runFilter = () -> {
            String textValue = searchField.getText();
            final String lowerCaseFilter = (textValue == null) ? "" : textValue.toLowerCase().trim();

            filteredData.setPredicate(item -> {
                // Pass the current text query directly down to your controller logic
                return filterLogic.test(item, lowerCaseFilter);
            });
        };

        // Trigger 1: Run filter logic when text changes
        searchField.textProperty().addListener((obs, oldVal, newVal) -> runFilter.run());

        // Trigger 2: Run filter logic when your tab button selection changes
        if (extraTrigger != null) {
            extraTrigger.addListener((obs, oldVal, newVal) -> runFilter.run());
        }

        SortedList<T> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());

        tableView.setItems(sortedData);
    }
}