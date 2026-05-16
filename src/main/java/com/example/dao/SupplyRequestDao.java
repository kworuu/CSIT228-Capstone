package com.example.dao;

import com.example.model.SupplyRequest;
import com.example.model.SupplyRequestItem;
import com.example.model.SupplyRequestStatus;
import com.example.util.DBConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplyRequestDao {

    /**
     * Saves a SupplyRequest and all its associated items in a single transaction.
     */
    public SupplyRequest saveRequestWithItems(SupplyRequest request) throws SQLException {
        String insertReqSql = "INSERT INTO supply_requests (requesting_barangay, requesting_user_id, evacuation_center_id, notes, status) VALUES (?, ?, ?, ?, ?)";
        String insertItemSql = "INSERT INTO supply_request_items (request_id, item_id, quantity_requested) VALUES (?, ?, ?)";

        Connection conn = DBConnectionManager.getInstance().getConnection();
        try {
            conn.setAutoCommit(false); // Start transaction

            // 1. Insert Header
            try (PreparedStatement psReq = conn.prepareStatement(insertReqSql, Statement.RETURN_GENERATED_KEYS)) {
                psReq.setString(1, request.getRequestingBarangay());
                psReq.setLong(2, request.getRequestingUserId());
                if (request.getEvacuationCenterId() != null) {
                    psReq.setLong(3, request.getEvacuationCenterId());
                } else {
                    psReq.setNull(3, Types.BIGINT);
                }
                psReq.setString(4, request.getNotes());
                psReq.setString(5, request.getStatus().toDb());

                psReq.executeUpdate();

                try (ResultSet rs = psReq.getGeneratedKeys()) {
                    if (rs.next()) request.setId(rs.getLong(1));
                    else throw new SQLException("Failed to retrieve generated ID for SupplyRequest");
                }
            }

            // 2. Insert Items
            try (PreparedStatement psItem = conn.prepareStatement(insertItemSql)) {
                for (SupplyRequestItem item : request.getItems()) {
                    psItem.setLong(1, request.getId());
                    psItem.setLong(2, item.getItemId());
                    psItem.setInt(3, item.getQuantityRequested());
                    psItem.addBatch();
                }
                psItem.executeBatch();
            }

            conn.commit(); // Commit transaction
            return request;
        } catch (SQLException e) {
            conn.rollback(); // Rollback on failure
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    /**
     * Retrieves all supply requests for a specific barangay (for the history table).
     */
    public List<SupplyRequest> findByBarangay(String barangayName) throws SQLException {
        List<SupplyRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM supply_requests WHERE requesting_barangay = ? ORDER BY created_at DESC";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barangayName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    requests.add(new SupplyRequest(
                            rs.getLong("id"),
                            rs.getString("requesting_barangay"),
                            rs.getLong("requesting_user_id"),
                            rs.getLong("evacuation_center_id") == 0 ? null : rs.getLong("evacuation_center_id"),
                            SupplyRequestStatus.fromDb(rs.getString("status")),
                            rs.getString("notes"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getLong("reviewed_by") == 0 ? null : rs.getLong("reviewed_by"),
                            rs.getTimestamp("reviewed_at") != null ? rs.getTimestamp("reviewed_at").toLocalDateTime() : null,
                            rs.getString("admin_notes")
                    ));
                }
            }
        }
        return requests;
    }

    /**
     * Retrieves all supply requests across the entire system (for the Admin dashboard).
     */
    public List<SupplyRequest> findAll() throws SQLException {
        List<SupplyRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM supply_requests ORDER BY created_at DESC";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                requests.add(new SupplyRequest(
                        rs.getLong("id"),
                        rs.getString("requesting_barangay"),
                        rs.getLong("requesting_user_id"),
                        rs.getLong("evacuation_center_id") == 0 ? null : rs.getLong("evacuation_center_id"),
                        SupplyRequestStatus.fromDb(rs.getString("status")),
                        rs.getString("notes"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getLong("reviewed_by") == 0 ? null : rs.getLong("reviewed_by"),
                        rs.getTimestamp("reviewed_at") != null ? rs.getTimestamp("reviewed_at").toLocalDateTime() : null,
                        rs.getString("admin_notes")
                ));
            }
        }
        return requests;
    }
}