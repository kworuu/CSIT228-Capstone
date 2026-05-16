package com.example.auth;

import com.example.model.Barangay;
import com.example.model.User;
import com.example.model.UserRole;

public class SessionContext {
    private static SessionContext instance;
    private final User currentUser;
    private final Barangay currentBarangay;

    private SessionContext(User user, Barangay barangay) {
        this.currentUser = user;
        this.currentBarangay = barangay;
    }

    public static void create(User user, Barangay barangay) {
        instance = new SessionContext(user, barangay);
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

    public Barangay getBarangay() {
        return currentBarangay;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.role() == UserRole.ADMIN;
    }

    public boolean isBarangay() {
        // Staff and Barangay roles both get Barangay-level access
        return currentUser != null && (currentUser.role() == UserRole.BARANGAY || currentUser.role() == UserRole.STAFF);
    }
}