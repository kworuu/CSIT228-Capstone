package com.example.dao;

import com.example.model.User;
import com.example.model.UserRole;
import com.example.util.DBConnectionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRowToUser(rs);
            }
        }
        return null;
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY display_name";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) users.add(mapRowToUser(rs));
        }
        return users;
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        Double lat = rs.getObject("latitude") != null ? rs.getDouble("latitude") : null;
        Double lng = rs.getObject("longitude") != null ? rs.getDouble("longitude") : null;

        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp loginTs = rs.getTimestamp("last_login");

        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("display_name"),
                UserRole.valueOf(rs.getString("role").toUpperCase()),
                rs.getString("assigned_barangay"),
                lat,
                lng,
                createdTs != null ? createdTs.toLocalDateTime() : null,
                loginTs != null ? loginTs.toLocalDateTime() : null
        );
    }
}