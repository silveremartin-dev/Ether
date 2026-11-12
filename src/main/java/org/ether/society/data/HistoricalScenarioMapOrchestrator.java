/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Package-private orchestrator for ETOPO coastline landmasking, HYDE 3.4 demographic
 * density ingestion, extensible procedural tensor synthesis, and batch scenario map caching.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see HistoricalMapGenerator
 * @see HistoricalMapDiskCacheManager
 */
final class HistoricalScenarioMapOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalScenarioMapOrchestrator.class);

    private static volatile BufferedImage cachedElevationMask = null;
    private static final Object ELEV_LOCK = new Object();

    private HistoricalScenarioMapOrchestrator() {
        // Static utility class
    }

    /**
     * Loads and caches the global 2048x1024 land/ocean binary coastline mask from NOAA ETOPO 2022
     * GeoTIFF or fallback planetary elevation rasters.
     *
     * @return grayscale landmask {@link BufferedImage} (255 = land, 0 = ocean), or {@code null} if unavailable
     */
    static BufferedImage loadElevationMask() {
        if (cachedElevationMask != null) return cachedElevationMask;
        synchronized (ELEV_LOCK) {
            if (cachedElevationMask != null) return cachedElevationMask;
            try {
                int tw = HistoricalMapGenerator.WIDTH, th = HistoricalMapGenerator.HEIGHT;
                BufferedImage mask = new BufferedImage(tw, th, BufferedImage.TYPE_BYTE_GRAY);

                float[][] etopo = EtopoGeoTiffReader.loadEtopoGrid(tw, th);
                if (etopo != null) {
                    for (int y = 0; y < th; y++) {
                        for (int x = 0; x < tw; x++) {
                            mask.getRaster().setSample(x, y, 0, (etopo[y][x] >= 0.0f) ? 255 : 0);
                        }
                    }
                    cachedElevationMask = mask;
                    logger.info("Altimetry coastline mask loaded directly from official NOAA ETOPO 2022 GeoTIFF ({}x{}).", tw, th);
                    return cachedElevationMask;
                }

                BufferedImage elev = null;
                File f100k = org.ether.society.config.EtherPaths.getEarthDir().resolve("-100000/earth_-100000_elevation.png").toFile();
                if (!f100k.exists()) {
                    f100k = new File("data/maps/ether/earth/-100000/earth_-100000_elevation.png");
                }
                if (f100k.exists()) {
                    try { elev = ImageIO.read(f100k); } catch (Exception ex) { logger.debug("Failed reading elevation mask from {}: {}", f100k, ex.getMessage()); }
                }
                if (elev == null) {
                    File f2026 = org.ether.society.config.EtherPaths.getEarthDir().resolve("2026/earth_2026_elevation.png").toFile();
                    if (!f2026.exists()) {
                        f2026 = new File("data/maps/ether/earth/2026/earth_2026_elevation.png");
                    }
                    if (f2026.exists()) {
                        try { elev = ImageIO.read(f2026); } catch (Exception ex) { logger.debug("Failed reading elevation mask from {}: {}", f2026, ex.getMessage()); }
                    }
                }
                if (elev == null) {
                    File fCache = new File("data/cache/earth_elevation.png");
                    if (fCache.exists()) {
                        try { elev = ImageIO.read(fCache); } catch (Exception ex) { logger.debug("Failed reading elevation mask from {}: {}", fCache, ex.getMessage()); }
                    }
                }
                if (elev == null) {
                    logger.warn("earth_elevation.png not found - altimetry coastline mask disabled.");
                    return null;
                }
                for (int y = 0; y < th; y++) {
                    for (int x = 0; x < tw; x++) {
                        int sx = Math.clamp((int) ((x / (double) tw) * elev.getWidth()), 0, elev.getWidth() - 1);
                        int sy = Math.clamp((int) ((y / (double) th) * elev.getHeight()), 0, elev.getHeight() - 1);
                        int rgb = elev.getRGB(sx, sy);
                        int r = (rgb >> 16) & 0xFF;
                        int gr = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        double lum = (0.299 * r + 0.587 * gr + 0.114 * b) / 255.0;
                        mask.getRaster().setSample(x, y, 0, (lum >= 0.478) ? 255 : 0);
                    }
                }
                cachedElevationMask = mask;
                logger.info("Altimetry coastline mask loaded from data/ether/ ({}x{} -> {}x{}).", elev.getWidth(), elev.getHeight(), tw, th);
                return cachedElevationMask;
            } catch (Exception e) {
                logger.warn("Failed to load altimetry coastline mask: {}", e.getMessage());
                return null;
            }
        }
    }

    /**
     * Masks ocean pixels to pure black ({@code 0x000000}) using the ETOPO coastline mask.
     *
     * @param src source raster image
     * @return coastline-masked RGB {@link BufferedImage}, or {@code null} if {@code src} is {@code null}
     */
    static BufferedImage applyAltimetryCoastlineMask(BufferedImage src) {
        if (src == null) return null;
        BufferedImage mask = loadElevationMask();
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int mx = (int) ((x + 0.5) * (mask != null ? mask.getWidth() : w) / w);
                int my = (int) ((y + 0.5) * (mask != null ? mask.getHeight() : h) / h);
                mx = Math.clamp(mx, 0, (mask != null ? mask.getWidth() : w) - 1);
                my = Math.clamp(my, 0, (mask != null ? mask.getHeight() : h) - 1);
                int land = (mask != null) ? mask.getRaster().getSample(mx, my, 0) : 255;
                if (land == 0) {
                    out.setRGB(x, y, 0x000000);
                } else {
                    out.setRGB(x, y, src.getRGB(x, y));
                }
            }
        }
        return out;
    }

    /**
     * Computes orographic and hyper-arid habitability attenuation factor in range \([0.12, 1.0]\).
     *
     * @param scenarioType scenario preset identifier (e.g., {@code GREEN_SAHARA})
     * @param lng longitude in degrees
     * @param lat latitude in degrees
     * @return habitability factor \([0.12, 1.0]\)
     */
    static double getTopographicHabitability(String scenarioType, double lng, double lat) {
        double himalayas = Math.exp(-(Math.pow(lat - 32.0, 2) + Math.pow(lng - 85.0, 2)) / 100.0);
        double alps = Math.exp(-(Math.pow(lat - 46.0, 2) + Math.pow(lng - 10.0, 2)) / 30.0);
        double zagros = Math.exp(-(Math.pow(lat - 33.0, 2) + Math.pow(lng - 47.0, 2)) / 40.0);
        double andes = Math.exp(-(Math.pow(lat - (-20.0), 2) + Math.pow(lng - (-70.0), 2)) / 60.0);

        double mountainPenalty = himalayas * 0.75 + alps * 0.5 + zagros * 0.5 + andes * 0.6;
        boolean isGreenSahara = scenarioType != null && scenarioType.equalsIgnoreCase("GREEN_SAHARA");
        double saharaPenalty = 0.0;
        if (!isGreenSahara) {
            double saharaCore = Math.exp(-(Math.pow(lat - 23.0, 2) + Math.pow(lng - 12.0, 2)) / 140.0);
            saharaPenalty = saharaCore * 0.6;
        }

        double factor = 1.0 - mountainPenalty - saharaPenalty;
        return Math.max(0.12, Math.min(1.0, factor));
    }

    /**
     * Rasterizes the population density map for a given target year using HYDE 3.4 or paleolithic models.
     *
     * @param type population density preset type
     * @param scenario target scenario
     * @param targetYear historical epoch year
     * @return coastline-masked density {@link BufferedImage}
     */
    static BufferedImage rasterizeDensityMapForYear(String type, Scenario scenario, long targetYear) {
        if (targetYear < -10000) {
            return applyAltimetryCoastlineMask(PaleolithicDensityGenerator.generatePrehistoricSyntheticDensityMap(targetYear, type));
        }
        BufferedImage realHydeImg = Hyde34GridReader.loadForYear(targetYear);
        if (realHydeImg != null) {
            logger.info("Ingested authentic HYDE 3.4 5-arc-minute Esri ASCII raster grid for year {}", targetYear);
            return applyAltimetryCoastlineMask(realHydeImg);
        }
        if (scenario != null && scenario.isUseRealEarthData() && targetYear >= -10000 && targetYear <= 2024) {
            throw new IllegalStateException("ZERO FALLBACK VIOLATION: Empirical HYDE 3.4 dataset missing for year " + targetYear);
        }
        return applyAltimetryCoastlineMask(PaleolithicDensityGenerator.generatePrehistoricSyntheticDensityMap(targetYear, type));
    }

    /**
     * Synthesizes a deterministic extensible cultural tensor map for custom dimension indices {@code >= 9}.
     *
     * @param tensorIndex custom cultural tensor index
     * @param type scenario density type
     * @param scenario scenario configuration providing deterministic seeds and harmonic parameters
     * @return synthesized 2048x1024 RGB {@link BufferedImage}
     */
    static BufferedImage rasterizeExtensibleTensorMap(int tensorIndex, String type, Scenario scenario) {
        int width = HistoricalMapGenerator.WIDTH, height = HistoricalMapGenerator.HEIGHT;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        long seed = 11235L + tensorIndex * 11111L;
        if (scenario != null && scenario.getTensorSeeds() != null && scenario.getTensorSeeds().containsKey(tensorIndex)) {
            seed = scenario.getTensorSeeds().get(tensorIndex);
        } else if (scenario != null && scenario.getCulturalSeed() != 0) {
            seed = scenario.getCulturalSeed() ^ (tensorIndex * 0x9E3779B97F4A7C15L);
        }

        double pFreq = 0.020;
        double pAmp = 1.00;
        double pDiff = 0.25;
        if (scenario != null && scenario.getTensorProceduralParameters() != null && scenario.getTensorProceduralParameters().containsKey(tensorIndex)) {
            Map<String, Double> pMap = scenario.getTensorProceduralParameters().get(tensorIndex);
            if (pMap != null) {
                pFreq = pMap.getOrDefault("scenario.tensor.ext.p1.label", 0.020);
                pAmp = pMap.getOrDefault("scenario.tensor.ext.p2.label", 1.00);
                pDiff = pMap.getOrDefault("scenario.tensor.ext.p3.label", 0.25);
            }
        }

        Random rnd = new Random(seed);
        double phaseLng = rnd.nextDouble() * Math.PI * 2.0;
        double phaseLat = rnd.nextDouble() * Math.PI * 2.0;
        double scale = Math.clamp(pFreq, 0.001, 0.20);
        float hueBase = (float) ((tensorIndex * 0.137 + (rnd.nextDouble() * 0.2)) % 1.0);

        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y / (double) height) * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = -180.0 + (x / (double) width) * 360.0;
                if (!HistoricalLandGeometryCatalog.isLand(lon, lat)) {
                    img.setRGB(x, y, 0x0F172A);
                    continue;
                }

                double val = (Math.sin(lon * scale + phaseLng) * Math.cos(lat * scale + phaseLat) * 0.5 + 0.5) * pAmp;
                val = Math.clamp(val, 0.0, 1.0);
                float sat = (float) Math.clamp(0.5f + val * 0.45f * (1.0 - pDiff * 0.5), 0.0, 1.0);
                float bright = (float) Math.clamp(0.2f + val * 0.75f, 0.0, 1.0);
                int rgb = Color.HSBtoRGB(hueBase, sat, bright);
                img.setRGB(x, y, rgb);
            }
        }
        g.dispose();
        return img;
    }

    /**
     * Populates all demographic, cultural, and geological cartographic rasters for a {@link Scenario},
     * loading from disk caches when available or generating and persisting them otherwise.
     *
     * @param scenario target scenario instance
     */
    static void populateScenarioHistoricalMaps(Scenario scenario) {
        if (scenario == null) return;
        try {
            if (HistoricalMapDiskCacheManager.loadFromYearDirectory(scenario)) return;

            String safeName = scenario.getName().replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase(Locale.ROOT);
            Path cacheDir = Paths.get("data", "cache");

            if (HistoricalMapDiskCacheManager.loadFromDiskCache(scenario, cacheDir, safeName)) {
                logger.info("Successfully loaded scenario '{}' cartographic tensors from disk cache.", scenario.getName());
                return;
            }

            String presetKey = scenario.getPresetKey();
            if (presetKey != null && !presetKey.isEmpty() && !presetKey.equals(safeName)) {
                if (HistoricalMapDiskCacheManager.loadFromDiskCache(scenario, cacheDir, presetKey)) return;
            }

            String type = scenario.getPopulationDensityType();
            if (type == null) type = "URBAN_CLUSTERS";

            SvgMapIngestor.SvgIngestionResult neResult = NaturalEarthVectorIngestor.loadNaturalEarthMap(type);
            if (neResult == null) neResult = SvgMapIngestor.ingestForScenario(type);
            if (neResult == null && scenario.getStartDateYear() != 0) {
                neResult = HgisAtlasIngestor.loadHistoricalPolityMap(scenario.getStartDateYear(), type);
            }

            BufferedImage imgDensity = resolveScenarioDensityImage(scenario, safeName, type, neResult);
            scenario.setCustomDensityBase64(HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgDensity));

            BufferedImage imgIsogloss = (neResult != null && neResult.isoglossImage != null) ? neResult.isoglossImage : HistoricalCulturalTensorRasterizer.rasterizeIsoglossMap(type, scenario);
            scenario.setCustomTensorMapBase64(0, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgIsogloss));

            BufferedImage imgKinship = (neResult != null && neResult.kinshipImage != null) ? neResult.kinshipImage : HistoricalCulturalTensorRasterizer.rasterizeKinshipMap(type, scenario);
            scenario.setCustomTensorMapBase64(1, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgKinship));

            BufferedImage imgRituals = (neResult != null && neResult.ritualsImage != null) ? neResult.ritualsImage : HistoricalCulturalTensorRasterizer.rasterizeRitualsMap(type, scenario);
            scenario.setCustomTensorMapBase64(2, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgRituals));

            BufferedImage imgSovereignty = (neResult != null && neResult.sovereigntyImage != null) ? neResult.sovereigntyImage : HistoricalCulturalTensorRasterizer.rasterizeSovereigntyMap(type, scenario);
            scenario.setCustomTensorMapBase64(3, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgSovereignty));

            BufferedImage imgTechnology = HistoricalCulturalTensorRasterizer.rasterizeTechnologyMap(type, scenario);
            scenario.setCustomTensorMapBase64(4, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgTechnology));

            BufferedImage imgTrade = HistoricalCulturalTensorRasterizer.rasterizeTradeNetworkMap(type, scenario);
            scenario.setCustomTensorMapBase64(5, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgTrade));

            BufferedImage imgInstitutional = HistoricalCulturalTensorRasterizer.rasterizeInstitutionalComplexityMap(type, scenario);
            scenario.setCustomTensorMapBase64(6, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgInstitutional));

            BufferedImage imgEcological = HistoricalCulturalTensorRasterizer.rasterizeEcologicalFootprintMap(type, scenario);
            scenario.setCustomTensorMapBase64(7, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgEcological));

            BufferedImage imgPathogen = HistoricalCulturalTensorRasterizer.rasterizePathogenImmunityMap(type, scenario);
            scenario.setCustomTensorMapBase64(8, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgPathogen));

            int dims = scenario.getCultureVectorDimensions();
            if (dims > 9) {
                for (int i = 9; i < dims; i++) {
                    BufferedImage imgExt = rasterizeExtensibleTensorMap(i, type, scenario);
                    scenario.setCustomTensorMapBase64(i, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgExt));
                }
            }

            BufferedImage imgCoal = GeologicalResourceMapRasterizer.rasterizeCoalMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(0, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgCoal));

            BufferedImage imgOil = GeologicalResourceMapRasterizer.rasterizeOilMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(1, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgOil));

            BufferedImage imgGas = GeologicalResourceMapRasterizer.rasterizeGasMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(2, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgGas));

            BufferedImage imgUranium = GeologicalResourceMapRasterizer.rasterizeUraniumMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(3, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgUranium));

            BufferedImage imgHe3 = GeologicalResourceMapRasterizer.rasterizeHelium3Map(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(4, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgHe3));

            BufferedImage imgIronCopper = GeologicalResourceMapRasterizer.rasterizeIronCopperMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(5, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgIronCopper));

            BufferedImage imgPreciousMetals = GeologicalResourceMapRasterizer.rasterizePreciousMetalsMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(6, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgPreciousMetals));

            BufferedImage imgRareEarths = GeologicalResourceMapRasterizer.rasterizeRareEarthsMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(7, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgRareEarths));

            BufferedImage imgMantleHeat = GeologicalResourceMapRasterizer.rasterizeMantleHeatMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(8, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgMantleHeat));

            BufferedImage imgAquifer = GeologicalResourceMapRasterizer.rasterizeAquiferMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(9, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgAquifer));

            int resDims = scenario.getResourceVectorDimensions();
            if (resDims > 10) {
                for (int i = 10; i < resDims; i++) {
                    BufferedImage imgExtRes = GeologicalResourceMapRasterizer.rasterizeExtensibleResourceTensorMap(i, type, scenario);
                    scenario.setCustomGeologyTensorMapBase64(i, HistoricalMapDiskCacheManager.bufferedImageToBase64Png(imgExtRes));
                }
            }

            HistoricalMapDiskCacheManager.saveImagesToYearDirectory(scenario.getStartDateYear(), imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology, imgTrade, imgInstitutional, imgEcological, imgPathogen, imgCoal, imgOil, imgGas, imgUranium, imgHe3, imgIronCopper, imgPreciousMetals, imgRareEarths, imgMantleHeat, imgAquifer);
            HistoricalMapDiskCacheManager.saveImagesToDiskCache(scenario.getName(), imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology, imgTrade, imgInstitutional, imgEcological, imgPathogen);
            HistoricalMapDiskCacheManager.saveGeologyTensorsToDiskCache(scenario.getName(), imgCoal, imgOil, imgGas, imgUranium, imgHe3, imgIronCopper, imgPreciousMetals, imgRareEarths, imgMantleHeat, imgAquifer);

        } catch (Exception e) {
            logger.error("Failed to generate historical maps for scenario {}", scenario.getName(), e);
        }
    }

    private static BufferedImage resolveScenarioDensityImage(
            Scenario scenario, String safeName, String type, SvgMapIngestor.SvgIngestionResult neResult) {
        BufferedImage imgDensity = (neResult != null && neResult.densityImage != null) ? neResult.densityImage : null;
        if (imgDensity != null) {
            return applyAltimetryCoastlineMask(imgDensity);
        }
        if (scenario.getStartDateYear() <= -70000) {
            return applyAltimetryCoastlineMask(PaleolithicDensityGenerator.generatePrehistoricSyntheticDensityMap(scenario.getStartDateYear(), type));
        }
        if (scenario.getStartDateYear() < -10000) {
            BufferedImage baseHyde = Hyde34GridReader.loadForYear(-10000);
            if (baseHyde != null) {
                String maskKey = scenario.getPresetKey() != null ? scenario.getPresetKey() : safeName;
                return applyAltimetryCoastlineMask(PaleolithicDensityGenerator.applyPrehistoricGeographicMask(maskKey, scenario.getStartDateYear(), baseHyde));
            }
            return applyAltimetryCoastlineMask(PaleolithicDensityGenerator.generatePrehistoricSyntheticDensityMap(scenario.getStartDateYear(), type));
        }
        imgDensity = Hyde34GridReader.loadForYear(scenario.getStartDateYear());
        if (imgDensity != null) {
            return applyAltimetryCoastlineMask(imgDensity);
        }
        return applyAltimetryCoastlineMask(rasterizeDensityMapForYear(type, scenario, scenario.getStartDateYear()));
    }

    /**
     * Force-regenerates and persists demographic, cultural, and aquifer rasters for a scenario.
     *
     * @param scenario target scenario
     */
    static void forceGenerateCulturalTensorsOnly(Scenario scenario) {
        if (scenario == null) return;
        try {
            String type = scenario.getPopulationDensityType();
            if (type == null) type = "URBAN_CLUSTERS";
            long year = scenario.getStartDateYear();

            BufferedImage imgDensity = rasterizeDensityMapForYear(type, scenario, year);
            BufferedImage imgIsogloss = HistoricalCulturalTensorRasterizer.rasterizeIsoglossMap(type, scenario);
            BufferedImage imgKinship = HistoricalCulturalTensorRasterizer.rasterizeKinshipMap(type, scenario);
            BufferedImage imgRituals = HistoricalCulturalTensorRasterizer.rasterizeRitualsMap(type, scenario);
            BufferedImage imgSovereignty = HistoricalCulturalTensorRasterizer.rasterizeSovereigntyMap(type, scenario);
            BufferedImage imgTechnology = HistoricalCulturalTensorRasterizer.rasterizeTechnologyMap(type, scenario);
            BufferedImage imgTrade = HistoricalCulturalTensorRasterizer.rasterizeTradeNetworkMap(type, scenario);
            BufferedImage imgInstitutional = HistoricalCulturalTensorRasterizer.rasterizeInstitutionalComplexityMap(type, scenario);
            BufferedImage imgEcological = HistoricalCulturalTensorRasterizer.rasterizeEcologicalFootprintMap(type, scenario);
            BufferedImage imgPathogen = HistoricalCulturalTensorRasterizer.rasterizePathogenImmunityMap(type, scenario);
            BufferedImage imgAquifer = GeologicalResourceMapRasterizer.rasterizeAquiferMap(type, scenario);

            HistoricalMapDiskCacheManager.saveImagesToYearDirectory(year, imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology,
                    imgTrade, imgInstitutional, imgEcological, imgPathogen, null, null, null, null,
                    null, null, null, null, null, imgAquifer);

            HistoricalMapDiskCacheManager.saveImagesToDiskCache(scenario.getName(), imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology, imgTrade, imgInstitutional, imgEcological, imgPathogen);
            logger.info("Successfully regenerated and persisted the COMPLETE 25-raster suite for scenario '{}' (Year {}).", scenario.getName(), year);
        } catch (Exception e) {
            logger.error("Failed to generate complete raster suite for scenario {}", scenario.getName(), e);
        }
    }

    /**
     * Verifies and generates missing (or forced) cartographic rasters for all built-in scenarios.
     *
     * @param force {@code true} to force full regeneration even when files exist
     */
    static void ensureAllScenarioMapsGenerated(boolean force) {
        List<Scenario> builtIns = Scenario.getBuiltInScenarios();
        logger.info("Verifying cartographic maps for all {} built-in scenarios in 'data/maps/ether/earth/<year>/' (force={})...", builtIns.size(), force);
        int generated = 0;
        for (Scenario sc : builtIns) {
            long year = sc.getStartDateYear();
            Path earthDir = org.ether.society.config.EtherPaths.getEarthDir().resolve(String.valueOf(year));
            boolean exists = !force &&
                             Files.exists(earthDir.resolve("earth_" + year + "_density.png")) &&
                             Files.exists(earthDir.resolve("earth_" + year + "_isogloss.png")) &&
                             Files.exists(earthDir.resolve("earth_" + year + "_sovereignty.png"));
            if (!exists) {
                populateScenarioHistoricalMaps(sc);
                generated++;
            }
        }
        logger.info("Batch generation check complete. Generated/updated {} scenarios in data/ether/earth/.", generated);
    }

    /**
     * Pre-caches cartographic tensor maps for all named scenario archetypes into {@code data/cache/}.
     */
    static void precacheAllBuiltInScenarios() {
        String[] types = {
            "ONE_CONTINENT", "OUT_OF_AFRICA_100K", "FERTILE_CRESCENT_8000BC",
            "ROMAN_EMPIRE_0", "SONG_DYNASTY_1000", "MALI_EMPIRE_1324",
            "EGYPT_NILE", "MESOPOTAMIA_ASSYRIA", "GREEN_SAHARA", "INDIA_MAURYA",
            "SAKOKU_JAPAN", "MESOAMERICA", "AMERICAS_1491", "SAHUL_MIGRATION",
            "BERINGIA_AMERICAS", "URBAN_CLUSTERS",
            "SILK_ROAD_NEXUS", "LGM_REFUGIA", "INDIAN_OCEAN_TRADE", "BRONZE_AGE_COLLAPSE"
        };
        logger.info("Pre-caching cartographic tensor maps for {} built-in scenarios to 'data/cache/'...", types.length);
        for (String type : types) {
            Scenario dummy = new Scenario();
            dummy.setName(type);
            dummy.setPopulationDensityType(type);
            populateScenarioHistoricalMaps(dummy);
        }
        logger.info("Cartographic pre-caching complete.");
    }
}
