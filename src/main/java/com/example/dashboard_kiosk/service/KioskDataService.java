package com.example.dashboard_kiosk.service;

import com.example.dashboard_kiosk.KioskConstants;
import com.example.dashboard_kiosk.model.EvacuationSite;
import com.example.model.EvacueeRecord;
import com.example.util.CenterEvent;
import com.example.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class KioskDataService {

    // ── Singleton ──────────────────────────────────────────────────────────

    private static final KioskDataService INSTANCE = new KioskDataService();

    private KioskDataService() { /* singleton */ }

    public static KioskDataService getInstance() { return INSTANCE; }

    // ── SQL ────────────────────────────────────────────────────────────────

    private static final String SQL_LOAD_CENTERS = """
            SELECT ec.id,
                   ec.name,
                   ec.address,
                   u.display_name AS barangay,
                   ec.created_at,
                   ec.latitude,
                   ec.longitude
              FROM evacuation_centers ec
              JOIN users u ON ec.user_id = u.id
             ORDER BY ec.name ASC
            """;

    private static final String SQL_LOAD_EVACUEES = """
            SELECT e.id,
                   e.full_name_enc,
                   ec.name           AS center_name,
                   u.display_name    AS barangay,
                   e.created_at
              FROM evacuees e
              JOIN evacuation_centers ec ON e.evacuation_center_id = ec.id
              JOIN users u               ON ec.user_id = u.id
             ORDER BY e.created_at DESC
             LIMIT 200
            """;

    private static final String SQL_LOAD_EVENTS = """
            SELECT csu.event_label,
                   csu.updated_at,
                   ec.name,
                   ec.id
              FROM center_status_updates csu
              JOIN evacuation_centers ec ON csu.center_id = ec.id
             ORDER BY csu.updated_at DESC
             LIMIT 15
            """;

    private static final String SQL_LOAD_EVACUEES_FOR_CENTER = """
            SELECT full_name_enc
              FROM evacuees
             WHERE evacuation_center_id = ?
             ORDER BY created_at DESC
             LIMIT 50
            """;

    // ── Public API ─────────────────────────────────────────────────────────

    public List<EvacuationSite> loadAllCenters() {
        List<EvacuationSite> sites = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LOAD_CENTERS);
             ResultSet rs        = ps.executeQuery()) {

            while (rs.next()) {
                sites.add(new EvacuationSite(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("name"),
                        rs.getString("address"),
                        rs.getString("barangay"),
                        "ACTIVE",
                        formatTimestamp(rs.getString("created_at")),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                ));
            }
        } catch (SQLException ex) {
            logError("loadAllCenters", ex);
            return Collections.emptyList();
        }
        return sites;
    }

    public List<EvacueeRecord> loadAllEvacuees() {
        List<EvacueeRecord> rows = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LOAD_EVACUEES);
             ResultSet rs        = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new EvacueeRecord(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("full_name_enc"),
                        rs.getString("center_name"),
                        rs.getString("barangay"),
                        formatTimestamp(rs.getString("created_at"))
                ));
            }
        } catch (SQLException ex) {
            logError("loadAllEvacuees", ex);
            return Collections.emptyList();
        }
        return rows;
    }

    public List<CenterEvent> loadRecentEvents() {
        List<CenterEvent> events = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LOAD_EVENTS);
             ResultSet rs        = ps.executeQuery()) {

            while (rs.next()) {
                events.add(new CenterEvent(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("event_label"),
                        formatTimestamp(rs.getString("updated_at"))
                ));
            }
        } catch (SQLException ex) {
            logError("loadRecentEvents", ex);
            return Collections.emptyList();
        }
        return events;
    }

    public List<String> loadEvacueeNamesForCenter(String centerId) {
        if (centerId == null || centerId.isBlank()) return Collections.emptyList();

        long id;
        try {
            id = Long.parseLong(centerId);
        } catch (NumberFormatException ex) {
            logError("loadEvacueeNamesForCenter[parse]", ex);
            return Collections.emptyList();
        }

        List<String> names = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LOAD_EVACUEES_FOR_CENTER)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("full_name_enc"));
                }
            }
        } catch (SQLException ex) {
            logError("loadEvacueeNamesForCenter[query]", ex);
            return Collections.emptyList();
        }
        return names;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    public static String formatTimestamp(String raw) {
        if (raw == null) return "—";
        try {
            LocalDateTime dt = LocalDateTime.parse(
                    raw.replace(" ", "T").substring(0, 19));
            return dt.format(KioskConstants.FULL_FORMATTER);
        } catch (Exception ex) {
            return raw;
        }
    }

    private static void logError(String op, Exception ex) {
        System.err.println(KioskConstants.LOG_DB_ERROR + op + " — " + ex.getMessage());
    }
}