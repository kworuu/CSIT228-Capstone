package com.example.dao;

import com.example.model.CenterStatus;
import com.example.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class CenterStatusDao {

    public void save(CenterStatus status) throws SQLException {
        String sql = "INSERT INTO center_statuses (center_id, structural_status, structural_notes, event, reported_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, status.centerId());
            ps.setString(2, status.structuralStatus());
            ps.setString(3, status.structuralNotes());
            ps.setString(4, status.event());
            ps.setTimestamp(5, Timestamp.valueOf(status.reportedAt()));
            ps.executeUpdate();
        }
    }
}
