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
 */
public class InventoryItemDao implements GenericDao<InventoryItem, Long> {

    private static final String COLS =
            "id, name, category, unit, critical_threshold, low_threshold, stock_quantity, created_at, created_by_user_id";

    private static final String SQL_INSERT =
            "INSERT INTO inventory_items (name, category, unit, critical_threshold, low_threshold, stock_quantity, created_by_user_id, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT " + COLS + " FROM inventory_items WHERE id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT " + COLS + " FROM inventory_items ORDER BY name";

    private static final String SQL_FIND_BY_CATEGORY =
            "SELECT " + COLS + " FROM inventory_items WHERE category = ? ORDER BY name";

    private static final String SQL_DELETE =
            "DELETE FROM inventory_items WHERE id = ?";

    private static final String SQL_COUNT_CRITICAL_GLOBAL =
            "SELECT COUNT(*) FROM inventory_items WHERE stock_quantity <= critical_threshold";

    private static final String SQL_FIND_CRITICAL_BY_USER =
            "SELECT " + COLS + " FROM inventory_items " +
                    "WHERE created_by_user_id = ? AND stock_quantity <= critical_threshold " +
                    "ORDER BY stock_quantity ASC LIMIT 2";
    private static final String SQL_COUNT_CRITICAL_USER =
            "SELECT COUNT(*) FROM inventory_items WHERE created_by_user_id = ? AND stock_quantity <= critical_threshold";

    // 3. Update the UPDATE query (Ensure indices match the method below)
    private static final String SQL_UPDATE =
            "UPDATE inventory_items SET " +
                    "name = ?, category = ?, unit = ?, critical_threshold = ?, " +
                    "low_threshold = ?, stock_quantity = ?, created_by_user_id = ? " +
                    "WHERE id = ?";


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
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, item.name());
            stmt.setString(2, item.category());
            stmt.setString(3, item.unit());
            stmt.setInt(4, item.criticalThreshold());
            stmt.setInt(5, item.lowThreshold());
            stmt.setInt(6, item.stockQuantity());

            // FIX 1: Index 7 is configured for 'created_by_user_id' inside SQL_INSERT statement
            if (item.createdByUserId() != null) {
                stmt.setLong(7, item.createdByUserId());
            } else {
                stmt.setNull(7, Types.BIGINT);
            }

            // FIX 2: Index 8 is mapped for the 'created_at' timestamp string field
            stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long generatedId = rs.getLong(1);
                    return new InventoryItem(
                            generatedId,
                            item.name(),
                            item.category(),
                            item.unit(),
                            item.criticalThreshold(),
                            item.lowThreshold(),
                            item.stockQuantity(),
                            item.createdAt(),
                            item.createdByUserId()
                    );
                }
            }

            return item;
        }
    }

    @Override
    public void update(InventoryItem item) throws SQLException {
        if (item.id() == 0) throw new IllegalArgumentException("Cannot update item with zero ID");

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, item.name());
            stmt.setString(2, item.category());
            stmt.setString(3, item.unit());
            stmt.setInt(4, item.criticalThreshold()); // Added this
            stmt.setInt(5, item.lowThreshold());
            stmt.setInt(6, item.stockQuantity());

            if (item.createdByUserId() != null) {
                stmt.setLong(7, item.createdByUserId());
            } else {
                stmt.setNull(7, Types.BIGINT);
            }
            stmt.setLong(8, item.id()); // The WHERE id = ? clause

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

    private InventoryItem mapRow(ResultSet rs) throws SQLException {
        return new InventoryItem(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getString("unit"),
                rs.getInt("critical_threshold"), // SQL Column Name
                rs.getInt("low_threshold"),      // SQL Column Name
                rs.getInt("stock_quantity"),     // SQL Column Name
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getObject("created_by_user_id", Long.class)
        );
    }

    public static int getAdminCriticalCount() throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_COUNT_CRITICAL_GLOBAL)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int getCriticalCountByUser(Long userId) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_COUNT_CRITICAL_USER)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<InventoryItem> findCriticalItemsByUser(Long userId) throws SQLException {
        List<InventoryItem> items = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_CRITICAL_BY_USER)) {

            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }
        return items;
    }
}
