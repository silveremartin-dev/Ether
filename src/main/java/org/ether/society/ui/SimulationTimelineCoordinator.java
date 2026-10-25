/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.ether.society.core.H3SimulationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;

/**
 * Coordinates simulation timeline interactions, snapshot seeking, timelapse recording,
 * and asynchronous time-stepping operations between UI controls and the simulation engine.
 */
public class SimulationTimelineCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(SimulationTimelineCoordinator.class);

    private final H3SimulationEngine engine;
    private final ControlPanel controlPanel;
    private final Runnable refreshViewsCallback;

    private boolean isRecording = false;
    private final AtomicBoolean isAsyncStepping = new AtomicBoolean(false);

    public SimulationTimelineCoordinator(H3SimulationEngine engine, ControlPanel controlPanel, Runnable refreshViewsCallback) {
        this.engine = engine;
        this.controlPanel = controlPanel;
        this.refreshViewsCallback = refreshViewsCallback;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void toggleTimelapseRecording() {
        if (engine == null) return;
        isRecording = !isRecording;
        if (isRecording) {
            logger.info("Timelapse recording STARTED");
            engine.getHistoryManager().captureWorldSnapshot(engine);
        } else {
            logger.info("Timelapse recording STOPPED");
            var snapshots = engine.getHistoryManager().getWorldSnapshots();
            if (!snapshots.isEmpty() && controlPanel != null) {
                int minYear = (int) (long) snapshots.firstKey();
                int maxYear = (int) (long) snapshots.lastKey();
                controlPanel.updateTimelapseSlider(minYear, maxYear, maxYear);
            }
        }
    }

    public void seekToStartSafe() {
        if (engine == null) return;
        engine.pause();
        if (controlPanel != null) {
            controlPanel.updatePlayPauseVisuals(false);
        }
        var snapshots = engine.getHistoryManager() != null ? engine.getHistoryManager().getWorldSnapshots() : null;
        if (snapshots != null && !snapshots.isEmpty()) {
            long firstTick = snapshots.firstKey();
            applySeekToTick(firstTick);
        } else {
            applySeekToTick(0);
        }
        logger.info("Rewind to start completed");
    }

    public void seekToEndSafe() {
        if (engine == null) return;
        engine.pause();
        if (controlPanel != null) {
            controlPanel.updatePlayPauseVisuals(false);
        }
        var snapshots = engine.getHistoryManager() != null ? engine.getHistoryManager().getWorldSnapshots() : null;
        if (snapshots != null && !snapshots.isEmpty()) {
            long lastTick = snapshots.lastKey();
            applySeekToTick(lastTick);
        } else {
            long endYear = engine.getCurrentScenario() != null ? engine.getCurrentScenario().getEndDateYear() : 2100;
            seekTimelapse((int) endYear);
            return;
        }
        logger.info("Seek to end completed");
    }

    public void stepBackwardSafe(boolean fast) {
        if (engine == null) return;
        engine.pause();
        if (controlPanel != null) {
            controlPanel.updatePlayPauseVisuals(false);
        }
        long curTick = engine.getTickCounter();
        var snapshots = engine.getHistoryManager() != null ? engine.getHistoryManager().getWorldSnapshots() : null;
        if (snapshots != null && !snapshots.isEmpty()) {
            int jumps = fast ? 5 : 1;
            Long targetTick = curTick;
            for (int i = 0; i < jumps; i++) {
                Long lower = snapshots.lowerKey(targetTick);
                if (lower != null) {
                    targetTick = lower;
                } else {
                    break;
                }
            }
            if (targetTick != null && !targetTick.equals(curTick)) {
                applySeekToTick(targetTick);
                return;
            }
        }
        long stepSize = fast ? 100 : 10;
        long target = Math.max(0, curTick - stepSize);
        applySeekToTick(target);
    }

    public void stepForwardSafe(boolean fast) {
        if (engine == null) return;
        engine.pause();
        if (controlPanel != null) {
            controlPanel.updatePlayPauseVisuals(false);
        }

        long curTick = engine.getTickCounter();
        var snapshots = engine.getHistoryManager() != null ? engine.getHistoryManager().getWorldSnapshots() : null;
        if (snapshots != null && !snapshots.isEmpty()) {
            int jumps = fast ? 5 : 1;
            Long targetTick = curTick;
            for (int i = 0; i < jumps; i++) {
                Long higher = snapshots.higherKey(targetTick);
                if (higher != null) {
                    targetTick = higher;
                } else {
                    break;
                }
            }
            if (targetTick != null && !targetTick.equals(curTick)) {
                applySeekToTick(targetTick);
                return;
            }
        }

        // If no future snapshot exists, calculate forward asynchronously to never freeze the JavaFX EDT
        if (!isAsyncStepping.compareAndSet(false, true)) {
            logger.warn("Step forward already in progress, skipping duplicate request");
            return;
        }

        int ticksToRun = fast ? 100 : 10;
        CompletableFuture.runAsync(() -> {
            try {
                engine.stepForward(ticksToRun);
            } catch (Exception ex) {
                logger.error("Error during asynchronous step forward", ex);
            } finally {
                isAsyncStepping.set(false);
                Platform.runLater(() -> {
                    engine.pause();
                    if (controlPanel != null) {
                        controlPanel.updatePlayPauseVisuals(false);
                    }
                    if (refreshViewsCallback != null) {
                        refreshViewsCallback.run();
                    }
                });
            }
        });
    }

    public void applySeekToTick(long tick) {
        if (engine == null) return;
        engine.pause();
        engine.seekToTick(tick);
        if (refreshViewsCallback != null) {
            refreshViewsCallback.run();
        }
    }

    public void seekTimelapse(int year) {
        if (engine == null) return;
        long startYear = engine.getCurrentScenario() != null ? engine.getCurrentScenario().getStartDateYear() : -20000;
        long targetTicks = Math.max(0, (year - startYear) * 12);
        applySeekToTick(targetTicks);
        logger.info("Timelapse seek to year: {} (tick {})", year, targetTicks);
    }
}
