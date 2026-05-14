package com.example.dao;

import com.example.model.InventoryItem;
import com.example.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link InventoryItem} entities.
 * Implements all CRUD operations against the {@code inventory_items} table.
 */

public class InventoryItemDao implements GenericDao<InventoryItem, Long> {

    // ── SQL constants ───────────────────────────────────────────

    // Updated to include created_by_user_id and stock_quantity
    private static final String COLS =
            "id, name, category, unit, critical_threshold, low_threshold, created_at, created_by_user_id, stock_quantity";

    private static final String SQL_FIND_BY_ID =
            "SELECT " + COLS + " FROM inventory_items WHERE id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT " + COLS + " FROM inventory_items ORDER BY name";

    private static final String SQL_FIND_BY_CATEGORY =
            "SELECT " + COLS + " FROM inventory_items WHERE category = ? ORDER BY name";

    // Updated INSERT to track the user who created the record and initial stock
    private static final String SQL_INSERT =
            "INSERT INTO inventory_items " +
                    "(name, category, unit, critical_threshold, low_threshold, created_by_user_id, stock_quantity) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

    // Updated UPDATE to allow modifying the assigned user if necessary
    private static final String SQL_UPDATE =
            "UPDATE inventory_items SET " +
                    "name = ?, category = ?, unit = ?, " +
                    "critical_threshold = ?, low_threshold = ?, created_by_user_id = ?, stock_quantity = ? " +
                    "WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM inventory_items WHERE id = ?";

    // Get Number of Critical Item on admin side
    private static final String SQL_COUNT_CRITICAL_GLOBAL =
            "SELECT COUNT(*) FROM inventory_items WHERE stock_quantity <= critical_threshold";

    // Get Number of Critical Item on brgy side
    private static final String SQL_COUNT_CRITICAL_BY_USER =
            "SELECT COUNT(*) FROM inventory_items WHERE created_by_user_id = ? AND stock_quantity <= critical_threshold";


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

            // Handle nullable creator ID
            if (item.getCreatedByUserId() != null) {
                stmt.setLong(6, item.getCreatedByUserId());
            } else {
                stmt.setNull(6, Types.BIGINT);
            }

            // Set initial stock (Parameter 7)
            stmt.setInt(7, item.getStockQuantity());

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

            if (item.getCreatedByUserId() != null) {
                stmt.setLong(6, item.getCreatedByUserId());
            } else {
                stmt.setNull(6, Types.BIGINT);
            }

            stmt.setInt(7, item.getStockQuantity()); // Update current stock
            stmt.setLong(8, item.getId());

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

        // Extracting Long which could be NULL in DB
        long userIdVal = rs.getLong("created_by_user_id");
        Long createdByUserId = rs.wasNull() ? null : userIdVal;

        return new InventoryItem(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getString("unit"),
                rs.getInt("critical_threshold"),
                rs.getInt("low_threshold"),
                created,
                rs.getInt("stock_quantity"), // Passing integrated stock to constructor
                createdByUserId // Passing the creator ID field
        );
    }

    /** Returns total number of critical items across the whole system (Admin view) */
    public static int getAdminCriticalCount() throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_COUNT_CRITICAL_GLOBAL)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Returns number of critical items for a specific barangay/user */
    public int getCriticalCountByUser(Long userId) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_COUNT_CRITICAL_BY_USER)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }


}