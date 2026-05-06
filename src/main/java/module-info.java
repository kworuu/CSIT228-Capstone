module com.example.dashboard_admin {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    opens com.example.dashboard_admin to javafx.fxml;
    exports com.example.dashboard_admin;

    // Ensure these match your folder names letter-for-letter
    opens com.example.dashboard_admin.helper_classes to javafx.fxml;
    exports com.example.dashboard_admin.helper_classes;

    exports com.example.dashboard_admin.views;
    opens com.example.dashboard_admin.views to javafx.fxml;

    exports com.example.dashboard_admin.map_logic;
    opens com.example.dashboard_admin.map_logic to javafx.fxml;

}