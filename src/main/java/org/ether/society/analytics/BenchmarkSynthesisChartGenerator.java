/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static org.ether.society.analytics.BenchmarkChartStylingHelper.*;

/**
 * <h1>BenchmarkSynthesisChartGenerator</h1>
 * <p>
 * High-DPI scientific chart generator producing publication-grade visual synthesis figures
 * across all 45 historical, paleoclimatic, prospective, and planetary scenarios, all 27
 * canonical telemetry variables, 2D cartographic density and 9D geocultural tensor rasters,
 * and the 7 Epistemological Validation Pillars.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see BenchmarkChartStylingHelper
 * @see BenchmarkChartSeriesBuilder
 * @see BenchmarkSecondaryChartsRenderer
 */
public final class BenchmarkSynthesisChartGenerator {

    private BenchmarkSynthesisChartGenerator() {
        // Utility class
    }

    /**
     * Data record for a single scenario point on the 45-scenario fidelity curve.
     */
    public record ScenarioFidelityPoint(
            int index,
            String shortLabel,
            int startYear,
            double fidelityPct,
            double spatialR,
            int domainGroup
    ) {}

    /**
     * Data record for one of the 27 canonical benchmark telemetry variables.
     */
    public record VariableQualityEntry(
            String shortName,
            String key,
            int tierFamily,
            double meanFidelity,
            double q1Fidelity,
            double q3Fidelity
    ) {}

    /**
     * Generates all 6 synthesis charts into the specified directory.
     *
     * @param outputDir target output directory for generated PNG charts
     * @throws IOException if image writing fails
     */
    public static void generateAllSynthesisCharts(File outputDir) throws IOException {
        if (outputDir == null) {
            outputDir = new File("docs/images/charts");
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        generateChart1All45ScenariosFidelityCurve(new File(outputDir, "chart1_45_scenarios_fidelity_curve.png"));
        generateChart2VariablesQualityProfile(new File(outputDir, "chart2_27_variables_quality_profile.png"));
        generateChart3Cartographic2DQualityCurves(new File(outputDir, "chart3_cartographic_2d_quality_curves.png"));
        generateChart4MultiScenarioHeatmapMatrix(new File(outputDir, "chart4_multi_scenario_heatmap_matrix.png"));
        generateChart5Pillars6And7Summary(new File(outputDir, "chart5_pillars_6_and_7_validation_summary.png"));
        generateChart6EngineScalingAndRoiPareto(new File(outputDir, "chart6_engine_scaling_and_roi_pareto.png"));
    }

    public static double computeContinuousTolerancePct(double year) {
        double tauPaleo = 50.0;
        double tauMod = 5.0;
        return tauMod + (tauPaleo - tauMod) / (1.0 + Math.exp((year - 1200.0) / 300.0));
    }

    public static void generateChart1All45ScenariosFidelityCurve(File outFile) throws IOException {
        int width = 1680;
        int height = 860;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = createAntiAliasedGraphics(img, width, height);

        drawHeader(g, width,
                "1. MASTER 45-SCENARIO CHRONOLOGICAL FIDELITY CURVE & CONTINUOUS CONFIDENCE ENVELOPE",
                "Fidelity Score (100% - MAPE) & Spatial Pearson Correlation (r) Across All Scenarios Plotted Against Continuous C1 Tolerance Floor");

        List<ScenarioFidelityPoint> pts = BenchmarkChartSeriesBuilder.build45ScenarioPoints();
        int plotX = 80;
        int plotY = 120;
        int plotW = width - 160;
        int plotH = height - 240;

        g.setColor(PANEL_BG);
        g.fill(new RoundRectangle2D.Double(plotX, plotY, plotW, plotH, 12, 12));
        g.setColor(GRID_LINE);
        g.draw(new RoundRectangle2D.Double(plotX, plotY, plotW, plotH, 12, 12));

        drawDomainBand(g, plotX, plotY, plotW, plotH, 0, 6, 45, new Color(88, 166, 255, 20), "PALEOLITHIC (-100k to -10k BP)");
        drawDomainBand(g, plotX, plotY, plotW, plotH, 6, 22, 45, new Color(63, 185, 80, 20), "HOLOCENE, ANTIQUITY & MEDIEVAL (-8k to 1206 CE)");
        drawDomainBand(g, plotX, plotY, plotW, plotH, 22, 30, 45, new Color(210, 153, 34, 20), "LATE MEDIEVAL & MODERN (1324 to 2000 CE)");
        drawDomainBand(g, plotX, plotY, plotW, plotH, 30, 45, 45, new Color(188, 140, 255, 20), "PROSPECTIVE SSPS & PLANETARY FRONTIERS");

        for (int pct = 50; pct <= 100; pct += 10) {
            int y = plotY + plotH - (int) ((pct - 50.0) / 50.0 * plotH);
            g.setColor(GRID_LINE);
            g.drawLine(plotX, y, plotX + plotW, y);
            g.setColor(TEXT_MUTED);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString(pct + "%", plotX - 42, y + 4);
        }

        double stepX = plotW / (double) pts.size();
        Path2D.Double tolerancePath = new Path2D.Double();
        Path2D.Double fidelityPath = new Path2D.Double();

        for (int i = 0; i < pts.size(); i++) {
            ScenarioFidelityPoint pt = pts.get(i);
            double x = plotX + (i + 0.5) * stepX;
            double tau = computeContinuousTolerancePct(pt.startYear());
            double floorFidelity = 100.0 - tau;
            double yFloor = plotY + plotH - ((floorFidelity - 50.0) / 50.0 * plotH);
            double yFid = plotY + plotH - ((pt.fidelityPct() - 50.0) / 50.0 * plotH);

            if (i == 0) {
                tolerancePath.moveTo(x, yFloor);
                fidelityPath.moveTo(x, yFid);
            } else {
                tolerancePath.lineTo(x, yFloor);
                fidelityPath.lineTo(x, yFid);
            }

            g.setColor(ACCENT_EMERALD);
            g.fillOval((int) x - 4, (int) yFid - 4, 8, 8);
        }

        g.setColor(ACCENT_CORAL);
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6f, 4f}, 0));
        g.draw(tolerancePath);

        g.setColor(ACCENT_EMERALD);
        g.setStroke(new BasicStroke(2.4f));
        g.draw(fidelityPath);

        int legY = height - 45;
        drawLegendItem(g, 120, legY, ACCENT_EMERALD, "Empirical Fidelity Score Fk = 100% - MAPEk");
        drawLegendItem(g, 550, legY, ACCENT_CORAL, "Continuous Epistemic Confidence Floor Fmin(t) = 100% - tau(t)");

        g.dispose();
        ImageIO.write(img, "PNG", outFile);
    }

    public static void generateChart2VariablesQualityProfile(File outFile) throws IOException {
        int width = 1680;
        int height = 980;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = createAntiAliasedGraphics(img, width, height);

        drawHeader(g, width,
                "2. QUALITY PROFILE OF ALL 27 CANONICAL STATE VARIABLES (TIER 1 & TIER 2)",
                "Mean Empirical Fidelity (100% - MAPE), Inter-Scenario Interquartile Spread [Q1, Q3] & Ontological Tier Grouping Across Historical Runs");

        List<VariableQualityEntry> vars = BenchmarkChartSeriesBuilder.build27VariableQualityEntries();
        int plotX = 390;
        int plotY = 115;
        int plotW = width - 575;
        int plotH = 780;

        g.setColor(PANEL_BG);
        g.fill(new RoundRectangle2D.Double(plotX, plotY, plotW, plotH, 12, 12));
        g.setColor(GRID_LINE);
        g.draw(new RoundRectangle2D.Double(plotX, plotY, plotW, plotH, 12, 12));

        for (int pct = 50; pct <= 100; pct += 10) {
            int x = plotX + (int) ((pct - 50.0) / 50.0 * plotW);
            g.setColor(GRID_LINE);
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4f, 4f}, 0));
            g.drawLine(x, plotY, x, plotY + plotH);
            g.setColor(TEXT_MUTED);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString(pct + "% Fidelity (" + (100 - pct) + "% MAPE)", x - 55, plotY + plotH + 22);
        }

        int targetX = plotX + (int) ((85.0 - 50.0) / 50.0 * plotW);
        g.setColor(ACCENT_AMBER);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6f, 4f}, 0));
        g.drawLine(targetX, plotY, targetX, plotY + plotH);
        g.drawString("Modern/Historical Target (>=85%)", targetX - 95, plotY - 8);

        int n = vars.size();
        double rowH = plotH / (double) n;
        Path2D.Double profileCurve = new Path2D.Double();

        for (int i = 0; i < n; i++) {
            VariableQualityEntry v = vars.get(i);
            int cy = (int) (plotY + (i + 0.5) * rowH);
            int barH = (int) Math.max(12, rowH * 0.62);

            Color famColor = switch (v.tierFamily()) {
                case 0 -> ACCENT_CYAN;
                case 1 -> ACCENT_EMERALD;
                case 2 -> ACCENT_AMBER;
                default -> ACCENT_VIOLET;
            };

            if (i % 2 == 0) {
                g.setColor(new Color(255, 255, 255, 6));
                g.fillRect(plotX + 1, (int) (plotY + i * rowH), plotW - 2, (int) rowH);
            }

            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(TEXT_BRIGHT);
            g.drawString(String.format("%02d. %s", i + 1, v.shortName()), 28, cy + 4);

            int barW = (int) ((Math.max(50.0, v.meanFidelity()) - 50.0) / 50.0 * plotW);
            g.setColor(new Color(famColor.getRed(), famColor.getGreen(), famColor.getBlue(), 145));
            g.fill(new RoundRectangle2D.Double(plotX + 2, cy - barH / 2.0, barW, barH, 6, 6));
            g.setColor(famColor);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new RoundRectangle2D.Double(plotX + 2, cy - barH / 2.0, barW, barH, 6, 6));

            int xQ1 = plotX + (int) ((Math.max(50.0, v.q1Fidelity()) - 50.0) / 50.0 * plotW);
            int xQ3 = plotX + (int) ((Math.min(100.0, v.q3Fidelity()) - 50.0) / 50.0 * plotW);
            g.setColor(TEXT_BRIGHT);
            g.setStroke(new BasicStroke(2.0f));
            g.drawLine(xQ1, cy, xQ3, cy);
            g.drawLine(xQ1, cy - 5, xQ1, cy + 5);
            g.drawLine(xQ3, cy - 5, xQ3, cy + 5);

            int xMean = plotX + barW;
            if (i == 0) profileCurve.moveTo(xMean, cy);
            else profileCurve.lineTo(xMean, cy);

            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.setColor(TEXT_BRIGHT);
            double mape = 100.0 - v.meanFidelity();
            g.drawString(String.format(Locale.US, "%.1f%% (MAPE %.1f%%)", v.meanFidelity(), mape),
                    plotX + plotW + 14, cy + 4);
        }

        g.setColor(ACCENT_TEAL);
        g.setStroke(new BasicStroke(2.2f));
        g.draw(profileCurve);

        int legY = height - 35;
        drawLegendItem(g, 95, legY, ACCENT_CYAN, "Tier 1: Orbital, Climate & Geochemical PDEs");
        drawLegendItem(g, 460, legY, ACCENT_EMERALD, "Tier 1/2: Demographics, Biosphere & Agro-Ecology");
        drawLegendItem(g, 870, legY, ACCENT_AMBER, "Tier 2: Macroeconomics, Exergy & Maritime Trade");
        drawLegendItem(g, 1280, legY, ACCENT_VIOLET, "Tier 2: Cliodynamics, Institutions & Culture");

        g.dispose();
        ImageIO.write(img, "PNG", outFile);
    }

    public static void generateChart3Cartographic2DQualityCurves(File outFile) throws IOException {
        int width = 1680;
        int height = 800;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = createAntiAliasedGraphics(img, width, height);

        drawHeader(g, width,
                "3. 2D CARTOGRAPHIC RASTER FIDELITY & 9-DIMENSIONAL GEOCULTURAL TENSOR QUALITY CURVES",
                "Left: Multi-Epoch Evolution of 2D Population Density Maps (Pearson r, SSIM, Jaccard IoU) | Right: 9 Orthogonal Geocultural Tensor Channels");

        int p1X = 85, p1Y = 120, p1W = 735, p1H = 540;
        g.setColor(PANEL_BG);
        g.fill(new RoundRectangle2D.Double(p1X, p1Y, p1W, p1H, 12, 12));
        g.setColor(GRID_LINE);
        g.draw(new RoundRectangle2D.Double(p1X, p1Y, p1W, p1H, 12, 12));

        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(TEXT_BRIGHT);
        g.drawString("A. 2D Demographic Density Raster Fidelity Across Historical Epochs", p1X + 18, p1Y + 28);

        double[] pearsonR = {0.680, 0.795, 0.742, 0.695, 0.589, 0.581, 0.582, 0.586, 0.712, 0.611, 0.734, 0.892, 0.648, 0.654};
        double[] ssimVal  = {0.745, 0.835, 0.684, 0.662, 0.521, 0.506, 0.525, 0.528, 0.645, 0.537, 0.688, 0.841, 0.552, 0.562};
        double[] iouVal   = {0.710, 0.656, 0.612, 0.585, 0.512, 0.487, 0.493, 0.491, 0.594, 0.541, 0.625, 0.785, 0.531, 0.623};

        int g1X = p1X + 55, g1Y = p1Y + 55, g1W = p1W - 80, g1H = p1H - 155;
        for (int pct = 30; pct <= 100; pct += 10) {
            int y = g1Y + g1H - (int) ((pct - 30.0) / 70.0 * g1H);
            g.setColor(GRID_LINE);
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4f, 4f}, 0));
            g.drawLine(g1X, y, g1X + g1W, y);
            g.setColor(TEXT_MUTED);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.drawString(String.format(Locale.US, "%.2f", pct / 100.0), g1X - 38, y + 4);
        }

        drawMetricCurve(g, g1X, g1Y, g1W, g1H, pearsonR, 0.30, 1.00, ACCENT_CYAN);
        drawMetricCurve(g, g1X, g1Y, g1W, g1H, ssimVal, 0.30, 1.00, ACCENT_EMERALD);
        drawMetricCurve(g, g1X, g1Y, g1W, g1H, iouVal, 0.30, 1.00, ACCENT_AMBER);

        g.dispose();
        ImageIO.write(img, "PNG", outFile);
    }

    public static void generateChart4MultiScenarioHeatmapMatrix(File outFile) throws IOException {
        BenchmarkSecondaryChartsRenderer.generateChart4MultiScenarioHeatmapMatrix(outFile);
    }

    public static void generateChart5Pillars6And7Summary(File outFile) throws IOException {
        BenchmarkSecondaryChartsRenderer.generateChart5Pillars6And7Summary(outFile);
    }

    public static void generateChart6EngineScalingAndRoiPareto(File outFile) throws IOException {
        BenchmarkSecondaryChartsRenderer.generateChart6EngineScalingAndRoiPareto(outFile);
    }
}
