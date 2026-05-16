package com.example.dao;

import com.example.model.SupplyRequest;
import com.example.model.SupplyRequestStatus;
import com.example.util.DBConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplyRequestDao {

    public void saveRequest(SupplyRequest request) throws SQLException {
        String sql = """
            INSERT INTO supply_requests 
            (item_id, quantity, status, requesting_barangay, requesting_user_id, notes) 
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, request.itemId());
            ps.setInt(2, request.quantity());
            ps.setString(3, request.status().name());
            ps.setString(4, request.requestingBarangay());

            if (request.requestingUserId() != null) ps.setLong(5, request.requestingUserId());
            else ps.setNull(5, Types.BIGINT);

            ps.setString(6, request.notes());
            ps.executeUpdate();
        }
    }

    public List<SupplyRequest> getRequestsByBarangay(String barangay) throws SQLException {
        List<SupplyRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM supply_requests WHERE requesting_barangay = ? ORDER BY created_at DESC";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barangay);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) requests.add(mapRow(rs));
            }
        }
        return requests;
    }

    public void updateStatus(long requestId, SupplyRequestStatus status) throws SQLException {
        String sql = "UPDATE supply_requests SET status = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, requestId);
            ps.executeUpdate();
        }
    }

    private SupplyRequest mapRow(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");

        return new SupplyRequest(
                rs.getLong("id"),
                rs.getLong("item_id"),
                rs.getInt("quantity"),
                SupplyRequestStatus.fromDb(rs.getString("status")),
                rs.getString("requesting_barangay"),
                rs.getObject("requesting_user_id") != null ? rs.getLong("requesting_user_id") : null,
                rs.getString("notes"),
                created != null ? created.toLocalDateTime() : null,
                updated != null ? updated.toLocalDateTime() : null
        );
    }
}