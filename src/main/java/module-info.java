module com.example.dashboard_admin {
    // JavaFX modules (existing)
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    // NEW — required for database integration
    requires java.sql;                  // JDBC core (Connection, ResultSet, etc.)
    requires org.mariadb.jdbc;          // MariaDB JDBC driver
    requires jbcrypt;                   // password hashing

    // Existing exports/opens for FXML controllers
    opens com.example.dashboard_admin to javafx.fxml;
    exports com.example.dashboard_admin;

    opens com.example.dashboard_admin.helper_classes to javafx.fxml;
    exports com.example.dashboard_admin.helper_classes;

    exports com.example.dashboard_admin.views;
    opens com.example.dashboard_admin.views to javafx.fxml;

    exports com.example.dashboard_admin.map_logic;
    opens com.example.dashboard_admin.map_logic to javafx.fxml;

    // NEW — packages I'll be adding for database integration later
            exports com.example.dashboard_admin.model;
            exports com.example.dashboard_admin.dao;
            exports com.example.dashboard_admin.service;
            exports com.example.dashboard_admin.util;
}