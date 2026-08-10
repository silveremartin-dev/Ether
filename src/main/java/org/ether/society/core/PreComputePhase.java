/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.database.H3Cell;
import org.ether.society.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Pre-computation phase that runs BEFORE simulation ticks begin.
 * 
 * Responsibilities:
 * 1. Interpolate low-resolution data to cell level
 * 2. Compute initial climate based on planet parameters
 * 3. Initialize food source maps based on biomes
 * 4. Set up initial resource stockpiles
 * 5. Distribute initial human population
 */
public class PreComputePhase {
    private static final Logger logger = LoggerFactory.getLogger(PreComputePhase.class);

    private final Scenario scenario;
    private final Random random;

    public PreComputePhase(Scenario scenario) {
        this.scenario = scenario;
        this.random = new Random(scenario.getPlanetPreset() != null
                ? scenario.getPlanetPreset().seed()
                : System.currentTimeMillis());
    }

    /**
     * Run all pre-computation steps on the given cells.
     */
    public void execute(List<H3Cell> cells) {
        logger.info("=== PreCompute Phase Starting ===");
        logger.info("Processing {} cells for scenario: {}", cells.size(), scenario.getName());

        long startTime = System.currentTimeMillis();

        // Step 1: Climate computation
        computeClimate(cells);

        // Step 2: Apply Future Scenario Physical Forcings if registered
        org.ether.society.procedural.FutureScenarioRegistry.PhysicalScenarioPreset futurePreset =
                org.ether.society.procedural.FutureScenarioRegistry.findPresetForScenario(scenario);
        if (futurePreset != null) {
            org.ether.society.procedural.FutureScenarioRegistry.applyScenarioForcing(futurePreset, cells);
        }

        // Step 3: Initialize resources based on biome
        initializeResources(cells);

        // Step 4: Distribute initial population
        distributeInitialPopulation(cells);

        // Step 5: Seed physical state parameters (Capital K0, Metal, Tech, Friction, Cohorts)
        seedInitialPhysicalState(cells);

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("=== PreCompute Phase Complete in {}ms ===", elapsed);
    }

    /**
     * Compute climate (temperature, precipitation) and glaciological forcings for each cell.
     * Handles: LGM ice sheets (2,000m depth, -120m sea level), Green Sahara, Younger Dryas.
     */
    private void computeClimate(List<H3Cell> cells) {
        logger.info("Computing climate and glaciological forcings...");
        double axialTilt = scenario.getAxialTiltDegrees();
        long year = scenario.getStartDateYear();

        boolean isGlacialMax = year <= -12000;
        boolean isYoungerDryas = (year >= -10900 && year <= -9700) || "YOUNGER_DRYAS".equals(scenario.getPopulationDensityType());
        boolean isGreenSahara = (year >= -8000 && year <= -5000) || "GREEN_SAHARA".equals(scenario.getPopulationDensityType());

        for (H3Cell cell : cells) {
            double lat = cell.getLatitude();
            double lng = cell.getLongitude();
            double elev = cell.getElevation();

            // Base temperature from latitude
            double latFactor = Math.abs(lat) / 90.0;
            double baseTemp = 30.0 - (latFactor * 50.0 * (axialTilt / 23.5));

            // Glacial Maximum LGM cooling (-6°C global) & Sea Level Drop (-120m)
            if (isGlacialMax) {
                baseTemp -= 6.0;
                cell.setSeaLevelOffsetMeters(-120.0);

                // Laurentide & Fennoscandian Ice Sheets (up to 2,000 meters thickness)
                if (lat > 48.0 && ((lng >= -160.0 && lng <= -50.0) || (lng >= -10.0 && lng <= 50.0))) {
                    double iceMeters = Math.min(2000.0, (lat - 48.0) * 120.0);
                    cell.setIceSheetThicknessMeters(iceMeters);
                    cell.setDynamicAlbedo(0.80);
                    cell.setBiome(Biome.GLACIER);
                    baseTemp -= (iceMeters * 0.005);
                }
            } else {
                cell.setSeaLevelOffsetMeters(0.0);
                cell.setIceSheetThicknessMeters(0.0);
            }

            // Younger Dryas abrupt cooling (-5.5°C in N. Atlantic)
            if (isYoungerDryas && lat >= 30.0 && lat <= 65.0 && lng >= -30.0 && lng <= 45.0) {
                baseTemp -= 5.5;
            }

            // Green Sahara African Humid Period (AHP)
            if (isGreenSahara && lat >= 12.0 && lat <= 30.0 && lng >= -15.0 && lng <= 35.0) {
                baseTemp = Math.min(baseTemp, 27.0);
            }

            double elevEffect = -elev * 0.006;
            double harshness = scenario.getClimateHarshness();
            double tempVariation = (random.nextDouble() - 0.5) * 10.0 * harshness;

            cell.setTemperature(baseTemp + elevEffect + tempVariation);

            // Precipitation: based on latitude, ocean proximity, and paleoclimate events
            double basePrecip = 1000 * (1 - Math.abs(lat - 45) / 90.0);

            if (isGreenSahara && lat >= 12.0 && lat <= 30.0 && lng >= -15.0 && lng <= 35.0) {
                basePrecip = 1200.0; // Lush savannah precipitation in Sahara
                cell.setBiome(Biome.SAVANNAH);
            } else if (isYoungerDryas && lat >= 28.0 && lat <= 38.0 && lng >= 30.0 && lng <= 45.0) {
                basePrecip = 250.0; // Aridification stress in Levant driving wild cereal foraging
            }

            if (cell.getBiome() == Biome.OCEAN || cell.getBiome() == Biome.DEEP_OCEAN) {
                basePrecip = 0;
            }
            cell.setRainfall(Math.max(0, basePrecip + random.nextDouble() * 500));
        }
    }

    /**
     * Initialize resource stockpiles based on biome type and coastal marine abundance.
     */
    private void initializeResources(List<H3Cell> cells) {
        logger.info("Initializing resources & coastal marine biomes...");

        for (H3Cell cell : cells) {
            Biome biome = cell.getBiome();
            if (biome == null)
                biome = Biome.PLAINS;

            // Detect coastal cell status & marine resources
            boolean coastal = biome == Biome.BEACH || (cell.getElevation() != null && cell.getElevation() > 0 && cell.getElevation() < 150.0);
            cell.setIsCoastal(coastal);
            if (coastal) {
                cell.setCoastalMarineResource(800.0 + random.nextDouble() * 200.0);
                cell.setBiomassFish(1200.0);
            }

            // Food resources based on biome
            double wildFood = switch (biome) {
                case JUNGLE -> 800 + random.nextDouble() * 400;
                case FOREST -> 600 + random.nextDouble() * 300;
                case SAVANNAH -> 700 + random.nextDouble() * 300;
                case PLAINS -> 400 + random.nextDouble() * 200;
                case HILLS -> 300 + random.nextDouble() * 150;
                case BEACH -> 200 + random.nextDouble() * 100;
                case TUNDRA -> 100 + random.nextDouble() * 50;
                case DESERT, SNOW -> 20 + random.nextDouble() * 30;
                case GLACIER -> 10 + random.nextDouble() * 10;
                case OCEAN, DEEP_OCEAN -> 0; // Fish handled separately
                case MOUNTAINS -> 50 + random.nextDouble() * 50;
            };
            cell.setFoodResource(wildFood);

            // Wood resources
            double wood = switch (biome) {
                case JUNGLE -> 1000 + random.nextDouble() * 500;
                case FOREST -> 800 + random.nextDouble() * 400;
                case SAVANNAH -> 400 + random.nextDouble() * 200;
                case HILLS, PLAINS -> 200 + random.nextDouble() * 100;
                default -> 0;
            };
            cell.setWoodResource(wood);

            // Water resources (also used as proxy for freshwater access)
            double water = switch (biome) {
                case JUNGLE -> 1000;
                case SAVANNAH -> 800;
                case FOREST, PLAINS -> 600 + random.nextDouble() * 200;
                case HILLS, BEACH -> 400;
                case TUNDRA, MOUNTAINS -> 300; // Snow melt
                case DESERT -> 50;
                case OCEAN, DEEP_OCEAN -> 0; // Salt water
                case SNOW, GLACIER -> 200;
            };
            cell.setWaterResource(water);
        }
    }

    /**
     * Calculate carrying capacity based on food and water resources.
     * 
     * @return carrying capacity for the cell
     */
    private double calculateCarryingCapacity(H3Cell cell) {
        double food = cell.getFoodResource();
        double water = cell.getWaterResource();
        Biome biome = cell.getBiome();

        // Normalize water to 0-1 scale (max 1000)
        double waterFactor = Math.min(1.0, water / 1000.0);

        // Base capacity: food availability * water factor
        double capacity = (food / 100.0) * (0.3 + waterFactor * 0.7);

        // Biome habitability modifier
        capacity *= switch (biome != null ? biome : Biome.PLAINS) {
            case JUNGLE -> 0.8; // Disease, difficulty
            case SAVANNAH -> 1.1; // Rich game & pasture
            case FOREST, PLAINS -> 1.0;
            case HILLS -> 0.9;
            case BEACH -> 0.7;
            case TUNDRA -> 0.4;
            case DESERT, SNOW -> 0.2;
            case GLACIER -> 0.05;
            case MOUNTAINS -> 0.3;
            case OCEAN, DEEP_OCEAN -> 0.0;
        };

        return Math.max(0, capacity);
    }

    /**
     * Distribute initial human population based on scenario settings.
     */
    private void distributeInitialPopulation(List<H3Cell> cells) {
        logger.info("Distributing initial population: {} humans", scenario.getInitialHumanCount());

        // Filter habitable cells (positive carrying capacity)
        List<H3Cell> habitable = cells.stream()
                .filter(c -> c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                .filter(c -> calculateCarryingCapacity(c) > 0)
                .toList();

        if (habitable.isEmpty()) {
            logger.warn("No habitable cells found!");
            return;
        }

        long totalPop = scenario.getInitialHumanCount();
        String distType = scenario.getPopulationDensityType();

        switch (distType) {
            case "ONE_CONTINENT", "OUT_OF_AFRICA" -> {
                // Cluster population in Africa origin (tropical/savannah/plains)
                H3Cell origin = habitable.stream()
                        .filter(c -> c.getLatitude() >= 0 && c.getLatitude() <= 15)
                        .filter(c -> c.getLongitude() >= 30 && c.getLongitude() <= 45)
                        .findFirst()
                        .orElse(habitable.stream()
                                .filter(c -> Math.abs(c.getLatitude()) < 30)
                                .findFirst()
                                .orElse(habitable.get(random.nextInt(habitable.size()))));
                int centerIdx = habitable.indexOf(origin);
                distributeNearCell(habitable, centerIdx, totalPop, 0.15);
            }
            case "BERINGIA_AMERICAS" -> {
                // Cluster population in Beringia / NE Siberia region
                H3Cell beringiaOrigin = habitable.stream()
                        .filter(c -> c.getLatitude() >= 55.0 && c.getLatitude() <= 72.0)
                        .filter(c -> c.getLongitude() >= 150.0 || c.getLongitude() <= -150.0)
                        .findFirst()
                        .orElse(habitable.stream()
                                .filter(c -> c.getLatitude() > 50.0)
                                .findFirst()
                                .orElse(habitable.get(0)));
                int centerIdx = habitable.indexOf(beringiaOrigin);
                distributeNearCell(habitable, centerIdx, totalPop, 0.20);
            }
            case "GREEN_SAHARA" -> {
                // Distribute population across lush North African savannahs
                List<H3Cell> saharaCells = habitable.stream()
                        .filter(c -> c.getLatitude() >= 12.0 && c.getLatitude() <= 28.0)
                        .filter(c -> c.getLongitude() >= -10.0 && c.getLongitude() <= 30.0)
                        .toList();
                if (!saharaCells.isEmpty()) {
                    distributeByCapacity(saharaCells, totalPop);
                } else {
                    distributeByCapacity(habitable, totalPop);
                }
            }
            case "YOUNGER_DRYAS" -> {
                // Cluster population in the Levant / Near East Natufian region
                H3Cell levantOrigin = habitable.stream()
                        .filter(c -> c.getLatitude() >= 30.0 && c.getLatitude() <= 38.0)
                        .filter(c -> c.getLongitude() >= 30.0 && c.getLongitude() <= 42.0)
                        .findFirst()
                        .orElse(habitable.get(0));
                int centerIdx = habitable.indexOf(levantOrigin);
                distributeNearCell(habitable, centerIdx, totalPop, 0.25);
            }
            case "ROMAN_EMPIRE" -> {
                // Distribute population across Roman Mediterranean provinces
                List<H3Cell> romanCells = habitable.stream()
                        .filter(c -> c.getLatitude() >= 25.0 && c.getLatitude() <= 55.0)
                        .filter(c -> c.getLongitude() >= -10.0 && c.getLongitude() <= 45.0)
                        .toList();
                if (!romanCells.isEmpty()) {
                    distributeByRomanDensity(romanCells, totalPop);
                } else {
                    distributeByCapacity(habitable, totalPop);
                }
            }
            case "RIVER_VALLEYS", "FERTILE_CRESCENT", "MESOPOTAMIA_ASSYRIA" -> {
                // Prefer high water access
                distributeByWater(habitable, totalPop);
            }
            case "COASTAL" -> {
                // Prefer beach biome and cells near ocean
                distributeByCoast(habitable, cells, totalPop);
            }
            case "SPARSE", "SPARSE_NOMADIC" -> {
                // Low-density scatter across all habitable terrain
                distributeByCapacity(habitable, totalPop);
                // Reduce all populations to simulate sparse nomadic life
                for (H3Cell c : habitable) {
                    c.setPopulation((int) (c.getPopulation() * 0.3));
                }
            }
            case "DENSE", "URBAN_CLUSTERS" -> {
                // High-density in optimal areas only
                List<H3Cell> optimal = habitable.stream()
                        .filter(c -> calculateCarryingCapacity(c) > 0.5)
                        .toList();
                if (optimal.isEmpty())
                    optimal = habitable;
                distributeByCapacity(optimal, totalPop);
            }
            case "AUSTRALIA_SAHUL" -> {
                List<H3Cell> sahulCells = habitable.stream()
                        .filter(c -> c.getLatitude() >= -42.0 && c.getLatitude() <= -10.0)
                        .filter(c -> c.getLongitude() >= 112.0 && c.getLongitude() <= 155.0)
                        .toList();
                if (!sahulCells.isEmpty()) {
                    distributeBySahulDensity(sahulCells, totalPop);
                } else {
                    distributeByCapacity(habitable, totalPop);
                }
            }
            case "EGYPT_NILE" -> {
                List<H3Cell> nileCells = habitable.stream()
                        .filter(c -> c.getLatitude() >= 21.0 && c.getLatitude() <= 32.0)
                        .filter(c -> c.getLongitude() >= 27.0 && c.getLongitude() <= 35.0)
                        .toList();
                if (!nileCells.isEmpty()) {
                    distributeByNileDensity(nileCells, totalPop);
                } else {
                    distributeByWater(habitable, totalPop);
                }
            }
            case "MESOAMERICA" -> {
                List<H3Cell> mesoCells = habitable.stream()
                        .filter(c -> c.getLatitude() >= 12.0 && c.getLatitude() <= 24.0)
                        .filter(c -> c.getLongitude() >= -105.0 && c.getLongitude() <= -85.0)
                        .toList();
                if (!mesoCells.isEmpty()) {
                    distributeByMesoamericaDensity(mesoCells, totalPop);
                } else {
                    distributeByCapacity(habitable, totalPop);
                }
            }
            case "AMERICAS_1491" -> {
                List<H3Cell> americasCells = habitable.stream()
                        .filter(c -> c.getLongitude() >= -130.0 && c.getLongitude() <= -35.0)
                        .toList();
                if (!americasCells.isEmpty()) {
                    distributeByAmericas1491Density(americasCells, totalPop);
                } else {
                    distributeByCapacity(habitable, totalPop);
                }
            }
            case "INDUSTRIAL_1800" -> {
                distributeByIndustrial1800Density(habitable, totalPop);
            }
            case "JAPAN_SAKOKU" -> {
                List<H3Cell> japanCells = habitable.stream()
                        .filter(c -> c.getLatitude() >= 30.0 && c.getLatitude() <= 45.0)
                        .filter(c -> c.getLongitude() >= 128.0 && c.getLongitude() <= 146.0)
                        .toList();
                if (!japanCells.isEmpty()) {
                    distributeByJapanSakokuDensity(japanCells, totalPop);
                } else {
                    distributeByCapacity(habitable, totalPop);
                }
            }
            case "WEST_AFRICA_MALI" -> {
                List<H3Cell> maliCells = habitable.stream()
                        .filter(c -> c.getLatitude() >= 5.0 && c.getLatitude() <= 25.0)
                        .filter(c -> c.getLongitude() >= -18.0 && c.getLongitude() <= 15.0)
                        .toList();
                if (!maliCells.isEmpty()) {
                    distributeByWestAfricaMaliDensity(maliCells, totalPop);
                } else {
                    distributeByCapacity(habitable, totalPop);
                }
            }
            case "INDIA_MAURYA" -> {
                List<H3Cell> indiaCells = habitable.stream()
                        .filter(c -> c.getLatitude() >= 8.0 && c.getLatitude() <= 35.0)
                        .filter(c -> c.getLongitude() >= 68.0 && c.getLongitude() <= 90.0)
                        .toList();
                if (!indiaCells.isEmpty()) {
                    distributeByIndiaMauryaDensity(indiaCells, totalPop);
                } else {
                    distributeByCapacity(habitable, totalPop);
                }
            }
            case "COLUMBIAN_CONTACT" -> {
                distributeByColumbianContactDensity(habitable, totalPop);
            }
            case "RANDOM" -> {
                // Randomized with capacity weighting
                distributeByCapacity(habitable, totalPop);
                // Add random noise
                for (H3Cell c : habitable) {
                    double factor = 0.5 + random.nextDouble();
                    c.setPopulation((int) (c.getPopulation() * factor));
                }
            }
            case "UNIFORM" -> {
                // Equal population in all habitable cells
                int perCell = (int) (totalPop / habitable.size());
                for (H3Cell c : habitable) {
                    c.setPopulation(perCell);
                }
            }
            case "EMPTY" -> {
                // No initial population
                for (H3Cell c : cells) {
                    c.setPopulation(0);
                }
            }
            default -> {
                // Default to capacity-based
                distributeByCapacity(habitable, totalPop);
            }
        }

        // Log distribution stats
        long totalAssigned = cells.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
        long populatedCells = cells.stream().filter(c -> c.getPopulation() != null && c.getPopulation() > 0).count();
        logger.info("Assigned {} population across {} cells (of {} habitable)",
                totalAssigned, populatedCells, habitable.size());
    }

    /**
     * Seeds initial physical state parameters across cells based on scenario configuration.
     */
    private void seedInitialPhysicalState(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        double capitalK0 = scenario.getInitialCapitalPerCapita();
        double techLevel = Math.clamp(Math.log10(Math.max(1.0, capitalK0)) * 2.2 + 0.2, 0.2, 10.0);

        for (H3Cell c : cells) {
            long pop = c.getPopulation() != null ? c.getPopulation() : 0;
            double cellCapital = pop * capitalK0;
            c.setResourceCapital(cellCapital);
            c.setResourceMetal(cellCapital * 0.15);
            c.setTechnologyLevel(techLevel);

            c.calculateMovementFriction(techLevel);
            c.updateAgePyramidFromTotal(techLevel);
        }
    }

    private void distributeByCoast(List<H3Cell> habitable, List<H3Cell> all, long totalPop) {
        // Weight by proximity to ocean
        double[] weights = new double[habitable.size()];
        double totalWeight = 0;

        for (int i = 0; i < habitable.size(); i++) {
            H3Cell c = habitable.get(i);

            // Beach biome gets high weight
            if (c.getBiome() == Biome.BEACH) {
                weights[i] = 5.0;
            } else {
                // Check if any neighbor is ocean
                double coastalBonus = 0;
                for (H3Cell other : all) {
                    if (other.getBiome() == Biome.OCEAN || other.getBiome() == Biome.DEEP_OCEAN) {
                        double dist = Math.sqrt(
                                Math.pow(c.getLatitude() - other.getLatitude(), 2) +
                                        Math.pow(c.getLongitude() - other.getLongitude(), 2));
                        if (dist < 5.0) { // Within ~500km
                            coastalBonus = Math.max(coastalBonus, 3.0 - dist * 0.5);
                        }
                    }
                }
                weights[i] = 1.0 + coastalBonus;
            }
            totalWeight += weights[i];
        }

        // Distribute population
        for (int i = 0; i < habitable.size(); i++) {
            long pop = Math.round(totalPop * weights[i] / totalWeight);
            habitable.get(i).setPopulation((int) Math.clamp(pop, 0L, (long) Integer.MAX_VALUE));
        }
    }

    private void distributeNearCell(List<H3Cell> cells, int centerIndex, long totalPop, double spread) {
        H3Cell center = cells.get(centerIndex);
        double centerLat = center.getLatitude();
        double centerLng = center.getLongitude();

        // Calculate weights based on distance
        double[] weights = new double[cells.size()];
        double totalWeight = 0;

        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            double dist = Math.sqrt(
                    Math.pow(c.getLatitude() - centerLat, 2) +
                            Math.pow(c.getLongitude() - centerLng, 2));
            weights[i] = Math.exp(-dist * spread * 10);
            totalWeight += weights[i];
        }

        // Distribute population
        for (int i = 0; i < cells.size(); i++) {
            long pop = Math.round(totalPop * weights[i] / totalWeight);
            cells.get(i).setPopulation((int) Math.clamp(pop, 0L, (long) Integer.MAX_VALUE));
        }
    }

    private void distributeByWater(List<H3Cell> cells, long totalPop) {
        double totalWeight = cells.stream()
                .mapToDouble(c -> c.getWaterResource() != null ? c.getWaterResource() : 0.0)
                .sum();

        if (totalWeight == 0) {
            // Fallback to equal distribution
            long perCell = totalPop / cells.size();
            cells.forEach(c -> c.setPopulation((int) Math.clamp(perCell, 0L, (long) Integer.MAX_VALUE)));
            return;
        }

        for (H3Cell c : cells) {
            double weight = c.getWaterResource() != null ? c.getWaterResource() : 0.0;
            long pop = Math.round(totalPop * weight / totalWeight);
            c.setPopulation((int) Math.clamp(pop, 0L, (long) Integer.MAX_VALUE));
        }
    }

    private void distributeByCapacity(List<H3Cell> cells, long totalPop) {
        double totalWeight = cells.stream()
                .mapToDouble(this::calculateCarryingCapacity)
                .sum();

        if (totalWeight == 0) {
            // Fallback to equal distribution
            long perCell = totalPop / cells.size();
            cells.forEach(c -> c.setPopulation((int) Math.clamp(perCell, 0L, (long) Integer.MAX_VALUE)));
            return;
        }

        for (H3Cell c : cells) {
            double weight = calculateCarryingCapacity(c);
            long pop = Math.round(totalPop * weight / totalWeight);
            c.setPopulation((int) Math.clamp(pop, 0L, (long) Integer.MAX_VALUE));
        }
    }

    private void distributeByRomanDensity(List<H3Cell> cells, long totalPop) {
        double[] weights = new double[cells.size()];
        double totalWeight = 0;

        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            double lat = c.getLatitude();
            double lng = c.getLongitude();

            // Distance to Rome (41.9, 12.5)
            double distRome = Math.sqrt(Math.pow(lat - 41.9, 2) + Math.pow(lng - 12.5, 2));
            // Distance to Alexandria (31.2, 29.9)
            double distAlex = Math.sqrt(Math.pow(lat - 31.2, 2) + Math.pow(lng - 29.9, 2));

            double centerBonus = Math.max(3.0 - distRome * 0.2, 0.0) + Math.max(2.5 - distAlex * 0.2, 0.0);
            double coastalBonus = Boolean.TRUE.equals(c.getIsCoastal()) ? 1.5 : 1.0;
            double baseCap = calculateCarryingCapacity(c);

            weights[i] = (baseCap + 0.1) * (1.0 + centerBonus) * coastalBonus;
            totalWeight += weights[i];
        }

        if (totalWeight == 0) {
            long perCell = totalPop / cells.size();
            cells.forEach(c -> c.setPopulation((int) Math.clamp(perCell, 0L, (long) Integer.MAX_VALUE)));
            return;
        }

        for (int i = 0; i < cells.size(); i++) {
            long pop = Math.round(totalPop * weights[i] / totalWeight);
            cells.get(i).setPopulation((int) Math.clamp(pop, 0L, (long) Integer.MAX_VALUE));
        }
    }

    private void distributeBySahulDensity(List<H3Cell> cells, long totalPop) {
        double[] weights = new double[cells.size()];
        double totalWeight = 0;
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            double coastalBonus = Boolean.TRUE.equals(c.getIsCoastal()) ? 2.0 : 0.8;
            double waterBonus = c.getWaterResource() != null ? c.getWaterResource() * 0.002 : 0.1;
            weights[i] = (calculateCarryingCapacity(c) + 0.1) * coastalBonus * (1.0 + waterBonus);
            totalWeight += weights[i];
        }
        distributeWithWeights(cells, weights, totalWeight, totalPop);
    }

    private void distributeByNileDensity(List<H3Cell> cells, long totalPop) {
        double[] weights = new double[cells.size()];
        double totalWeight = 0;
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            double waterBonus = c.getWaterResource() != null ? c.getWaterResource() * 0.005 : 0.05;
            weights[i] = waterBonus + 0.05;
            totalWeight += weights[i];
        }
        distributeWithWeights(cells, weights, totalWeight, totalPop);
    }

    private void distributeByMesoamericaDensity(List<H3Cell> cells, long totalPop) {
        double[] weights = new double[cells.size()];
        double totalWeight = 0;
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            double distTenochtitlan = Math.sqrt(Math.pow(lat - 19.4, 2) + Math.pow(lng - 99.1, 2));
            double distTikal = Math.sqrt(Math.pow(lat - 17.2, 2) + Math.pow(lng - 89.6, 2));
            double hubBonus = Math.max(3.0 - distTenochtitlan * 0.3, 0.0) + Math.max(3.0 - distTikal * 0.3, 0.0);
            weights[i] = (calculateCarryingCapacity(c) + 0.1) * (1.0 + hubBonus);
            totalWeight += weights[i];
        }
        distributeWithWeights(cells, weights, totalWeight, totalPop);
    }

    private void distributeByAmericas1491Density(List<H3Cell> cells, long totalPop) {
        double[] weights = new double[cells.size()];
        double totalWeight = 0;
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            double meso = (lat >= 12 && lat <= 24 && lng >= -105 && lng <= -85) ? 3.0 : 0.2;
            double andes = (lat >= -22 && lat <= 2 && lng >= -82 && lng <= -68) ? 3.5 : 0.2;
            double mississippi = (lat >= 30 && lat <= 40 && lng >= -92 && lng <= -80) ? 1.5 : 0.2;
            weights[i] = calculateCarryingCapacity(c) * (meso + andes + mississippi);
            totalWeight += weights[i];
        }
        distributeWithWeights(cells, weights, totalWeight, totalPop);
    }

    private void distributeByIndustrial1800Density(List<H3Cell> cells, long totalPop) {
        double[] weights = new double[cells.size()];
        double totalWeight = 0;
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            double europe = (lat >= 35 && lat <= 60 && lng >= -10 && lng <= 30) ? 4.0 : 0.5;
            double eastAsia = (lat >= 20 && lat <= 42 && lng >= 100 && lng <= 130) ? 3.5 : 0.5;
            double northAmerica = (lat >= 30 && lat <= 50 && lng >= -90 && lng <= -70) ? 1.5 : 0.5;
            weights[i] = calculateCarryingCapacity(c) * (europe + eastAsia + northAmerica);
            totalWeight += weights[i];
        }
        distributeWithWeights(cells, weights, totalWeight, totalPop);
    }

    private void distributeByJapanSakokuDensity(List<H3Cell> cells, long totalPop) {
        double[] weights = new double[cells.size()];
        double totalWeight = 0;
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            double distEdo = Math.sqrt(Math.pow(lat - 35.7, 2) + Math.pow(lng - 139.7, 2));
            double distKyoto = Math.sqrt(Math.pow(lat - 35.0, 2) + Math.pow(lng - 135.7, 2));
            double distOsaka = Math.sqrt(Math.pow(lat - 34.7, 2) + Math.pow(lng - 135.5, 2));
            double urbanHotspot = Math.max(5.0 - distEdo * 1.5, 0.0) + Math.max(4.0 - distKyoto * 1.5, 0.0) + Math.max(4.0 - distOsaka * 1.5, 0.0);
            double coastalBonus = Boolean.TRUE.equals(c.getIsCoastal()) ? 1.8 : 1.0;
            weights[i] = (calculateCarryingCapacity(c) + 0.1) * (1.0 + urbanHotspot) * coastalBonus;
            totalWeight += weights[i];
        }
        distributeWithWeights(cells, weights, totalWeight, totalPop);
    }

    private void distributeByWestAfricaMaliDensity(List<H3Cell> cells, long totalPop) {
        double[] weights = new double[cells.size()];
        double totalWeight = 0;
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            double distTimbuktu = Math.sqrt(Math.pow(lat - 16.7, 2) + Math.pow(lng - (-3.0), 2));
            double distGao = Math.sqrt(Math.pow(lat - 16.3, 2) + Math.pow(lng - 0.0, 2));
            double distJenne = Math.sqrt(Math.pow(lat - 13.9, 2) + Math.pow(lng - (-4.5), 2));
            double urbanHotspot = Math.max(4.0 - distTimbuktu * 1.2, 0.0) + Math.max(3.5 - distGao * 1.2, 0.0) + Math.max(4.0 - distJenne * 1.2, 0.0);
            double waterBonus = c.getWaterResource() != null ? c.getWaterResource() * 0.003 : 0.1;
            weights[i] = (calculateCarryingCapacity(c) + 0.1) * (1.0 + urbanHotspot + waterBonus);
            totalWeight += weights[i];
        }
        distributeWithWeights(cells, weights, totalWeight, totalPop);
    }

    private void distributeByIndiaMauryaDensity(List<H3Cell> cells, long totalPop) {
        double[] weights = new double[cells.size()];
        double totalWeight = 0;
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            double distPatali = Math.sqrt(Math.pow(lat - 25.6, 2) + Math.pow(lng - 85.1, 2));
            double distTaxila = Math.sqrt(Math.pow(lat - 33.7, 2) + Math.pow(lng - 72.8, 2));
            double distVaranasi = Math.sqrt(Math.pow(lat - 25.3, 2) + Math.pow(lng - 83.0, 2));
            double urbanHotspot = Math.max(5.0 - distPatali * 0.8, 0.0) + Math.max(4.0 - distTaxila * 0.8, 0.0) + Math.max(4.0 - distVaranasi * 0.8, 0.0);
            double waterBonus = c.getWaterResource() != null ? c.getWaterResource() * 0.004 : 0.1;
            weights[i] = (calculateCarryingCapacity(c) + 0.1) * (1.0 + urbanHotspot + waterBonus);
            totalWeight += weights[i];
        }
        distributeWithWeights(cells, weights, totalWeight, totalPop);
    }

    private void distributeByColumbianContactDensity(List<H3Cell> cells, long totalPop) {
        double[] weights = new double[cells.size()];
        double totalWeight = 0;
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            double distTeno = Math.sqrt(Math.pow(lat - 19.4, 2) + Math.pow(lng - (-99.1), 2));
            double distSanto = Math.sqrt(Math.pow(lat - 18.5, 2) + Math.pow(lng - (-69.9), 2));
            double distCusco = Math.sqrt(Math.pow(lat - (-13.5), 2) + Math.pow(lng - (-71.9), 2));
            double urbanHotspot = Math.max(5.0 - distTeno * 0.5, 0.0) + Math.max(4.0 - distSanto * 0.5, 0.0) + Math.max(5.0 - distCusco * 0.5, 0.0);
            weights[i] = (calculateCarryingCapacity(c) + 0.1) * (1.0 + urbanHotspot);
            totalWeight += weights[i];
        }
        distributeWithWeights(cells, weights, totalWeight, totalPop);
    }

    private void distributeWithWeights(List<H3Cell> cells, double[] weights, double totalWeight, long totalPop) {
        if (totalWeight == 0) {
            long perCell = totalPop / cells.size();
            cells.forEach(c -> c.setPopulation((int) Math.clamp(perCell, 0L, (long) Integer.MAX_VALUE)));
            return;
        }
        for (int i = 0; i < cells.size(); i++) {
            long pop = Math.round(totalPop * weights[i] / totalWeight);
            cells.get(i).setPopulation((int) Math.clamp(pop, 0L, (long) Integer.MAX_VALUE));
        }
    }
}
