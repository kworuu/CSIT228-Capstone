package com.example.auth;

import com.example.model.Barangay;
import com.example.model.User;
import com.example.model.UserRole;

/**
 * Holds the currently-authenticated user (and their barangay, if applicable)
 * for the lifetime of one login session.
 *
 * <p>Cleared on logout via {@link #clear()}; populated on successful login
 * by {@link AuthService}.</p>
 *
 * <p>Single-instance, single-threaded use: this is a JavaFX desktop app
 * with exactly one logged-in user at a time. No need for ThreadLocal.</p>
 */
public final class SessionContext {

    private static SessionContext instance;

    private final User user;
    private final Barangay barangay;  // null for admin sessions

    private SessionContext(User user, Barangay barangay) {
        this.user = user;
        this.barangay = barangay;
    }

    /**
     * Establishes a new session. Replaces any previous session silently.
     *
     * @param user     the authenticated user (must not be null)
     * @param barangay the user's barangay if role is BARANGAY/STAFF;
     *                 null for ADMIN
     */
    public static void set(User user, Barangay barangay) {
        if (user == null) {
            throw new IllegalArgumentException("Cannot set session with null user");
        }
        instance = new SessionContext(user, barangay);
    }

    /**
     * Returns the current session, or {@code null} if no one is logged in.
     */
    public static SessionContext current() {
        return instance;
    }

    /** Clears the current session — call from logout handlers. */
    public static void clear() {
        instance = null;
    }

    public User getUser()         { return user; }
    public Barangay getBarangay() { return barangay; }

    public boolean isAdmin() {
        return user.getRole() == UserRole.ADMIN;
    }

    public boolean isBarangay() {
        return user.getRole().isBarangayRole();
    }
}