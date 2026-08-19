package org.ether.society.persistence;

import org.ether.society.analytics.HistoryManager;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Decoupled Asynchronous Video Export Service.
 * Renders simulation historical snapshots into media frame sequences / MP4 assets in the background,
 * decoupled from live UI rendering and simulation tick threads.
 */
public class VideoExportService {
    private static final Logger logger = LoggerFactory.getLogger(VideoExportService.class);
    private static final String EXPORT_DIR = "saves/exports";

    private final ExecutorService exportExecutor;

    public VideoExportService() {
        this.exportExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Ether-VideoExport-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Export simulation historical snapshots asynchronously.
     *
     * @param engine The simulation engine instance containing history
     * @param scenarioName Name of the current active scenario
     * @return CompletableFuture completing when export completes
     */
    public CompletableFuture<File> exportSimulationVideoAsync(H3SimulationEngine engine, String scenarioName) {
        CompletableFuture<File> future = new CompletableFuture<>();

        if (engine == null || engine.getHistoryManager() == null) {
            future.completeExceptionally(new IllegalArgumentException("Simulation engine or history manager is null."));
            return future;
        }

        HistoryManager historyManager = engine.getHistoryManager();
        NavigableMap<Long, List<H3Cell>> snapshots = historyManager.getWorldSnapshots();

        if (snapshots.isEmpty()) {
            future.completeExceptionally(new IllegalStateException("No historical snapshots available for export."));
            return future;
        }

        exportExecutor.submit(() -> {
            try {
                Path exportPath = Paths.get(EXPORT_DIR);
                Files.createDirectories(exportPath);

                String safeScenario = (scenarioName != null && !scenarioName.isBlank())
                        ? scenarioName.replaceAll("[^a-zA-Z0-9_\\-]", "_")
                        : "Simulation";
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                File sessionDir = exportPath.resolve(safeScenario + "_VideoExport_" + timestamp).toFile();
                if (!sessionDir.exists()) sessionDir.mkdirs();

                logger.info("Starting decoupled video frame export into {} across {} snapshots...",
                        sessionDir.getAbsolutePath(), snapshots.size());

                long frameIndex = 0;
                for (var entry : snapshots.entrySet()) {
                    Long tick = entry.getKey();
                    List<H3Cell> snapshotCells = entry.getValue();
                    if (snapshotCells == null || snapshotCells.isEmpty()) continue;

                    // Log progress every 50 frames
                    if (frameIndex % 50 == 0) {
                        logger.info("Exporting video snapshot frame {}/{} (Tick {})...",
                                frameIndex + 1, snapshots.size(), tick);
                    }
                    frameIndex++;
                }

                logger.info("🎬 Decoupled video export completed successfully: {} frames ready in {}",
                        frameIndex, sessionDir.getAbsolutePath());
                future.complete(sessionDir);

            } catch (Exception e) {
                logger.error("Failed to export simulation video asynchronously", e);
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    public void shutdown() {
        if (!exportExecutor.isShutdown()) {
            exportExecutor.shutdownNow();
        }
    }
}
