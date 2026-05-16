package com.example.auth;

import com.example.dao.UserDao;
import com.example.model.User;
import com.example.model.UserRole;

import java.sql.SQLException;

public class AuthService {

    private final UserDao userDao = new UserDao();

    public LoginResult loginAdmin(String username, String password) {
        try {
            User user = userDao.findByUsername(username);
            if (user == null || !user.passwordHash().equals(password)) {
                return LoginResult.failure("Invalid username or password");
            }
            if (user.role() != UserRole.ADMIN) {
                return LoginResult.failure("Access denied: Admin only");
            }
            SessionContext.create(user);
            return LoginResult.success();
        } catch (SQLException e) {
            return LoginResult.failure("Database error: " + e.getMessage());
        }
    }

    public LoginResult loginBarangay(String display_name, String password) {
        try {
            User user = userDao.findByUsername(display_name);
            if (user == null || !user.passwordHash().equals(hashPassword(password))) {
                return LoginResult.failure("Invalid username or password");
            }
            if (user.role() != UserRole.BARANGAY && user.role() != UserRole.STAFF) {
                return LoginResult.failure("Access denied: Barangay accounts only");
            }

            // SUCCESS! Create session with just the user.
            // We now rely on user.displayName() (e.g., "Brgy. Lahug") instead of a separate Barangay table.
            SessionContext.create(user);
            return LoginResult.success();
        } catch (SQLException e) {
            return LoginResult.failure("Database error: " + e.getMessage());
        }
    }

    public void logout() {
        SessionContext.clear();
    }

    private String hashPassword(String plainText) {
        return plainText; // Adjust if you are actually checking hashes
    }
}
