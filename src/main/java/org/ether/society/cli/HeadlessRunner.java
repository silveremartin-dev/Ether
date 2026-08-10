/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.cli;

import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.profiling.SimulationProfiler;
import org.ether.society.data.SampleDataGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Headless Execution Engine Runner.
 * Enables pure headless simulation execution without JavaFX GUI dependencies.
 * Essential for distributed compute nodes, server environments, and CLI benchmarks.
 */
public class HeadlessRunner {
    private static final Logger logger = LoggerFactory.getLogger(HeadlessRunner.class);

    public static void main(String[] args) {
        run(args);
    }

    public static void run(String[] args) {
        System.out.println("==========================================================");
        System.out.println("          ETHER SIMULATION ENGINE — HEADLESS MODE          ");
        System.out.println("==========================================================");

        int ticksToRun = 300; // 10 months default
        int cellCount = 3000;
        boolean showProfile = true;
        boolean isClusterMode = false;
        org.ether.society.network.ClusterManager.ClusterRole clusterRole = org.ether.society.network.ClusterManager.ClusterRole.MASTER;
        String masterHost = "127.0.0.1";
        int port = 9090;
        String secretToken = "EtherClusterSecret2026";

        String scenarioName = "OUT_OF_AFRICA";
        boolean ticksExplicitlySet = false;

        // Parse CLI parameters
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (("--ticks".equalsIgnoreCase(arg) || "-t".equalsIgnoreCase(arg)) && i + 1 < args.length) {
                ticksToRun = Integer.parseInt(args[++i]);
                ticksExplicitlySet = true;
            } else if (("--cells".equalsIgnoreCase(arg) || "-c".equalsIgnoreCase(arg)) && i + 1 < args.length) {
                cellCount = Integer.parseInt(args[++i]);
            } else if ("--profile".equalsIgnoreCase(arg) || "-p".equalsIgnoreCase(arg)) {
                showProfile = true;
            } else if (arg.startsWith("--scenario=")) {
                scenarioName = arg.substring("--scenario=".length());
            } else if (("-s".equalsIgnoreCase(arg) || "--scenario".equalsIgnoreCase(arg)) && i + 1 < args.length) {
                scenarioName = args[++i];
            } else if ("--mode=cluster".equalsIgnoreCase(arg) || "--cluster".equalsIgnoreCase(arg)) {
                isClusterMode = true;
            } else if ("--role=worker".equalsIgnoreCase(arg) || "--worker".equalsIgnoreCase(arg)) {
                clusterRole = org.ether.society.network.ClusterManager.ClusterRole.WORKER;
            } else if (arg.startsWith("--master-host=")) {
                masterHost = arg.substring("--master-host=".length());
            } else if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring("--port=".length()));
            } else if (arg.startsWith("--secret=")) {
                secretToken = arg.substring("--secret=".length());
            }
        }

        try {
            org.ether.society.network.ClusterManager clusterManager = null;
            if (isClusterMode) {
                clusterManager = new org.ether.society.network.ClusterManager(clusterRole, masterHost, port, secretToken);
                clusterManager.start();
                logger.info("Headless Cluster Manager active.");
            }

            Configuration config = ConfigurationLoader.loadDefault();
            H3SimulationEngine engine = new H3SimulationEngine(config);

            // Configure requested scenario
            Scenario scenario = new Scenario();
            scenario.setName("Headless: " + scenarioName);
            scenario.setPopulationDensityType(scenarioName);

            // Match StartDatePreset by enum key or full descriptive display name
            boolean matched = false;
            for (org.ether.society.model.StartDatePreset preset : org.ether.society.model.StartDatePreset.values()) {
                if (preset.name().equalsIgnoreCase(scenarioName)
                        || preset.getDisplayName().equalsIgnoreCase(scenarioName)
                        || scenarioName.toLowerCase().contains(preset.getDisplayName().toLowerCase())) {
                    scenario.setStartDateYear(preset.getYear());
                    scenario.setInitialHumanCount(preset.getEstimatedPopulation());
                    scenario.setInitialTechLevel(preset.getEstimatedTechLevel());
                    scenario.setName(preset.getDisplayName());
                    logger.info("Matched Historical Scenario Preset: '{}' (Start Year: {}, Initial Pop: {}, Tech Level: {})",
                            preset.getDisplayName(), preset.getYear(), preset.getEstimatedPopulation(), preset.getEstimatedTechLevel());
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                logger.info("Custom Scenario Name: '{}' (Using default start parameters)", scenarioName);
            }

            // If ticks were not explicitly specified on CLI, calculate ticks from Scenario duration
            if (!ticksExplicitlySet) {
                ticksToRun = scenario.calculateScenarioTicks();
                logger.info("Computed Scenario Duration: {} ticks (from Year {} to Year {})",
                        ticksToRun, scenario.getStartDateYear(), scenario.getEndDateYear());
            }

            logger.info("Initializing Headless Ether Engine (Scenario={}, ClusterMode={}, Role={}, Cells={}, Ticks={})...",
                    scenario.getName(), isClusterMode, clusterRole, cellCount, ticksToRun);

            List<H3Cell> cells = SampleDataGenerator.generateEuropeSample();

            if (clusterManager != null) {
                clusterManager.setTotalGridCellCount(cells.size());
                clusterManager.dispatchScenarioToCluster(scenario);
            }

            System.out.printf("Initializing grid with %d cells...\n", cells.size());
            engine.initializeFromScenario(scenario, cells);

            SimulationProfiler profiler = engine.getProfiler();
            profiler.reset();

            System.out.printf("Starting Headless Execution for %d Ticks...\n", ticksToRun);
            long startNanos = System.nanoTime();

            // Execute ticks directly in main thread for maximum throughput
            engine.stepForward(ticksToRun);

            long totalNanos = System.nanoTime() - startNanos;
            double totalSec = totalNanos / 1_000_000_000.0;
            double actualTPS = ticksToRun / totalSec;

            System.out.println("\n----------------------------------------------------------");
            System.out.printf("Headless Execution Completed in %.3f seconds!\n", totalSec);
            System.out.printf("Throughput: %.2f TPS (Ticks Per Second)\n", actualTPS);
            System.out.println("----------------------------------------------------------\n");

            if (showProfile) {
                System.out.println(profiler.generateReport());
            }

            if (clusterManager != null) {
                clusterManager.stop();
            }

            engine.shutdown();
            System.out.println("Headless Engine Shutdown Complete.");
        } catch (Exception e) {
            logger.error("Fatal error during Headless simulation run", e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
