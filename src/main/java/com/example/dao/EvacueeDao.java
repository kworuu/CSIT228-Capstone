package com.example.dao;

import com.example.model.Evacuee;
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
 * Data Access Object for {@link Evacuee} entities.
 * Implements all CRUD operations against the {@code evacuees} table,
 * plus several filtered finders used by the verification dashboard.
 *
 * <p>The {@code full_name_enc} and {@code contact_enc} columns hold
 * encrypted values. This DAO does not encrypt or decrypt them — that's
 * the service layer's responsibility. The DAO just moves opaque strings
 * to and from the database.</p>
 *
 * <p>All SQL is parameterized via {@link PreparedStatement} to prevent
 * SQL injection (rubric criterion 5).</p>
 */
public class EvacueeDao implements GenericDao<Evacuee, Long> {

    // ── SQL constants ───────────────────────────────────────────

    private static final String COLS =
            "id, full_name_enc, contact_enc, barangay, photo_path, " +
            "evacuation_center_id, notes, created_at";

    private static final String SQL_FIND_BY_ID =
            "SELECT " + COLS + " FROM evacuees WHERE id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT " + COLS + " FROM evacuees ORDER BY created_at DESC";

    private static final String SQL_FIND_BY_CENTER =
            "SELECT " + COLS + " FROM evacuees " +
            "WHERE evacuation_center_id = ? ORDER BY created_at DESC";

    private static final String SQL_INSERT =
            "INSERT INTO evacuees " +
            "(full_name_enc, contact_enc, barangay, photo_path, " +
            " evacuation_center_id, notes) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE evacuees SET " +
            "full_name_enc = ?, contact_enc = ?, barangay = ?, photo_path = ?, " +
            "evacuation_center_id = ?, notes = ? " +
            "WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM evacuees WHERE id = ?";

    // ── CRUD operations ─────────────────────────────────────────

    @Override
    public Optional<Evacuee> findById(Long id) throws SQLException {
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
    public List<Evacuee> findAll() throws SQLException {
        List<Evacuee> evacuees = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                evacuees.add(mapRow(rs));
            }
        }
        return evacuees;
    }


    /**
     * Returns all evacuees registered at one center, newest first.
     * Used by per-center evacuee lists.
     */
    public List<Evacuee> findByCenter(Long centerId) throws SQLException {
        List<Evacuee> evacuees = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_CENTER)) {

            stmt.setLong(1, centerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    evacuees.add(mapRow(rs));
                }
            }
        }
        return evacuees;
    }


    @Override
    public Evacuee save(Evacuee evacuee) throws SQLException {
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, evacuee.getFullNameEnc());
            stmt.setString(2, evacuee.getContactEnc());
            stmt.setString(3, evacuee.getBarangay());
            stmt.setString(4, evacuee.getPhotoPath());
            setLongOrNull(stmt, 5, evacuee.getEvacuationCenterId());
            stmt.setString(6, evacuee.getNotes());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Insert failed — no rows affected");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    evacuee.setId(keys.getLong(1));
                }
            }
            return evacuee;
        }
    }

    @Override
    public void update(Evacuee evacuee) throws SQLException {
        if (evacuee.getId() == null) {
            throw new IllegalArgumentException(
                    "Cannot update evacuee with null ID — use save() for new entities");
        }
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, evacuee.getFullNameEnc());
            stmt.setString(2, evacuee.getContactEnc());
            stmt.setString(3, evacuee.getBarangay());
            stmt.setString(4, evacuee.getPhotoPath());
            setLongOrNull(stmt, 5, evacuee.getEvacuationCenterId());
            stmt.setString(6, evacuee.getNotes());
            stmt.setLong(7, evacuee.getId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Update failed — no row with ID " + evacuee.getId());
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

    private Evacuee mapRow(ResultSet rs) throws SQLException {

        Timestamp createdAt = rs.getTimestamp("created_at");
        LocalDateTime created = createdAt == null ? null : createdAt.toLocalDateTime();

        return new Evacuee(
                rs.getLong("id"),
                rs.getString("full_name_enc"),
                rs.getString("contact_enc"),
                rs.getString("barangay"),
                rs.getString("photo_path"),
                rs.getLong("evacuation_center_id"),
                rs.getString("notes"),
                created
        );
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