/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.config.EtherPaths;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * <h1>SpatialRasterFidelityAuditHelper</h1>
 * <p>
 * Evaluates spatial 2D raster fidelity between simulated final H3 cells and authentic empirical
 * raster ground truth on disk ({@code earth_<year>_density.png}) without external dependencies.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see CellularMapComparisonMetrics
 * @see MapComparisonMetrics
 */
public final class SpatialRasterFidelityAuditHelper {

    private static final Logger logger = LoggerFactory.getLogger(SpatialRasterFidelityAuditHelper.class);

    private SpatialRasterFidelityAuditHelper() {
        // Pure static helper
    }

    /**
     * Resolves the empirical ground-truth density raster for a scenario's horizon.
     *
     * @param scenario target scenario
     * @param horizonYear evaluation year (e.g., end year or start year)
     * @return loaded {@link BufferedImage} or {@code null} if absent
     */
    public static BufferedImage loadEmpiricalDensityRaster(Scenario scenario, long horizonYear) {
        if (scenario == null) return null;
        String planet = "earth";
        if (scenario.getPlanetPreset() != null && scenario.getPlanetPreset().getCanonicalPlanet() != null
                && !scenario.getPlanetPreset().getCanonicalPlanet().isBlank()) {
            planet = scenario.getPlanetPreset().getCanonicalPlanet().toLowerCase(Locale.ROOT);
        }

        Path basePlanetDir = EtherPaths.getEtherDir().resolve(planet);
        if (!Files.isDirectory(basePlanetDir)) {
            basePlanetDir = Paths.get("data", "maps", "ether", planet);
        }
        if (!Files.isDirectory(basePlanetDir)) {
            basePlanetDir = EtherPaths.getEarthDir();
            planet = "earth";
        }
        if (!Files.isDirectory(basePlanetDir)) {
            return null;
        }

        Path yearDir = basePlanetDir.resolve(String.valueOf(horizonYear));
        long resolvedYear = horizonYear;

        if (!Files.isDirectory(yearDir)) {
            try (Stream<Path> stream = Files.list(basePlanetDir)) {
                Optional<Long> closest = stream
                        .filter(Files::isDirectory)
                        .map(p -> {
                            try {
                                return Long.parseLong(p.getFileName().toString());
                            } catch (NumberFormatException nfe) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .min(Comparator.comparingLong(y -> Math.abs(y - horizonYear)));
                if (closest.isPresent()) {
                    resolvedYear = closest.get();
                    yearDir = basePlanetDir.resolve(String.valueOf(resolvedYear));
                }
            } catch (Exception e) {
                logger.debug("Failed to list map dirs in {}: {}", basePlanetDir, e.getMessage());
            }
        }

        if (!Files.isDirectory(yearDir)) {
            return null;
        }

        Path densityFile = yearDir.resolve(planet + "_" + resolvedYear + "_density.png");
        if (!Files.exists(densityFile)) {
            try (Stream<Path> stream = Files.list(yearDir)) {
                Optional<Path> anyDensity = stream
                        .filter(p -> p.getFileName().toString().endsWith("_density.png"))
                        .findFirst();
                if (anyDensity.isPresent()) {
                    densityFile = anyDensity.get();
                }
            } catch (Exception e) {
                logger.debug("Optional directory scan for density raster fallback failed: {}", e.getMessage());
            }
        }

        if (Files.exists(densityFile)) {
            try {
                return ImageIO.read(densityFile.toFile());
            } catch (Exception e) {
                logger.warn("Could not read empirical density raster from {}: {}", densityFile, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Directly audits the spatial 2D agreement between simulated cellular population and the disk raster.
     *
     * @param cells simulation cells at end of run
     * @param scenario scenario configuration
     * @param endYear actual target horizon year
     * @return comparison result with Pearson r, SSIM, RMSE, and Jaccard
     */
    public static MapComparisonMetrics.MapComparisonResult evaluateEndStateRasterFidelity(
            List<H3Cell> cells, Scenario scenario, long endYear) {
        if (cells == null || cells.isEmpty() || scenario == null) {
            return new MapComparisonMetrics.MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "Empty Inputs");
        }

        BufferedImage groundTruth = loadEmpiricalDensityRaster(scenario, endYear);
        if (groundTruth == null) {
            return new MapComparisonMetrics.MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "No Empirical Raster on Disk");
        }

        double minLat = scenario.getMinLat();
        double maxLat = scenario.getMaxLat();
        double minLng = scenario.getMinLng();
        double maxLng = scenario.getMaxLng();
        boolean windowed = scenario.isClippingEnabled();

        return MapComparisonMetrics.compareCellsToRaster(cells, groundTruth, minLat, maxLat, minLng, maxLng, windowed);
    }
}
