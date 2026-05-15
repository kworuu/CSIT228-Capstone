package com.example.util;

import com.example.dao.EvacuationCenterDao;
import com.example.dao.EvacueeDao;
import com.example.dao.InventoryItemDao;
import com.example.dao.TransactionDao;
import com.example.dao.UserDao;
import com.example.model.EvacuationCenter;
import com.example.model.InventoryItem;
import com.example.model.Transaction;
import com.example.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Smoke test for the full DAO layer.
 * Reads from every DAO to confirm column-to-field mapping is correct.
 * Does not write anything, so it's safe to run repeatedly.
 */
public class DBConnectionSmokeTest {

    public static void main(String[] args) {
        System.out.println("CivicGuard — DAO layer smoke test");
        System.out.println("==================================");

        try {
            testEvacuationCenters();
            testInventoryItems();
            testUsers();
            testEvacuees();
            testTransactions();

            System.out.println("\nAll DAO smoke tests passed.");
        } catch (SQLException e) {
            System.err.println("\nDatabase error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void testEvacuationCenters() throws SQLException {
        System.out.println("\n[1] EvacuationCenterDao");
        EvacuationCenterDao dao = new EvacuationCenterDao();
        List<EvacuationCenter> all = dao.findAll();
        System.out.println("  findAll() -> " + all.size() + " centers");
        for (EvacuationCenter c : all) {
            System.out.println("    - " + c );
        }
        System.out.println("  findAllActive() -> " + dao.findAllActive().size());
    }

    private static void testInventoryItems() throws SQLException {
        System.out.println("\n[2] InventoryItemDao");
        InventoryItemDao dao = new InventoryItemDao();
        List<InventoryItem> all = dao.findAll();
        System.out.println("  findAll() -> " + all.size() + " items");
        for (InventoryItem i : all) {
            System.out.println("    - " + i + " [crit=" + i.getCriticalThreshold()
                    + ", low=" + i.getLowThreshold() + "]");
        }
        System.out.println("  findByCategory(\"food\") -> " + dao.findByCategory("food").size());
    }

    private static void testUsers() throws SQLException {
        System.out.println("\n[3] UserDao");
        UserDao dao = new UserDao();
        List<User> all = dao.findAll();
        System.out.println("  findAll() -> " + all.size() + " users");
        for (User u : all) {
            System.out.println("    - " + u + " (role=" + u.getRole() + ")");
        }
        Optional<User> admin = dao.findByUsername("admin");
        System.out.println("  findByUsername(\"admin\") -> " +
                (admin.isPresent() ? "found id=" + admin.get().getId() : "NOT FOUND"));
    }

    private static void testEvacuees() throws SQLException {
        System.out.println("\n[5] EvacueeDao");
        EvacueeDao dao = new EvacueeDao();
        System.out.println("  findAll() -> " + dao.findAll().size());
        System.out.println("  findByCenter(1) -> " + dao.findByCenter(1L).size());
    }

    private static void testTransactions() throws SQLException {
        System.out.println("\n[6] TransactionDao");
        TransactionDao dao = new TransactionDao();
        List<Transaction> all = dao.findAll();
        System.out.println("  findAll() -> " + all.size());
        for (Transaction t : all) {
            System.out.println("    - " + t.getClass().getSimpleName()
                    + " #" + t.getId()
                    + " (item " + t.getItemId() + ", qty " + t.getQuantity()
                    + ", signed " + t.getSignedQuantity() + ")");
        }
        System.out.println("  findRecent(5) -> " + dao.findRecent(5).size());
        System.out.println("  findByDirection(\"inflow\") -> "
                + dao.findByDirection("inflow").size());
    }
}