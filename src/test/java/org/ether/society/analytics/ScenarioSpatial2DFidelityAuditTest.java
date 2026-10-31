package org.ether.society.analytics;

import org.ether.society.data.HistoricalMapGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.ether.society.network.codec.CellTopologyWireCodec;
import org.ether.society.network.codec.WorldBufferWireCodec;
import org.ether.society.ui.analytics.SpatialTensorRasterRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <h1>2D Spatial Cartographic Fidelity & Discrepancy Audit</h1>
 * <p>
 * Evaluates the spatial fidelity of simulated scenario snapshots against real-world
 * empirical raster layers (NOAA, HYDE 3.4, Seshat, FAO) using 2D tensor metrics:
 * <ul>
 *   <li>Structural Similarity Index Measure (SSIM)</li>
 *   <li>Spatial Pearson Cross-Correlation (r)</li>
 *   <li>Categorical Jaccard Ecumene Overlap (IoU)</li>
 *   <li>Maximum Spatial Error Coordinates & Anomaly Characterization</li>
 * </ul>
 * </p>
 */
public class ScenarioSpatial2DFidelityAuditTest {

    public record SpatialAuditEntry(
            String scenarioName,
            String resolution,
            int targetYear,
            double ssim,
            double pearsonR,
            double jaccardIoU,
            double rmse,
            double maxDeltaLat,
            double maxDeltaLng,
            double maxDeltaValue,
            String spatialDiagnostic
    ) {}

    @Test
    @DisplayName("Evaluate 2D Spatial Fidelity on Audited Scenarios")
    public void test2DSpatialFidelityAcrossScenarios() throws Exception {
        System.out.println("================================================================================");
        System.out.println(" 🗺️ 2D SPATIAL CARTOGRAPHIC FIDELITY & RASTER DISCREPANCY AUDIT");
        System.out.println("================================================================================");

        List<SpatialAuditEntry> results = new ArrayList<>();

        // 1. Ramses II Resolution 6 vs Ground Truth (-1279 BC)
        auditRamsesRes6(results);

        // 2. Antonine Plague (165 CE)
        auditAntoninePlague(results);

        // 3. Black Death (1347 CE)
        auditBlackDeath(results);

        // 4. World Wars (1914 CE)
        auditWorldWars1914(results);

        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println(" 📊 2D CARTOGRAPHIC FIDELITY SYNTHESIS TABLE (SIMULATED SNAPSHOT VS EMPIRICAL RASTER)");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("| %-28s | %-6s | %-6s | %-7s | %-7s | %-7s | %-16s | %-25s |%n",
                "Scenario", "Res", "Epoch", "SSIM", "Pearson", "Jaccard", "Max Delta Coord", "Spatial Anomaly / Diagnosis");
        System.out.println("|------------------------------|--------|--------|---------|---------|---------|------------------|---------------------------|");

        for (SpatialAuditEntry e : results) {
            String coordStr = String.format(Locale.US, "%.1f°N, %.1f°E", e.maxDeltaLat(), e.maxDeltaLng());
            System.out.printf(Locale.US, "| %-28s | %-6s | %-6d | %-7.4f | %-7.4f | %-7.4f | %-16s | %-25s |%n",
                    e.scenarioName(), e.resolution(), e.targetYear(), e.ssim(), e.pearsonR(), e.jaccardIoU(), coordStr, e.spatialDiagnostic());
        }
        System.out.println("================================================================================\n");

        assertTrue(!results.isEmpty(), "At least one scenario 2D audit must be produced");
    }

    private void auditRamsesRes6(List<SpatialAuditEntry> results) {
        try {
            File epochDir = new File("data/ether/earth/-1279");
            File groundTruthFile = new File(epochDir, "earth_-1279_density.png");
            if (!groundTruthFile.exists()) return;

            BufferedImage groundTruth = ImageIO.read(groundTruthFile);

            // Execute a fast 1-year run at resolution 6
            Scenario sc = null;
            for (Scenario s : Scenario.getBuiltInScenarios()) {
                if ("ramesses_ii".equals(s.getPresetKey()) || "earth_-1279_ramesses_ii".equals(s.getPresetKey())) {
                    sc = s;
                    break;
                }
            }
            if (sc == null) return;

            sc.setH3Resolution(6);
            sc.setEndDateYear(-1278); // 1 year run for fast spatial evaluation
            SimulationRunRecord run = HeadlessBatchRunner.executeScenarioHeadless(sc);

            List<H3Cell> snapshotCells = run.getSpatialSnapshotAt(-1278);
            if (snapshotCells == null || snapshotCells.isEmpty()) return;

            BufferedImage simRaster = SpatialTensorRasterRenderer.rasterizeCellsToImage(snapshotCells, "density", 512, 256);
            MapComparisonMetrics.MapComparisonResult comp = sc.isClippingEnabled()
                    ? MapComparisonMetrics.compareImagesWindowed(simRaster, groundTruth, sc.getMinLat(), sc.getMaxLat(), sc.getMinLng(), sc.getMaxLng())
                    : MapComparisonMetrics.compareImages(simRaster, groundTruth);

            String diag = comp.getPearsonR() >= 0.70 ? "Nile Corridor Confined (Stable)" : "Desert Diffusion";
            results.add(new SpatialAuditEntry(
                    "Ramesses II (-1279 BC)", "Res 6", -1279,
                    comp.getSsim(), comp.getPearsonR(), comp.getJaccardIndex(), comp.getRmse(),
                    comp.getMaxDeltaLat(), comp.getMaxDeltaLng(), comp.getMaxDeltaValue(),
                    diag
            ));
        } catch (Exception e) {
            System.err.println("Error in Ramses Res 6 audit: " + e.getMessage());
        }
    }

    private void auditAntoninePlague(List<SpatialAuditEntry> results) {
        try {
            File epochDir = new File("data/ether/earth/165");
            File groundTruthFile = new File(epochDir, "earth_165_density.png");
            if (!groundTruthFile.exists()) return;

            BufferedImage groundTruth = ImageIO.read(groundTruthFile);

            Scenario sc = null;
            for (Scenario s : Scenario.getBuiltInScenarios()) {
                if ("antonine_plague_165".equals(s.getPresetKey())) {
                    sc = s;
                    break;
                }
            }
            if (sc == null) return;

            sc.setH3Resolution(6);
            sc.setEndDateYear(166);
            SimulationRunRecord run = HeadlessBatchRunner.executeScenarioHeadless(sc);

            List<H3Cell> snapshotCells = run.getSpatialSnapshotAt(166);
            if (snapshotCells == null || snapshotCells.isEmpty()) return;

            BufferedImage simRaster = SpatialTensorRasterRenderer.rasterizeCellsToImage(snapshotCells, "density", 512, 256);
            MapComparisonMetrics.MapComparisonResult comp = sc.isClippingEnabled()
                    ? MapComparisonMetrics.compareImagesWindowed(simRaster, groundTruth, sc.getMinLat(), sc.getMaxLat(), sc.getMinLng(), sc.getMaxLng())
                    : MapComparisonMetrics.compareImages(simRaster, groundTruth);

            String diag = comp.getPearsonR() >= 0.75 ? "Mediterranean Basin Coherent" : "Levant Disconnection";
            results.add(new SpatialAuditEntry(
                    "Peste Antonine (165 CE)", "Res 6", 165,
                    comp.getSsim(), comp.getPearsonR(), comp.getJaccardIndex(), comp.getRmse(),
                    comp.getMaxDeltaLat(), comp.getMaxDeltaLng(), comp.getMaxDeltaValue(),
                    diag
            ));
        } catch (Exception e) {
            System.err.println("Error in Antonine audit: " + e.getMessage());
        }
    }

    private void auditBlackDeath(List<SpatialAuditEntry> results) {
        try {
            File epochDir = new File("data/ether/earth/1347");
            File groundTruthFile = new File(epochDir, "earth_1347_density.png");
            if (!groundTruthFile.exists()) return;

            BufferedImage groundTruth = ImageIO.read(groundTruthFile);

            Scenario sc = null;
            for (Scenario s : Scenario.getBuiltInScenarios()) {
                if ("black_death_1347".equals(s.getPresetKey())) {
                    sc = s;
                    break;
                }
            }
            if (sc == null) return;

            sc.setH3Resolution(3);
            sc.setEndDateYear(1348);
            SimulationRunRecord run = HeadlessBatchRunner.executeScenarioHeadless(sc);

            List<H3Cell> snapshotCells = run.getSpatialSnapshotAt(1348);
            if (snapshotCells == null || snapshotCells.isEmpty()) return;

            BufferedImage simRaster = SpatialTensorRasterRenderer.rasterizeCellsToImage(snapshotCells, "density", 512, 256);
            MapComparisonMetrics.MapComparisonResult comp = MapComparisonMetrics.compareImages(simRaster, groundTruth);

            String diag = comp.getPearsonR() >= 0.85 ? "Eurasian Urban Density Aligned" : "Trade Route Discontinuity";
            results.add(new SpatialAuditEntry(
                    "Peste Noire (1347 CE)", "Res 3", 1347,
                    comp.getSsim(), comp.getPearsonR(), comp.getJaccardIndex(), comp.getRmse(),
                    comp.getMaxDeltaLat(), comp.getMaxDeltaLng(), comp.getMaxDeltaValue(),
                    diag
            ));
        } catch (Exception e) {
            System.err.println("Error in Black Death audit: " + e.getMessage());
        }
    }

    private void auditWorldWars1914(List<SpatialAuditEntry> results) {
        try {
            File epochDir = new File("data/ether/earth/1914");
            File groundTruthFile = new File(epochDir, "earth_1914_density.png");
            if (!groundTruthFile.exists()) return;

            BufferedImage groundTruth = ImageIO.read(groundTruthFile);

            Scenario sc = null;
            for (Scenario s : Scenario.getBuiltInScenarios()) {
                if ("world_wars_totalitarian_1914".equals(s.getPresetKey())) {
                    sc = s;
                    break;
                }
            }
            if (sc == null) return;

            sc.setH3Resolution(3);
            sc.setEndDateYear(1915);
            SimulationRunRecord run = HeadlessBatchRunner.executeScenarioHeadless(sc);

            List<H3Cell> snapshotCells = run.getSpatialSnapshotAt(1915);
            if (snapshotCells == null || snapshotCells.isEmpty()) return;

            BufferedImage simRaster = SpatialTensorRasterRenderer.rasterizeCellsToImage(snapshotCells, "density", 512, 256);
            MapComparisonMetrics.MapComparisonResult comp = MapComparisonMetrics.compareImages(simRaster, groundTruth);

            String diag = comp.getPearsonR() >= 0.85 ? "Industrial Demographic Core Aligned" : "Western Front Over-attrition";
            results.add(new SpatialAuditEntry(
                    "Guerres Mondiales (1914 CE)", "Res 3", 1914,
                    comp.getSsim(), comp.getPearsonR(), comp.getJaccardIndex(), comp.getRmse(),
                    comp.getMaxDeltaLat(), comp.getMaxDeltaLng(), comp.getMaxDeltaValue(),
                    diag
            ));
        } catch (Exception e) {
            System.err.println("Error in 1914 audit: " + e.getMessage());
        }
    }
}
