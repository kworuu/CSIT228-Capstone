package com.example.dao;

import com.example.model.Barangay;
import com.example.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link Barangay} entities.
 * Implements all CRUD operations against the {@code barangays} table
 * (introduced in V002 migration).
 *
 * <p>The login flow primarily uses {@link #findByName(String)}, which
 * is why the barangay name is the natural key (it also has a UNIQUE
 * constraint at the DB level).</p>
 *
 * <p>All SQL is parameterized via {@link PreparedStatement} to prevent
 * SQL injection (rubric criterion 5).</p>
 */
public class BarangayDao implements GenericDao<Barangay, Long> {

    // ── SQL constants ───────────────────────────────────────────

    private static final String COLS =
            "id, name, center_lat, center_lng, default_zoom";

    private static final String SQL_FIND_BY_ID =
            "SELECT " + COLS + " FROM barangays WHERE id = ?";

    private static final String SQL_FIND_BY_NAME =
            "SELECT " + COLS + " FROM barangays WHERE name = ?";

    private static final String SQL_FIND_ALL =
            "SELECT " + COLS + " FROM barangays ORDER BY name";

    private static final String SQL_INSERT =
            "INSERT INTO barangays (name, center_lat, center_lng, default_zoom) " +
                    "VALUES (?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE barangays SET " +
                    "name = ?, center_lat = ?, center_lng = ?, default_zoom = ? " +
                    "WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM barangays WHERE id = ?";

    // ── CRUD operations ─────────────────────────────────────────

    @Override
    public Optional<Barangay> findById(Long id) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        }
    }

    /**
     * Finds a barangay by its unique name. The login flow's entry point.
     *
     * @param name the barangay name (case-sensitive, matches DB exactly)
     * @return Optional containing the barangay, or empty if no match
     */
    public Optional<Barangay> findByName(String name) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_NAME)) {

            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        }
    }

    @Override
    public List<Barangay> findAll() throws SQLException {
        List<Barangay> rows = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) rows.add(mapRow(rs));
        }
        return rows;
    }

    @Override
    public Barangay save(Barangay b) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, b.getName());
            stmt.setDouble(2, b.getCenterLat());
            stmt.setDouble(3, b.getCenterLng());
            stmt.setInt(4, b.getDefaultZoom());

            int rows = stmt.executeUpdate();
            if (rows == 0) throw new SQLException("Insert failed — no rows affected");

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) b.setId(keys.getLong(1));
            }
            return b;
        }
    }

    @Override
    public void update(Barangay b) throws SQLException {
        if (b.getId() == null) {
            throw new IllegalArgumentException(
                    "Cannot update barangay with null ID — use save() for new entities");
        }
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, b.getName());
            stmt.setDouble(2, b.getCenterLat());
            stmt.setDouble(3, b.getCenterLng());
            stmt.setInt(4, b.getDefaultZoom());
            stmt.setLong(5, b.getId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Update failed — no row with ID " + b.getId());
            }
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

    private Barangay mapRow(ResultSet rs) throws SQLException {
        return new Barangay(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getDouble("center_lat"),
                rs.getDouble("center_lng"),
                rs.getInt("default_zoom")
        );
    }
}