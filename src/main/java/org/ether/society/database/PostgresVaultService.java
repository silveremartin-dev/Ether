package org.ether.society.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Base64;

/**
 * Service managing PostgreSQL Upload and Download of simulation scenarios,
 * pre-cached maps, cultural tensors, and historical snapshots.
 */
public class PostgresVaultService {
    private static final Logger logger = LoggerFactory.getLogger(PostgresVaultService.class);

    /* Internal state variable for db url (String). */
    private final String dbUrl;
    /* Internal state variable for user (String). */
    private final String user;
    /* Internal state variable for password (String). */
    private final String password;

    /*
     * Postgres vault service.
     * Enforces physical invariants and updates associated state variables within {@code PostgresVaultService}.
     *
     * @param dbUrl the db url parameter (String)
     * @param user the user parameter (String)
     * @param password the password parameter (String)
     */
    public PostgresVaultService(String dbUrl, String user, String password) {
        this.dbUrl = dbUrl;
        this.user = user;
        this.password = password;
    }

    /*
     * Initialize schema.
     * Enforces physical invariants and updates associated state variables within {@code PostgresVaultService}.
     *
     * @return the resulting computation or state reference
     */
    public boolean initializeSchema() {
        String sqlScenarios = "CREATE TABLE IF NOT EXISTS ether_scenarios (" +
                "id SERIAL PRIMARY KEY, " +
                "scenario_key VARCHAR(100) UNIQUE NOT NULL, " +
                "name VARCHAR(255) NOT NULL, " +
                "year_tag BIGINT NOT NULL, " +
                "parameters_json TEXT NOT NULL" +
                ");";

        String sqlMaps = "CREATE TABLE IF NOT EXISTS ether_scenario_maps (" +
                "id SERIAL PRIMARY KEY, " +
                "filename VARCHAR(255) UNIQUE NOT NULL, " +
                "size_bytes BIGINT NOT NULL, " +
                "image_base64 TEXT NOT NULL" +
                ");";

        try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlScenarios);
            stmt.execute(sqlMaps);
            logger.info("PostgreSQL Ether Vault schema successfully initialized.");
            return true;
        } catch (SQLException e) {
            logger.error("Failed to initialize PostgreSQL schema: {}", e.getMessage());
            return false;
        }
    }

    /*
     * Upload map to vault.
     * Enforces physical invariants and updates associated state variables within {@code PostgresVaultService}.
     *
     * @param filename the filename parameter (String)
     * @param pngFile the png file parameter (File)
     * @return the resulting computation or state reference
     */
    public boolean uploadMapToVault(String filename, File pngFile) throws IOException {
        if (!pngFile.exists()) {
            logger.warn("Map file {} does not exist for upload.", pngFile.getAbsolutePath());
            return false;
        }

        byte[] bytes = new byte[(int) pngFile.length()];
        try (FileInputStream fis = new FileInputStream(pngFile)) {
            fis.read(bytes);
        }
        String b64 = Base64.getEncoder().encodeToString(bytes);

        String sql = "INSERT INTO ether_scenario_maps (filename, size_bytes, image_base64) VALUES (?, ?, ?) " +
                "ON CONFLICT (filename) DO UPDATE SET image_base64 = EXCLUDED.image_base64, size_bytes = EXCLUDED.size_bytes;";

        try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, filename);
            pstmt.setLong(2, pngFile.length());
            pstmt.setString(3, b64);
            pstmt.executeUpdate();
            logger.info("Uploaded map '{}' ({} bytes) to PostgreSQL Vault.", filename, pngFile.length());
            return true;
        } catch (SQLException e) {
            logger.error("Failed to upload map '{}' to PostgreSQL: {}", filename, e.getMessage());
            return false;
        }
    }

    /*
     * Download map from vault.
     * Enforces physical invariants and updates associated state variables within {@code PostgresVaultService}.
     *
     * @param filename the filename parameter (String)
     * @param targetFile the target file parameter (File)
     * @return the resulting computation or state reference
     */
    public boolean downloadMapFromVault(String filename, File targetFile) {
        String sql = "SELECT image_base64 FROM ether_scenario_maps WHERE filename = ?;";

        try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, filename);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String b64 = rs.getString("image_base64");
                    byte[] bytes = Base64.getDecoder().decode(b64);
                    if (targetFile.getParentFile() != null) {
                        targetFile.getParentFile().mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                        fos.write(bytes);
                    }
                    logger.info("Downloaded map '{}' from PostgreSQL Vault to {}", filename, targetFile.getAbsolutePath());
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to download map '{}' from PostgreSQL: {}", filename, e.getMessage());
        }
        return false;
    }
}
