package com.example.dao;

import com.example.model.Transaction;
import com.example.util.DBConnectionManager;

import java.sql.*;
import java.util.List;

public class TransactionDao {
    public boolean hasTransactions(long itemId) throws SQLException {
        String sql = "SELECT 1 FROM transactions WHERE item_id = ? LIMIT 1";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Returns true if a record is found
            }
        }
    }

    public List<Transaction> findAll() throws SQLException {
        List<Transaction> transactions = new java.util.ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Safely check if item_id is NULL in the database
                long itemId = rs.getObject("item_id") != null ? rs.getLong("item_id") : 0L;

                transactions.add(new Transaction(
                        rs.getLong("id"),
                        rs.getString("direction"),
                        itemId, // Pass the nullable Long instead of rs.getLong()
                        rs.getInt("quantity"),
                        rs.getObject("destination_id") != null ? rs.getLong("destination_id") : null,
                        rs.getString("created_by"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getString("notes")
                ));
            }
        }
        return transactions;
    }


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
