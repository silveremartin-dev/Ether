/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.cli;

import org.ether.society.core.dod.NativeRustBridge;
import org.ether.society.i18n.I18n;
import org.ether.society.network.ClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

/**
 * Command-line argument configuration carrier and parser for {@link HeadlessRunner}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see HeadlessRunner
 */
public final class HeadlessCliOptions {

    private static final Logger logger = LoggerFactory.getLogger(HeadlessCliOptions.class);

    public enum EarlyAction {
        NONE,
        HELP,
        BENCHMARK_REAL,
        BENCHMARK_DOD
    }

    public EarlyAction earlyAction = EarlyAction.NONE;
    public int ticksToRun = 300;
    public int h3Resolution = -1;
    public int threadCount = Runtime.getRuntime().availableProcessors();
    public boolean isSingleCore = false;
    public boolean showProfile = true;

    public HeadlessRunner.CliEngineMode engineMode = NativeRustBridge.isNativeAvailable()
            ? HeadlessRunner.CliEngineMode.RUST_NATIVE
            : HeadlessRunner.CliEngineMode.JAVA_VECTOR_SIMD;

    public boolean isClusterMode = false;
    public ClusterManager.ClusterRole clusterRole = ClusterManager.ClusterRole.MASTER;
    public ClusterManager.PartitionStrategy partitionStrategy = ClusterManager.PartitionStrategy.HILBERT;
    public String masterHost = "127.0.0.1";
    public int port = 9090;
    public String secretToken = System.getenv("ETHER_CLUSTER_SECRET") != null
            ? System.getenv("ETHER_CLUSTER_SECRET")
            : "default-cluster-token";
    public int syncInterval = 5;
    public long barrierTimeoutMs = 3000;
    public int heartbeatIntervalSec = 2;
    public int heartbeatTimeoutSec = 8;
    public String workerNodeId = null;
    public String workerCapacity = "Compute Core Node";
    public boolean workerGpuEnabled = false;
    public boolean haloExchangeEnabled = false;
    public boolean snapshotsEnabled = false;
    public int snapshotIntervalTicks = 0;
    public String snapshotDir = "saves/cluster_snapshots";
    public int maxSnapshots = 12;

    public Boolean cliSparseCellSkipping = null;
    public Boolean cliOceanMacroAggregation = null;
    public Boolean cliCoastalNavigationOnly = null;
    public Boolean cliOceanMultiRateTicking = null;
    public Integer cliClimateTickFrequency = null;
    public Boolean cliParallelExecution = null;
    public Boolean cliSpatialRangeTruncation = null;

    public String scenarioName = "OUT_OF_AFRICA";
    public boolean ticksExplicitlySet = false;
    public Long overrideStartYear = null;
    public Long overrideEndYear = null;
    public Double overrideDt = null;
    public Long overrideInitialPop = null;
    public Integer overrideInitialTech = null;
    public Long randomSeed = null;

    public boolean saveEnabled = true;
    public String customSaveName = null;

    /**
     * Parses CLI arguments into a structured {@link HeadlessCliOptions} instance.
     *
     * @param args      command-line tokens
     * @param i18nPrefs language preference node
     * @param themePrefs theme preference node
     * @return parsed CLI options
     */
    public static HeadlessCliOptions parse(String[] args, Preferences i18nPrefs, Preferences themePrefs) {
        HeadlessCliOptions o = new HeadlessCliOptions();
        if (args == null) return o;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i].trim();
            String a = arg.toLowerCase();

            if (a.equals("--help") || a.equals("-help") || a.equals("-?") || a.equals("-h")) {
                o.earlyAction = EarlyAction.HELP;
                return o;
            } else if (a.equals("--benchmark") || a.equals("-b") || a.equals("--bench") || a.equals("--benchmark-real")) {
                o.earlyAction = EarlyAction.BENCHMARK_REAL;
                return o;
            } else if (a.equals("--benchmark-dod") || a.equals("--benchmark-micro")) {
                o.earlyAction = EarlyAction.BENCHMARK_DOD;
                return o;
            } else if (a.startsWith("--engine=")) {
                o.engineMode = parseEngineMode(a.substring("--engine=".length()));
            } else if ((a.equals("-e") || a.equals("--engine")) && i + 1 < args.length) {
                o.engineMode = parseEngineMode(args[++i].toLowerCase());
            } else if (a.equals("--rust") || a.equals("--native")) {
                o.engineMode = HeadlessRunner.CliEngineMode.RUST_NATIVE;
            } else if (a.equals("--gpu") || a.equals("--opencl") || a.equals("--gpu-on")) {
                logger.warn("GPU Compute Shaders (OpenCL) are not available in headless CLI mode; falling back to Java SIMD Vector acceleration.");
                o.engineMode = HeadlessRunner.CliEngineMode.JAVA_VECTOR_SIMD;
                o.workerGpuEnabled = false;
            } else if (a.equals("--no-gpu") || a.equals("--gpu-off")) {
                o.workerGpuEnabled = false;
            } else if (a.equals("--simd") || a.equals("--vector")) {
                o.engineMode = HeadlessRunner.CliEngineMode.JAVA_VECTOR_SIMD;
            } else if (a.equals("--cpu") || a.equals("--cpu-jit") || a.equals("--jit")) {
                o.engineMode = HeadlessRunner.CliEngineMode.CPU_JIT;
            } else if (a.equals("--safe") || a.equals("--fallback")) {
                o.engineMode = HeadlessRunner.CliEngineMode.SAFE_FALLBACK;
                o.isSingleCore = true;
                o.threadCount = 1;
            } else if (a.equals("--single-core") || a.equals("--monocoeur") || a.equals("--single-threaded")) {
                o.isSingleCore = true;
                o.threadCount = 1;
            } else if (a.equals("--multi-core") || a.equals("--multicoeur")) {
                o.isSingleCore = false;
                o.threadCount = Runtime.getRuntime().availableProcessors();
            } else if (a.startsWith("--threads=") || a.startsWith("--cores=")) {
                o.threadCount = Integer.parseInt(a.substring(a.indexOf('=') + 1));
                if (o.threadCount <= 1) o.isSingleCore = true;
            } else if ((a.equals("--threads") || a.equals("--cores")) && i + 1 < args.length) {
                o.threadCount = Integer.parseInt(args[++i]);
                if (o.threadCount <= 1) o.isSingleCore = true;
            } else if (a.equals("--mode=cluster") || a.equals("--cluster")) {
                o.isClusterMode = true;
            } else if (a.equals("--mode=local") || a.equals("--local")) {
                o.isClusterMode = false;
            } else if (a.equals("--role=worker") || a.equals("--worker") || a.equals("--node")) {
                o.clusterRole = ClusterManager.ClusterRole.WORKER;
                o.isClusterMode = true;
            } else if (a.equals("--role=master") || a.equals("--master") || a.equals("--server")) {
                o.clusterRole = ClusterManager.ClusterRole.MASTER;
                o.isClusterMode = true;
            } else if (a.startsWith("--node-id=") || a.startsWith("--worker-id=") || a.startsWith("--id=")) {
                o.workerNodeId = arg.substring(arg.indexOf('=') + 1).trim();
            } else if (a.startsWith("--worker-capacity=") || a.startsWith("--capacity=")) {
                o.workerCapacity = arg.substring(arg.indexOf('=') + 1).trim();
            } else if (a.startsWith("--master-host=") || a.startsWith("--host=") || a.startsWith("--server-host=")) {
                o.masterHost = arg.substring(arg.indexOf('=') + 1).trim();
            } else if ((a.equals("-host") || a.equals("--host")) && i + 1 < args.length) {
                o.masterHost = args[++i].trim();
            } else if (a.startsWith("--port=")) {
                o.port = Integer.parseInt(a.substring("--port=".length()));
            } else if ((a.equals("-p") || a.equals("-port") || a.equals("--port"))
                    && i + 1 < args.length && !args[i + 1].startsWith("-") && args[i + 1].matches("\\d+")) {
                o.port = Integer.parseInt(args[++i]);
            } else if (a.startsWith("--secret=") || a.startsWith("--auth-token=")) {
                o.secretToken = arg.substring(arg.indexOf('=') + 1).trim();
            } else if (a.startsWith("--sync-interval=")) {
                o.syncInterval = Integer.parseInt(a.substring("--sync-interval=".length()));
            } else if (a.startsWith("--barrier-timeout=")) {
                o.barrierTimeoutMs = Long.parseLong(a.substring("--barrier-timeout=".length()));
            } else if (a.startsWith("--heartbeat-interval=")) {
                o.heartbeatIntervalSec = Integer.parseInt(a.substring("--heartbeat-interval=".length()));
            } else if (a.startsWith("--heartbeat-timeout=")) {
                o.heartbeatTimeoutSec = Integer.parseInt(a.substring("--heartbeat-timeout=".length()));
            } else if (a.startsWith("--partition-strategy=") || a.startsWith("--split-strategy=")) {
                String stratStr = a.substring(a.indexOf('=') + 1).toUpperCase();
                if (stratStr.contains("LOAD") || stratStr.contains("WEIGHT") || stratStr.contains("DYN")) {
                    o.partitionStrategy = ClusterManager.PartitionStrategy.LOAD_AWARE;
                } else if (stratStr.contains("SLICE") || stratStr.contains("EQUAL") || stratStr.contains("LAT")) {
                    o.partitionStrategy = ClusterManager.PartitionStrategy.EQUAL_SLICES;
                } else {
                    o.partitionStrategy = ClusterManager.PartitionStrategy.HILBERT;
                }
            } else if (a.startsWith("--halo-exchange")) {
                o.haloExchangeEnabled = !a.endsWith("=false");
            } else if (a.startsWith("--snapshots")) {
                o.snapshotsEnabled = !a.endsWith("=false");
            } else if (a.startsWith("--snapshot-interval=")) {
                o.snapshotIntervalTicks = Integer.parseInt(a.substring("--snapshot-interval=".length()));
                o.snapshotsEnabled = o.snapshotIntervalTicks > 0;
            } else if (a.startsWith("--snapshot-dir=")) {
                o.snapshotDir = arg.substring(arg.indexOf('=') + 1).trim();
            } else if (a.startsWith("--max-snapshots=")) {
                o.maxSnapshots = Integer.parseInt(a.substring("--max-snapshots=".length()));
            } else if (a.equals("--worker-gpu")) {
                o.workerGpuEnabled = true;
            } else if (a.equals("--worker-rust")) {
                o.engineMode = HeadlessRunner.CliEngineMode.RUST_NATIVE;
            } else if (a.equals("--worker-simd")) {
                o.engineMode = HeadlessRunner.CliEngineMode.JAVA_VECTOR_SIMD;
            } else if (a.equals("--worker-cpu")) {
                o.engineMode = HeadlessRunner.CliEngineMode.CPU_JIT;
            } else if (a.startsWith("--strict-determinism") || a.equals("--no-determinism") || a.equals("--fast")) {
                logger.info("Determinism settings are governed directly by scenario preset configuration.");
            } else if (a.startsWith("--sparse-skipping") || a.startsWith("--sparse-cells")) {
                o.cliSparseCellSkipping = !a.endsWith("=false");
            } else if (a.equals("--no-sparse-skipping")) {
                o.cliSparseCellSkipping = false;
            } else if (a.startsWith("--ocean-macro-aggregation") || a.startsWith("--macro-aggregation") || a.startsWith("--macro-ocean")) {
                o.cliOceanMacroAggregation = !a.endsWith("=false");
            } else if (a.equals("--no-ocean-macro-aggregation")) {
                o.cliOceanMacroAggregation = false;
            } else if (a.startsWith("--coastal-nav-only") || a.startsWith("--coastal-navigation")) {
                o.cliCoastalNavigationOnly = !a.endsWith("=false");
            } else if (a.equals("--no-coastal-nav")) {
                o.cliCoastalNavigationOnly = false;
            } else if (a.startsWith("--ocean-multi-rate") || a.startsWith("--multi-rate-climate") || a.startsWith("--multi-freq-climate")) {
                o.cliOceanMultiRateTicking = !a.endsWith("=false");
            } else if (a.equals("--no-ocean-multi-rate")) {
                o.cliOceanMultiRateTicking = false;
            } else if (a.startsWith("--climate-freq=")) {
                o.cliClimateTickFrequency = Integer.parseInt(a.substring("--climate-freq=".length()));
                o.cliOceanMultiRateTicking = true;
            } else if (a.startsWith("--parallel") || a.startsWith("--async-threads")) {
                o.cliParallelExecution = !a.endsWith("=false");
            } else if (a.equals("--no-parallel")) {
                o.cliParallelExecution = false;
            } else if (a.startsWith("--spatial-truncation")) {
                o.cliSpatialRangeTruncation = !a.endsWith("=false");
            } else if (a.equals("--no-spatial-truncation")) {
                o.cliSpatialRangeTruncation = false;
            } else if (a.equals("--windowed") || a.equals("--clipped") || a.startsWith("--windowed=")
                    || a.equals("--fullscreen") || a.equals("--global") || a.equals("--no-windowed")) {
                logger.info("Spatial windowing and bounding boxes are governed directly by scenario preset definition.");
            } else if (a.startsWith("--scenario=")) {
                o.scenarioName = arg.substring("--scenario=".length()).trim();
            } else if ((a.equals("-s") || a.equals("--scenario")) && i + 1 < args.length) {
                o.scenarioName = args[++i].trim();
            } else if (a.startsWith("--ticks=")) {
                o.ticksToRun = Integer.parseInt(a.substring("--ticks=".length()));
                o.ticksExplicitlySet = true;
            } else if ((a.equals("-t") || a.equals("--ticks")) && i + 1 < args.length) {
                o.ticksToRun = Integer.parseInt(args[++i]);
                o.ticksExplicitlySet = true;
            } else if (a.startsWith("--cells=") || a.equals("-c") || a.equals("--cells")) {
                logger.warn("Arbitrary cell count subsampling (--cells) is unsupported; use --res=<level> instead.");
                if (!a.contains("=") && i + 1 < args.length && !args[i + 1].startsWith("-")) i++;
            } else if (a.startsWith("--res=") || a.startsWith("--resolution=") || a.equals("-r") || a.equals("--res") || a.equals("--resolution")) {
                logger.info("Spatial resolution is governed directly by scenario preset definition; CLI override ignored.");
                if (!a.contains("=") && i + 1 < args.length && !args[i + 1].startsWith("-")) i++;
            } else if (a.startsWith("--start-year=")) {
                o.overrideStartYear = Long.parseLong(a.substring("--start-year=".length()));
            } else if (a.startsWith("--end-year=")) {
                o.overrideEndYear = Long.parseLong(a.substring("--end-year=".length()));
            } else if (a.startsWith("--dt=") || a.startsWith("--temporal-res=")) {
                o.overrideDt = Double.parseDouble(a.substring(a.indexOf('=') + 1));
            } else if (a.startsWith("--initial-pop=") || a.startsWith("--pop=")) {
                o.overrideInitialPop = Long.parseLong(a.substring(a.indexOf('=') + 1));
            } else if (a.startsWith("--initial-tech=") || a.startsWith("--tech=")) {
                o.overrideInitialTech = Integer.parseInt(a.substring(a.indexOf('=') + 1));
            } else if (a.startsWith("--seed=")) {
                o.randomSeed = Long.parseLong(a.substring("--seed=".length()));
            } else if (a.startsWith("--scenario-suffix=") || a.equals("--scenario-suffix")) {
                if (!a.contains("=") && i + 1 < args.length && !args[i + 1].startsWith("-")) i++;
            } else if (a.startsWith("--save=")) {
                o.customSaveName = arg.substring("--save=".length()).trim();
                o.saveEnabled = true;
            } else if (a.equals("--save") && i + 1 < args.length && !args[i + 1].startsWith("-")) {
                o.customSaveName = args[++i].trim();
                o.saveEnabled = true;
            } else if (a.equals("--no-save")) {
                o.saveEnabled = false;
            } else if (a.equals("--profile") || a.equals("-p")) {
                o.showProfile = true;
            } else if (a.equals("--no-profile")) {
                o.showProfile = false;
            } else if (a.startsWith("--lang=") || a.startsWith("--language=")) {
                if (i18nPrefs != null) i18nPrefs.put("ether_language", a.substring(a.indexOf('=') + 1));
            } else if ((a.equals("-l") || a.equals("--lang") || a.equals("--language")) && i + 1 < args.length) {
                if (i18nPrefs != null) i18nPrefs.put("ether_language", args[++i].trim().toLowerCase());
            } else if (a.startsWith("--theme=")) {
                if (themePrefs != null) themePrefs.put("ether_theme", a.substring(a.indexOf('=') + 1).toUpperCase());
            }
        }
        return o;
    }

    /**
     * Resolves a backend mode token into {@link HeadlessRunner.CliEngineMode}.
     *
     * @param str raw mode token
     * @return resolved CLI engine mode
     */
    public static HeadlessRunner.CliEngineMode parseEngineMode(String str) {
        if (str.contains("rust") || str.contains("native")) return HeadlessRunner.CliEngineMode.RUST_NATIVE;
        if (str.contains("simd") || str.contains("vector")) return HeadlessRunner.CliEngineMode.JAVA_VECTOR_SIMD;
        if (str.contains("safe") || str.contains("sw") || str.contains("fallback")) return HeadlessRunner.CliEngineMode.SAFE_FALLBACK;
        if (str.contains("gpu") || str.contains("shader") || str.contains("opencl")) {
            logger.warn("GPU Compute Shaders (OpenCL) are not available in headless CLI mode; falling back to Java SIMD Vector acceleration.");
            return HeadlessRunner.CliEngineMode.JAVA_VECTOR_SIMD;
        }
        return HeadlessRunner.CliEngineMode.CPU_JIT;
    }

    /** Prints the formatted CLI manual to standard output. */
    public static void printHelp() {
        System.out.println("Usage: java -jar society-simulation.jar [options]");
        System.out.println("\n[Tab 4] Compute Engine & Hardware Acceleration Options:");
        System.out.println("  --engine=<rust|gpu|simd|cpu|safe>, -e <type>  Select compute backend (Default: rust/simd)");
        System.out.println("  --rust, --native                              Force Native Rust Multi-Core Engine (Rayon + AVX-512)");
        System.out.println("  --gpu, --opencl, --gpu-on                     Force OpenCL GPU Compute Shaders Pipeline");
        System.out.println("  --no-gpu, --gpu-off                           Disable GPU hardware offloading");
        System.out.println("  --simd, --vector                              Force Java 21 Incubator Vector SIMD Engine");
        System.out.println("  --cpu, --cpu-jit                              Force Pure CPU Java JIT Engine");
        System.out.println("  --safe, --fallback                            Force Software Safe Fallback (Single-Thread SW)");
        System.out.println("\n[Tab 4] Threading & Core Allocation Options:");
        System.out.println("  --single-core, --monocoeur                    Run in single-threaded / single-core mode");
        System.out.println("  --multi-core, --multicoeur                    Run in parallel multi-core mode (all available CPUs)");
        System.out.println("  --threads=<N>, --cores=<N>                    Explicitly allocate N CPU worker threads");
        System.out.println("\n[Tab 4] Distributed Multi-Node Clustering Options:");
        System.out.println("  --mode=<local|cluster>, --cluster             Select local standalone or distributed cluster");
        System.out.println("  --role=<master|worker>, --master, --worker    Node role in cluster mode (Default: master)");
        System.out.println("  --server, --node                              Aliases for --master and --worker roles");
        System.out.println("  --node-id=<ID>, --worker-id=<ID>              Unique identifier name for this node/worker");
        System.out.println("  --worker-capacity=<DESC>                      Descriptive hardware capacity label for worker");
        System.out.println("  --master-host=<IP>, --host=<IP>               Master IP/hostname (Default: 127.0.0.1)");
        System.out.println("  --port=<PORT>, -P <PORT>                      Cluster communication port (Default: 9090)");
        System.out.println("  --secret=<TOKEN>, --auth-token=<TOKEN>        Cluster AES-256 handshake authentication token");
        System.out.println("  --partition-strategy=<HILBERT|LOAD_AWARE|EQUAL_SLICES>  Spatial partition algorithm");
        System.out.println("  --sync-interval=<N>                           State sync frequency across cluster nodes in ticks");
        System.out.println("  --barrier-timeout=<MS>                        Lock-step barrier wait timeout in milliseconds");
        System.out.println("  --heartbeat-interval=<SEC>                    Worker heartbeat ping interval in seconds");
        System.out.println("  --heartbeat-timeout=<SEC>                     Master heartbeat timeout before node drop");
        System.out.println("  --snapshots / --snapshot-interval=<N> / --snapshot-dir=<PATH>");
        System.out.println("  --worker-gpu, --worker-rust, --worker-simd    Acceleration backend on worker nodes");
        System.out.println("\n[Tab 3] Scenario Determinism & Approximation Options:");
        System.out.println("  --sparse-skipping[=true|false], --sparse-cells");
        System.out.println("  --ocean-macro-aggregation[=true|false]");
        System.out.println("  --coastal-nav-only[=true|false]");
        System.out.println("  --ocean-multi-rate[=true|false] / --climate-freq=<N>");
        System.out.println("  --parallel[=true|false] / --spatial-truncation[=true|false]");
        System.out.println("\nScenario, Spatial & Chronological Options:");
        System.out.println("  --scenario=<NAME>, -s <NAME>                  Scenario preset");
        System.out.println("  --ticks=<N>, -t <N>                           Target number of simulation ticks");
        System.out.println("  --start-year=<YEAR> / --end-year=<YEAR> / --dt=<DAYS>");
        System.out.println("  --initial-pop=<N> / --initial-tech=<N> / --seed=<LONG>");
        System.out.println("\nPersistence, Profiling & Global Preferences:");
        System.out.println("  --save=<NAME> / --no-save / --profile / --no-profile");
        System.out.println("  --lang=<EN|FR|DE|ES|ZH>, -l <LANG> / --theme=<dark|light|presentation>");
        System.out.println("  --help, -h, -?                                Display this help manual");
    }
}
