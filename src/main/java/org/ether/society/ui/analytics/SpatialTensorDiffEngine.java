/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.scene.image.Image;
import org.ether.society.data.HistoricalMapGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.analytics.MapComparisonMetrics;
import org.ether.society.analytics.SimulationRunRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <h2>Spatial Tensor Diff Engine</h2>
 * <p>
 * High-performance computation engine for asynchronous multi-channel raster difference
 * evaluation, SSIM, Pearson spatial correlation, categorical overlap (Dice/Jaccard),
 * and empirical fidelity reporting across paired simulation runs.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public final class SpatialTensorDiffEngine {

    private static final Logger logger = LoggerFactory.getLogger(SpatialTensorDiffEngine.class);

    private SpatialTensorDiffEngine() {
        // Utility class
    }

    /**
     * Immutable bundle encapsulating computed 2D map comparison output.
     *
     * @param imgA JavaFX raster image for scenario A
     * @param imgB JavaFX raster image for scenario B
     * @param imgDiff JavaFX discrepancy heatmap Δ(A - B)
     * @param metrics statistical fidelity metrics
     * @param formattedReport human-readable analytical report
     */
    public record ComparisonResult(
            Image imgA,
            Image imgB,
            Image imgDiff,
            MapComparisonMetrics.MapComparisonResult metrics,
            String formattedReport
    ) {}

    /**
     * Asynchronously extracts, generates, and compares cartographic rasters for two scenarios at a target year.
     *
     * @param itemA selectable scenario item A
     * @param itemB selectable scenario item B
     * @param targetYear target temporal epoch in years AD/BP
     * @param channelKey canonical tensor channel name
     * @param runResolver resolver mapping items to historical snapshot records
     * @param rasterImageCache in-memory cache for buffered images
     * @param onProgress progress callback indicating total maps needed
     * @return CompletableFuture completing with {@link ComparisonResult}
     */
    public static CompletableFuture<ComparisonResult> compute2DComparisonAsync(
            ScenarioSelectableItem itemA,
            ScenarioSelectableItem itemB,
            int targetYear,
            String channelKey,
            Function<ScenarioSelectableItem, SimulationRunRecord> runResolver,
            Map<String, BufferedImage> rasterImageCache,
            Consumer<Integer> onProgress) {

        return CompletableFuture.supplyAsync(() -> {
            BufferedImage bufA = null;
            BufferedImage bufB = null;
            BufferedImage bufDiff = null;

            try {
                Scenario scA = itemA != null ? itemA.getScenario() : null;
                Scenario scB = itemB != null ? itemB.getScenario() : scA;
                String runIdA = itemA != null ? itemA.getRunId() : "N/A";
                String runIdB = itemB != null ? itemB.getRunId() : runIdA;

                SimulationRunRecord runA = runResolver != null ? runResolver.apply(itemA) : null;
                SimulationRunRecord runB = runResolver != null ? runResolver.apply(itemB) : null;

                List<H3Cell> snapshotCellsA = runA != null ? runA.getSpatialSnapshotAt(targetYear) : null;
                List<H3Cell> snapshotCellsB = runB != null ? runB.getSpatialSnapshotAt(targetYear) : null;

                String cacheKeyA = runIdA + "_" + targetYear + "_" + channelKey;
                String cacheKeyB = runIdB + "_" + targetYear + "_" + channelKey;

                int mapsToGenerate = 0;
                if (!rasterImageCache.containsKey(cacheKeyA)) mapsToGenerate++;
                if (!rasterImageCache.containsKey(cacheKeyB) && !cacheKeyA.equals(cacheKeyB)) mapsToGenerate++;

                if (onProgress != null && mapsToGenerate > 0) {
                    onProgress.accept(mapsToGenerate);
                }

                // 1. Channel A Raster Extraction with In-Memory Cache
                bufA = rasterImageCache.get(cacheKeyA);
                if (bufA == null) {
                    File diskRasterA = SpatialTensorDiskCatalog.getDiskRasterForChannel(targetYear, channelKey);
                    if ("HISTORICAL_GROUND_TRUTH".equals(runIdA) && diskRasterA != null && diskRasterA.exists()) {
                        String filePathKey = "FILE_" + diskRasterA.getAbsolutePath();
                        bufA = rasterImageCache.get(filePathKey);
                        if (bufA == null) {
                            bufA = ImageIO.read(diskRasterA);
                            if (bufA != null) rasterImageCache.put(filePathKey, bufA);
                        }
                    } else if (snapshotCellsA != null && !snapshotCellsA.isEmpty()) {
                        bufA = SpatialTensorRasterRenderer.rasterizeCellsToImage(snapshotCellsA, channelKey, 512, 256);
                    } else if (diskRasterA != null && diskRasterA.exists()) {
                        String filePathKey = "FILE_" + diskRasterA.getAbsolutePath();
                        bufA = rasterImageCache.get(filePathKey);
                        if (bufA == null) {
                            bufA = ImageIO.read(diskRasterA);
                            if (bufA != null) rasterImageCache.put(filePathKey, bufA);
                        }
                    } else if ("density".equals(channelKey) && scA != null) {
                        bufA = HistoricalMapGenerator.rasterizeDensityMapForYear(scA.getPopulationDensityType(), scA, targetYear);
                    } else if (scA != null) {
                        HistoricalMapGenerator.populateScenarioHistoricalMaps(scA);
                        bufA = SpatialTensorRasterRenderer.base64ToBufferedImage(SpatialTensorDiskCatalog.extractChannelBase64(scA, channelKey));
                    }
                    if (bufA != null) {
                        rasterImageCache.put(cacheKeyA, bufA);
                    }
                }

                // 2. Channel B Raster Extraction with In-Memory Cache
                if ("HISTORICAL_GROUND_TRUTH".equals(runIdA) && "HISTORICAL_GROUND_TRUTH".equals(runIdB) && bufA != null) {
                    bufB = bufA;
                } else {
                    bufB = rasterImageCache.get(cacheKeyB);
                    if (bufB == null) {
                        File diskRasterB = SpatialTensorDiskCatalog.getDiskRasterForChannel(targetYear, channelKey);
                        if ("HISTORICAL_GROUND_TRUTH".equals(runIdB) && diskRasterB != null && diskRasterB.exists()) {
                            String filePathKey = "FILE_" + diskRasterB.getAbsolutePath();
                            bufB = rasterImageCache.get(filePathKey);
                            if (bufB == null) {
                                bufB = ImageIO.read(diskRasterB);
                                if (bufB != null) rasterImageCache.put(filePathKey, bufB);
                            }
                        } else if (snapshotCellsB != null && !snapshotCellsB.isEmpty()) {
                            int w = SpatialTensorRasterRenderer.computeOptimalRasterWidth(snapshotCellsB.size());
                            bufB = SpatialTensorRasterRenderer.rasterizeCellsToImage(snapshotCellsB, channelKey, w, w / 2);
                        } else if (diskRasterB != null && diskRasterB.exists()) {
                            String filePathKey = "FILE_" + diskRasterB.getAbsolutePath();
                            bufB = rasterImageCache.get(filePathKey);
                            if (bufB == null) {
                                bufB = ImageIO.read(diskRasterB);
                                if (bufB != null) rasterImageCache.put(filePathKey, bufB);
                            }
                        } else if ("density".equals(channelKey) && scB != null) {
                            bufB = HistoricalMapGenerator.rasterizeDensityMapForYear(scB.getPopulationDensityType(), scB, targetYear);
                        } else if (scB != null) {
                            HistoricalMapGenerator.populateScenarioHistoricalMaps(scB);
                            bufB = SpatialTensorRasterRenderer.base64ToBufferedImage(SpatialTensorDiskCatalog.extractChannelBase64(scB, channelKey));
                        }
                        if (bufB != null) {
                            rasterImageCache.put(cacheKeyB, bufB);
                        }
                    }
                }

                if (bufA != null && bufB != null) {
                    bufDiff = MapComparisonMetrics.generateDiscrepancyHeatmap(bufA, bufB);
                }
            } catch (Exception ex) {
                logger.warn("Could not generate 2D comparison maps for target year {}: {}", targetYear, ex.getMessage());
            }

            final Image imgFxA = SpatialTensorRasterRenderer.bufferedImageToFxImage(bufA);
            final Image imgFxB = SpatialTensorRasterRenderer.bufferedImageToFxImage(bufB);
            final Image imgFxDiff = SpatialTensorRasterRenderer.bufferedImageToFxImage(bufDiff);

            final MapComparisonMetrics.MapComparisonResult metrics = (bufA != null && bufB != null)
                    ? MapComparisonMetrics.compareImages(bufA, bufB) : null;

            final String report = (metrics != null)
                    ? formatFidelityReport(metrics, targetYear, channelKey)
                    : I18n.getOrDefault("analytics.status.tensor_unavailable", "⚠️ Cartographic tensor data unavailable for the selected epoch and channel.");

            return new ComparisonResult(imgFxA, imgFxB, imgFxDiff, metrics, report);
        });
    }

    /**
     * Formats spatial comparison metrics into a structured markdown/plaintext report.
     *
     * @param metrics comparison result
     * @param targetYear target year
     * @param channelKey channel name
     * @return formatted report string
     */
    public static String formatFidelityReport(MapComparisonMetrics.MapComparisonResult metrics, int targetYear, String channelKey) {
        if (metrics == null) return "";
        String pearsonEval = metrics.getPearsonR() >= 0.85
                ? "✅ (" + I18n.getOrDefault("analytics.spatial.high_fidelity", "High Fidelity") + ")"
                : "⚠️ (" + I18n.getOrDefault("analytics.spatial.divergence_detected", "Divergence Detected") + ")";

        return String.format(
                "📊 %s (Year %d AD - %s)\n" +
                "------------------------------------------------------------------------\n" +
                "• %s : %.4f\n" +
                "• %s : %.4f %s\n" +
                "• %s : %.4f\n" +
                "• %s : %.4f\n" +
                "• %s : %.4f\n" +
                "• %s : %.4f\n" +
                "• %s : %.4f (%s)\n" +
                "------------------------------------------------------------------------\n" +
                "%s",
                I18n.getOrDefault("analytics.spatial.metrics_header", "2D CARTOGRAPHIC FIDELITY METRICS"),
                targetYear,
                channelKey,
                I18n.getOrDefault("analytics.spatial.rmse", "Spatial Root Mean Square Error (RMSE)"), metrics.getRmse(),
                I18n.getOrDefault("analytics.spatial.pearson", "Spatial Pearson Correlation (r)"), metrics.getPearsonR(), pearsonEval,
                I18n.getOrDefault("analytics.spatial.ssim", "2D Structural Similarity (SSIM)"), metrics.getSsim(),
                I18n.getOrDefault("analytics.spatial.jaccard", "Categorical Jaccard Overlap Index"), metrics.getJaccardIndex(),
                I18n.getOrDefault("analytics.spatial.dice", "Dice Overlap Coefficient"), metrics.getDiceCoefficient(),
                I18n.getOrDefault("analytics.spatial.kl", "Spatial Entropy KL Divergence"), metrics.getKlDivergence(),
                I18n.getOrDefault("analytics.spatial.max_delta", "Maximum Spatial Discrepancy"), metrics.getMaxDeltaValue(),
                String.format(I18n.getOrDefault("analytics.spatial.coords_fmt", "Lat: %.2f°, Lng: %.2f°"), metrics.getMaxDeltaLat(), metrics.getMaxDeltaLng()),
                metrics.getFormattedReport()
        );
    }
}
