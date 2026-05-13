package com.example.util;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.function.BiPredicate;

public class SearchTableUtility {
    public static <T> void setupSearch(TextField searchField, TableView<T> tableView,
                                       javafx.collections.ObservableList<T> masterData,
                                       BiPredicate<T, String> filterLogic){

        FilteredList<T> filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(item -> {
                // If search field is empty, display all data
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();
                return filterLogic.test(item, lowerCaseFilter);
            });
        });

        SortedList<T> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());


        tableView.setItems(sortedData);
    }
}
