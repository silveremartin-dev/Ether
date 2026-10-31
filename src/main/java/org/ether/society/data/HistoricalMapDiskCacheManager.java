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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Persistence, Disk Caching, and Cartographic Tensor Storage Manager.
 * 
 * <p>Handles reading and writing multi-spectral geographic, demographic, and sociological
 * tensor rasters from standardized epoch directories ({@code data/ether/earth/<year>/})
 * and runtime disk cache storage ({@code data/cache/}).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class HistoricalMapDiskCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalMapDiskCacheManager.class);

    private HistoricalMapDiskCacheManager() {
        // Static utility class
    }

    /**
     * Converts a BufferedImage into a base64 encoded PNG data string.
     *
     * @param bImg image to encode
     * @return base64 string, or {@code null} on failure
     */
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

    /**
     * Writes a BufferedImage to a target PNG file on disk with updated modification timestamp.
     *
     * @param img image to write
     * @param targetFile destination file
     */
    public static void writePngFile(BufferedImage img, File targetFile) {
        if (img == null || targetFile == null) return;
        try {
            ImageIO.write(img, "PNG", targetFile);
            targetFile.setLastModified(System.currentTimeMillis());
        } catch (Exception e) {
            logger.warn("Failed to write image to {}: {}", targetFile.getAbsolutePath(), e.getMessage());
        }
    }

    /**
     * Attempts to load all 25 rasters for a scenario from the standardized epoch directory.
     *
     * @param scenario target scenario to populate
     * @return {@code true} if maps were loaded successfully
     */
    public static boolean loadFromYearDirectory(Scenario scenario) {
        if (scenario == null) return false;
        String planet = "earth";
        if (scenario.getPlanetPreset() != null && scenario.getPlanetPreset().getCanonicalPlanet() != null && !scenario.getPlanetPreset().getCanonicalPlanet().isBlank()) {
            planet = scenario.getPlanetPreset().getCanonicalPlanet().toLowerCase(java.util.Locale.ROOT);
        }
        Path basePlanetDir = Paths.get("data", "ether", planet);
        if (!Files.isDirectory(basePlanetDir)) {
            basePlanetDir = Paths.get("data", "ether", "earth");
            planet = "earth";
        }
        if (!Files.isDirectory(basePlanetDir)) {
            return false;
        }

        long year = scenario.getStartDateYear();
        Path planetDir = basePlanetDir.resolve(String.valueOf(year));
        long resolvedYear = year;

        if (!Files.isDirectory(planetDir)) {
            if (scenario.getPlanetPreset() != null) {
                long assoc = scenario.getPlanetPreset().getAssociatedEpochYear();
                Path assocDir = basePlanetDir.resolve(String.valueOf(assoc));
                if (Files.isDirectory(assocDir)) {
                    planetDir = assocDir;
                    resolvedYear = assoc;
                }
            }
            if (!Files.isDirectory(planetDir)) {
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
                        .min(Comparator.comparingLong(y -> Math.abs(y - year)));
                    if (closest.isPresent()) {
                        resolvedYear = closest.get();
                        planetDir = basePlanetDir.resolve(String.valueOf(resolvedYear));
                    }
                } catch (Exception e) {
                    logger.debug("Failed to list {} map dirs: {}", planet, e.getMessage());
                }
            }
        }

        if (!Files.isDirectory(planetDir)) {
            return false;
        }

        try {
            Path densityFile = planetDir.resolve(planet + "_" + resolvedYear + "_density.png");
            if (!Files.exists(densityFile)) {
                try (Stream<Path> stream = Files.list(planetDir)) {
                    Optional<Path> anyDensity = stream
                        .filter(p -> p.getFileName().toString().endsWith("_density.png"))
                        .findFirst();
                    if (anyDensity.isPresent()) {
                        densityFile = anyDensity.get();
                    }
                }
            }

            if (Files.exists(densityFile)) {
                BufferedImage img = ImageIO.read(densityFile.toFile());
                if (img != null) {
                    scenario.setCustomDensityBase64(bufferedImageToBase64Png(img));
                }
            }

            String[] mapTags = {
                "isogloss", "kinship", "rituals", "sovereignty",
                "technology", "tradenetwork", "institutional", "ecological", "pathogen"
            };
            for (int i = 0; i < mapTags.length; i++) {
                Path p = planetDir.resolve(planet + "_" + resolvedYear + "_" + mapTags[i] + ".png");
                if (Files.exists(p)) {
                    BufferedImage img = ImageIO.read(p.toFile());
                    if (img != null) {
                        scenario.setCustomTensorMapBase64(i, bufferedImageToBase64Png(img));
                    }
                }
            }

            String[] geoTags = {
                "coal", "oil", "gas", "uranium",
                "helium3", "iron_copper", "precious_metals", "rare_earths", "geothermal", "aquifers"
            };
            for (int i = 0; i < geoTags.length; i++) {
                Path p = planetDir.resolve(planet + "_" + resolvedYear + "_" + geoTags[i] + ".png");
                if (Files.exists(p)) {
                    BufferedImage img = ImageIO.read(p.toFile());
                    if (img != null) {
                        scenario.setCustomGeologyTensorMapBase64(i, bufferedImageToBase64Png(img));
                    }
                }
            }

            boolean loaded = scenario.getCustomDensityBase64() != null;
            if (loaded) {
                logger.info("Successfully loaded scenario '{}' (Planet {}, Year {}, resolved epoch {}) maps from standardized directory '{}'.", scenario.getName(), planet, year, resolvedYear, planetDir);
            }
            return loaded;
        } catch (Exception e) {
            logger.warn("Failed to load scenario '{}' maps from year directory '{}': {}", scenario.getName(), planetDir, e.getMessage());
            return false;
        }
    }

    /**
     * Loads cartographic tensors from runtime disk cache.
     *
     * @param scenario target scenario
     * @param cacheDir cache root path
     * @param safeName sanitized file prefix
     * @return {@code true} if tensors were loaded successfully
     */
    public static boolean loadFromDiskCache(Scenario scenario, Path cacheDir, String safeName) {
        Path densityCache = cacheDir.resolve(safeName + "_density.png");
        if (!Files.exists(densityCache)) {
            return false;
        }
        try {
            BufferedImage imgDensity = ImageIO.read(densityCache.toFile());
            if (imgDensity == null) return false;

            scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));

            String[] mapKeys = {
                "_isogloss.png", "_kinship.png", "_rituals.png", "_sovereignty.png",
                "_technology.png", "_tradenetwork.png", "_institutional.png", "_ecological.png", "_pathogen.png"
            };
            for (int i = 0; i < mapKeys.length; i++) {
                Path p = cacheDir.resolve(safeName + mapKeys[i]);
                if (Files.exists(p)) {
                    BufferedImage img = ImageIO.read(p.toFile());
                    if (img != null) {
                        scenario.setCustomTensorMapBase64(i, bufferedImageToBase64Png(img));
                    }
                }
            }

            String[] geoKeys = {
                "_coal.png", "_oil.png", "_gas.png", "_uranium.png",
                "_he3.png", "_ironcopper.png", "_preciousmetals.png", "_rareearths.png", "_mantleheat.png", "_aquifer.png"
            };
            for (int i = 0; i < geoKeys.length; i++) {
                Path p = cacheDir.resolve(safeName + geoKeys[i]);
                if (!Files.exists(p) && i == 6) {
                    p = cacheDir.resolve(safeName + "_preciousree.png");
                }
                if (!Files.exists(p) && i == 7) {
                    p = cacheDir.resolve(safeName + "_rareearths.png");
                }
                if (Files.exists(p)) {
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

    /**
     * Persists all 25 rasters into the standardized epoch directory.
     */
    public static void saveImagesToYearDirectory(long year, BufferedImage imgDensity, BufferedImage imgSovereignty,
            BufferedImage imgIsogloss, BufferedImage imgKinship, BufferedImage imgRituals, BufferedImage imgTech,
            BufferedImage imgTrade, BufferedImage imgInst, BufferedImage imgEco, BufferedImage imgPathogen,
            BufferedImage imgCoal, BufferedImage imgOil, BufferedImage imgGas, BufferedImage imgUranium,
            BufferedImage imgHe3, BufferedImage imgIronCopper, BufferedImage imgPreciousMetals, BufferedImage imgRareEarths, BufferedImage imgMantleHeat, BufferedImage imgAquifer) {
        try {
            Path earthDir = Paths.get("data", "ether", "earth", String.valueOf(year));
            Files.createDirectories(earthDir);

            writePngFile(imgDensity, earthDir.resolve("earth_" + year + "_density.png").toFile());
            writePngFile(imgIsogloss, earthDir.resolve("earth_" + year + "_isogloss.png").toFile());
            writePngFile(imgKinship, earthDir.resolve("earth_" + year + "_kinship.png").toFile());
            writePngFile(imgRituals, earthDir.resolve("earth_" + year + "_rituals.png").toFile());
            writePngFile(imgSovereignty, earthDir.resolve("earth_" + year + "_sovereignty.png").toFile());
            writePngFile(imgTech, earthDir.resolve("earth_" + year + "_technology.png").toFile());
            writePngFile(imgTrade, earthDir.resolve("earth_" + year + "_tradenetwork.png").toFile());
            writePngFile(imgInst, earthDir.resolve("earth_" + year + "_institutional.png").toFile());
            writePngFile(imgEco, earthDir.resolve("earth_" + year + "_ecological.png").toFile());

            String[] staticMinerals = {"coal", "oil", "gas", "uranium", "helium3", "iron_copper", "precious_metals", "rare_earths", "geothermal"};
            BufferedImage[] staticImgs = {imgCoal, imgOil, imgGas, imgUranium, imgHe3, imgIronCopper, imgPreciousMetals, imgRareEarths, imgMantleHeat};
            for (int i = 0; i < staticMinerals.length; i++) {
                String min = staticMinerals[i];
                Path minPath = earthDir.resolve("earth_" + year + "_" + min + ".png");
                if (staticImgs[i] != null) {
                    writePngFile(staticImgs[i], minPath.toFile());
                } else if (!minPath.toFile().exists()) {
                    File srcMin = new File("data/ether/earth/2025/earth_2025_" + min + ".png");
                    if (!srcMin.exists()) srcMin = new File("data/ether/earth/-100000/earth_-100000_" + min + ".png");
                    if (srcMin.exists()) {
                        Files.copy(srcMin.toPath(), minPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            writePngFile(imgAquifer, earthDir.resolve("earth_" + year + "_aquifers.png").toFile());

            // Biomes & Paleoclimatic layers
            Path biomesPath = earthDir.resolve("earth_" + year + "_biomes.png");
            BufferedImage biomesImg = PaleoClimateMapRasterizer.rasterizeBiomesMap(year);
            if (biomesImg != null) {
                writePngFile(biomesImg, biomesPath.toFile());
            }

            Path tempPath = earthDir.resolve("earth_" + year + "_temperature.png");
            BufferedImage tempImg = PaleoClimateMapRasterizer.rasterizeTemperatureMap(year);
            if (tempImg != null) {
                writePngFile(tempImg, tempPath.toFile());
            }

            Path rainPath = earthDir.resolve("earth_" + year + "_precipitation.png");
            BufferedImage rainImg = PaleoClimateMapRasterizer.rasterizePrecipitationMap(year);
            if (rainImg != null) {
                writePngFile(rainImg, rainPath.toFile());
            }

            Path seasPath = earthDir.resolve("earth_" + year + "_seasonality.png");
            BufferedImage seasImg = PaleoClimateMapRasterizer.rasterizeSeasonalityMap(year);
            if (seasImg != null) {
                writePngFile(seasImg, seasPath.toFile());
            }

            Path elevPath = earthDir.resolve("earth_" + year + "_elevation.png");
            File srcElev = new File("data/ether/earth/earth_elevation.png");
            if (!srcElev.exists()) srcElev = new File("data/ether/earth/2026/earth_2026_elevation.png");
            if (!srcElev.exists()) srcElev = new File("data/ether/earth/-100000/earth_-100000_elevation.png");
            if (srcElev.exists()) {
                Files.copy(srcElev.toPath(), elevPath, StandardCopyOption.REPLACE_EXISTING);
                elevPath.toFile().setLastModified(System.currentTimeMillis());
            } else {
                BufferedImage mask = HistoricalMapGenerator.loadElevationMask();
                if (mask != null) writePngFile(mask, elevPath.toFile());
            }

            HistoricalEpochMetadataGenerator.generateOrUpdateEpochMetadataFiles(earthDir, year);
            logger.info("Persisted standard scenario cartographic maps and updated documentation into 'data/ether/earth/{}/'", year);
        } catch (Exception e) {
            logger.warn("Failed to persist scenario maps to year directory 'data/ether/earth/{}/': {}", year, e.getMessage());
        }
    }

    /**
     * Persists cultural tensors into the standardized epoch directory.
     */
    public static void saveCulturalTensorsToYearDirectory(long year, BufferedImage imgSovereignty,
            BufferedImage imgIsogloss, BufferedImage imgKinship, BufferedImage imgRituals, BufferedImage imgTech,
            BufferedImage imgTrade, BufferedImage imgInst, BufferedImage imgEco, BufferedImage imgPathogen) {
        try {
            Path earthDir = Paths.get("data", "ether", "earth", String.valueOf(year));
            Files.createDirectories(earthDir);

            if (imgIsogloss != null)        ImageIO.write(imgIsogloss,        "PNG", earthDir.resolve("earth_" + year + "_isogloss.png").toFile());
            if (imgKinship != null)         ImageIO.write(imgKinship,         "PNG", earthDir.resolve("earth_" + year + "_kinship.png").toFile());
            if (imgRituals != null)         ImageIO.write(imgRituals,         "PNG", earthDir.resolve("earth_" + year + "_rituals.png").toFile());
            if (imgSovereignty != null)     ImageIO.write(imgSovereignty,     "PNG", earthDir.resolve("earth_" + year + "_sovereignty.png").toFile());
            if (imgTech != null)            ImageIO.write(imgTech,            "PNG", earthDir.resolve("earth_" + year + "_technology.png").toFile());
            if (imgTrade != null)           ImageIO.write(imgTrade,           "PNG", earthDir.resolve("earth_" + year + "_tradenetwork.png").toFile());
            if (imgInst != null)            ImageIO.write(imgInst,            "PNG", earthDir.resolve("earth_" + year + "_institutional.png").toFile());
            if (imgEco != null)             ImageIO.write(imgEco,             "PNG", earthDir.resolve("earth_" + year + "_ecological.png").toFile());
            if (imgPathogen != null)        ImageIO.write(imgPathogen,        "PNG", earthDir.resolve("earth_" + year + "_pathogen.png").toFile());

            logger.info("Persisted 9 authentic cultural tensors into 'data/ether/earth/{}/'", year);
        } catch (Exception e) {
            logger.error("Failed to save cultural tensors for year {}", year, e);
        }
    }

    /**
     * Saves cultural tensors to runtime cache.
     */
    public static void saveImagesToDiskCache(String scenarioName, BufferedImage imgDensity, BufferedImage imgSovereignty, BufferedImage imgIsogloss, BufferedImage imgKinship, BufferedImage imgRituals, BufferedImage imgTech, BufferedImage imgTrade, BufferedImage imgInst, BufferedImage imgEco, BufferedImage imgPathogen) {
        if (scenarioName == null || scenarioName.isBlank()) scenarioName = "scenario";
        try {
            Path cacheDir = Paths.get("data", "cache");
            Files.createDirectories(cacheDir);
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
     * Saves geological resource tensors to runtime cache.
     */
    public static void saveGeologyTensorsToDiskCache(String scenarioName,
            BufferedImage imgCoal, BufferedImage imgOil, BufferedImage imgGas,
            BufferedImage imgUranium, BufferedImage imgHe3, BufferedImage imgIronCopper,
            BufferedImage imgPreciousMetals, BufferedImage imgRareEarths, BufferedImage imgMantleHeat, BufferedImage imgAquifer) {
        if (scenarioName == null || scenarioName.isBlank()) scenarioName = "scenario";
        try {
            Path cacheDir = Paths.get("data", "cache");
            Files.createDirectories(cacheDir);
            String safeName = scenarioName.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase(java.util.Locale.ROOT);

            if (imgCoal != null)           ImageIO.write(imgCoal,           "PNG", cacheDir.resolve(safeName + "_coal.png").toFile());
            if (imgOil != null)            ImageIO.write(imgOil,            "PNG", cacheDir.resolve(safeName + "_oil.png").toFile());
            if (imgGas != null)            ImageIO.write(imgGas,            "PNG", cacheDir.resolve(safeName + "_gas.png").toFile());
            if (imgUranium != null)        ImageIO.write(imgUranium,        "PNG", cacheDir.resolve(safeName + "_uranium.png").toFile());
            if (imgHe3 != null)            ImageIO.write(imgHe3,            "PNG", cacheDir.resolve(safeName + "_he3.png").toFile());
            if (imgIronCopper != null)     ImageIO.write(imgIronCopper,     "PNG", cacheDir.resolve(safeName + "_ironcopper.png").toFile());
            if (imgPreciousMetals != null) ImageIO.write(imgPreciousMetals, "PNG", cacheDir.resolve(safeName + "_preciousmetals.png").toFile());
            if (imgRareEarths != null)     ImageIO.write(imgRareEarths,     "PNG", cacheDir.resolve(safeName + "_rareearths.png").toFile());
            if (imgMantleHeat != null)     ImageIO.write(imgMantleHeat,     "PNG", cacheDir.resolve(safeName + "_mantleheat.png").toFile());
            if (imgAquifer != null)        ImageIO.write(imgAquifer,        "PNG", cacheDir.resolve(safeName + "_aquifer.png").toFile());

            logger.info("Persisted 10 geological tensor maps to disk cache 'data/cache/{}_*.png'", safeName);
        } catch (Exception e) {
            logger.warn("Failed to write geological tensor maps to disk cache directory: {}", e.getMessage());
        }
    }
}

