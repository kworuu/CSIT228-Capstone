package com.example.util;

import com.example.dao.EvacuationCenterDao;
import com.example.model.EvacuationCenter;

import java.sql.SQLException;
import java.util.List;

/**
 * Smoke test verifying the DAO layer works against the real database.
 */
public class DBConnectionSmokeTest {

    public static void main(String[] args) {
        System.out.println("CivicGuard — DAO smoke test");
        System.out.println("===========================");

        EvacuationCenterDao dao = new EvacuationCenterDao();

        try {
            // Test 1: findAll
            System.out.println("\nTest 1: findAll()");
            List<EvacuationCenter> all = dao.findAll();
            System.out.println("  Loaded " + all.size() + " centers");
            for (EvacuationCenter c : all) {
                System.out.println("  - " + c + " (capacity " + c.getCapacity() + ")");
            }

            // Test 2: findById
            System.out.println("\nTest 2: findById(1)");
            dao.findById(1L).ifPresentOrElse(
                    c -> System.out.println("  Found: " + c),
                    () -> System.out.println("  Not found")
            );

            // Test 3: findAllActive
            System.out.println("\nTest 3: findAllActive()");
            List<EvacuationCenter> active = dao.findAllActive();
            System.out.println("  " + active.size() + " active centers");

            System.out.println("\nDAO smoke test successful.");

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}