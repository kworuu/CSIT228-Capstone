package com.example.auth;

import com.example.dao.BarangayDao;
import com.example.dao.UserDao;
import com.example.model.Barangay;
import com.example.model.User;
import com.example.model.UserRole;

import java.sql.SQLException;

public class AuthService {

    private final UserDao userDao = new UserDao();
    private final BarangayDao barangayDao = new BarangayDao();

    public LoginResult loginAdmin(String username, String password) {
        try {
            User user = userDao.findByUsername(username);
            if (user == null || !user.passwordHash().equals(hashPassword(password))) {
                return LoginResult.failure("Invalid username or password");
            }
            if (user.role() != UserRole.ADMIN) {
                return LoginResult.failure("Access denied: Admin only");
            }
            SessionContext.create(user, null);
            return LoginResult.success();
        } catch (SQLException e) {
            return LoginResult.failure("Database error: " + e.getMessage());
        }
    }

    public LoginResult loginBarangay(String username, String password) {
        try {
            User user = userDao.findByUsername(username);
            if (user == null || !user.passwordHash().equals(hashPassword(password))) {
                return LoginResult.failure("Invalid username or password");
            }
            if (user.role() != UserRole.BARANGAY && user.role() != UserRole.STAFF) {
                return LoginResult.failure("Access denied: Barangay accounts only");
            }

            Barangay brgy = barangayDao.findByName(user.assignedBarangay()).orElse(null);
            if (brgy == null) {
                return LoginResult.failure("Configuration error: Assigned barangay not found");
            }

            SessionContext.create(user, brgy);
            return LoginResult.success();
        } catch (SQLException e) {
            return LoginResult.failure("Database error: " + e.getMessage());
        }
    }

    public void logout() {
        SessionContext.clear();
    }

    private String hashPassword(String plainText) {
        // In a real application, you would use a strong hashing algorithm like BCrypt.
        // For this project, we are assuming no hashing for simplicity.
        return plainText;
    }
}
