/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
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
 * @see HistoricalScenarioMapOrchestrator
 * @see HomininPaleoOccupancyCalculator
 * @see PaleoClimateMapRasterizer
 * @see GeologicalResourceMapRasterizer
 */
public class HistoricalMapGenerator {

    /** Standard global equirectangular raster width in pixels. */
    public static final int WIDTH = 2048;
    /** Standard global equirectangular raster height in pixels. */
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

    /** Computes hominin clade probability weights at (lon, lat). */
    public static double[] computeHomininCladeWeights(double lon, double lat) {
        return HomininPaleoOccupancyCalculator.computeHomininCladeWeights(lon, lat);
    }

    /** Returns 1-indexed dominant hominin entity identifier at (lon, lat). */
    public static int getHomininEntityId(double lon, double lat) {
        return HomininPaleoOccupancyCalculator.getHomininEntityId(lon, lat);
    }

    /** Returns hominin species classification index at (lon, lat). */
    public static int getHomininSpeciesType(double lon, double lat) {
        return HomininPaleoOccupancyCalculator.getHomininSpeciesType(lon, lat);
    }

    /** Blends paleolithic sociological traits across Sapiens, Neanderthal, and Denisovan clades. */
    public static double blendPaleoTraits(double lon, double lat, double valSapiens, double valNeanderthal, double valDenisovan) {
        return HomininPaleoOccupancyCalculator.blendPaleoTraits(lon, lat, valSapiens, valNeanderthal, valDenisovan);
    }

    /** Smooths a closed polygon via Chaikin corner-cutting subdivision. */
    public static double[][] chaikinSmoothPolygon(double[][] poly, int iterations) {
        return PaleoGeographyCoordinatesCatalog.chaikinSmoothPolygon(poly, iterations);
    }

    /** Computes minimum distance in degrees from (lon, lat) to a polyline. */
    public static double distToPolyline(double lon, double lat, double[][] line) {
        return HomininPaleoOccupancyCalculator.distToPolyline(lon, lat, line);
    }

    /** Computes signed distance in degrees from (lon, lat) to a closed polygon (negative inside). */
    public static double signedDistanceToPolygon(double lon, double lat, double[][] poly) {
        return HomininPaleoOccupancyCalculator.signedDistanceToPolygon(lon, lat, poly);
    }

    /** Returns {@code true} if (lon, lat) is an uninhabited oceanic island at {@code year}. */
    public static boolean isRemoteOceanicIsland(double lon, double lat, long year) {
        return HomininPaleoOccupancyCalculator.isRemoteOceanicIsland(lon, lat, year);
    }

    /** Computes continuous hominin occupancy weight in \([0.0, 1.0]\) at (lon, lat, year). */
    public static double getHomininOccupancyWeight(double lon, double lat, long year) {
        return HomininPaleoOccupancyCalculator.getHomininOccupancyWeight(lon, lat, year);
    }

    /** Returns {@code true} if (lon, lat) is occupied by hominins at {@code year}. */
    public static boolean isHomininOccupied(double lon, double lat, long year) {
        return HomininPaleoOccupancyCalculator.isHomininOccupied(lon, lat, year);
    }

    /** Returns {@code true} if (lng, lat) lies on continental or insular land. */
    public static boolean isLand(double lng, double lat) {
        return HistoricalLandGeometryCatalog.isLand(lng, lat);
    }

    /** Creates a pure transparent 2048x1024 ARGB canvas. */
    public static BufferedImage createPureTransparentCanvas() {
        return new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    }

    // --- PALEOCLIMATOLOGY DELEGATIONS ---

    /** Computes mean annual surface temperature (°C) for (lat, lon, elevM, year). */
    public static double computeSurfaceTemperature(double lat, double lon, double elevM, long year) {
        return PaleoClimateMapRasterizer.computeSurfaceTemperature(lat, lon, elevM, year);
    }

    /** Computes mean annual precipitation (mm/yr) for (lat, lon, elevM, year). */
    public static double computeAnnualPrecipitation(double lat, double lon, double elevM, long year) {
        return PaleoClimateMapRasterizer.computeAnnualPrecipitation(lat, lon, elevM, year);
    }

    /** Computes annual thermal seasonality amplitude (°C) for (lat, lon, elevM, year). */
    public static double computeSeasonalityAmplitude(double lat, double lon, double elevM, long year) {
        return PaleoClimateMapRasterizer.computeSeasonalityAmplitude(lat, lon, elevM, year);
    }

    /** Rasterizes the global biome classification map for {@code year}. */
    public static BufferedImage rasterizeBiomesMap(long year) {
        return PaleoClimateMapRasterizer.rasterizeBiomesMap(year);
    }

    /** Rasterizes the global surface temperature map for {@code year}. */
    public static BufferedImage rasterizeTemperatureMap(long year) {
        return PaleoClimateMapRasterizer.rasterizeTemperatureMap(year);
    }

    /** Rasterizes the global annual precipitation map for {@code year}. */
    public static BufferedImage rasterizePrecipitationMap(long year) {
        return PaleoClimateMapRasterizer.rasterizePrecipitationMap(year);
    }

    /** Rasterizes the global thermal seasonality map for {@code year}. */
    public static BufferedImage rasterizeSeasonalityMap(long year) {
        return PaleoClimateMapRasterizer.rasterizeSeasonalityMap(year);
    }

    // --- DISK CACHE & FILE PERSISTENCE DELEGATIONS ---

    /** Encodes a {@link BufferedImage} to a Base64 PNG string. */
    public static String bufferedImageToBase64Png(BufferedImage bImg) {
        return HistoricalMapDiskCacheManager.bufferedImageToBase64Png(bImg);
    }

    /** Writes a {@link BufferedImage} to disk as PNG. */
    public static void writePngFile(BufferedImage img, File targetFile) {
        HistoricalMapDiskCacheManager.writePngFile(img, targetFile);
    }

    /** Loads scenario rasters from {@code data/maps/ether/earth/<year>/} if available. */
    public static boolean loadFromYearDirectory(Scenario scenario) {
        return HistoricalMapDiskCacheManager.loadFromYearDirectory(scenario);
    }

    /** Loads scenario rasters from legacy {@code data/cache/} directory if available. */
    public static boolean loadFromDiskCache(Scenario scenario, Path cacheDir, String safeName) {
        return HistoricalMapDiskCacheManager.loadFromDiskCache(scenario, cacheDir, safeName);
    }

    /** Persists the full 20-channel demographic, cultural, and geological raster suite to {@code data/maps/ether/earth/<year>/}. */
    public static void saveImagesToYearDirectory(long year, BufferedImage imgDensity, BufferedImage imgSovereignty,
            BufferedImage imgIsogloss, BufferedImage imgKinship, BufferedImage imgRituals, BufferedImage imgTech,
            BufferedImage imgTrade, BufferedImage imgInst, BufferedImage imgEco, BufferedImage imgPathogen,
            BufferedImage imgCoal, BufferedImage imgOil, BufferedImage imgGas, BufferedImage imgUranium,
            BufferedImage imgHe3, BufferedImage imgIronCopper, BufferedImage imgPreciousMetals, BufferedImage imgRareEarths, BufferedImage imgMantleHeat, BufferedImage imgAquifer) {
        HistoricalMapDiskCacheManager.saveImagesToYearDirectory(year, imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTech, imgTrade, imgInst, imgEco, imgPathogen, imgCoal, imgOil, imgGas, imgUranium, imgHe3, imgIronCopper, imgPreciousMetals, imgRareEarths, imgMantleHeat, imgAquifer);
    }

    /** Persists cultural tensor rasters to {@code data/maps/ether/earth/<year>/}. */
    public static void saveCulturalTensorsToYearDirectory(long year, BufferedImage imgSovereignty,
            BufferedImage imgIsogloss, BufferedImage imgKinship, BufferedImage imgRituals, BufferedImage imgTech,
            BufferedImage imgTrade, BufferedImage imgInst, BufferedImage imgEco, BufferedImage imgPathogen) {
        HistoricalMapDiskCacheManager.saveCulturalTensorsToYearDirectory(year, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTech, imgTrade, imgInst, imgEco, imgPathogen);
    }

    /** Persists demographic and cultural rasters to {@code data/cache/}. */
    public static void saveImagesToDiskCache(String scenarioName, BufferedImage imgDensity, BufferedImage imgSovereignty, BufferedImage imgIsogloss, BufferedImage imgKinship, BufferedImage imgRituals, BufferedImage imgTech, BufferedImage imgTrade, BufferedImage imgInst, BufferedImage imgEco, BufferedImage imgPathogen) {
        HistoricalMapDiskCacheManager.saveImagesToDiskCache(scenarioName, imgDensity, imgSovereignty, imgIsogloss, imgKinship, imgRituals, imgTech, imgTrade, imgInst, imgEco, imgPathogen);
    }

    /** Persists geological resource rasters to {@code data/cache/}. */
    public static void saveGeologyTensorsToDiskCache(String scenarioName,
            BufferedImage imgCoal, BufferedImage imgOil, BufferedImage imgGas,
            BufferedImage imgUranium, BufferedImage imgHe3, BufferedImage imgIronCopper,
            BufferedImage imgPreciousMetals, BufferedImage imgRareEarths, BufferedImage imgMantleHeat, BufferedImage imgAquifer) {
        HistoricalMapDiskCacheManager.saveGeologyTensorsToDiskCache(scenarioName, imgCoal, imgOil, imgGas, imgUranium, imgHe3, imgIronCopper, imgPreciousMetals, imgRareEarths, imgMantleHeat, imgAquifer);
    }

    /** Generates or updates epoch provenance and README files inside an earth epoch directory. */
    public static void generateOrUpdateEpochMetadataFiles(Path earthDir, long year) {
        HistoricalEpochMetadataGenerator.generateOrUpdateEpochMetadataFiles(earthDir, year);
    }

    /** Applies Pleistocene glaciation and hominin dispersal masks to a base density raster. */
    public static BufferedImage applyPrehistoricGeographicMask(String scenarioKey, long year, BufferedImage src) {
        return PaleolithicDensityGenerator.applyPrehistoricGeographicMask(scenarioKey, year, src);
    }

    /** Generates analytical prehistoric population density maps for deep-time epochs. */
    public static BufferedImage generatePrehistoricSyntheticDensityMap(long year, String type) {
        return PaleolithicDensityGenerator.generatePrehistoricSyntheticDensityMap(year, type);
    }

    // --- CULTURAL & GEOLOGICAL RASTERIZER DELEGATIONS ---

    /** Blends clade RGB colors weighted by hominin clade probabilities and occupancy weight. */
    public static int blendCladeRgb(double[] w, int[] cladeColors, double occWeight) {
        return HistoricalCulturalTensorRasterizer.blendCladeRgb(w, cladeColors, occWeight);
    }

    /** Rasterizes the political sovereignty tensor map. */
    public static BufferedImage rasterizeSovereigntyMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeSovereigntyMap(type, scenario);
    }

    /** Rasterizes the linguistic isogloss tensor map. */
    public static BufferedImage rasterizeIsoglossMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeIsoglossMap(type, scenario);
    }

    /** Rasterizes the kinship and family structure tensor map. */
    public static BufferedImage rasterizeKinshipMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeKinshipMap(type, scenario);
    }

    /** Rasterizes the ritual and religious tradition tensor map. */
    public static BufferedImage rasterizeRitualsMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeRitualsMap(type, scenario);
    }

    /** Rasterizes the technological index tensor map. */
    public static BufferedImage rasterizeTechnologyMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeTechnologyMap(type, scenario);
    }

    /** Rasterizes the trade network connectivity tensor map. */
    public static BufferedImage rasterizeTradeNetworkMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeTradeNetworkMap(type, scenario);
    }

    /** Rasterizes the institutional administrative complexity tensor map. */
    public static BufferedImage rasterizeInstitutionalComplexityMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeInstitutionalComplexityMap(type, scenario);
    }

    /** Rasterizes the anthropogenic ecological footprint tensor map. */
    public static BufferedImage rasterizeEcologicalFootprintMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizeEcologicalFootprintMap(type, scenario);
    }

    /** Rasterizes the endemic pathogen immunity tensor map. */
    public static BufferedImage rasterizePathogenImmunityMap(String type, Scenario scenario) {
        return HistoricalCulturalTensorRasterizer.rasterizePathogenImmunityMap(type, scenario);
    }

    /** Rasterizes the coal reserves geological tensor map. */
    public static BufferedImage rasterizeCoalMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeCoalMap(type, scenario);
    }

    /** Rasterizes the crude oil reserves geological tensor map. */
    public static BufferedImage rasterizeOilMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeOilMap(type, scenario);
    }

    /** Rasterizes the natural gas reserves geological tensor map. */
    public static BufferedImage rasterizeGasMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeGasMap(type, scenario);
    }

    /** Rasterizes the uranium ore reserves geological tensor map. */
    public static BufferedImage rasterizeUraniumMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeUraniumMap(type, scenario);
    }

    /** Rasterizes the Helium-3 / regolith volatiles geological tensor map. */
    public static BufferedImage rasterizeHelium3Map(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeHelium3Map(type, scenario);
    }

    /** Rasterizes the iron and copper metallogenic provinces tensor map. */
    public static BufferedImage rasterizeIronCopperMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeIronCopperMap(type, scenario);
    }

    /** Rasterizes the gold and silver precious metals tensor map. */
    public static BufferedImage rasterizePreciousMetalsMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizePreciousMetalsMap(type, scenario);
    }

    /** Rasterizes the rare earth elements (REE) geological tensor map. */
    public static BufferedImage rasterizeRareEarthsMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeRareEarthsMap(type, scenario);
    }

    /** Rasterizes the Davies (2013) terrestrial mantle heat flow tensor map. */
    public static BufferedImage rasterizeMantleHeatMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeMantleHeatMap(type, scenario);
    }

    /** Computes paleoclimate aquifer recharge multiplier for (lon, lat, year). */
    public static double getPaleoAquiferRechargeFactor(double lon, double lat, long year) {
        return GeologicalResourceMapRasterizer.getPaleoAquiferRechargeFactor(lon, lat, year);
    }

    /** Rasterizes the UNESCO WHYMAP groundwater aquifer recharge tensor map. */
    public static BufferedImage rasterizeAquiferMap(String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeAquiferMap(type, scenario);
    }

    /** Rasterizes an extensible resource tensor map for custom resource dimensions {@code >= 10}. */
    public static BufferedImage rasterizeExtensibleResourceTensorMap(int index, String type, Scenario scenario) {
        return GeologicalResourceMapRasterizer.rasterizeExtensibleResourceTensorMap(index, type, scenario);
    }

    /** Loads USGS MRDS mineral deposit coordinates matching commodity keywords. */
    public static List<double[]> loadMRDSDeposits(String... commodityKeywords) {
        return GeologicalResourceMapRasterizer.loadMRDSDeposits(commodityKeywords);
    }

    /** Renders radial Gaussian alpha spots onto an ARGB geological layer. */
    public static void rasterizeSpotListToAlpha(BufferedImage img, List<double[]> spots, Color themeColor, double defaultRadiusPx) {
        GeologicalResourceMapRasterizer.rasterizeSpotListToAlpha(img, spots, themeColor, defaultRadiusPx);
    }

    /** Renders 3-tier color-graded radial Gaussian spots onto an ARGB geological layer. */
    public static void rasterizeTieredSpotList(BufferedImage img, List<double[]> spots, Color lowColor, Color medColor, Color highColor, double defaultRadiusPx) {
        GeologicalResourceMapRasterizer.rasterizeTieredSpotList(img, spots, lowColor, medColor, highColor, defaultRadiusPx);
    }

    // --- ELEVATION MASK & CARTOGRAPHIC PIPELINE DELEGATIONS ---

    /** Loads and caches the global 2048x1024 NOAA ETOPO coastline mask. */
    public static BufferedImage loadElevationMask() {
        return HistoricalScenarioMapOrchestrator.loadElevationMask();
    }

    /** Masks ocean pixels to pure black using the cached ETOPO coastline mask. */
    public static BufferedImage applyAltimetryCoastlineMask(BufferedImage src) {
        return HistoricalScenarioMapOrchestrator.applyAltimetryCoastlineMask(src);
    }

    /** Computes topographic and hyper-arid habitability factor in \([0.12, 1.0]\). */
    public static double getTopographicHabitability(String scenarioType, double lng, double lat) {
        return HistoricalScenarioMapOrchestrator.getTopographicHabitability(scenarioType, lng, lat);
    }

    /** Rasterizes the demographic density map for {@code scenario}'s start year. */
    public static BufferedImage rasterizeDensityMap(String type, Scenario scenario) {
        return rasterizeDensityMapForYear(type, scenario, (scenario != null) ? scenario.getStartDateYear() : -10000);
    }

    /** Rasterizes the demographic density map for an explicit {@code targetYear}. */
    public static BufferedImage rasterizeDensityMapForYear(String type, Scenario scenario, long targetYear) {
        return HistoricalScenarioMapOrchestrator.rasterizeDensityMapForYear(type, scenario, targetYear);
    }

    /** Synthesizes an extensible cultural tensor map for custom dimension {@code tensorIndex}. */
    public static BufferedImage rasterizeExtensibleTensorMap(int tensorIndex, String type, Scenario scenario) {
        return HistoricalScenarioMapOrchestrator.rasterizeExtensibleTensorMap(tensorIndex, type, scenario);
    }

    /** Creates an empty black RGB canvas of dimensions {@code width x height}. */
    public static BufferedImage rasterizeCoastlines(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return img;
    }

    /** Populates procedural/historical maps for {@code scenario}. */
    public static void generateProceduralMapsForScenario(Scenario scenario) {
        populateScenarioHistoricalMaps(scenario);
    }

    /** Populates all cartographic tensor maps for {@code scenario}, utilizing disk caches when available. */
    public static void populateScenarioHistoricalMaps(Scenario scenario) {
        HistoricalScenarioMapOrchestrator.populateScenarioHistoricalMaps(scenario);
    }

    /** Force-regenerates demographic, cultural, and aquifer rasters for {@code scenario}. */
    public static void forceGenerateCulturalTensorsOnly(Scenario scenario) {
        HistoricalScenarioMapOrchestrator.forceGenerateCulturalTensorsOnly(scenario);
    }

    /** Force-populates historical maps for {@code scenario}. */
    public static void forceGenerateScenarioHistoricalMaps(Scenario scenario) {
        populateScenarioHistoricalMaps(scenario);
    }

    /** Ensures all built-in scenario rasters exist on disk without forcing overwrite. */
    public static void ensureAllScenarioMapsGenerated() {
        ensureAllScenarioMapsGenerated(false);
    }

    /** Ensures all built-in scenario rasters exist on disk, optionally forcing regeneration. */
    public static void ensureAllScenarioMapsGenerated(boolean force) {
        HistoricalScenarioMapOrchestrator.ensureAllScenarioMapsGenerated(force);
    }

    /** Pre-caches cartographic tensor maps for all named scenario archetypes. */
    public static void precacheAllBuiltInScenarios() {
        HistoricalScenarioMapOrchestrator.precacheAllBuiltInScenarios();
    }
}
