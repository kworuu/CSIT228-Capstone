package com.example.dao;

import com.example.model.EvacuationCenter;
import com.example.util.DBConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvacuationCenterDao {

    public List<EvacuationCenter> findAll() throws SQLException {
        List<EvacuationCenter> centers = new ArrayList<>();
        String sql = "SELECT * FROM evacuation_centers ORDER BY name";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) centers.add(mapRow(rs));
        }
        return centers;
    }

    public List<EvacuationCenter> findByBarangay(String barangayDisplayName) throws SQLException {
        List<EvacuationCenter> centers = new ArrayList<>();
        // FIXED: Since 'barangay' column is gone, we JOIN the users table to filter by display name!
        String sql = """
            SELECT ec.* FROM evacuation_centers ec
            JOIN users u ON ec.user_id = u.id
            WHERE u.display_name = ?
            ORDER BY ec.name
            """;
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barangayDisplayName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) centers.add(mapRow(rs));
            }
        }
        return centers;
    }

    public void save(EvacuationCenter center) throws SQLException {
        String sql = "INSERT INTO evacuation_centers (name, address, user_id, photo_path, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, center.name());
            ps.setString(2, center.address());
            if (center.userId() != null) ps.setLong(3, center.userId()); else ps.setNull(3, Types.BIGINT);
            ps.setString(4, center.photoPath());
            if (center.latitude() != null) ps.setDouble(5, center.latitude()); else ps.setNull(5, Types.DECIMAL);
            if (center.longitude() != null) ps.setDouble(6, center.longitude()); else ps.setNull(6, Types.DECIMAL);

            ps.executeUpdate();
        }
    }

    public void updateCenterStatus(EvacuationCenter center) throws SQLException {
        // FIXED: Only update name and address, since capacity and occupancy are gone!
        String sql = "UPDATE evacuation_centers SET name = ?, address = ? WHERE id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, center.name());
            ps.setString(2, center.address());
            ps.setLong(3, center.id());
            ps.executeUpdate();
        }
    }

    private EvacuationCenter mapRow(ResultSet rs) throws SQLException {
        Double lat = rs.getObject("latitude") != null ? rs.getDouble("latitude") : null;
        Double lng = rs.getObject("longitude") != null ? rs.getDouble("longitude") : null;
        Long uId = rs.getObject("user_id") != null ? rs.getLong("user_id") : null;
        Timestamp created = rs.getTimestamp("created_at");

        // FIXED: Uses exact 8-parameter record
        return new EvacuationCenter(
                rs.getLong("id"), rs.getString("name"), rs.getString("address"),
                uId, rs.getString("photo_path"), lat, lng,
                created != null ? created.toLocalDateTime() : null
        );
    }
    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM evacuation_centers WHERE id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}