/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Advanced Population Simulation &amp; Density Generator.
 *
 * <p>Supports:</p>
 * <ol>
 *   <li>Earth historical human density distributions across pivot eras (-100k BP to 1800 CE).</li>
 *   <li>Procedural tech-adapted human population distribution based on topographic relief,
 *       thermal/climate niche (Xu et al. 2020), freshwater/aquifer proximity, Zipf urban agglomeration
 *       (Zipf 1949; Gabaix 1999), and pressurized extraterrestrial pioneer outposts.</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see ProceduralPopulationSuitabilityHelper
 */
public class ProceduralPopulationEngine extends AbstractSimulationEngine {
    public ProceduralPopulationEngine() {
        super("ProceduralPopulationEngine", "ProceduralPopulationEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(ProceduralPopulationEngine.class);

    /**
     * Historical pivot dates with estimated global population and tech levels for Earth preset.
     *
     * @param year              chronological year (negative BCE/BP, positive CE)
     * @param eraName           display name of the historical epoch
     * @param defaultPopulation estimated global human population
     * @param techLevel         baseline technological index
     * @param description       summary of settlement geography
     */
    public record EarthHistoricalPivot(long year, String eraName, long defaultPopulation, double techLevel, String description) {}

    /** Canonical Earth historical demographic pivot epochs. */
    public static final List<EarthHistoricalPivot> EARTH_PIVOTS = loadEarthPivots();

    private static List<EarthHistoricalPivot> loadEarthPivots() {
        String path = "data/history/ether/earth/demographics/earth_historical_pivots.json";
        try {
            java.io.File file = new java.io.File(path);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            if (file.exists()) {
                return Collections.unmodifiableList(mapper.readValue(file, new com.fasterxml.jackson.core.type.TypeReference<List<EarthHistoricalPivot>>() {}));
            }
            java.io.InputStream is = ProceduralPopulationEngine.class.getResourceAsStream("/" + path);
            if (is != null) {
                try (is) {
                    return Collections.unmodifiableList(mapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<List<EarthHistoricalPivot>>() {}));
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load earth historical pivots from JSON, falling back to canonical defaults", e);
        }
        return List.of(
                new EarthHistoricalPivot(-100000, "Out of Africa (-100,000 BP)", 15000L, 0.2, "Pre-Neolithic Sapiens: East African Rift & early coastal migration routes."),
                new EarthHistoricalPivot(-40000, "Upper Paleolithic (-40,000 BP)", 500000L, 0.6, "Paleolithic Hunter-Gatherers: Spread across Africa, Europe, Asia, and Australia."),
                new EarthHistoricalPivot(-10000, "Neolithic Revolution (-10,000 BP)", 5000000L, 1.2, "Agricultural Emergence: Fertile Crescent, Nile, Yangtze/Yellow, Indus, Mesoamerica."),
                new EarthHistoricalPivot(-3000, "First Empires / Bronze Age (-3,000 BCE)", 50000000L, 2.5, "River Civilizations: Mesopotamia, Egypt, Indus Valley, Shang China."),
                new EarthHistoricalPivot(-500, "Classical Era (-500 BCE)", 200000000L, 3.8, "Classical Antiquity: Mediterranean/Rome, Han Dynasty, Maurya India, Achaemenid Empire."),
                new EarthHistoricalPivot(1000, "High Middle Ages (1000 CE)", 300000000L, 4.5, "Feudal & Imperial Networks: Song China, Chola India, Europe, Abbasid Caliphate, Maya."),
                new EarthHistoricalPivot(1500, "Early Modern / Exploration (1500 CE)", 500000000L, 5.2, "Age of Discovery: Ming China, Ottoman Empire, Renaissance Europe, Mughal Empire."),
                new EarthHistoricalPivot(1800, "Industrial Revolution (1800 CE)", 1000000000L, 6.5, "Pre-Industrial Peak & Early Steam: High density in Europe, East Asia, and Eastern Americas.")
        );
    }

    /**
     * Distributes total human population across cells according to hostile outpost constraints or procedural tech suitability.
     *
     * @param cells           target H3 cell grid
     * @param scenario        active scenario configuration
     * @param totalPopulation total initial population headcount
     * @param techLevel       initial technological index
     * @param pattern         spatial density distribution pattern
     * @param isEarthPreset   whether using Earth cartography
     * @param startYear       starting chronological year
     */
    public static void distributePopulation(List<H3Cell> cells, Scenario scenario, long totalPopulation,
                                            double techLevel, org.ether.society.model.PopulationDistributionPattern pattern, boolean isEarthPreset, long startYear) {
        distributePopulation(cells, scenario, totalPopulation, techLevel, pattern != null ? pattern.name() : "UNBIASED_NATURAL", isEarthPreset, startYear);
    }

    public static void distributePopulation(List<H3Cell> cells, Scenario scenario, long totalPopulation,
                                            double techLevel, String pattern, boolean isEarthPreset, long startYear) {
        if (cells == null || cells.isEmpty()) return;

        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            if (c == null) continue;
            c.setPopulation(0);
            c.setBiomassHuman(0.0);
        }

        List<H3Cell> landCells = new ArrayList<>(cells.size());
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            if (c == null) continue;
            if ((c.getBiome() != null && c.getBiome() != Biome.DEEP_OCEAN)
                    || (c.getElevation() != null && c.getElevation() > -200.0)
                    || Boolean.TRUE.equals(c.getIsCoastal())
                    || Boolean.TRUE.equals(c.getIsPolder())
                    || Boolean.TRUE.equals(c.getHasFloatingInfrastructure())
                    || techLevel >= 8.5) {
                landCells.add(c);
            }
        }

        if (landCells.isEmpty()) {
            for (int i = 0; i < cells.size(); i++) {
                H3Cell c = cells.get(i);
                if (c != null) landCells.add(c);
            }
        }

        double capitalPerCapita = scenario != null ? scenario.getInitialCapitalPerCapita() : Math.pow(10, (techLevel - 0.2) / 2.2);
        long seedVal = scenario != null ? scenario.getSeed() : 12345L;

        org.ether.society.generation.PlanetPreset preset = scenario != null ? scenario.getPlanetPreset() : null;
        boolean isHostile = org.ether.society.engines.tier1.PressurizedHabitatEngine.isHostileEnvironment(preset);

        if (isHostile) {
            distributeExtraterrestrialPioneers(landCells, preset, scenario, totalPopulation, capitalPerCapita);
        } else {
            distributeProcedural(landCells, totalPopulation, techLevel, pattern, seedVal, capitalPerCapita);
        }
    }

    /**
     * Concentrates extraterrestrial pioneering populations strictly into isolated pressurized outposts.
     */
    private static void distributeExtraterrestrialPioneers(List<H3Cell> landCells,
                                                           org.ether.society.generation.PlanetPreset preset,
                                                           Scenario scenario,
                                                           long totalPopulation,
                                                           double capitalPerCapita) {
        if (landCells == null || landCells.isEmpty() || totalPopulation <= 0) return;

        for (H3Cell c : landCells) {
            c.setPopulation(0);
            c.setBiomassHuman(0.0);
            c.setHabitatType(org.ether.society.model.HabitatType.NONE);
        }

        String presetKey = scenario != null && scenario.getPresetKey() != null ? scenario.getPresetKey().toLowerCase() : "";
        String scenarioName = scenario != null && scenario.getName() != null ? scenario.getName().toLowerCase() : "";

        record OutpostTarget(double lat, double lng, double popShare, String name) {}
        List<OutpostTarget> targets = new ArrayList<>();

        if (presetKey.contains("mars") || scenarioName.contains("mars")) {
            targets.add(new OutpostTarget(22.4, -49.8, 0.60, "Chryse Planitia Outpost"));
            targets.add(new OutpostTarget(18.38, 77.58, 0.40, "Jezero Crater Science Hub"));
        } else if (presetKey.contains("moon") || presetKey.contains("shackleton") || scenarioName.contains("lune") || scenarioName.contains("moon")) {
            targets.add(new OutpostTarget(-89.9, 0.0, 1.0, "Shackleton Crater Base"));
        } else if (presetKey.contains("venus") || scenarioName.contains("venus") || scenarioName.contains("vénus")) {
            targets.add(new OutpostTarget(0.0, 0.0, 0.70, "Aphrodite Cloud Station"));
            targets.add(new OutpostTarget(0.0, 120.0, 0.30, "Ishtar Aerostat Station"));
        } else if (presetKey.contains("mercury") || presetKey.contains("caloris") || scenarioName.contains("mercure")) {
            targets.add(new OutpostTarget(30.5, -170.0, 1.0, "Caloris Forge Colony"));
        } else if (presetKey.contains("titan") || scenarioName.contains("titan")) {
            targets.add(new OutpostTarget(68.0, 50.0, 1.0, "Kraken Mare Hydrocarbon Base"));
        } else {
            List<H3Cell> sortedByShelter = landCells.stream()
                    .sorted(Comparator.comparingDouble(c -> c.getElevationOr(0.0)))
                    .toList();
            if (!sortedByShelter.isEmpty()) {
                int outpostCount = Math.clamp(landCells.size() / 200, 1, 5);
                double remainingShare = 1.0;
                for (int idx = 0; idx < outpostCount; idx++) {
                    double share = (idx == outpostCount - 1) ? remainingShare : (0.50 / Math.pow(2.0, idx));
                    remainingShare -= share;
                    H3Cell c = sortedByShelter.get(idx % sortedByShelter.size());
                    targets.add(new OutpostTarget(c.getLatitudeOr(0.0), c.getLongitudeOr(0.0), share, "Outpost Habitat #" + (idx + 1)));
                }
            }
        }

        long assignedPop = 0;
        double derivedTech = Math.clamp(Math.log10(Math.max(1.0, capitalPerCapita)) * 2.2 + 0.2, 0.2, 10.0);

        for (int i = 0; i < targets.size(); i++) {
            OutpostTarget target = targets.get(i);
            H3Cell bestCell = null;
            double minDistSq = Double.MAX_VALUE;
            for (H3Cell c : landCells) {
                double dLat = c.getLatitudeOr(0.0) - target.lat();
                double dLng = c.getLongitudeOr(0.0) - target.lng();
                double distSq = dLat * dLat + dLng * dLng;
                if (distSq < minDistSq) {
                    minDistSq = distSq;
                    bestCell = c;
                }
            }

            if (bestCell != null) {
                long outpostPop = (i == targets.size() - 1)
                        ? (totalPopulation - assignedPop)
                        : Math.round(totalPopulation * target.popShare());
                assignedPop += outpostPop;

                bestCell.setPopulation((int) outpostPop);
                bestCell.setBiomassHuman((double) outpostPop);
                bestCell.setResourceCapital(outpostPop * capitalPerCapita);
                bestCell.setResourceMetal(outpostPop * capitalPerCapita * 0.20);
                bestCell.setFoodResource(outpostPop * 36.0 * 10.0);
                bestCell.setEnergyFire(outpostPop * 80.0);
                bestCell.calculateMovementFriction();
                bestCell.updateAgePyramidFromTotal();

                org.ether.society.engines.tier1.PressurizedHabitatEngine.initializePioneerOutpost(bestCell, preset, outpostPop);
                logger.info("Placed Extraterrestrial Outpost '{}' at ({}, {}) with {} colonists.",
                        target.name(), bestCell.getLatitudeOr(0.0), bestCell.getLongitudeOr(0.0), outpostPop);
            }
        }
    }

    /**
     * Procedural human population distribution taking into account relief, thermal niche,
     * freshwater/aquifer proximity, carrying capacity, and technological adaptation across eras.
     */
    private static void distributeProcedural(List<H3Cell> landCells, long totalPopulation,
                                             double techLevel, String pattern, long seed, double capitalPerCapita) {
        double[] weights = new double[landCells.size()];
        double totalWeight = 0.0;

        java.util.Random seedRand = new java.util.Random(seed != 0 ? seed : 12345L);

        for (int i = 0; i < landCells.size(); i++) {
            H3Cell cell = landCells.get(i);
            double envSuit = ProceduralPopulationSuitabilityHelper.calculateBiomeAndElevSuitability(cell, techLevel);
            double tempSuit = ProceduralPopulationSuitabilityHelper.calculateTemperatureSuitability(cell.getTemperatureOr(15.0), techLevel);
            double waterSuit = ProceduralPopulationSuitabilityHelper.calculateWaterAndRiverSuitability(cell, techLevel);
            double resourceSuit = ProceduralPopulationSuitabilityHelper.calculateResourceSuitability(cell);
            double patternMult = ProceduralPopulationSuitabilityHelper.calculatePatternMultiplier(cell, pattern);
            double noiseMult = 0.80 + (seedRand.nextDouble() * 0.40);

            double cellWeight = envSuit * tempSuit * waterSuit * resourceSuit * patternMult * noiseMult;
            weights[i] = Math.max(0.00001, cellWeight);
            totalWeight += weights[i];
        }

        if (techLevel >= 2.5 && "URBAN_CLUSTERS".equals(pattern)) {
            applyUrbanClustering(landCells, weights, techLevel);
            totalWeight = 0.0;
            for (double w : weights) totalWeight += w;
        }

        applyNormalizedPopulation(landCells, weights, totalWeight, totalPopulation, capitalPerCapita, techLevel);
        logger.info("Procedurally distributed {} humans across {} cells (Capital: {} kg/capita, Pattern: {})",
                totalPopulation, landCells.size(), capitalPerCapita, pattern);
    }

    /**
     * Applies continuous Zipf's law agglomeration scaling across cell weights (Zipf 1949, Gabaix 1999).
     */
    private static void applyUrbanClustering(List<H3Cell> landCells, double[] weights, double techLevel) {
        int cellCount = landCells.size();
        if (cellCount == 0) return;

        double sum = 0.0;
        for (int i = 0; i < cellCount; i++) sum += weights[i];
        double meanWeight = sum / cellCount;
        if (meanWeight <= 0.0) return;

        double alphaUrban = 0.35 * Math.min(1.0, Math.max(0.0, techLevel / 10.0));
        for (int i = 0; i < cellCount; i++) {
            if (weights[i] > meanWeight) {
                weights[i] *= Math.pow(weights[i] / meanWeight, alphaUrban);
            }
        }
    }

    /**
     * Detects urban nodes / spatial density singularities on the H3 grid based on local population maxima.
     *
     * @param cells input H3 cell list
     * @return sorted list of high-density urban hub cells
     */
    public static List<H3Cell> detectUrbanNodes(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return List.of();

        List<H3Cell> populated = cells.stream()
                .filter(c -> c.getPopulation() != null && c.getPopulation() > 0)
                .sorted(Comparator.comparingInt(H3Cell::getPopulation).reversed())
                .toList();

        if (populated.isEmpty()) return List.of();

        int maxPop = populated.get(0).getPopulation();
        int threshold = Math.max(10, (int) (maxPop * 0.20));

        List<H3Cell> nodes = new ArrayList<>();
        for (H3Cell c : populated) {
            if (c.getPopulation() >= threshold) {
                nodes.add(c);
            } else {
                break;
            }
        }
        return nodes;
    }

    /**
     * Normalizes weights so sum of cell populations matches {@code totalPopulation}, spatializes
     * physical capital $K(x)$, food reserves $F(x)$, energy $E(x)$, and initializes age pyramids.
     */
    private static void applyNormalizedPopulation(List<H3Cell> landCells, double[] weights, double totalWeight,
                                                  long totalPopulation, double capitalPerCapita, double techLevel) {
        if (totalWeight <= 0) return;

        double derivedTech = (techLevel > 0.0)
                ? techLevel
                : Math.clamp(1.0 + Math.log10(Math.max(1.0, capitalPerCapita / 50.0)) * 1.4, 0.2, 10.0);

        long assigned = 0;
        long maxCellPop = 1;
        for (int i = 0; i < landCells.size(); i++) {
            H3Cell cell = landCells.get(i);
            double proportion = weights[i] / totalWeight;
            long pop = Math.round(totalPopulation * proportion);
            cell.setPopulation((int) Math.clamp(pop, 0L, (long) Integer.MAX_VALUE));
            cell.setBiomassHuman((double) Math.clamp(pop, 0L, (long) Integer.MAX_VALUE));

            if (cell.getPopulation() > maxCellPop) {
                maxCellPop = cell.getPopulation();
            }
            assigned += pop;
        }

        long diff = totalPopulation - assigned;
        if (diff != 0 && !landCells.isEmpty()) {
            H3Cell topCell = landCells.stream()
                    .filter(c -> c.getPopulation() != null && c.getPopulation() > 0)
                    .max(Comparator.comparingInt(H3Cell::getPopulation))
                    .orElse(landCells.get(0));
            long curTopPop = topCell.getPopulationOr(0);
            long newTopPop = Math.clamp(curTopPop + diff, 0L, (long) Integer.MAX_VALUE);
            topCell.setPopulation((int) newTopPop);
            topCell.setBiomassHuman((double) newTopPop);
            if (newTopPop > maxCellPop) maxCellPop = newTopPop;
        }

        double baseCapital = capitalPerCapita;
        for (H3Cell cell : landCells) {
            int pop = cell.getPopulationOr(0);
            double normDensity = maxCellPop > 0 ? ((double) pop / (double) maxCellPop) : 0.0;

            double envSuit = ProceduralPopulationSuitabilityHelper.calculateBiomeAndElevSuitability(cell, derivedTech);
            double cellCapitalPerCapita = baseCapital * (0.4 + 0.6 * normDensity) * Math.clamp(envSuit, 0.2, 2.0);
            double cellCapitalTotal = pop * cellCapitalPerCapita;
            cell.setResourceCapital(cellCapitalTotal);
            cell.setResourceMetal(cellCapitalTotal * 0.15);

            double foodBiomeFactor = cell.getFoodResource() != null && cell.getFoodResource() > 0 ? Math.clamp(cell.getFoodResource() / 1000.0, 0.2, 2.0) : 1.0;
            double soilNPK = cell.getSoilOrganicCarbon() != null && cell.getSoilOrganicCarbon() > 0 ? Math.clamp(cell.getSoilOrganicCarbon() / 100.0, 0.2, 2.5) : 1.0;
            cell.setFoodResource(pop * 6.0 * foodBiomeFactor * soilNPK * 10.0);
            if (derivedTech >= 1.2) {
                cell.setBiomassAgriculture(pop * 420.0 * foodBiomeFactor);
            }

            double energyBiomePot = (cell.getWoodResource() != null ? cell.getWoodResource() / 1000.0 : 0.5) * 0.5
                    + (cell.getMantleHeatFlow() != null ? cell.getMantleHeatFlow() / 100.0 : 0.8) * 0.5;
            double cellEnergyPerCapita = 50.0 * (0.7 * (cellCapitalPerCapita / Math.max(1.0, baseCapital)) + 0.3 * Math.clamp(energyBiomePot, 0.1, 2.0));
            cell.setEnergyFire(pop * cellEnergyPerCapita);

            cell.calculateMovementFriction();
            cell.updateAgePyramidFromTotal();
        }
    }

    // --- Delegated Paleoclimatic, Orbital & Ecological Functions ---

    /**
     * Calculates planetary solar insolation ($\text{W/m}^2$) dynamically based on celestial mechanics.
     *
     * @param lat             latitude in degrees
     * @param obliquityDeg    axial obliquity in degrees
     * @param eccentricity    orbital eccentricity
     * @param semiMajorAxisAU semi-major axis in AU
     * @param startYearBP     epoch year
     * @param isEarth         whether using Earth Milankovitch orbital solution
     * @return solar insolation in $\text{W/m}^2$
     */
    public static double calculatePlanetarySolarInsolation(double lat, double obliquityDeg, double eccentricity,
                                                           double semiMajorAxisAU, long startYearBP, boolean isEarth) {
        return ProceduralPopulationSuitabilityHelper.calculatePlanetarySolarInsolation(
                lat, obliquityDeg, eccentricity, semiMajorAxisAU, startYearBP, isEarth);
    }

    /**
     * Standard Earth-preset wrapper for Milankovitch summer insolation at $65^\circ\text{N}$.
     *
     * @param startYearBP epoch year
     * @return summer insolation at $65^\circ\text{N}$ in $\text{W/m}^2$
     */
    public static double calculateMilankovitchSummerInsolation65N(long startYearBP) {
        return ProceduralPopulationSuitabilityHelper.calculateMilankovitchSummerInsolation65N(startYearBP);
    }

    /**
     * Calculates plant Net Primary Productivity (NPP) multiplier based on EPICA Dome C atmospheric $\text{CO}_2$.
     *
     * @param startYearBP epoch year
     * @return $\text{CO}_2$ vegetation multiplier
     */
    public static double calculateCO2VegetationMultiplier(long startYearBP) {
        return ProceduralPopulationSuitabilityHelper.calculateCO2VegetationMultiplier(startYearBP);
    }

    /**
     * Calculates coastal foraging carrying capacity boost ($2.5\times$) along intertidal coastal margins.
     *
     * @param cell target H3 cell
     * @return coastal foraging multiplier
     */
    public static double calculateCoastalForagingMultiplier(H3Cell cell) {
        return ProceduralPopulationSuitabilityHelper.calculateCoastalForagingMultiplier(cell);
    }

    /**
     * Computes Quaternary Megafauna Extinction (QME) abundance index (Sandom et al. 2014; Barnosky 2004).
     *
     * @param startYearBP   epoch year
     * @param humanDensity  human population density
     * @param climateStress climate stress index
     * @return megafauna abundance percentage $\in [1.0, 100.0]$
     */
    public static double calculateMegafaunaAbundanceIndex(long startYearBP, double humanDensity, double climateStress) {
        return ProceduralPopulationSuitabilityHelper.calculateMegafaunaAbundanceIndex(startYearBP, humanDensity, climateStress);
    }
}
