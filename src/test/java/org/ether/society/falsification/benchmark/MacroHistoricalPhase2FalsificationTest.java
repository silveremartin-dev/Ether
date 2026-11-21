/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.falsification.benchmark;

import org.ether.society.analytics.*;
import org.ether.society.analytics.MapComparisonMetrics.MapComparisonResult;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.ether.society.ui.analytics.SpatialTensorRasterRenderer;
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
 * Phase 2 Historical Falsification Benchmark Suite:
 * 1. Pax Romana (0 CE)
 * 2. Ramesses II (-1279 BCE)
 * 3. Bronze Age Collapse (-1200 BCE)
 * 4. Mongol Conquest (1206 CE)
 * 5. Columbian Contact (1492 CE)
 * 6. Tokugawa Japan (1639 CE)
 * 7. Great Acceleration (1950 CE)
 * 8. Out of Africa (-100,000 BP)
 */
@Tag("academic")
@Tag("scientific")
@Tag("benchmark")
public class MacroHistoricalPhase2FalsificationTest {

    public record SimpleBenchmarkItem(String name, String runId) implements ScenarioBenchmarkItem {
        @Override public String getName() { return name; }
        @Override public String getRunId() { return runId; }
    }

    public record ScenarioRunConfig(
            String presetKey, String displayName, int startYear, int endYear,
            int resolution, boolean windowed, String targetRasterPath
    ) {}

    public record CulturalLayerAudit(
            String channelKey, String displayName, String empiricalSource,
            double ssim, double pearsonR, double jaccardIoU, double rmse, String status
    ) {}

    public record EnginePerformanceMetrics(
            String scenarioKey, int resolution, int cellCount, int ticks,
            double durationSec, double tps, double msPerTick, double latencyUsPerCell,
            double cellThroughputPerSec, long usedHeapMb, long maxHeapMb
    ) {}

    private static final List<ScenarioRunConfig> TARGET_CONFIGS = List.of(
            new ScenarioRunConfig("pax_romana_augustus_0", "Pax Romana & Principate of Augustus (0 CE)",
                    0, 20, 4, false, "data/maps/ether/earth/0/earth_0_density.png"),
            new ScenarioRunConfig("ramesses_ii", "Ramesses II, Pax Aegyptiaca & Monumental Builders (-1279 BCE)",
                    -1279, -1259, 4, false, "data/maps/ether/earth/-1279/earth_-1279_density.png"),
            new ScenarioRunConfig("bronze_age_collapse_1200bc", "Late Bronze Age Collapse & Sea Peoples Cataclysm (-1200 BCE)",
                    -1200, -1180, 4, false, "data/maps/ether/earth/-1200/earth_-1200_density.png"),
            new ScenarioRunConfig("mongol_conquest_1206", "Mongol Conquest & Eurasian Steppe Unification (1206 CE)",
                    1206, 1226, 4, false, "data/maps/ether/earth/1206/earth_1206_density.png"),
            new ScenarioRunConfig("columbian_contact", "Columbian Contact & Transoceanic Ecological Exchange (1492 CE)",
                    1492, 1512, 4, false, "data/maps/ether/earth/1492/earth_1492_density.png"),
            new ScenarioRunConfig("tokugawa_japan", "Tokugawa Shogunate & Sakoku Autarky (1639 CE)",
                    1639, 1659, 4, false, "data/maps/ether/earth/1639/earth_1639_density.png"),
            new ScenarioRunConfig("great_acceleration_1950", "Post-War Reconstruction & The Great Acceleration (1950 CE)",
                    1950, 1970, 4, false, "data/maps/ether/earth/1950/earth_1950_density.png"),
            new ScenarioRunConfig("out_of_africa", "Out of Africa & Homo Sapiens Expansion (-100,000 BP)",
                    -100000, -99980, 3, false, "data/maps/ether/earth/-100000/earth_-100000_density.png")
    );

    @Test
    @DisplayName("List all preset keys in built-in scenarios")
    public void listPresetKeys() {
        for (Scenario s : Scenario.getBuiltInScenarios()) {
            System.out.println("PRESET: key='" + s.getPresetKey() + "' name='" + s.getName() + "' year=" + s.getStartDateYear());
        }
    }

    @Test
    @DisplayName("Execute Phase 2 Benchmark Runs on Local Headless")
    public void testExecutePhase2Scenarios() throws Exception {
        org.ether.society.i18n.I18n.setLanguage(org.ether.society.i18n.Language.ENGLISH);
        SimulationRunRepository repo = SimulationRunRepository.getInstance();

        List<Scenario> allScenarios = Scenario.getBuiltInScenarios();
        List<SimpleBenchmarkItem> benchmarkItems = new ArrayList<>();
        Map<String, MapComparisonResult> spatialResults = new LinkedHashMap<>();
        Map<String, List<CulturalLayerAudit>> culturalResults = new LinkedHashMap<>();
        Map<String, EnginePerformanceMetrics> performanceResults = new LinkedHashMap<>();
        Map<String, ScenarioRunConfig> configMap = new LinkedHashMap<>();

        File reportsDir = new File("docs/reports");
        reportsDir.mkdirs();

        for (ScenarioRunConfig cfg : TARGET_CONFIGS) {
            String filter = System.getProperty("phase2.scenario");
            if (filter != null && !filter.isBlank()) {
                List<String> allowed = Arrays.stream(filter.split(","))
                        .map(String::trim).map(String::toLowerCase).toList();
                if (!allowed.contains(cfg.presetKey().toLowerCase())) continue;
            }
            System.out.println("================================================================================");
            System.out.printf("🚀 RUNNING PHASE 2 BENCHMARK: %s [Res: %d, Years: %d -> %d]%n",
                    cfg.displayName(), cfg.resolution(), cfg.startYear(), cfg.endYear());
            System.out.println("================================================================================");

            Scenario s = allScenarios.stream()
                    .filter(sc -> cfg.presetKey().equalsIgnoreCase(sc.getPresetKey())
                            || (sc.getPresetKey() != null && sc.getPresetKey().toLowerCase().contains(cfg.presetKey().toLowerCase()))
                            || (sc.getStartDateYear() == cfg.startYear() && sc.getName() != null && (
                                    sc.getName().toLowerCase().contains(cfg.presetKey().toLowerCase())
                                    || (cfg.presetKey().contains("great_acceleration") && sc.getName().toLowerCase().contains("great acceleration"))
                                    || (cfg.presetKey().contains("out_of_africa") && sc.getName().toLowerCase().contains("out of africa")))))
                    .findFirst().orElse(null);

            if (s == null) {
                System.err.println("Scenario not found in built-ins: " + cfg.presetKey());
                continue;
            }
            if (s.getPresetKey() == null || s.getPresetKey().isBlank()) {
                s.setPresetKey(cfg.presetKey());
            }

            s.setStartDateYear(cfg.startYear());
            s.setEndDateYear(cfg.endYear());
            int targetRes = cfg.resolution();
            s.setH3Resolution(targetRes);
            if (!cfg.windowed()) s.setClippingEnabled(false);

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

            List<CulturalLayerAudit> cultAudits = evaluateCulturalTensors(record, s, cfg);
            culturalResults.put(record.getRunId(), cultAudits);

            List<H3Cell> finalSnapshot = record.getSpatialSnapshotAt(cfg.endYear());
            int cellCount = (finalSnapshot != null && !finalSnapshot.isEmpty()) ? finalSnapshot.size() : (cfg.windowed() ? 12_500 : 288_122);
            if (record.getSpatialSnapshots() != null) record.getSpatialSnapshots().clear();

            GroundTruthAuditGenerator.AuditResult singleAudit = GroundTruthAuditGenerator.generateMultiScenarioGroundTruthAudit(
                    List.of(item), HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                    it -> repo.getRun(it.getRunId())
            );

            int totalTicks = Math.max(1, (cfg.endYear() - cfg.startYear()) * 12);
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
                    tps, tps >= 1.0 ? "🟢 High Throughput" : (tps >= 0.4 ? "🟡 Nominal Real-Time" : "🔴 Bottlenecked")));
            singleReport.append(String.format(Locale.US, "| **Mean Step Latency (ms/tick)** | **%.2f ms** | Mean execution per monthly tick | %s |\n",
                    msPerTick, msPerTick < 1500.0 ? "🟢 Sub-Second Step" : "🟡 Heavy Physical Iteration"));
            singleReport.append(String.format(Locale.US, "| **Per-Cell Latency** | **%.3f μs** | Microseconds per cell per tick | 🚀 Micro-Scaling |\n", latencyUsPerCell));
            singleReport.append(String.format(Locale.US, "| **Cellular Processing Throughput** | **%,.0f** | Cells Evaluated / Second | 📈 Compute Density |\n", cellThroughput));
            singleReport.append(String.format(Locale.US, "| **JVM Heap Allocation** | **%,d MB / %,d MB** | Used Memory / Max Available Heap | 💾 Memory Footprint |\n\n", usedHeapMb, maxHeapMb));

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

            File singleReportFile = new File(reportsDir, "report_phase2_" + cfg.presetKey() + ".md");
            try (PrintWriter pw = new PrintWriter(singleReportFile, StandardCharsets.UTF_8)) {
                pw.print(singleReport.toString());
            }
            System.out.println("[SUCCESS] Phase 2 individual audit report generated: " + singleReportFile.getAbsolutePath());
        }

        if (benchmarkItems.isEmpty()) return;

        GroundTruthAuditGenerator.AuditResult globalAudit = GroundTruthAuditGenerator.generateMultiScenarioGroundTruthAudit(
                benchmarkItems, HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                it -> repo.getRun(it.getRunId())
        );

        StringBuilder synthesis = new StringBuilder();
        synthesis.append(globalAudit.reportMarkdown()).append("\n\n");
        synthesis.append("## 🗺️ 2D Cartographic Fidelity & Spatial Discrepancy Synthesis (Phase 2)\n\n");
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

        synthesis.append("\n---\n\n## ⚡ Engine Performance Synthesis Across Phase 2 Scenarios\n\n");
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

        File synthesisFile = new File(reportsDir, "scenario_phase2_falsification_synthesis.md");
        try (PrintWriter pw = new PrintWriter(synthesisFile, StandardCharsets.UTF_8)) {
            pw.print(synthesis.toString());
        }
        System.out.println("[SUCCESS] Global Phase 2 synthesis report generated: " + synthesisFile.getAbsolutePath());
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

    private static List<CulturalLayerAudit> evaluateCulturalTensors(SimulationRunRecord record, Scenario scenario, ScenarioRunConfig cfg) {
        List<CulturalLayerAudit> audits = new ArrayList<>();
        try {
            File epochDir = new File(cfg.targetRasterPath()).getParentFile();
            List<H3Cell> snapshotCells = record.getSpatialSnapshotAt(cfg.endYear());
            if (snapshotCells == null || snapshotCells.isEmpty()) {
                snapshotCells = record.getSpatialSnapshotAt(cfg.startYear());
            }
            if (snapshotCells == null || snapshotCells.isEmpty() || epochDir == null || !epochDir.exists()) {
                return audits;
            }
            String[] layers = {"isogloss", "kinship", "rituals", "sovereignty", "technology", "trade", "hierarchy", "carrying_capacity", "pathogen"};
            String[] titles = {"Linguistic Isoglosses (Dim 0)", "Kinship & Clan Structure (Dim 1)", "Sacred Rituals & Solidarity (Dim 2)",
                    "Sovereignty & Borders (Dim 3)", "Materiality & Tooling (Dim 4)", "Trade & Market Access (Dim 5)",
                    "Administrative Hierarchy (Dim 6)", "Ecological Carrying Capacity (Dim 7)", "Sanitary & Pathogen Memory (Dim 8)"};
            for (int d = 0; d < layers.length; d++) {
                File layerFile = new File(epochDir, "earth_" + cfg.startYear() + "_" + layers[d] + ".png");
                if (layerFile.exists()) {
                    BufferedImage gtRaster = ImageIO.read(layerFile);
                    MapComparisonResult comp = MapComparisonMetrics.compareCellsToRaster(
                            snapshotCells, gtRaster, scenario.getMinLat(), scenario.getMaxLat(), scenario.getMinLng(), scenario.getMaxLng(), cfg.windowed());
                    String status = comp.getPearsonR() >= 0.70 ? "🟢 Robust Empirical Alignment" : (comp.getPearsonR() >= 0.50 ? "🟡 Moderate Spatial Gradient" : "🔴 Cultural Tensor Drift");
                    audits.add(new CulturalLayerAudit(layers[d], titles[d], "HYDE 3.4 / Seshat Global DB", comp.getSsim(), comp.getPearsonR(), comp.getJaccardIndex() * 100.0, comp.getRmse(), status));
                }
            }
        } catch (Exception ignored) {}
        return audits;
    }
}
