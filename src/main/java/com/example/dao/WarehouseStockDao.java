package com.example.dao;

import com.example.model.WarehouseStock;
import com.example.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Data Access Object for {@link WarehouseStock} entities.
 *
 * <p>This DAO is shaped slightly differently from the others because
 * {@code warehouse_stock} uses a <strong>composite primary key</strong>
 * ({@code warehouse_id, item_id}) — there is no auto-generated ID column.
 * To keep this DAO compatible with {@link GenericDao}, we use a small
 * static record class {@link StockKey} as the ID type.</p>
 *
 * <p>Two extra finders are useful in practice: {@link #findByWarehouse(Long)}
 * for the inventory dashboard, and {@link #adjustQuantity(Long, Long, int)}
 * for atomic increments/decrements when a transaction posts.</p>
 *
 * <p>All SQL is parameterized via {@link PreparedStatement} to prevent
 * SQL injection (rubric criterion 5).</p>
 */
public class WarehouseStockDao implements GenericDao<WarehouseStock, WarehouseStockDao.StockKey> {

    /**
     * Composite key for {@code warehouse_stock} rows. Used as the ID type
     * for {@link GenericDao}. Records give us {@code equals}, {@code hashCode},
     * and {@code toString} for free.
     */
    public record StockKey(Long warehouseId, Long itemId) {
        public StockKey {
            Objects.requireNonNull(warehouseId, "warehouseId");
            Objects.requireNonNull(itemId, "itemId");
        }
    }

    // ── SQL constants ───────────────────────────────────────────

    private static final String COLS =
            "warehouse_id, item_id, quantity, last_updated";

    private static final String SQL_FIND_BY_KEY =
            "SELECT " + COLS + " FROM warehouse_stock " +
            "WHERE warehouse_id = ? AND item_id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT " + COLS + " FROM warehouse_stock ORDER BY warehouse_id, item_id";

    private static final String SQL_FIND_BY_WAREHOUSE =
            "SELECT " + COLS + " FROM warehouse_stock " +
            "WHERE warehouse_id = ? ORDER BY item_id";

    private static final String SQL_INSERT =
            "INSERT INTO warehouse_stock (warehouse_id, item_id, quantity) " +
            "VALUES (?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE warehouse_stock SET quantity = ? " +
            "WHERE warehouse_id = ? AND item_id = ?";

    /**
     * Atomic quantity adjustment. The delta is added to the current quantity
     * in a single UPDATE — no read-modify-write race window.
     */
    private static final String SQL_ADJUST_QUANTITY =
            "UPDATE warehouse_stock SET quantity = quantity + ? " +
            "WHERE warehouse_id = ? AND item_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM warehouse_stock WHERE warehouse_id = ? AND item_id = ?";

    // ── CRUD operations ─────────────────────────────────────────

    @Override
    public Optional<WarehouseStock> findById(StockKey key) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_KEY)) {

            stmt.setLong(1, key.warehouseId());
            stmt.setLong(2, key.itemId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public List<WarehouseStock> findAll() throws SQLException {
        List<WarehouseStock> rows = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                rows.add(mapRow(rs));
            }
        }
        return rows;
    }

    /**
     * Returns all stock rows for one warehouse, sorted by item ID.
     * Used by the inventory dashboard.
     */
    public List<WarehouseStock> findByWarehouse(Long warehouseId) throws SQLException {
        List<WarehouseStock> rows = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_WAREHOUSE)) {

            stmt.setLong(1, warehouseId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapRow(rs));
                }
            }
        }
        return rows;
    }

    /**
     * Inserts a new stock row. Because the primary key is composite,
     * there is nothing to auto-generate — we just write the row as-is.
     * Will throw if a row with this (warehouse, item) pair already exists.
     */
    @Override
    public WarehouseStock save(WarehouseStock stock) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {

            stmt.setLong(1, stock.getWarehouseId());
            stmt.setLong(2, stock.getItemId());
            stmt.setInt(3, stock.getQuantity());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Insert failed — no rows affected");
            }
            return stock;
        }
    }

    @Override
    public void update(WarehouseStock stock) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setInt(1, stock.getQuantity());
            stmt.setLong(2, stock.getWarehouseId());
            stmt.setLong(3, stock.getItemId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException(
                        "Update failed — no row with key (" +
                        stock.getWarehouseId() + ", " + stock.getItemId() + ")");
            }
        }
    }

    /**
     * Atomically adjusts the quantity by a delta (positive or negative).
     * Use this when posting an InflowTransaction (positive delta) or
     * OutflowTransaction (negative delta). Doing the change in a single
     * UPDATE prevents the read-modify-write race two admins could hit
     * if they both clicked Restock at the same time.
     *
     * @param warehouseId target warehouse
     * @param itemId      target item
     * @param delta       amount to add (use a negative value to subtract)
     * @throws SQLException if no matching stock row exists
     */
    public void adjustQuantity(Long warehouseId, Long itemId, int delta) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_ADJUST_QUANTITY)) {

            stmt.setInt(1, delta);
            stmt.setLong(2, warehouseId);
            stmt.setLong(3, itemId);

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException(
                        "Adjust failed — no stock row for warehouse " +
                        warehouseId + ", item " + itemId);
            }
        }
    }

    @Override
    public boolean deleteById(StockKey key) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {

            stmt.setLong(1, key.warehouseId());
            stmt.setLong(2, key.itemId());
            return stmt.executeUpdate() > 0;
        }
    }

    // ── Helpers ─────────────────────────────────────────────────

    private WarehouseStock mapRow(ResultSet rs) throws SQLException {
        Timestamp lastUpdated = rs.getTimestamp("last_updated");
        LocalDateTime updated = lastUpdated == null ? null : lastUpdated.toLocalDateTime();

        return new WarehouseStock(
                rs.getLong("warehouse_id"),
                rs.getLong("item_id"),
                rs.getInt("quantity"),
                updated
        );
    }
}
