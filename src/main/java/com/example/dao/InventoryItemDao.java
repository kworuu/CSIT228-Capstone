package com.example.dao;

import com.example.model.InventoryItem;
import com.example.util.DBConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InventoryItemDao {

    public List<InventoryItem> findAll() throws SQLException {
        List<InventoryItem> items = new ArrayList<>();
        String sql = "SELECT * FROM inventory_items ORDER BY name";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) items.add(mapRow(rs));
        }
        return items;
    }

    public void save(InventoryItem item) throws SQLException {
        String sql = "INSERT INTO inventory_items (name, category, unit, low_threshold, critical_threshold, stock_quantity, created_by_user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.name());
            ps.setString(2, item.category());
            ps.setString(3, item.unit());
            ps.setInt(4, item.lowThreshold());
            ps.setInt(5, item.criticalThreshold());
            ps.setInt(6, item.stockQuantity());
            if (item.createdByUserId() != null) ps.setLong(7, item.createdByUserId());
            else ps.setNull(7, Types.BIGINT);
            ps.executeUpdate();
        }
    }

    public static int getAdminCriticalCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM inventory_items WHERE stock_quantity <= critical_threshold";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public List<InventoryItem> findCriticalItemsByUser(Long userId) throws SQLException {
        List<InventoryItem> items = new ArrayList<>();
        String sql = "SELECT * FROM inventory_items WHERE stock_quantity <= low_threshold ORDER BY stock_quantity ASC";
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) items.add(mapRow(rs));
        }
        return items;
    }

    private InventoryItem mapRow(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        Long createdBy = rs.getObject("created_by_user_id") != null ? rs.getLong("created_by_user_id") : null;

        return new InventoryItem(
                rs.getLong("id"), rs.getString("name"), rs.getString("category"),
                rs.getString("unit"), rs.getInt("low_threshold"), rs.getInt("critical_threshold"),
                rs.getInt("stock_quantity"), createdBy
        );
    }

    public List<InventoryItem> findByCategory(String category) throws SQLException {
        List<InventoryItem> items = new ArrayList<>();
        String sql = "SELECT * FROM inventory_items WHERE category = ? ORDER BY name";

        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, category);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }
        return items;
    }
}