/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.cli;

import org.ether.society.config.EtherPaths;
import org.ether.society.data.EtopoGeoTiffReader;
import org.ether.society.data.HistoricalInstitutionalRasterizer;
import org.ether.society.data.HistoricalMapGenerator;
import org.ether.society.data.Hyde34GridReader;
import org.ether.society.data.Hyde34LandUseReader;
import org.ether.society.data.PaleoClimateMapRasterizer;
import org.ether.society.data.PaleolithicDensityGenerator;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Utility CLI that deterministically regenerates all Earth cartographic raster layers whose
 * construction algorithms were upgraded (ETOPO conservative elevation, WorldClim 5-arc-minute
 * conservative temperature/precipitation/seasonality, HYDE 3.4 conservative population density,
 * land-use-coupled Anthrome biomes, and HYDE land-use ecological footprint).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class RegenerateModifiedRastersCli {
    private static final Logger logger = LoggerFactory.getLogger(RegenerateModifiedRastersCli.class);

    private RegenerateModifiedRastersCli() {
        // Static CLI entry point
    }

    public static void main(String[] args) throws Exception {
        long t0 = System.currentTimeMillis();
        Path candidateDir = EtherPaths.getEarthDir();
        if (!Files.isDirectory(candidateDir)) {
            candidateDir = Path.of("data/maps/ether/earth");
        }
        if (!Files.isDirectory(candidateDir)) {
            throw new IllegalStateException("Earth raster root directory not found: " + candidateDir);
        }
        final Path earthDir = candidateDir;

        // 1. Discover all epoch year directories
        List<Long> epochYears = new ArrayList<>();
        try (Stream<Path> stream = Files.list(earthDir)) {
            stream.filter(Files::isDirectory).forEach(p -> {
                try {
                    epochYears.add(Long.parseLong(p.getFileName().toString()));
                } catch (NumberFormatException ignored) { org.slf4j.LoggerFactory.getLogger(RegenerateModifiedRastersCli.class).debug("Non-fatal condition in RegenerateModifiedRastersCli: {}", ignored.getMessage()); }
            });
        }
        Collections.sort(epochYears);
        logger.info("Found {} epoch directories in '{}'.", epochYears.size(), earthDir);

        // 2. Regenerate ETOPO 2022 conservative elevation baseline once
        float[][] etopo = EtopoGeoTiffReader.loadEtopoGrid(HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT);
        if (etopo == null) {
            throw new IllegalStateException("Failed to load NOAA ETOPO 2022 GeoTIFF for conservative elevation regeneration.");
        }
        BufferedImage elevImg = EtopoGeoTiffReader.renderGrayscaleElevationImage(
                etopo, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT);
        byte[] elevPng = encodePng(elevImg);
        Files.write(earthDir.resolve("earth_elevation.png"), elevPng);
        HistoricalMapGenerator.loadElevationMask();

        // Warm caches for shared HYDE empirical epochs
        Map<Long, byte[]> holoceneDensityPngCache = new ConcurrentHashMap<>();
        Map<Long, byte[]> ecologicalPngCache = new ConcurrentHashMap<>();
        Map<String, byte[]> modernClimatePngCache = new ConcurrentHashMap<>();

        // Precompute Holocene HYDE density & ecological rasters once per empirical epoch (61 distinct ZIPs)
        List<Long> distinctHydeEpochs = epochYears.stream()
                .filter(y -> y >= -10000L)
                .map(Hyde34GridReader::findNearestHydeYear)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        logger.info("Generating conservative HYDE 3.4 density + ecological rasters for {} distinct empirical epochs (parallel)...", distinctHydeEpochs.size());
        distinctHydeEpochs.parallelStream().forEach(hydeYr -> {
            Scenario dummy = new Scenario();
            dummy.setStartDateYear(hydeYr);
            dummy.setPopulationDensityType("URBAN_CLUSTERS");
            BufferedImage dens = HistoricalMapGenerator.applyAltimetryCoastlineMask(Hyde34GridReader.loadForYear(hydeYr));
            holoceneDensityPngCache.put(hydeYr, encodePngUnchecked(dens));

            BufferedImage eco = HistoricalInstitutionalRasterizer.rasterizeEcologicalFootprintMap("URBAN_CLUSTERS", dummy);
            ecologicalPngCache.put(hydeYr, encodePngUnchecked(eco));
        });

        // Pre-Holographic deep-time zero anthropogenic footprint
        Scenario deepTimeDummy = new Scenario();
        deepTimeDummy.setStartDateYear(-20000L);
        deepTimeDummy.setPopulationDensityType("OUT_OF_AFRICA_100K");
        byte[] deepTimeEcoPng = encodePng(HistoricalInstitutionalRasterizer.rasterizeEcologicalFootprintMap("OUT_OF_AFRICA_100K", deepTimeDummy));

        // 3. Write all 7 updated layers per epoch directory in parallel
        java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);
        epochYears.parallelStream().forEach(year -> {
            try {
                Path dir = earthDir.resolve(String.valueOf(year));
                Files.write(dir.resolve("earth_" + year + "_elevation.png"), elevPng);

                // Density + Ecological
                if (year >= -10000L) {
                    long effHyde = Hyde34GridReader.findNearestHydeYear(year);
                    Files.write(dir.resolve("earth_" + year + "_density.png"), holoceneDensityPngCache.get(effHyde));
                    Files.write(dir.resolve("earth_" + year + "_ecological.png"), ecologicalPngCache.get(effHyde));
                } else {
                    BufferedImage paleoDens;
                    if (year <= -70000L) {
                        paleoDens = HistoricalMapGenerator.applyAltimetryCoastlineMask(
                                PaleolithicDensityGenerator.generatePrehistoricSyntheticDensityMap(year, "OUT_OF_AFRICA_100K"));
                    } else {
                        BufferedImage base10k = Hyde34GridReader.loadForYear(-10000L);
                        paleoDens = HistoricalMapGenerator.applyAltimetryCoastlineMask(
                                PaleolithicDensityGenerator.applyPrehistoricGeographicMask("LGM_REFUGIA", year, base10k));
                    }
                    Files.write(dir.resolve("earth_" + year + "_density.png"), encodePngUnchecked(paleoDens));
                    Files.write(dir.resolve("earth_" + year + "_ecological.png"), deepTimeEcoPng);
                }

                // Climate layers (Temperature, Precipitation, Seasonality, Biomes)
                // Years > -4500 share identical temperature/precipitation/seasonality baseline (unless PaleoClim anomaly differs)
                if (year > -4500L) {
                    byte[] tBytes = modernClimatePngCache.computeIfAbsent("T", k -> encodePngUnchecked(PaleoClimateMapRasterizer.rasterizeTemperatureMap(year)));
                    byte[] pBytes = modernClimatePngCache.computeIfAbsent("P", k -> encodePngUnchecked(PaleoClimateMapRasterizer.rasterizePrecipitationMap(year)));
                    byte[] sBytes = modernClimatePngCache.computeIfAbsent("S", k -> encodePngUnchecked(PaleoClimateMapRasterizer.rasterizeSeasonalityMap(year)));
                    Files.write(dir.resolve("earth_" + year + "_temperature.png"), tBytes);
                    Files.write(dir.resolve("earth_" + year + "_precipitation.png"), pBytes);
                    Files.write(dir.resolve("earth_" + year + "_seasonality.png"), sBytes);
                } else {
                    Files.write(dir.resolve("earth_" + year + "_temperature.png"), encodePngUnchecked(PaleoClimateMapRasterizer.rasterizeTemperatureMap(year)));
                    Files.write(dir.resolve("earth_" + year + "_precipitation.png"), encodePngUnchecked(PaleoClimateMapRasterizer.rasterizePrecipitationMap(year)));
                    Files.write(dir.resolve("earth_" + year + "_seasonality.png"), encodePngUnchecked(PaleoClimateMapRasterizer.rasterizeSeasonalityMap(year)));
                }

                // Biomes: always epoch-resolved when year >= -10000 (modulates climax canopy with HYDE 3.4 cropland/irrigation)
                if (year >= -10000L) {
                    long effHyde = Hyde34GridReader.findNearestHydeYear(year);
                    String biomeKey = (year <= -4500L) ? ("B_" + year) : ("B_MOD_" + effHyde);
                    byte[] bBytes = modernClimatePngCache.computeIfAbsent(biomeKey,
                            k -> encodePngUnchecked(PaleoClimateMapRasterizer.rasterizeBiomesMap(year)));
                    Files.write(dir.resolve("earth_" + year + "_biomes.png"), bBytes);
                } else {
                    Files.write(dir.resolve("earth_" + year + "_biomes.png"), encodePngUnchecked(PaleoClimateMapRasterizer.rasterizeBiomesMap(year)));
                }

                int c = count.incrementAndGet();
                if (c % 32 == 0 || c == epochYears.size()) {
                    logger.info("Regenerated modified rasters for {}/{} epoch directories...", c, epochYears.size());
                }
            } catch (Exception e) {
                logger.error("Failed regenerating rasters for epoch {}: {}", year, e.getMessage(), e);
            }
        });

        long elapsedSec = Math.max(1L, (System.currentTimeMillis() - t0) / 1000L);
        logger.info("Completed regeneration of 7 modified layers across {} epoch directories in {}s.", count, elapsedSec);
    }

    private static byte[] encodePng(BufferedImage img) throws IOException {
        if (img == null) {
            throw new IllegalArgumentException("Cannot encode null BufferedImage to PNG");
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(256 * 1024)) {
            ImageIO.write(img, "PNG", baos);
            return baos.toByteArray();
        }
    }

    private static byte[] encodePngUnchecked(BufferedImage img) {
        try {
            return encodePng(img);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

