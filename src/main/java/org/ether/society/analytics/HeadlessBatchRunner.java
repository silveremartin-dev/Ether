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
import org.ether.society.procedural.PlanetPreset;
import org.ether.society.procedural.ProceduralGenerator;
import org.ether.society.procedural.SimulationPerformanceConfig;
import org.ether.society.ui.ExecutionContextPanel;
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

    public static List<SimulationRunRecord> executeBatch(List<Scenario> scenarios) {
        return executeBatch(scenarios, null, null);
    }

    public static List<SimulationRunRecord> executeBatch(List<Scenario> scenarios, BatchProgressListener listener, java.util.function.BooleanSupplier cancelSupplier) {
        return executeBatchParallel(scenarios, 1, listener, cancelSupplier);
    }

    public static List<SimulationRunRecord> executeBatchParallel(List<Scenario> scenarios, int threadCount, BatchProgressListener listener, java.util.function.BooleanSupplier cancelSupplier) {
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

    public static SimulationRunRecord executeScenarioHeadless(Scenario scenario) {
        return executeScenarioHeadless(scenario, null, null);
    }

    public static SimulationRunRecord executeScenarioHeadless(Scenario scenario, BatchProgressListener listener, java.util.function.BooleanSupplier cancelSupplier) {
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

        ExecutionContextPanel.HardwareMode activeHw = org.ether.society.ui.ExecutionContextPanel.getActiveHardwareMode();
        parameterMatrix.put("Contexte Matériel", activeHw != null ? activeHw.name() + " (Headless)" : "CPU_JIT (Headless)");

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
        PlanetPreset preset = scenario.getPlanetPreset() != null 
            ? scenario.getPlanetPreset().withResolution(1) 
            : PlanetPreset.EARTH_LIKE.withResolution(1);
        
        List<H3Cell> cells = ProceduralGenerator.getInstance().generatePlanet(preset);

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
        perfConfig.setEnableParallelExecution(activeHw != org.ether.society.ui.ExecutionContextPanel.HardwareMode.GPU_OFF);
        perfConfig.setParallelThreadCount(switch (activeHw) {
            case NATIVE_RUST, JAVA_VECTOR_SIMD -> Math.max(1, cores);
            case GPU_SHADERS -> 2;
            case CPU_JIT -> Math.max(1, cores);
            case GPU_OFF -> 1;
            default -> Math.max(1, cores);
        });
        engine.setPerformanceConfig(perfConfig);
        engine.setTemporalScale(H3SimulationEngine.TemporalScale.MONTHLY);

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
                
                // Capture spatial state snapshot at intermediate milestones
                record.addSpatialSnapshot((int) currentSimYear, engine.getCells());
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

        engine.shutdown();

        SimulationRunRepository.getInstance().registerRun(record);
        logger.info("✅ Finished Physical Headless execution for scenario: '{}'. Real physics ticks: {}. Generated {} snapshots.", 
            scenario.getName(), ticksExecuted, record.getTimeSeriesData().size());

        return record;
    }

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
