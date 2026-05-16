package com.example.dao;

import com.example.model.ActivityTimelineItem;
import com.example.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ActivityTimelineDao {

    public List<ActivityTimelineItem> getBarangayTimeline(String barangayName) throws SQLException {
        List<ActivityTimelineItem> timeline = new ArrayList<>();

        String sql = """
            SELECT t.created_at AS event_date, 
                   UPPER(t.direction) AS action_type, 
                   CONCAT('Recorded ', t.quantity, ' items') AS target_desc,
                   ec.name AS center_name,
                   u.display_name AS user_name
            FROM transactions t
            JOIN evacuation_centers ec ON t.destination_id = ec.id AND t.destination_type = 'EVACUATION_CENTER'
            JOIN users u ON t.user_id = u.id
            WHERE ec.barangay = ?
            
            UNION ALL
            
            SELECT sr.created_at AS event_date, 
                   'REQUEST' AS action_type, 
                   CONCAT('Submitted supply request (Status: ', UPPER(sr.status), ')') AS target_desc,
                   'Barangay LGU' AS center_name,
                   u.display_name AS user_name
            FROM supply_requests sr
            JOIN users u ON sr.requesting_user_id = u.id
            WHERE sr.requesting_barangay = ?
            
            ORDER BY event_date DESC
            LIMIT 50
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, barangayName);
            ps.setString(2, barangayName);

            try (ResultSet rs = ps.executeQuery()) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
                while (rs.next()) {
                    String formattedDate = rs.getTimestamp("event_date").toLocalDateTime().format(dtf);
                    timeline.add(new ActivityTimelineItem(
                            formattedDate,
                            rs.getString("action_type"),
                            rs.getString("target_desc"),
                            rs.getString("center_name"),
                            rs.getString("user_name")
                    ));
                }
            }
        }
        return timeline;
    }
}