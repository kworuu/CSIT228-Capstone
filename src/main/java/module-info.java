module com.example.capstone {
    // JavaFX modules (existing)
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    // NEW — required for database integration
    requires java.sql;                  // JDBC core (Connection, ResultSet, etc.)
    requires org.mariadb.jdbc;          // MariaDB JDBC driver
    requires jbcrypt;                   // password hashing

    // Dashboards
    opens com.example.dashboard_admin to javafx.fxml;
    exports com.example.dashboard_admin;

    opens com.example.dashboard_admin.helper_classes to javafx.fxml;
    exports com.example.dashboard_admin.helper_classes;

    exports com.example.dashboard_admin.views;
    opens com.example.dashboard_admin.views to javafx.fxml;

    opens com.example.dashboard_barangay to javafx.fxml;
    exports com.example.dashboard_barangay;

    // Shared UI logic
    exports com.example.map_logic;
    opens com.example.map_logic to javafx.fxml;

    // Shared Database & Business Logic
    exports com.example.model;
    exports com.example.dao;
    exports com.example.service;
    exports com.example.util;
}