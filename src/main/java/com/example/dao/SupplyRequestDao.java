package com.example.dao;

import com.example.model.SupplyRequest;
import com.example.model.SupplyRequestStatus;
import com.example.util.DBConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplyRequestDao {

    public void saveRequest(SupplyRequest request) throws SQLException {
        // FIXED: Only inserts valid columns
        String sql = """
            INSERT INTO supply_requests 
            (item_id, quantity, status, requesting_user_id, notes) 
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, request.itemId());
            ps.setInt(2, request.quantity());
            ps.setString(3, request.status().name());

            if (request.requestingUserId() != null) ps.setLong(4, request.requestingUserId());
            else ps.setNull(4, Types.BIGINT);

            ps.setString(5, request.notes());
            ps.executeUpdate();
        }
    }

    public List<SupplyRequest> findAll() throws SQLException {
        List<SupplyRequest> requests = new ArrayList<>();
        // FIXED: Join the users table to dynamically fetch the Barangay's Display Name
        String sql = """
            SELECT sr.*, u.display_name as barangay_name 
            FROM supply_requests sr 
            JOIN users u ON sr.requesting_user_id = u.id 
            ORDER BY sr.created_at DESC
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                requests.add(mapRow(rs));
            }
        }
        return requests;
    }

    public List<SupplyRequest> getRequestsByBarangay(String barangayDisplayName) throws SQLException {
        List<SupplyRequest> requests = new ArrayList<>();
        // FIXED: Filter using the joined user's display name
        String sql = """
            SELECT sr.*, u.display_name as barangay_name 
            FROM supply_requests sr 
            JOIN users u ON sr.requesting_user_id = u.id 
            WHERE u.display_name = ? 
            ORDER BY sr.created_at DESC
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barangayDisplayName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) requests.add(mapRow(rs));
            }
        }
        return requests;
    }

    public void updateStatus(long requestId, SupplyRequestStatus status) throws SQLException {
        String sql = "UPDATE supply_requests SET status = ? WHERE id = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, requestId);
            ps.executeUpdate();
        }
    }

    private SupplyRequest mapRow(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");

        // FIXED: Matches exactly 8 parameters. Grabs the barangay_name from our JOIN.
        return new SupplyRequest(
                rs.getLong("id"),
                rs.getObject("item_id") != null ? rs.getLong("item_id") : 0L,
                rs.getInt("quantity"),
                SupplyRequestStatus.fromDb(rs.getString("status")),
                rs.getString("barangay_name"),
                rs.getObject("requesting_user_id") != null ? rs.getLong("requesting_user_id") : null,
                rs.getString("notes"),
                created != null ? created.toLocalDateTime() : null
        );
    }
}