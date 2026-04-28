module com.example.dashboard_admin {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.dashboard_admin to javafx.fxml;
    exports com.example.dashboard_admin;
}