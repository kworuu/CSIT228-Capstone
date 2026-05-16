package com.example.auth;

import com.example.dao.BarangayDao;
import com.example.model.Barangay;
import com.example.util.Route;
import com.example.util.Router;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

import java.sql.SQLException;
import java.util.List;

public class BarangayLoginController {

    @FXML private ComboBox<String> barangayCombo;
    @FXML private PasswordField    passwordField;
    @FXML private Button           loginButton;
    @FXML private Label            errorLabel;

    private final AuthService  authService  = new AuthService();
    private final BarangayDao  barangayDao  = new BarangayDao();

    @FXML
    public void initialize() {
        loadBarangaysAsync();
    }

    @FXML
    private void handleLogin() {
        String barangayName = barangayCombo.getValue();
        String password = passwordField.getText();

        if (barangayName == null || barangayName.isBlank()) {
            showError("Please select your barangay.");
            return;
        }

        clearError();
        setBusy(true);

        Thread worker = new Thread(() -> {
            // No SQLException needs to be caught here because AuthService handles it
            LoginResult result = authService.loginBarangay(barangayName, password);
            Platform.runLater(() -> handleResult(result));
        }, "barangay-login-worker");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleBack() {
        Router.getInstance().navigate(Route.KIOSK);
    }

    private void loadBarangaysAsync() {
        Thread loader = new Thread(() -> {
            try {
                List<Barangay> all = barangayDao.findAll();
                List<String> names = all.stream().map(Barangay::getName).toList();
                Platform.runLater(() ->
                        barangayCombo.setItems(FXCollections.observableArrayList(names)));
            } catch (SQLException e) {
                Platform.runLater(() ->
                        showError("Could not load barangays. Check your database connection."));
                System.err.println("[BarangayLogin] Failed to load barangays: " + e.getMessage());
            }
        }, "barangay-list-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void handleResult(LoginResult result) {
        if (result.isSuccess()) {
            Router.getInstance().navigate(Route.BARANGAY_DASHBOARD);
        } else {
            showError(result.errorMessage());
            passwordField.clear();
            setBusy(false);
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        loginButton.setText(busy ? "Signing in…" : "Sign In");
        barangayCombo.setDisable(busy);
        passwordField.setDisable(busy);
    }
}
