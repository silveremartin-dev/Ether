/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Advanced Population Simulation & Density Generator.
 * Supports:
 * 1. Earth historical pre-generated human density distributions across pivot eras (-100k to 1800).
 * 2. Procedural tech-adapted human population distribution based on Tab 1 & Tab 2 parameters:
 *    - Heightmap & Relief suitability
 *    - Thermal & Climate comfort
 *    - Waterways / River proximity & Freshwater constraints
 *    - Technological adaptation capacity (Neolithic = strict river/coastal dependence; 17th-18th century = high variable adaptation).
 *
 * @author Silvere Martin-Michiellot
 * @version 2.3.0
 */
public class ProceduralPopulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(ProceduralPopulationEngine.class);

    /**
     * Historical pivot dates with estimated global population and tech levels for Earth preset.
     */
    public record EarthHistoricalPivot(long year, String eraName, long defaultPopulation, double techLevel, String description) {}

    public static final List<EarthHistoricalPivot> EARTH_PIVOTS = List.of(
            new EarthHistoricalPivot(-100000, "Out of Africa (-100,000 BP)", 15000L, 0.2, "Pre-Neolithic Sapiens: East African Rift & early coastal migration routes."),
            new EarthHistoricalPivot(-40000, "Upper Paleolithic (-40,000 BP)", 500000L, 0.6, "Paleolithic Hunter-Gatherers: Spread across Africa, Europe, Asia, and Australia."),
            new EarthHistoricalPivot(-10000, "Neolithic Revolution (-10,000 BP)", 5000000L, 1.2, "Agricultural Emergence: Fertile Crescent, Nile, Yangtze/Yellow, Indus, Mesoamerica."),
            new EarthHistoricalPivot(-3000, "First Empires / Bronze Age (-3,000 BCE)", 50000000L, 2.5, "River Civilizations: Mesopotamia, Egypt, Indus Valley, Shang China."),
            new EarthHistoricalPivot(-500, "Classical Era (-500 BCE)", 200000000L, 3.8, "Classical Antiquity: Mediterranean/Rome, Han Dynasty, Maurya India, Achaemenid Empire."),
            new EarthHistoricalPivot(1000, "High Middle Ages (1000 CE)", 300000000L, 4.5, "Feudal & Imperial Networks: Song China, Chola India, Europe, Abbasid Caliphate, Maya."),
            new EarthHistoricalPivot(1500, "Early Modern / Exploration (1500 CE)", 500000000L, 5.2, "Age of Discovery: Ming China, Ottoman Empire, Renaissance Europe, Mughal Empire."),
            new EarthHistoricalPivot(1800, "Industrial Revolution (1800 CE)", 1000000000L, 6.5, "Pre-Industrial Peak & Early Steam: High density in Europe, East Asia, and Eastern Americas.")
    );

    /**
     * Distributes total human population across cells according to Earth historical maps or procedural tech suitability.
     * Note: startYear is provided purely for logging/reference and does not affect demographic calculations.
     */
    public static void distributePopulation(List<H3Cell> cells, Scenario scenario, long totalPopulation, double techLevel, String pattern, boolean isEarthPreset, long startYear) {
        if (cells == null || cells.isEmpty()) return;

        // Reset populations first
        for (H3Cell c : cells) {
            c.setPopulation(0);
        }

        // Filter land cells
        List<H3Cell> landCells = cells.stream()
                .filter(c -> c.getElevation() > 0 && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                .toList();

        if (landCells.isEmpty()) return;

        double capitalPerCapita = scenario != null ? scenario.getInitialCapitalPerCapita() : Math.pow(10, (techLevel - 0.2) / 2.2);

        long seedVal = scenario != null ? scenario.getSeed() : 12345L;
        if (isEarthPreset) {
            distributeEarthHistorical(landCells, totalPopulation, techLevel, capitalPerCapita);
        } else {
            distributeProcedural(landCells, totalPopulation, techLevel, pattern, seedVal, capitalPerCapita);
        }
    }

    /**
     * Earth pre-generated historical population density mapping based on tech suitability.
     */
    private static void distributeEarthHistorical(List<H3Cell> landCells, long totalPopulation, double techLevel, double capitalPerCapita) {
        double[] weights = new double[landCells.size()];
        double totalWeight = 0.0;

        for (int i = 0; i < landCells.size(); i++) {
            H3Cell cell = landCells.get(i);
            double lat = cell.getLatitude();
            double lng = cell.getLongitude();
            Biome biome = cell.getBiome();

            double baseSuitability = calculateBiomeAndElevSuitability(cell, techLevel);
            double geoWeight = getEarthHistoricalRegionalWeight(lat, lng, biome, techLevel);

            double w = baseSuitability * geoWeight;
            weights[i] = Math.max(0.0001, w);
            totalWeight += weights[i];
        }

        applyNormalizedPopulation(landCells, weights, totalWeight, totalPopulation, capitalPerCapita);
        logger.info("Distributed {} humans on Earth map (Tech: {}, Capital: {} kg/capita)", totalPopulation, techLevel, capitalPerCapita);
    }

    /**
     * Regional weighting heuristic for Earth historical geography based on techLevel.
     */
    private static double getEarthHistoricalRegionalWeight(double lat, double lng, Biome biome, double techLevel) {
        boolean isEastAfrica = (lat >= -15 && lat <= 15) && (lng >= 25 && lng <= 45);
        boolean isFertileCrescent = (lat >= 28 && lat <= 38) && (lng >= 34 && lng <= 48);
        boolean isNileDelta = (lat >= 20 && lat <= 31) && (lng >= 28 && lng <= 34);
        boolean isIndusValley = (lat >= 22 && lat <= 34) && (lng >= 67 && lng <= 76);
        boolean isYellowYangtzeChina = (lat >= 22 && lat <= 41) && (lng >= 102 && lng <= 122);
        boolean isGangesIndia = (lat >= 8 && lat <= 28) && (lng >= 72 && lng <= 88);
        boolean isMediterraneanEurope = (lat >= 35 && lat <= 58) && (lng >= -10 && lng <= 30);
        boolean isMesoamerica = (lat >= 14 && lat <= 22) && (lng >= -105 && lng <= -88);
        boolean isAndes = (lat >= -20 && lat <= 0) && (lng >= -80 && lng <= -65);

        if (techLevel <= 0.5) {
            // Early Paleolithic / Out of Africa
            if (isEastAfrica) return 12.0;
            if (lat >= 10 && lat <= 30 && lng >= 35 && lng <= 75) return 2.0; // Early migration coastal belt
            return 0.1;
        } else if (techLevel <= 1.0) {
            // Upper Paleolithic
            if (isEastAfrica) return 5.0;
            if (isFertileCrescent || isNileDelta) return 4.0;
            if (isYellowYangtzeChina || isGangesIndia) return 4.0;
            if (isMediterraneanEurope) return 3.0;
            return 0.5;
        } else if (techLevel <= 1.8) {
            // Neolithic Revolution
            if (isFertileCrescent) return 15.0;
            if (isYellowYangtzeChina) return 12.0;
            if (isNileDelta) return 10.0;
            if (isIndusValley) return 8.0;
            if (isMesoamerica) return 6.0;
            if (isMediterraneanEurope) return 4.0;
            return 0.8;
        } else if (techLevel <= 3.0) {
            // Bronze Age
            if (isNileDelta || isFertileCrescent) return 18.0;
            if (isYellowYangtzeChina) return 16.0;
            if (isIndusValley) return 14.0;
            if (isMediterraneanEurope) return 5.0;
            if (isMesoamerica || isAndes) return 4.0;
            return 1.0;
        } else if (techLevel <= 4.2) {
            // Classical Antiquity
            if (isYellowYangtzeChina) return 20.0;
            if (isGangesIndia || isIndusValley) return 18.0;
            if (isMediterraneanEurope) return 18.0;
            if (isFertileCrescent || isNileDelta) return 12.0;
            if (isMesoamerica || isAndes) return 5.0;
            return 1.2;
        } else if (techLevel <= 5.2) {
            // Medieval
            if (isYellowYangtzeChina) return 22.0;
            if (isGangesIndia) return 20.0;
            if (isMediterraneanEurope) return 15.0;
            if (isFertileCrescent || isNileDelta) return 10.0;
            if (isMesoamerica || isAndes) return 6.0;
            return 1.5;
        } else if (techLevel <= 6.2) {
            // Early Modern
            if (isYellowYangtzeChina) return 22.0;
            if (isGangesIndia) return 20.0;
            if (isMediterraneanEurope) return 18.0;
            if (lat >= 30 && lat <= 50 && lng >= -100 && lng <= -70) return 4.0;
            return 2.0;
        } else {
            // Industrial Revolution
            if (isMediterraneanEurope) return 25.0;
            if (isYellowYangtzeChina) return 24.0;
            if (isGangesIndia) return 22.0;
            if (lat >= 30 && lat <= 48 && lng >= -90 && lng <= -70) return 12.0;
            return 2.5;
        }
    }

    /**
     * Procedural human population simulation taking into account heightmap, temperature, biomes,
     * river/waterway proximity, and technological adaptation capacity across eras.
     */
    private static void distributeProcedural(List<H3Cell> landCells, long totalPopulation, double techLevel, String pattern, long seed, double capitalPerCapita) {
        double[] weights = new double[landCells.size()];
        double totalWeight = 0.0;

        java.util.Random seedRand = new java.util.Random(seed != 0 ? seed : 12345L);

        for (int i = 0; i < landCells.size(); i++) {
            H3Cell cell = landCells.get(i);

            // 1. Environmental base suitability
            double envSuit = calculateBiomeAndElevSuitability(cell, techLevel);

            // 2. Temperature comfort suitability
            double tempSuit = calculateTemperatureSuitability(cell.getTemperature(), techLevel);

            // 3. Freshwater & River proximity constraint (Tech dependent!)
            double waterSuit = calculateWaterAndRiverSuitability(cell, techLevel);

            // 4. Resource & Carrying capacity bonus
            double resourceSuit = calculateResourceSuitability(cell);

            // 5. Pattern multiplier
            double patternMult = calculatePatternMultiplier(cell, pattern);

            // 6. Seed-based deterministic noise variation (0.8 to 1.2)
            double noiseMult = 0.80 + (seedRand.nextDouble() * 0.40);

            // Total weight formula
            double cellWeight = envSuit * tempSuit * waterSuit * resourceSuit * patternMult * noiseMult;

            weights[i] = Math.max(0.00001, cellWeight);
            totalWeight += weights[i];
        }

        // Apply tech-driven urbanization / clustering if tech level > 3.0 or high population
        if (techLevel >= 2.5 && "URBAN_CLUSTERS".equals(pattern)) {
            applyUrbanClustering(landCells, weights, techLevel);
            // Recalculate total weight after clustering
            totalWeight = 0.0;
            for (double w : weights) totalWeight += w;
        }

        applyNormalizedPopulation(landCells, weights, totalWeight, totalPopulation, capitalPerCapita);
        logger.info("Procedurally distributed {} humans across {} cells (Capital: {} kg/capita, Pattern: {})", totalPopulation, landCells.size(), capitalPerCapita, pattern);
    }

    /**
     * Biome & Elevation suitability adjusted by technological capacity.
     */
    private static double calculateBiomeAndElevSuitability(H3Cell cell, double techLevel) {
        Biome b = cell.getBiome();
        double elev = cell.getElevation();

        // Biome base score
        double biomeScore = switch (b) {
            case PLAINS -> 1.0;
            case FOREST -> 0.85;
            case BEACH -> 0.90;
            case HILLS -> 0.65;
            case JUNGLE -> 0.45;
            case TUNDRA -> 0.10 + (techLevel * 0.05); // High tech improves survival in cold
            case DESERT -> 0.05 + (techLevel * 0.06); // High tech allows irrigation/air-cond/wells
            case MOUNTAINS -> 0.02 + (techLevel * 0.04);
            case SNOW -> 0.005 + (techLevel * 0.01);
            default -> 0.1;
        };

        // Altitude suitability
        // Low tech (Neolithic): people cannot settle high altitudes (>800m).
        // High tech (Industrial): terrace farming, roads, heating allow living up to 3000m.
        double maxHabitableElev = 500.0 + (techLevel * 300.0); // e.g. Tech 0 = 500m, Tech 5 = 2000m, Tech 10 = 3500m
        double elevFactor = 1.0;

        if (elev > maxHabitableElev) {
            double overflow = (elev - maxHabitableElev) / 1000.0;
            elevFactor = Math.max(0.001, Math.exp(-overflow * (3.5 - techLevel * 0.25)));
        } else if (elev > 0) {
            elevFactor = 1.0 - (elev / (maxHabitableElev * 2.0));
        }

        return biomeScore * Math.max(0.01, elevFactor);
    }

    /**
     * Temperature comfort rating.
     * Optimal range: 12°C - 24°C.
     * Low tech has narrow tolerance; High tech broadens thermal adaptation.
     */
    private static double calculateTemperatureSuitability(double temp, double techLevel) {
        double optimalTemp = 18.0;
        // Thermal tolerance width sigma increases with technology
        double sigma = 8.0 + (techLevel * 2.0); // Tech 0: sigma 8, Tech 6: sigma 20
        double diff = temp - optimalTemp;

        return Math.exp(- (diff * diff) / (2.0 * sigma * sigma));
    }

    /**
     * Freshwater & River / Coastal proximity constraint.
     * Crucial user requirement:
     * - Neolithic / Prehistory (Tech <= 1.5): humans can ONLY live near rivers, lakes, coasts.
     *   Independence from water sources is severely penalized.
     * - 17th Century / Industrial (Tech >= 5.0): humans can dig deep wells, build aqueducts, transport water.
     */
    private static double calculateWaterAndRiverSuitability(H3Cell cell, double techLevel) {
        double water = cell.getWaterResource() != null ? cell.getWaterResource() : 0.0;
        double accessibleAquifer = cell.getAccessibleAquifer() != null ? cell.getAccessibleAquifer() : 0.0;
        Biome b = cell.getBiome();

        boolean isCoastal = (b == Biome.BEACH || b == Biome.PLAINS || cell.getElevation() < 50);
        boolean isRiverValley = (b == Biome.PLAINS && cell.getRainfall() > 0.4) || water > 400.0;

        // Base water score combining surface water, coastal access, AND accessible groundwater table (nappe phréatique accessible)
        double aquiferScore = Math.min(1.0, accessibleAquifer / 10000.0);
        double baseWaterScore = (water / 1000.0) * 0.45 + (aquiferScore * 0.35) + (isCoastal ? 0.2 : 0.0);
        if (isRiverValley) baseWaterScore += 0.3;

        // Accessible groundwater allows human settlement even in arid/dry areas (Oasis & shallow wells)
        if (accessibleAquifer > 3000.0 && (b == Biome.DESERT || b == Biome.PLAINS)) {
            baseWaterScore += 0.25;
        }

        baseWaterScore = Math.max(0.01, Math.min(1.0, baseWaterScore));

        // Tech constraint exponent:
        // Low tech (Tech 0-1): power = 2.5 to 3.0 (steep drop off if far from water/accessible aquifers)
        // High tech (Tech 5+): power = 0.5 (very lenient with deep drilling & aqueducts)
        double exponent = Math.max(0.4, 3.0 - (techLevel * 0.4));

        return Math.pow(baseWaterScore, exponent);
    }

    /**
     * Resource & carrying capacity bonus (Food, Wood, Metal, Agriculture).
     */
    private static double calculateResourceSuitability(H3Cell cell) {
        double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
        double wood = cell.getWoodResource() != null ? cell.getWoodResource() : 0.0;
        double metal = cell.getResourceMetal() != null ? cell.getResourceMetal() : 0.0;

        double bonus = 1.0 + (food / 2000.0) + (wood / 3000.0) + (metal / 4000.0);
        return Math.min(3.0, bonus);
    }

    /**
     * Pattern multiplier based on user selected density pattern.
     */
    private static double calculatePatternMultiplier(H3Cell cell, String pattern) {
        if (pattern == null) return 1.0;
        Biome b = cell.getBiome();

        return switch (pattern) {
            case "UNBIASED_NATURAL", "UNBIASED", "NONE", "NATURAL_EQUILIBRIUM" -> 1.0;
            case "FERTILE_CRESCENT", "RIVER_VALLEYS" -> (b == Biome.PLAINS || b == Biome.BEACH) ? 4.5 : 0.3;
            case "MESOAMERICA" -> (b == Biome.JUNGLE || b == Biome.HILLS || b == Biome.PLAINS) ? 3.5 : 0.5;
            case "MESOPOTAMIA_ASSYRIA" -> (b == Biome.PLAINS) ? 5.0 : 0.2;
            case "SPARSE_NOMADIC" -> (b == Biome.PLAINS || b == Biome.DESERT || b == Biome.TUNDRA) ? 1.5 : 0.8;
            default -> 1.0;
        };
    }

    /**
     * Creates high-density urban clusters on top suitable cells when tech permits cities.
     */
    private static void applyUrbanClustering(List<H3Cell> landCells, double[] weights, double techLevel) {
        int cellCount = landCells.size();
        int cityCount = Math.min(30, Math.max(2, (int) (cellCount * 0.03)));

        // Find top indices
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < cellCount; i++) indices.add(i);
        indices.sort((i1, i2) -> Double.compare(weights[i2], weights[i1]));

        // Boost top city centers
        double cityBoost = 5.0 + (techLevel * 2.0);
        for (int k = 0; k < cityCount && k < indices.size(); k++) {
            int idx = indices.get(k);
            weights[idx] *= cityBoost;
        }
    }

    /**
     * Normalizes weights so sum of cell populations exactly matches totalPopulation.
     * Computes cell terrain friction matrix and populates age pyramid cohorts based on tech level.
     */
    private static void applyNormalizedPopulation(List<H3Cell> landCells, double[] weights, double totalWeight, long totalPopulation, double capitalPerCapita) {
        if (totalWeight <= 0) return;

        double derivedTech = Math.clamp(Math.log10(Math.max(1.0, capitalPerCapita)) * 2.2 + 0.2, 0.2, 10.0);

        long assigned = 0;
        for (int i = 0; i < landCells.size(); i++) {
            H3Cell cell = landCells.get(i);
            double proportion = weights[i] / totalWeight;
            long pop = Math.round(totalPopulation * proportion);
            cell.setPopulation((int) Math.clamp(pop, 0L, (long) Integer.MAX_VALUE));
            
            // Seed physical capital & tool stocks from initial capital K0
            double cellCapital = pop * capitalPerCapita;
            cell.setResourceCapital(cellCapital);
            cell.setResourceMetal(cellCapital * 0.15);
            cell.setTechnologyLevel(derivedTech);

            // Calculate movement friction & age pyramid cohort distribution
            cell.calculateMovementFriction(derivedTech);
            cell.updateAgePyramidFromTotal(derivedTech);

            assigned += pop;
        }

        // Adjust rounding discrepancy on highest weight cell
        long diff = totalPopulation - assigned;
        if (diff != 0 && !landCells.isEmpty()) {
            H3Cell topCell = landCells.get(0);
            long newTopPop = Math.clamp((long) topCell.getPopulation() + diff, 0L, (long) Integer.MAX_VALUE);
            topCell.setPopulation((int) newTopPop);
            topCell.setResourceCapital(topCell.getPopulation() * capitalPerCapita);
            topCell.updateAgePyramidFromTotal(derivedTech);
        }
    }
}
