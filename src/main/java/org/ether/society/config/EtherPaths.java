/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvère Martin-Michiellot
 */
package org.ether.society.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Centralized Path Configuration Manager for Ether.
 *
 * Implements 12-Factor App principles: all directory paths are configurable
 * via environment variables with intelligent fallback defaults suited for both
 * local desktop execution (with user home storage) and Docker container execution
 * (with mounted volume storage).
 *
 * Environment variables:
 * - ETHER_DATA_DIR         : Read-only factory data (presets, maps, events, history)
 * - ETHER_SAVES_DIR        : Mutable simulation save files and cluster snapshots
 * - ETHER_LOGS_DIR         : Mutable runtime logs
 * - ETHER_USER_PRESETS_DIR : Mutable user-created custom presets and scenarios
 * - ETHER_CACHE_DIR        : Ephemeral tile and network caches
 */
public final class EtherPaths {
    private static final Logger logger = LoggerFactory.getLogger(EtherPaths.class);

    // Environment variable names
    public static final String ENV_DATA_DIR = "ETHER_DATA_DIR";
    public static final String ENV_SAVES_DIR = "ETHER_SAVES_DIR";
    public static final String ENV_LOGS_DIR = "ETHER_LOGS_DIR";
    public static final String ENV_USER_PRESETS_DIR = "ETHER_USER_PRESETS_DIR";
    public static final String ENV_CACHE_DIR = "ETHER_CACHE_DIR";

    private static Path dataDir;
    private static Path savesDir;
    private static Path logsDir;
    private static Path userPresetsDir;
    private static Path cacheDir;

    static {
        initPaths();
    }

    private EtherPaths() {
        // Utility class
    }

    /**
     * Initializes all paths from environment variables or sensible defaults.
     */
    public static synchronized void initPaths() {
        // 1. Data directory (Factory maps, presets, events)
        String envData = System.getenv(ENV_DATA_DIR);
        if (envData != null && !envData.isBlank()) {
            dataDir = Paths.get(envData).toAbsolutePath().normalize();
        } else {
            // Check if local ./data exists, otherwise fallback to project root data
            Path localData = Paths.get("data").toAbsolutePath().normalize();
            dataDir = localData;
        }

        // 2. Saves directory (Persistent simulation saves)
        String envSaves = System.getenv(ENV_SAVES_DIR);
        if (envSaves != null && !envSaves.isBlank()) {
            savesDir = Paths.get(envSaves).toAbsolutePath().normalize();
        } else {
            // Default: project ./saves directory for convenience and Docker compatibility
            savesDir = Paths.get("saves").toAbsolutePath().normalize();
        }

        // 3. Logs directory
        String envLogs = System.getenv(ENV_LOGS_DIR);
        if (envLogs != null && !envLogs.isBlank()) {
            logsDir = Paths.get(envLogs).toAbsolutePath().normalize();
        } else {
            logsDir = Paths.get("logs").toAbsolutePath().normalize();
        }

        // 4. User presets directory (User custom scenarios and planet presets)
        String envUserPresets = System.getenv(ENV_USER_PRESETS_DIR);
        if (envUserPresets != null && !envUserPresets.isBlank()) {
            userPresetsDir = Paths.get(envUserPresets).toAbsolutePath().normalize();
        } else {
            String userHome = System.getProperty("user.home", ".");
            userPresetsDir = Paths.get(userHome, ".ether_society", "user_presets").toAbsolutePath().normalize();
        }

        // 5. Ephemeral cache directory
        String envCache = System.getenv(ENV_CACHE_DIR);
        if (envCache != null && !envCache.isBlank()) {
            cacheDir = Paths.get(envCache).toAbsolutePath().normalize();
        } else {
            String userHome = System.getProperty("user.home", ".");
            cacheDir = Paths.get(userHome, ".ether_society", "cache").toAbsolutePath().normalize();
        }

        ensureDirectories();
    }

    private static void ensureDirectories() {
        createDirSafely(dataDir, "Data");
        createDirSafely(savesDir, "Saves");
        createDirSafely(logsDir, "Logs");
        createDirSafely(userPresetsDir, "User Presets");
        createDirSafely(cacheDir, "Cache");

        // Ensure preset sub-directories exist in data directory
        createDirSafely(getPresetsPlanetsDir(), "Presets - Planets");
        createDirSafely(getPresetsEcologyDir(), "Presets - Ecology");
        createDirSafely(getPresetsScenariosDir(), "Presets - Scenarios");
        createDirSafely(getPresetsLeadersDir(), "Presets - Leaders");
    }

    private static void createDirSafely(Path path, String name) {
        if (path == null) return;
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            logger.warn("Could not create directory for {}: {} ({})", name, path, e.getMessage());
        }
    }

    // ── Root Paths ────────────────────────────────────────────────────────────

    public static Path getDataDir() {
        return dataDir;
    }

    public static Path getSavesDir() {
        return savesDir;
    }

    public static Path getLogsDir() {
        return logsDir;
    }

    public static Path getUserPresetsDir() {
        return userPresetsDir;
    }

    public static Path getCacheDir() {
        return cacheDir;
    }

    // ── Preset Sub-Directories ────────────────────────────────────────────────

    public static Path getPresetsDir() {
        return dataDir.resolve("presets");
    }

    public static Path getPresetsPlanetsDir() {
        return getPresetsDir().resolve("planets");
    }

    public static Path getPresetsEcologyDir() {
        return getPresetsDir().resolve("ecology");
    }

    public static Path getPresetsScenariosDir() {
        return getPresetsDir().resolve("scenarios");
    }

    public static Path getPresetsLeadersDir() {
        return getPresetsDir().resolve("leaders");
    }

    // ── Saves Sub-Directories ─────────────────────────────────────────────────

    public static Path getEnginesCompiledDir() {
        return savesDir.resolve(Paths.get("engines", "compiled"));
    }

    public static Path getClusterSnapshotsDir() {
        return savesDir.resolve("cluster_snapshots");
    }
}
