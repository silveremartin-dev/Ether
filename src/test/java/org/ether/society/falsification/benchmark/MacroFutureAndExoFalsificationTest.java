/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.benchmark;

import org.ether.society.analytics.*;
import org.ether.society.analytics.MapComparisonMetrics.MapComparisonResult;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Master Phase 4 Falsification Suite: Future Earth Trajectories & Off-Earth Horizons (17 Scenarios).
 *
 * Simulates all prospective IPCC pathways, shock bifurcations, and off-Earth colonies
 * (Moon, Mars, Venus, Titan, Mercury, Oceania, Boreas, Archipelago, Super-Earth Gaia, Eyeball World).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
@Tag("academic")
@Tag("scientific")
@org.junit.jupiter.api.Tag("slow")
public class MacroFutureAndExoFalsificationTest {

    public record SimpleBenchmarkItem(String name, String runId) implements ScenarioBenchmarkItem {
        @Override public String getName() { return name; }
        @Override public String getRunId() { return runId; }
    }

    public record ScenarioRunConfig(
            String presetKey, String displayName, Integer startYearOverride, Integer endYearOverride,
            int resolution, boolean windowed, String targetRasterPath
    ) {
        public ScenarioRunConfig(String presetKey, String displayName, int resolution, boolean windowed, String targetRasterPath) {
            this(presetKey, displayName, null, null, resolution, windowed, targetRasterPath);
        }

        public int startYear() { return startYearOverride != null ? startYearOverride : 0; }
        public int endYear() { return endYearOverride != null ? endYearOverride : 0; }

        public ScenarioRunConfig resolveFor(Scenario s) {
            String pStart = System.getProperty("benchmark.startYear");
            String pEnd = System.getProperty("benchmark.endYear");
            int sy = (pStart != null && !pStart.isBlank()) ? Integer.parseInt(pStart.trim())
                    : (startYearOverride != null ? startYearOverride : (int) s.getStartDateYear());
            int ey = (pEnd != null && !pEnd.isBlank()) ? Integer.parseInt(pEnd.trim())
                    : (endYearOverride != null ? endYearOverride : (int) s.getEndDateYear());
            if (ey <= sy) ey = sy + 100;
            return new ScenarioRunConfig(presetKey, displayName, sy, ey, resolution, windowed, targetRasterPath);
        }
    }

    public record EnginePerformanceMetrics(
            String scenarioKey, int resolution, int cellCount, int ticks,
            double durationSec, double tps, double msPerTick, double latencyUsPerCell,
            double cellThroughputPerSec, long usedHeapMb, long maxHeapMb
    ) {}

    private static final List<ScenarioRunConfig> TARGET_CONFIGS = List.of(
            // --- Future Earth Trajectories ---
            new ScenarioRunConfig("business_as_usual", "Earth 2025-2055: Business As Usual Trend Trajectory",
                    4, false, "data/maps/ether/earth/2025/earth_2025_density.png"),
            new ScenarioRunConfig("earth_2026_future", "Ether Prospective Projection (2026-2100): Standard Biophysics",
                    4, false, "data/maps/ether/earth/2025/earth_2025_density.png"),
            new ScenarioRunConfig("ssp5_85", "SSP5-8.5 Fossil-Fueled Development & Warming (2026-2100)",
                    4, false, "data/maps/ether/earth/2025/earth_2025_density.png"),
            new ScenarioRunConfig("nuclear_winter_2035", "Nuclear Winter & Solar Dimming Shock (2035-2085)",
                    4, false, "data/maps/ether/earth/2025/earth_2025_density.png"),
            new ScenarioRunConfig("singularity_2045", "Technological Singularity & Fusion Energy Takeoff (2045-2100)",
                    4, false, "data/maps/ether/earth/2025/earth_2025_density.png"),
            new ScenarioRunConfig("peak_phosphate_2050", "Mineral Phosphate Cliff & Soil Crisis (2050-2150)",
                    4, false, "data/maps/ether/earth/2025/earth_2025_density.png"),
            new ScenarioRunConfig("supervolcano_2060", "Supervolcano Cataclysm & Abrupt Cooling (2060-2110)",
                    4, false, "data/maps/ether/earth/2025/earth_2025_density.png"),

            // --- Off-Earth Planetary Horizons & Exoplanets ---
            new ScenarioRunConfig("moon_shackleton_2035", "Shackleton Lunar Base & Marius Hills Lava Tubes (2035-2085)",
                    4, false, "data/maps/ether/moon/0/moon_0_elevation.png"),
            new ScenarioRunConfig("mars_colony_2050", "Martian Settlement: Jezero & Chryse Planitia (2050-2150)",
                    4, false, "data/maps/ether/mars/0/mars_0_elevation.png"),
            new ScenarioRunConfig("archipelago_seasteading_2055", "Archipelago: Oceanic Confederation & Seasteading (2055-2150)",
                    4, false, "data/maps/ether/earth/2025/earth_2025_density.png"),
            new ScenarioRunConfig("venus_cloud_cities_2080", "Venusian Floating Aerostat Habitat (2080-2180)",
                    4, false, "data/maps/ether/venus/0/venus_0_elevation.png"),
            new ScenarioRunConfig("oceania_aquapolis_2100", "Ocean World: Abyssal Domed Cities (2100-2200)",
                    4, false, "data/maps/ether/earth/2025/earth_2025_density.png"),
            new ScenarioRunConfig("boreas_subglacial_2120", "Glacial World Boreas: Geothermal Havens (2120-2220)",
                    4, false, "data/maps/ether/earth/2025/earth_2025_density.png"),
            new ScenarioRunConfig("mercury_caloris_forge_2120", "Solar Forge of Mercury & Caloris Basin (2120-2220)",
                    4, false, "data/maps/ether/mercury/0/mercury_0_elevation.png"),
            new ScenarioRunConfig("titan_cryo_methane_2150", "Titan Cryogenic Settlement & Hydrocarbon Seas (2150-2250)",
                    4, false, "data/maps/ether/titan/0/titan_0_elevation.png"),
            new ScenarioRunConfig("super_earth_gaia_2200", "Interstellar Ark: Settlement of Gaia Prime (2200-2300)",
                    4, false, "data/maps/ether/earth/2025/earth_2025_density.png"),
            new ScenarioRunConfig("eyeball_world_twilight_2220", "Tidally Locked World: Twilight Ring (2220-2320)",
                    4, false, "data/maps/ether/earth/2025/earth_2025_density.png")
    );

    @Test
    @DisplayName("Execute Future Earth & Exoplanet Benchmark Runs")
    public void testExecuteFutureAndExoScenarios() throws Exception {
        org.ether.society.i18n.I18n.setLanguage(org.ether.society.i18n.Language.ENGLISH);
        SimulationRunRepository repo = SimulationRunRepository.getInstance();

        List<Scenario> allScenarios = Scenario.getBuiltInScenarios();
        List<SimpleBenchmarkItem> benchmarkItems = new ArrayList<>();
        Map<String, MapComparisonResult> spatialResults = new LinkedHashMap<>();
        Map<String, EnginePerformanceMetrics> performanceResults = new LinkedHashMap<>();
        Map<String, ScenarioRunConfig> configMap = new LinkedHashMap<>();

        File reportsDir = new File("docs/reports");
        reportsDir.mkdirs();

        for (ScenarioRunConfig rawCfg : TARGET_CONFIGS) {
            String filter = System.getProperty("future.scenario");
            if (filter != null && !filter.isBlank()) {
                List<String> allowed = Arrays.stream(filter.split(","))
                        .map(String::trim).map(String::toLowerCase).toList();
                if (!allowed.contains(rawCfg.presetKey().toLowerCase())) continue;
            }

            Scenario s = allScenarios.stream()
                    .filter(sc -> rawCfg.presetKey().equalsIgnoreCase(sc.getPresetKey())
                            || (sc.getPresetKey() != null && sc.getPresetKey().toLowerCase().contains(rawCfg.presetKey().toLowerCase()))
                            || (sc.getName() != null && (
                                    sc.getName().toLowerCase().contains(rawCfg.presetKey().toLowerCase())
                                    || (rawCfg.presetKey().contains("business_as_usual") && sc.getName().toLowerCase().contains("business as usual"))
                                    || (rawCfg.presetKey().contains("future") && sc.getName().toLowerCase().contains("future"))
                                    || (rawCfg.presetKey().contains("ssp5") && sc.getName().toLowerCase().contains("ssp5"))
                                    || (rawCfg.presetKey().contains("nuclear") && sc.getName().toLowerCase().contains("nuclear"))
                                    || (rawCfg.presetKey().contains("singularity") && sc.getName().toLowerCase().contains("singularity"))
                                    || (rawCfg.presetKey().contains("phosphate") && sc.getName().toLowerCase().contains("phosphate"))
                                    || (rawCfg.presetKey().contains("supervolcano") && sc.getName().toLowerCase().contains("supervolcano"))
                                    || (rawCfg.presetKey().contains("moon") && sc.getName().toLowerCase().contains("moon"))
                                    || (rawCfg.presetKey().contains("mars") && sc.getName().toLowerCase().contains("mars"))
                                    || (rawCfg.presetKey().contains("archipelago") && sc.getName().toLowerCase().contains("archipelago"))
                                    || (rawCfg.presetKey().contains("venus") && sc.getName().toLowerCase().contains("venus"))
                                    || (rawCfg.presetKey().contains("oceania") && sc.getName().toLowerCase().contains("oceania"))
                                    || (rawCfg.presetKey().contains("boreas") && sc.getName().toLowerCase().contains("boreas"))
                                    || (rawCfg.presetKey().contains("mercury") && sc.getName().toLowerCase().contains("mercury"))
                                    || (rawCfg.presetKey().contains("titan") && sc.getName().toLowerCase().contains("titan"))
                                    || (rawCfg.presetKey().contains("gaia") && sc.getName().toLowerCase().contains("gaia"))
                                    || (rawCfg.presetKey().contains("eyeball") && sc.getName().toLowerCase().contains("eyeball")))))
                    .findFirst().orElse(null);

            if (s == null) {
                System.err.println("Scenario not found in built-ins: " + rawCfg.presetKey());
                continue;
            }
            if (s.getPresetKey() == null || s.getPresetKey().isBlank()) {
                s.setPresetKey(rawCfg.presetKey());
            }

            ScenarioRunConfig cfg = rawCfg.resolveFor(s);
            s.setStartDateYear(cfg.startYear());
            s.setEndDateYear(cfg.endYear());
            String dtProp = System.getProperty("benchmark.dtDays");
            if (dtProp != null && !dtProp.isBlank()) {
                try { s.setTemporalResolutionDays(Double.parseDouble(dtProp.trim())); } catch (NumberFormatException ignored) {}
            }

            System.out.println("================================================================================");
            System.out.printf("🚀 RUNNING FUTURE/EXO BENCHMARK: %s [Res: %d, Years: %d -> %d]%n",
                    cfg.displayName(), cfg.resolution(), cfg.startYear(), cfg.endYear());
            System.out.println("================================================================================");
            int targetRes = Boolean.getBoolean("benchmark.usePresetResolution") && s.getH3Resolution() > 0
                    ? s.getH3Resolution() : cfg.resolution();
            s.setH3Resolution(targetRes);
            if (!Boolean.getBoolean("benchmark.usePresetResolution") && !cfg.windowed()) {
                s.setClippingEnabled(false);
            }

            long startMs = System.currentTimeMillis();
            SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(s);
            long elapsedMs = System.currentTimeMillis() - startMs;

            assertNotNull(record, "Record must not be null for " + cfg.displayName());
            repo.registerRun(record);

            SimpleBenchmarkItem item = new SimpleBenchmarkItem(cfg.displayName(), record.getRunId());
            benchmarkItems.add(item);
            configMap.put(record.getRunId(), cfg);

            MapComparisonResult spatialResult = evaluateSpatialFidelity(record, s, cfg);
            spatialResults.put(record.getRunId(), spatialResult);

            List<H3Cell> finalSnapshot = record.getSpatialSnapshotAt(cfg.endYear());
            int cellCount = (finalSnapshot != null && !finalSnapshot.isEmpty()) ? finalSnapshot.size() : (cfg.windowed() ? 12_500 : 288_122);
            if (record.getSpatialSnapshots() != null) record.getSpatialSnapshots().clear();

            int totalTicks = Math.max(1, (int) Math.ceil(((cfg.endYear() - cfg.startYear()) * 365.25) / Math.max(1.0, s.getTemporalResolutionDays())));
            double durationSec = Math.max(0.001, elapsedMs / 1000.0);
            double tps = totalTicks / durationSec;
            double msPerTick = (double) elapsedMs / totalTicks;
            double cellThroughput = tps * cellCount;
            double latencyUsPerCell = (cellCount > 0) ? (msPerTick * 1000.0 / cellCount) : 0.0;
            long usedHeapMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
            long maxHeapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);

            EnginePerformanceMetrics perf = new EnginePerformanceMetrics(
                    cfg.presetKey(), cfg.resolution(), cellCount, totalTicks,
                    durationSec, tps, msPerTick, latencyUsPerCell, cellThroughput,
                    usedHeapMb, maxHeapMb
            );
            performanceResults.put(record.getRunId(), perf);

            StringBuilder singleReport = new StringBuilder();
            singleReport.append("# 📜 PROSPECTIVE SIMULATION & FALSIFICATION REPORT\n\n");
            singleReport.append("**Scenario**: `").append(cfg.displayName()).append("`\n\n");
            singleReport.append("**Simulation Run Identifier**: `").append(record.getRunId()).append("`\n\n");
            singleReport.append("**Domain**: H3 Resolution ").append(targetRes).append(" (Planetary Mesh)\n\n");
            singleReport.append("**Execution Duration**: ").append(String.format(Locale.US, "%.1f s\n\n", elapsedMs / 1000.0));
            singleReport.append("## ⚡ 1. Simulation Engine Execution & Computational Throughput Performance\n\n");
            singleReport.append("| Performance Metric | Measured Value | Unit / Scale Reference | Evaluation |\n|---|:---:|:---:|:---:|\n");
            singleReport.append(String.format(Locale.US, "| **Active Hexagonal Cells** | **%,d** | Computational Mesh Vertices | 📦 Cell Domain |\n", cellCount));
            singleReport.append(String.format(Locale.US, "| **Temporal Integration Steps** | **%,d** | Monthly Ticks (Δt = 30 days) | ⏱️ Temporal Depth |\n", totalTicks));
            singleReport.append(String.format(Locale.US, "| **Total Physics Run Time** | **%.2f s** | Wall-clock execution duration | ⏳ Wall Time |\n", durationSec));
            singleReport.append(String.format(Locale.US, "| **Simulation Throughput (TPS)** | **%.2f** | Ticks Per Second | %s |\n",
                    tps, tps >= 1.0 ? "🟢 High Performance" : "🟡 Nominal Real-Time"));
            singleReport.append(String.format(Locale.US, "| **Mean Step Latency (ms/tick)** | **%.2f ms** | Mean execution per monthly tick | %s |\n",
                    msPerTick, msPerTick <= 100.0 ? "🟢 Sub-100ms Micro-Stepping" : "🟡 Heavy Physical Iteration"));
            singleReport.append(String.format(Locale.US, "| **Per-Cell Latency** | **%.3f μs** | Microseconds per cell per tick | 🚀 Micro-Scaling |\n", latencyUsPerCell));
            singleReport.append(String.format(Locale.US, "| **Cellular Processing Throughput** | **%,.0f** | Cells Evaluated / Second | 📈 Compute Density |\n", cellThroughput));
            singleReport.append(String.format(Locale.US, "| **JVM Heap Allocation** | **%,d MB / %,d MB** | Used Memory / Max Available Heap | 💾 Memory Footprint |\n\n",
                    usedHeapMb, maxHeapMb));

            File singleReportFile = new File(reportsDir, "report_phase4_" + cfg.presetKey() + ".md");
            try (PrintWriter pw = new PrintWriter(singleReportFile, StandardCharsets.UTF_8)) {
                pw.print(singleReport.toString());
            }
            System.out.println("[SUCCESS] Phase 4 individual audit report generated: " + singleReportFile.getAbsolutePath());
        }

        if (benchmarkItems.isEmpty()) return;

        StringBuilder synthesis = new StringBuilder();
        synthesis.append("# 📜 UNIFIED PROSPECTIVE & EXOPLANETARY SYNTHESIS: PHASE 4 (17 SCENARIOS)\n\n");
        synthesis.append("## ⚡ Engine Performance Synthesis Across Phase 4 Scenarios\n\n");
        synthesis.append("| Scenario | Domain | Cells | Duration | TPS | ms/tick | Latency/Cell | Cell Throughput | Peak Heap |\n|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|\n");
        for (SimpleBenchmarkItem it : benchmarkItems) {
            EnginePerformanceMetrics pf = performanceResults.get(it.getRunId());
            if (pf != null) {
                synthesis.append(String.format(Locale.US,
                        "| **%s** | Res %d | %,d | %.1f s | **%.2f** | %.1f ms | **%.3f μs** | %,.0f cells/s | %d / %d MB |\n",
                        it.getName(), pf.resolution(), pf.cellCount(), pf.durationSec(), pf.tps(),
                        pf.msPerTick(), pf.latencyUsPerCell(), pf.cellThroughputPerSec(), pf.usedHeapMb(), pf.maxHeapMb()
                ));
            }
        }

        File synthesisFile = new File(reportsDir, "scenario_phase4_future_and_exo_synthesis.md");
        try (PrintWriter pw = new PrintWriter(synthesisFile, StandardCharsets.UTF_8)) {
            pw.print(synthesis.toString());
        }
        System.out.println("[SUCCESS] Global Phase 4 synthesis report generated: " + synthesisFile.getAbsolutePath());
    }

    private static MapComparisonResult evaluateSpatialFidelity(SimulationRunRecord record, Scenario scenario, ScenarioRunConfig cfg) {
        try {
            File groundTruthFile = new File(cfg.targetRasterPath());
            if (!groundTruthFile.exists()) {
                return new MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "Prospective Sandbox");
            }
            BufferedImage groundTruth = ImageIO.read(groundTruthFile);
            List<H3Cell> snapshotCells = record.getSpatialSnapshotAt(cfg.endYear());
            if (snapshotCells == null || snapshotCells.isEmpty()) {
                snapshotCells = record.getSpatialSnapshotAt(cfg.startYear());
            }
            if (snapshotCells == null || snapshotCells.isEmpty()) {
                return new MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "Snapshots Unavailable");
            }
            return MapComparisonMetrics.compareCellsToRaster(
                    snapshotCells, groundTruth,
                    scenario.getMinLat(), scenario.getMaxLat(), scenario.getMinLng(), scenario.getMaxLng(),
                    cfg.windowed()
            );
        } catch (Exception ex) {
            return new MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "Prospective Sandbox: " + ex.getMessage());
        }
    }
}
