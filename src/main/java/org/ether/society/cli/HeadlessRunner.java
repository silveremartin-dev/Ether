/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.cli;

import org.ether.society.benchmark.EmpiricalEngineBenchmarkRunner;
import org.ether.society.benchmark.RealPlanetaryEngineBenchmarkRunner;
import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.SimulationPipeline;
import org.ether.society.core.profiling.SimulationProfiler;
import org.ether.society.data.SampleDataGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.network.ClusterManager;
import org.ether.society.network.cluster.ClusterSnapshotManager;
import org.ether.society.network.cluster.WorkerGPUOffloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Headless &amp; CLI Execution Engine Runner for Master Servers and Worker Nodes.
 * Delegates CLI option parsing to {@link HeadlessCliOptions} and scenario matching
 * to {@link HeadlessScenarioResolver}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class HeadlessRunner {

    private static final Logger logger = LoggerFactory.getLogger(HeadlessRunner.class);
    private static final Preferences execPrefs = Preferences.userRoot().node("org/ether/society/ui");
    private static final Preferences prefPrefs = Preferences.userRoot().node("org/ether/society/ui");
    private static final Preferences i18nPrefs = Preferences.userNodeForPackage(I18n.class);
    private static final Preferences themePrefs = Preferences.userRoot().node("org/ether/society/ui");

    public enum CliEngineMode {
        RUST_NATIVE,
        JAVA_VECTOR_SIMD,
        CPU_JIT,
        SAFE_FALLBACK
    }

    /**
     * CLI entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        run(args);
    }

    /**
     * Executes the headless simulation or cluster worker lifecycle from CLI arguments.
     *
     * @param args command-line arguments
     */
    public static void run(String[] args) {
        System.out.println("================================================================================");
        System.out.println("           ETHER PLANETARY SIMULATION ENGINE — HIGH-PERFORMANCE CLI             ");
        System.out.println("================================================================================");

        HeadlessCliOptions opts = HeadlessCliOptions.parse(args, i18nPrefs, themePrefs);
        switch (opts.earlyAction) {
            case HELP -> {
                HeadlessCliOptions.printHelp();
                return;
            }
            case BENCHMARK_REAL -> {
                RealPlanetaryEngineBenchmarkRunner.runBenchmarks();
                return;
            }
            case BENCHMARK_DOD -> {
                EmpiricalEngineBenchmarkRunner.runBenchmarks();
                return;
            }
            default -> {
            }
        }

        configureRuntimePreferences(opts);

        try {
            ClusterManager clusterManager = startClusterIfEnabled(opts);
            if (opts.isClusterMode && opts.clusterRole == ClusterManager.ClusterRole.WORKER) {
                runWorkerNodeBlocking(clusterManager);
                return;
            }

            SimulationPipeline.resetGlobalSimulationState();
            Configuration config = ConfigurationLoader.loadDefault();
            H3SimulationEngine engine = new H3SimulationEngine(config);

            Scenario scenario = HeadlessScenarioResolver.resolveAndConfigure(opts);
            SimulationPerformanceConfig effectivePerfConfig = scenario.toPerformanceConfig();
            effectivePerfConfig.setParallelThreadCount(opts.threadCount);

            int ticksToRun = opts.ticksExplicitlySet ? opts.ticksToRun : scenario.calculateScenarioTicks();
            if (!opts.ticksExplicitlySet) {
                logger.info("Computed Scenario Duration: {} ticks (from Year {} to Year {})",
                        ticksToRun, scenario.getStartDateYear(), scenario.getEndDateYear());
            }

            int effectiveRes = resolveEffectiveResolution(opts, scenario);
            scenario.setH3Resolution(effectiveRes);
            printConfigurationSummary(opts, effectivePerfConfig, scenario, effectiveRes);

            List<H3Cell> cells = buildGridCells(scenario, effectiveRes);
            if (clusterManager != null) {
                clusterManager.setTotalGridCellCount(cells.size());
                clusterManager.dispatchScenarioToCluster(scenario);
                engine.setClusterManager(clusterManager);
            }

            System.out.printf("Initializing grid with %,d cells (H3 Res: %d)...%n", cells.size(), effectiveRes);
            engine.initializeFromScenario(scenario, cells);
            engine.setPerformanceConfig(effectivePerfConfig);
            if (engine.getHistoryManager() != null) {
                engine.getHistoryManager().configureTargetCadence(ticksToRun, opts.maxSnapshots);
            }
            double dtDays = scenario.getTemporalResolutionDays() > 0 ? scenario.getTemporalResolutionDays() : 30.0;
            long durYears = Math.max(1, scenario.getEndDateYear() - scenario.getStartDateYear());
            long sampIntervalYears = Math.max(1, Math.min(20, durYears / 25));
            int ticksPerSamp = Math.max(1, (int) Math.round((sampIntervalYears * 365.25) / dtDays));
            engine.setTelemetrySubscription(org.ether.society.core.telemetry.TelemetrySubscription.allCanonicalBenchmarks(ticksPerSamp));

            SimulationProfiler profiler = engine.getProfiler();
            profiler.reset();

            System.out.printf("Starting Headless Execution for %,d Ticks...%n", ticksToRun);
            long startNanos = System.nanoTime();
            engine.stepForward(ticksToRun);
            double totalSec = (System.nanoTime() - startNanos) / 1_000_000_000.0;
            double actualTPS = ticksToRun / Math.max(0.0001, totalSec);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.printf("Headless Execution Completed in %.3f seconds!%n", totalSec);
            System.out.printf("Throughput      : %.2f TPS (Ticks Per Second)%n", actualTPS);
            System.out.printf("Cell Throughput : %,d cell-updates/sec%n", (long) (actualTPS * cells.size()));
            System.out.printf("Final Pop       : %,d humans (Year %d)%n",
                    engine.getTotalPopulation(), engine.getTimeManager().getCurrentYear());
            System.out.println("--------------------------------------------------------------------------------\n");

            if (opts.showProfile) {
                System.out.println(profiler.generateReport());
            }
            if (opts.saveEnabled) {
                persistRun(engine, opts);
            }
            if (opts.generateReport) {
                generateAndWriteHeadlessReport(engine, scenario, opts);
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

    private static void configureRuntimePreferences(HeadlessCliOptions opts) {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
                opts.isSingleCore ? "1" : String.valueOf(opts.threadCount));
        execPrefs.putBoolean("ether_gpu_enabled", false);
        prefPrefs.putBoolean("ether_gpu_enabled", false);
        String hwMode = switch (opts.engineMode) {
            case RUST_NATIVE -> "NATIVE_RUST";
            case JAVA_VECTOR_SIMD -> "JAVA_VECTOR_SIMD";
            case CPU_JIT -> "CPU_JIT";
            case SAFE_FALLBACK -> "GPU_OFF";
        };
        execPrefs.put("ether_hardware_mode", hwMode);
    }

    private static ClusterManager startClusterIfEnabled(HeadlessCliOptions opts) throws Exception {
        if (!opts.isClusterMode) return null;
        ClusterManager cm = new ClusterManager(opts.clusterRole, opts.masterHost, opts.port, opts.secretToken);
        cm.setPartitionStrategy(opts.partitionStrategy);
        cm.setBarrierTimeoutMs(opts.barrierTimeoutMs);
        cm.setSyncInterval(opts.syncInterval);
        cm.setHeartbeatIntervalSec(opts.heartbeatIntervalSec);
        cm.setHeartbeatTimeoutSec(opts.heartbeatTimeoutSec);
        cm.setHaloExchangeEnabled(opts.haloExchangeEnabled);
        if (opts.workerNodeId != null && !opts.workerNodeId.isBlank()) cm.setCustomWorkerId(opts.workerNodeId);
        if (opts.workerCapacity != null && !opts.workerCapacity.isBlank()) cm.setCustomWorkerCapacity(opts.workerCapacity);
        if (opts.snapshotsEnabled && opts.clusterRole == ClusterManager.ClusterRole.MASTER) {
            ClusterSnapshotManager snapMgr = new ClusterSnapshotManager(Paths.get(opts.snapshotDir), opts.maxSnapshots);
            cm.setSnapshotManager(snapMgr);
            cm.setSnapshotIntervalTicks(opts.snapshotIntervalTicks > 0 ? opts.snapshotIntervalTicks : 50);
        }
        cm.start();
        logger.info("Headless Cluster Manager active (Role: {}).", opts.clusterRole);
        return cm;
    }

    private static void runWorkerNodeBlocking(ClusterManager clusterManager) throws InterruptedException {
        final WorkerGPUOffloader gpuOffloader = new WorkerGPUOffloader();
        clusterManager.setWorkerComputeDelegate(buf -> gpuOffloader.computeChunk(buf, 86400f));
        System.out.println("Worker node listening for remote compute tasks from Master... (Press Ctrl+C to terminate)");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down worker node...");
            clusterManager.stop();
        }));
        while (true) {
            Thread.sleep(5000L);
        }
    }

    private static int resolveEffectiveResolution(HeadlessCliOptions opts, Scenario scenario) {
        if (opts.h3Resolution >= 0) return opts.h3Resolution;
        if (scenario.isClippingEnabled() && scenario.getH3Resolution() >= 5) return scenario.getH3Resolution();
        return scenario.getH3Resolution() > 0 ? scenario.getH3Resolution() : 4;
    }

    private static List<H3Cell> buildGridCells(Scenario scenario, int effectiveRes) {
        if (effectiveRes < 0) return SampleDataGenerator.generateEuropeSample();
        scenario.setH3Resolution(effectiveRes);
        return org.ether.society.core.ScenarioGridInitializer.initializeCellsForScenario(scenario, null, null);
    }

    private static void persistRun(H3SimulationEngine engine, HeadlessCliOptions opts) {
        try {
            String saveTarget = opts.customSaveName != null ? opts.customSaveName : "Headless_" + opts.scenarioName;
            System.out.printf("Persisting simulation save '%s'...%n", saveTarget);
            if (engine.getSimulationSaveManager() != null) {
                engine.getSimulationSaveManager().saveSimulation(engine, saveTarget, saveTarget);
                System.out.println("Simulation state successfully persisted to disk & database.");
            }
        } catch (Exception e) {
            logger.warn("Could not persist simulation save: {}", e.getMessage());
        }
    }

    private static void generateAndWriteHeadlessReport(H3SimulationEngine engine, Scenario scenario, HeadlessCliOptions opts) {
        try {
            String saveTarget = opts.customSaveName != null ? opts.customSaveName : "Headless_" + opts.scenarioName;
            var repo = org.ether.society.analytics.SimulationRunRepository.getInstance();
            org.ether.society.ui.analytics.ComparativeTelemetryDataLoader.ensureHistoryLoadedForSave(
                    saveTarget, scenario.getName(), repo);
            org.ether.society.analytics.SimulationRunRecord run = repo.getRun(saveTarget);
            if (run == null || run.getTimeSeriesData().isEmpty()) {
                run = org.ether.society.analytics.HeadlessBatchRunner.executeScenarioHeadless(
                        scenario, opts.computeEngineContributions);
            } else if (opts.computeEngineContributions && run.getEngineContributionReport() == null) {
                var contrib = org.ether.society.analytics.Tier2EngineContributionAnalyzer.analyze(scenario, true, run);
                if (contrib != null) {
                    run.setEngineContributionReport(contrib);
                    org.ether.society.analytics.Tier2EngineContributionAnalyzer.saveReport(
                            org.ether.society.config.EtherPaths.getSavesDir().resolve(saveTarget), contrib);
                }
            }
            if (run == null) return;

            final org.ether.society.analytics.SimulationRunRecord finalRun = run;
            org.ether.society.analytics.ScenarioBenchmarkItem item = new org.ether.society.analytics.ScenarioBenchmarkItem() {
                @Override public String getName() { return scenario.getName(); }
                @Override public String getRunId() { return finalRun.getRunId(); }
            };
            var audit = org.ether.society.analytics.GroundTruthAuditGenerator.generateMultiScenarioGroundTruthAudit(
                    List.of(item),
                    org.ether.society.analytics.HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                    ignored -> finalRun);
            System.out.println(audit.reportMarkdown());

            java.nio.file.Path reportsDir = java.nio.file.Paths.get("docs/reports");
            java.nio.file.Files.createDirectories(reportsDir);
            String safeFile = saveTarget.replaceAll("[^a-zA-Z0-9._-]", "_");
            java.nio.file.Path outFile = reportsDir.resolve("headless_report_" + safeFile + ".md");
            java.nio.file.Files.writeString(outFile, audit.reportMarkdown(), java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("Headless comparative report written to: " + outFile.toAbsolutePath());
        } catch (Exception e) {
            logger.warn("Could not generate headless comparative report: {}", e.getMessage());
        }
    }

    private static void printConfigurationSummary(HeadlessCliOptions opts,
                                                   SimulationPerformanceConfig cfg,
                                                   Scenario scenario,
                                                   int effectiveRes) {
        System.out.println("Configuration Summary:");
        System.out.printf("   • Engine Backend      : %s%n", opts.engineMode);
        System.out.printf("   • Threading Mode      : %s (%d worker threads)%n",
                opts.isSingleCore ? "Single-Core" : "Multi-Core", opts.threadCount);
        System.out.printf("   • Topology Mode       : %s%n",
                opts.isClusterMode ? "Distributed Cluster (" + opts.clusterRole + ")" : "Local Standalone");
        System.out.printf("   • Strict Determinism  : %s%n", cfg.isStrictDeterminism() ? "ON" : "OFF");
        System.out.printf("   • Scenario Epoch      : %s (Start: %d, End: %d, H3 Res: %d)%n",
                opts.scenarioName, scenario.getStartDateYear(), scenario.getEndDateYear(), effectiveRes);
        System.out.println("--------------------------------------------------------------------------------");
    }
}
