package com.example.auth;

import com.example.model.User;
import com.example.model.UserRole;

public class SessionContext {
    private static SessionContext instance;
    private final User currentUser;

    private SessionContext(User user) {
        this.currentUser = user;
    }

    public static void create(User user) {
        instance = new SessionContext(user);
    }

    public static void clear() {
        instance = null;
    }

    public static SessionContext current() {
        return instance;
    }

    public User getUser() {
        return currentUser;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.role() == UserRole.ADMIN;
    }

    public boolean isBarangay() {
        return currentUser != null && (currentUser.role() == UserRole.BARANGAY || currentUser.role() == UserRole.STAFF);
    }
}