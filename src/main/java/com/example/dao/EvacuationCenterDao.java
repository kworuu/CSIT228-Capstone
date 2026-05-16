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

    // NEW: Fixes the ManageSupplyRequestsController error!
    public List<EvacuationCenter> findByBarangay(String barangay) throws SQLException {
        List<EvacuationCenter> centers = new ArrayList<>();
        String sql = "SELECT * FROM evacuation_centers WHERE barangay = ? ORDER BY name";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barangay);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) centers.add(mapRow(rs));
            }
        }
        return centers;
    }

    public void save(EvacuationCenter center) throws SQLException {
        String sql = "INSERT INTO evacuation_centers (name, address, barangay, capacity, current_occupancy, latitude, longitude, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, center.name());
            ps.setString(2, center.address());
            ps.setString(3, center.barangay());
            ps.setInt(4, center.capacity());
            ps.setInt(5, center.currentOccupancy());

            if (center.latitude() != null) ps.setDouble(6, center.latitude()); else ps.setNull(6, Types.DECIMAL);
            if (center.longitude() != null) ps.setDouble(7, center.longitude()); else ps.setNull(7, Types.DECIMAL);
            if (center.userId() != null) ps.setLong(8, center.userId()); else ps.setNull(8, Types.BIGINT);

            ps.executeUpdate();
        }
    }

    public void updateCenterStatus(EvacuationCenter center) throws SQLException {
        String sql = "UPDATE evacuation_centers SET capacity = ?, current_occupancy = ? WHERE id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, center.capacity());
            ps.setInt(2, center.currentOccupancy());
            ps.setLong(3, center.id());
            ps.executeUpdate();
        }
    }

    private EvacuationCenter mapRow(ResultSet rs) throws SQLException {
        Double lat = rs.getObject("latitude") != null ? rs.getDouble("latitude") : null;
        Double lng = rs.getObject("longitude") != null ? rs.getDouble("longitude") : null;
        Long uId = rs.getObject("user_id") != null ? rs.getLong("user_id") : null;

        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");

        return new EvacuationCenter(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("barangay"),
                rs.getInt("capacity"),
                rs.getInt("current_occupancy"),
                lat,
                lng,
                uId,
                created != null ? created.toLocalDateTime() : null,
                updated != null ? updated.toLocalDateTime() : null
        );
    }
}