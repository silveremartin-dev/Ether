/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.database.H3Cell;
import org.ether.society.model.*;
import org.ether.society.procedural.FutureScenarioRegistry;
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
                : 12345L);
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
        try {
            FutureScenarioRegistry.PhysicalScenarioPreset futurePreset =
                    FutureScenarioRegistry.findPresetForScenario(scenario);
            if (futurePreset != null) {
                FutureScenarioRegistry.applyScenarioForcing(futurePreset, cells);
            }
        } catch (NoClassDefFoundError | Exception e) {
            logger.warn("Could not apply future scenario forcing for scenario {}: {}", scenario.getName(), e.getMessage());
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
     * Preserves climate if already initialized via Tab 1 / raster tensors.
     */
    private void computeClimate(List<H3Cell> cells) {
        boolean hasExistingClimate = cells.stream().anyMatch(c -> c.getTemperature() != null && c.getRainfall() != null);
        if (hasExistingClimate) {
            logger.info("Preserving existing climate & temperature data from configuration/raster maps");
            return;
        }

        logger.info("Computing climate and glaciological forcings procedurally...");
        double axialTilt = scenario.getAxialTiltDegrees();
        long year = scenario.getStartDateYear();

        boolean isGlacialMax = year <= -12000;
        boolean isYoungerDryas = (year >= -10900 && year <= -9700) || "YOUNGER_DRYAS".equals(scenario.getPopulationDensityType());
        boolean isGreenSahara = (year >= -8000 && year <= -5000) || "GREEN_SAHARA".equals(scenario.getPopulationDensityType());

        for (H3Cell cell : cells) {
            double lat = cell.getLatitude();
            double lng = cell.getLongitude();
            double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;

            // Base temperature from latitude
            double latFactor = Math.abs(lat) / 90.0;
            double baseTemp = 30.0 - (latFactor * 50.0 * (axialTilt / 23.5));

            // Glacial Maximum LGM cooling (-6°C global) & Sea Level Drop (-120m)
            if (isGlacialMax) {
                baseTemp -= 6.0;
                cell.setSeaLevelOffsetMeters(-120.0);

                // Laurentide & Fennoscandian Ice Sheets (up to 2,000 meters thickness on land)
                boolean isLand = elev > 0 && cell.getBiome() != Biome.OCEAN && cell.getBiome() != Biome.DEEP_OCEAN;
                if (isLand) {
                    boolean isLaurentide = (lat > 54.0 && lng >= -160.0 && lng <= -55.0);
                    boolean isFennoscandian = (lat > 58.0 && lng >= 5.0 && lng <= 50.0);
                    boolean isGreenlandIceland = (lat > 64.0 && lng >= -55.0 && lng < 5.0);
                    if (isLaurentide || isFennoscandian || isGreenlandIceland) {
                        double baseLat = isLaurentide ? 54.0 : (isFennoscandian ? 58.0 : 64.0);
                        double iceMeters = Math.min(2000.0, (lat - baseLat) * 120.0);
                        cell.setIceSheetThicknessMeters(iceMeters);
                        cell.setDynamicAlbedo(0.80);
                        cell.setBiome(Biome.GLACIER);
                        baseTemp -= (iceMeters * 0.005);
                    }
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

            double finalTemp = baseTemp + elevEffect + tempVariation;
            cell.setTemperature(finalTemp);

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
                if (Math.abs(lat) > 72.0 || finalTemp < -2.0) {
                    cell.setBiome(Biome.GLACIER); // Polar Arctic sea ice & ice cap
                }
            } else {
                if (Math.abs(lat) > 75.0 || finalTemp < -15.0) {
                    cell.setBiome(Biome.GLACIER); // Continental ice sheet / Glacier
                }
            }
            cell.setRainfall(Math.max(0, basePrecip + random.nextDouble() * 500));
        }

        double seaOffset = isGlacialMax ? -120.0 : 0.0;
        org.ether.society.procedural.SeaLevelTransitionEngine.applySeaLevelTransition(cells, seaOffset);
    }

    /**
     * Initialize resource stockpiles based on biome type and coastal marine abundance.
     * Preserves existing resources if already populated by Tab 2.
     */
    private void initializeResources(List<H3Cell> cells) {
        boolean hasExistingResources = cells.stream().anyMatch(c -> (c.getFoodResource() != null && c.getFoodResource() > 0)
                || (c.getWaterResource() != null && c.getWaterResource() > 0));

        if (hasExistingResources) {
            logger.info("Preserving existing resource distributions (Food, Water, Wood, Minerals) from configuration/raster maps");
            for (H3Cell cell : cells) {
                if (cell.getIsCoastal() == null) {
                    Biome biome = cell.getBiome() != null ? cell.getBiome() : Biome.PLAINS;
                    boolean coastal = biome == Biome.BEACH || (cell.getElevation() != null && cell.getElevation() > 0 && cell.getElevation() < 150.0);
                    cell.setIsCoastal(coastal);
                }
                if (cell.getBiomassFish() == null || cell.getBiomassFish() == 0.0) {
                    if (Boolean.TRUE.equals(cell.getIsCoastal())) {
                        cell.setBiomassFish(1200.0);
                    } else if (cell.getBiome() == Biome.LAKE) {
                        cell.setBiomassFish(800.0);
                    }
                }
                if (cell.getBiomassNatural() == null && cell.getFoodResource() != null) {
                    cell.setBiomassNatural(cell.getFoodResource());
                }
                if (cell.getBiomassLivestock() == null) {
                    Biome b = cell.getBiome();
                    cell.setBiomassLivestock(b == Biome.SAVANNAH || b == Biome.PLAINS ? 400.0 : (b == Biome.TUNDRA ? 200.0 : 0.0));
                }
                if (cell.getBiomassAgriculture() == null) {
                    cell.setBiomassAgriculture(0.0);
                }
            }
            return;
        }

        logger.info("Initializing fallback resources & coastal marine biomes...");

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
            } else if (biome == Biome.LAKE) {
                cell.setBiomassFish(800.0);
            } else {
                cell.setBiomassFish(0.0);
            }

            double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;

            // Food resources based on biome (stored in Gigajoules GJ)
            double wildFood = switch (biome) {
                case JUNGLE -> 2500.0 + random.nextDouble() * 1000.0;
                case FOREST -> 2000.0 + random.nextDouble() * 800.0;
                case SAVANNAH -> 2200.0 + random.nextDouble() * 800.0;
                case PLAINS -> 1700.0 + random.nextDouble() * 600.0;
                case HILLS -> 1000.0 + random.nextDouble() * 400.0;
                case BEACH, LAKE -> 800.0 + random.nextDouble() * 400.0;
                case TUNDRA -> (elev < 1500.0 ? 450.0 : 150.0) + random.nextDouble() * 150.0;
                case DESERT -> 40.0 + random.nextDouble() * 40.0;
                case SNOW -> elev > 4000.0 ? 0.0 : 5.0;
                case GLACIER -> 0.0;
                case OCEAN, DEEP_OCEAN -> 0.0;
                case MOUNTAINS -> elev > 4500.0 ? 0.0 : (elev > 3000.0 ? 25.0 : 250.0);
            };
            cell.setFoodResource(wildFood);
            cell.setBiomassNatural(wildFood);
            cell.setBiomassLivestock(biome == Biome.SAVANNAH || biome == Biome.PLAINS ? 400.0 : (biome == Biome.TUNDRA ? 200.0 : 0.0));
            cell.setBiomassAgriculture(0.0);

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
                case JUNGLE, LAKE -> 1000.0;
                case SAVANNAH -> 800.0;
                case FOREST, PLAINS -> 600.0 + random.nextDouble() * 200.0;
                case HILLS, BEACH -> 400.0;
                case TUNDRA -> 300.0;
                case MOUNTAINS -> elev > 5000.0 ? 0.0 : 300.0;
                case DESERT -> 50.0;
                case OCEAN, DEEP_OCEAN -> 0.0;
                case SNOW -> 100.0;
                case GLACIER -> 0.0;
            };
            cell.setWaterResource(water);
        }
    }

    /**
     * Distribute initial human population based on scenario settings.
     * Preserves already configured populations (from UI or custom density maps),
     * or delegates to ProceduralPopulationEngine.
     */
    private void distributeInitialPopulation(List<H3Cell> cells) {
        // 1. If cells already have population (from Tab 3 UI setup or loaded scenario), preserve it!
        boolean hasExistingPop = cells.stream().anyMatch(c -> c.getPopulation() != null && c.getPopulation() > 0);
        if (hasExistingPop) {
            long totalAssigned = cells.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
            long populatedCells = cells.stream().filter(c -> c.getPopulation() != null && c.getPopulation() > 0).count();
            logger.info("Preserving existing population distribution: {} humans across {} cells", totalAssigned, populatedCells);
            return;
        }

        // 2. Otherwise distribute via standard ProceduralPopulationEngine
        logger.info("Distributing initial population via ProceduralPopulationEngine: {} humans", scenario.getInitialHumanCount());
        long totalPop = scenario.getInitialHumanCount();
        double capitalK0 = scenario.getInitialCapitalPerCapita();
        double techLevel = Math.clamp(Math.log10(Math.max(1.0, capitalK0)) * 2.2 + 0.2, 0.2, 10.0);
        String pattern = scenario.getPopulationDensityType() != null ? scenario.getPopulationDensityType() : "UNBIASED_NATURAL";
        boolean isEarthPreset = scenario.getPlanetPreset() != null
                && ((scenario.getPlanetPreset().elevationUseImport() && "earth".equalsIgnoreCase(scenario.getPlanetPreset().elevationMapSource()))
                || (scenario.getPlanetPreset().name() != null && (scenario.getPlanetPreset().name().toLowerCase().contains("terre") || scenario.getPlanetPreset().name().toLowerCase().contains("earth"))));
        long startYear = scenario.getStartDateYear();

        org.ether.society.procedural.ProceduralPopulationEngine.distributePopulation(cells, scenario, totalPop, techLevel, pattern, isEarthPreset, startYear);
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
}
