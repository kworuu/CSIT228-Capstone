package com.example.auth;

import com.example.util.Route;
import com.example.util.Router;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AdminLoginController {

    private final AuthService authService = new AuthService();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        LoginResult result = authService.loginAdmin(username, password);

        if (result.isSuccess()) {
            Router.getInstance().navigate(Route.ADMIN_DASHBOARD);
        } else {
            errorLabel.setText(result.errorMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    @FXML
    private void handleBack() {
        Router.getInstance().navigate(Route.KIOSK);
    }
}
