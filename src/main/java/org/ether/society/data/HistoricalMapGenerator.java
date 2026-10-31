/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Ultra-High Precision Cartographic Generator for Ether Simulation Engine.
 * 
 * <p>Central facade coordinating continuous topographic landmasking, empirical
 * altimetry, paleoclimatology, and multi-channel spatial tensors across all
 * built-in scenarios.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class HistoricalMapGenerator {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalMapGenerator.class);

    public static final int WIDTH = 2048;
    public static final int HEIGHT = 1024;

    // Quaternary Continental Ice Sheets - Delegated to PaleoGeographyCoordinatesCatalog
    public static final double[][] POLY_LAURENTIDE_LGM = PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_LGM;
    public static final double[][] POLY_LAURENTIDE_MIS3 = PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_MIS3;
    public static final double[][] POLY_LAURENTIDE_YD = PaleoGeographyCoordinatesCatalog.POLY_LAURENTIDE_YD;
    public static final double[][] POLY_FENNOSCANDIA_LGM = PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_LGM;
    public static final double[][] POLY_FENNOSCANDIA_MIS3 = PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_MIS3;
    public static final double[][] POLY_FENNOSCANDIA_YD = PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_YD;
    public static final double[][] POLY_FENNOSCANDIA_EARLY_HOLOCENE = PaleoGeographyCoordinatesCatalog.POLY_FENNOSCANDIA_EARLY_HOLOCENE;
    public static final double[][] POLY_ALPS_LGM = PaleoGeographyCoordinatesCatalog.POLY_ALPS_LGM;
    public static final double[][] POLY_PATAGONIA_LGM = PaleoGeographyCoordinatesCatalog.POLY_PATAGONIA_LGM;
    public static final double[][] POLY_BERINGIA_REFUGE = PaleoGeographyCoordinatesCatalog.POLY_BERINGIA_REFUGE;

    // Detailed Continental Migration Arteries - Delegated to PaleoGeographyCoordinatesCatalog
    public static final double[][] ROUTE_PACIFIC_KELP_HIGHWAY = PaleoGeographyCoordinatesCatalog.ROUTE_PACIFIC_KELP_HIGHWAY;
    public static final double[][] ROUTE_ICE_FREE_CORRIDOR = PaleoGeographyCoordinatesCatalog.ROUTE_ICE_FREE_CORRIDOR;
    public static final double[][] ROUTE_COLUMBIA_SNAKE = PaleoGeographyCoordinatesCatalog.ROUTE_COLUMBIA_SNAKE;
    public static final double[][] ROUTE_MISSOURI_RIVER = PaleoGeographyCoordinatesCatalog.ROUTE_MISSOURI_RIVER;
    public static final double[][] ROUTE_MISSISSIPPI_OHIO = PaleoGeographyCoordinatesCatalog.ROUTE_MISSISSIPPI_OHIO;
    public static final double[][] ROUTE_LOWER_MISSISSIPPI = PaleoGeographyCoordinatesCatalog.ROUTE_LOWER_MISSISSIPPI;
    public static final double[][] ROUTE_SOUTHEAST_FLORIDA = PaleoGeographyCoordinatesCatalog.ROUTE_SOUTHEAST_FLORIDA;
    public static final double[][] ROUTE_ANDES_CORRIDOR = PaleoGeographyCoordinatesCatalog.ROUTE_ANDES_CORRIDOR;
    public static final double[][] ROUTE_AMAZON_MAINSTEM = PaleoGeographyCoordinatesCatalog.ROUTE_AMAZON_MAINSTEM;
    public static final double[][] ROUTE_AMAZON_MADEIRA = PaleoGeographyCoordinatesCatalog.ROUTE_AMAZON_MADEIRA;
    public static final double[][] ROUTE_BRAZIL_SAVANNA = PaleoGeographyCoordinatesCatalog.ROUTE_BRAZIL_SAVANNA;
    public static final double[][] ROUTE_BERINGIA_STEPPE_CORRIDOR = PaleoGeographyCoordinatesCatalog.ROUTE_BERINGIA_STEPPE_CORRIDOR;

    // Biome Constants - Delegated to PaleoClimateMapRasterizer
    public static final int BIOME_DEEP_OCEAN = PaleoClimateMapRasterizer.BIOME_DEEP_OCEAN;
    public static final int BIOME_OCEAN      = PaleoClimateMapRasterizer.BIOME_OCEAN;
    public static final int BIOME_BEACH      = PaleoClimateMapRasterizer.BIOME_BEACH;
    public static final int BIOME_PLAINS     = PaleoClimateMapRasterizer.BIOME_PLAINS;
    public static final int BIOME_FOREST     = PaleoClimateMapRasterizer.BIOME_FOREST;
    public static final int BIOME_JUNGLE     = PaleoClimateMapRasterizer.BIOME_JUNGLE;
    public static final int BIOME_DESERT     = PaleoClimateMapRasterizer.BIOME_DESERT;
    public static final int BIOME_HILLS      = PaleoClimateMapRasterizer.BIOME_HILLS;
    public static final int BIOME_MOUNTAINS  = PaleoClimateMapRasterizer.BIOME_MOUNTAINS;
    public static final int BIOME_TUNDRA     = PaleoClimateMapRasterizer.BIOME_TUNDRA;
    public static final int BIOME_SNOW       = PaleoClimateMapRasterizer.BIOME_SNOW;
    public static final int BIOME_GLACIER    = PaleoClimateMapRasterizer.BIOME_GLACIER;

    // Cultural Tensor Clade Palette Constants
    public static final int[] COLORS_SOVEREIGNTY_100K = HistoricalCulturalTensorRasterizer.COLORS_SOVEREIGNTY_100K;
    public static final int[] COLORS_ISOGLOSS_100K = HistoricalCulturalTensorRasterizer.COLORS_ISOGLOSS_100K;
    public static final int[] COLORS_KINSHIP_100K = HistoricalCulturalTensorRasterizer.COLORS_KINSHIP_100K;
    public static final int[] COLORS_RITUALS_100K = HistoricalCulturalTensorRasterizer.COLORS_RITUALS_100K;

    // --- HOMININ OCCUPANCY & PALEO TRAITS DELEGATIONS ---

    public static double[] computeHomininCladeWeights(double lon, double lat) {
        return HomininPaleoOccupancyCalculator.computeHomininCladeWeights(lon, lat);
    }

    public static int getHomininEntityId(double lon, double lat) {
        return HomininPaleoOccupancyCalculator.getHomininEntityId(lon, lat);
    }

    public static int getHomininSpeciesType(double lon, double lat) {
        return HomininPaleoOccupancyCalculator.getHomininSpeciesType(lon, lat);
    }

    public static double blendPaleoTraits(double lon, double lat, double valSapiens, double valNeanderthal, double valDenisovan) {
        return HomininPaleoOccupancyCalculator.blendPaleoTraits(lon, lat, valSapiens, valNeanderthal, valDenisovan);
    }

    public static double[][] chaikinSmoothPolygon(double[][] poly, int iterations) {
        return PaleoGeographyCoordinatesCatalog.chaikinSmoothPolygon(poly, iterations);
    }

    public static double distToPolyline(double lon, double lat, double[][] line) {
        return HomininPaleoOccupancyCalculator.distToPolyline(lon, lat, line);
    }

    public static double signedDistanceToPolygon(double lon, double lat, double[][] poly) {
        return HomininPaleoOccupancyCalculator.signedDistanceToPolygon(lon, lat, poly);
    }

    public static boolean isRemoteOceanicIsland(double lon, double lat, long year) {
        return HomininPaleoOccupancyCalculator.isRemoteOceanicIsland(lon, lat, year);
    }

    public static double getHomininOccupancyWeight(double lon, double lat, long year) {
        return HomininPaleoOccupancyCalculator.getHomininOccupancyWeight(lon, lat, year);
    }

    public static boolean isHomininOccupied(double lon, double lat, long year) {
        return HomininPaleoOccupancyCalculator.isHomininOccupied(lon, lat, year);
    }

    public static boolean isLand(double lng, double lat) {
        return HistoricalLandGeometryCatalog.isLand(lng, lat);
    }

    public static BufferedImage createPureTransparentCanvas() {
        return new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    }

    // --- PALEOCLIMATOLOGY DELEGATIONS ---

    public static double computeSurfaceTemperature(double lat, double lon, double elevM, long year) {
        return PaleoClimateMapRasterizer.computeSurfaceTemperature(lat, lon, elevM, year);
    }

    public static double computeAnnualPrecipitation(double lat, double lon, double elevM, long year) {
        return PaleoClimateMapRasterizer.computeAnnualPrecipitation(lat, lon, elevM, year);
    }

    public static double computeSeasonalityAmplitude(double lat, double lon, double elevM, long year) {
        return PaleoClimateMapRasterizer.computeSeasonalityAmplitude(lat, lon, elevM, year);
    }

    public static BufferedImage rasterizeBiomesMap(long year) {
        return PaleoClimateMapRasterizer.rasterizeBiomesMap(year);
    }

    public static BufferedImage rasterizeTemperatureMap(long year) {
        return PaleoClimateMapRasterizer.rasterizeTemperatureMap(year);
    }

    public static BufferedImage rasterizePrecipitationMap(long year) {
        return PaleoClimateMapRasterizer.rasterizePrecipitationMap(year);
    }

    public static BufferedImage rasterizeSeasonalityMap(long year) {
        return PaleoClimateMapRasterizer.rasterizeSeasonalityMap(year);
    }

    // --- DISK CACHE & FILE PERSISTENCE DELEGATIONS ---

    public static String bufferedImageToBase64Png(BufferedImage bImg) {
        return HistoricalMapDiskCacheManager.bufferedImageToBase64Png(bImg);
    }

    public static void writePngFile(BufferedImage img, File targetFile) {
        HistoricalMapDiskCacheManager.writePngFile(img, targetFile);
    }

    public static boolean loadFromYearDirectory(Scenario scenario) {
        return HistoricalMapDiskCacheManager.loadFromYearDirectory(scenario);
    }

    public static boolean loadFromDiskCache(Scenario scenario, java.nio.file.Path cacheDir, String safeName) {
        return HistoricalMapDiskCacheManager.loadFromDiskCache(scenario, cacheDir, safeName);
    }

    public static void saveImagesToYearDirectory(long year, BufferedImage imgDensity, BufferedImage imgSovereignty,
            BufferedImage imgIsogloss, BufferedImage imgKinship, BufferedImage imgRituals, BufferedImage imgTech,
            BufferedImage imgTrade, BufferedImage imgInst, BufferedImage imgEco, BufferedImage imgPathogen,
            BufferedImage imgCoal, BufferedImage imgOil, BufferedImage imgGas, BufferedImage imgUranium,
            BufferedImage imgHe3, BufferedImage imgIronCopper, BufferedImage imgPreciousMetals, BufferedImage imgRareEarths, BufferedImage imgMantleHeat, BufferedImage imgAquifer) {
        HistoricalMapDiskCacheManager.saveImagesToYearDirectory(year, imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTech, imgTrade, imgInst, imgEco, imgPathogen, imgCoal, imgOil, imgGas, imgUranium, imgHe3, imgIronCopper, imgPreciousMetals, imgRareEarths, imgMantleHeat, imgAquifer);
    }

    public static void saveCulturalTensorsToYearDirectory(long year, BufferedImage imgSovereignty,
            BufferedImage imgIsogloss, BufferedImage imgKinship, BufferedImage imgRituals, BufferedImage imgTech,
            BufferedImage imgTrade, BufferedImage imgInst, BufferedImage imgEco, BufferedImage imgPathogen) {
        HistoricalMapDiskCacheManager.saveCulturalTensorsToYearDirectory(year, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTech, imgTrade, imgInst, imgEco, imgPathogen);
    }

    public static void saveImagesToDiskCache(String scenarioName, BufferedImage imgDensity, BufferedImage imgSovereignty, BufferedImage imgIsogloss, BufferedImage imgKinship, BufferedImage imgRituals, BufferedImage imgTech, BufferedImage imgTrade, BufferedImage imgInst, BufferedImage imgEco, BufferedImage imgPathogen) {
        HistoricalMapDiskCacheManager.saveImagesToDiskCache(scenarioName, imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTech, imgTrade, imgInst, imgEco, imgPathogen);
    }

    public static void saveGeologyTensorsToDiskCache(String scenarioName,
            BufferedImage imgCoal, BufferedImage imgOil, BufferedImage imgGas,
            BufferedImage imgUranium, BufferedImage imgHe3, BufferedImage imgIronCopper,
            BufferedImage imgPreciousMetals, BufferedImage imgRareEarths, BufferedImage imgMantleHeat, BufferedImage imgAquifer) {
        HistoricalMapDiskCacheManager.saveGeologyTensorsToDiskCache(scenarioName, imgCoal, imgOil, imgGas, imgUranium, imgHe3, imgIronCopper, imgPreciousMetals, imgRareEarths, imgMantleHeat, imgAquifer);
    }

    public static void generateOrUpdateEpochMetadataFiles(java.nio.file.Path earthDir, long year) {
        HistoricalEpochMetadataGenerator.generateOrUpdateEpochMetadataFiles(earthDir, year);
    }

    public static BufferedImage applyPrehistoricGeographicMask(String scenarioKey, long year, BufferedImage src) {
        return PaleolithicDensityGenerator.applyPrehistoricGeographicMask(scenarioKey, year, src);
    }

    public static BufferedImage generatePrehistoricSyntheticDensityMap(long year, String type) {
        return PaleolithicDensityGenerator.generatePrehistoricSyntheticDensityMap(year, type);
    }

    public static int blendCladeRgb(double[] w, int[] cladeColors, double occWeight) {
        return HistoricalCulturalTensorRasterizer.blendCladeRgb(w, cladeColors, occWeight);
    }

    public static BufferedImage rasterizeSovereigntyMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeSovereigntyMap(type, scenario);
    }

    public static BufferedImage rasterizeIsoglossMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeIsoglossMap(type, scenario);
    }

    public static BufferedImage rasterizeKinshipMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeKinshipMap(type, scenario);
    }

    public static BufferedImage rasterizeRitualsMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeRitualsMap(type, scenario);
    }

    public static BufferedImage rasterizeTechnologyMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeTechnologyMap(type, scenario);
    }

    public static BufferedImage rasterizeTradeNetworkMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeTradeNetworkMap(type, scenario);
    }

    public static BufferedImage rasterizeInstitutionalComplexityMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeInstitutionalComplexityMap(type, scenario);
    }

    public static BufferedImage rasterizeEcologicalFootprintMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeEcologicalFootprintMap(type, scenario);
    }

    public static BufferedImage rasterizePathogenImmunityMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizePathogenImmunityMap(type, scenario);
    }

    public static BufferedImage rasterizeCoalMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeCoalMap(type, scenario);
    }

    public static BufferedImage rasterizeOilMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeOilMap(type, scenario);
    }

    public static BufferedImage rasterizeGasMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeGasMap(type, scenario);
    }

    public static BufferedImage rasterizeUraniumMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeUraniumMap(type, scenario);
    }

    public static BufferedImage rasterizeHelium3Map(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeHelium3Map(type, scenario);
    }

    public static BufferedImage rasterizeIronCopperMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeIronCopperMap(type, scenario);
    }

    public static BufferedImage rasterizePreciousMetalsMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizePreciousMetalsMap(type, scenario);
    }

    public static BufferedImage rasterizeRareEarthsMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeRareEarthsMap(type, scenario);
    }

    public static BufferedImage rasterizeMantleHeatMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeMantleHeatMap(type, scenario);
    }

    public static double getPaleoAquiferRechargeFactor(double lon, double lat, long year) {
        return GeologicalResourceMapRasterizer.getPaleoAquiferRechargeFactor(lon, lat, year);
    }

    public static BufferedImage rasterizeAquiferMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeAquiferMap(type, scenario);
    }

    public static BufferedImage rasterizeExtensibleResourceTensorMap(int index, String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeExtensibleResourceTensorMap(index, type, scenario);
    }

    public static java.util.List<double[]> loadMRDSDeposits(String... commodityKeywords) {
        return GeologicalResourceMapRasterizer.loadMRDSDeposits(commodityKeywords);
    }

    public static void rasterizeSpotListToAlpha(BufferedImage img, java.util.List<double[]> spots, Color themeColor, double defaultRadiusPx) {
        GeologicalResourceMapRasterizer.rasterizeSpotListToAlpha(img, spots, themeColor, defaultRadiusPx);
    }

    public static void rasterizeTieredSpotList(BufferedImage img, java.util.List<double[]> spots, Color lowColor, Color medColor, Color highColor, double defaultRadiusPx) {
        GeologicalResourceMapRasterizer.rasterizeTieredSpotList(img, spots, lowColor, medColor, highColor, defaultRadiusPx);
    }

    // --- ELEVATION MASK & CARTOGRAPHIC PIPELINE ---

    private static volatile BufferedImage cachedElevationMask = null;
    private static final Object ELEV_LOCK = new Object();

    public static BufferedImage loadElevationMask() {
        if (cachedElevationMask != null) return cachedElevationMask;
        synchronized (ELEV_LOCK) {
            if (cachedElevationMask != null) return cachedElevationMask;
            try {
                int tw = WIDTH, th = HEIGHT;
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
                File f100k = new File("data/ether/earth/-100000/earth_-100000_elevation.png");
                if (f100k.exists()) {
                    try { elev = ImageIO.read(f100k); } catch (Exception ignored) {}
                }
                if (elev == null) {
                    File f2026 = new File("data/ether/earth/2026/earth_2026_elevation.png");
                    if (f2026.exists()) {
                        try { elev = ImageIO.read(f2026); } catch (Exception ignored) {}
                    }
                }
                if (elev == null) {
                    File fCache = new File("data/cache/earth_elevation.png");
                    if (fCache.exists()) {
                        try { elev = ImageIO.read(fCache); } catch (Exception ignored) {}
                    }
                }
                if (elev == null) {
                    logger.warn("earth_elevation.png not found â€” altimetry coastline mask disabled.");
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

    public static BufferedImage applyAltimetryCoastlineMask(BufferedImage src) {
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

    public static double getTopographicHabitability(String scenarioType, double lng, double lat) {
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

    public static BufferedImage rasterizeDensityMap(String type, Scenario scenario) {
        return rasterizeDensityMapForYear(type, scenario, (scenario != null) ? scenario.getStartDateYear() : -10000);
    }

    public static BufferedImage rasterizeDensityMapForYear(String type, Scenario scenario, long targetYear) {
        if (scenario != null && scenario.isUseRealEarthData() && (targetYear < -10000 || targetYear > 2024)) {
            throw new IllegalStateException("ZERO FALLBACK VIOLATION: Empirical HYDE 3.4 dataset unavailable for year " + targetYear);
        }
        if (targetYear < -10000) {
            return applyAltimetryCoastlineMask(generatePrehistoricSyntheticDensityMap(targetYear, type));
        }
        BufferedImage realHydeImg = Hyde34GridReader.loadForYear(targetYear);
        if (realHydeImg != null) {
            logger.info("Ingested authentic HYDE 3.4 5-arc-minute Esri ASCII raster grid for year {}", targetYear);
            return applyAltimetryCoastlineMask(realHydeImg);
        }
        return applyAltimetryCoastlineMask(generatePrehistoricSyntheticDensityMap(targetYear, type));
    }

    public static BufferedImage rasterizeExtensibleTensorMap(int tensorIndex, String type, Scenario scenario) {
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

    public static BufferedImage rasterizeCoastlines(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return img;
    }

    public static void generateProceduralMapsForScenario(Scenario scenario) {
        populateScenarioHistoricalMaps(scenario);
    }

    public static void populateScenarioHistoricalMaps(Scenario scenario) {
        if (scenario == null) return;
        try {
            if (loadFromYearDirectory(scenario)) return;

            String safeName = scenario.getName().replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase(java.util.Locale.ROOT);
            java.nio.file.Path cacheDir = java.nio.file.Paths.get("data", "cache");

            if (loadFromDiskCache(scenario, cacheDir, safeName)) {
                logger.info("Successfully loaded scenario '{}' cartographic tensors from disk cache.", scenario.getName());
                return;
            }

            String presetKey = scenario.getPresetKey();
            if (presetKey != null && !presetKey.isEmpty() && !presetKey.equals(safeName)) {
                if (loadFromDiskCache(scenario, cacheDir, presetKey)) return;
            }

            String type = scenario.getPopulationDensityType();
            if (type == null) type = "URBAN_CLUSTERS";

            SvgMapIngestor.SvgIngestionResult neResult = NaturalEarthVectorIngestor.loadNaturalEarthMap(type);
            if (neResult == null) neResult = SvgMapIngestor.ingestForScenario(type);
            if (neResult == null && scenario.getStartDateYear() != 0) {
                neResult = HgisAtlasIngestor.loadHistoricalPolityMap(scenario.getStartDateYear(), type);
            }

            BufferedImage imgDensity = (neResult != null && neResult.densityImage != null) ? neResult.densityImage : null;
            if (imgDensity == null) {
                if (scenario.getStartDateYear() <= -70000) {
                    imgDensity = applyAltimetryCoastlineMask(generatePrehistoricSyntheticDensityMap(scenario.getStartDateYear(), type));
                } else if (scenario.getStartDateYear() < -10000) {
                    BufferedImage baseHyde = Hyde34GridReader.loadForYear(-10000);
                    if (baseHyde != null) {
                        imgDensity = applyAltimetryCoastlineMask(applyPrehistoricGeographicMask(scenario.getPresetKey() != null ? scenario.getPresetKey() : safeName, scenario.getStartDateYear(), baseHyde));
                    } else {
                        imgDensity = applyAltimetryCoastlineMask(generatePrehistoricSyntheticDensityMap(scenario.getStartDateYear(), type));
                    }
                } else {
                    imgDensity = Hyde34GridReader.loadForYear(scenario.getStartDateYear());
                    if (imgDensity != null) {
                        imgDensity = applyAltimetryCoastlineMask(imgDensity);
                    } else {
                        imgDensity = applyAltimetryCoastlineMask(rasterizeDensityMap(type, scenario));
                    }
                }
            } else {
                imgDensity = applyAltimetryCoastlineMask(imgDensity);
            }
            scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));

            BufferedImage imgIsogloss = (neResult != null && neResult.isoglossImage != null) ? neResult.isoglossImage : rasterizeIsoglossMap(type, scenario);
            scenario.setCustomTensorMapBase64(0, bufferedImageToBase64Png(imgIsogloss));

            BufferedImage imgKinship = (neResult != null && neResult.kinshipImage != null) ? neResult.kinshipImage : rasterizeKinshipMap(type, scenario);
            scenario.setCustomTensorMapBase64(1, bufferedImageToBase64Png(imgKinship));

            BufferedImage imgRituals = (neResult != null && neResult.ritualsImage != null) ? neResult.ritualsImage : rasterizeRitualsMap(type, scenario);
            scenario.setCustomTensorMapBase64(2, bufferedImageToBase64Png(imgRituals));

            BufferedImage imgSovereignty = (neResult != null && neResult.sovereigntyImage != null) ? neResult.sovereigntyImage : rasterizeSovereigntyMap(type, scenario);
            scenario.setCustomTensorMapBase64(3, bufferedImageToBase64Png(imgSovereignty));

            BufferedImage imgTechnology = rasterizeTechnologyMap(type, scenario);
            scenario.setCustomTensorMapBase64(4, bufferedImageToBase64Png(imgTechnology));

            BufferedImage imgTrade = rasterizeTradeNetworkMap(type, scenario);
            scenario.setCustomTensorMapBase64(5, bufferedImageToBase64Png(imgTrade));

            BufferedImage imgInstitutional = rasterizeInstitutionalComplexityMap(type, scenario);
            scenario.setCustomTensorMapBase64(6, bufferedImageToBase64Png(imgInstitutional));

            BufferedImage imgEcological = rasterizeEcologicalFootprintMap(type, scenario);
            scenario.setCustomTensorMapBase64(7, bufferedImageToBase64Png(imgEcological));

            BufferedImage imgPathogen = rasterizePathogenImmunityMap(type, scenario);
            scenario.setCustomTensorMapBase64(8, bufferedImageToBase64Png(imgPathogen));

            int dims = scenario.getCultureVectorDimensions();
            if (dims > 9) {
                for (int i = 9; i < dims; i++) {
                    BufferedImage imgExt = rasterizeExtensibleTensorMap(i, type, scenario);
                    scenario.setCustomTensorMapBase64(i, bufferedImageToBase64Png(imgExt));
                }
            }

            BufferedImage imgCoal = rasterizeCoalMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(0, bufferedImageToBase64Png(imgCoal));

            BufferedImage imgOil = rasterizeOilMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(1, bufferedImageToBase64Png(imgOil));

            BufferedImage imgGas = rasterizeGasMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(2, bufferedImageToBase64Png(imgGas));

            BufferedImage imgUranium = rasterizeUraniumMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(3, bufferedImageToBase64Png(imgUranium));

            BufferedImage imgHe3 = rasterizeHelium3Map(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(4, bufferedImageToBase64Png(imgHe3));

            BufferedImage imgIronCopper = rasterizeIronCopperMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(5, bufferedImageToBase64Png(imgIronCopper));

            BufferedImage imgPreciousMetals = rasterizePreciousMetalsMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(6, bufferedImageToBase64Png(imgPreciousMetals));

            BufferedImage imgRareEarths = rasterizeRareEarthsMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(7, bufferedImageToBase64Png(imgRareEarths));

            BufferedImage imgMantleHeat = rasterizeMantleHeatMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(8, bufferedImageToBase64Png(imgMantleHeat));

            BufferedImage imgAquifer = rasterizeAquiferMap(type, scenario);
            scenario.setCustomGeologyTensorMapBase64(9, bufferedImageToBase64Png(imgAquifer));

            int resDims = scenario.getResourceVectorDimensions();
            if (resDims > 10) {
                for (int i = 10; i < resDims; i++) {
                    BufferedImage imgExtRes = rasterizeExtensibleResourceTensorMap(i, type, scenario);
                    scenario.setCustomGeologyTensorMapBase64(i, bufferedImageToBase64Png(imgExtRes));
                }
            }

            saveImagesToYearDirectory(scenario.getStartDateYear(), imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology, imgTrade, imgInstitutional, imgEcological, imgPathogen, imgCoal, imgOil, imgGas, imgUranium, imgHe3, imgIronCopper, imgPreciousMetals, imgRareEarths, imgMantleHeat, imgAquifer);
            saveImagesToDiskCache(scenario.getName(), imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology, imgTrade, imgInstitutional, imgEcological, imgPathogen);
            saveGeologyTensorsToDiskCache(scenario.getName(), imgCoal, imgOil, imgGas, imgUranium, imgHe3, imgIronCopper, imgPreciousMetals, imgRareEarths, imgMantleHeat, imgAquifer);

        } catch (Exception e) {
            logger.error("Failed to generate historical maps for scenario {}", scenario.getName(), e);
        }
    }

    public static void forceGenerateCulturalTensorsOnly(Scenario scenario) {
        if (scenario == null) return;
        try {
            String type = scenario.getPopulationDensityType();
            if (type == null) type = "URBAN_CLUSTERS";
            long year = scenario.getStartDateYear();

            BufferedImage imgDensity = rasterizeDensityMap(type, scenario);
            BufferedImage imgIsogloss = rasterizeIsoglossMap(type, scenario);
            BufferedImage imgKinship = rasterizeKinshipMap(type, scenario);
            BufferedImage imgRituals = rasterizeRitualsMap(type, scenario);
            BufferedImage imgSovereignty = rasterizeSovereigntyMap(type, scenario);
            BufferedImage imgTechnology = rasterizeTechnologyMap(type, scenario);
            BufferedImage imgTrade = rasterizeTradeNetworkMap(type, scenario);
            BufferedImage imgInstitutional = rasterizeInstitutionalComplexityMap(type, scenario);
            BufferedImage imgEcological = rasterizeEcologicalFootprintMap(type, scenario);
            BufferedImage imgPathogen = rasterizePathogenImmunityMap(type, scenario);

            BufferedImage imgAquifer = rasterizeAquiferMap(type, scenario);

            saveImagesToYearDirectory(year, imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology,
                    imgTrade, imgInstitutional, imgEcological, imgPathogen, null, null, null, null,
                    null, null, null, null, null, imgAquifer);

            saveImagesToDiskCache(scenario.getName(), imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTechnology, imgTrade, imgInstitutional, imgEcological, imgPathogen);
            logger.info("Successfully regenerated and persisted the COMPLETE 25-raster suite for scenario '{}' (Year {}).", scenario.getName(), year);
        } catch (Exception e) {
            logger.error("Failed to generate complete raster suite for scenario {}", scenario.getName(), e);
        }
    }

    public static void forceGenerateScenarioHistoricalMaps(Scenario scenario) {
        populateScenarioHistoricalMaps(scenario);
    }

    public static void ensureAllScenarioMapsGenerated() {
        ensureAllScenarioMapsGenerated(false);
    }

    public static void ensureAllScenarioMapsGenerated(boolean force) {
        List<Scenario> builtIns = Scenario.getBuiltInScenarios();
        logger.info("Verifying cartographic maps for all {} built-in scenarios in 'data/ether/earth/<year>/' (force={})...", builtIns.size(), force);
        int generated = 0;
        for (Scenario sc : builtIns) {
            long year = sc.getStartDateYear();
            java.nio.file.Path earthDir = java.nio.file.Paths.get("data", "ether", "earth", String.valueOf(year));
            boolean exists = !force &&
                             java.nio.file.Files.exists(earthDir.resolve("earth_" + year + "_density.png")) &&
                             java.nio.file.Files.exists(earthDir.resolve("earth_" + year + "_isogloss.png")) &&
                             java.nio.file.Files.exists(earthDir.resolve("earth_" + year + "_sovereignty.png"));
            if (!exists) {
                forceGenerateScenarioHistoricalMaps(sc);
                generated++;
            }
        }
        logger.info("Batch generation check complete. Generated/updated {} scenarios in data/ether/earth/.", generated);
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

