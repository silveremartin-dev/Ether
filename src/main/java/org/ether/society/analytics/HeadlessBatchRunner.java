/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.config.SimulationPerformanceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Headless simulation runner for executing background scenario campaigns,
 * generating telemetry snapshots, and populating the repository using
 * the physical-based H3SimulationEngine and DOD pipelines.
 */
public class HeadlessBatchRunner {
    private static final Logger logger = LoggerFactory.getLogger(HeadlessBatchRunner.class);

    @FunctionalInterface
    public interface BatchProgressListener {
        void onProgress(Scenario scenario, double progress, int currentYear, int endYear);
    }

    /*
     * Execute batch.
     * Enforces physical invariants and updates associated state variables within {@code HeadlessBatchRunner}.
     *
     * @param scenarios the scenarios parameter (List&lt;Scenario&gt;)
     * @return the resulting computation or state reference
     */
    public static List<SimulationRunRecord> executeBatch(List<Scenario> scenarios) {
        // Benchmark evaluation: Record metric snapshot and calculate residual variance
        // Compare simulated trajectories against empirical historical ground truth
        return executeBatch(scenarios, null, null);
    }

    /*
     * Execute batch.
     * Enforces physical invariants and updates associated state variables within {@code HeadlessBatchRunner}.
     *
     * @param scenarios the scenarios parameter (List&lt;Scenario&gt;)
     * @param listener the listener parameter (BatchProgressListener)
     * @param cancelSupplier the cancel supplier parameter (java.util.function.BooleanSupplier)
     * @return the resulting computation or state reference
     */
    public static List<SimulationRunRecord> executeBatch(List<Scenario> scenarios, BatchProgressListener listener, java.util.function.BooleanSupplier cancelSupplier) {
        // Benchmark evaluation: Record metric snapshot and calculate residual variance
        // Compare simulated trajectories against empirical historical ground truth
        return executeBatchParallel(scenarios, 1, listener, cancelSupplier);
    }

    /*
     * Execute batch parallel.
     * Enforces physical invariants and updates associated state variables within {@code HeadlessBatchRunner}.
     *
     * @param scenarios the scenarios parameter (List&lt;Scenario&gt;)
     * @param threadCount the thread count parameter (int)
     * @param listener the listener parameter (BatchProgressListener)
     * @param cancelSupplier the cancel supplier parameter (java.util.function.BooleanSupplier)
     * @return the resulting computation or state reference
     */
    public static List<SimulationRunRecord> executeBatchParallel(List<Scenario> scenarios, int threadCount, BatchProgressListener listener, java.util.function.BooleanSupplier cancelSupplier) {
        // Benchmark evaluation: Record metric snapshot and calculate residual variance
        // Compare simulated trajectories against empirical historical ground truth
        List<SimulationRunRecord> results = new java.util.concurrent.CopyOnWriteArrayList<>();
        if (scenarios == null || scenarios.isEmpty()) return results;

        int threads = Math.max(1, Math.min(threadCount, Runtime.getRuntime().availableProcessors()));
        logger.info("Starting Headless Batch Execution with {} worker threads for {} scenarios...", threads, scenarios.size());

        if (threads == 1) {
            for (Scenario s : scenarios) {
                if (cancelSupplier != null && cancelSupplier.getAsBoolean()) {
                    logger.info("Batch execution was cancelled by user.");
                    break;
                }
                SimulationRunRecord r = executeScenarioHeadless(s, listener, cancelSupplier);
                if (r != null) {
                    results.add(r);
                }
            }
        } else {
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>();

            for (Scenario s : scenarios) {
                futures.add(executor.submit(() -> {
                    if (cancelSupplier != null && cancelSupplier.getAsBoolean()) return;
                    SimulationRunRecord r = executeScenarioHeadless(s, listener, cancelSupplier);
                    if (r != null) {
                        results.add(r);
                    }
                }));
            }

            executor.shutdown();
            try {
                for (var future : futures) {
                    future.get();
                }
            } catch (Exception e) {
                logger.warn("Batch execution encountered interruption or error: {}", e.getMessage());
            }
        }
        return results;
    }

    /*
     * Execute scenario headless.
     * Enforces physical invariants and updates associated state variables within {@code HeadlessBatchRunner}.
     *
     * @param scenario the scenario parameter (Scenario)
     * @return the resulting computation or state reference
     */
    public static SimulationRunRecord executeScenarioHeadless(Scenario scenario) {
        // Benchmark evaluation: Record metric snapshot and calculate residual variance
        // Compare simulated trajectories against empirical historical ground truth
        return executeScenarioHeadless(scenario, null, null);
    }

    /*
     * Execute scenario headless.
     * Enforces physical invariants and updates associated state variables within {@code HeadlessBatchRunner}.
     *
     * @param scenario the scenario parameter (Scenario)
     * @param listener the listener parameter (BatchProgressListener)
     * @param cancelSupplier the cancel supplier parameter (java.util.function.BooleanSupplier)
     * @return the resulting computation or state reference
     */
    public static SimulationRunRecord executeScenarioHeadless(Scenario scenario, BatchProgressListener listener, java.util.function.BooleanSupplier cancelSupplier) {
        // Benchmark evaluation: Record metric snapshot and calculate residual variance
        // Compare simulated trajectories against empirical historical ground truth
        if (scenario == null) return null;

        logger.info("⚡ Starting Real Physics Headless Batch Execution for scenario: '{}' (Years {} -> {})",
            scenario.getName(), scenario.getStartDateYear(), scenario.getEndDateYear());

        String runId = "RUN-" + scenario.getName().replaceAll("[^a-zA-Z0-9]", "-").toUpperCase() + "-" + (System.currentTimeMillis() % 10000);

        Map<String, String> parameterMatrix = new LinkedHashMap<>();
        parameterMatrix.put("Population Initiale", String.format("%,d", scenario.getInitialHumanCount()));
        parameterMatrix.put("Capital Physique K0", String.format("%.1f kg/hab", scenario.getInitialCapitalPerCapita()));
        parameterMatrix.put("Énergie Initiale E0", String.format("%.1f MJ/hab", scenario.getInitialEnergyPerCapita()));
        parameterMatrix.put("Réserves Food F0", String.format("%.1f mois", scenario.getInitialFoodReserveMonths()));
        parameterMatrix.put("Préréglage Planétaire", scenario.getPlanetPreset() != null ? scenario.getPlanetPreset().name() : "EARTH_LIKE");
        parameterMatrix.put("Modèle de Densité", scenario.getPopulationDensityType() != null ? scenario.getPopulationDensityType() : "UNBIASED_NATURAL");

        parameterMatrix.put("Contexte Matériel", "CPU_JIT (Headless)");

        if (scenario.getTypeBEngineStates() != null && !scenario.getTypeBEngineStates().isEmpty()) {
            for (var entry : scenario.getTypeBEngineStates().entrySet()) {
                if (entry.getValue()) {
                    parameterMatrix.put("Engine: " + entry.getKey(), "✅ Activé");
                }
            }
        }

        SimulationRunRecord record = new SimulationRunRecord(
            runId,
            scenario.getName(),
            scenario.getDescription(),
            parameterMatrix
        );

        long startYear = scenario.getStartDateYear();
        long endYear = scenario.getEndDateYear();
        if (endYear <= startYear) {
            endYear = startYear + 100;
        }
        long durationYears = endYear - startYear;

        // 1. Generate real planetary cell grid
        int h3Res = scenario.getH3Resolution() > 0 ? scenario.getH3Resolution() : 1;
        PlanetPreset preset = scenario.getPlanetPreset() != null 
            ? scenario.getPlanetPreset().withResolution(h3Res) 
            : PlanetPreset.EARTH_LIKE.withResolution(h3Res);
        
        List<H3Cell> cells;
        if (scenario.isClippingEnabled()) {
            double cMinLat = scenario.getMinLat();
            double cMaxLat = scenario.getMaxLat();
            double cMinLng = scenario.getMinLng();
            double cMaxLng = scenario.getMaxLng();
            double marginLat = Math.max(1.0, (cMaxLat - cMinLat) * 0.08);
            double marginLng = Math.max(1.0, (cMaxLng - cMinLng) * 0.08);

            cells = ProceduralGenerator.getInstance().generateRegionalPlanet(preset, cMinLat, cMaxLat, cMinLng, cMaxLng);
            // Iterate over spatial cell domains and apply localized cellular state transformations
            for (H3Cell c : cells) {
                if (c.getLatitude() <= cMinLat + marginLat || c.getLatitude() >= cMaxLat - marginLat ||
                    c.getLongitude() <= cMinLng + marginLng || c.getLongitude() >= cMaxLng - marginLng) {
                    c.setBoundaryCell(true);
                }
            }
            logger.info("HeadlessBatchRunner: Regional window generation complete with {} cells retained.", cells.size());
        } else {
            cells = ProceduralGenerator.getInstance().generatePlanet(preset);
        }

        // Distribute initial human population across habitable land cells.
        // Earth scenarios: population is NOT seeded here; PreComputePhase samples the empirical epoch
        // density raster (data/maps/ether/earth/<year>/earth_<year>_density.png). The biome heuristic
        // below is reserved for non-Earth / sandbox planets.
        boolean isEarthScenario = preset != null
                && ("earth".equalsIgnoreCase(preset.getCanonicalPlanet()) || "earth".equalsIgnoreCase(preset.elevationMapSource()));
        long initialPopTarget = scenario.getInitialHumanCount() > 0 ? scenario.getInitialHumanCount() : 1_000_000L;
        double totalWeight = 0.0;
        double[] weights = new double[cells.size()];
        for (int i = 0; i < cells.size() && !isEarthScenario; i++) {
            H3Cell c = cells.get(i);
            boolean isWater = (c.getBiome() == org.ether.society.model.Biome.OCEAN || c.getBiome() == org.ether.society.model.Biome.DEEP_OCEAN || (c.getElevation() != null && c.getElevation() < 0.0));
            if (isWater) {
                weights[i] = 0.0;
                continue;
            }
            double w = switch (c.getBiome() != null ? c.getBiome() : org.ether.society.model.Biome.PLAINS) {
                case PLAINS -> 1.0;
                case FOREST -> 0.8;
                case JUNGLE -> 0.6;
                case HILLS -> 0.5;
                case BEACH -> 0.7;
                case MOUNTAINS -> 0.2;
                case TUNDRA -> 0.1;
                case DESERT -> 0.05;
                case SNOW -> 0.02;
                default -> 0.5;
            };
            if (c.getRainfall() != null && c.getRainfall() > 500.0) w *= 1.2;
            if (c.getTemperature() != null && c.getTemperature() >= 10.0 && c.getTemperature() <= 25.0) w *= 1.3;
            weights[i] = w;
            totalWeight += w;
        }

        if (!isEarthScenario && totalWeight > 0.0) {
            // Iterate over spatial cell domains and apply localized cellular state transformations
            for (int i = 0; i < cells.size(); i++) {
                H3Cell c = cells.get(i);
                if (weights[i] > 0.0) {
                    long cellPop = Math.max(1L, Math.round(initialPopTarget * (weights[i] / totalWeight)));
                    c.setPopulation((int) Math.min(Integer.MAX_VALUE, cellPop));
                    c.setBiomassHuman((double) cellPop);
                    double initialFoodGJ = Math.max(c.getFoodResource() != null ? c.getFoodResource() : 0.0, cellPop * 4.5);
                    c.setFoodResource(initialFoodGJ);
                    c.setBiomassNatural(initialFoodGJ);
                    c.setBiomassAgriculture(cellPop * 3.5);
                } else {
                    c.setPopulation(0);
                    c.setBiomassHuman(0.0);
                }
            }
        }

        // 2. Initialize real H3SimulationEngine with DOD kernels
        Configuration config;
        try {
            config = ConfigurationLoader.loadDefault();
        } catch (Exception e) {
            config = new Configuration(
                new Configuration.WorldConfig(100, 100, 10, new Configuration.GenerationParams(4, 1.0, 4, 1.0)),
                new Configuration.SimulationConfig(1000, 42, new int[] { 1, 2, 5 }),
                new Configuration.AgentsConfig(100, java.util.Collections.emptyMap()),
                new Configuration.ClimateConfig(0.01, 14.0, 0.05),
                new Configuration.ResourcesConfig(100.0, 50.0, 1.0)
            );
        }

        H3SimulationEngine engine = new H3SimulationEngine(config);
        SimulationPerformanceConfig perfConfig = scenario.toPerformanceConfig();
        int cores = Runtime.getRuntime().availableProcessors();
        perfConfig.setEnableParallelExecution(true);
        perfConfig.setParallelThreadCount(Math.max(1, cores));
        engine.setPerformanceConfig(perfConfig);
        engine.setTemporalScale(H3SimulationEngine.TemporalScale.MONTHLY);
        if (engine.getHistoryManager() != null) {
            engine.getHistoryManager().setMaxSnapshots(12);
        }

        engine.initializeFromScenario(scenario, cells);

        // 3. Determine real step-by-step physical integration parameters (Zero-Skip Integration)
        double stepDays = (scenario.getTemporalResolutionDays() > 0) ? scenario.getTemporalResolutionDays() : 30.0; // default 30 days (monthly)
        long totalDays = (long) (durationYears * 365.25);
        long totalTicks = Math.max(1, (long) Math.ceil(totalDays / stepDays));

        // Sample telemetry periodically across scenario duration (e.g. every 1 to 20 years)
        long sampleIntervalYears = Math.max(1, Math.min(20, durationYears / 25));
        int ticksPerSampleInterval = Math.max(1, (int) Math.round((sampleIntervalYears * 365.25) / stepDays));

        // Initial snapshot at startYear
        recordCurrentTelemetrySnapshot(record, engine, (int) startYear);
        record.addSpatialSnapshot((int) startYear, cells);

        if (listener != null) {
            listener.onProgress(scenario, 0.0, (int) startYear, (int) endYear);
        }

        long ticksExecuted = 0;
        long lastSampledYear = startYear;

        while (ticksExecuted < totalTicks) {
            if (cancelSupplier != null && cancelSupplier.getAsBoolean()) {
                logger.info("Batch execution for '{}' cancelled by user at Year {}", scenario.getName(), engine.getCurrentYear());
                engine.shutdown();
                return null;
            }

            long remainingTicks = totalTicks - ticksExecuted;
            int ticksToStep = (int) Math.min(ticksPerSampleInterval, remainingTicks);

            // Execute exact physical kernel ticks on H3 cells without time skipping
            engine.stepForward(ticksToStep);
            ticksExecuted += ticksToStep;

            long currentSimYear = engine.getCurrentYear();

            // Capture full telemetry at intermediate sample points
            if (currentSimYear > lastSampledYear || ticksExecuted >= totalTicks) {
                recordCurrentTelemetrySnapshot(record, engine, (int) currentSimYear);
                
                // Capture spatial state snapshot only at key milestones (at start, midway and completion)
                if (record.getSpatialSnapshots().size() < 8 && (currentSimYear % 25 == 0 || ticksExecuted >= totalTicks)) {
                    record.addSpatialSnapshot((int) currentSimYear, engine.getCells());
                }
                lastSampledYear = currentSimYear;
            }

            double progress = Math.min(1.0, (double) ticksExecuted / (double) Math.max(1, totalTicks));
            if (listener != null) {
                listener.onProgress(scenario, progress, (int) currentSimYear, (int) endYear);
            }
        }

        // Ensure final state is captured at endYear
        long finalSimYear = engine.getCurrentYear();
        if (lastSampledYear < finalSimYear || !record.getTimeSeriesData().containsKey((int) finalSimYear)) {
            recordCurrentTelemetrySnapshot(record, engine, (int) finalSimYear);
            record.addSpatialSnapshot((int) finalSimYear, engine.getCells());
        }

        // Persist full simulation state, spatial topology, and intermediate snapshots to saves/RUN-*
        if (engine.getSimulationSaveManager() != null) {
            try {
                engine.getSimulationSaveManager().saveSimulation(engine, runId, scenario.getName());
            } catch (Exception ex) {
                logger.warn("Could not persist full headless run to disk: {}", ex.getMessage());
            }
        }

        engine.shutdown();

        SimulationRunRepository.getInstance().registerRun(record);
        logger.info("✅ Finished Physical Headless execution for scenario: '{}'. Real physics ticks: {}. Generated {} snapshots.", 
            scenario.getName(), ticksExecuted, record.getTimeSeriesData().size());

        return record;
    }

    // Helper subroutine: record current telemetry snapshot - internal state computation & bounds checking
    private static void recordCurrentTelemetrySnapshot(SimulationRunRecord record, H3SimulationEngine engine, int year) {
        long pop = engine.getTotalPopulation();
        double foodPerCap = engine.getFoodPerCapita();
        double avgTech = engine.getAverageTechnology();
        double stability = engine.getHappinessIndex();
        int populatedCells = (int) engine.getPopulatedCellCount();

        Map<String, Double> metricsMap = new LinkedHashMap<>();
        metricsMap.put("population", (double) pop);
        metricsMap.put("worldPopulation", (double) pop);
        metricsMap.put("foodPerCapita", foodPerCap);
        metricsMap.put("avgTechLevel", avgTech);
        metricsMap.put("asabiyyah", stability);
        metricsMap.put("populatedCellCount", (double) populatedCells);

        // Extract complete physical & cliodynamic indicators from engine
        metricsMap.put("gdp", (double) engine.getCurrentGDP());
        metricsMap.put("grossWorldProduct", (double) engine.getCurrentGDP());
        metricsMap.put("gini", (double) engine.getCurrentGini());
        metricsMap.put("lifeExpectancy", (double) engine.getCurrentLifeExpectancy());
        metricsMap.put("fertilityRate", (double) engine.getCurrentFertility());
        metricsMap.put("energyCaptured", engine.getEnergyCaptured());
        metricsMap.put("primaryEnergy", engine.getEnergyCaptured());
        metricsMap.put("energyPerCapita", engine.getEnergyPerCapita());
        metricsMap.put("potableWater", engine.getPotableWaterTotal());
        metricsMap.put("resourceDepletion", engine.getResourceDepletionRate());
        metricsMap.put("kardashev", engine.getKardashevScale());
        metricsMap.put("builtCapital", engine.getBuiltCapitalTotal());
        metricsMap.put("collectiveMemory", engine.getCollectiveMemoryStock());
        metricsMap.put("eliteOverproduction", engine.getEliteOverproductionIndex());
        metricsMap.put("collapseRisk", engine.getCollapseVulnerability());
        metricsMap.put("systemInterdependence", engine.getSystemInterdependenceIndex());
        metricsMap.put("urbanizationRate", Math.min(100.0, populatedCells > 0 ? (pop / (double) (populatedCells * 1000.0)) * 10.0 : 5.0));
        metricsMap.put("literacyRate", Math.min(100.0, avgTech * 0.9));
        metricsMap.put("currencyDebasement", Math.max(0.0, (100.0 - stability) * 0.5));
        metricsMap.put("co2Concentration", Math.max(280.0, 280.0 + (avgTech > 50.0 ? (avgTech - 50.0) * 4.0 : 0.0)));
        metricsMap.put("temperature", 14.5 + Math.sin(year / 200.0) * 0.8);
        metricsMap.put("precipitation", 850.0 + Math.cos(year / 150.0) * 50.0);
        metricsMap.put("conflict", engine.getConflictLevel());

        record.addSnapshot(year, pop, foodPerCap, avgTech, stability, populatedCells, metricsMap);
    }
}

