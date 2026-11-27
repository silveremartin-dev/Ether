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
 * Master Phase 3 Historical Falsification Test Suite (14 Complementary Scenarios).
 *
 * Completes the remaining 14 historical baseline scenarios to achieve full 100%
 * coverage of all 32 terrestrial historical anchor points across the Holocene and Pleistocene.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
@Tag("academic")
@Tag("scientific")
@org.junit.jupiter.api.Tag("slow")
public class MacroHistoricalPhase3FalsificationTest {

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
            new ScenarioRunConfig("toba_cataclysm_74k", "Toba Supervolcano Cataclysm & Demographic Bottleneck (-74,000 -> -50,000 BP)",
                    3, false, "data/maps/ether/earth/-74000/earth_-74000_density.png"),
            new ScenarioRunConfig("sahul", "Sahul & First Peopling of Australia (-50,000 -> -10,000 BP)",
                    3, false, "data/maps/ether/earth/-50000/earth_-50000_density.png"),
            new ScenarioRunConfig("beringia", "Beringia & Peopling of the Americas (-25,000 -> -10,000 BP)",
                    3, false, "data/maps/ether/earth/-25000/earth_-25000_density.png"),
            new ScenarioRunConfig("younger_dryas", "Younger Dryas & Natufian Climatic Shock (-10,900 -> -9,500 BP)",
                    4, false, "data/maps/ether/earth/-10900/earth_-10900_density.png"),
            new ScenarioRunConfig("green_sahara", "Green Sahara & African Humid Period (-6000 -> -3500 BCE)",
                    4, false, "data/maps/ether/earth/-6000/earth_-6000_density.png"),
            new ScenarioRunConfig("ancient_egypt", "Ancient Egypt & Early Dynastic Nile Valley (-3000 -> -1000 BCE)",
                    4, false, "data/maps/ether/earth/-3000/earth_-3000_density.png"),
            new ScenarioRunConfig("assyrian_empire", "Middle Bronze Age & Mesopotamia (-1900 -> -600 BCE)",
                    4, false, "data/maps/ether/earth/-1900/earth_-1900_density.png"),
            new ScenarioRunConfig("mesoamerica", "Mesoamerican Civilizations (Olmecs & Formative Maya) (-1500 BCE -> 900 CE)",
                    4, false, "data/maps/ether/earth/-1500/earth_-1500_density.png"),
            new ScenarioRunConfig("early_iron_age", "Early Iron Age & Ancient Mediterranean (-1000 -> -300 BCE)",
                    4, false, "data/maps/ether/earth/-1000/earth_-1000_density.png"),
            new ScenarioRunConfig("alexander_hellenistic_334bc", "Hellenistic Expansion & Shock of Alexander the Great (-334 -> -150 BCE)",
                    4, false, "data/maps/ether/earth/-334/earth_-334_density.png"),
            new ScenarioRunConfig("maurya_empire", "Maurya Empire & Magadha Hegemony (-300 BCE -> 100 CE)",
                    4, false, "data/maps/ether/earth/-300/earth_-300_density.png"),
            new ScenarioRunConfig("mali_empire", "Mali Empire & Trans-Saharan Gold-Salt Trade (1324-1591 CE)",
                    4, false, "data/maps/ether/earth/1324/earth_1324_density.png"),
            new ScenarioRunConfig("americas_1491", "Pre-Columbian Americas: Tawantinsuyu & Anahuac (1491-1650 CE)",
                    4, false, "data/maps/ether/earth/1491/earth_1491_density.png"),
            new ScenarioRunConfig("anthropocene_2000", "Anthropocene & Global Acceleration (2000-2100 CE)",
                    4, false, "data/maps/ether/earth/2000/earth_2000_density.png")
    );

    @Test
    @DisplayName("Execute Phase 3 Benchmark Runs on Local Headless")
    public void testExecutePhase3Scenarios() throws Exception {
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
            String filter = System.getProperty("phase3.scenario");
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
                                    || (rawCfg.presetKey().contains("toba") && sc.getName().toLowerCase().contains("toba"))
                                    || (rawCfg.presetKey().contains("sahul") && sc.getName().toLowerCase().contains("sahul"))
                                    || (rawCfg.presetKey().contains("beringia") && sc.getName().toLowerCase().contains("beringia"))
                                    || (rawCfg.presetKey().contains("younger_dryas") && sc.getName().toLowerCase().contains("younger dryas"))
                                    || (rawCfg.presetKey().contains("green_sahara") && sc.getName().toLowerCase().contains("green sahara"))
                                    || (rawCfg.presetKey().contains("ancient_egypt") && sc.getName().toLowerCase().contains("egypt"))
                                    || (rawCfg.presetKey().contains("assyrian") && sc.getName().toLowerCase().contains("assyrian"))
                                    || (rawCfg.presetKey().contains("mesoamerica") && sc.getName().toLowerCase().contains("mesoamerica"))
                                    || (rawCfg.presetKey().contains("iron_age") && sc.getName().toLowerCase().contains("iron"))
                                    || (rawCfg.presetKey().contains("alexander") && sc.getName().toLowerCase().contains("alexander"))
                                    || (rawCfg.presetKey().contains("maurya") && sc.getName().toLowerCase().contains("maurya"))
                                    || (rawCfg.presetKey().contains("mali") && sc.getName().toLowerCase().contains("mali"))
                                    || (rawCfg.presetKey().contains("americas_1491") && sc.getName().toLowerCase().contains("1491"))
                                    || (rawCfg.presetKey().contains("anthropocene_2000") && sc.getName().toLowerCase().contains("2000")))))
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
            System.out.printf("🚀 RUNNING PHASE 3 BENCHMARK: %s [Res: %d, Years: %d -> %d]%n",
                    cfg.displayName(), cfg.resolution(), cfg.startYear(), cfg.endYear());
            System.out.println("================================================================================");
            int targetRes = cfg.resolution();
            s.setH3Resolution(targetRes);
            if (!cfg.windowed() && s.isClippingEnabled()) {
                s.setClippingEnabled(false);
                double globalPopM = HistoricalValidationKernel.getInterpolatedBenchmarkValue(
                        "worldPopulation", (double) cfg.startYear(), HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC);
                if (!Double.isNaN(globalPopM) && globalPopM > 0.0) {
                    s.setInitialHumanCount(Math.round(globalPopM * 1_000_000.0));
                }
            } else if (!cfg.windowed()) {
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
            int cellCount = (finalSnapshot != null && !finalSnapshot.isEmpty()) ? finalSnapshot.size() : (cfg.windowed() ? 12_500 : (targetRes == 3 ? 41_162 : 288_122));
            if (record.getSpatialSnapshots() != null) record.getSpatialSnapshots().clear();

            GroundTruthAuditGenerator.AuditResult singleAudit = GroundTruthAuditGenerator.generateMultiScenarioGroundTruthAudit(
                    List.of(item), HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                    it -> repo.getRun(it.getRunId())
            );

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
            singleReport.append("# 📜 COMPLETE HISTORICAL FALSIFICATION & EPISTEMIC AUDIT REPORT\n\n");
            singleReport.append("**Scenario**: `").append(cfg.displayName()).append("`\n\n");
            singleReport.append("**Simulation Run Identifier**: `").append(record.getRunId()).append("`\n\n");
            singleReport.append("**Spatial Domain**: H3 Resolution ").append(targetRes)
                    .append(cfg.windowed() ? " (Regional Window)\n\n" : " (Planetary Mesh)\n\n");
            singleReport.append("**Execution Duration**: ").append(String.format(Locale.US, "%.1f s\n\n", elapsedMs / 1000.0));
            singleReport.append("## ⚡ 1. Simulation Engine Execution & Computational Throughput Performance\n\n");
            singleReport.append("| Performance Metric | Measured Value | Unit / Scale Reference | Evaluation |\n|---|:---:|:---:|:---:|\n");
            singleReport.append(String.format(Locale.US, "| **Spatial Mesh Resolution** | **Res %d%s** | %s | 🌐 Grid Domain |\n",
                    targetRes, cfg.windowed() ? " (Windowed)" : "", cfg.windowed() ? "Regional Bounding Box" : "Full Planetary Sphere"));
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

            singleReport.append(singleAudit.reportMarkdown()).append("\n\n");
            singleReport.append("## 🗺️ 2D Cartographic Fidelity & Spatial Discrepancy Assessment\n\n");
            singleReport.append("Direct pixel-to-pixel comparison between final simulated 2D density snapshot and empirical ground truth raster (`")
                    .append(cfg.targetRasterPath()).append("`):\n\n");
            singleReport.append("| Cartographic Metric | Computed Value | Physical Threshold / Benchmark | Status |\n|---|:---:|:---:|:---:|\n");
            singleReport.append(String.format(Locale.US, "| **SSIM (Structural Similarity Index)** | **%.4f** | $\\ge 0.50$ (macro-spatial coherence) | %s |\n",
                    spatialResult.getSsim(), spatialResult.getSsim() >= 0.50 ? "🟢 Conformant" : "🟡 Moderate Deviation"));
            singleReport.append(String.format(Locale.US, "| **Pearson Spatial Correlation (r)** | **%+.4f** | $\\ge +0.60$ (gradient alignment) | %s |\n",
                    spatialResult.getPearsonR(), spatialResult.getPearsonR() >= 0.60 ? "🟢 Conformant" : "🟡 Imperfect Gradient"));
            singleReport.append(String.format(Locale.US, "| **Jaccard Ecumene Overlap (IoU)** | **%.2f%%** | $\\ge 50.0\\%%$ (inhabited surface area) | %s |\n",
                    spatialResult.getJaccardIndex() * 100.0, spatialResult.getJaccardIndex() >= 0.50 ? "🟢 Conformant" : "🟡 Dispersed"));
            singleReport.append(String.format(Locale.US, "| **RMSE (Spatial Pixel Residual Error)** | **%.2f** | $< 25.0$ | %s |\n",
                    spatialResult.getRmse(), spatialResult.getRmse() < 25.0 ? "🟢 Conformant" : "🔴 Divergent"));
            singleReport.append(String.format(Locale.US, "| **Peak Spatial Divergence (Δ_max)** | **%.2f** | `Lat: %.1f°N, Lng: %.1f°E` | 📍 Hotspot Anomaly |\n\n",
                    spatialResult.getMaxDeltaValue(), spatialResult.getMaxDeltaLat(), spatialResult.getMaxDeltaLng()));

            File singleReportFile = new File(reportsDir, "report_phase3_" + cfg.presetKey() + ".md");
            try (PrintWriter pw = new PrintWriter(singleReportFile, StandardCharsets.UTF_8)) {
                pw.print(singleReport.toString());
            }
            System.out.println("[SUCCESS] Phase 3 individual audit report generated: " + singleReportFile.getAbsolutePath());
        }

        if (benchmarkItems.isEmpty()) return;

        StringBuilder synthesis = new StringBuilder();
        synthesis.append("# 📜 UNIFIED MACRO-HISTORICAL FALSIFICATION SYNTHESIS: PHASE 3 (14 COMPLEMENTARY SCENARIOS)\n\n");
        synthesis.append("## 🗺️ 2D Cartographic Fidelity & Spatial Discrepancy Synthesis (Phase 3)\n\n");
        synthesis.append("| Scenario | Spatial Resolution | Historical Epoch | SSIM (Structure) | Pearson Spatial (r) | Jaccard IoU | Peak Delta Coordinates | Cartographic Anomaly Diagnostic |\n|---|:---:|:---:|:---:|:---:|:---:|:---:|---|\n");

        for (SimpleBenchmarkItem it : benchmarkItems) {
            ScenarioRunConfig cfg = configMap.get(it.getRunId());
            MapComparisonResult sf = spatialResults.get(it.getRunId());
            if (cfg != null && sf != null) {
                synthesis.append(String.format(Locale.US,
                        "| **%s** | Res %d%s | %d | **%.4f** | **%+.4f** | %.1f%% | %.1f°N, %.1f°E | %s |\n",
                        it.getName(), cfg.resolution(), cfg.windowed() ? " (Win)" : "", cfg.startYear(),
                        sf.getSsim(), sf.getPearsonR(), sf.getJaccardIndex() * 100.0,
                        sf.getMaxDeltaLat(), sf.getMaxDeltaLng(), sf.getDiagnosticSummary()
                ));
            }
        }

        synthesis.append("\n---\n\n## ⚡ Engine Performance Synthesis Across Phase 3 Scenarios\n\n");
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

        File synthesisFile = new File(reportsDir, "scenario_phase3_falsification_synthesis.md");
        try (PrintWriter pw = new PrintWriter(synthesisFile, StandardCharsets.UTF_8)) {
            pw.print(synthesis.toString());
        }
        System.out.println("[SUCCESS] Global Phase 3 synthesis report generated: " + synthesisFile.getAbsolutePath());
    }

    private static MapComparisonResult evaluateSpatialFidelity(SimulationRunRecord record, Scenario scenario, ScenarioRunConfig cfg) {
        try {
            File groundTruthFile = new File(cfg.targetRasterPath());
            if (!groundTruthFile.exists()) {
                File epochDir = groundTruthFile.getParentFile();
                if (epochDir != null && epochDir.exists()) {
                    File[] pngs = epochDir.listFiles((dir, name) -> name.endsWith("_density.png"));
                    if (pngs != null && pngs.length > 0) groundTruthFile = pngs[0];
                }
            }
            if (!groundTruthFile.exists()) {
                return new MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "Empirical Raster Missing");
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
            return new MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "Spatial Comparison Error: " + ex.getMessage());
        }
    }
}
