package com.example.dao;

import com.example.model.User;
import com.example.model.UserRole;
import com.example.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link User} entities.
 * Implements all CRUD operations against the refactored {@code users} table.
 *
 * <p>Important: this DAO never compares passwords. It only stores and
 * retrieves {@code password_hash} values. Password comparison and hashing
 * happen in the service layer (AuthService) using bcrypt.</p>
 *
 * <p>All SQL is parameterized via {@link PreparedStatement} to prevent
 * SQL injection.</p>
 */
public class UserDao implements GenericDao<User, Long> {

    // ── SQL constants ───────────────────────────────────────────

    private static final String COLS =
            "id, username, password_hash, display_name, role, " +
                    "latitude, longitude, zoom, last_login_at";

    private static final String SQL_FIND_BY_ID =
            "SELECT " + COLS + " FROM users WHERE id = ?";

    private static final String SQL_FIND_BY_USERNAME =
            "SELECT " + COLS + " FROM users WHERE username = ?";

    private static final String SQL_FIND_ALL =
            "SELECT " + COLS + " FROM users ORDER BY display_name";

    private static final String SQL_INSERT =
            "INSERT INTO users " +
                    "(username, password_hash, display_name, role, latitude, longitude, zoom) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE users SET " +
                    "username = ?, password_hash = ?, display_name = ?, " +
                    "role = ?, latitude = ?, longitude = ?, zoom = ? " +
                    "WHERE id = ?";

    private static final String SQL_UPDATE_LAST_LOGIN =
            "UPDATE users SET last_login_at = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM users WHERE id = ?";

    // ── CRUD operations ─────────────────────────────────────────

    @Override
    public Optional<User> findById(Long id) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Finds a user by their unique username. This is the entry point
     * for the login flow — the AuthService calls this, then checks the
     * returned bcrypt hash against the user-supplied password.
     *
     * @param username the username to look up (case-sensitive)
     * @return Optional containing the user, or empty if no match
     */
    public Optional<User> findByUsername(String username) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_USERNAME)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    @Override
    public User save(User user) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getDisplayName());

            // Handle UserRole correctly (depends on if you use .toDb() or .name())
            stmt.setString(4, user.getRole() != null ? user.getRole().name().toLowerCase() : "barangay");

            setDoubleOrNull(stmt, 5, user.getLatitude());
            setDoubleOrNull(stmt, 6, user.getLongitude());
            setIntOrNull(stmt, 7, user.getZoom());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Insert failed — no rows affected");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            return user;
        }
    }

    @Override
    public void update(User user) throws SQLException {
        if (user.getId() == null) {
            throw new IllegalArgumentException(
                    "Cannot update user with null ID — use save() for new entities");
        }
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getDisplayName());
            stmt.setString(4, user.getRole() != null ? user.getRole().name().toLowerCase() : "barangay");

            setDoubleOrNull(stmt, 5, user.getLatitude());
            setDoubleOrNull(stmt, 6, user.getLongitude());
            setIntOrNull(stmt, 7, user.getZoom());
            stmt.setLong(8, user.getId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Update failed — no row with ID " + user.getId());
            }
        }
    }

    /**
     * Updates only the {@code last_login_at} timestamp for a user.
     * Called by AuthService after a successful login. Kept separate
     * from {@link #update(User)} so login doesn't risk overwriting
     * other fields with stale in-memory values.
     */
    public void updateLastLogin(Long userId, LocalDateTime timestamp) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_LAST_LOGIN)) {

            stmt.setTimestamp(1, Timestamp.valueOf(timestamp));
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        }
    }

    @Override
    public boolean deleteById(Long id) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // ── Helpers ─────────────────────────────────────────────────

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setDisplayName(rs.getString("display_name"));

        // Match the role enum mapping (adjust if you have a custom fromDb() method)
        String roleStr = rs.getString("role");
        if (roleStr != null) {
            user.setRole(UserRole.valueOf(roleStr.toUpperCase()));
        }

        // Handle safe null checking for coordinates and zoom
        double lat = rs.getDouble("latitude");
        if (!rs.wasNull()) user.setLatitude(lat);

        double lng = rs.getDouble("longitude");
        if (!rs.wasNull()) user.setLongitude(lng);

        int zoom = rs.getInt("zoom");
        if (!rs.wasNull()) user.setZoom(zoom);

        // Map timestamp
        Timestamp lastLoginAt = rs.getTimestamp("last_login_at");
        if (lastLoginAt != null) {
            user.setLastLoginAt(lastLoginAt.toLocalDateTime());
        }

        return user;
    }

    private void setDoubleOrNull(PreparedStatement stmt, int index, Double value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, java.sql.Types.DECIMAL);
        } else {
            stmt.setDouble(index, value);
        }
    }

    private void setIntOrNull(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(index, value);
        }
    }
}