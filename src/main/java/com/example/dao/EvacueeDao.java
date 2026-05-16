package com.example.dao;

import com.example.dashboard_barangay.RosterModalController;
import com.example.model.Evacuee;
import com.example.util.DBConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvacueeDao {

    public void saveEvacuee(Evacuee evacuee) throws SQLException {
        String sql = """
            INSERT INTO evacuees 
            (full_name_enc, contact_enc, photo_path, evacuation_center_id, user_id, notes) 
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, evacuee.fullNameEnc());
            ps.setString(2, evacuee.contactEnc());
            ps.setString(3, evacuee.photoPath());
            ps.setLong(4, evacuee.evacuationCenterId());

            if (evacuee.userId() != null) ps.setLong(5, evacuee.userId());
            else ps.setNull(5, Types.BIGINT);

            ps.setString(6, evacuee.notes());
            ps.executeUpdate();
        }
    }

    public List<RosterModalController.RosterItem> getRosterByCenter(long centerId) throws SQLException {
        List<RosterModalController.RosterItem> roster = new ArrayList<>();
        String sql = "SELECT full_name_enc, created_at FROM evacuees WHERE evacuation_center_id = ? ORDER BY full_name_enc ASC";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, centerId);
            try (ResultSet rs = ps.executeQuery()) {
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy");
                while (rs.next()) {
                    String name = rs.getString("full_name_enc");
                    String date = rs.getTimestamp("created_at").toLocalDateTime().format(dtf);
                    roster.add(new RosterModalController.RosterItem(name, "REGISTERED", date));
                }
            }
        }
        return roster;
    }
}