module com.example.capstone {
    // JavaFX modules (existing)
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    // NEW — required for database integration
    requires java.sql;                  // JDBC core (Connection, ResultSet, etc.)
    requires org.mariadb.jdbc;          // MariaDB JDBC driver
    requires jbcrypt;
    requires jdk.jsobject;
    requires jdk.httpserver;
    requires java.net.http;
    requires java.desktop;
    requires jakarta.persistence;

    // Main package
    opens com.example to javafx.graphics, javafx.fxml;
    exports com.example;

    // Dashboards
    opens com.example.dashboard_admin to javafx.fxml;
    exports com.example.dashboard_admin;


    opens com.example.dashboard_barangay to javafx.fxml;
    exports com.example.dashboard_barangay;

    // Kiosk Dashboard
    opens com.example.dashboard_kiosk to javafx.fxml, javafx.graphics;
    exports com.example.dashboard_kiosk;

    // Authentication package
    opens com.example.auth to javafx.fxml; // <--- ADDED THIS LINE

    // Shared UI logic
    exports com.example.map_logic;
    opens com.example.map_logic to javafx.fxml;
    
    exports com.example.map_logic_v2;
    opens com.example.map_logic_v2 to javafx.fxml, jdk.jsobject;

    // Shared Database & Business Logic
    exports com.example.model;
    opens com.example.model to javafx.base;

    exports com.example.dao;
    exports com.example.service;
    exports com.example.util;
    opens com.example.util to javafx.fxml;

    exports com.example.dashboard_admin.views;
    opens com.example.dashboard_admin.views to javafx.fxml;
    exports com.example.dashboard_kiosk.session;
    opens com.example.dashboard_kiosk.session to javafx.fxml, javafx.graphics;
    exports com.example.dashboard_kiosk.observer;
    opens com.example.dashboard_kiosk.observer to javafx.fxml, javafx.graphics;
    exports com.example.dashboard_kiosk.controller;
    opens com.example.dashboard_kiosk.controller to javafx.fxml, javafx.graphics, jdk.jsobject;
    exports com.example.dashboard_kiosk.service;
    opens com.example.dashboard_kiosk.service to javafx.fxml, javafx.graphics;
}