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
import org.ether.society.gpu.GPUManager;
import org.ether.society.model.Scenario;
import org.ether.society.ui.ExecutionContextPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Headless & CLI Execution Engine Runner.
 * Fully supports all execution flags identically to Tab 4 (Execution Context Panel):
 * Single-Core vs. Multi-Core, CPU vs. GPU, Java vs. Native Rust Core, Distributed Cluster vs. Local Standalone.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class HeadlessRunner {
    private static final Logger logger = LoggerFactory.getLogger(HeadlessRunner.class);
    private static final Preferences prefs = Preferences.userNodeForPackage(ExecutionContextPanel.class);

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
        System.out.println("==========================================================");
        System.out.println("       ETHER PLANETARY SIMULATION ENGINE — CLI RUNNER     ");
        System.out.println("==========================================================");

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

        boolean isClusterMode = false;
        org.ether.society.network.ClusterManager.ClusterRole clusterRole = org.ether.society.network.ClusterManager.ClusterRole.MASTER;
        String masterHost = "127.0.0.1";
        int port = 9090;
        String secretToken = "EtherClusterSecret2026";
        int syncInterval = 5;

        String scenarioName = "OUT_OF_AFRICA";
        boolean ticksExplicitlySet = false;

        // Parse CLI parameters
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].trim();
            if ("--help".equalsIgnoreCase(arg) || "-help".equalsIgnoreCase(arg) || "-?".equalsIgnoreCase(arg) || "-h".equalsIgnoreCase(arg)) {
                printHelp();
                return;
            } else if (arg.startsWith("--engine=")) {
                String eStr = arg.substring("--engine=".length()).toLowerCase();
                engineMode = parseEngineMode(eStr);
            } else if (("-e".equalsIgnoreCase(arg) || "--engine".equalsIgnoreCase(arg)) && i + 1 < args.length) {
                engineMode = parseEngineMode(args[++i].toLowerCase());
            } else if ("--single-core".equalsIgnoreCase(arg) || "--monocoeur".equalsIgnoreCase(arg) || "--single-threaded".equalsIgnoreCase(arg)) {
                isSingleCore = true;
                threadCount = 1;
            } else if ("--multi-core".equalsIgnoreCase(arg) || "--multicoeur".equalsIgnoreCase(arg)) {
                isSingleCore = false;
                threadCount = Runtime.getRuntime().availableProcessors();
            } else if (arg.startsWith("--threads=") || arg.startsWith("--cores=")) {
                threadCount = Integer.parseInt(arg.substring(arg.indexOf('=') + 1));
                if (threadCount <= 1) isSingleCore = true;
            } else if (("--threads".equalsIgnoreCase(arg) || "--cores".equalsIgnoreCase(arg)) && i + 1 < args.length) {
                threadCount = Integer.parseInt(args[++i]);
                if (threadCount <= 1) isSingleCore = true;
            } else if ("--gpu".equalsIgnoreCase(arg) || "--gpu-on".equalsIgnoreCase(arg) || "--opencl".equalsIgnoreCase(arg)) {
                engineMode = CliEngineMode.GPU_SHADERS;
            } else if ("--no-gpu".equalsIgnoreCase(arg) || "--gpu-off".equalsIgnoreCase(arg)) {
                if (engineMode == CliEngineMode.GPU_SHADERS) {
                    engineMode = CliEngineMode.JAVA_VECTOR_SIMD;
                }
            } else if ("--rust".equalsIgnoreCase(arg) || "--native".equalsIgnoreCase(arg)) {
                engineMode = CliEngineMode.RUST_NATIVE;
            } else if ("--simd".equalsIgnoreCase(arg) || "--vector".equalsIgnoreCase(arg)) {
                engineMode = CliEngineMode.JAVA_VECTOR_SIMD;
            } else if ("--safe".equalsIgnoreCase(arg) || "--fallback".equalsIgnoreCase(arg)) {
                engineMode = CliEngineMode.SAFE_FALLBACK;
                isSingleCore = true;
                threadCount = 1;
            } else if (arg.startsWith("--ticks=")) {
                ticksToRun = Integer.parseInt(arg.substring("--ticks=".length()));
                ticksExplicitlySet = true;
            } else if (("-t".equalsIgnoreCase(arg) || "--ticks".equalsIgnoreCase(arg)) && i + 1 < args.length) {
                ticksToRun = Integer.parseInt(args[++i]);
                ticksExplicitlySet = true;
            } else if (arg.startsWith("--cells=")) {
                cellCount = Integer.parseInt(arg.substring("--cells=".length()));
            } else if (("-c".equalsIgnoreCase(arg) || "--cells".equalsIgnoreCase(arg)) && i + 1 < args.length) {
                cellCount = Integer.parseInt(args[++i]);
            } else if (arg.startsWith("--res=") || arg.startsWith("--resolution=")) {
                h3Resolution = Integer.parseInt(arg.substring(arg.indexOf('=') + 1));
            } else if (("-r".equalsIgnoreCase(arg) || "--res".equalsIgnoreCase(arg) || "--resolution".equalsIgnoreCase(arg)) && i + 1 < args.length) {
                h3Resolution = Integer.parseInt(args[++i]);
            } else if (arg.startsWith("--scenario=")) {
                scenarioName = arg.substring("--scenario=".length());
            } else if (("-s".equalsIgnoreCase(arg) || "--scenario".equalsIgnoreCase(arg)) && i + 1 < args.length) {
                scenarioName = args[++i];
            } else if ("--mode=cluster".equalsIgnoreCase(arg) || "--cluster".equalsIgnoreCase(arg)) {
                isClusterMode = true;
            } else if ("--mode=local".equalsIgnoreCase(arg) || "--local".equalsIgnoreCase(arg)) {
                isClusterMode = false;
            } else if ("--role=worker".equalsIgnoreCase(arg) || "--worker".equalsIgnoreCase(arg)) {
                clusterRole = org.ether.society.network.ClusterManager.ClusterRole.WORKER;
                isClusterMode = true;
            } else if ("--role=master".equalsIgnoreCase(arg) || "--master".equalsIgnoreCase(arg)) {
                clusterRole = org.ether.society.network.ClusterManager.ClusterRole.MASTER;
                isClusterMode = true;
            } else if (arg.startsWith("--master-host=")) {
                masterHost = arg.substring("--master-host=".length());
            } else if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring("--port=".length()));
            } else if (arg.startsWith("--secret=")) {
                secretToken = arg.substring("--secret=".length());
            } else if (arg.startsWith("--sync-interval=")) {
                syncInterval = Integer.parseInt(arg.substring("--sync-interval=".length()));
            } else if ("--profile".equalsIgnoreCase(arg) || "-p".equalsIgnoreCase(arg)) {
                showProfile = true;
            }
        }

        // Configure system properties & preferences
        if (isSingleCore) {
            System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1");
        } else {
            System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(threadCount));
        }

        GPUManager gpuManager = new GPUManager();
        if (engineMode == CliEngineMode.GPU_SHADERS) {
            gpuManager.setGpuEnabled(true);
            prefs.putBoolean("ether_gpu_enabled", true);
            prefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.GPU_SHADERS.name());
        } else if (engineMode == CliEngineMode.RUST_NATIVE) {
            gpuManager.setGpuEnabled(false);
            prefs.putBoolean("ether_gpu_enabled", false);
            prefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.NATIVE_RUST.name());
        } else if (engineMode == CliEngineMode.JAVA_VECTOR_SIMD) {
            gpuManager.setGpuEnabled(false);
            prefs.putBoolean("ether_gpu_enabled", false);
            prefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.JAVA_VECTOR_SIMD.name());
        } else if (engineMode == CliEngineMode.CPU_JIT) {
            gpuManager.setGpuEnabled(false);
            prefs.putBoolean("ether_gpu_enabled", false);
            prefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.CPU_JIT.name());
        } else {
            gpuManager.setGpuEnabled(false);
            prefs.putBoolean("ether_gpu_enabled", false);
            prefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.GPU_OFF.name());
        }

        System.out.println("🔧 Configuration Summary:");
        System.out.printf("   • Engine Backend : %s\n", engineMode);
        System.out.printf("   • Threading Mode : %s (%d worker threads)\n", isSingleCore ? "Single-Core (Monocœur)" : "Multi-Core (Multicœur)", threadCount);
        System.out.printf("   • Topology Mode  : %s\n", isClusterMode ? "Distributed Cluster (" + clusterRole + " on " + masterHost + ":" + port + ")" : "Local Standalone");
        System.out.printf("   • H3 Resolution  : %s\n", h3Resolution >= 0 ? "Res " + h3Resolution : "Default Grid / Sample");
        System.out.printf("   • Scenario Epoch : %s\n", scenarioName);
        System.out.println("----------------------------------------------------------");

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

            if (!matched) {
                scenario.setEndDateYear(scenario.getStartDateYear() + Math.max(500, (long)(ticksToRun * scenario.getTemporalResolutionDays() / 365.25) + 50));
                logger.info("Custom Scenario Name: '{}' (Using default start parameters)", scenarioName);
            }

            if (!ticksExplicitlySet) {
                ticksToRun = scenario.calculateScenarioTicks();
                logger.info("Computed Scenario Duration: {} ticks (from Year {} to Year {})",
                        ticksToRun, scenario.getStartDateYear(), scenario.getEndDateYear());
            }

            List<H3Cell> cells;
            if (h3Resolution >= 0) {
                logger.info("Generating planetary Earth grid at H3 Resolution {}...", h3Resolution);
                org.ether.society.procedural.PlanetPreset planetPreset = org.ether.society.procedural.PlanetPreset.EARTH_LIKE.withResolution(h3Resolution);
                List<H3Cell> generated = org.ether.society.procedural.ProceduralGenerator.getInstance().generatePlanet(planetPreset);
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

            System.out.printf("Initializing grid with %d cells (H3 Res: %s)...\n", cells.size(), h3Resolution >= 0 ? String.valueOf(h3Resolution) : "Default");
            engine.initializeFromScenario(scenario, cells);

            if (isClusterMode && clusterRole == org.ether.society.network.ClusterManager.ClusterRole.WORKER) {
                org.ether.society.flux.FluxEngine workerFluxEngine = new org.ether.society.flux.FluxEngine();
                clusterManager.setWorkerComputeDelegate(buf -> workerFluxEngine.tick(buf, 86400f));
                System.out.println("🟢 Worker node listening for remote compute tasks from Master... (Press Ctrl+C to terminate)");
                while (true) {
                    Thread.sleep(5000);
                }
            }

            SimulationProfiler profiler = engine.getProfiler();
            profiler.reset();

            System.out.printf("Starting Headless Execution for %d Ticks...\n", ticksToRun);
            long startNanos = System.nanoTime();

            engine.stepForward(ticksToRun);

            long totalNanos = System.nanoTime() - startNanos;
            double totalSec = totalNanos / 1_000_000_000.0;
            double actualTPS = ticksToRun / totalSec;

            System.out.println("\n----------------------------------------------------------");
            System.out.printf("Headless Execution Completed in %.3f seconds!\n", totalSec);
            System.out.printf("Throughput: %.2f TPS (Ticks Per Second)\n", actualTPS);
            System.out.printf("Cell Throughput: %,d cell-updates/sec\n", (long) (actualTPS * cells.size()));
            System.out.println("----------------------------------------------------------\n");

            if (showProfile) {
                System.out.println(profiler.generateReport());
            }

            try {
                System.out.println("💾 Persisting simulation save and database state...");
                if (engine.getSimulationSaveManager() != null) {
                    engine.getSimulationSaveManager().saveSimulation(engine, "Headless_" + scenarioName);
                    System.out.println("✅ Simulation state successfully persisted to disk & database.");
                }
            } catch (Exception e) {
                logger.warn("Could not persist simulation save: {}", e.getMessage());
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
        System.out.println("\nCompute Engine & Hardware Acceleration Options:");
        System.out.println("  --engine=<rust|gpu|simd|cpu|safe>, -e <type>  Select compute backend (Default: rust/simd)");
        System.out.println("  --rust, --native                              Force Native Rust Multi-Core Engine (Rayon + AVX-512)");
        System.out.println("  --gpu, --opencl                               Force OpenCL GPU Compute Shaders Pipeline");
        System.out.println("  --simd, --vector                              Force Java 21 Incubator Vector SIMD Engine");
        System.out.println("  --safe, --fallback                            Force Software Safe Fallback (Single-Thread SW)");
        System.out.println("\nThreading & Core Allocation Options:");
        System.out.println("  --single-core, --monocoeur                    Run in single-threaded / single-core mode");
        System.out.println("  --multi-core, --multicoeur                    Run in parallel multi-core mode (all available CPUs)");
        System.out.println("  --threads=<N>, --cores=<N>                    Explicitly allocate N CPU worker threads");
        System.out.println("\nExecution Topology Options:");
        System.out.println("  --mode=<local|cluster>, --cluster             Select local standalone or distributed cluster");
        System.out.println("  --role=<master|worker>                        Node role in cluster mode (Default: master)");
        System.out.println("  --master-host=<IP>                            Master IP/hostname (for worker nodes, Default: 127.0.0.1)");
        System.out.println("  --port=<PORT>                                 Cluster communication port (Default: 9090)");
        System.out.println("  --secret=<TOKEN>                              Cluster authentication token");
        System.out.println("  --sync-interval=<N>                           State sync frequency across cluster nodes in ticks");
        System.out.println("\nScenario, Spatial & Chronological Options:");
        System.out.println("  --scenario=<NAME>, -s <NAME>                  Scenario preset (e.g. OUT_OF_AFRICA, NEOLITHIC, CLASSICAL, INDUSTRIAL, MODERN)");
        System.out.println("  --res=<2|3|4|5>, -r <N>                       H3 planetary grid resolution level");
        System.out.println("  --cells=<N>, -c <N>                           Number of H3 cells to simulate");
        System.out.println("  --ticks=<N>, -t <N>                           Target number of simulation ticks (Default: from scenario)");
        System.out.println("  --profile, -p                                 Display end-of-run profiling telemetry");
        System.out.println("  --help, -h                                    Display this help manual");
    }
}
