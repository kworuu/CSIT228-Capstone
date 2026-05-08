package com.example.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton manager for CivicGuard's MariaDB database connection.
 *
 * <p>Loads connection settings from the {@code db.properties} file on the
 * classpath exactly once. Subsequent calls reuse the cached configuration.
 * Each call to {@link #getConnection()} returns a fresh JDBC connection that
 * the caller is responsible for closing — typically via try-with-resources.</p>
 *
 * <p>Implements the Singleton design pattern with thread-safe lazy
 * initialization. The class is declared {@code final} to prevent subclassing,
 * and the constructor is private to prevent external instantiation.</p>
 *
 * <p>Configuration keys expected in {@code db.properties}:</p>
 * <ul>
 *   <li>{@code db.host} — hostname or IP of the MariaDB server</li>
 *   <li>{@code db.port} — usually 3306</li>
 *   <li>{@code db.name} — database name (civicguard)</li>
 *   <li>{@code db.user} — database username</li>
 *   <li>{@code db.password} — database password</li>
 * </ul>
 *
 * @author CivicGuard team
 * @since 1.0
 */
public final class DBConnectionManager {

    /** Singleton instance, lazily initialized on first access. */
    private static DBConnectionManager instance;

    /** JDBC connection URL, built once during construction. */
    private final String jdbcUrl;

    /** Database username from configuration. */
    private final String username;

    /** Database password from configuration. */
    private final String password;

    /**
     * Private constructor — loads {@code db.properties} from the classpath
     * and constructs the JDBC URL. Called exactly once via {@link #getInstance()}.
     *
     * @throws IllegalStateException if {@code db.properties} is missing or unreadable
     */
    private DBConnectionManager() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "db.properties not found on classpath. " +
                                "Ensure it lives at src/main/resources/db.properties");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read db.properties", e);
        }

        String host = requireProperty(props, "db.host");
        String port = requireProperty(props, "db.port");
        String name = requireProperty(props, "db.name");
        this.username = requireProperty(props, "db.user");
        this.password = requireProperty(props, "db.password");

        this.jdbcUrl = String.format(
                "jdbc:mariadb://%s:%s/%s?useUnicode=true&characterEncoding=utf8",
                host, port, name);
    }

    /**
     * Returns the singleton instance, creating it if needed.
     * Thread-safe via synchronized block — multiple threads attempting
     * to initialize concurrently will see only one instance created.
     *
     * @return the singleton {@code DBConnectionManager}
     */
    public static synchronized DBConnectionManager getInstance() {
        if (instance == null) {
            instance = new DBConnectionManager();
        }
        return instance;
    }

    /**
     * Opens a fresh JDBC connection to the database. The caller owns
     * the returned connection and is responsible for closing it.
     *
     * <p>Recommended usage with try-with-resources:</p>
     * <pre>{@code
     * try (Connection conn = DBConnectionManager.getInstance().getConnection();
     *      PreparedStatement stmt = conn.prepareStatement(sql)) {
     *     // ...
     * }
     * }</pre>
     *
     * @return an open {@link Connection} ready for use
     * @throws SQLException if the connection cannot be established
     *                      (e.g. wrong credentials, server down, network failure)
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /**
     * Helper to fetch a required property and fail fast if missing.
     */
    private static String requireProperty(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required property '" + key + "' missing from db.properties");
        }
        return value;
    }
}
