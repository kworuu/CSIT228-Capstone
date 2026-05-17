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
            -- PART 1: Admin Deployments (Transactions)
            SELECT t.created_at AS event_date,
                   'SUPPLY DISPATCHED' AS action_type,
                   CONCAT(t.quantity, 'x ', i.name) AS target_desc,
                   'Central LGU (Admin)' AS center_name,
                   u.display_name AS user_name
            FROM transactions t
            JOIN users u ON t.destination_id = u.id    
            JOIN inventory_items i ON t.item_id = i.id 
            WHERE u.display_name = ?
            
            UNION ALL
            
            -- PART 2: Barangay Supply Requests
            SELECT sr.created_at AS event_date,
                   CONCAT('REQUEST ', UPPER(sr.status)) AS action_type,
                   CONCAT(sr.quantity, 'x ', COALESCE(i.name, 'Unknown Item')) AS target_desc,
                   COALESCE(ec.name, 'Barangay General') AS center_name,
                   u_req.display_name AS user_name
            FROM supply_requests sr
            JOIN users u_req ON sr.requesting_user_id = u_req.id
            LEFT JOIN inventory_items i ON sr.item_id = i.id 
            LEFT JOIN evacuation_centers ec ON sr.target_center_id = ec.id
            WHERE u_req.display_name = ?
            
            ORDER BY event_date DESC
            LIMIT 50
            """;

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, barangayName);
            ps.setString(2, barangayName);

            try (ResultSet rs = ps.executeQuery()) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
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
