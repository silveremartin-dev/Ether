/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvère Martin-Michiellot
 */
package org.ether.society.data.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.ether.society.config.EtherPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <h1>GitHub Content Synchronizer Service</h1>
 * <p>
 * High-performance, non-blocking asynchronous synchronizer for official Ether assets:
 * <ul>
 *   <li>Scenario, planet, and ecology presets ({@code data/presets/**})</li>
 *   <li>Historical events and timeline chronicles ({@code data/events/**}, {@code data/history/**})</li>
 *   <li>Processed planetary cartographic rasters ({@code data/maps/ether/**})</li>
 *   <li>Official baseline scenario checkpoints and snapshots ({@code saves/**})</li>
 * </ul>
 * </p>
 *
 * <h2>Non-Destructive Branching &amp; Conflict Protection Invariants:</h2>
 * <ol>
 *   <li><b>User Customization Preservation</b>: If a user modifies an existing scenario, changes parameters
 *       (e.g., H3 resolution, cohort size, time step, active theories), modifies a map, or creates a custom
 *       scenario, the local file's SHA-256 will diverge from the sync manifest baseline. The synchronizer
 *       strictly detects this divergence and <b>NEVER overwrites</b> user modifications.</li>
 *   <li><b>User Saves &amp; Snapshots Isolation</b>: User-created simulation saves, branched checkpoints, and custom
 *       trajectories in {@code saves/} are never modified, replaced, or deleted.</li>
 *   <li><b>Zero-Blocking Runtime</b>: All fetch and synchronization operations run on a background daemon worker
 *       without stalling the JavaFX UI or headless CLI simulation loops.</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class GitHubContentSyncService {
    private static final Logger logger = LoggerFactory.getLogger(GitHubContentSyncService.class);

    private static final String DEFAULT_REPO = "silveremartin-dev/Ether";
    private static final String DEFAULT_BRANCH = "main";
    private static final String MANIFEST_FILE_NAME = ".sync_manifest.json";

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Ether-GitHubContentSyncThread");
        t.setDaemon(true);
        return t;
    });

    private static final AtomicBoolean syncInProgress = new AtomicBoolean(false);
    private static final List<SyncEventListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Listener interface for synchronization lifecycle events and new scenario discovery.
     */
    public interface SyncEventListener {
        default void onSyncStarted() {}
        default void onFileSynced(String relativePath) {}
        default void onScenarioDiscovered(String scenarioName) {}
        default void onSyncCompleted(int filesDownloaded, int filesChecked) {}
        default void onSyncFailed(String reason) {}
    }

    private GitHubContentSyncService() {
        // Utility singleton
    }

    /**
     * Registers an event listener for synchronization updates.
     *
     * @param listener the listener to attach
     */
    public static void addListener(SyncEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes an attached sync event listener.
     *
     * @param listener the listener to detach
     */
    public static void removeListener(SyncEventListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Initiates asynchronous background synchronization against the official GitHub repository.
     * Guaranteed non-blocking and safe to call during application startup.
     *
     * @return CompletableFuture completing with the number of newly downloaded/updated files.
     */
    public static CompletableFuture<Integer> startBackgroundSync() {
        if (!syncInProgress.compareAndSet(false, true)) {
            logger.info("GitHub content synchronization already running in background.");
            return CompletableFuture.completedFuture(0);
        }

        return CompletableFuture.supplyAsync(() -> {
            int downloadedCount = 0;
            try {
                notifyStarted();
                String repo = resolveRepository();
                String branch = resolveBranch();
                logger.info("Starting background GitHub content synchronization against repo='{}', branch='{}'...", repo, branch);

                Map<String, ManifestEntry> localManifest = loadSyncManifest();
                List<RemoteFileCandidate> remoteFiles = discoverRemoteFiles(repo, branch);

                if (remoteFiles.isEmpty()) {
                    logger.debug("No remote files discovered or GitHub API unreachable. Offline mode active.");
                    notifyCompleted(0, 0);
                    return 0;
                }

                int checked = 0;
                for (RemoteFileCandidate candidate : remoteFiles) {
                    checked++;
                    boolean downloaded = processRemoteCandidate(repo, branch, candidate, localManifest);
                    if (downloaded) {
                        downloadedCount++;
                        notifyFileSynced(candidate.path());
                        if (candidate.path().startsWith("data/presets/scenarios/")) {
                            notifyScenarioDiscovered(candidate.path());
                        }
                    }
                }

                saveSyncManifest(localManifest);
                logger.info("GitHub content synchronization completed: {} file(s) updated/downloaded ({} checked).",
                        downloadedCount, checked);
                notifyCompleted(downloadedCount, checked);
                return downloadedCount;
            } catch (Exception e) {
                logger.warn("GitHub content sync encountered a non-fatal network error (running offline): {}", e.getMessage());
                notifyFailed(e.getMessage());
                return downloadedCount;
            } finally {
                syncInProgress.set(false);
            }
        }, backgroundExecutor);
    }

    /**
     * Synchronously processes a single remote candidate file, applying non-destructive conflict checks.
     */
    private static boolean processRemoteCandidate(String repo, String branch, RemoteFileCandidate candidate,
                                                  Map<String, ManifestEntry> manifest) {
        String relPath = candidate.path();
        Path localPath = Paths.get(relPath);

        // Security check: ensure path does not escape project directory
        if (relPath.contains("..") || localPath.isAbsolute()) {
            logger.warn("Ignoring invalid remote path: {}", relPath);
            return false;
        }

        // Only sync authorized asset categories
        if (!isAuthorizedSyncPath(relPath)) {
            return false;
        }

        boolean existsLocally = Files.exists(localPath);

        if (!existsLocally) {
            // Case 1: Missing local file -> download directly
            return downloadRemoteFile(repo, branch, candidate, manifest);
        }

        // Case 2: File exists locally -> conflict check
        String currentLocalHash = computeSha256(localPath);
        ManifestEntry previousSync = manifest.get(relPath);

        if (previousSync == null) {
            // User-created or pre-existing unmanifested file: protect local version
            logger.debug("Local file '{}' not in sync manifest; preserving user local version.", relPath);
            return false;
        }

        if (previousSync.sha256() != null && !previousSync.sha256().equalsIgnoreCase(currentLocalHash)) {
            // User modified this file locally (hash diverged from last official sync)
            logger.info("Local modifications detected in '{}'; preserving user branch and skipping remote overwrite.", relPath);
            return false;
        }

        // Local file matches previous sync hash; check if remote has updated
        if (candidate.sha() != null && previousSync.remoteSha() != null
                && candidate.sha().equalsIgnoreCase(previousSync.remoteSha())) {
            // Remote unchanged
            return false;
        }

        // Official update available: download and update manifest
        return downloadRemoteFile(repo, branch, candidate, manifest);
    }

    /**
     * Validates whether a relative path falls within official sync categories.
     */
    public static boolean isAuthorizedSyncPath(String relPath) {
        String p = relPath.replace('\\', '/').toLowerCase();
        return p.startsWith("data/presets/")
                || p.startsWith("data/events/")
                || p.startsWith("data/history/")
                || p.startsWith("data/maps/ether/")
                || (p.startsWith("saves/") && isOfficialSavePath(p));
    }

    /**
     * Validates if a save path is an official scenario checkpoint.
     */
    private static boolean isOfficialSavePath(String p) {
        // Only sync official baseline runs (e.g. RUN-EPOCH_*, RUN-ALEXANDER_*, etc.)
        return p.startsWith("saves/run-") || p.startsWith("saves/baseline-");
    }

    /**
     * Downloads a remote file from GitHub raw CDN and updates the local sync manifest.
     */
    private static boolean downloadRemoteFile(String repo, String branch, RemoteFileCandidate candidate,
                                              Map<String, ManifestEntry> manifest) {
        String relPath = candidate.path();
        String rawUrl = "https://raw.githubusercontent.com/" + repo + "/" + branch + "/" + relPath;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(rawUrl))
                    .header("User-Agent", "Ether-Simulation-Engine/1.0.0-beta.2")
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                Path targetPath = Paths.get(relPath);
                if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                    Files.createDirectories(targetPath.getParent());
                }

                try (InputStream is = response.body();
                     OutputStream os = Files.newOutputStream(targetPath)) {
                    byte[] buffer = new byte[16384];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                }

                String newHash = computeSha256(targetPath);
                manifest.put(relPath, new ManifestEntry(newHash, candidate.sha(), Instant.now().toString(), targetPath.toFile().length()));
                logger.debug("Successfully downloaded official asset '{}' (SHA: {}).", relPath, newHash);
                return true;
            } else if (response.statusCode() == 404) {
                logger.debug("Remote asset '{}' not found on GitHub (HTTP 404).", relPath);
            } else {
                logger.warn("Failed to download '{}': HTTP status code {}", relPath, response.statusCode());
            }
        } catch (Exception e) {
            logger.warn("Exception downloading remote file '{}': {}", relPath, e.getMessage());
        }
        return false;
    }

    /**
     * Discovers remote file list via GitHub Tree API or fallbacks.
     */
    private static List<RemoteFileCandidate> discoverRemoteFiles(String repo, String branch) {
        List<RemoteFileCandidate> results = new ArrayList<>();
        String apiUrl = "https://api.github.com/repos/" + repo + "/git/trees/" + branch + "?recursive=1";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("User-Agent", "Ether-Simulation-Engine/1.0.0-beta.2")
                    .header("Accept", "application/vnd.github.v3+json")
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode treeNode = root.path("tree");
                if (treeNode.isArray()) {
                    for (JsonNode node : treeNode) {
                        String type = node.path("type").asText();
                        if ("blob".equalsIgnoreCase(type)) {
                            String path = node.path("path").asText();
                            String sha = node.path("sha").asText();
                            long size = node.path("size").asLong(0);
                            if (isAuthorizedSyncPath(path)) {
                                results.add(new RemoteFileCandidate(path, sha, size));
                            }
                        }
                    }
                }
            } else {
                logger.debug("GitHub API tree returned status {}, falling back to local scan.", response.statusCode());
            }
        } catch (Exception e) {
            logger.debug("GitHub API tree discovery failed: {}", e.getMessage());
        }
        return results;
    }

    /**
     * Computes the SHA-256 hash of a local file in uppercase hexadecimal.
     */
    public static String computeSha256(Path file) {
        if (file == null || !Files.exists(file)) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buffer = new byte[16384];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        } catch (Exception e) {
            logger.warn("Could not compute SHA-256 for file '{}': {}", file, e.getMessage());
            return "";
        }
    }

    /**
     * Loads the local synchronization manifest from disk.
     */
    private static Map<String, ManifestEntry> loadSyncManifest() {
        Path manifestPath = Paths.get("data", MANIFEST_FILE_NAME);
        if (!Files.exists(manifestPath)) {
            return new ConcurrentHashMap<>();
        }
        try {
            return mapper.readValue(manifestPath.toFile(), new TypeReference<ConcurrentHashMap<String, ManifestEntry>>() {});
        } catch (Exception e) {
            logger.warn("Could not parse sync manifest, initializing new: {}", e.getMessage());
            return new ConcurrentHashMap<>();
        }
    }

    /**
     * Persists the updated synchronization manifest to disk.
     */
    private static void saveSyncManifest(Map<String, ManifestEntry> manifest) {
        Path manifestPath = Paths.get("data", MANIFEST_FILE_NAME);
        try {
            if (manifestPath.getParent() != null && !Files.exists(manifestPath.getParent())) {
                Files.createDirectories(manifestPath.getParent());
            }
            mapper.writeValue(manifestPath.toFile(), manifest);
        } catch (Exception e) {
            logger.warn("Failed to persist sync manifest: {}", e.getMessage());
        }
    }

    private static String resolveRepository() {
        String prop = System.getProperty("ether.sync.repo");
        if (prop != null && !prop.isBlank()) return prop.trim();
        String env = System.getenv("ETHER_SYNC_REPO");
        if (env != null && !env.isBlank()) return env.trim();
        return DEFAULT_REPO;
    }

    private static String resolveBranch() {
        String prop = System.getProperty("ether.sync.branch");
        if (prop != null && !prop.isBlank()) return prop.trim();
        String env = System.getenv("ETHER_SYNC_BRANCH");
        if (env != null && !env.isBlank()) return env.trim();
        return DEFAULT_BRANCH;
    }

    // Listener notifications
    private static void notifyStarted() {
        for (SyncEventListener l : listeners) {
            try { l.onSyncStarted(); } catch (Throwable t) { logger.trace("Listener error", t); }
        }
    }

    private static void notifyFileSynced(String path) {
        for (SyncEventListener l : listeners) {
            try { l.onFileSynced(path); } catch (Throwable t) { logger.trace("Listener error", t); }
        }
    }

    private static void notifyScenarioDiscovered(String path) {
        String scenarioName = Paths.get(path).getFileName().toString().replace(".json", "");
        for (SyncEventListener l : listeners) {
            try { l.onScenarioDiscovered(scenarioName); } catch (Throwable t) { logger.trace("Listener error", t); }
        }
    }

    private static void notifyCompleted(int downloaded, int checked) {
        for (SyncEventListener l : listeners) {
            try { l.onSyncCompleted(downloaded, checked); } catch (Throwable t) { logger.trace("Listener error", t); }
        }
    }

    private static void notifyFailed(String reason) {
        for (SyncEventListener l : listeners) {
            try { l.onSyncFailed(reason); } catch (Throwable t) { logger.trace("Listener error", t); }
        }
    }

    /**
     * Remote candidate file record.
     */
    public record RemoteFileCandidate(String path, String sha, long size) {}

    /**
     * Manifest entry recording local and remote hash states.
     */
    public record ManifestEntry(String sha256, String remoteSha, String syncedAt, long sizeBytes) {}
}
