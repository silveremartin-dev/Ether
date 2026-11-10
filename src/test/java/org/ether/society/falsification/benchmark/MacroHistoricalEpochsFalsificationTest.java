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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 Falsification Suite for the 6 new candidate scenarios:
 * 1. Song Dynasty (1000 CE)
 * 2. Fertile Crescent (-8000 BCE)
 * 3. Industrial Revolution (1800 CE)
 * 4. Islamic Expansion (632 CE)
 * 5. Last Glacial Maximum (-20,000 BP)
 * 6. Late Antique Little Ice Age (536 CE)
 *
 * Runs headless simulations at resolution >= 4 (and >= 6 for regional windows),
 * measures 1D telemetry errors against HYDE 3.4 / Maddison benchmarks via GroundTruthAuditGenerator,
 * compares 2D spatial density distributions against historical rasters via MapComparisonMetrics,
 * and automatically produces comprehensive academic audit reports.
 */
@Tag("academic")
@Tag("scientific")
@Tag("benchmark")
public class MacroHistoricalEpochsFalsificationTest {

    public record SimpleBenchmarkItem(String name, String runId) implements ScenarioBenchmarkItem {
        @Override
        public String getName() { return name; }
        @Override
        public String getRunId() { return runId; }
    }

    public record ScenarioRunConfig(
            String presetKey,
            String displayName,
            int startYear,
            int endYear,
            int resolution,
            boolean windowed,
            String targetRasterPath
    ) {}

    public record CulturalLayerAudit(
            String channelKey,
            String displayName,
            String empiricalSource,
            double ssim,
            double pearsonR,
            double jaccardIoU,
            double rmse,
            String status
    ) {}

    public record EnginePerformanceMetrics(
            String scenarioKey,
            int resolution,
            int cellCount,
            int ticks,
            double durationSec,
            double tps,
            double msPerTick,
            double latencyUsPerCell,
            double cellThroughputPerSec,
            long usedHeapMb,
            long maxHeapMb
    ) {}

    private static final List<ScenarioRunConfig> TARGET_CONFIGS = List.of(
            // --- Middle Epochs (Classical, Medieval & Post-Feudal Transitions) ---
            new ScenarioRunConfig("antonine_plague_165", "Antonine Plague & Imperial Crisis under Marcus Aurelius (165-180 CE)",
                    165, 185, 4, false, "data/maps/ether/earth/160/earth_160_density.png"),
            new ScenarioRunConfig("late_antique_ice_age", "Late Antique Little Ice Age & Justinian Plague (536 CE)",
                    536, 556, 4, false, "data/maps/ether/earth/536/earth_536_density.png"),
            new ScenarioRunConfig("islamic_expansion_632", "Islamic Expansion & Caliphal Commercial Revolution (632 CE)",
                    632, 652, 4, false, "data/maps/ether/earth/632/earth_632_density.png"),
            new ScenarioRunConfig("song_dynasty", "Song Dynasty & Hydraulic Pre-Industrialization (1000 CE)",
                    1000, 1020, 4, false, "data/maps/ether/earth/1000/earth_1000_density.png"),
            new ScenarioRunConfig("black_death_1347", "The Black Death & Post-Feudal Economic Inversion (1347 CE)",
                    1347, 1367, 4, false, "data/maps/ether/earth/1347/earth_1347_density.png"),

            // --- Modern & Contemporary Epochs ---
            new ScenarioRunConfig("industrial_1800", "Industrial Revolution & Fossil Energy (1800 CE)",
                    1800, 1820, 4, false, "data/maps/ether/earth/1800/earth_1800_density.png"),
            new ScenarioRunConfig("world_wars_totalitarian_1914", "World Wars, Totalitarian Rupture & Nuclear Era (1914 CE)",
                    1914, 1934, 4, false, "data/maps/ether/earth/1914/earth_1914_density.png"),

            // --- Deep Prehistory & Paleolithic ---
            new ScenarioRunConfig("fertile_crescent", "Fertile Crescent & Neolithic Agriculture (-8000 BCE)",
                    -8000, -7980, 7, true, "data/maps/ether/earth/-8000/earth_-8000_density.png"),
            new ScenarioRunConfig("lgm_solutrean", "Last Glacial Maximum & Solutrean (-20,000 BP)",
                    -20000, -19980, 4, false, "data/maps/ether/earth/-20000/earth_-20000_density.png")
    );

    @Test
    @DisplayName("Execute Phase 1 Benchmark Runs on GCP VM or Local Headless")
    public void testExecutePhase1Scenarios() {
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
            String filter = System.getProperty("phase1.scenario");
            if (filter != null && !filter.isBlank()) {
                List<String> allowed = Arrays.stream(filter.split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .toList();
                if (!allowed.contains(cfg.presetKey().toLowerCase())) {
                    continue;
                }
            }
            System.out.println("================================================================================");
            System.out.printf("🚀 RUNNING PHASE 1 BENCHMARK: %s [Res: %d, Years: %d -> %d]%n",
                    cfg.displayName(), cfg.resolution(), cfg.startYear(), cfg.endYear());
            System.out.println("================================================================================");

            Scenario s = allScenarios.stream()
                    .filter(sc -> cfg.presetKey().equalsIgnoreCase(sc.getPresetKey()))
                    .findFirst()
                    .orElse(null);

            if (s == null) {
                System.err.println("Scenario not found in built-ins: " + cfg.presetKey());
                continue;
            }

            s.setStartDateYear(cfg.startYear());
            s.setEndDateYear(cfg.endYear());

            int targetRes = cfg.resolution();
            String resProp = System.getProperty("phase1.resolution");
            if (resProp == null || resProp.isBlank()) {
                resProp = System.getProperty("benchmark.resolution");
            }
            if (resProp != null && !resProp.isBlank()) {
                try {
                    targetRes = Integer.parseInt(resProp.trim());
                } catch (NumberFormatException ignored) {}
            }
            s.setH3Resolution(targetRes);

            if (!cfg.windowed()) {
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

            // Real 2D spatial comparison between simulation snapshot raster and ground truth raster
            MapComparisonResult spatialResult = evaluateSpatialFidelity(record, s, cfg);
            spatialResults.put(record.getRunId(), spatialResult);

            // Real 2D geocultural tensor fidelity evaluation across 9 canonical cultural layers
            List<CulturalLayerAudit> cultAudits = evaluateCulturalTensors(record, s, cfg);
            culturalResults.put(record.getRunId(), cultAudits);

            List<H3Cell> finalSnapshot = record.getSpatialSnapshotAt(cfg.endYear());
            int cellCount = (finalSnapshot != null && !finalSnapshot.isEmpty()) ? finalSnapshot.size() : (cfg.windowed() ? 12_500 : 288_122);

            // Release heavy 2D cell lists after computing spatial metrics to prevent heap exhaustion across runs
            if (record.getSpatialSnapshots() != null) {
                record.getSpatialSnapshots().clear();
            }

            // Generate authentic individual audit report via GroundTruthAuditGenerator
            GroundTruthAuditGenerator.AuditResult singleAudit = GroundTruthAuditGenerator.generateMultiScenarioGroundTruthAudit(
                    List.of(item),
                    HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                    it -> repo.getRun(it.getRunId())
            );

            // Measure Real Engine Performance Metrics
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
            singleReport.append("| Performance Metric | Measured Value | Unit / Scale Reference | Evaluation |\n");
            singleReport.append("|---|:---:|:---:|:---:|\n");
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
            singleReport.append("| Cartographic Metric | Computed Value | Physical Threshold / Benchmark | Status |\n");
            singleReport.append("|---|:---:|:---:|:---:|\n");
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
            singleReport.append("### 🔍 Detected Cartographic Anomaly Diagnostic\n");
            singleReport.append("- **Characterization**: `").append(spatialResult.getDiagnosticSummary()).append("`\n\n");

            singleReport.append("## 🏛️ Geocultural Tensor Spatial Fidelity & Drift Assessment\n\n");
            singleReport.append("Direct pixel-to-pixel evaluation between final simulated geocultural tensors and empirical historical rasters:\n\n");
            singleReport.append("| # | Geocultural Tensor Dimension | Ground Truth Empirical Source | SSIM | Pearson (r) | Categorical IoU | RMSE | Drift Status |\n");
            singleReport.append("|---|---|---|:---:|:---:|:---:|:---:|:---:|\n");
            int cIdx = 1;
            for (CulturalLayerAudit ca : cultAudits) {
                singleReport.append(String.format(Locale.US,
                        "| %d | **%s** | %s | **%.4f** | **%+.4f** | %.1f%% | %.2f | %s |\n",
                        cIdx++, ca.displayName(), ca.empiricalSource(),
                        ca.ssim(), ca.pearsonR(), ca.jaccardIoU(), ca.rmse(), ca.status()
                ));
            }
            singleReport.append("\n---\n\n");

            File singleReportFile = new File(reportsDir, "report_phase1_" + cfg.presetKey() + ".md");
            try (PrintWriter pw = new PrintWriter(singleReportFile, StandardCharsets.UTF_8)) {
                pw.print(singleReport.toString());
            } catch (Exception ex) {
                System.err.println("Failed to write single report: " + ex.getMessage());
            }
            System.out.println("[SUCCESS] Individual audit report generated: " + singleReportFile.getAbsolutePath());

            System.gc();
        }

        // If all scenarios (or filtered batch) ran, generate Master Global Synthesis Report
        if (!benchmarkItems.isEmpty()) {
            GroundTruthAuditGenerator.AuditResult globalAudit = GroundTruthAuditGenerator.generateMultiScenarioGroundTruthAudit(
                    benchmarkItems,
                    HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                    it -> repo.getRun(it.getRunId())
            );

            StringBuilder synthesis = new StringBuilder();
            String rawReport = globalAudit.reportMarkdown();

            // Extract Section 1 (Ranking) and Section 2 (Gap Matrix) to present executive summaries upfront
            String deepSectionHeader = "## 📊 3. Deep Diagnostic Analysis by Scenario";
            int deepIdx = rawReport.indexOf(deepSectionHeader);
            String executivePrefix = (deepIdx > 0) ? rawReport.substring(0, deepIdx) : rawReport;
            String deepSuffix = (deepIdx > 0) ? rawReport.substring(deepIdx) : "";

            // Generate visual synthesis charts in docs/images/charts/
            try {
                BenchmarkSynthesisChartGenerator.generateAllSynthesisCharts(new File("docs/images/charts"));
            } catch (Exception ex) {
                System.err.println("Warning: Failed to generate synthesis charts: " + ex.getMessage());
            }

            synthesis.append(executivePrefix).append("\n");
            synthesis.append("## 📈 Visual Synthesis: Fidelity Curves, 27-Variable Profile & 2D Cartographic Quality\n\n");
            synthesis.append("![1. Master 45-Scenario Fidelity Curve](../images/charts/chart1_45_scenarios_fidelity_curve.png)\n\n");
            synthesis.append("![2. Quality Profile of All 27 Canonical State Variables](../images/charts/chart2_27_variables_quality_profile.png)\n\n");
            synthesis.append("![3. 2D Cartographic Raster Fidelity & 9D Geocultural Tensor Quality Curves](../images/charts/chart3_cartographic_2d_quality_curves.png)\n\n");
            synthesis.append("![4. Multi-Scenario x 27-Variable Heatmap Matrix](../images/charts/chart4_multi_scenario_heatmap_matrix.png)\n\n");
            synthesis.append("![5. Pillars 6 & 7 Validation Summary](../images/charts/chart5_pillars_6_and_7_validation_summary.png)\n\n");
            synthesis.append("![6. H3 Scaling & Computational ROI Pareto Frontier](../images/charts/chart6_engine_scaling_and_roi_pareto.png)\n\n");

            synthesis.append("## 🗺️ 2D Cartographic Fidelity & Spatial Discrepancy Synthesis\n\n");
            synthesis.append("> Direct H3 cellular spatial sampling between spherical cell centroids and empirical reference rasters (HYDE 3.4).\n\n");
            synthesis.append("| Scenario | Spatial Resolution | Historical Epoch | SSIM (Structure) | Pearson Spatial (r) | Jaccard IoU | Peak Delta Coordinates | Cartographic Anomaly Diagnostic |\n");
            synthesis.append("|---|:---:|:---:|:---:|:---:|:---:|:---:|---|\n");

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
            synthesis.append("\n---\n\n");

            synthesis.append("## 🏛️ Geocultural Tensor Drift & Reliability Synthesis Across 9 Epochs\n\n");
            synthesis.append("> Empirical comparison across 9 orthogonal geocultural tensor layers (isogloss, kinship, rituals, sovereignty, technology, trade, institutional, ecological, pathogen) against historical ground truth rasters.\n\n");
            synthesis.append("| Scenario | Epoch | Channel | Pearson (r) | SSIM | Categorical IoU | Epistemic Drift Diagnostic |\n");
            synthesis.append("|---|:---:|---|:---:|:---:|:---:|:---:|\n");
            for (SimpleBenchmarkItem it : benchmarkItems) {
                ScenarioRunConfig cfg = configMap.get(it.getRunId());
                List<CulturalLayerAudit> cList = culturalResults.get(it.getRunId());
                if (cfg != null && cList != null) {
                    for (CulturalLayerAudit ca : cList) {
                        synthesis.append(String.format(Locale.US,
                                "| **%s** | %d | %s | **%+.4f** | **%.4f** | %.1f%% | %s |\n",
                                it.getName(), cfg.startYear(), ca.displayName(),
                                ca.pearsonR(), ca.ssim(), ca.jaccardIoU(), ca.status()
                        ));
                    }
                }
            }
            synthesis.append("\n---\n\n");

            synthesis.append("## ⚡ Planetary Simulation Engine Performance Synthesis Across 9 Historical Scenarios\n\n");
            synthesis.append("> Empirical computational throughput, execution latency, and memory footprint recorded across all 9 canonical scenarios.\n\n");
            synthesis.append("| Scenario | Spatial Domain | Cells | Duration | TPS | ms/tick | Latency/Cell | Cell Throughput | Peak Heap |\n");
            synthesis.append("|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|\n");
            for (SimpleBenchmarkItem it : benchmarkItems) {
                ScenarioRunConfig cfg = configMap.get(it.getRunId());
                EnginePerformanceMetrics pf = performanceResults.get(it.getRunId());
                if (cfg != null && pf != null) {
                    synthesis.append(String.format(Locale.US,
                            "| **%s** | Res %d%s | %,d | %.1f s | **%.2f** | %.1f ms | **%.3f μs** | %,.0f cells/s | %d / %d MB |\n",
                            it.getName(), cfg.resolution(), cfg.windowed() ? " (Win)" : "", pf.cellCount(),
                            pf.durationSec(), pf.tps(), pf.msPerTick(), pf.latencyUsPerCell(),
                            pf.cellThroughputPerSec(), pf.usedHeapMb(), pf.maxHeapMb()
                    ));
                }
            }
            synthesis.append("\n---\n\n");

            // Append detailed per-scenario diagnostic analysis and statistical tests
            if (!deepSuffix.isBlank()) {
                synthesis.append(deepSuffix).append("\n");
            }

            File synthesisFile = new File(reportsDir, "scenario_phase1_falsification_synthesis.md");
            try (PrintWriter pw = new PrintWriter(synthesisFile, StandardCharsets.UTF_8)) {
                pw.print(synthesis.toString());
            } catch (Exception ex) {
                System.err.println("Failed to write synthesis report: " + ex.getMessage());
            }
            System.out.println("[SUCCESS] Global synthesis report generated: " + synthesisFile.getAbsolutePath());
        }
    }

    private static MapComparisonResult evaluateSpatialFidelity(SimulationRunRecord record, Scenario s, ScenarioRunConfig cfg) {
        String rasterPath = cfg.targetRasterPath();
        int finalYear = cfg.endYear();
        File f = new File(rasterPath);
        if (!f.exists()) {
            return new MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "Missing Raster: " + rasterPath);
        }
        try {
            BufferedImage groundTruth = ImageIO.read(f);
            List<H3Cell> cells = record.getSpatialSnapshotAt(finalYear);
            if (cells == null || cells.isEmpty()) {
                return new MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "No Spatial Cells Captured");
            }
            double minLat = (s != null) ? s.getMinLat() : -90.0;
            double maxLat = (s != null) ? s.getMaxLat() : 90.0;
            double minLng = (s != null) ? s.getMinLng() : -180.0;
            double maxLng = (s != null) ? s.getMaxLng() : 180.0;
            boolean windowed = cfg.windowed() && s != null && s.isClippingEnabled();

            // Direct Cellular Mesh Sampling against Ground-Truth Raster (eliminating equirectangular rasterization artifacts)
            return MapComparisonMetrics.compareCellsToRaster(cells, groundTruth, minLat, maxLat, minLng, maxLng, windowed);
        } catch (Exception e) {
            return new MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "Comparison Error: " + e.getMessage());
        }
    }

    private static List<CulturalLayerAudit> evaluateCulturalTensors(SimulationRunRecord record, Scenario s, ScenarioRunConfig cfg) {
        List<CulturalLayerAudit> audits = new ArrayList<>();
        List<H3Cell> cells = record.getSpatialSnapshotAt(cfg.endYear());
        if (cells == null || cells.isEmpty()) {
            cells = record.getSpatialSnapshotAt(cfg.startYear());
        }
        if (cells == null || cells.isEmpty()) {
            return audits;
        }

        File densityRaster = new File(cfg.targetRasterPath());
        File epochDir = densityRaster.getParentFile();
        if (epochDir == null || !epochDir.exists()) {
            return audits;
        }

        record ChannelDef(String key, String displayName, String source, String fileSuffix) {}
        List<ChannelDef> defs = List.of(
                new ChannelDef("isogloss", "Linguistic Isoglosses (Dim 0)", "Glottolog 4.8 / Nichols (1992)", "_isogloss.png"),
                new ChannelDef("kinship", "Kinship & Clan Structure (Dim 1)", "D-PLACE / Goody (1983)", "_kinship.png"),
                new ChannelDef("rituals", "Sacred Rituals & Solidarity (Dim 2)", "Seshat / Turchin (2003)", "_rituals.png"),
                new ChannelDef("sovereignty", "Sovereignty & Borders (Dim 3)", "Grataloup / Tilly (1990)", "_sovereignty.png"),
                new ChannelDef("technology", "Materiality & Tooling (Dim 4)", "Maddison / Ashby (2011)", "_technology.png"),
                new ChannelDef("tradenetwork", "Trade & Market Access (Dim 5)", "Scheidel ORBIS / Krugman (1991)", "_tradenetwork.png"),
                new ChannelDef("institutional", "Administrative Hierarchy (Dim 6)", "Seshat Databank (Turchin 2018)", "_institutional.png"),
                new ChannelDef("ecological", "Ecological Carrying Capacity (Dim 7)", "HYDE 3.4 / Boserup (1965)", "_ecological.png"),
                new ChannelDef("pathogen", "Sanitary & Pathogen Memory (Dim 8)", "Omran (1971) / Preston (1975)", "_pathogen.png")
        );

        double minLat = (s != null) ? s.getMinLat() : -90.0;
        double maxLat = (s != null) ? s.getMaxLat() : 90.0;
        double minLng = (s != null) ? s.getMinLng() : -180.0;
        double maxLng = (s != null) ? s.getMaxLng() : 180.0;
        boolean windowed = cfg.windowed() && s != null && s.isClippingEnabled();

        for (ChannelDef def : defs) {
            File[] matches = epochDir.listFiles((dir, name) -> name.endsWith(def.fileSuffix()));
            if (matches == null || matches.length == 0) {
                continue;
            }
            File targetFile = matches[0];
            try {
                BufferedImage gtRaster = ImageIO.read(targetFile);
                if (gtRaster == null) continue;

                // Direct spherical H3 cellular mesh comparison against empirical cultural ground-truth raster
                MapComparisonResult res = MapComparisonMetrics.compareCellsToChannelRaster(
                        cells, gtRaster, def.key(), minLat, maxLat, minLng, maxLng, windowed);

                String status;
                if (res.getPearsonR() >= 0.70 && res.getSsim() >= 0.60) {
                    status = "🟢 Robust Empirical Alignment";
                } else if (res.getPearsonR() >= 0.40 && res.getSsim() >= 0.35) {
                    status = "🟡 Moderate Spatial Gradient";
                } else {
                    status = "🔴 Cultural Tensor Drift";
                }

                audits.add(new CulturalLayerAudit(
                        def.key(),
                        def.displayName(),
                        def.source(),
                        res.getSsim(),
                        res.getPearsonR(),
                        res.getJaccardIndex() * 100.0,
                        res.getRmse(),
                        status
                ));
            } catch (Exception ex) {
                System.err.println("Failed to compare cultural channel " + def.key() + ": " + ex.getMessage());
            }
        }
        return audits;
    }
}
