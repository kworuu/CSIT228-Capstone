package com.example.auth;

import com.example.dao.BarangayDao;
import com.example.dao.UserDao;
import com.example.model.Barangay;
import com.example.model.User;
import com.example.model.UserRole;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service-layer authentication. Wraps {@link UserDao} and {@link BarangayDao}
 * with bcrypt password verification and session establishment.
 *
 * <p>This class is the <i>only</i> place in the codebase that compares
 * passwords. Controllers and DAOs never touch plaintext.</p>
 *
 * <p>Error handling philosophy: return a {@link LoginResult} for any
 * expected failure (bad credentials, missing barangay, wrong role).
 * Only truly exceptional cases — DB unreachable, etc. — propagate as
 * {@link SQLException}, and the caller decides how to surface them.</p>
 */
public class AuthService {

    private final UserDao userDao = new UserDao();
    private final BarangayDao barangayDao = new BarangayDao();

    /** Generic message used for all credential failures — never leak which field was wrong. */
    private static final String BAD_CREDENTIALS = "Invalid username or password";

    /**
     * Admin login. Accepts a username + password, verifies the bcrypt hash,
     * confirms the role is ADMIN, and populates the SessionContext.
     */
    public LoginResult loginAsAdmin(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return LoginResult.failure(BAD_CREDENTIALS);
        }

        Optional<User> opt = userDao.findByUsername(username.trim());
        if (opt.isEmpty()) return LoginResult.failure(BAD_CREDENTIALS);

        User user = opt.get();

        if (user.getRole() != UserRole.ADMIN) {
            return LoginResult.failure("This account is not an admin account.");
        }

        if (!verifyPassword(password, user.getPasswordHash())) {
            return LoginResult.failure(BAD_CREDENTIALS);
        }

        // Success — update last login and start session (no barangay for admin)
        userDao.updateLastLogin(user.getId(), LocalDateTime.now());
        SessionContext.set(user, null);
        return LoginResult.success();
    }

    /**
     * Barangay login. The user types the <b>barangay name</b> (not a username);
     * we resolve the matching user row via the {@code assigned_barangay} column.
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Look up the barangay by name → confirms it exists.</li>
     *   <li>Find the user where {@code role='barangay'} AND
     *       {@code assigned_barangay = name}.</li>
     *   <li>Verify bcrypt hash.</li>
     *   <li>Populate SessionContext with both user and barangay.</li>
     * </ol>
     */
    public LoginResult loginAsBarangay(String barangayName, String password) throws SQLException {
        if (barangayName == null || barangayName.isBlank() || password == null || password.isEmpty()) {
            return LoginResult.failure(BAD_CREDENTIALS);
        }

        String name = barangayName.trim();

        Optional<Barangay> brgyOpt = barangayDao.findByName(name);
        if (brgyOpt.isEmpty()) {
            return LoginResult.failure(BAD_CREDENTIALS);
        }
        Barangay barangay = brgyOpt.get();

        Optional<User> userOpt = findBarangayUser(name);
        if (userOpt.isEmpty()) {
            return LoginResult.failure(BAD_CREDENTIALS);
        }
        User user = userOpt.get();

        if (!user.getRole().isBarangayRole()) {
            return LoginResult.failure(BAD_CREDENTIALS);
        }

        if (!verifyPassword(password, user.getPasswordHash())) {
            return LoginResult.failure(BAD_CREDENTIALS);
        }

        userDao.updateLastLogin(user.getId(), LocalDateTime.now());
        SessionContext.set(user, barangay);
        return LoginResult.success();
    }

    /** Logs out the current user. */
    public void logout() {
        SessionContext.clear();
    }

    // ── Helpers ─────────────────────────────────────────────────

    /**
     * Bcrypt verify. Wraps the underlying call so we can swallow malformed-hash
     * exceptions and return false (treat as failed login rather than a 500).
     */
    private boolean verifyPassword(String plaintext, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) return false;
        // Defensive: if the placeholder hash from V001 is still in place,
        // refuse the login rather than throwing.
        if (!storedHash.startsWith("$2")) return false;
        try {
            return BCrypt.checkpw(plaintext, storedHash);
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    /**
     * Looks up the user row for a given barangay name.
     *
     * <p>This is a direct JDBC query rather than going through UserDao
     * because UserDao currently has no {@code findByAssignedBarangay}
     * method, and adding one would expand its public surface for a
     * single caller. If a second consumer emerges, promote this to
     * UserDao and remove the SQL here.</p>
     */
    private Optional<User> findBarangayUser(String barangayName) throws SQLException {
        String sql =
                "SELECT id, username, password_hash, email, display_name, role, " +
                        "       assigned_center_id, created_at, last_login_at " +
                        "FROM users " +
                        "WHERE assigned_barangay = ? AND role IN ('barangay','staff') " +
                        "LIMIT 1";

        try (var conn = com.example.util.DBConnectionManager.getInstance().getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, barangayName);
            try (var rs = stmt.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                Long assignedCenterId = rs.getLong("assigned_center_id");
                if (rs.wasNull()) assignedCenterId = null;

                var createdTs = rs.getTimestamp("created_at");
                var lastLoginTs = rs.getTimestamp("last_login_at");

                User u = new User(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        UserRole.fromDb(rs.getString("role")),
                        assignedCenterId,
                        createdTs == null ? null : createdTs.toLocalDateTime(),
                        lastLoginTs == null ? null : lastLoginTs.toLocalDateTime()
                );
                return Optional.of(u);
            }
        }
    }
}