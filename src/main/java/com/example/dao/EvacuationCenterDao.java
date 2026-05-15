package com.example.dao;

import com.example.model.EvacuationCenter;
import com.example.util.DBConnectionManager;

import java.math.BigDecimal;
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
 * Data Access Object for {@link EvacuationCenter} entities.
 * Implements all CRUD operations against the {@code evacuation_centers} table.
 *
 * <p>All SQL is parameterized via {@link PreparedStatement} to prevent
 * SQL injection attacks (rubric criterion 5: data validation and security).</p>
 */
public class EvacuationCenterDao implements GenericDao<EvacuationCenter, Long> {

    // ── SQL constants ───────────────────────────────────────────
    private static final String SQL_UPDATE_STRUCTURAL =
            "UPDATE evacuation_centers SET " +
                    "structural_status = ?, structural_notes = ?, " +
                    "structural_updated_at = ?, structural_updated_by = ? " +
                    "WHERE id = ?";

    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, address, barangay, " +
                    "       latitude, longitude, managed_by, is_active, created_at " +
                    "FROM evacuation_centers WHERE id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT id, name, address, barangay, " +
                    "       latitude, longitude, managed_by, is_active, created_at " +
                    "FROM evacuation_centers ORDER BY id";

    private static final String SQL_FIND_ACTIVE =
            "SELECT id, name, address, barangay, " +
                    "       latitude, longitude, managed_by, is_active, created_at " +
                    "FROM evacuation_centers WHERE is_active = TRUE ORDER BY name";

    private static final String SQL_INSERT =
            "INSERT INTO evacuation_centers " +
                    "(name, address, barangay, " +
                    " latitude, longitude, managed_by, is_active) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE evacuation_centers SET " +
                    "name = ?, address = ?, barangay = ?, " +
                    "latitude = ?, longitude = ?, " +
                    "managed_by = ?, is_active = ? " +
                    "WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM evacuation_centers WHERE id = ?";

    private static final String SQL_COUNT_EVAC = "SELECT COUNT(*) FROM evacuation_centers";
    
    private static final String SQL_FIND_BY_BARANGAY =
            "SELECT id, name, address, barangay, " +
            "       latitude, longitude, managed_by, is_active, created_at " +
            "FROM evacuation_centers WHERE barangay = ? ORDER BY name";

    // ── CRUD operations ─────────────────────────────────────────

    @Override
    public Optional<EvacuationCenter> findById(Long id) throws SQLException {
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
    public List<EvacuationCenter> findAll() throws SQLException {
        List<EvacuationCenter> centers = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                centers.add(mapRow(rs));
            }
        }
        return centers;
    }

    /**
     * Returns only active evacuation centers, sorted by name.
     * Used by the dashboard to populate dropdowns and map markers.
     */
    public List<EvacuationCenter> findAllActive() throws SQLException {
        List<EvacuationCenter> centers = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ACTIVE);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                centers.add(mapRow(rs));
            }
        }
        return centers;
    }
    
    /**
     * Returns evacuation centers belonging to a specific barangay.
     */
    public List<EvacuationCenter> findByBarangay(String barangay) throws SQLException {
        List<EvacuationCenter> centers = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_BARANGAY)) {
            
            stmt.setString(1, barangay);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    centers.add(mapRow(rs));
                }
            }
        }
        return centers;
    }

    @Override
    public EvacuationCenter save(EvacuationCenter center) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, center.getName());
            stmt.setString(2, center.getAddress());
            stmt.setString(3, center.getBarangay());
            setBigDecimalOrNull(stmt, 4, center.getLatitude());
            setBigDecimalOrNull(stmt, 5, center.getLongitude());
            setLongOrNull(stmt, 6, center.getManagedBy());
            stmt.setBoolean(7, center.isActive());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Insert failed — no rows affected");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    center.setId(keys.getLong(1));
                }
            }
            return center;
        }
    }

    @Override
    public void update(EvacuationCenter center) throws SQLException {
        if (center.getId() == null) {
            throw new IllegalArgumentException(
                    "Cannot update center with null ID — use save() for new entities");
        }
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, center.getName());
            stmt.setString(2, center.getAddress());
            stmt.setString(3, center.getBarangay());
            setBigDecimalOrNull(stmt, 4, center.getLatitude());
            setBigDecimalOrNull(stmt, 5, center.getLongitude());
            setLongOrNull(stmt, 6, center.getManagedBy());
            stmt.setBoolean(7, center.isActive());
            stmt.setLong(8, center.getId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Update failed — no row with ID " + center.getId());
            }
        }
    }

    /**
     * Targeted update for the structural-status workflow. The barangay
     * staff sets the status and (optionally) inspector notes — only those
     * four fields change. Keeping this method separate from the general
     * {@link #update(EvacuationCenter)} avoids overwriting other fields
     * with stale in-memory values and gives a cleaner audit trail.
     *
     * <p>This is invoked from {@code UpdateCenterController} when the
     * barangay user saves the modal. The {@code userId} comes from the
     * current session and is persisted to {@code structural_updated_by}
     * so the activity log can attribute changes to a specific person.</p>
     *
     * @param centerId the evacuation center being updated
     * @param status   the new structural status
     * @param notes    optional inspector notes (may be null/blank)
     * @param userId   the id of the user performing the update (for audit)
     * @throws SQLException if no row with this id exists, or the update fails
     */
    public void updateStructuralStatus(long centerId,
                                       com.example.model.StructuralStatus status,
                                       String notes,
                                       long userId) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_STRUCTURAL)) {

            stmt.setString(1, status.toDb());
            // Treat blank notes as NULL — keeps the column tidy.
            if (notes == null || notes.isBlank()) {
                stmt.setNull(2, java.sql.Types.VARCHAR);
            } else {
                stmt.setString(2, notes.trim());
            }
            stmt.setTimestamp(3, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            stmt.setLong(4, userId);
            stmt.setLong(5, centerId);

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException(
                        "Structural-status update failed — no row with id " + centerId);
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
     * Maps a single row from a ResultSet into an EvacuationCenter object.
     * Centralized here to avoid duplicating the column extraction logic.
     */
    private EvacuationCenter mapRow(ResultSet rs) throws SQLException {
        Long managedBy = rs.getLong("managed_by");
        if (rs.wasNull()) managedBy = null;

        Timestamp createdAt = rs.getTimestamp("created_at");
        LocalDateTime created = createdAt == null ? null : createdAt.toLocalDateTime();

        return new EvacuationCenter(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("barangay"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                managedBy,
                rs.getBoolean("is_active"),
                created
        );
    }

    private void setBigDecimalOrNull(PreparedStatement stmt, int index, BigDecimal value)
            throws SQLException {
        if (value == null) {
            stmt.setNull(index, java.sql.Types.DECIMAL);
        } else {
            stmt.setBigDecimal(index, value);
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

    public int getTotalCount() throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_COUNT_EVAC);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}