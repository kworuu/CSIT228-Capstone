module com.example.dashboard {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.gluonhq.maps;

    opens com.example.dashboard to javafx.fxml;
    opens com.example.capstone_kioskview to javafx.fxml;

    exports com.example.dashboard;
    exports com.example.capstone_kioskview;
}