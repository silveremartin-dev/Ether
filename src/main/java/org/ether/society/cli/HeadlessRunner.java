/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.cli;

import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.dod.NativeRustBridge;
import org.ether.society.core.profiling.SimulationProfiler;
import org.ether.society.data.SampleDataGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.ether.society.network.ClusterManager;
import org.ether.society.network.cluster.ClusterSnapshotManager;
import org.ether.society.network.cluster.WorkerGPUOffloader;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.ui.ExecutionContextPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Headless &amp; CLI Execution Engine Runner for Master Servers and Worker Nodes.
 * Fully aligned with UI Tab 3 (Scenario Optimizations &amp; Determinism) and
 * Tab 4 (Execution Context Hardware &amp; Cluster Topology).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class HeadlessRunner {
    private static final Logger logger = LoggerFactory.getLogger(HeadlessRunner.class);
    private static final Preferences execPrefs = Preferences.userNodeForPackage(ExecutionContextPanel.class);
    private static final Preferences prefPrefs = Preferences.userNodeForPackage(org.ether.society.ui.PreferencesPanel.class);
    private static final Preferences i18nPrefs = Preferences.userNodeForPackage(org.ether.society.i18n.I18n.class);
    private static final Preferences themePrefs = Preferences.userNodeForPackage(org.ether.society.ui.Theme.class);

    public enum CliEngineMode {
        RUST_NATIVE,
        GPU_SHADERS,
        JAVA_VECTOR_SIMD,
        CPU_JIT,
        SAFE_FALLBACK
    }

    public static void main(String[] args) {
        run(args);
    }

    public static void run(String[] args) {
        System.out.println("================================================================================");
        System.out.println("           ETHER PLANETARY SIMULATION ENGINE â€” HIGH-PERFORMANCE CLI             ");
        System.out.println("================================================================================");

        int ticksToRun = 300;
        int cellCount = 3000;
        int h3Resolution = -1;
        int threadCount = Runtime.getRuntime().availableProcessors();
        boolean isSingleCore = false;
        boolean showProfile = true;

        CliEngineMode engineMode = CliEngineMode.RUST_NATIVE;
        if (!NativeRustBridge.isNativeAvailable()) {
            engineMode = CliEngineMode.JAVA_VECTOR_SIMD;
        }

        // Tab 4: Cluster & Node Configuration
        boolean isClusterMode = false;
        ClusterManager.ClusterRole clusterRole = ClusterManager.ClusterRole.MASTER;
        ClusterManager.PartitionStrategy partitionStrategy = ClusterManager.PartitionStrategy.HILBERT;
        String masterHost = "127.0.0.1";
        int port = 9090;
        String secretToken = "EtherClusterSecret2026";
        int syncInterval = 5;
        long barrierTimeoutMs = 3000;
        int heartbeatIntervalSec = 2;
        int heartbeatTimeoutSec = 8;
        String workerNodeId = null;
        String workerCapacity = "Compute Core Node";
        boolean workerGpuEnabled = false;
        boolean haloExchangeEnabled = false;
        boolean snapshotsEnabled = false;
        int snapshotIntervalTicks = 0;
        String snapshotDir = "saves/cluster_snapshots";
        int maxSnapshots = 5;

        // Tab 3: Performance & Scenario Optimizations Tracking
        Boolean cliStrictDeterminism = null;
        Boolean cliSparseCellSkipping = null;
        Boolean cliOceanMacroAggregation = null;
        Boolean cliCoastalNavigationOnly = null;
        Boolean cliOceanMultiRateTicking = null;
        Integer cliClimateTickFrequency = null;
        Boolean cliParallelExecution = null;
        Boolean cliSpatialRangeTruncation = null;

        // Scenario & Spatial Overrides
        String scenarioName = "OUT_OF_AFRICA";
        boolean ticksExplicitlySet = false;
        Long overrideStartYear = null;
        Long overrideEndYear = null;
        Double overrideDt = null;
        Long overrideInitialPop = null;
        Integer overrideInitialTech = null;
        Long randomSeed = null;

        // Persistence & Telemetry
        boolean saveEnabled = true;
        String customSaveName = null;

        // Parse CLI parameters
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].trim();
            String a = arg.toLowerCase();

            if (a.equals("--help") || a.equals("-help") || a.equals("-?") || a.equals("-h")) {
                printHelp();
                return;
            }

            // Tab 4: Engine & Hardware Backend
            else if (a.startsWith("--engine=")) {
                engineMode = parseEngineMode(a.substring("--engine=".length()));
            } else if ((a.equals("-e") || a.equals("--engine")) && i + 1 < args.length) {
                engineMode = parseEngineMode(args[++i].toLowerCase());
            } else if (a.equals("--rust") || a.equals("--native")) {
                engineMode = CliEngineMode.RUST_NATIVE;
            } else if (a.equals("--gpu") || a.equals("--opencl") || a.equals("--gpu-on")) {
                engineMode = CliEngineMode.GPU_SHADERS;
                workerGpuEnabled = true;
            } else if (a.equals("--no-gpu") || a.equals("--gpu-off")) {
                if (engineMode == CliEngineMode.GPU_SHADERS) {
                    engineMode = CliEngineMode.JAVA_VECTOR_SIMD;
                }
                workerGpuEnabled = false;
            } else if (a.equals("--simd") || a.equals("--vector")) {
                engineMode = CliEngineMode.JAVA_VECTOR_SIMD;
            } else if (a.equals("--cpu") || a.equals("--cpu-jit")) {
                engineMode = CliEngineMode.CPU_JIT;
            } else if (a.equals("--safe") || a.equals("--fallback")) {
                engineMode = CliEngineMode.SAFE_FALLBACK;
                isSingleCore = true;
                threadCount = 1;
            }

            // Tab 4: Threading & Cores
            else if (a.equals("--single-core") || a.equals("--monocoeur") || a.equals("--single-threaded")) {
                isSingleCore = true;
                threadCount = 1;
            } else if (a.equals("--multi-core") || a.equals("--multicoeur")) {
                isSingleCore = false;
                threadCount = Runtime.getRuntime().availableProcessors();
            } else if (a.startsWith("--threads=") || a.startsWith("--cores=")) {
                threadCount = Integer.parseInt(a.substring(a.indexOf('=') + 1));
                if (threadCount <= 1) isSingleCore = true;
            } else if ((a.equals("--threads") || a.equals("--cores")) && i + 1 < args.length) {
                threadCount = Integer.parseInt(args[++i]);
                if (threadCount <= 1) isSingleCore = true;
            }

            // Tab 4: Cluster & Multi-Node Topology
            else if (a.equals("--mode=cluster") || a.equals("--cluster")) {
                isClusterMode = true;
            } else if (a.equals("--mode=local") || a.equals("--local")) {
                isClusterMode = false;
            } else if (a.equals("--role=worker") || a.equals("--worker") || a.equals("--node")) {
                clusterRole = ClusterManager.ClusterRole.WORKER;
                isClusterMode = true;
            } else if (a.equals("--role=master") || a.equals("--master") || a.equals("--server")) {
                clusterRole = ClusterManager.ClusterRole.MASTER;
                isClusterMode = true;
            } else if (a.startsWith("--node-id=") || a.startsWith("--worker-id=") || a.startsWith("--id=")) {
                workerNodeId = arg.substring(arg.indexOf('=') + 1).trim();
            } else if (a.startsWith("--worker-capacity=") || a.startsWith("--capacity=")) {
                workerCapacity = arg.substring(arg.indexOf('=') + 1).trim();
            } else if (a.startsWith("--master-host=") || a.startsWith("--host=") || a.startsWith("--server-host=")) {
                masterHost = arg.substring(arg.indexOf('=') + 1).trim();
            } else if ((a.equals("-h") || a.equals("-host") || a.equals("--host")) && i + 1 < args.length) {
                masterHost = args[++i].trim();
            } else if (a.startsWith("--port=")) {
                port = Integer.parseInt(a.substring("--port=".length()));
            } else if ((a.equals("-p") || a.equals("-port") || a.equals("--port")) && i + 1 < args.length && !args[i+1].startsWith("-") && args[i+1].matches("\\d+")) {
                port = Integer.parseInt(args[++i]);
            } else if (a.startsWith("--secret=") || a.startsWith("--auth-token=")) {
                secretToken = arg.substring(arg.indexOf('=') + 1).trim();
            } else if (a.startsWith("--sync-interval=")) {
                syncInterval = Integer.parseInt(a.substring("--sync-interval=".length()));
            } else if (a.startsWith("--barrier-timeout=")) {
                barrierTimeoutMs = Long.parseLong(a.substring("--barrier-timeout=".length()));
            } else if (a.startsWith("--heartbeat-interval=")) {
                heartbeatIntervalSec = Integer.parseInt(a.substring("--heartbeat-interval=".length()));
            } else if (a.startsWith("--heartbeat-timeout=")) {
                heartbeatTimeoutSec = Integer.parseInt(a.substring("--heartbeat-timeout=".length()));
            } else if (a.startsWith("--partition-strategy=") || a.startsWith("--split-strategy=")) {
                String stratStr = a.substring(a.indexOf('=') + 1).toUpperCase();
                if (stratStr.contains("LOAD") || stratStr.contains("WEIGHT") || stratStr.contains("DYN")) {
                    partitionStrategy = ClusterManager.PartitionStrategy.LOAD_AWARE;
                } else if (stratStr.contains("SLICE") || stratStr.contains("EQUAL") || stratStr.contains("LAT")) {
                    partitionStrategy = ClusterManager.PartitionStrategy.EQUAL_SLICES;
                } else {
                    partitionStrategy = ClusterManager.PartitionStrategy.HILBERT;
                }
            } else if (a.startsWith("--halo-exchange")) {
                haloExchangeEnabled = !a.endsWith("=false");
            } else if (a.startsWith("--snapshots")) {
                snapshotsEnabled = !a.endsWith("=false");
            } else if (a.startsWith("--snapshot-interval=")) {
                snapshotIntervalTicks = Integer.parseInt(a.substring("--snapshot-interval=".length()));
                snapshotsEnabled = snapshotIntervalTicks > 0;
            } else if (a.startsWith("--snapshot-dir=")) {
                snapshotDir = arg.substring(arg.indexOf('=') + 1).trim();
            } else if (a.startsWith("--max-snapshots=")) {
                maxSnapshots = Integer.parseInt(a.substring("--max-snapshots=".length()));
            } else if (a.equals("--worker-gpu")) {
                workerGpuEnabled = true;
            } else if (a.equals("--worker-rust")) {
                engineMode = CliEngineMode.RUST_NATIVE;
            } else if (a.equals("--worker-simd")) {
                engineMode = CliEngineMode.JAVA_VECTOR_SIMD;
            } else if (a.equals("--worker-cpu")) {
                engineMode = CliEngineMode.CPU_JIT;
            }

            // Tab 3: Scenario Determinism & Approximation Options
            else if (a.startsWith("--strict-determinism")) {
                cliStrictDeterminism = !a.endsWith("=false");
            } else if (a.equals("--no-determinism") || a.equals("--fast")) {
                cliStrictDeterminism = false;
            } else if (a.startsWith("--sparse-skipping") || a.startsWith("--sparse-cells")) {
                cliSparseCellSkipping = !a.endsWith("=false");
            } else if (a.equals("--no-sparse-skipping")) {
                cliSparseCellSkipping = false;
            } else if (a.startsWith("--ocean-macro-aggregation") || a.startsWith("--macro-aggregation") || a.startsWith("--macro-ocean")) {
                cliOceanMacroAggregation = !a.endsWith("=false");
            } else if (a.equals("--no-ocean-macro-aggregation")) {
                cliOceanMacroAggregation = false;
            } else if (a.startsWith("--coastal-nav-only") || a.startsWith("--coastal-navigation")) {
                cliCoastalNavigationOnly = !a.endsWith("=false");
            } else if (a.equals("--no-coastal-nav")) {
                cliCoastalNavigationOnly = false;
            } else if (a.startsWith("--ocean-multi-rate") || a.startsWith("--multi-rate-climate") || a.startsWith("--multi-freq-climate")) {
                cliOceanMultiRateTicking = !a.endsWith("=false");
            } else if (a.equals("--no-ocean-multi-rate")) {
                cliOceanMultiRateTicking = false;
            } else if (a.startsWith("--climate-freq=")) {
                cliClimateTickFrequency = Integer.parseInt(a.substring("--climate-freq=".length()));
                cliOceanMultiRateTicking = true;
            } else if (a.startsWith("--parallel") || a.startsWith("--async-threads")) {
                cliParallelExecution = !a.endsWith("=false");
            } else if (a.equals("--no-parallel")) {
                cliParallelExecution = false;
            } else if (a.startsWith("--spatial-truncation")) {
                cliSpatialRangeTruncation = !a.endsWith("=false");
            } else if (a.equals("--no-spatial-truncation")) {
                cliSpatialRangeTruncation = false;
            }

            // Scenario, Spatiotemporal & Historical Presets
            else if (a.startsWith("--scenario=")) {
                scenarioName = arg.substring("--scenario=".length()).trim();
            } else if ((a.equals("-s") || a.equals("--scenario")) && i + 1 < args.length) {
                scenarioName = args[++i].trim();
            } else if (a.startsWith("--ticks=")) {
                ticksToRun = Integer.parseInt(a.substring("--ticks=".length()));
                ticksExplicitlySet = true;
            } else if ((a.equals("-t") || a.equals("--ticks")) && i + 1 < args.length) {
                ticksToRun = Integer.parseInt(args[++i]);
                ticksExplicitlySet = true;
            } else if (a.startsWith("--cells=")) {
                cellCount = Integer.parseInt(a.substring("--cells=".length()));
            } else if ((a.equals("-c") || a.equals("--cells")) && i + 1 < args.length) {
                cellCount = Integer.parseInt(args[++i]);
            } else if (a.startsWith("--res=") || a.startsWith("--resolution=")) {
                h3Resolution = Integer.parseInt(a.substring(a.indexOf('=') + 1));
            } else if ((a.equals("-r") || a.equals("--res") || a.equals("--resolution")) && i + 1 < args.length) {
                h3Resolution = Integer.parseInt(args[++i]);
            } else if (a.startsWith("--start-year=")) {
                overrideStartYear = Long.parseLong(a.substring("--start-year=".length()));
            } else if (a.startsWith("--end-year=")) {
                overrideEndYear = Long.parseLong(a.substring("--end-year=".length()));
            } else if (a.startsWith("--dt=") || a.startsWith("--temporal-res=")) {
                overrideDt = Double.parseDouble(a.substring(a.indexOf('=') + 1));
            } else if (a.startsWith("--initial-pop=") || a.startsWith("--pop=")) {
                overrideInitialPop = Long.parseLong(a.substring(a.indexOf('=') + 1));
            } else if (a.startsWith("--initial-tech=") || a.startsWith("--tech=")) {
                overrideInitialTech = Integer.parseInt(a.substring(a.indexOf('=') + 1));
            } else if (a.startsWith("--seed=")) {
                randomSeed = Long.parseLong(a.substring("--seed=".length()));
            }

            // Persistence & Telemetry
            else if (a.startsWith("--save=")) {
                customSaveName = arg.substring("--save=".length()).trim();
                saveEnabled = true;
            } else if (a.equals("--no-save")) {
                saveEnabled = false;
            } else if (a.equals("--profile") || a.equals("-p")) {
                showProfile = true;
            } else if (a.equals("--no-profile")) {
                showProfile = false;
            }

            // Preferences (Language & Theme)
            else if (a.startsWith("--lang=") || a.startsWith("--language=")) {
                String lang = a.substring(a.indexOf('=') + 1);
                i18nPrefs.put("ether_language", lang);
            } else if ((a.equals("-l") || a.equals("--lang") || a.equals("--language")) && i + 1 < args.length) {
                i18nPrefs.put("ether_language", args[++i].trim().toLowerCase());
            } else if (a.startsWith("--theme=")) {
                String theme = a.substring(a.indexOf('=') + 1).toUpperCase();
                themePrefs.put("ether_theme", theme);
            }
        }

        // Configure system properties & preferences
        if (isSingleCore) {
            System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1");
        } else {
            System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(threadCount));
        }

        if (engineMode == CliEngineMode.RUST_NATIVE) {
            execPrefs.putBoolean("ether_gpu_enabled", false);
            prefPrefs.putBoolean("ether_gpu_enabled", false);
            execPrefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.NATIVE_RUST.name());
        } else if (engineMode == CliEngineMode.JAVA_VECTOR_SIMD) {
            execPrefs.putBoolean("ether_gpu_enabled", false);
            prefPrefs.putBoolean("ether_gpu_enabled", false);
            execPrefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.JAVA_VECTOR_SIMD.name());
        } else if (engineMode == CliEngineMode.CPU_JIT) {
            execPrefs.putBoolean("ether_gpu_enabled", false);
            prefPrefs.putBoolean("ether_gpu_enabled", false);
            execPrefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.CPU_JIT.name());
        } else {
            execPrefs.putBoolean("ether_gpu_enabled", false);
            prefPrefs.putBoolean("ether_gpu_enabled", false);
            execPrefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.GPU_OFF.name());
        }

        try {
            ClusterManager clusterManager = null;
            if (isClusterMode) {
                clusterManager = new ClusterManager(clusterRole, masterHost, port, secretToken);
                clusterManager.setPartitionStrategy(partitionStrategy);
                clusterManager.setBarrierTimeoutMs(barrierTimeoutMs);
                clusterManager.setSyncInterval(syncInterval);
                clusterManager.setHeartbeatIntervalSec(heartbeatIntervalSec);
                clusterManager.setHeartbeatTimeoutSec(heartbeatTimeoutSec);
                clusterManager.setHaloExchangeEnabled(haloExchangeEnabled);

                if (workerNodeId != null && !workerNodeId.isBlank()) {
                    clusterManager.setCustomWorkerId(workerNodeId);
                }
                if (workerCapacity != null && !workerCapacity.isBlank()) {
                    clusterManager.setCustomWorkerCapacity(workerCapacity);
                }

                if (snapshotsEnabled && clusterRole == ClusterManager.ClusterRole.MASTER) {
                    ClusterSnapshotManager snapMgr = new ClusterSnapshotManager(Paths.get(snapshotDir), maxSnapshots);
                    clusterManager.setSnapshotManager(snapMgr);
                    clusterManager.setSnapshotIntervalTicks(snapshotIntervalTicks > 0 ? snapshotIntervalTicks : 50);
                }

                clusterManager.start();
                logger.info("Headless Cluster Manager active (Role: {}).", clusterRole);
            }

            // WORKER NODE EXECUTION LOOP
            if (isClusterMode && clusterRole == ClusterManager.ClusterRole.WORKER) {
                final WorkerGPUOffloader gpuOffloader = new WorkerGPUOffloader();
                clusterManager.setWorkerComputeDelegate(buf -> {
                    gpuOffloader.computeChunk(buf, 86400f);
                });

                System.out.println("🟢 Worker node listening for remote compute tasks from Master... (Press Ctrl+C to terminate)");

                final ClusterManager finalClusterMgr = clusterManager;
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("\nShutting down worker node...");
                    if (finalClusterMgr != null) finalClusterMgr.stop();
                }));

                while (true) {
                    Thread.sleep(5000);
                }
            }

            // MASTER / STANDALONE SIMULATION INITIALIZATION
            Configuration config = ConfigurationLoader.loadDefault();
            H3SimulationEngine engine = new H3SimulationEngine(config);

            // Configure requested scenario
            Scenario scenario = new Scenario();
            scenario.setName("Headless: " + scenarioName);
            scenario.setPopulationDensityType(scenarioName);

            // Match CalibrationScenarioDefinition, StartDatePreset, or Built-In Scenarios
            boolean matched = false;

            // 1. Check Historical Calibration Scenario Targets
            for (org.ether.society.analytics.HistoricalScenarioCalibrationHarness.CalibrationScenarioDefinition calDef :
                    org.ether.society.analytics.HistoricalScenarioCalibrationHarness.CALIBRATION_SCENARIOS) {
                if (calDef.scenarioKey().equalsIgnoreCase(scenarioName)
                        || calDef.displayName().equalsIgnoreCase(scenarioName)
                        || scenarioName.toLowerCase().contains(calDef.scenarioKey().toLowerCase())) {
                    scenario.setStartDateYear(calDef.startYear());
                    scenario.setEndDateYear(calDef.endYear());
                    scenario.setInitialHumanCount((long)(calDef.initialWorldPopMillions() * 1_000_000L));
                    scenario.setInitialCapitalPerCapita(calDef.initialCapitalPerCapita());
                    scenario.setInitialTechLevel((int) calDef.initialTechLevel());
                    scenario.setTemporalResolutionDays(365.0);
                    scenario.setName(calDef.displayName());
                    scenario.setDescription(calDef.historicalRegimeDescription());
                    logger.info("Matched Historical Calibration Scenario: '{}' (Years {} -> {}, Pop: {} M, Tech: {})",
                            calDef.displayName(), calDef.startYear(), calDef.endYear(), calDef.initialWorldPopMillions(), calDef.initialTechLevel());
                    matched = true;
                    break;
                }
            }

            // 2. Check Built-In Scenarios
            if (!matched) {
                for (Scenario builtIn : Scenario.getBuiltInScenarios()) {
                    if ((builtIn.getPresetKey() != null && builtIn.getPresetKey().equalsIgnoreCase(scenarioName))
                            || (builtIn.getName() != null && builtIn.getName().equalsIgnoreCase(scenarioName))
                            || (builtIn.getPresetKey() != null && scenarioName.toLowerCase().contains(builtIn.getPresetKey().toLowerCase()))) {
                        scenario.setStartDateYear(builtIn.getStartDateYear());
                        scenario.setEndDateYear(builtIn.getEndDateYear());
                        scenario.setInitialHumanCount(builtIn.getInitialHumanCount());
                        scenario.setInitialCapitalPerCapita(builtIn.getInitialCapitalPerCapita());
                        scenario.setInitialEnergyPerCapita(builtIn.getInitialEnergyPerCapita());
                        scenario.setInitialTechLevel(builtIn.getInitialTechLevel());
                        scenario.setTemporalResolutionDays(builtIn.getTemporalResolutionDays());
                        scenario.setName(builtIn.getName());
                        scenario.setDescription(builtIn.getDescription());
                        logger.info("Matched Built-In Scenario: '{}' (Years {} -> {}, Initial Pop: {})",
                                builtIn.getName(), builtIn.getStartDateYear(), builtIn.getEndDateYear(), builtIn.getInitialHumanCount());
                        matched = true;
                        break;
                    }
                }
            }

            // 3. Check StartDatePreset by enum key or full descriptive display name
            if (!matched) {
                for (org.ether.society.model.StartDatePreset preset : org.ether.society.model.StartDatePreset.values()) {
                    if (preset.name().equalsIgnoreCase(scenarioName)
                            || preset.getDisplayName().equalsIgnoreCase(scenarioName)
                            || scenarioName.toLowerCase().contains(preset.getDisplayName().toLowerCase())) {
                        scenario.setStartDateYear(preset.getYear());
                        scenario.setEndDateYear(preset.getYear() + Math.max(500, (long)(ticksToRun * scenario.getTemporalResolutionDays() / 365.25) + 50));
                        scenario.setInitialHumanCount(preset.getEstimatedPopulation());
                        scenario.setInitialTechLevel(preset.getEstimatedTechLevel());
                        scenario.setName(preset.getDisplayName());
                        logger.info("Matched Historical Scenario Preset: '{}' (Start Year: {}, End Year: {}, Initial Pop: {}, Tech Level: {})",
                                preset.getDisplayName(), preset.getYear(), scenario.getEndDateYear(), preset.getEstimatedPopulation(), preset.getEstimatedTechLevel());
                        matched = true;
                        break;
                    }
                }
            }

            if (!matched) {
                scenario.setEndDateYear(scenario.getStartDateYear() + Math.max(500, (long)(ticksToRun * scenario.getTemporalResolutionDays() / 365.25) + 50));
                logger.info("Custom Scenario Name: '{}' (Using default start parameters)", scenarioName);
            }

            // Apply CLI Overrides on Spatiotemporal parameters
            if (overrideStartYear != null) scenario.setStartDateYear(overrideStartYear);
            if (overrideEndYear != null) scenario.setEndDateYear(overrideEndYear);
            if (overrideDt != null) scenario.setTemporalResolutionDays(overrideDt);
            if (overrideInitialPop != null) scenario.setInitialHumanCount(overrideInitialPop);
            if (overrideInitialTech != null) scenario.setInitialTechLevel(overrideInitialTech);
            if (randomSeed != null) scenario.setSeed(randomSeed);

            // Apply CLI Overrides on Tab 3 Scenario Optimizations
            if (cliStrictDeterminism != null) {
                scenario.setStrictDeterminism(cliStrictDeterminism);
            }
            if (cliSparseCellSkipping != null) {
                scenario.setSparseCellSkippingEnabled(cliSparseCellSkipping);
            }
            if (cliOceanMacroAggregation != null) {
                scenario.setOceanMacroAggregationEnabled(cliOceanMacroAggregation);
            }
            if (cliCoastalNavigationOnly != null) {
                scenario.setCoastalNavigationOnlyEnabled(cliCoastalNavigationOnly);
            }
            if (cliOceanMultiRateTicking != null) {
                scenario.setOceanMultiRateTickingEnabled(cliOceanMultiRateTicking);
            }
            if (cliClimateTickFrequency != null) {
                scenario.setClimateTickFrequency(cliClimateTickFrequency);
            }
            if (cliParallelExecution != null) {
                scenario.setParallelExecutionEnabled(cliParallelExecution);
                scenario.setParallelThreadCount(threadCount);
            }
            if (cliSpatialRangeTruncation != null) {
                scenario.setSpatialRangeTruncationEnabled(cliSpatialRangeTruncation);
            }

            // Convert scenario to effective runtime performance configuration
            SimulationPerformanceConfig effectivePerfConfig = scenario.toPerformanceConfig();
            effectivePerfConfig.setParallelThreadCount(threadCount);

            if (!ticksExplicitlySet) {
                ticksToRun = scenario.calculateScenarioTicks();
                logger.info("Computed Scenario Duration: {} ticks (from Year {} to Year {})",
                        ticksToRun, scenario.getStartDateYear(), scenario.getEndDateYear());
            }

            System.out.println("ðŸ”§ Configuration Summary:");
            System.out.printf("   [Tab 4: Execution Context & Infrastructure]\n");
            System.out.printf("   â€¢ Engine Backend      : %s\n", engineMode);
            System.out.printf("   â€¢ Threading Mode      : %s (%d worker threads)\n", isSingleCore ? "Single-Core (MonocÅ“ur)" : "Multi-Core (MulticÅ“ur)", threadCount);
            System.out.printf("   â€¢ Topology Mode       : %s\n", isClusterMode ? "Distributed Cluster (" + clusterRole + ")" : "Local Standalone");
            if (isClusterMode) {
                System.out.printf("   â€¢ Cluster Connection  : %s:%d (Secret Token: %s)\n", masterHost, port, secretToken.replaceAll(".", "*"));
                System.out.printf("   â€¢ Partition Strategy  : %s (Sync Interval: %d ticks, Barrier Timeout: %d ms)\n", partitionStrategy, syncInterval, barrierTimeoutMs);
                if (clusterRole == ClusterManager.ClusterRole.WORKER) {
                    System.out.printf("   â€¢ Worker Node ID      : %s (Capacity: %s, GPU Offload: %s)\n",
                            workerNodeId != null ? workerNodeId : "Auto-Generated", workerCapacity, workerGpuEnabled ? "ENABLED" : "DISABLED");
                }
                if (snapshotsEnabled) {
                    System.out.printf("   â€¢ Snapshot Cadence    : Every %d ticks -> %s (max retained: %d)\n", snapshotIntervalTicks, snapshotDir, maxSnapshots);
                }
            }
            System.out.printf("\n   [Tab 3: Scenario Determinism & Approximations]\n");
            System.out.printf("   â€¢ Strict Determinism  : %s\n", effectivePerfConfig.isStrictDeterminism() ? "ON (Tier 1 Bit-Identical Physics)" : "OFF (Heuristic Shortcuts Allowed)");
            System.out.printf("   â€¢ Sparse Cell Skip    : %s (Deserts & Abyssal Oceans)\n", effectivePerfConfig.isEnableSparseCellSkipping() ? "ENABLED" : "DISABLED");
            System.out.printf("   â€¢ Ocean Macro-Aggreg  : %s (Deep Basins z < -200m)\n", effectivePerfConfig.isEnableOceanMacroAggregation() ? "ENABLED" : "DISABLED");
            System.out.printf("   â€¢ Coastal Nav Only    : %s (Pathfinding Focused on Coasts)\n", effectivePerfConfig.isEnableCoastalNavigationOnly() ? "ENABLED" : "DISABLED");
            System.out.printf("   â€¢ Multi-Rate Climate  : %s (Every %d Ticks)\n", effectivePerfConfig.isEnableMultiFreqClimateTicks() ? "ENABLED" : "DISABLED", effectivePerfConfig.getClimateTickFrequency());
            System.out.printf("   â€¢ Async Parallelism   : %s (%d Threads)\n", effectivePerfConfig.isEnableParallelExecution() ? "ENABLED" : "DISABLED", threadCount);
            System.out.printf("   â€¢ Spatial Truncation  : %s (10^-6 Cutoff)\n", effectivePerfConfig.isEnableSpatialRangeTruncation() ? "ENABLED" : "DISABLED");
            System.out.printf("   â€¢ Scenario Epoch      : %s (Start: %d, End: %d, H3 Res: %s)\n",
                    scenarioName, scenario.getStartDateYear(), scenario.getEndDateYear(), h3Resolution >= 0 ? "Res " + h3Resolution : "Default");
            System.out.println("--------------------------------------------------------------------------------");

            List<H3Cell> cells;
            if (h3Resolution >= 0) {
                logger.info("Generating planetary Earth grid at H3 Resolution {}...", h3Resolution);
                org.ether.society.generation.PlanetPreset planetPreset = org.ether.society.generation.PlanetPreset.EARTH_LIKE.withResolution(h3Resolution);
                List<H3Cell> generated = org.ether.society.generation.ProceduralGenerator.getInstance().generatePlanet(planetPreset);
                cells = (cellCount > 0 && cellCount < generated.size())
                        ? new ArrayList<>(generated.subList(0, cellCount))
                        : generated;
            } else {
                List<H3Cell> allCells = SampleDataGenerator.generateEuropeSample();
                cells = (cellCount > 0 && cellCount < allCells.size())
                        ? new ArrayList<>(allCells.subList(0, cellCount))
                        : allCells;
            }

            if (clusterManager != null) {
                clusterManager.setTotalGridCellCount(cells.size());
                clusterManager.dispatchScenarioToCluster(scenario);
                engine.setClusterManager(clusterManager);
            }

            System.out.printf("Initializing grid with %,d cells (H3 Res: %s)...\n", cells.size(), h3Resolution >= 0 ? String.valueOf(h3Resolution) : "Default");
            engine.initializeFromScenario(scenario, cells);
            engine.setPerformanceConfig(effectivePerfConfig);

            SimulationProfiler profiler = engine.getProfiler();
            profiler.reset();

            System.out.printf("Starting Headless Execution for %,d Ticks...\n", ticksToRun);
            long startNanos = System.nanoTime();

            engine.stepForward(ticksToRun);

            long totalNanos = System.nanoTime() - startNanos;
            double totalSec = totalNanos / 1_000_000_000.0;
            double actualTPS = ticksToRun / Math.max(0.0001, totalSec);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.printf("Headless Execution Completed in %.3f seconds!\n", totalSec);
            System.out.printf("Throughput      : %.2f TPS (Ticks Per Second)\n", actualTPS);
            System.out.printf("Cell Throughput : %,d cell-updates/sec\n", (long) (actualTPS * cells.size()));
            System.out.printf("Final Pop       : %,d humans (Year %d)\n", (long) engine.getTotalPopulation(), engine.getTimeManager().getCurrentYear());
            System.out.println("--------------------------------------------------------------------------------\n");

            if (showProfile) {
                System.out.println(profiler.generateReport());
            }

            if (saveEnabled) {
                try {
                    String saveTarget = customSaveName != null ? customSaveName : "Headless_" + scenarioName;
                    System.out.printf("ðŸ’¾ Persisting simulation save '%s'...\n", saveTarget);
                    if (engine.getSimulationSaveManager() != null) {
                        engine.getSimulationSaveManager().saveSimulation(engine, saveTarget);
                        System.out.println("âœ… Simulation state successfully persisted to disk & database.");
                    }
                } catch (Exception e) {
                    logger.warn("Could not persist simulation save: {}", e.getMessage());
                }
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

    private static CliEngineMode parseEngineMode(String str) {
        if (str.contains("rust") || str.contains("native")) return CliEngineMode.RUST_NATIVE;
        if (str.contains("gpu") || str.contains("shader") || str.contains("opencl")) return CliEngineMode.GPU_SHADERS;
        if (str.contains("simd") || str.contains("vector")) return CliEngineMode.JAVA_VECTOR_SIMD;
        if (str.contains("safe") || str.contains("sw")) return CliEngineMode.SAFE_FALLBACK;
        return CliEngineMode.CPU_JIT;
    }

    private static void printHelp() {
        System.out.println("Usage: java -jar society-simulation.jar [options]");
        System.out.println("\nðŸŒ [Tab 4] Compute Engine & Hardware Acceleration Options:");
        System.out.println("  --engine=<rust|gpu|simd|cpu|safe>, -e <type>  Select compute backend (Default: rust/simd)");
        System.out.println("  --rust, --native                              Force Native Rust Multi-Core Engine (Rayon + AVX-512)");
        System.out.println("  --gpu, --opencl, --gpu-on                     Force OpenCL GPU Compute Shaders Pipeline");
        System.out.println("  --no-gpu, --gpu-off                           Disable GPU hardware offloading");
        System.out.println("  --simd, --vector                              Force Java 21 Incubator Vector SIMD Engine");
        System.out.println("  --cpu, --cpu-jit                              Force Pure CPU Java JIT Engine");
        System.out.println("  --safe, --fallback                            Force Software Safe Fallback (Single-Thread SW)");
        System.out.println("\nâš™ï¸ [Tab 4] Threading & Core Allocation Options:");
        System.out.println("  --single-core, --monocoeur                    Run in single-threaded / single-core mode");
        System.out.println("  --multi-core, --multicoeur                    Run in parallel multi-core mode (all available CPUs)");
        System.out.println("  --threads=<N>, --cores=<N>                    Explicitly allocate N CPU worker threads");
        System.out.println("\nðŸ›°ï¸ [Tab 4] Distributed Multi-Node Clustering Options:");
        System.out.println("  --mode=<local|cluster>, --cluster             Select local standalone or distributed cluster");
        System.out.println("  --role=<master|worker>, --master, --worker    Node role in cluster mode (Default: master)");
        System.out.println("  --server, --node                              Aliases for --master and --worker roles");
        System.out.println("  --node-id=<ID>, --worker-id=<ID>              Unique identifier name for this node/worker");
        System.out.println("  --worker-capacity=<DESC>                      Descriptive hardware capacity label for worker");
        System.out.println("  --master-host=<IP>, --host=<IP>               Master IP/hostname (for worker nodes, Default: 127.0.0.1)");
        System.out.println("  --port=<PORT>, -P <PORT>                      Cluster communication port (Default: 9090)");
        System.out.println("  --secret=<TOKEN>, --auth-token=<TOKEN>        Cluster AES-256 handshake authentication token");
        System.out.println("  --partition-strategy=<HILBERT|LOAD_AWARE|EQUAL_SLICES>  Spatial partition algorithm (Default: HILBERT)");
        System.out.println("  --sync-interval=<N>                           State sync frequency across cluster nodes in ticks (Default: 5)");
        System.out.println("  --barrier-timeout=<MS>                        Lock-step barrier wait timeout in milliseconds (Default: 3000)");
        System.out.println("  --heartbeat-interval=<SEC>                    Worker heartbeat ping interval in seconds (Default: 2)");
        System.out.println("  --heartbeat-timeout=<SEC>                     Master heartbeat timeout before node drop (Default: 8)");
        System.out.println("  --snapshots                                   Enable periodic WorldBuffer cluster snapshots");
        System.out.println("  --snapshot-interval=<N>                       Snapshot checkpoint interval in ticks (Default: 50)");
        System.out.println("  --snapshot-dir=<PATH>                         Snapshot directory path (Default: saves/cluster_snapshots)");
        System.out.println("  --worker-gpu, --worker-rust, --worker-simd    Acceleration backend specifically on worker nodes");
        System.out.println("\nâš¡ [Tab 3] Scenario Determinism & Approximation Options:");
        System.out.println("  --strict-determinism[=true|false]             Enforce strict bit-identical physical conservation (Tier 1)");
        System.out.println("  --no-determinism, --fast                      Disable strict determinism to allow heuristic approximations");
        System.out.println("  --sparse-skipping[=true|false], --sparse-cells Skip compute updates on empty ocean/desert cells");
        System.out.println("  --ocean-macro-aggregation[=true|false]        Group abyssal ocean basins (z < -200m) into coarse blocks");
        System.out.println("  --coastal-nav-only[=true|false]               Focus trade/military maritime pathfinding on coasts & straits");
        System.out.println("  --ocean-multi-rate[=true|false]               Run thermohaline & climate diffusion at sub-frequency");
        System.out.println("  --climate-freq=<N>                            Multi-rate climate frequency (Default: 5 ticks)");
        System.out.println("  --parallel[=true|false], --async-threads      Enable async multi-thread parallel stream processing");
        System.out.println("  --spatial-truncation[=true|false]             Truncate dispersion plumes at 10^-6 cutoff");
        System.out.println("\nðŸ—ºï¸ Scenario, Spatial & Chronological Options:");
        System.out.println("  --scenario=<NAME>, -s <NAME>                  Scenario preset (e.g. OUT_OF_AFRICA, NEOLITHIC, CLASSICAL, INDUSTRIAL, MODERN)");
        System.out.println("  --res=<2..6>, -r <N>, --resolution=<N>        H3 planetary grid resolution level");
        System.out.println("  --cells=<N>, -c <N>                           Number of H3 cells to simulate");
        System.out.println("  --ticks=<N>, -t <N>                           Target number of simulation ticks (Default: from scenario)");
        System.out.println("  --start-year=<YEAR>                           Override scenario starting year (BCE negative, CE positive)");
        System.out.println("  --end-year=<YEAR>                             Override scenario ending year");
        System.out.println("  --dt=<DAYS>, --temporal-res=<DAYS>            Time step per tick in days (Default: from scenario)");
        System.out.println("  --initial-pop=<N>, --pop=<N>                  Override initial human population count");
        System.out.println("  --initial-tech=<N>, --tech=<N>                Override initial technological level (0..10)");
        System.out.println("  --seed=<LONG>                                 PRNG deterministic random seed");
        System.out.println("\nðŸ’¾ Persistence, Profiling & Global Preferences:");
        System.out.println("  --save=<NAME>                                 Custom save file name for persistence");
        System.out.println("  --no-save                                     Disable simulation saving upon completion");
        System.out.println("  --profile, -p / --no-profile                  Display end-of-run profiling telemetry report");
        System.out.println("  --lang=<EN|FR|DE|ES|ZH>, -l <LANG>            Set internationalization interface language");
        System.out.println("  --theme=<dark|light|presentation>             Set UI visual theme");
        System.out.println("  --help, -h, -?                                Display this help manual");
    }
}

