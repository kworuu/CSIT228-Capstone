package com.example.dao;

import com.example.model.User;
import com.example.model.UserRole;
import com.example.util.DBConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE display_name = ?";
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
        Integer zoom = rs.getObject("zoom") != null ? rs.getInt("zoom") : null;

        Timestamp loginTs = rs.getTimestamp("last_login_at");

        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("display_name"),
                UserRole.valueOf(rs.getString("role").toUpperCase()),
                lat,
                lng,
                zoom,
                loginTs != null ? loginTs.toLocalDateTime() : null
        );
    }
}