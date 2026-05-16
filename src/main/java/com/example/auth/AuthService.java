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
                return new LoginResult(false, "Invalid username or password", null);
            }
            if (user.role() != UserRole.ADMIN) {
                return new LoginResult(false, "Access denied: Admin only", null);
            }
            SessionContext.create(user, null);
            return new LoginResult(true, "Success", user);
        } catch (SQLException e) {
            return new LoginResult(false, "Database error: " + e.getMessage(), null);
        }
    }

    public LoginResult loginBarangay(String username, String password) {
        try {
            User user = userDao.findByUsername(username);
            if (user == null || !user.passwordHash().equals(hashPassword(password))) {
                return new LoginResult(false, "Invalid username or password", null);
            }
            if (user.role() != UserRole.BARANGAY && user.role() != UserRole.STAFF) {
                return new LoginResult(false, "Access denied: Barangay accounts only", null);
            }

            Barangay brgy = barangayDao.findByName(user.assignedBarangay());
            if (brgy == null) {
                return new LoginResult(false, "Configuration error: Assigned barangay not found", null);
            }

            SessionContext.create(user, brgy);
            return new LoginResult(true, "Success", user);
        } catch (SQLException e) {
            return new LoginResult(false, "Database error: " + e.getMessage(), null);
        }
    }

    public void logout() {
        SessionContext.clear();
    }

    private String hashPassword(String plainText) {
        return plainText; // Placeholder for actual hash check if implemented
    }
}