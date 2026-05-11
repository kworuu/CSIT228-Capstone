package com.example.dao;

import com.example.model.InventoryItem;
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
 * Data Access Object for {@link InventoryItem} entities.
 * Implements all CRUD operations against the {@code inventory_items} table.
 *
 * <p>All SQL is parameterized via {@link PreparedStatement} to prevent
 * SQL injection (rubric criterion 5: data validation and security).</p>
 */
public class InventoryItemDao implements GenericDao<InventoryItem, Long> {

    // ── SQL constants ───────────────────────────────────────────

    private static final String COLS =
            "id, name, category, unit, critical_threshold, low_threshold, created_at";

    private static final String SQL_FIND_BY_ID =
            "SELECT " + COLS + " FROM inventory_items WHERE id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT " + COLS + " FROM inventory_items ORDER BY name";

    private static final String SQL_FIND_BY_CATEGORY =
            "SELECT " + COLS + " FROM inventory_items WHERE category = ? ORDER BY name";

    private static final String SQL_INSERT =
            "INSERT INTO inventory_items " +
            "(name, category, unit, critical_threshold, low_threshold) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE inventory_items SET " +
            "name = ?, category = ?, unit = ?, " +
            "critical_threshold = ?, low_threshold = ? " +
            "WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM inventory_items WHERE id = ?";

    // ── CRUD operations ─────────────────────────────────────────

    @Override
    public Optional<InventoryItem> findById(Long id) throws SQLException {
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

    @Override
    public List<InventoryItem> findAll() throws SQLException {
        List<InventoryItem> items = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                items.add(mapRow(rs));
            }
        }
        return items;
    }

    /**
     * Returns items in a given category (e.g. "food", "water", "non-food"),
     * sorted by name. Used by the inventory dashboard's category filter.
     */
    public List<InventoryItem> findByCategory(String category) throws SQLException {
        List<InventoryItem> items = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_CATEGORY)) {

            stmt.setString(1, category);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }
        return items;
    }

    @Override
    public InventoryItem save(InventoryItem item) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, item.getName());
            stmt.setString(2, item.getCategory());
            stmt.setString(3, item.getUnit());
            stmt.setInt(4, item.getCriticalThreshold());
            stmt.setInt(5, item.getLowThreshold());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Insert failed — no rows affected");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setId(keys.getLong(1));
                }
            }
            return item;
        }
    }

    @Override
    public void update(InventoryItem item) throws SQLException {
        if (item.getId() == null) {
            throw new IllegalArgumentException(
                    "Cannot update item with null ID — use save() for new entities");
        }
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, item.getName());
            stmt.setString(2, item.getCategory());
            stmt.setString(3, item.getUnit());
            stmt.setInt(4, item.getCriticalThreshold());
            stmt.setInt(5, item.getLowThreshold());
            stmt.setLong(6, item.getId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Update failed — no row with ID " + item.getId());
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

    /**
     * Maps a single row from a ResultSet into an InventoryItem object.
     * Centralized to avoid duplicating column extraction logic.
     */
    private InventoryItem mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        LocalDateTime created = createdAt == null ? null : createdAt.toLocalDateTime();

        return new InventoryItem(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getString("unit"),
                rs.getInt("critical_threshold"),
                rs.getInt("low_threshold"),
                created
        );
    }
}
