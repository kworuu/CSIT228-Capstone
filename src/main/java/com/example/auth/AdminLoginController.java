package com.example.auth;

import com.example.util.Route;
import com.example.util.Router;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;

/**
 * Controller for {@code AdminLogin.fxml}.
 *
 * <p>Collects username + password, delegates to {@link AuthService},
 * and routes to the admin dashboard on success or shows an inline
 * error banner on failure.</p>
 *
 * <p>Authentication runs on a background thread so the UI doesn't
 * freeze if the database is slow (bcrypt verification alone takes
 * 50–100 ms, plus the JDBC round-trip).</p>
 */
public class AdminLoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;
    @FXML private Label         errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        clearError();
        setBusy(true);

        // Run JDBC + bcrypt off the FX thread — they can each take
        // tens of milliseconds and the UI must stay responsive.
        Thread worker = new Thread(() -> {
            try {
                LoginResult result = authService.loginAsAdmin(username, password);
                Platform.runLater(() -> handleResult(result));
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    showError("Could not reach database. Please try again.");
                    setBusy(false);
                    System.err.println("[AdminLogin] DB error: " + e.getMessage());
                });
            }
        }, "admin-login-worker");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleBack() {
        Router.getInstance().navigate(Route.KIOSK);
    }

    // ── Helpers ─────────────────────────────────────────────────

    private void handleResult(LoginResult result) {
        if (result.success().isSuccess()) {
            Router.getInstance().navigate(Route.ADMIN_DASHBOARD);
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
        usernameField.setDisable(busy);
        passwordField.setDisable(busy);
    }
}