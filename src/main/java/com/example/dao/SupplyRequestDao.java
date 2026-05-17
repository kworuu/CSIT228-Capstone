package com.example.dao;

import com.example.model.SupplyRequest;
import com.example.model.SupplyRequestStatus;
import com.example.util.DBConnectionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SupplyRequestDao {

    public void saveRequest(SupplyRequest req) throws SQLException {
        String sql = "INSERT INTO supply_requests (requesting_user_id, item_id, target_center_id, quantity, status, notes) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, req.userId());
            ps.setLong(2, req.itemId());

            if (req.targetCenterId() != null) {
                ps.setLong(3, req.targetCenterId());
            } else {
                ps.setNull(3, java.sql.Types.BIGINT);
            }

            ps.setInt(4, req.quantity());
            // FIX: Added .toLowerCase() to match database layout standards during insert
            ps.setString(5, req.status().name().toLowerCase());
            ps.setString(6, req.notes());
            ps.executeUpdate();
        }
    }

    public void updateStatus(long requestId, SupplyRequestStatus newStatus) throws SQLException {
        String sql = "UPDATE supply_requests SET status = ? WHERE id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // FIX: Added .toLowerCase() to perfectly match the database ENUM data definition constraints
            ps.setString(1, newStatus.name().toLowerCase());
            ps.setLong(2, requestId);
            ps.executeUpdate();
        }
    }

    private SupplyRequest mapRowToSupplyRequest(ResultSet rs) throws SQLException {
        // FIX: Replaced valueOf directly with the new safe case-insensitive parsing method
        SupplyRequestStatus status = SupplyRequestStatus.fromString(rs.getString("status"));

        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        java.time.LocalDateTime createdAt = (createdAtTimestamp != null) ? createdAtTimestamp.toLocalDateTime() : null;

        return new SupplyRequest(
                rs.getLong("id"),
                rs.getLong("item_id"),
                rs.getString("item_name"),
                rs.getObject("target_center_id") != null ? rs.getLong("target_center_id") : null,
                rs.getString("center_name") != null ? rs.getString("center_name") : "General LGU",
                rs.getInt("quantity"),
                status,
                rs.getString("barangay"),
                rs.getLong("requesting_user_id"),
                rs.getString("notes"),
                createdAt
        );
    }


    public List<SupplyRequest> getRequestsByBarangay(String brgyName) throws SQLException {
        List<SupplyRequest> list = new ArrayList<>();
        String sql = """
            SELECT sr.*, u.display_name as barangay, 
                   i.name as item_name, 
                   ec.name as center_name
            FROM supply_requests sr 
            JOIN users u ON sr.requesting_user_id = u.id 
            JOIN inventory_items i ON sr.item_id = i.id
            LEFT JOIN evacuation_centers ec ON sr.target_center_id = ec.id
            WHERE u.display_name = ? 
            ORDER BY sr.created_at DESC
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, brgyName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToSupplyRequest(rs));
                }
            }
        }
        return list;
    }

    public List<SupplyRequest> findAllForAdmin() throws SQLException {
        List<SupplyRequest> list = new ArrayList<>();
        String sql = """
            SELECT sr.*, u.display_name as barangay, 
                   i.name as item_name, 
                   ec.name as center_name
            FROM supply_requests sr 
            JOIN users u ON sr.requesting_user_id = u.id 
            JOIN inventory_items i ON sr.item_id = i.id
            LEFT JOIN evacuation_centers ec ON sr.target_center_id = ec.id
            ORDER BY sr.created_at DESC
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToSupplyRequest(rs));
            }
        }
        return list;
    }

}