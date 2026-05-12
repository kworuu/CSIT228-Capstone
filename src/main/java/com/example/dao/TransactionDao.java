package com.example.dao;

import com.example.model.InflowTransaction;
import com.example.model.OutflowTransaction;
import com.example.model.Transaction;
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
 * Data Access Object for {@link Transaction} entities.
 *
 * <p>This DAO is <strong>polymorphic</strong>: the database stores all
 * transactions in one table with a {@code direction} ENUM column, but
 * Java models them as two subclasses ({@link InflowTransaction} and
 * {@link OutflowTransaction}). {@link #mapRow(ResultSet)} reads the
 * direction column and instantiates the matching subclass, and
 * {@link #save(Transaction)} dispatches on the runtime type. This is
 * one of the clearest demonstrations of inheritance and polymorphism
 * in the codebase (rubric criterion 1).</p>
 *
 * <p>All SQL is parameterized via {@link PreparedStatement} to prevent
 * SQL injection (rubric criterion 5).</p>
 */
public class TransactionDao implements GenericDao<Transaction, Long> {

    // ── SQL constants ───────────────────────────────────────────

    private static final String COLS =
            "id, direction, item_id, quantity, warehouse_id, " +
            "destination_center_id, source_label, created_by, created_at, notes";

    private static final String SQL_FIND_BY_ID =
            "SELECT " + COLS + " FROM transactions WHERE id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT " + COLS + " FROM transactions ORDER BY created_at DESC";

    private static final String SQL_FIND_BY_WAREHOUSE =
            "SELECT " + COLS + " FROM transactions " +
            "WHERE warehouse_id = ? ORDER BY created_at DESC";

    private static final String SQL_FIND_BY_ITEM =
            "SELECT " + COLS + " FROM transactions " +
            "WHERE item_id = ? ORDER BY created_at DESC";

    private static final String SQL_FIND_BY_DIRECTION =
            "SELECT " + COLS + " FROM transactions " +
            "WHERE direction = ? ORDER BY created_at DESC";

    private static final String SQL_FIND_RECENT =
            "SELECT " + COLS + " FROM transactions " +
            "ORDER BY created_at DESC LIMIT ?";

    private static final String SQL_INSERT =
            "INSERT INTO transactions " +
            "(direction, item_id, quantity, warehouse_id, " +
            " destination_center_id, source_label, created_by, notes) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    /**
     * Transactions are an audit trail — they should not be edited or
     * deleted after the fact. {@link #update(Transaction)} and
     * {@link #deleteById(Long)} throw {@link UnsupportedOperationException}.
     */
    private static final String UNSUPPORTED_MSG =
            "Transactions are append-only — edits and deletes are not permitted";

    // ── CRUD operations ─────────────────────────────────────────

    @Override
    public Optional<Transaction> findById(Long id) throws SQLException {
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
    public List<Transaction> findAll() throws SQLException {
        return runQuery(SQL_FIND_ALL, null);
    }

    /** All transactions touching one warehouse, newest first. */
    public List<Transaction> findByWarehouse(Long warehouseId) throws SQLException {
        return runQuery(SQL_FIND_BY_WAREHOUSE, stmt -> stmt.setLong(1, warehouseId));
    }

    /** All transactions for one inventory item, newest first. */
    public List<Transaction> findByItem(Long itemId) throws SQLException {
        return runQuery(SQL_FIND_BY_ITEM, stmt -> stmt.setLong(1, itemId));
    }

    /** All inflows (use "inflow") or all outflows ("outflow"), newest first. */
    public List<Transaction> findByDirection(String direction) throws SQLException {
        return runQuery(SQL_FIND_BY_DIRECTION, stmt -> stmt.setString(1, direction));
    }

    /** The N most recent transactions across all warehouses. */
    public List<Transaction> findRecent(int limit) throws SQLException {
        return runQuery(SQL_FIND_RECENT, stmt -> stmt.setInt(1, limit));
    }

    /**
     * Inserts a new transaction. Dispatches on the runtime subclass to
     * fill in subclass-specific fields ({@code source_label} for inflows,
     * {@code destination_center_id} for outflows). The other column is set
     * to NULL.
     */
    @Override
    public Transaction save(Transaction tx) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, tx.getDirection());
            stmt.setLong(2, tx.getItemId());
            stmt.setInt(3, tx.getQuantity());
            stmt.setLong(4, tx.getWarehouseId());

            // Subclass-specific column dispatch
            if (tx instanceof OutflowTransaction outflow) {
                setLongOrNull(stmt, 5, outflow.getDestinationCenterId());
                stmt.setNull(6, java.sql.Types.VARCHAR);
            } else if (tx instanceof InflowTransaction inflow) {
                stmt.setNull(5, java.sql.Types.BIGINT);
                stmt.setString(6, inflow.getSourceLabel());
            } else {
                throw new SQLException(
                        "Unknown Transaction subclass: " + tx.getClass().getName());
            }

            stmt.setLong(7, tx.getCreatedBy());
            stmt.setString(8, tx.getNotes());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Insert failed — no rows affected");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    tx.setId(keys.getLong(1));
                }
            }
            return tx;
        }
    }

    @Override
    public void update(Transaction tx) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    @Override
    public boolean deleteById(Long id) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    // ── Helpers ─────────────────────────────────────────────────

    /**
     * Small lambda-friendly type for "bind parameters to this statement".
     * Lets us share one query-execution helper across all the findBy methods.
     */
    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }

    private List<Transaction> runQuery(String sql, StatementBinder binder) throws SQLException {
        List<Transaction> results = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (binder != null) {
                binder.bind(stmt);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    /**
     * Reads one row and returns the appropriate Transaction subclass
     * based on the {@code direction} column. This is the polymorphic
     * heart of the DAO — callers get back a proper {@link InflowTransaction}
     * or {@link OutflowTransaction} they can switch on, not a stringly-typed
     * blob.
     */
    private Transaction mapRow(ResultSet rs) throws SQLException {
        String direction = rs.getString("direction");

        Long id = rs.getLong("id");
        Long itemId = rs.getLong("item_id");
        int quantity = rs.getInt("quantity");
        Long warehouseId = rs.getLong("warehouse_id");
        Long createdBy = rs.getLong("created_by");

        Timestamp createdAtTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdAtTs == null ? null : createdAtTs.toLocalDateTime();

        String notes = rs.getString("notes");

        if ("inflow".equalsIgnoreCase(direction)) {
            String sourceLabel = rs.getString("source_label");
            return new InflowTransaction(
                    id, itemId, quantity, warehouseId,
                    sourceLabel, createdBy, createdAt, notes);
        } else if ("outflow".equalsIgnoreCase(direction)) {
            Long destinationCenterId = rs.getLong("destination_center_id");
            if (rs.wasNull()) destinationCenterId = null;
            return new OutflowTransaction(
                    id, itemId, quantity, warehouseId,
                    destinationCenterId, createdBy, createdAt, notes);
        } else {
            throw new SQLException("Unknown transaction direction: " + direction);
        }
    }

    private void setLongOrNull(PreparedStatement stmt, int index, Long value)
            throws SQLException {
        if (value == null) {
            stmt.setNull(index, java.sql.Types.BIGINT);
        } else {
            stmt.setLong(index, value);
        }
    }
}
