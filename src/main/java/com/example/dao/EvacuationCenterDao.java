package com.example.dao;

import com.example.model.EvacuationCenter;
import com.example.model.StructuralStatus;
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

    public void updateCenterStatus(EvacuationCenter center) throws SQLException {
        String sql = "UPDATE evacuation_centers SET is_active = ?, capacity = ? WHERE id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, center.isActive());
            ps.setInt(2, center.capacity());
            ps.setLong(3, center.id());
            ps.executeUpdate();
        }
    }

    private EvacuationCenter mapRow(ResultSet rs) throws SQLException {
        Double lat = rs.getObject("latitude") != null ? rs.getDouble("latitude") : null;
        Double lng = rs.getObject("longitude") != null ? rs.getDouble("longitude") : null;
        Long uId = rs.getObject("user_id") != null ? rs.getLong("user_id") : null;

        Timestamp structUpdated = rs.getTimestamp("structural_updated_at");
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");

        return new EvacuationCenter(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("barangay"),
                rs.getString("photo_path"),
                rs.getInt("capacity"),
                rs.getInt("current_occupancy"),
                lat,
                lng,
                StructuralStatus.fromDb(rs.getString("structural_status")),
                rs.getString("structural_notes"),
                structUpdated != null ? structUpdated.toLocalDateTime() : null,
                uId,
                rs.getBoolean("is_active"),
                created != null ? created.toLocalDateTime() : null,
                updated != null ? updated.toLocalDateTime() : null
        );
    }
}