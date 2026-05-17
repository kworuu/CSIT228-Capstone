package com.example.dao;

import com.example.model.SupplyRequest;
import com.example.model.SupplyRequestStatus;
import com.example.util.DBConnectionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SupplyRequestDao {

    public void saveRequest(SupplyRequest request) throws SQLException {
        String sql = """
                INSERT INTO supply_requests 
                (item_id, quantity, status, requesting_user_id, notes) 
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // item_id can be null when status only — your existing rows have null item_id
            if (request.itemId() > 0) {
                ps.setLong(1, request.itemId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }

            ps.setInt(2, request.quantity());
            ps.setString(3, request.status().name().toLowerCase());

            if (request.requestingUserId() != null) {
                ps.setLong(4, request.requestingUserId());
            } else {
                ps.setNull(4, Types.BIGINT);
            }

            ps.setString(5, request.notes());
            ps.executeUpdate();
        }
    }

    public List<SupplyRequest> findAll() throws SQLException {
        List<SupplyRequest> requests = new ArrayList<>();

        // LEFT JOIN inventory_items so rows with null item_id still appear
        String sql = """
                SELECT sr.*, 
                       u.display_name AS barangay_name, 
                       ii.name AS item_name
                FROM supply_requests sr
                JOIN users u ON sr.requesting_user_id = u.id
                LEFT JOIN inventory_items ii ON sr.item_id = ii.id
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

        String sql = """
                SELECT sr.*, 
                       u.display_name AS barangay_name, 
                       ii.name AS item_name
                FROM supply_requests sr
                JOIN users u ON sr.requesting_user_id = u.id
                LEFT JOIN inventory_items ii ON sr.item_id = ii.id
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
            ps.setString(1, status.name().toLowerCase());
            ps.setLong(2, requestId);
            ps.executeUpdate();
        }
    }

    private SupplyRequest mapRow(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
//        Timestamp updated = rs.getTimestamp("updated_at"); // Fetch updated_at

        // Pull item_name from JOIN; may be null when item_id was null OR item was deleted
        String itemName = rs.getString("item_name");
        if (itemName == null) {
            itemName = "—";  // dash placeholder when no item is linked
        }

        return new SupplyRequest(
                rs.getLong("id"),
                rs.getObject("item_id") != null ? rs.getLong("item_id") : 0L,
                itemName,
                rs.getInt("quantity"),
                SupplyRequestStatus.fromDb(rs.getString("status")),
                rs.getString("barangay_name"),
                rs.getObject("requesting_user_id") != null ? rs.getLong("requesting_user_id") : null,
                rs.getString("notes"),
                created != null ? created.toLocalDateTime() : null
                // Removed: updated != null ? updated.toLocalDateTime() : null // Pass updated_at
        );
    }
}
