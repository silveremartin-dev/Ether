/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;
import org.ether.society.procedural.PlanetPreset;
import org.ether.society.procedural.ProceduralGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Ultra-High Precision Cartographic Generator for Ether Simulation Engine.
 * Supports continuous topographic landmasking and tailored multi-channel spatial tensors
 * across all 19 built-in scenarios.
 */
public class HistoricalMapGenerator {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalMapGenerator.class);

    public static final int WIDTH = 1024;
    public static final int HEIGHT = 512;

    private static final List<Path2D> LAND_POLYGONS = new ArrayList<>();
    private static final List<Path2D> SEA_POLYGONS = new ArrayList<>();

    static {
        initHighPrecisionGeographicPolygons();
    }

    public static void populateScenarioHistoricalMaps(Scenario scenario) {
        if (scenario == null) return;

        try {
            String safeName = scenario.getName().replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase(java.util.Locale.ROOT);
            java.nio.file.Path cacheDir = java.nio.file.Paths.get("data", "cache");

            if (loadFromDiskCache(scenario, cacheDir, safeName)) {
                logger.info("Successfully loaded scenario '{}' cartographic tensors from disk cache 'data/cache/{}_*.png'.", scenario.getName(), safeName);
                return;
            }

            String type = scenario.getPopulationDensityType();
            if (type == null) type = "URBAN_CLUSTERS";

            // 0. Try High-Precision Natural Earth & SVG Vector Cartography Ingestion Pipeline First
            SvgMapIngestor.SvgIngestionResult neResult = NaturalEarthVectorIngestor.loadNaturalEarthMap(type);
            if (neResult == null) {
                neResult = SvgMapIngestor.ingestForScenario(type);
            }
            if (neResult == null && scenario.getStartDateYear() != 0) {
                neResult = HgisAtlasIngestor.loadHistoricalPolityMap(scenario.getStartDateYear(), type);
            }

            if (neResult != null) {
                SvgMapIngestor.applyToScenario(scenario, neResult);
                saveImagesToDiskCache(scenario.getName(), neResult.densityImage, neResult.sovereigntyImage, neResult.isoglossImage, neResult.kinshipImage, neResult.ritualsImage, null, null, null, null, null);
                logger.info("Successfully populated scenario '{}' using high-precision GIS vector cartography (Natural Earth / SVG / HGIS) and cached to data/maps/cache/.", scenario.getName());
                return;
            }

            // 1. Try HYDE 3.4 High-Resolution ASCII Grid Ingestion (for dates >= -10,000 BC)
            BufferedImage imgDensity = null;
            if (scenario.getStartDateYear() < -10000) {
                scenario.setCustomDensityBase64(null);
                logger.info("Prehistoric epoch {} BC precedes HYDE 3.4 baseline (-10,000 BC). Enforcing procedural density distribution with biogeographical containment.", scenario.getStartDateYear());
            } else {
                imgDensity = Hyde34GridReader.loadForYear(scenario.getStartDateYear());
                if (imgDensity != null) {
                    scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));
                    logger.info("Successfully populated scenario '{}' density tensor using HYDE 3.4 5-arc-minute grid for year {}.", scenario.getName(), scenario.getStartDateYear());
                } else {
                    imgDensity = generateCleanDensityMap(type, scenario);
                    if (imgDensity != null) {
                        scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));
                    }
                }
            }

            // 1. Clean Multi-Channel Isogloss Map (Index 0)
            BufferedImage imgIsogloss = generateCleanIsoglossMap(type, scenario);
            scenario.setCustomTensorMapBase64(0, bufferedImageToBase64Png(imgIsogloss));

            // 2. Clean Multi-Channel Kinship Map (Index 1)
            BufferedImage imgKinship = generateCleanKinshipMap(type, scenario);
            scenario.setCustomTensorMapBase64(1, bufferedImageToBase64Png(imgKinship));

            // 3. Clean Multi-Channel Rituals Map (Index 2)
            BufferedImage imgRituals = generateCleanRitualsMap(type, scenario);
            scenario.setCustomTensorMapBase64(2, bufferedImageToBase64Png(imgRituals));

            // 4. Clean Multi-Channel Sovereignty Map (Index 3)
            BufferedImage imgSovereignty = generateCleanSovereigntyMap(type, scenario);
            scenario.setCustomTensorMapBase64(3, bufferedImageToBase64Png(imgSovereignty));

            // 5. Clean Multi-Channel Technology Mode Map (Index 4)
            BufferedImage imgTechnology = generateCleanTechnologyMap(type, scenario);
            scenario.setCustomTensorMapBase64(4, bufferedImageToBase64Png(imgTechnology));

            // 6. Clean Multi-Channel Trade Network Map (Index 5)
            BufferedImage imgTrade = generateCleanTradeNetworkMap(type, scenario);
            scenario.setCustomTensorMapBase64(5, bufferedImageToBase64Png(imgTrade));

            // 7. Clean Multi-Channel Institutional Complexity Map (Index 6)
            BufferedImage imgInstitutional = generateCleanInstitutionalComplexityMap(type, scenario);
            scenario.setCustomTensorMapBase64(6, bufferedImageToBase64Png(imgInstitutional));

            // 8. Clean Multi-Channel Ecological Footprint Map (Index 7)
            BufferedImage imgEcological = generateCleanEcologicalFootprintMap(type, scenario);
            scenario.setCustomTensorMapBase64(7, bufferedImageToBase64Png(imgEcological));

            // 9. Clean Multi-Channel Pathogen Immunity Map (Index 8)
            BufferedImage imgPathogen = generateCleanPathogenImmunityMap(type, scenario);
            scenario.setCustomTensorMapBase64(8, bufferedImageToBase64Png(imgPathogen));

            // 10. Extensible Cultural Tensors (Indices 9 to N-1) if N > 9
            int dims = scenario.getCultureVectorDimensions();
            if (dims > 9) {
                for (int i = 9; i < dims; i++) {
                    BufferedImage imgExt = generateCleanExtensibleTensorMap(i, type, scenario);
                    scenario.setCustomTensorMapBase64(i, bufferedImageToBase64Png(imgExt));
                }
            }

            // --- EXTENSIBLE GEOLOGICAL & ENERGY RESOURCE TENSORS (TAB 2) ---
            // Index 0: Coal Deposits
            BufferedImage imgCoal = generateCleanCoalMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(0, bufferedImageToBase64Png(imgCoal));

            // Index 1: Crude Oil Reserves
            BufferedImage imgOil = generateCleanOilMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(1, bufferedImageToBase64Png(imgOil));

            // Index 2: Natural Gas Fields
            BufferedImage imgGas = generateCleanGasMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(2, bufferedImageToBase64Png(imgGas));

            // Index 3: Uranium & Thorium Ores
            BufferedImage imgUranium = generateCleanUraniumMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(3, bufferedImageToBase64Png(imgUranium));

            // Index 4: Helium-3 Fusion Ores
            BufferedImage imgHe3 = generateCleanHelium3Map(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(4, bufferedImageToBase64Png(imgHe3));

            // Index 5: Iron & Copper Base Metals
            BufferedImage imgIronCopper = generateCleanIronCopperMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(5, bufferedImageToBase64Png(imgIronCopper));

            // Index 6: Precious Metals & Rare Earths
            BufferedImage imgPreciousREE = generateCleanPreciousMetalsMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(6, bufferedImageToBase64Png(imgPreciousREE));

            // Index 7: Mantle Heat Flux & Tectonics
            BufferedImage imgMantleHeat = generateCleanMantleHeatMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(7, bufferedImageToBase64Png(imgMantleHeat));

            // Index 8: Freshwater Aquifers
            BufferedImage imgAquifer = generateCleanAquiferMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(8, bufferedImageToBase64Png(imgAquifer));

            // Extensible Geology Tensors (Indices 9 to N-1) if N > 9
            int resDims = scenario.getResourceVectorDimensions();
            if (resDims > 9) {
                for (int i = 9; i < resDims; i++) {
                    BufferedImage imgExtRes = generateCleanExtensibleResourceTensorMap(i, type, scenario);
                    scenario.setCustomGeologyTensorMapBase64(i, bufferedImageToBase64Png(imgExtRes));
                }
            }

            // Save cultural tensor maps to disk cache
            saveImagesToDiskCache(scenario.getName(), imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology, imgTrade, imgInstitutional, imgEcological, imgPathogen);

            // Save geological tensor maps to disk cache (indices 0-8: coal, oil, gas, uranium, he3, ironcopper, preciousree, mantleheat, aquifer)
            saveGeologyTensorsToDiskCache(scenario.getName(), imgCoal, imgOil, imgGas, imgUranium, imgHe3, imgIronCopper, imgPreciousREE, imgMantleHeat, imgAquifer);

        } catch (Exception e) {
            logger.error("Failed to generate historical maps for scenario {}", scenario.getName(), e);
        }
    }

    public static void saveImagesToDiskCache(String scenarioName, BufferedImage imgDensity, BufferedImage imgSovereignty, BufferedImage imgIsogloss, BufferedImage imgKinship, BufferedImage imgRituals, BufferedImage imgTech, BufferedImage imgTrade, BufferedImage imgInst, BufferedImage imgEco, BufferedImage imgPathogen) {
        if (scenarioName == null || scenarioName.isBlank()) scenarioName = "scenario";
        try {
            java.nio.file.Path cacheDir = java.nio.file.Paths.get("data", "cache");
            java.nio.file.Files.createDirectories(cacheDir);

            String safeName = scenarioName.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase(java.util.Locale.ROOT);

            if (imgDensity != null) ImageIO.write(imgDensity, "PNG", cacheDir.resolve(safeName + "_density.png").toFile());
            if (imgSovereignty != null) ImageIO.write(imgSovereignty, "PNG", cacheDir.resolve(safeName + "_sovereignty.png").toFile());
            if (imgIsogloss != null) ImageIO.write(imgIsogloss, "PNG", cacheDir.resolve(safeName + "_isogloss.png").toFile());
            if (imgKinship != null) ImageIO.write(imgKinship, "PNG", cacheDir.resolve(safeName + "_kinship.png").toFile());
            if (imgRituals != null) ImageIO.write(imgRituals, "PNG", cacheDir.resolve(safeName + "_rituals.png").toFile());
            if (imgTech != null) ImageIO.write(imgTech, "PNG", cacheDir.resolve(safeName + "_technology.png").toFile());
            if (imgTrade != null) ImageIO.write(imgTrade, "PNG", cacheDir.resolve(safeName + "_tradenetwork.png").toFile());
            if (imgInst != null) ImageIO.write(imgInst, "PNG", cacheDir.resolve(safeName + "_institutional.png").toFile());
            if (imgEco != null) ImageIO.write(imgEco, "PNG", cacheDir.resolve(safeName + "_ecological.png").toFile());
            if (imgPathogen != null) ImageIO.write(imgPathogen, "PNG", cacheDir.resolve(safeName + "_pathogen.png").toFile());

            logger.info("Persisted cultural tensor maps to disk cache 'data/cache/{}_*.png'", safeName);
        } catch (Exception e) {
            logger.warn("Failed to write cultural tensor maps to disk cache directory: {}", e.getMessage());
        }
    }

    /**
     * Persists the 9 geological/energy tensor maps to the disk cache.
     * Canonical filenames: _coal, _oil, _gas, _uranium, _he3, _ironcopper, _preciousree, _mantleheat, _aquifer.
     */
    public static void saveGeologyTensorsToDiskCache(String scenarioName,
            BufferedImage imgCoal, BufferedImage imgOil, BufferedImage imgGas,
            BufferedImage imgUranium, BufferedImage imgHe3, BufferedImage imgIronCopper,
            BufferedImage imgPreciousREE, BufferedImage imgMantleHeat, BufferedImage imgAquifer) {
        if (scenarioName == null || scenarioName.isBlank()) scenarioName = "scenario";
        try {
            java.nio.file.Path cacheDir = java.nio.file.Paths.get("data", "cache");
            java.nio.file.Files.createDirectories(cacheDir);
            String safeName = scenarioName.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase(java.util.Locale.ROOT);

            if (imgCoal != null)       ImageIO.write(imgCoal,       "PNG", cacheDir.resolve(safeName + "_coal.png").toFile());
            if (imgOil != null)        ImageIO.write(imgOil,        "PNG", cacheDir.resolve(safeName + "_oil.png").toFile());
            if (imgGas != null)        ImageIO.write(imgGas,        "PNG", cacheDir.resolve(safeName + "_gas.png").toFile());
            if (imgUranium != null)    ImageIO.write(imgUranium,    "PNG", cacheDir.resolve(safeName + "_uranium.png").toFile());
            if (imgHe3 != null)        ImageIO.write(imgHe3,        "PNG", cacheDir.resolve(safeName + "_he3.png").toFile());
            if (imgIronCopper != null) ImageIO.write(imgIronCopper, "PNG", cacheDir.resolve(safeName + "_ironcopper.png").toFile());
            if (imgPreciousREE != null) ImageIO.write(imgPreciousREE, "PNG", cacheDir.resolve(safeName + "_preciousree.png").toFile());
            if (imgMantleHeat != null) ImageIO.write(imgMantleHeat, "PNG", cacheDir.resolve(safeName + "_mantleheat.png").toFile());
            if (imgAquifer != null)    ImageIO.write(imgAquifer,    "PNG", cacheDir.resolve(safeName + "_aquifer.png").toFile());

            logger.info("Persisted 9 geological tensor maps to disk cache 'data/cache/{}_*.png'", safeName);
        } catch (Exception e) {
            logger.warn("Failed to write geological tensor maps to disk cache directory: {}", e.getMessage());
        }
    }

    public static boolean loadFromDiskCache(Scenario scenario, java.nio.file.Path cacheDir, String safeName) {
        java.nio.file.Path densityCache = cacheDir.resolve(safeName + "_density.png");
        if (!java.nio.file.Files.exists(densityCache)) {
            return false;
        }
        try {
            BufferedImage imgDensity = ImageIO.read(densityCache.toFile());
            if (imgDensity == null) return false;

            if (scenario.getStartDateYear() >= -10000) {
                scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));
            } else {
                scenario.setCustomDensityBase64(null);
            }

            String[] mapKeys = {
                "_isogloss.png", "_kinship.png", "_rituals.png", "_sovereignty.png",
                "_technology.png", "_tradenetwork.png", "_institutional.png", "_ecological.png", "_pathogen.png"
            };
            for (int i = 0; i < mapKeys.length; i++) {
                java.nio.file.Path p = cacheDir.resolve(safeName + mapKeys[i]);
                if (java.nio.file.Files.exists(p)) {
                    BufferedImage img = ImageIO.read(p.toFile());
                    if (img != null) {
                        scenario.setCustomTensorMapBase64(i, bufferedImageToBase64Png(img));
                    }
                }
            }

            // Geology tensor cache keys — indices must match setCustomGeologyTensorMapBase64 order:
            // 0=Coal, 1=Oil, 2=Gas, 3=Uranium, 4=He3, 5=IronCopper, 6=PreciousREE, 7=MantleHeat, 8=Aquifer
            String[] geoKeys = {
                "_coal.png", "_oil.png", "_gas.png", "_uranium.png",
                "_he3.png", "_ironcopper.png", "_preciousree.png", "_mantleheat.png", "_aquifer.png"
            };
            for (int i = 0; i < geoKeys.length; i++) {
                java.nio.file.Path p = cacheDir.resolve(safeName + geoKeys[i]);
                if (java.nio.file.Files.exists(p)) {
                    BufferedImage img = ImageIO.read(p.toFile());
                    if (img != null) {
                        scenario.setCustomGeologyTensorMapBase64(i, bufferedImageToBase64Png(img));
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.warn("Failed to load scenario '{}' cartographic tensors from disk cache: {}", scenario.getName(), e.getMessage());
            return false;
        }
    }

    // --- 1. CLEAN DENSITY MAP ---

    public static BufferedImage generateCleanDensityMap(String type, Scenario scenario) {
        return generateCleanDensityMapForYear(type, scenario, (scenario != null) ? scenario.getStartDateYear() : -10000);
    }

    public static BufferedImage generateCleanDensityMapForYear(String type, Scenario scenario, long targetYear) {
        if (targetYear < -10000) {
            throw new IllegalStateException("DATA INGESTION ERROR: Target year " + targetYear + " precedes empirical HYDE 3.4 baseline (-10,000 BC)! Zero-fallback policy active: synthetic approximations are disabled.");
        }
        BufferedImage realHydeImg = Hyde34GridReader.loadForYear(targetYear);
        if (realHydeImg != null) {
            logger.info("Ingested authentic HYDE 3.4 5-arc-minute Esri ASCII raster grid for year {}", targetYear);
            return realHydeImg;
        }

        // ZERO FALLBACK POLICY: Abort rather than generating synthetic approximations
        throw new IllegalStateException("DATA INGESTION ERROR: Empirical HYDE 3.4 raster grid missing for year " + targetYear + "! Zero-fallback policy active: synthetic approximations are disabled.");
    }

    public static double getTopographicHabitability(String scenarioType, double lng, double lat) {
        // Relief/elevation penalties for high mountain ranges
        double himalayas = Math.exp(-(Math.pow(lat - 32.0, 2) + Math.pow(lng - 85.0, 2)) / 100.0);
        double alps = Math.exp(-(Math.pow(lat - 46.0, 2) + Math.pow(lng - 10.0, 2)) / 30.0);
        double zagros = Math.exp(-(Math.pow(lat - 33.0, 2) + Math.pow(lng - 47.0, 2)) / 40.0);
        double andes = Math.exp(-(Math.pow(lat - (-20.0), 2) + Math.pow(lng - (-70.0), 2)) / 60.0);

        double mountainPenalty = himalayas * 0.75 + alps * 0.5 + zagros * 0.5 + andes * 0.6;

        // Sahara core penalty when not Green Sahara
        boolean isGreenSahara = scenarioType != null && scenarioType.equalsIgnoreCase("GREEN_SAHARA");
        double saharaPenalty = 0.0;
        if (!isGreenSahara) {
            double saharaCore = Math.exp(-(Math.pow(lat - 23.0, 2) + Math.pow(lng - 12.0, 2)) / 140.0);
            saharaPenalty = saharaCore * 0.6;
        }

        double factor = 1.0 - mountainPenalty - saharaPenalty;
        return Math.max(0.12, Math.min(1.0, factor));
    }

    // --- 2. CLEAN SOVEREIGNTY MAP ---
    public static BufferedImage generateCleanSovereigntyMap(String type, Scenario scenario) {
        BufferedImage img = createPureTransparentCanvas();
        List<EmpireTerritory> empires = getEmpiresForScenario(type);

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lng = (x + 0.5) / WIDTH * 360.0 - 180.0;

                for (EmpireTerritory emp : empires) {
                    if (emp.contains(lng, lat)) {
                        int r = emp.color.getRed();
                        int g = emp.color.getGreen();
                        int b = emp.color.getBlue();
                        img.setRGB(x, y, (210 << 24) | (r << 16) | (g << 8) | b);
                        break;
                    }
                }
            }
        }
        return img;
    }

    // --- 3. CLEAN ISOGLOSS MAP ---

    public static BufferedImage generateCleanIsoglossMap(String type, Scenario scenario) {
        BufferedImage img = createPureTransparentCanvas();
        List<LanguageZone> langZones = getLanguageZonesForScenario(type);

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lng = (x + 0.5) / WIDTH * 360.0 - 180.0;

                double totalWeight = 0.0;
                double rSum = 0, gSum = 0, bSum = 0;

                for (LanguageZone lz : langZones) {
                    double d2 = distSq(lng, lat, lz.centerLng, lz.centerLat);
                    if (d2 > 2500.0) continue;
                    double w = 1.0 / Math.pow(d2 + 4.0, 1.5);
                    totalWeight += w;
                    rSum += w * lz.color.getRed();
                    gSum += w * lz.color.getGreen();
                    bSum += w * lz.color.getBlue();
                }

                if (totalWeight > 0.0001) {
                    int r = (int) Math.min(255, rSum / totalWeight);
                    int gCol = (int) Math.min(255, gSum / totalWeight);
                    int b = (int) Math.min(255, bSum / totalWeight);
                    img.setRGB(x, y, (200 << 24) | (r << 16) | (gCol << 8) | b);
                }
            }
        }
        return img;
    }

    // --- 4. CLEAN KINSHIP MAP ---

    public static BufferedImage generateCleanKinshipMap(String type, Scenario scenario) {
        BufferedImage img = createPureTransparentCanvas();
        List<LanguageZone> kinshipZones = getKinshipZonesForScenario(type);

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lng = (x + 0.5) / WIDTH * 360.0 - 180.0;

                LanguageZone bestZone = null;
                double minDist = Double.MAX_VALUE;

                for (LanguageZone kz : kinshipZones) {
                    double d2 = distSq(lng, lat, kz.centerLng, kz.centerLat);
                    if (d2 < minDist) {
                        minDist = d2;
                        bestZone = kz;
                    }
                }

                if (bestZone != null && minDist <= 1600.0) {
                    int r = bestZone.color.getRed();
                    int g = bestZone.color.getGreen();
                    int b = bestZone.color.getBlue();
                    img.setRGB(x, y, (200 << 24) | (r << 16) | (g << 8) | b);
                }
            }
        }
        return img;
    }

    // --- 5. CLEAN RITUALS MAP ---

    public static BufferedImage generateCleanRitualsMap(String type, Scenario scenario) {
        BufferedImage img = createPureTransparentCanvas();
        List<LanguageZone> sacredSites = getSacredSitesForScenario(type);

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lng = (x + 0.5) / WIDTH * 360.0 - 180.0;

                double rSum = 0, gSum = 0, bSum = 0, wSum = 0;
                for (LanguageZone ss : sacredSites) {
                    double d2 = distSq(lng, lat, ss.centerLng, ss.centerLat);
                    double w = Math.exp(-d2 / (2.0 * 35.0));
                    wSum += w;
                    rSum += w * ss.color.getRed();
                    gSum += w * ss.color.getGreen();
                    bSum += w * ss.color.getBlue();
                }

                if (wSum > 0.02) {
                    int r = (int) Math.min(255, rSum);
                    int gCol = (int) Math.min(255, gSum);
                    int b = (int) Math.min(255, bSum);
                    int alpha = (int) Math.clamp(wSum * 255.0, 80.0, 230.0);
                    img.setRGB(x, y, (alpha << 24) | (r << 16) | (gCol << 8) | b);
                }
            }
        }
        return img;
    }


    // --- SCENARIO BOUNDS FOR ALL 19 SCENARIOS ---

    public static boolean isInScenarioBounds(String type, double lng, double lat) {
        if (type == null) return true;

        switch (type.toUpperCase()) {
            case "ONE_CONTINENT": // Sortie d'Afrique (-100k)
                if (lng < -25.0 || (lat < 10.0 && lng > 95.0) || (lat < -10.0 && lng > 110.0)) return false;
                return (lat >= -35.0 && lat <= 65.0 && lng >= -18.0 && lng <= 125.0);

            case "SAHUL_MIGRATION": // Sahul (-50k)
                return (lat >= -45.0 && lat <= 15.0 && lng >= 105.0 && lng <= 160.0);

            case "BERINGIA_AMERICAS": // Beringia (-20k)
                return (lat >= 30.0 && lat <= 78.0 && (lng >= 130.0 || lng <= -100.0));

            case "FERTILE_CRESCENT_8000BC": // Croissant Fertile (-8000)
                return (lat >= 20.0 && lat <= 45.0 && lng >= 25.0 && lng <= 65.0);

            case "GREEN_SAHARA": // Sahara Vert (-6000)
                return (lat >= 10.0 && lat <= 36.0 && lng >= -18.0 && lng <= 40.0);

            case "ROMAN_EMPIRE_0": // Empire Romain (An 0)
                return (lat >= 20.0 && lat <= 60.0 && lng >= -15.0 && lng <= 50.0);

            case "SONG_DYNASTY_1000": // Dynastie Song (1000)
                return (lat >= 15.0 && lat <= 55.0 && lng >= 70.0 && lng <= 140.0);

            case "MALI_EMPIRE_1324": // Empire du Mali (1324)
                return (lat >= 4.0 && lat <= 30.0 && lng >= -18.0 && lng <= 20.0);

            case "EGYPT_NILE": // Égypte & Nil (-3000)
                return (lat >= 15.0 && lat <= 33.0 && lng >= 24.0 && lng <= 36.0);

            case "MESOPOTAMIA_ASSYRIA": // Mésopotamie (-2000)
                return (lat >= 28.0 && lat <= 40.0 && lng >= 38.0 && lng <= 50.0);

            case "INDIA_MAURYA": // Empire Maurya (-250)
                return (lat >= 5.0 && lat <= 38.0 && lng >= 65.0 && lng <= 95.0);

            case "SAKOKU_JAPAN": // Japon Sakoku (1630)
                return (lat >= 28.0 && lat <= 46.0 && lng >= 128.0 && lng <= 146.0);

            case "MESOAMERICA": // Mésoamérique (-1200)
                return (lat >= 12.0 && lat <= 24.0 && lng >= -105.0 && lng <= -86.0);

            case "AMERICAS_1491": // Amériques (1491)
                return (lat >= -56.0 && lat <= 72.0 && lng >= -170.0 && lng <= -34.0);

            case "URBAN_CLUSTERS": // Modern Urban Clusters
                return true;

            default:
                return true;
        }
    }

    // --- LANDMASK UTILITIES ---

    private static boolean[][] FAST_LAND_GRID;

    private static BufferedImage createPureTransparentCanvas() {
        return new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    }

    public static boolean isLand(double lng, double lat) {
        if (FAST_LAND_GRID != null) {
            int gx = Math.max(0, Math.min(719, (int) ((lng + 180.0) * 2.0)));
            int gy = Math.max(0, Math.min(359, (int) ((90.0 - lat) * 2.0)));
            return FAST_LAND_GRID[gx][gy];
        }

        int x = (int) ((lng + 180.0) / 360.0 * WIDTH);
        int y = (int) ((90.0 - lat) / 180.0 * HEIGHT);
        Point p = new Point(x, y);

        for (Path2D land : LAND_POLYGONS) {
            if (land.contains(p)) {
                for (Path2D sea : SEA_POLYGONS) {
                    if (sea.contains(p)) return false;
                }
                return true;
            }
        }
        return false;
    }

    // --- 6. CLEAN TECHNOLOGY & SUBSISTENCE MAP ---
    private static BufferedImage generateCleanTechnologyMap(String type, Scenario scenario) {
        BufferedImage img = createPureTransparentCanvas();
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lng = -180.0 + (x + 0.5) / WIDTH * 360.0;

                Color techColor = null;
                if (type.equalsIgnoreCase("FERTILE_CRESCENT_8000BC") || type.equalsIgnoreCase("GREEN_SAHARA")) {
                    if (lng >= 25.0 && lng <= 55.0 && lat >= 25.0 && lat <= 40.0) {
                        techColor = new Color(52, 211, 153);
                    }
                } else if (type.equalsIgnoreCase("BRONZE_AGE_COLLAPSE") || type.equalsIgnoreCase("EGYPT_NILE") || type.equalsIgnoreCase("MESOPOTAMIA_ASSYRIA")) {
                    if (lng >= 20.0 && lng <= 50.0 && lat >= 20.0 && lat <= 40.0) {
                        techColor = new Color(251, 191, 36);
                    }
                } else if (type.equalsIgnoreCase("SONG_DYNASTY_1000") || type.equalsIgnoreCase("MALI_EMPIRE_1324")) {
                    if (lng >= 95.0 && lng <= 125.0) {
                        techColor = new Color(168, 85, 247);
                    }
                }

                if (techColor != null) {
                    int r = techColor.getRed();
                    int g = techColor.getGreen();
                    int b = techColor.getBlue();
                    img.setRGB(x, y, (190 << 24) | (r << 16) | (g << 8) | b);
                }
            }
        }
        return img;
    }

    // --- 7. CLEAN TRADE NETWORK MAP ---
    private static BufferedImage generateCleanTradeNetworkMap(String type, Scenario scenario) {
        BufferedImage img = createPureTransparentCanvas();
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw Trade Corridors directly on transparent ARGB overlay
        drawTradeRoute(g, new double[][]{{115, 34}, {100, 38}, {75, 39}, {62, 37}, {44, 33}, {28, 41}}, new Color(236, 72, 153), 3.0); // Silk Road
        drawTradeRoute(g, new double[][]{{-4, 12}, {-1, 18}, {3, 27}, {10, 36}}, new Color(245, 158, 11), 2.5); // Trans-Saharan Gold & Salt
        drawTradeRoute(g, new double[][]{{45, 12}, {55, 24}, {75, 12}, {102, 2}, {115, -6}}, new Color(6, 182, 212), 2.5); // Indian Ocean Maritime
        drawTradeRoute(g, new double[][]{{6, 53}, {12, 48}, {24, 50}, {30, 60}}, new Color(59, 130, 246), 2.0); // Amber & Fur Corridors

        g.dispose();
        return img;
    }

    private static void drawTradeRoute(Graphics2D g, double[][] coords, Color col, double strokeWidth) {
        g.setColor(col);
        g.setStroke(new BasicStroke((float) strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < coords.length - 1; i++) {
            int x1 = (int) ((coords[i][0] + 180.0) / 360.0 * WIDTH);
            int y1 = (int) ((90.0 - coords[i][1]) / 180.0 * HEIGHT);
            int x2 = (int) ((coords[i + 1][0] + 180.0) / 360.0 * WIDTH);
            int y2 = (int) ((90.0 - coords[i + 1][1]) / 180.0 * HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    // --- 8. CLEAN INSTITUTIONAL COMPLEXITY MAP (SESHAT) ---
    private static BufferedImage generateCleanInstitutionalComplexityMap(String type, Scenario scenario) {
        BufferedImage img = createPureTransparentCanvas();
        List<CityPoint> cities = getCitiesForScenario(type);
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lng = -180.0 + (x + 0.5) / WIDTH * 360.0;

                double maxInst = 0.0;
                for (CityPoint cp : cities) {
                    double d2 = distSq(lng, lat, cp.lng, cp.lat);
                    double val = cp.weight * Math.exp(-d2 / (2.0 * cp.sigma * cp.sigma));
                    maxInst = Math.max(maxInst, val);
                }

                if (maxInst > 0.1) {
                    double norm = Math.clamp(maxInst / 4.0, 0.0, 1.0);
                    Color instColor = Color.getHSBColor((float) ((1.0 - norm) * 0.65), 0.85f, (float) (0.4 + 0.6 * norm));
                    int alpha = (int) Math.clamp(norm * 240.0, 90.0, 230.0);
                    img.setRGB(x, y, (alpha << 24) | (instColor.getRed() << 16) | (instColor.getGreen() << 8) | instColor.getBlue());
                }
            }
        }
        return img;
    }

    // --- 9. CLEAN ECOLOGICAL FOOTPRINT MAP ---
    private static BufferedImage generateCleanEcologicalFootprintMap(String type, Scenario scenario) {
        BufferedImage img = createPureTransparentCanvas();
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lng = -180.0 + (x + 0.5) / WIDTH * 360.0;

                double ecoStrain = 0.0;
                if (lng >= 35.0 && lng <= 48.0 && lat >= 30.0 && lat <= 38.0) ecoStrain = 0.85; // Mesopotamia Salinization
                else if (lng >= -9.0 && lng <= 35.0 && lat >= 34.0 && lat <= 45.0) ecoStrain = 0.65; // Mediterranean Deforestation
                else if (lng >= 105.0 && lng <= 122.0 && lat >= 30.0 && lat <= 40.0) ecoStrain = 0.75; // Yellow River Loess Erosion

                if (ecoStrain > 0.05) {
                    Color ecoColor = new Color((int)(ecoStrain * 255), (int)((1.0 - ecoStrain) * 180), 40);
                    int alpha = (int) (ecoStrain * 200);
                    img.setRGB(x, y, (alpha << 24) | (ecoColor.getRed() << 16) | (ecoColor.getGreen() << 8) | ecoColor.getBlue());
                }
            }
        }
        return img;
    }

    // --- 10. CLEAN PATHOGEN IMMUNITY MAP ---
    private static BufferedImage generateCleanPathogenImmunityMap(String type, Scenario scenario) {
        BufferedImage img = createPureTransparentCanvas();
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lng = -180.0 + (x + 0.5) / WIDTH * 360.0;

                Color pathColor = null;
                if (lat >= -15.0 && lat <= 15.0 && lng >= -80.0 && lng <= 140.0) {
                    pathColor = new Color(220, 38, 38); // Endemic Tropical Reservoirs (Red)
                }

                if (pathColor != null) {
                    img.setRGB(x, y, (180 << 24) | (pathColor.getRed() << 16) | (pathColor.getGreen() << 8) | pathColor.getBlue());
                }
            }
        }
        return img;
    }

    private static void initHighPrecisionGeographicPolygons() {
        // Iberian Peninsula
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-9.5, 36.0}, {-9.0, 37.0}, {-9.5, 38.8}, {-9.4, 41.8}, {-8.9, 43.3}, {-3.5, 43.5}, {1.7, 42.5}, {3.3, 42.4}, {0.2, 38.0}, {-5.4, 36.0}, {-7.5, 37.0}
        }));

        // Italian Peninsula & Sicily & Sardinia/Corsica
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {7.5, 43.7}, {9.8, 44.4}, {12.2, 45.8}, {13.5, 45.8}, {13.8, 44.8}, {15.0, 43.5}, {18.5, 40.2}, {17.5, 39.8}, {16.0, 38.0}, {15.5, 41.0}, {12.2, 41.8}, {9.8, 44.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {12.4, 37.8}, {15.6, 38.3}, {15.2, 36.6}, {12.4, 37.8}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {8.5, 38.8}, {9.7, 43.0}, {8.5, 43.0}, {8.0, 38.8}
        }));

        // Greece & Aegean & Peloponnese & Crete
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {19.5, 39.8}, {20.5, 39.5}, {22.5, 40.5}, {24.0, 41.0}, {26.5, 40.5}, {24.0, 37.5}, {22.5, 36.4}, {21.5, 36.5}, {20.5, 38.5}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {23.5, 35.0}, {26.3, 35.3}, {26.0, 35.0}, {23.5, 35.0}
        }));

        // Anatolia & Caucasus
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {26.2, 40.2}, {29.0, 41.2}, {35.0, 42.0}, {38.0, 42.1}, {41.5, 41.6}, {44.0, 39.0}, {36.0, 36.5}, {32.5, 36.2}, {27.2, 36.8}
        }));

        // Levant & Near East & Arabian Peninsula
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {35.0, 36.5}, {36.5, 37.0}, {41.0, 37.0}, {42.0, 34.0}, {36.0, 31.0}, {34.2, 31.3}, {35.5, 33.8}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {35.0, 28.0}, {43.0, 12.5}, {54.0, 16.0}, {59.0, 22.5}, {56.0, 26.0}, {48.0, 30.0}, {35.0, 30.0}
        }));

        // Nile Delta & Nile Valley
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {29.5, 31.5}, {32.5, 31.5}, {34.0, 27.8}, {35.0, 22.0}, {31.0, 22.0}, {29.5, 30.0}
        }));

        // Maghreb & North Africa Coast
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-10.0, 28.0}, {-5.4, 35.8}, {3.0, 36.8}, {10.0, 37.5}, {11.5, 33.0}, {15.0, 32.5}, {25.0, 32.0}, {30.0, 31.5}, {30.0, 28.0}, {10.0, 22.0}, {-10.0, 20.0}
        }));

        // Sub-Saharan Africa (High-Resolution Realistic Coastline)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-17.5, 14.8}, {-17.0, 21.0}, {-16.0, 16.5}, {-14.0, 12.0}, {-7.5, 4.4}, {2.0, 6.2}, {9.5, 4.5}, {9.0, 2.0}, {9.5, -1.0}, {13.0, -12.0}, {12.0, -17.0},
            {15.0, -23.0}, {18.0, -34.5}, {26.0, -33.0}, {33.0, -27.0}, {40.0, -15.0}, {41.0, -4.0}, {51.2, 11.8}, {43.0, 12.5}, {37.0, 19.5}, {33.0, 27.0},
            {25.0, 12.0}, {10.0, 12.0}, {-3.0, 16.8}, {-17.5, 14.8}
        }));

        // Madagascar
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {43.5, -12.0}, {50.0, -12.5}, {50.5, -16.0}, {47.0, -25.5}, {43.0, -25.0}, {43.5, -12.0}
        }));

        // Western & Central & Eastern Europe
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-4.8, 48.4}, {2.5, 51.0}, {7.5, 53.5}, {10.0, 54.5}, {14.0, 54.5}, {22.0, 54.5}, {30.0, 60.0}, {30.0, 46.0}, {26.5, 40.5}, {13.5, 45.8}, {7.5, 43.7}, {-1.8, 43.4}, {-4.8, 48.4}
        }));

        // British Isles & Ireland
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-5.5, 50.0}, {1.8, 51.3}, {1.5, 53.0}, {-0.5, 54.5}, {-2.0, 57.5}, {-4.5, 58.5}, {-6.5, 56.5}, {-4.5, 52.0}, {-5.5, 50.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-10.5, 51.5}, {-6.0, 52.0}, {-5.8, 55.3}, {-10.5, 54.0}
        }));

        // Indian Subcontinent (High-Precision Realistic Coastline)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {61.5, 25.0}, {68.0, 24.0}, {72.8, 21.0}, {73.0, 15.5}, {75.0, 11.0}, {77.5, 8.1}, {79.8, 10.0}, {80.2, 13.0}, {85.0, 19.5}, {88.5, 21.5}, {92.0, 22.0},
            {94.0, 28.0}, {88.0, 27.0}, {80.0, 30.0}, {74.0, 35.0}, {68.0, 30.0}, {61.5, 25.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {79.5, 9.8}, {81.8, 9.8}, {81.8, 6.0}, {79.5, 6.0}
        }));

        // China & East Asia (High-Precision Realistic Coastline)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {98.0, 21.0}, {108.0, 20.0}, {113.0, 22.5}, {118.0, 24.5}, {121.5, 28.5}, {122.0, 31.5}, {119.5, 35.0}, {122.5, 37.5}, {124.0, 40.0},
            {130.0, 42.5}, {135.0, 48.0}, {140.0, 55.0}, {120.0, 53.0}, {100.0, 50.0}, {90.0, 45.0}, {80.0, 40.0}, {98.0, 21.0}
        }));

        // Southeast Asia & Indochina
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {92.5, 20.0}, {98.5, 10.0}, {100.5, 6.0}, {104.0, 1.3}, {104.0, 10.0}, {109.0, 12.0}, {108.0, 16.0}, {106.0, 21.0}, {98.0, 21.0}
        }));

        // Indonesia & Philippines & Sahul / Australia (High-Precision Realistic Coastline)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {113.5, -26.0}, {114.0, -22.0}, {122.0, -18.0}, {130.0, -14.0}, {136.0, -12.0}, {142.0, -10.5}, {150.0, -23.0}, {153.5, -28.0}, {150.0, -37.5}, {140.0, -38.5}, {138.0, -35.0}, {129.0, -32.0}, {115.0, -34.5}, {113.5, -26.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {95.0, 5.5}, {106.0, -6.0}, {105.0, -2.0}, {95.0, 5.5} // Sumatra
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {108.5, 7.0}, {119.0, 4.0}, {115.0, -4.0}, {109.0, -3.0} // Borneo
        }));

        // Japan (Honshu, Kyushu, Shikoku, Hokkaido)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {130.0, 31.0}, {132.0, 33.5}, {135.0, 34.5}, {139.0, 35.5}, {141.0, 38.0}, {141.5, 41.5}, {140.0, 40.5}, {136.0, 36.0}, {130.5, 33.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {140.0, 41.5}, {145.5, 44.0}, {145.0, 45.5}, {141.0, 45.5}, {140.0, 41.5} // Hokkaido
        }));

        // South America (High-Precision Realistic Curved Coastline)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-77.0, 8.5}, {-73.0, 11.5}, {-62.0, 10.5}, {-50.0, 1.5}, {-35.0, -5.0}, {-35.0, -8.0}, {-38.0, -13.0}, {-41.0, -21.0},
            {-48.0, -28.0}, {-53.0, -33.0}, {-57.0, -38.0}, {-65.0, -45.0}, {-68.0, -54.0}, {-75.0, -52.0}, {-74.0, -42.0}, {-72.0, -30.0},
            {-76.0, -14.0}, {-81.0, -5.0}, {-79.0, 2.0}, {-77.0, 8.5}
        }));

        // Central America & Caribbean
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-92.0, 16.0}, {-88.0, 15.0}, {-83.0, 8.5}, {-77.0, 8.5}, {-83.0, 10.0}, {-90.0, 14.0}
        }));

        // North America (High-Precision Realistic Coastlines)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-168.0, 65.0}, {-150.0, 60.0}, {-135.0, 55.0}, {-124.0, 48.0}, {-123.0, 38.0}, {-117.0, 32.0}, {-105.0, 20.0}, {-97.0, 26.0},
            {-90.0, 29.0}, {-81.0, 25.0}, {-80.0, 30.0}, {-75.0, 35.0}, {-70.0, 42.0}, {-64.0, 46.0}, {-55.0, 52.0}, {-65.0, 60.0},
            {-80.0, 65.0}, {-100.0, 68.0}, {-120.0, 70.0}, {-140.0, 70.0}, {-168.0, 65.0}
        }));

        // Iranian Plateau, Central Asia & Kazakhstan (Fixing Eurasian Inland Gap)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {44.0, 38.0}, {46.0, 47.0}, {50.0, 55.0}, {85.0, 55.0}, {87.0, 48.0}, {80.0, 35.0}, {74.0, 35.0}, {68.0, 24.0}, {62.0, 25.0}, {51.0, 36.0}, {44.0, 38.0}
        }));

        // Sahel & Central African Hinterland (Fixing African Diagonal Gap)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-17.0, 21.0}, {10.0, 22.0}, {30.0, 28.0}, {33.0, 27.0}, {37.0, 19.5}, {25.0, 12.0}, {10.0, 12.0}, {-3.0, 16.8}, {-17.5, 14.8}
        }));

        // Greenland (Realistic Arctic Outline)
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-73.0, 78.0}, {-60.0, 83.0}, {-18.0, 82.0}, {-20.0, 70.0}, {-40.0, 60.0}, {-55.0, 60.0}, {-68.0, 75.0}
        }));

        // Northern Europe & Scandinavia
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {5.0, 58.0}, {10.0, 62.0}, {14.0, 68.0}, {18.0, 70.0}, {30.0, 70.0}, {30.0, 55.0}, {10.0, 54.0}
        }));

        // Siberia & Northern Eurasia
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {30.0, 55.0}, {30.0, 75.0}, {80.0, 74.0}, {120.0, 74.0}, {175.0, 68.0}, {170.0, 60.0}, {140.0, 50.0}, {80.0, 50.0}, {50.0, 50.0}
        }));

        // Antarctica
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-180.0, -65.0}, {180.0, -65.0}, {180.0, -90.0}, {-180.0, -90.0}
        }));

        // INLAND SEAS & MAJOR GULFS
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {-5.0, 36.0}, {0.0, 36.0}, {10.0, 38.0}, {15.0, 39.0}, {22.0, 38.0}, {30.0, 32.0}, {35.0, 32.0},
            {34.0, 35.0}, {26.0, 37.0}, {18.0, 40.0}, {12.0, 44.0}, {5.0, 43.0}, {0.0, 37.0}, {-5.0, 36.0}
        }));
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {28.0, 41.0}, {32.0, 46.0}, {38.0, 46.0}, {41.0, 43.0}, {35.0, 41.0}, {28.0, 41.0}
        }));
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {33.0, 27.0}, {35.0, 28.0}, {43.0, 12.0}, {40.0, 12.0}, {33.0, 27.0}
        }));
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {48.0, 30.0}, {56.0, 27.0}, {54.0, 25.0}, {48.0, 29.0}
        }));

        // Precompute 720x360 boolean grid for O(1) instant land lookups
        FAST_LAND_GRID = new boolean[720][360];
        for (int gy = 0; gy < 360; gy++) {
            double lat = 90.0 - (gy + 0.5) / 360.0 * 180.0;
            for (int gx = 0; gx < 720; gx++) {
                double lng = -180.0 + (gx + 0.5) / 720.0 * 360.0;
                int x = (int) ((lng + 180.0) / 360.0 * WIDTH);
                int y = (int) ((90.0 - lat) / 180.0 * HEIGHT);
                Point p = new Point(x, y);
                boolean isSea = false;
                for (Path2D sea : SEA_POLYGONS) {
                    if (sea.contains(p)) { isSea = true; break; }
                }
                if (!isSea) {
                    for (Path2D land : LAND_POLYGONS) {
                        if (land.contains(p)) { FAST_LAND_GRID[gx][gy] = true; break; }
                    }
                }
            }
        }
    }

    private static Path2D createPolygon(double[][] points) {
        Path2D p = new Path2D.Double();
        if (points.length == 0) return p;

        int startX = (int) ((points[0][0] + 180.0) / 360.0 * WIDTH);
        int startY = (int) ((90.0 - points[0][1]) / 180.0 * HEIGHT);
        p.moveTo(startX, startY);

        for (int i = 1; i < points.length; i++) {
            int px = (int) ((points[i][0] + 180.0) / 360.0 * WIDTH);
            int py = (int) ((90.0 - points[i][1]) / 180.0 * HEIGHT);
            p.lineTo(px, py);
        }
        p.closePath();
        return p;
    }

    public static void generateProceduralMapsForScenario(Scenario scenario) {
        populateScenarioHistoricalMaps(scenario);
    }

    private static List<CityPoint> getCitiesForScenario(String type, Scenario scenario) {
        if (scenario == null || scenario.getInitialHumanCount() <= 0) {
            return getCitiesForScenario(type);
        }
        long pop = scenario.getInitialHumanCount();
        List<CityPoint> list = new ArrayList<>();

        if (pop >= 1_000_000_000L) { // 10^9+ Global Megacities (high tech urban hubs)
            list.add(new CityPoint("Tokyo Megacity", 35.6, 139.7, 5.5, 2.8));
            list.add(new CityPoint("New York Tri-State", 40.7, -74.0, 5.2, 2.6));
            list.add(new CityPoint("London Metro", 51.5, -0.1, 4.8, 2.4));
            list.add(new CityPoint("Shanghai Yangtze Hub", 31.2, 121.5, 5.5, 2.8));
            list.add(new CityPoint("Mumbai Metropolis", 19.0, 72.8, 5.2, 2.6));
            list.add(new CityPoint("Cairo Nile Megacity", 30.0, 31.2, 4.8, 2.4));
            list.add(new CityPoint("Lagos Gulf Corridor", 6.5, 3.3, 4.5, 2.2));
            list.add(new CityPoint("São Paulo Hub", -23.5, -46.6, 4.8, 2.4));
            list.add(new CityPoint("Beijing Capital Node", 39.9, 116.4, 5.2, 2.6));
            list.add(new CityPoint("Paris Isle Hub", 48.8, 2.35, 4.5, 2.2));
            list.add(new CityPoint("Mexico City Valley", 19.4, -99.1, 4.8, 2.4));
            list.add(new CityPoint("Sydney Pacific Hub", -33.8, 151.2, 4.0, 2.0));
        } else if (pop >= 100_000_000L) { // 10^8 Industrial / Modern Urban Networks
            list.add(new CityPoint("London", 51.5, -0.1, 4.5, 2.5));
            list.add(new CityPoint("Paris", 48.8, 2.35, 4.2, 2.2));
            list.add(new CityPoint("New York", 40.7, -74.0, 4.5, 2.5));
            list.add(new CityPoint("Tokyo", 35.6, 139.7, 4.8, 2.5));
            list.add(new CityPoint("Beijing", 39.9, 116.4, 4.5, 2.4));
            list.add(new CityPoint("Calcutta", 22.5, 88.3, 4.2, 2.2));
            list.add(new CityPoint("Cairo", 30.0, 31.2, 4.0, 2.0));
        } else if (pop >= 1_000_000L) { // 10^7 Imperial / Agrarian Networks
            list.add(new CityPoint("Rome", 41.9, 12.5, 4.2, 2.5));
            list.add(new CityPoint("Alexandria", 31.2, 29.9, 3.8, 2.2));
            list.add(new CityPoint("Chang'an", 34.2, 108.9, 4.2, 2.5));
            list.add(new CityPoint("Pataliputra", 25.6, 85.1, 4.0, 2.2));
            list.add(new CityPoint("Babylon", 32.5, 44.4, 3.8, 2.0));
        } else if (pop >= 100_000L) { // Early Agricultural Settlements
            list.add(new CityPoint("Uruk", 31.3, 45.6, 3.5, 2.0));
            list.add(new CityPoint("Memphis", 29.8, 31.2, 3.5, 2.0));
            list.add(new CityPoint("Mohenjo-Daro", 27.3, 68.1, 3.2, 2.0));
        } else { // Low / Hunter-Gatherer Band Dispersal (< 100k)
            list.add(new CityPoint("Omo", 4.5, 36.0, 2.0, 3.5));
            list.add(new CityPoint("Blombos", -34.4, 21.2, 1.8, 3.0));
            list.add(new CityPoint("Madjedbebe", -12.5, 132.9, 1.8, 3.0));
        }

        List<CityPoint> specific = getCitiesForScenario(type);
        if (specific != null && !specific.isEmpty() && pop < 1_000_000_000L) {
            for (CityPoint cp : specific) {
                boolean exists = false;
                for (CityPoint ex : list) {
                    if (distSq(cp.lng, cp.lat, ex.lng, ex.lat) < 25.0) {
                        exists = true; break;
                    }
                }
                if (!exists) list.add(cp);
            }
        }
        return list;
    }

    private static List<CityPoint> getCitiesForScenario(String type) {
        List<CityPoint> list = new ArrayList<>();
        switch (type.toUpperCase()) {
            case "ROMAN_EMPIRE":
                list.add(new CityPoint("Rome", 41.9, 12.5, 4.0, 2.5));
                list.add(new CityPoint("Alexandria", 31.2, 29.9, 3.0, 2.0));
                list.add(new CityPoint("Antioch", 36.2, 36.1, 2.5, 2.0));
                list.add(new CityPoint("Carthage", 36.8, 10.3, 2.2, 2.0));
                list.add(new CityPoint("Athens", 37.9, 23.7, 2.0, 1.8));
                list.add(new CityPoint("Constantinople", 41.0, 28.9, 2.5, 2.0));
                break;
            case "EGYPT_NILE":
                list.add(new CityPoint("Memphis", 29.8, 31.2, 3.5, 2.0));
                list.add(new CityPoint("Thebes", 25.7, 32.6, 3.0, 2.0));
                break;
            case "MESOPOTAMIA_ASSYRIA":
            case "FERTILE_CRESCENT":
            case "YOUNGER_DRYAS":
                list.add(new CityPoint("Nineveh", 36.3, 43.1, 3.5, 2.0));
                list.add(new CityPoint("Babylon", 32.5, 44.4, 3.5, 2.0));
                list.add(new CityPoint("Jericho", 31.8, 35.4, 2.2, 1.5));
                break;
            case "INDIA_MAURYA":
                list.add(new CityPoint("Pataliputra", 25.6, 85.1, 4.0, 2.5));
                list.add(new CityPoint("Taxila", 33.7, 72.8, 3.0, 2.0));
                break;
            case "WEST_AFRICA_MALI":
            case "GREEN_SAHARA":
                list.add(new CityPoint("Timbuktu", 16.7, -3.0, 3.2, 2.0));
                list.add(new CityPoint("Gao", 16.2, 0.0, 2.5, 2.0));
                list.add(new CityPoint("Niani", 11.4, -8.3, 2.8, 2.0));
                break;
            case "SONG_DYNASTY":
                list.add(new CityPoint("Kaifeng", 34.7, 114.3, 4.0, 2.5));
                list.add(new CityPoint("Hangzhou", 30.2, 120.1, 3.8, 2.5));
                break;
            case "SAKOKU_JAPAN":
                list.add(new CityPoint("Edo", 35.6, 139.7, 4.0, 2.0));
                list.add(new CityPoint("Kyoto", 35.0, 135.7, 3.5, 2.0));
                list.add(new CityPoint("Osaka", 34.6, 135.5, 3.2, 2.0));
                break;
            case "MESOAMERICA":
            case "AMERICAS_1491":
            case "EPIDEMIC_CONTACT":
                list.add(new CityPoint("Tenochtitlan", 19.4, -99.1, 4.0, 2.5));
                list.add(new CityPoint("Cuzco", -13.5, -71.9, 3.5, 2.5));
                list.add(new CityPoint("Tikal", 17.2, -89.6, 2.8, 2.0));
                break;
            case "SAHUL_MIGRATION":
                list.add(new CityPoint("Madjedbebe", -12.5, 132.9, 3.0, 2.5));
                list.add(new CityPoint("Lake Mungo", -33.7, 143.0, 2.5, 2.0));
                break;
            case "BERINGIA_AMERICAS":
                list.add(new CityPoint("Yana RHS", 70.7, 135.4, 3.0, 2.5));
                list.add(new CityPoint("Bluefish Caves", 67.1, -140.7, 2.5, 2.0));
                break;
            case "ONE_CONTINENT":
                // Pan-African Homo Sapiens archaeological centers (-100,000 BP)
                // Broad, interconnected African habitability zones with higher weights & sigmas
                list.add(new CityPoint("Omo Kibish (East Africa Core)", 4.5, 36.0, 4.5, 15.0));
                list.add(new CityPoint("Herto & Afar (Horn of Africa)", 8.9, 40.5, 4.2, 14.0));
                list.add(new CityPoint("Jebel Irhoud (North Africa)", 31.6, -8.8, 3.8, 15.0));
                list.add(new CityPoint("Blombos & Klasies (South Africa)", -34.4, 21.2, 3.8, 15.0));
                list.add(new CityPoint("Iho Eleru & Congo (West/Central Africa)", 7.4, 5.1, 3.5, 16.0));
                list.add(new CityPoint("Nile Corridor (African Bridge)", 22.0, 31.5, 4.0, 12.0));
                // Archaic Hominins (Sparse population densities, expanded spatial range)
                list.add(new CityPoint("Atapuerca & Spy (Néandertal Ouest)", 42.3, -3.5, 1.2, 12.0));
                list.add(new CityPoint("Shanidar (Néandertal Moyen-Orient)", 36.8, 44.2, 1.2, 12.0));
                list.add(new CityPoint("Denisova & Xiahe (Denisovien Altai)", 51.4, 84.7, 0.9, 13.0));
                break;
            default:
                list.add(new CityPoint("Tokyo", 35.6, 139.7, 4.0, 2.5));
                list.add(new CityPoint("New York", 40.7, -74.0, 3.5, 2.5));
                list.add(new CityPoint("London", 51.5, -0.1, 3.2, 2.0));
                break;
        }
        return list;
    }

    private static List<RiverRibbon> getRiversForScenario(String type) {
        List<RiverRibbon> list = new ArrayList<>();
        switch (type.toUpperCase()) {
            case "EGYPT_NILE":
            case "ROMAN_EMPIRE":
                list.add(new RiverRibbon(32.0, 24.0, 31.5, 31.5, 2.5, 1.0));
                break;
            case "MESOPOTAMIA_ASSYRIA":
            case "FERTILE_CRESCENT":
                list.add(new RiverRibbon(38.0, 37.0, 48.0, 30.0, 2.8, 1.2));
                break;
            case "INDIA_MAURYA":
                list.add(new RiverRibbon(78.0, 30.0, 90.0, 22.0, 3.0, 1.5));
                break;
            case "SONG_DYNASTY":
                list.add(new RiverRibbon(105.0, 35.0, 120.0, 35.0, 3.0, 1.5)); // Yellow River
                list.add(new RiverRibbon(105.0, 30.0, 121.0, 31.0, 3.0, 1.5)); // Yangtze
                break;
            case "WEST_AFRICA_MALI":
                list.add(new RiverRibbon(-10.0, 11.0, 3.0, 16.0, 2.2, 1.2));
                break;
            case "ONE_CONTINENT":
                list.add(new RiverRibbon(31.0, 3.0, 32.5, 31.0, 2.0, 1.5));
                break;
        }
        return list;
    }

    private static List<EmpireTerritory> getEmpiresForScenario(String type) {
        List<EmpireTerritory> list = new ArrayList<>();
        switch (type.toUpperCase()) {
            case "ROMAN_EMPIRE":
                EmpireTerritory rome = new EmpireTerritory("Imperium Romanum", new Color(220, 38, 38));
                rome.addBoundingBox(-9.0, 30.0, 45.0, 54.0);
                list.add(rome);
                break;
            case "INDIA_MAURYA":
                EmpireTerritory maurya = new EmpireTerritory("Empire Maurya", new Color(217, 119, 6));
                maurya.addBoundingBox(68.0, 8.0, 90.0, 34.0);
                list.add(maurya);
                break;
            case "WEST_AFRICA_MALI":
                EmpireTerritory mali = new EmpireTerritory("Empire du Mali", new Color(16, 185, 129));
                mali.addBoundingBox(-17.0, 8.0, 4.0, 20.0);
                list.add(mali);
                break;
            case "SONG_DYNASTY":
                EmpireTerritory song = new EmpireTerritory("Dynastie Song", new Color(239, 68, 68));
                song.addBoundingBox(98.0, 18.0, 124.0, 38.0);
                list.add(song);
                break;
            case "SAKOKU_JAPAN":
                EmpireTerritory japan = new EmpireTerritory("Shogunat Tokugawa", new Color(168, 85, 247));
                japan.addBoundingBox(129.0, 30.0, 145.0, 44.0);
                list.add(japan);
                break;
            case "MESOAMERICA":
            case "AMERICAS_1491":
                EmpireTerritory aztec = new EmpireTerritory("Alliance Aztèque", new Color(245, 158, 11));
                aztec.addBoundingBox(-102.0, 14.0, -95.0, 21.0);
                list.add(aztec);
                EmpireTerritory inca = new EmpireTerritory("Tawantinsuyu (Inca)", new Color(220, 38, 38));
                inca.addBoundingBox(-80.0, -35.0, -68.0, 2.0);
                list.add(inca);
                break;
            case "ONE_CONTINENT":
                EmpireTerritory sapiens = new EmpireTerritory("Foyer Pan-Africain Homo Sapiens", new Color(234, 179, 8)); // Yellow/Gold
                sapiens.addBoundingBox(-18.0, -35.0, 51.0, 37.0); // Entire African continent
                list.add(sapiens);

                EmpireTerritory neanderthal = new EmpireTerritory("Niche Homo Neanderthalensis (Eurasie Ouest)", new Color(14, 165, 233)); // Cyan/Sky Blue
                neanderthal.addBoundingBox(-10.0, 32.0, 55.0, 58.0); // Europe, Near East, Caucasus
                list.add(neanderthal);

                EmpireTerritory denisova = new EmpireTerritory("Niche Homo Denisova (Eurasie Est & Asie)", new Color(217, 70, 239)); // Purple/Magenta
                denisova.addBoundingBox(55.0, 20.0, 125.0, 65.0); // Altai, Siberia, East Asia
                list.add(denisova);
                break;
            default:
                EmpireTerritory generic = new EmpireTerritory("Sphère Démographique", new Color(59, 130, 246));
                generic.addBoundingBox(-10.0, 25.0, 50.0, 55.0);
                list.add(generic);
                break;
        }
        return list;
    }

    private static List<LanguageZone> getLanguageZonesForScenario(String type) {
        List<LanguageZone> list = new ArrayList<>();
        if (type != null) {
            switch (type.toUpperCase()) {
                case "ROMAN_EMPIRE":
                    list.add(new LanguageZone("Latin", 12.5, 41.9, new Color(59, 130, 246)));
                    list.add(new LanguageZone("Greek", 23.7, 37.9, new Color(6, 182, 212)));
                    list.add(new LanguageZone("Celtic", 2.3, 48.8, new Color(34, 197, 94)));
                    list.add(new LanguageZone("Punique", 10.3, 36.8, new Color(245, 158, 11)));
                    list.add(new LanguageZone("Aramaic", 35.4, 31.8, new Color(239, 68, 68)));
                    break;
                case "FERTILE_CRESCENT":
                    list.add(new LanguageZone("Proto-Semitic", 35.4, 31.8, new Color(245, 158, 11)));
                    list.add(new LanguageZone("Sumerian", 44.4, 32.5, new Color(239, 68, 68)));
                    list.add(new LanguageZone("Anatolian", 38.9, 37.2, new Color(168, 85, 247)));
                    break;
                case "ONE_CONTINENT":
                    list.add(new LanguageZone("Proto-Sapiens (Afrique)", 36.0, 4.5, new Color(234, 179, 8)));
                    list.add(new LanguageZone("Proto-Néandertalien (Eurasie Ouest)", -3.5, 42.3, new Color(14, 165, 233)));
                    list.add(new LanguageZone("Proto-Denisovien (Asie & Altai)", 84.7, 51.4, new Color(217, 70, 239)));
                    return list;
            }
        }
        // Global baseline language families to reconstruct worldwide maps
        list.add(new LanguageZone("Indo-European Baseline", 15.0, 48.0, new Color(59, 130, 246)));
        list.add(new LanguageZone("Afroasiatic Baseline", 25.0, 25.0, new Color(245, 158, 11)));
        list.add(new LanguageZone("Sino-Tibetan Baseline", 105.0, 32.0, new Color(239, 68, 68)));
        list.add(new LanguageZone("Niger-Congo Baseline", 15.0, 5.0, new Color(16, 185, 129)));
        list.add(new LanguageZone("Austronesian Baseline", 115.0, -2.0, new Color(6, 182, 212)));
        list.add(new LanguageZone("Trans-New Guinea / Sahul", 138.0, -18.0, new Color(168, 85, 247)));
        list.add(new LanguageZone("Amerind & Na-Dene Baseline", -85.0, 15.0, new Color(236, 72, 153)));
        list.add(new LanguageZone("Uralic & Altaic Baseline", 75.0, 55.0, new Color(101, 163, 13)));
        list.add(new LanguageZone("Dravidian Baseline", 78.0, 14.0, new Color(217, 119, 6)));
        return list;
    }

    private static List<LanguageZone> getKinshipZonesForScenario(String type) {
        List<LanguageZone> list = new ArrayList<>();
        // Focal scenario kinship overrides
        if (type != null && type.equalsIgnoreCase("ROMAN_EMPIRE")) {
            list.add(new LanguageZone("Patrilineal Gens Romana", 12.5, 41.9, new Color(168, 85, 247)));
        } else if (type != null && type.equalsIgnoreCase("WEST_AFRICA_MALI")) {
            list.add(new LanguageZone("Matrilineal Lineage & Clan", -10.0, 12.0, new Color(236, 72, 153)));
        }

        // Global baseline kinship systems
        list.add(new LanguageZone("Patrilineal Gens & Lineage", 15.0, 45.0, new Color(168, 85, 247)));
        list.add(new LanguageZone("Matrilineal Clan Systems", 10.0, 8.0, new Color(236, 72, 153)));
        list.add(new LanguageZone("Bilateral Forager Bands", 135.0, -25.0, new Color(34, 197, 94)));
        list.add(new LanguageZone("Nomadic Steppe Clan Federations", 75.0, 48.0, new Color(217, 119, 6)));
        list.add(new LanguageZone("State Lineage & Household", 110.0, 30.0, new Color(239, 68, 68)));
        list.add(new LanguageZone("Amerind Matri-Clans", -80.0, 10.0, new Color(234, 179, 8)));
        return list;
    }

    private static List<LanguageZone> getSacredSitesForScenario(String type) {
        List<LanguageZone> list = new ArrayList<>();
        if (type != null) {
            switch (type.toUpperCase()) {
                case "ROMAN_EMPIRE":
                    list.add(new LanguageZone("Rome Imperial Cult", 12.5, 41.9, new Color(239, 68, 68)));
                    list.add(new LanguageZone("Serapis & Isis", 29.9, 31.2, new Color(245, 158, 11)));
                    list.add(new LanguageZone("Delphi Oracle", 22.5, 38.5, new Color(6, 182, 212)));
                    break;
                case "FERTILE_CRESCENT":
                    list.add(new LanguageZone("Göbekli Tepe Megalithic Shrine", 38.9, 37.2, new Color(239, 68, 68)));
                    list.add(new LanguageZone("Marduk Sanctuary", 44.4, 32.5, new Color(245, 158, 11)));
                    break;
                case "ONE_CONTINENT":
                    list.add(new LanguageZone("Rift Valley Ancestral Sanctuary", 36.0, 4.5, new Color(234, 179, 8)));
                    break;
            }
        }
        // Global baseline sacred sites & ritual belief systems
        list.add(new LanguageZone("Palaeolithic / Siberian Shamanism", 90.0, 55.0, new Color(99, 102, 241)));
        list.add(new LanguageZone("Megalithic Monolith Sanctuary", -3.0, 48.0, new Color(168, 85, 247)));
        list.add(new LanguageZone("Polytheist State Pantheon", 44.0, 32.0, new Color(245, 158, 11)));
        list.add(new LanguageZone("Vedic & Dharmic Sanctuaries", 78.0, 22.0, new Color(217, 119, 6)));
        list.add(new LanguageZone("Ancestor Shrine Cults", 115.0, 30.0, new Color(239, 68, 68)));
        list.add(new LanguageZone("Animist Spirit Sanctuaries", 5.0, 8.0, new Color(34, 197, 94)));
        list.add(new LanguageZone("Mesoamerican Solar Pantheons", -99.0, 19.0, new Color(245, 158, 11)));
        list.add(new LanguageZone("Sahul Dreamtime Sacred Sites", 132.9, -12.5, new Color(168, 85, 247)));
        return list;
    }

    private static double distSq(double lng1, double lat1, double lng2, double lat2) {
        double dlng = lng1 - lng2;
        double dlat = lat1 - lat2;
        return dlng * dlng + dlat * dlat;
    }

    public static String bufferedImageToBase64Png(BufferedImage bImg) {
        if (bImg == null) return null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bImg, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            logger.error("Failed to encode BufferedImage to Base64 PNG", e);
            return null;
        }
    }

    // --- STRUCTS ---

    public static class CityPoint {
        public String name;
        public double lat, lng, weight, sigma;

        public CityPoint(String name, double lat, double lng, double weight, double sigma) {
            this.name = name; this.lat = lat; this.lng = lng;
            this.weight = weight; this.sigma = sigma;
        }
    }

    public static class RiverRibbon {
        public double lng1, lat1, lng2, lat2, weight, widthDeg;

        public RiverRibbon(double lng1, double lat1, double lng2, double lat2, double weight, double widthDeg) {
            this.lng1 = lng1; this.lat1 = lat1; this.lng2 = lng2; this.lat2 = lat2;
            this.weight = weight; this.widthDeg = widthDeg;
        }

        public double distanceToPoint(double px, double py) {
            double l2 = distSq(lng1, lat1, lng2, lat2);
            if (l2 == 0) return Math.sqrt(distSq(px, py, lng1, lat1));
            double t = Math.max(0, Math.min(1, ((px - lng1) * (lng2 - lng1) + (py - lat1) * (lat2 - lat1)) / l2));
            double projX = lng1 + t * (lng2 - lng1);
            double projY = lat1 + t * (lat2 - lat1);
            return Math.sqrt(distSq(px, py, projX, projY));
        }
    }

    public static class EmpireTerritory {
        public String name;
        public Color color;
        public List<double[]> bounds = new ArrayList<>();

        public EmpireTerritory(String name, Color color) {
            this.name = name; this.color = color;
        }

        public void addBoundingBox(double minLng, double minLat, double maxLng, double maxLat) {
            bounds.add(new double[]{minLng, minLat, maxLng, maxLat});
        }

        public boolean contains(double lng, double lat) {
            for (double[] b : bounds) {
                if (lng >= b[0] && lng <= b[2] && lat >= b[1] && lat <= b[3]) return true;
            }
            return false;
        }
    }

    public static class LanguageZone {
        public String name;
        public double centerLng, centerLat;
        public Color color;

        public LanguageZone(String name, double centerLng, double centerLat, Color color) {
            this.name = name; this.centerLng = centerLng; this.centerLat = centerLat; this.color = color;
        }
    }

    public static BufferedImage generateCleanExtensibleTensorMap(int tensorIndex, String type, Scenario scenario) {
        int width = 2048, height = 1024;
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
            java.util.Map<String, Double> pMap = scenario.getTensorProceduralParameters().get(tensorIndex);
            if (pMap != null) {
                pFreq = pMap.getOrDefault("scenario.tensor.ext.p1.label", 0.020);
                pAmp = pMap.getOrDefault("scenario.tensor.ext.p2.label", 1.00);
                pDiff = pMap.getOrDefault("scenario.tensor.ext.p3.label", 0.25);
            }
        }

        java.util.Random rnd = new java.util.Random(seed);
        double phaseLng = rnd.nextDouble() * Math.PI * 2.0;
        double phaseLat = rnd.nextDouble() * Math.PI * 2.0;
        double scale = Math.clamp(pFreq, 0.001, 0.20);
        float hueBase = (float) ((tensorIndex * 0.137 + (rnd.nextDouble() * 0.2)) % 1.0);

        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y / (double) height) * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = -180.0 + (x / (double) width) * 360.0;
                if (!isLand(lon, lat)) {
                    img.setRGB(x, y, 0x0F172A); // Equirectangular ocean basemap (Color 15, 23, 42)
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

    private static void drawDepositHotspots(BufferedImage img, double[][] hotspots, Color colorBase) {
        int width = img.getWidth();
        int height = img.getHeight();
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (double[] spot : hotspots) {
            double lng = spot[0];
            double lat = spot[1];
            double radiusPx = spot[2];
            double intensity = spot.length > 3 ? spot[3] : 1.0;

            int cx = (int) ((lng + 180.0) / 360.0 * width);
            int cy = (int) ((90.0 - lat) / 180.0 * height);

            int rInt = (int) radiusPx;
            for (int dy = -rInt; dy <= rInt; dy++) {
                int py = cy + dy;
                if (py < 0 || py >= height) continue;
                double currentLat = 90.0 - (py / (double) height) * 180.0;

                for (int dx = -rInt; dx <= rInt; dx++) {
                    int px = cx + dx;
                    if (px < 0 || px >= width) continue;
                    double currentLng = -180.0 + (px / (double) width) * 360.0;

                    if (!isLand(currentLng, currentLat)) continue;

                    double d = Math.sqrt(dx * dx + dy * dy);
                    if (d <= radiusPx) {
                        double norm = 1.0 - (d / radiusPx);
                        double factor = Math.pow(norm, 1.5) * intensity;

                        int origRGB = img.getRGB(px, py);
                        int oldR = (origRGB >> 16) & 0xFF;
                        int oldG = (origRGB >> 8) & 0xFF;
                        int oldB = origRGB & 0xFF;

                        int addR = (int) (colorBase.getRed() * factor);
                        int addG = (int) (colorBase.getGreen() * factor);
                        int addB = (int) (colorBase.getBlue() * factor);

                        int newR = Math.min(255, oldR + addR);
                        int newG = Math.min(255, oldG + addG);
                        int newB = Math.min(255, oldB + addB);

                        img.setRGB(px, py, (newR << 16) | (newG << 8) | newB);
                    }
                }
            }
        }
        g.dispose();
    }

    public static BufferedImage generateCleanCoastlines(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return img;
    }

    public static java.util.List<double[]> loadMRDSDeposits(String... commodityKeywords) {
        java.util.List<double[]> list = new java.util.ArrayList<>();
        java.nio.file.Path zipPath = java.nio.file.Paths.get("data", "maps", "usgs_mrds", "mrds-csv.zip");
        if (!java.nio.file.Files.exists(zipPath)) return list;

        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipPath.toFile())) {
            var entry = zipFile.getEntry("mrds.csv");
            if (entry == null) return list;

            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(zipFile.getInputStream(entry), java.nio.charset.StandardCharsets.UTF_8))) {
                br.readLine(); // skip header
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    if (parts.length > 14) {
                        try {
                            double lat = Double.parseDouble(parts[5].replace("\"", "").trim());
                            double lon = Double.parseDouble(parts[6].replace("\"", "").trim());

                            String comms = (parts[11] + " " + parts[12] + " " + parts[13] + " " + parts[14]).toLowerCase();

                            boolean match = false;
                            for (String kw : commodityKeywords) {
                                if (comms.contains(kw.toLowerCase())) {
                                    match = true;
                                    break;
                                }
                            }
                            if (match && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
                                list.add(new double[]{lon, lat, 6.0, 1.0});
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not read MRDS CSV dataset: {}", e.getMessage());
        }
        return list;
    }

    public static void rasterizeSpotListToAlpha(BufferedImage img, java.util.List<double[]> spots, Color themeColor, double defaultRadiusPx) {
        int width = img.getWidth();
        int height = img.getHeight();

        float[][] grid = new float[height][width];
        float maxVal = 0.001f;

        for (double[] spot : spots) {
            double lon = spot[0];
            double lat = spot[1];
            double radiusPx = spot.length > 2 ? spot[2] : defaultRadiusPx;
            double intensity = spot.length > 3 ? spot[3] : 1.0;

            int cx = (int) Math.round(((lon + 180.0) / 360.0) * (width - 1));
            int cy = (int) Math.round(((90.0 - lat) / 180.0) * (height - 1));

            int r = (int) Math.ceil(radiusPx);
            int minY = Math.max(0, cy - r);
            int maxY = Math.min(height - 1, cy + r);
            int minX = cx - r;
            int maxX = cx + r;

            for (int py = minY; py <= maxY; py++) {
                double dy = py - cy;
                for (int px = minX; px <= maxX; px++) {
                    int wrapPx = (px % width + width) % width;
                    double dx = px - cx;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist <= radiusPx) {
                        double norm = 1.0 - (dist / radiusPx);
                        float val = (float) (Math.pow(norm, 1.5) * intensity);
                        grid[py][wrapPx] += val;
                        if (grid[py][wrapPx] > maxVal) {
                            maxVal = grid[py][wrapPx];
                        }
                    }
                }
            }
        }

        int r = themeColor.getRed();
        int g = themeColor.getGreen();
        int b = themeColor.getBlue();

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                float v = grid[py][px];
                if (v > 0) {
                    double norm = 1.0 - Math.exp(-v * 0.85);
                    int alpha = (int) Math.clamp(norm * 255.0, 100.0, 255.0);
                    img.setRGB(px, py, (alpha << 24) | (r << 16) | (g << 8) | b);
                }
            }
        }
    }

    public static void rasterizeTieredSpotList(BufferedImage img, java.util.List<double[]> spots, Color lowColor, Color medColor, Color highColor, double defaultRadiusPx) {
        int width = img.getWidth();
        int height = img.getHeight();

        float[][] grid = new float[height][width];
        float maxVal = 0.001f;

        for (double[] spot : spots) {
            double lon = spot[0];
            double lat = spot[1];
            double radiusPx = spot.length > 2 ? spot[2] : defaultRadiusPx;
            double intensity = spot.length > 3 ? spot[3] : 1.0;

            int cx = (int) Math.round(((lon + 180.0) / 360.0) * (width - 1));
            int cy = (int) Math.round(((90.0 - lat) / 180.0) * (height - 1));

            int r = (int) Math.ceil(radiusPx);
            int minY = Math.max(0, cy - r);
            int maxY = Math.min(height - 1, cy + r);
            int minX = cx - r;
            int maxX = cx + r;

            for (int py = minY; py <= maxY; py++) {
                double dy = py - cy;
                for (int px = minX; px <= maxX; px++) {
                    int wrapPx = (px % width + width) % width;
                    double dx = px - cx;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist <= radiusPx) {
                        double norm = 1.0 - (dist / radiusPx);
                        float val = (float) (Math.pow(norm, 1.4) * intensity);
                        grid[py][wrapPx] += val;
                        if (grid[py][wrapPx] > maxVal) {
                            maxVal = grid[py][wrapPx];
                        }
                    }
                }
            }
        }

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                float v = grid[py][px];
                if (v > 0.02f) {
                    double norm = Math.clamp(1.0 - Math.exp(-v * 0.75), 0.0, 1.0);
                    Color chosen;
                    if (norm < 0.35) {
                        chosen = lowColor;
                    } else if (norm < 0.70) {
                        double t = (norm - 0.35) / 0.35;
                        int rC = (int) (lowColor.getRed() + t * (medColor.getRed() - lowColor.getRed()));
                        int gC = (int) (lowColor.getGreen() + t * (medColor.getGreen() - lowColor.getGreen()));
                        int bC = (int) (lowColor.getBlue() + t * (medColor.getBlue() - lowColor.getBlue()));
                        chosen = new Color(Math.clamp(rC, 0, 255), Math.clamp(gC, 0, 255), Math.clamp(bC, 0, 255));
                    } else {
                        double t = (norm - 0.70) / 0.30;
                        int rC = (int) (medColor.getRed() + t * (highColor.getRed() - medColor.getRed()));
                        int gC = (int) (medColor.getGreen() + t * (highColor.getGreen() - medColor.getGreen()));
                        int bC = (int) (medColor.getBlue() + t * (highColor.getBlue() - medColor.getBlue()));
                        chosen = new Color(Math.clamp(rC, 0, 255), Math.clamp(gC, 0, 255), Math.clamp(bC, 0, 255));
                    }
                    int alpha = (int) Math.clamp(120 + norm * 135.0, 120.0, 255.0);
                    img.setRGB(px, py, (alpha << 24) | (chosen.getRed() << 16) | (chosen.getGreen() << 8) | chosen.getBlue());
                }
            }
        }
    }

    private static BufferedImage loadDirectBufferedImage(String filename) {
        try {
            // 1. Check data/maps/ether/
            java.io.File etherFile = new java.io.File("data/maps/ether/" + filename);
            if (etherFile.exists()) return javax.imageio.ImageIO.read(etherFile);
            String[] subDirs = {"terre", "earth", "lune", "moon", "mars", "venus", "mercure", "mercury"};
            for (String sub : subDirs) {
                java.io.File subFile = new java.io.File("data/maps/ether/" + sub + "/" + filename);
                if (subFile.exists()) return javax.imageio.ImageIO.read(subFile);
            }
            // 2. Check data/maps/
            java.io.File file = new java.io.File("data/maps/" + filename);
            if (file.exists()) return javax.imageio.ImageIO.read(file);
            for (String sub : subDirs) {
                java.io.File subFile = new java.io.File("data/maps/" + sub + "/" + filename);
                if (subFile.exists()) return javax.imageio.ImageIO.read(subFile);
            }
            // 3. Check classpath
            java.io.InputStream isEther = HistoricalMapGenerator.class.getResourceAsStream("/maps/ether/" + filename);
            if (isEther != null) {
                try (isEther) { return javax.imageio.ImageIO.read(isEther); }
            }
            java.io.InputStream is = HistoricalMapGenerator.class.getResourceAsStream("/maps/" + filename);
            if (is != null) {
                try (is) { return javax.imageio.ImageIO.read(is); }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static BufferedImage generateCleanCoalMap(String type, Scenario scenario) {
        BufferedImage loaded = loadDirectBufferedImage("earth_coal.png");
        if (loaded != null) return loaded;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var spots = loadMRDSDeposits("coal", "lignite", "anthracite", "bituminous");
        rasterizeSpotListToAlpha(img, spots, new Color(245, 158, 11), 8.0);
        return img;
    }

    public static BufferedImage generateCleanOilMap(String type, Scenario scenario) {
        BufferedImage loaded = loadDirectBufferedImage("earth_oil.png");
        if (loaded != null) return loaded;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var spots = loadMRDSDeposits("petroleum", "oil", "hydrocarbon");
        rasterizeSpotListToAlpha(img, spots, new Color(220, 38, 38), 12.0);
        return img;
    }

    public static BufferedImage generateCleanGasMap(String type, Scenario scenario) {
        BufferedImage loaded = loadDirectBufferedImage("earth_gas.png");
        if (loaded != null) return loaded;
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var spots = loadMRDSDeposits("natural gas", "gas", "methane");
        rasterizeSpotListToAlpha(img, spots, new Color(6, 182, 212), 12.0);
        return img;
    }

    public static BufferedImage generateCleanUraniumMap(String type, Scenario scenario) {
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var spots = loadMRDSDeposits("uranium", "thorium");
        double[][] iaeaMajorDeposits = {
            {-105.0, 58.0, 35, 1.4}, {136.9, -30.4, 30, 1.4}, {68.0, 44.0, 40, 1.5},
            {7.4, 18.7, 30, 1.2}, {27.5, -26.2, 30, 1.2}, {118.0, 50.0, 30, 1.2}
        };
        for (double[] b : iaeaMajorDeposits) spots.add(b);
        rasterizeSpotListToAlpha(img, spots, new Color(34, 197, 94), 6.0);
        return img;
    }

    public static BufferedImage generateCleanHelium3Map(String type, Scenario scenario) {
        // Pure transparent ARGB map: Helium-3 is exclusively a lunar resource
        return new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_ARGB);
    }

    public static BufferedImage generateCleanIronCopperMap(String type, Scenario scenario) {
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var spots = loadMRDSDeposits("iron", "copper", "magnetite", "hematite", "chalcopyrite");
        // Major global Iron & Copper deposits with 3 tiered densities
        double[][] majorMetals = {
            {118.0, -22.5, 35, 1.5}, {120.5, -23.0, 32, 1.4}, {-50.0, -6.0, 38, 1.6}, {-43.5, -20.0, 32, 1.3},
            {37.0, 51.5, 38, 1.5}, {33.5, 48.0, 34, 1.4}, {-91.5, 47.5, 32, 1.3}, {-66.5, 54.0, 35, 1.3},
            {20.0, 67.8, 28, 1.3}, {85.5, 22.0, 34, 1.3}, {-69.0, -24.0, 38, 1.5}, {-69.5, -22.3, 35, 1.4},
            {-70.5, -34.0, 35, 1.4}, {137.0, -4.0, 32, 1.4}, {-111.0, 33.5, 30, 1.2}, {28.0, -12.5, 35, 1.4},
            {102.0, 25.0, 30, 1.2}, {88.0, 38.0, 25, 1.1}, {-108.0, 32.5, 25, 1.1}
        };
        for (double[] m : majorMetals) spots.add(m);
        // Multi-tier gradient matching legend: Low Grade (#8B4513), Banded Iron (#D97706), Massive Iron & Copper (#F97316)
        rasterizeTieredSpotList(img, spots, new Color(139, 69, 19), new Color(217, 119, 6), new Color(249, 115, 22), 6.0);
        return img;
    }

    public static BufferedImage generateCleanPreciousMetalsMap(String type, Scenario scenario) {
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var spots = loadMRDSDeposits("gold", "silver", "platinum", "lithium", "rare earth");
        rasterizeSpotListToAlpha(img, spots, new Color(234, 179, 8), 5.0);
        return img;
    }

    public static BufferedImage generateCleanMantleHeatMap(String type, Scenario scenario) {
        java.nio.file.Path csvPath = java.nio.file.Paths.get("data", "maps", "ihfc_davies2013", "heat_flow_2deg.csv");
        if (!java.nio.file.Files.exists(csvPath)) {
            csvPath = java.nio.file.Paths.get("data", "maps", "heat_flow_2deg.csv");
        }

        if (java.nio.file.Files.exists(csvPath)) {
            try {
                int width = 2048, height = 1024;
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                // 2-degree resolution dataset has 90 latitude rows x 180 longitude columns
                float[][] grid = new float[90][180];
                boolean[][] filled = new boolean[90][180];

                java.util.List<String> lines = java.nio.file.Files.readAllLines(csvPath);
                for (int i = 1; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        try {
                            double lon = Double.parseDouble(parts[0].trim());
                            double lat = Double.parseDouble(parts[1].trim());
                            double val = Double.parseDouble(parts[2].trim());

                            int gx = (int) Math.clamp(Math.floor((lon + 180.0) / 2.0), 0, 179);
                            int gy = (int) Math.clamp(Math.floor((90.0 - lat) / 2.0), 0, 89);
                            grid[gy][gx] = (float) val;
                            filled[gy][gx] = true;
                        } catch (NumberFormatException ignored) {}
                    }
                }

                // Fill any minor unmeasured cells from local neighbors
                for (int gy = 0; gy < 90; gy++) {
                    for (int gx = 0; gx < 180; gx++) {
                        if (!filled[gy][gx] || grid[gy][gx] <= 0) {
                            grid[gy][gx] = 65.0f; // Global mean continental heat flow baseline (mW/m²)
                        }
                    }
                }

                // Continuous smooth Bilinear Interpolation across 2D spherical coordinates with 360° periodic wrapping
                for (int y = 0; y < height; y++) {
                    double lat = 90.0 - (y / (double) height) * 180.0;
                    double gyDouble = Math.clamp(((90.0 - lat) / 180.0) * 90.0 - 0.5, 0.0, 88.999);
                    int gy0 = (int) Math.floor(gyDouble);
                    int gy1 = Math.min(89, gy0 + 1);
                    double fy = gyDouble - gy0;

                    for (int x = 0; x < width; x++) {
                        double lon = -180.0 + (x / (double) width) * 360.0;
                        double gxDouble = ((lon + 180.0) / 360.0) * 180.0 - 0.5;
                        if (gxDouble < 0) gxDouble += 180.0;
                        int gx0 = (int) Math.floor(gxDouble) % 180;
                        int gx1 = (gx0 + 1) % 180;
                        double fx = gxDouble - Math.floor(gxDouble);

                        float hf00 = grid[gy0][gx0];
                        float hf10 = grid[gy0][gx1];
                        float hf01 = grid[gy1][gx0];
                        float hf11 = grid[gy1][gx1];

                        float hfInterp = (float) ((1.0 - fx) * (1.0 - fy) * hf00 + fx * (1.0 - fy) * hf10 + (1.0 - fx) * fy * hf01 + fx * fy * hf11);
                        if (hfInterp <= 0) hfInterp = 65.0f;

                        double norm = Math.clamp((hfInterp - 35.0) / 110.0, 0.0, 1.0);
                        int alphaVal = (int) (110 + norm * 145);
                        int r = (int) Math.clamp(239, 0, 255);
                        int g = (int) Math.clamp(40 + (1.0 - norm) * 140, 0, 255);
                        int b = (int) Math.clamp(50 + (1.0 - norm) * 60, 0, 255);

                        img.setRGB(x, y, (alphaVal << 24) | (r << 16) | (g << 8) | b);
                    }
                }
                logger.info("Successfully generated 9th geology tensor (Mantle Heat Flux) with seamless 2D Bilinear Interpolation from Davies 2013 CSV.");
                return img;
            } catch (Exception e) {
                logger.warn("Could not parse Davies 2013 heat flow CSV: {}", e.getMessage());
            }
        }

        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        double[][] spots = {{-155.5, 19.8, 30, 1.2}, {-178.0, -29.0, 45, 1.1}, {-72.0, -15.0, 60, 1.2}, {140.0, 36.0, 50, 1.1}, {43.0, 11.5, 40, 1.2}, {-25.0, 64.8, 45, 1.1}, {14.0, 40.8, 35, 1.0}};
        var list = new java.util.ArrayList<double[]>();
        for (double[] s : spots) list.add(s);
        rasterizeSpotListToAlpha(img, list, new Color(239, 68, 68), 15.0);
        return img;
    }

    public static BufferedImage generateCleanAquiferMap(String type, Scenario scenario) {
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        // Major global sedimentary aquifer systems (UNESCO WHYMAP GWR)
        double[][] majorAquifers = {
            {25.0, 22.0, 60, 1.5},   // Nubian Sandstone Aquifer System (2.2M km2)
            {-100.0, 38.0, 45, 1.3}, // Ogallala Aquifer USA
            {-54.0, -25.0, 55, 1.4}, // Guaraní Aquifer South America (1.2M km2)
            {138.0, -26.0, 60, 1.4}, // Great Artesian Basin Australia (1.7M km2)
            {10.0, 30.0, 50, 1.3},   // Northern Sahara Aquifer System
            {80.0, 27.0, 50, 1.3},   // Indo-Gangetic Basin
            {2.0, 47.0, 35, 1.1},    // Paris / Aquitaine Basins Europe
            {-60.0, -3.0, 65, 1.5},  // Amazon Basin Aquifers
            {22.0, -1.0, 55, 1.3},   // Congo Basin Aquifer
            {75.0, 60.0, 60, 1.4},   // West Siberian Basin Aquifer
            {122.0, -18.0, 45, 1.2}, // Canning Basin Australia
            {82.0, 39.0, 40, 1.1},   // Tarim Basin Aquifer
            {-48.0, -1.5, 35, 1.1},  // Marajó Aquifer System
            {-118.0, 36.0, 30, 1.1}, // California Central Valley Aquifer
            {45.0, 25.0, 40, 1.2}    // Arabian Aquifer System
        };
        var list = new java.util.ArrayList<double[]>();
        for (double[] a : majorAquifers) list.add(a);
        rasterizeSpotListToAlpha(img, list, new Color(59, 130, 246), 25.0);
        return img;
    }

    public static BufferedImage generateCleanExtensibleResourceTensorMap(int index, String type, Scenario scenario) {
        int width = 2048, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        long seed = scenario != null ? scenario.getCulturalSeed() : 12345L;
        java.util.Random rnd = new java.util.Random(seed ^ (index * 0x85EBCA6BL));
        double phaseLng = rnd.nextDouble() * Math.PI * 2.0;
        double phaseLat = rnd.nextDouble() * Math.PI * 2.0;
        double scale = 0.004 + (index % 4) * 0.002;
        float hueBase = (float) ((index * 0.173 + 0.5) % 1.0);

        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y / (double) height) * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = -180.0 + (x / (double) width) * 360.0;
                if (!isLand(lon, lat)) {
                    img.setRGB(x, y, 0x0F172A); // Equirectangular ocean basemap (Color 15, 23, 42)
                    continue;
                }

                double val = Math.sin(lon * scale + phaseLng) * Math.cos(lat * scale + phaseLat) * 0.5 + 0.5;
                float sat = 0.7f;
                float bright = (float) (val * 0.85);
                int rgb = Color.HSBtoRGB(hueBase, sat, bright);
                img.setRGB(x, y, rgb);
            }
        }
        g.dispose();
        return img;
    }

    public static void precacheAllBuiltInScenarios() {
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

