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

        // Step 2: Initialize resources based on biome
        initializeResources(cells);

        // Step 3: Distribute initial population
        distributeInitialPopulation(cells);

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("=== PreCompute Phase Complete in {}ms ===", elapsed);
    }

    /**
     * Compute climate (temperature, precipitation) for each cell.
     * Uses: latitude, elevation, axial tilt, month.
     */
    private void computeClimate(List<H3Cell> cells) {
        logger.info("Computing climate...");
        double axialTilt = scenario.getAxialTiltDegrees();

        for (H3Cell cell : cells) {
            double lat = cell.getLatitude();
            double elev = cell.getElevation();

            // Base temperature from latitude (equator=hot, poles=cold)
            double latFactor = Math.abs(lat) / 90.0;
            double baseTemp = 30.0 - (latFactor * 50.0 * (axialTilt / 23.5));

            // Elevation effect: -6°C per 1000m
            double elevEffect = -elev * 0.006;

            // Apply climate harshness (more extreme temps)
            double harshness = scenario.getClimateHarshness();
            double tempVariation = (random.nextDouble() - 0.5) * 10.0 * harshness;

            cell.setTemperature(baseTemp + elevEffect + tempVariation);

            // Precipitation: based on latitude and proximity to ocean
            double basePrecip = 1000 * (1 - Math.abs(lat - 45) / 90.0);
            if (cell.getBiome() == Biome.OCEAN || cell.getBiome() == Biome.DEEP_OCEAN) {
                basePrecip = 0; // Ocean cells don't have rainfall
            }
            cell.setRainfall(Math.max(0, basePrecip + random.nextDouble() * 500));
        }
    }

    /**
     * Initialize resource stockpiles based on biome type.
     */
    private void initializeResources(List<H3Cell> cells) {
        logger.info("Initializing resources...");

        for (H3Cell cell : cells) {
            Biome biome = cell.getBiome();
            if (biome == null)
                biome = Biome.PLAINS;

            // Food resources based on biome
            double wildFood = switch (biome) {
                case JUNGLE -> 800 + random.nextDouble() * 400;
                case FOREST -> 600 + random.nextDouble() * 300;
                case PLAINS -> 400 + random.nextDouble() * 200;
                case HILLS -> 300 + random.nextDouble() * 150;
                case BEACH -> 200 + random.nextDouble() * 100;
                case TUNDRA -> 100 + random.nextDouble() * 50;
                case DESERT, SNOW -> 20 + random.nextDouble() * 30;
                case OCEAN, DEEP_OCEAN -> 0; // Fish handled separately
                case MOUNTAINS -> 50 + random.nextDouble() * 50;
            };
            cell.setFoodResource(wildFood);

            // Wood resources
            double wood = switch (biome) {
                case JUNGLE -> 1000 + random.nextDouble() * 500;
                case FOREST -> 800 + random.nextDouble() * 400;
                case HILLS, PLAINS -> 200 + random.nextDouble() * 100;
                default -> 0;
            };
            cell.setWoodResource(wood);

            // Water resources (also used as proxy for freshwater access)
            double water = switch (biome) {
                case JUNGLE -> 1000;
                case FOREST, PLAINS -> 600 + random.nextDouble() * 200;
                case HILLS, BEACH -> 400;
                case TUNDRA, MOUNTAINS -> 300; // Snow melt
                case DESERT -> 50;
                case OCEAN, DEEP_OCEAN -> 0; // Salt water
                case SNOW -> 200;
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
            case FOREST, PLAINS -> 1.0;
            case HILLS -> 0.9;
            case BEACH -> 0.7;
            case TUNDRA -> 0.4;
            case DESERT, SNOW -> 0.2;
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
            case "ONE_CONTINENT" -> {
                // Cluster population in one area (Africa-like origin)
                // Find a cell near equator in tropical/plains biome
                H3Cell origin = habitable.stream()
                        .filter(c -> Math.abs(c.getLatitude()) < 30)
                        .filter(c -> c.getBiome() == Biome.PLAINS || c.getBiome() == Biome.FOREST)
                        .findFirst()
                        .orElse(habitable.get(random.nextInt(habitable.size())));
                int centerIdx = habitable.indexOf(origin);
                distributeNearCell(habitable, centerIdx, totalPop, 0.15);
            }
            case "RIVER_VALLEYS" -> {
                // Prefer high water access
                distributeByWater(habitable, totalPop);
            }
            case "COASTAL" -> {
                // Prefer beach biome and cells near ocean
                distributeByCoast(habitable, cells, totalPop);
            }
            case "SPARSE" -> {
                // Low-density scatter across all habitable terrain
                distributeByCapacity(habitable, totalPop);
                // Reduce all populations to simulate sparse nomadic life
                for (H3Cell c : habitable) {
                    c.setPopulation((int) (c.getPopulation() * 0.3));
                }
            }
            case "DENSE" -> {
                // High-density in optimal areas only
                List<H3Cell> optimal = habitable.stream()
                        .filter(c -> calculateCarryingCapacity(c) > 0.5)
                        .toList();
                if (optimal.isEmpty())
                    optimal = habitable;
                distributeByCapacity(optimal, totalPop);
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
            int pop = (int) Math.round(totalPop * weights[i] / totalWeight);
            habitable.get(i).setPopulation(pop);
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
            int pop = (int) Math.round(totalPop * weights[i] / totalWeight);
            cells.get(i).setPopulation(pop);
        }
    }

    private void distributeByWater(List<H3Cell> cells, long totalPop) {
        double totalWeight = cells.stream()
                .mapToDouble(c -> c.getWaterResource() != null ? c.getWaterResource() : 0.0)
                .sum();

        if (totalWeight == 0) {
            // Fallback to equal distribution
            int perCell = (int) (totalPop / cells.size());
            cells.forEach(c -> c.setPopulation(perCell));
            return;
        }

        for (H3Cell c : cells) {
            double weight = c.getWaterResource() != null ? c.getWaterResource() : 0.0;
            int pop = (int) Math.round(totalPop * weight / totalWeight);
            c.setPopulation(pop);
        }
    }

    private void distributeByCapacity(List<H3Cell> cells, long totalPop) {
        double totalWeight = cells.stream()
                .mapToDouble(this::calculateCarryingCapacity)
                .sum();

        if (totalWeight == 0) {
            // Fallback to equal distribution
            int perCell = (int) (totalPop / cells.size());
            cells.forEach(c -> c.setPopulation(perCell));
            return;
        }

        for (H3Cell c : cells) {
            double weight = calculateCarryingCapacity(c);
            int pop = (int) Math.round(totalPop * weight / totalWeight);
            c.setPopulation(pop);
        }
    }
}
