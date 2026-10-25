/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.data.HistoricalMapGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static org.ether.society.ui.analytics.SpatialTensorDiskCatalog.RASTER_IMAGE_CACHE;

/**
 * Manages timeline scrubbing, asynchronous frame preloading, and automated interactive playback
 * for 2D spatial tensor comparisons.
 */
public class SpatialTensorTimelineAnimator {

    private static final Logger logger = LoggerFactory.getLogger(SpatialTensorTimelineAnimator.class);

    private final List<Integer> preloadedTimelineYears = new CopyOnWriteArrayList<>();
    private Timeline timelineAnimation;
    private boolean isPlayingAnimation = false;
    private int currentTimelineFrameIndex = 0;

    public boolean isPlayingAnimation() {
        return isPlayingAnimation;
    }

    public List<Integer> getPreloadedTimelineYears() {
        return preloadedTimelineYears;
    }

    public void toggleDateAnimation(Button playTimelineBtn, Slider dateSlider) {
        if (isPlayingAnimation) {
            stopAnimation(playTimelineBtn);
        } else {
            if (preloadedTimelineYears.isEmpty() && dateSlider != null) {
                preloadedTimelineYears.add((int) dateSlider.getValue());
            }
            isPlayingAnimation = true;
            if (playTimelineBtn != null) playTimelineBtn.setText(I18n.getOrDefault("analytics.btn.pause_timeline", "⏸️ Pause"));
            currentTimelineFrameIndex = 0;
            if (dateSlider != null && !preloadedTimelineYears.isEmpty()) {
                int curVal = (int) dateSlider.getValue();
                for (int i = 0; i < preloadedTimelineYears.size(); i++) {
                    if (preloadedTimelineYears.get(i) >= curVal) {
                        currentTimelineFrameIndex = i;
                        break;
                    }
                }
            }
            timelineAnimation = new Timeline(new KeyFrame(Duration.millis(350), evt -> {
                if (dateSlider != null && !preloadedTimelineYears.isEmpty()) {
                    currentTimelineFrameIndex = (currentTimelineFrameIndex + 1) % preloadedTimelineYears.size();
                    int targetYr = preloadedTimelineYears.get(currentTimelineFrameIndex);
                    dateSlider.setValue(targetYr);
                }
            }));
            timelineAnimation.setCycleCount(Timeline.INDEFINITE);
            timelineAnimation.play();
        }
    }

    public void stopAnimation(Button playTimelineBtn) {
        if (timelineAnimation != null) {
            timelineAnimation.stop();
        }
        isPlayingAnimation = false;
        if (playTimelineBtn != null) {
            playTimelineBtn.setText(I18n.getOrDefault("analytics.btn.play_timeline", "▶️ Play Timeline"));
        }
    }

    public void preloadAll2DTensorsAsync(ScenarioSelectableItem itemA, ScenarioSelectableItem itemB,
                                         int startYear, int endYear, String channelKey,
                                         Function<ScenarioSelectableItem, SimulationRunRecord> runResolver,
                                         Button playTimelineBtn, HBox mapRegenBox,
                                         ProgressBar mapRegenProgress, Label mapRegenStatusLabel) {
        if (itemA == null && itemB == null) return;
        if (playTimelineBtn != null) {
            playTimelineBtn.setDisable(true);
        }
        if (mapRegenBox != null) {
            mapRegenBox.setVisible(true);
            mapRegenBox.setManaged(true);
            mapRegenProgress.setProgress(0.0);
            mapRegenStatusLabel.setText(I18n.getOrDefault("analytics.status.preloading_frames", "⏳ Preloading 2D temporal frames..."));
        }

        preloadedTimelineYears.clear();
        CompletableFuture.runAsync(() -> {
            try {
                SimulationRunRecord runA = runResolver != null ? runResolver.apply(itemA) : null;
                SimulationRunRecord runB = runResolver != null ? runResolver.apply(itemB) : null;
                int step = Math.max(1, (endYear - startYear) / 12);

                int totalFrames = ((endYear - startYear) / step) + 1;
                int count = 0;

                for (int yr = startYear; yr <= endYear; yr += step) {
                    preloadedTimelineYears.add(yr);
                    count++;
                    final double progress = (double) count / (double) Math.max(1, totalFrames);
                    final int currentYr = yr;

                    preloadSingleYear(runA, itemA, currentYr, channelKey);
                    preloadSingleYear(runB, itemB, currentYr, channelKey);

                    Platform.runLater(() -> {
                        if (mapRegenProgress != null) mapRegenProgress.setProgress(progress);
                        if (mapRegenStatusLabel != null) {
                            mapRegenStatusLabel.setText(String.format("%s (Year %d)",
                                    I18n.getOrDefault("analytics.status.caching_frame", "Caching 2D Tensor Frame"), currentYr));
                        }
                    });
                }
            } catch (Exception ex) {
                logger.warn("Asynchronous 2D tensor frame preloading encountered an error: {}", ex.getMessage());
            } finally {
                Platform.runLater(() -> {
                    if (mapRegenBox != null) {
                        mapRegenBox.setVisible(false);
                        mapRegenBox.setManaged(false);
                    }
                    if (playTimelineBtn != null) {
                        playTimelineBtn.setDisable(false);
                    }
                });
            }
        });
    }

    private void preloadSingleYear(SimulationRunRecord run, ScenarioSelectableItem item, int yr, String channelKey) {
        if (item == null) return;
        String runId = item.getRunId() != null ? item.getRunId() : (run != null ? run.getRunId() : "DEFAULT");
        String cacheKey = "MAP_" + runId + "_" + yr + "_" + channelKey;
        if (!RASTER_IMAGE_CACHE.containsKey(cacheKey)) {
            try {
                List<H3Cell> snapshotCells = run != null ? run.getSpatialSnapshotAt(yr) : null;
                File diskRaster = SpatialTensorDiskCatalog.getDiskRasterForChannel(yr, channelKey);
                BufferedImage buf = null;
                if ("HISTORICAL_GROUND_TRUTH".equals(runId) && diskRaster != null && diskRaster.exists()) {
                    buf = ImageIO.read(diskRaster);
                } else if (snapshotCells != null && !snapshotCells.isEmpty()) {
                    buf = SpatialTensorRasterRenderer.rasterizeCellsToImage(snapshotCells, channelKey, 512, 256);
                } else if (diskRaster != null && diskRaster.exists()) {
                    buf = ImageIO.read(diskRaster);
                } else if (item.getScenario() != null) {
                    if ("density".equals(channelKey)) {
                        buf = HistoricalMapGenerator.rasterizeDensityMapForYear(item.getScenario().getPopulationDensityType(), item.getScenario(), yr);
                    } else {
                        HistoricalMapGenerator.populateScenarioHistoricalMaps(item.getScenario());
                        buf = SpatialTensorRasterRenderer.base64ToBufferedImage(SpatialTensorDiskCatalog.extractChannelBase64(item.getScenario(), channelKey));
                    }
                }
                if (buf != null) {
                    RASTER_IMAGE_CACHE.put(cacheKey, buf);
                }
            } catch (Exception ignored) {}
        }
    }
}
