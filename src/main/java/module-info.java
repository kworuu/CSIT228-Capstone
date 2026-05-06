module com.example.dashboard_admin {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;


    opens com.example.dashboard_admin to javafx.fxml;
    exports com.example.dashboard_admin;

    // Ensure these match your folder names letter-for-letter
    opens com.example.dashboard_admin.HelperClasses to javafx.fxml;
    exports com.example.dashboard_admin.HelperClasses;

    exports com.example.dashboard_admin.Views;
    opens com.example.dashboard_admin.Views to javafx.fxml;
    exports com.example.dashboard_admin.MapLogic;
    opens com.example.dashboard_admin.MapLogic to javafx.fxml;
}