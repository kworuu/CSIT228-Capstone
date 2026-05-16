package com.example.dao;

import com.example.model.Transaction;
import com.example.util.DBConnectionManager;

import java.sql.*;

public class TransactionDao {

    public void recordTransaction(Transaction t) throws SQLException {
        String sql = """
            INSERT INTO transactions 
            (item_id, quantity, direction, destination_type, destination_id, user_id, notes) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, t.itemId());
            ps.setInt(2, t.quantity());
            ps.setString(3, t.direction());
            ps.setString(4, t.destinationType());

            if (t.destinationId() != null) ps.setLong(5, t.destinationId());
            else ps.setNull(5, Types.BIGINT);

            if (t.userId() != null) ps.setLong(6, t.userId());
            else ps.setNull(6, Types.BIGINT);

            ps.setString(7, t.notes());
            ps.executeUpdate();
        }
    }
}