/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.data.ImageMapLoader;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetHydrographyFlowHelper;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.h3.H3Service;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * <h1>ScenarioGridInitializer</h1>
 * <p>
 * Single unified headless pipeline constructing H3 hexagonal scenario meshes identically for both
 * CLI/Headless batch execution and JavaFX GUI launch.
 * </p>
 *
 * <h2>Unified Two-Stage Geospatial Pipeline</h2>
 * <ol>
 *   <li><b>Geometry &amp; Windowing</b>: Constructs pure H3 cellular topology via {@link H3Service}
 *       (full globe or regional bounding box), tagging clipped perimeter boundary cells.</li>
 *   <li><b>2048×1024 Raster Resolution &amp; Spherical H3 Aggregation</b>: Resolves authoritative $2048 \times 1024$
 *       rasters (empirical ETOPO/WorldClim/planetary archives, custom imported Base64 rasters, or
 *       upstream {@link ProceduralGenerator#synthesizeProceduralPlanetRasters procedural Simplex noise rasters}
 *       when no imported maps exist), then aggregates every raster uniformly onto the H3 mesh via
 *       {@link org.ether.society.data.H3RasterAggregator} and computes hydrography flow.</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ScenarioGridInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioGridInitializer.class);

    private ScenarioGridInitializer() {
        // Pure headless initializer
    }

    /**
     * Builds and initializes the H3 spatial grid for a scenario identically across GUI and headless runs.
     *
     * @param scenario         target scenario configuration
     * @param progressCallback optional normalized progress listener {@code (completed, total)}
     * @param cancelSupplier   optional cancellation predicate
     * @return initialized H3 cell list ready for {@link H3SimulationEngine#initializeFromScenario(Scenario, List)}
     */
    public static List<H3Cell> initializeCellsForScenario(
            Scenario scenario,
            BiConsumer<Integer, Integer> progressCallback,
            BooleanSupplier cancelSupplier) {
        if (scenario == null) {
            throw new IllegalArgumentException("scenario must not be null");
        }

        int h3Res = scenario.getH3Resolution() > 0 ? scenario.getH3Resolution() : 3;
        PlanetPreset basePreset = scenario.getPlanetPreset() != null
                ? scenario.getPlanetPreset()
                : PlanetPreset.EARTH_LIKE;
        PlanetPreset effectivePreset = basePreset.withResolution(h3Res);

        if (progressCallback != null) progressCallback.accept(1, 10);

        // 1. Construct pure H3 geometry (global or regional bounding box)
        List<H3Cell> cells;
        if (scenario.isClippingEnabled()) {
            double minLat = scenario.getMinLat();
            double maxLat = scenario.getMaxLat();
            double minLng = scenario.getMinLng();
            double maxLng = scenario.getMaxLng();
            cells = H3Service.getInstance().generateRegionalMetadata(h3Res, minLat, maxLat, minLng, maxLng);
            double marginLat = Math.max(1.0, (maxLat - minLat) * 0.08);
            double marginLng = Math.max(1.0, (maxLng - minLng) * 0.08);
            for (H3Cell c : cells) {
                if (c.getLatitude() <= minLat + marginLat || c.getLatitude() >= maxLat - marginLat
                        || c.getLongitude() <= minLng + marginLng || c.getLongitude() >= maxLng - marginLng) {
                    c.setBoundaryCell(true);
                }
            }
            logger.info("Regional H3 Res {} mesh built with {} cells (bounds [{},{}] x [{},{}]).",
                    h3Res, cells.size(), minLat, maxLat, minLng, maxLng);
        } else {
            cells = H3Service.getInstance().generateGlobalMetadata(h3Res);
            logger.info("Global H3 Res {} mesh built with {} cells.", h3Res, cells.size());
        }

        if (cancelSupplier != null && cancelSupplier.getAsBoolean()) {
            return cells;
        }
        if (progressCallback != null) progressCallback.accept(3, 10);

        // 2. Resolve 2048x1024 rasters (empirical / custom Base64 / upstream procedural Simplex synthesis)
        BufferedImage elevImg = decodeBase64Png(effectivePreset.customElevBase64());
        BufferedImage biomeImg = decodeBase64Png(effectivePreset.customBiomeBase64());
        BufferedImage tempImg = decodeBase64Png(effectivePreset.customClimateBase64());
        BufferedImage rainImg = decodeBase64Png(effectivePreset.customRainfallBase64());
        BufferedImage seasonImg = decodeBase64Png(effectivePreset.customSeasonalityBase64());

        String planetKey = resolveCanonicalPlanetKey(effectivePreset);
        long targetYear = scenario.getStartDateYear();
        boolean useEmpiricalOrImported = effectivePreset.elevationUseImport()
                || "earth".equals(planetKey)
                || !planetKey.isBlank();

        if (elevImg == null && useEmpiricalOrImported && isKnownPlanetaryArchive(planetKey)) {
            elevImg = ImageMapLoader.loadMapBufferedImage(planetKey, targetYear, "elevation.png");
            if (biomeImg == null) biomeImg = ImageMapLoader.loadMapBufferedImage(planetKey, targetYear, "biomes.png");
            if (tempImg == null) tempImg = ImageMapLoader.loadMapBufferedImage(planetKey, targetYear, "temperature.png");
            if (rainImg == null) rainImg = ImageMapLoader.loadMapBufferedImage(planetKey, targetYear, "precipitation.png");
            if (seasonImg == null) seasonImg = ImageMapLoader.loadMapBufferedImage(planetKey, targetYear, "seasonality.png");
        }

        if (elevImg == null) {
            // Procedural planet: synthesize standard 2048x1024 rasters upstream before uniform H3 aggregation
            logger.info("Synthesizing upstream 2048x1024 procedural Simplex rasters for preset '{}'...", effectivePreset.name());
            ProceduralGenerator.ProceduralRasterSuite suite =
                    ProceduralGenerator.getInstance().synthesizeProceduralPlanetRasters(effectivePreset, 2048, 1024);
            elevImg = suite.elevation();
            if (biomeImg == null) biomeImg = suite.biomes();
            if (tempImg == null) tempImg = suite.temperature();
            if (rainImg == null) rainImg = suite.rainfall();
            if (seasonImg == null) seasonImg = suite.seasonality();
        }

        if (cancelSupplier != null && cancelSupplier.getAsBoolean()) {
            return cells;
        }
        if (progressCallback != null) progressCallback.accept(7, 10);

        // 3. Aggregate 2048x1024 rasters uniformly onto H3 cells via spherical H3RasterAggregator
        new ImageMapLoader().mapBufferedImagesToCells(
                cells, elevImg, biomeImg, null, tempImg, rainImg, seasonImg,
                effectivePreset.minAltitudeMeters(), effectivePreset.maxAltitudeMeters());

        if (cancelSupplier != null && cancelSupplier.getAsBoolean()) {
            return cells;
        }
        if (progressCallback != null) progressCallback.accept(9, 10);

        // 4. Compute deterministic hydrography flow & basin accumulation
        PlanetHydrographyFlowHelper.accumulateHydrographyFlow(cells, effectivePreset, cancelSupplier);

        if (progressCallback != null) progressCallback.accept(10, 10);
        return cells;
    }

    private static String resolveCanonicalPlanetKey(PlanetPreset preset) {
        if (preset == null) return "earth";
        String canonical = preset.getCanonicalPlanet();
        if (canonical != null && !canonical.isBlank()) return canonical.toLowerCase(Locale.ROOT);
        String src = preset.elevationMapSource() != null ? preset.elevationMapSource().toLowerCase(Locale.ROOT) : "";
        String nm = preset.name() != null ? preset.name().toLowerCase(Locale.ROOT) : "";
        if (src.equals("earth") || nm.contains("terre") || nm.contains("earth")) return "earth";
        if (src.equals("mars") || nm.contains("mars") || nm.contains("ares")) return "mars";
        if (src.equals("venus") || nm.contains("venus") || nm.contains("vénus") || nm.contains("hesperos")) return "venus";
        if (src.equals("moon") || nm.contains("lune") || nm.contains("moon") || nm.contains("selene")) return "moon";
        if (src.equals("mercury") || nm.contains("mercure") || nm.contains("mercury") || nm.contains("hermes")) return "mercury";
        if (src.equals("titan") || nm.contains("titan")) return "titan";
        return "";
    }

    private static boolean isKnownPlanetaryArchive(String planetKey) {
        return "earth".equals(planetKey) || "mars".equals(planetKey) || "venus".equals(planetKey)
                || "moon".equals(planetKey) || "mercury".equals(planetKey) || "titan".equals(planetKey);
    }

    private static BufferedImage decodeBase64Png(String b64) {
        if (b64 == null || b64.isBlank()) return null;
        try {
            String payload = b64.contains(",") ? b64.substring(b64.indexOf(',') + 1) : b64;
            return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(payload.trim())));
        } catch (Exception e) {
            return null;
        }
    }
}
