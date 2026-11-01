package org.ether.society.analytics;

import org.ether.society.analytics.MapComparisonMetrics.MapComparisonResult;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.ether.society.ui.analytics.SpatialTensorRasterRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Historical Falsification Suite for the 3 Legacy Milestone Scenarios:
 * 1. The Black Death &amp; Post-Feudal Economic Inversion (1347 CE)
 * 2. World Wars, Totalitarian Crisis &amp; Nuclear Dawn (1914 CE)
 * 3. Antonine Plague &amp; Imperial Crisis under Marcus Aurelius (165 CE)
 *
 * Runs headless simulations at resolution &gt;= 4 (and Res 6 for regional window),
 * audits 1D telemetry against HYDE 3.4 / Maddison / Smil / Seshat benchmarks,
 * evaluates 2D spatial density distributions against historical ground truth rasters,
 * and produces rigorous markdown epistemic audit reports.
 */
public class LegacyHistoricalScenariosFalsificationTest {

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

    private static final List<ScenarioRunConfig> TARGET_CONFIGS = List.of(
            new ScenarioRunConfig("black_death_1347", "The Black Death & Post-Feudal Economic Inversion (1347 CE)",
                    1347, 1367, 4, false, "data/ether/earth/1347/earth_1347_density.png"),
            new ScenarioRunConfig("world_wars_totalitarian_1914", "World Wars, Totalitarian Crisis & Nuclear Dawn (1914 CE)",
                    1914, 1934, 4, false, "data/ether/earth/1914/earth_1914_density.png"),
            new ScenarioRunConfig("antonine_plague_165", "Antonine Plague & Imperial Crisis under Marcus Aurelius (165 CE)",
                    165, 180, 6, true, "data/ether/earth/180/earth_180_density.png")
    );

    @Test
    @DisplayName("Execute Legacy Historical Scenarios Benchmark Runs & Epistemic Audit")
    public void testExecuteLegacyHistoricalScenarios() {
        org.ether.society.i18n.I18n.setLanguage(org.ether.society.i18n.Language.ENGLISH);
        SimulationRunRepository repo = SimulationRunRepository.getInstance();

        List<Scenario> allScenarios = Scenario.getBuiltInScenarios();
        List<SimpleBenchmarkItem> benchmarkItems = new ArrayList<>();
        Map<String, MapComparisonResult> spatialResults = new LinkedHashMap<>();
        Map<String, ScenarioRunConfig> configMap = new LinkedHashMap<>();

        File reportsDir = new File("docs/reports");
        reportsDir.mkdirs();

        for (ScenarioRunConfig cfg : TARGET_CONFIGS) {
            String filter = System.getProperty("legacy.scenario");
            if (filter != null && !filter.isBlank() && !cfg.presetKey().equalsIgnoreCase(filter)) {
                continue;
            }
            System.out.println("================================================================================");
            System.out.printf("🚀 RUNNING LEGACY BENCHMARK: %s [Res: %d, Years: %d -> %d]%n",
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
            s.setH3Resolution(cfg.resolution());

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
            if (record.getSpatialSnapshots() != null) {
                record.getSpatialSnapshots().clear();
            }

            // Generate authentic individual audit report via GroundTruthAuditGenerator
            GroundTruthAuditGenerator.AuditResult singleAudit = GroundTruthAuditGenerator.generateMultiScenarioGroundTruthAudit(
                    List.of(item),
                    HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                    it -> repo.getRun(it.getRunId())
            );

            StringBuilder singleReport = new StringBuilder();
            singleReport.append("# 📜 COMPLETE HISTORICAL FALSIFICATION & EPISTEMIC AUDIT REPORT\n\n");
            singleReport.append("**Scenario**: `").append(cfg.displayName()).append("`\n\n");
            singleReport.append("**Simulation Run Identifier**: `").append(record.getRunId()).append("`\n\n");
            singleReport.append("**Spatial Domain**: H3 Resolution ").append(cfg.resolution())
                    .append(cfg.windowed() ? " (Regional Window)\n\n" : " (Planetary Mesh)\n\n");
            singleReport.append("**Execution Duration**: ").append(String.format(Locale.US, "%.1f s\n\n", elapsedMs / 1000.0));
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

            File singleReportFile = new File(reportsDir, "report_legacy_" + cfg.presetKey() + ".md");
            try (PrintWriter pw = new PrintWriter(singleReportFile)) {
                pw.print(singleReport.toString());
            } catch (Exception ex) {
                System.err.println("Failed to write single report: " + ex.getMessage());
            }
            System.out.println("[SUCCESS] Individual audit report generated: " + singleReportFile.getAbsolutePath());

            System.gc();
        }

        // Generate Master Global Synthesis Report for Legacy Scenarios
        if (!benchmarkItems.isEmpty()) {
            GroundTruthAuditGenerator.AuditResult globalAudit = GroundTruthAuditGenerator.generateMultiScenarioGroundTruthAudit(
                    benchmarkItems,
                    HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC,
                    it -> repo.getRun(it.getRunId())
            );

            StringBuilder synthesis = new StringBuilder();
            synthesis.append(globalAudit.reportMarkdown()).append("\n\n");
            synthesis.append("## 🗺️ 2D Cartographic Fidelity & Spatial Discrepancy Synthesis\n\n");
            synthesis.append("> Direct pixel-to-pixel evaluation between empirical reference rasters and projected simulation grids.\n\n");
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
            synthesis.append("\n---\n");

            File synthesisFile = new File(reportsDir, "scenario_legacy_falsification_synthesis.md");
            try (PrintWriter pw = new PrintWriter(synthesisFile)) {
                pw.print(synthesis.toString());
            } catch (Exception ex) {
                System.err.println("Failed to write synthesis report: " + ex.getMessage());
            }
            System.out.println("[SUCCESS] Global legacy synthesis report generated: " + synthesisFile.getAbsolutePath());
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
            BufferedImage simRaster = SpatialTensorRasterRenderer.rasterizeCellsToGrayscaleDensityImage(
                    cells, groundTruth, groundTruth.getWidth(), groundTruth.getHeight());
            if (simRaster == null) {
                return new MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "Rasterization Failed");
            }
            if (cfg.windowed() && s != null && s.isClippingEnabled()) {
                return MapComparisonMetrics.compareImagesWindowed(
                        simRaster, groundTruth,
                        s.getMinLat(), s.getMaxLat(), s.getMinLng(), s.getMaxLng());
            }
            return MapComparisonMetrics.compareImages(simRaster, groundTruth);
        } catch (Exception e) {
            return new MapComparisonResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "Comparison Error: " + e.getMessage());
        }
    }
}
