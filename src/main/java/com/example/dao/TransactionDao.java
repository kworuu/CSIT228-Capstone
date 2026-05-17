package com.example.dao;

import com.example.model.Transaction;
import com.example.util.DBConnectionManager;

import java.sql.*;

public class TransactionDao {

    public void recordTransaction(Transaction t) throws SQLException {
        // Query updated to perfectly mirror table: transactions (direction, item_id, quantity, destination_id, created_by, notes)
        String sql = """
            INSERT INTO transactions 
            (direction, item_id, quantity, destination_id, created_by, notes) 
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, t.direction().toLowerCase());
            ps.setLong(2, t.itemId());
            ps.setInt(3, t.quantity());

            // Check if destination_id is null before binding
            if (t.destinationId() != null) {
                ps.setLong(4, t.destinationId());
            } else {
                ps.setNull(4, Types.BIGINT);
            }

            ps.setString(5, t.createdBy());
            ps.setString(6, t.notes());

            ps.executeUpdate();
        }
    }
}