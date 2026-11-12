/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Biome;
import org.ether.society.ui.util.ScenarioCellPopulationDistributor;

import java.util.List;
import java.util.Locale;

/**
 * Helper delegate responsible for evaluating H3 cell colors, rendering hypsometric cell meshes,
 * and computing live statistical diagnostics for {@link ScenarioPreviewPaneBuilder}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
final class ScenarioPreviewCellColorHelper {

    private ScenarioPreviewCellColorHelper() {
        // Utility delegate class
    }

    /**
     * Evaluates the display color for an {@link H3Cell} under the selected preview mode.
     *
     * @param c       target H3 hexagonal cell
     * @param idx     selected index in the preview mode combo box
     * @param mode    lowercase label of the selected preview mode
     * @param context host preview context
     * @return computed JavaFX {@link Color}
     */
    static Color getPreviewColorForCell(
            H3Cell c,
            int idx,
            String mode,
            ScenarioPreviewPaneBuilder.HostContext context) {
        if (c == null) return Color.BLACK;

        double lat = c.getLatitude() != null ? c.getLatitude() : 0.0;
        double lon = c.getLongitude() != null ? c.getLongitude() : 0.0;
        double elev = c.getElevation() != null ? c.getElevation() : 0.0;
        boolean isLand = isLandCell(c, elev);

        if (idx == 0) {
            if (context.isDensityImportActive()) {
                Image customDensityImage = context.getCustomDensityImage();
                if (customDensityImage != null && customDensityImage.getWidth() > 0) {
                    if (!isLand) return ScenarioCellPopulationDistributor.getReliefAndDensityColor(c);
                    Color customCol = ScenarioPreviewRasterRenderer.sampleImageColorAtLatLon(customDensityImage, lat, lon);
                    if (customCol != null) {
                        double brightness = (customCol.getRed() + customCol.getGreen() + customCol.getBlue()) / 3.0;
                        if (Math.abs(customCol.getRed() - customCol.getGreen()) < 0.02
                                && Math.abs(customCol.getGreen() - customCol.getBlue()) < 0.02) {
                            Color heatCol;
                            if (brightness <= 0.02) heatCol = Color.rgb(30, 95, 165);
                            else if (brightness < 0.20) heatCol = Color.rgb(16, 185, 129);
                            else if (brightness < 0.45) heatCol = Color.rgb(234, 179, 8);
                            else if (brightness < 0.75) heatCol = Color.rgb(249, 115, 22);
                            else heatCol = Color.rgb(239, 68, 68);
                            return ScenarioPreviewRasterRenderer.blendColors(
                                    ScenarioCellPopulationDistributor.getReliefAndDensityColor(c),
                                    heatCol,
                                    Math.max(0.65, brightness));
                        }
                        return customCol;
                    }
                }
                return ScenarioCellPopulationDistributor.getReliefAndDensityColor(c);
            }
            return ScenarioCellPopulationDistributor.getReliefAndDensityColor(c);
        }

        if (!isLand) {
            return ScenarioCellPopulationDistributor.getReliefAndDensityColor(c);
        }

        int dims = context.getCultureVectorDimensions();
        if (idx >= 1 && idx <= dims) {
            int tIndex = idx - 1;
            if (context.isTensorImportActive(tIndex)) {
                Image img = context.getCustomTensorImage(tIndex);
                if (img != null && img.getWidth() > 0) {
                    Color customCol = ScenarioPreviewRasterRenderer.sampleImageColorAtLatLon(img, lat, lon);
                    if (customCol != null) return customCol;
                }
                return ScenarioCellPopulationDistributor.getReliefAndDensityColor(c);
            }
            return context.evaluateProceduralTensorColor(
                    tIndex, lat, lon, elev,
                    context.getTensorParam1(tIndex),
                    context.getTensorParam2(tIndex),
                    context.getTensorParam3(tIndex),
                    context.getTensorSeed(tIndex));
        }

        int derivedOffset = idx - (dims + 1);
        if (derivedOffset == 1 || mode.contains("k(x)")) {
            double cap = c.getResourceCapital() != null ? c.getResourceCapital() : 0.0;
            double norm = Math.clamp(cap / 500000.0, 0.0, 1.0);
            return Color.hsb((1.0 - norm) * 240.0, 0.85, 0.90);
        } else if (derivedOffset == 2 || mode.contains("e(x)")) {
            double energy = c.getEnergyFire() != null ? c.getEnergyFire() : 0.0;
            double norm = Math.clamp(energy / 1000000.0, 0.0, 1.0);
            return Color.hsb(30.0 + norm * 30.0, 0.90, 0.95);
        } else if (derivedOffset == 3 || mode.contains("f(x)")) {
            double food = c.getFoodResource() != null ? c.getFoodResource() : 0.0;
            double norm = Math.clamp(food / 100000.0, 0.0, 1.0);
            return Color.hsb(120.0, 0.70 + norm * 0.30, 0.60 + norm * 0.35);
        } else if (derivedOffset == 4 || mode.contains("i(x)")) {
            double tech = org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c);
            double norm = Math.clamp(tech / 10.0, 0.0, 1.0);
            return Color.hsb(270.0 + norm * 60.0, 0.85, 0.90);
        } else if (derivedOffset == 5 || mode.contains("malthus")) {
            double capK0 = context.computeAutoCapitalFromYear(context.getStartYear());
            double cap = ScenarioCellPopulationDistributor.computeCellCarryingCapacity(c, capK0);
            long pop = c.getPopulation() != null ? c.getPopulation() : 0;
            double ratio = Math.clamp(pop / Math.max(1.0, cap), 0.0, 1.5);
            return Color.hsb((1.0 - Math.min(1.0, ratio)) * 120.0, 0.85, 0.90);
        } else if (derivedOffset == 6 || mode.contains("friction")) {
            double fric = c.getMovementFriction() != null ? c.getMovementFriction() : 1.0;
            double norm = Math.clamp((fric - 1.0) / 4.0, 0.0, 1.0);
            return Color.rgb((int) (norm * 255), (int) ((1.0 - norm) * 200), 50);
        }

        return ScenarioCellPopulationDistributor.getReliefAndDensityColor(c);
    }

    /**
     * Renders the H3 cell mesh with optional hypsometric shading and coastal/datum highlights onto the canvas.
     *
     * @param gc                  canvas graphics context
     * @param w                   canvas width in pixels
     * @param h                   canvas height in pixels
     * @param scale               pixels-per-degree projection scale
     * @param offX                horizontal pan offset
     * @param offY                vertical pan offset
     * @param zoomFactor          camera zoom factor
     * @param currentPreviewCells active list of preview H3 cells
     * @param idx                 selected preview mode index
     * @param mode                lowercase preview mode label
     * @param isReliefActive      whether hypsometric relief shading is enabled
     * @param context             host preview context
     */
    static void drawCellMesh(
            GraphicsContext gc,
            double w,
            double h,
            double scale,
            double offX,
            double offY,
            double zoomFactor,
            List<H3Cell> currentPreviewCells,
            int idx,
            String mode,
            boolean isReliefActive,
            ScenarioPreviewPaneBuilder.HostContext context) {
        double minLat = -90, maxLat = 90;
        double minLng = -180, maxLng = 180;

        double cellCount = currentPreviewCells.size();
        double areaSqDeg = (maxLat - minLat) * (maxLng - minLng);
        double avgAreaPerCell = areaSqDeg / Math.max(1.0, cellCount);
        double dynamicCellDeg = Math.sqrt(avgAreaPerCell) * 1.08;
        double cellSize = Math.max(1.5, dynamicCellDeg * scale);

        PlanetPreset activePlanet = context.getActivePlanetPreset() != null
                ? context.getActivePlanetPreset()
                : PlanetPreset.EARTH_LIKE;
        boolean hasOcean = activePlanet.waterLevel() > -0.4;

        for (H3Cell c : currentPreviewCells) {
            double x = (c.getLongitude() - minLng) * scale + offX;
            double y = (maxLat - c.getLatitude()) * scale + offY;

            if (x < -cellSize || x > w + cellSize || y < -cellSize || y > h + cellSize) continue;

            Color col = getPreviewColorForCell(c, idx, mode, context);
            boolean isCoastalCell = Boolean.TRUE.equals(c.getIsCoastal());
            double elev = c.getElevation() != null ? c.getElevation() : 0.0;
            boolean isDatumCell = !hasOcean && Math.abs(elev) < 350.0;

            if (isReliefActive) {
                double mult;
                if (hasOcean && elev <= 0) {
                    double depthRatio = Math.clamp(Math.abs(elev) / 4000.0, 0.0, 1.0);
                    mult = 0.85 + 0.15 * (1.0 - depthRatio);
                } else {
                    double decl = c.getMovementFriction() != null ? Math.max(0.0, c.getMovementFriction() - 1.0) : 0.0;
                    double normElev = Math.clamp(elev / 4000.0, 0.0, 1.0);
                    mult = 0.75 + 0.30 * normElev + 0.15 * Math.min(1.0, decl);
                }
                col = Color.color(
                        Math.clamp(col.getRed() * mult, 0.0, 1.0),
                        Math.clamp(col.getGreen() * mult, 0.0, 1.0),
                        Math.clamp(col.getBlue() * mult, 0.0, 1.0));
            }
            gc.setFill(col);
            double absLat = Math.abs(c.getLatitude());
            double latDistort = Math.cos(Math.toRadians(absLat));
            double cellW = Math.max(1.2, cellSize / Math.max(0.18, latDistort));
            double cellH = Math.max(1.2, cellSize * 1.05);

            gc.fillRect(x - cellW * 0.5, y - cellH * 0.5, cellW + 0.6, cellH + 0.6);

            if (isCoastalCell && hasOcean) {
                gc.setStroke(Color.rgb(254, 240, 138, 0.85));
                gc.setLineWidth(Math.max(0.5, 1.0 * zoomFactor));
                gc.strokeLine(x - cellW * 0.5, y, x + cellW * 0.5, y);
            } else if (isDatumCell) {
                gc.setStroke(Color.rgb(251, 191, 36, 0.85));
                gc.setLineWidth(Math.max(0.5, 0.8 * zoomFactor));
                gc.strokeLine(x - cellW * 0.4, y, x + cellW * 0.4, y);
            }
        }
    }

    /**
     * Updates the status label below the preview canvas with spatial summary statistics for the active layer.
     *
     * @param previewStatusLabel  target JavaFX status label
     * @param currentPreviewCells active list of H3 cells
     * @param curSel              selected preview mode index
     * @param layerName           raw selected preview mode string
     * @param context             host preview context
     */
    static void updatePreviewDiagnostics(
            Label previewStatusLabel,
            List<H3Cell> currentPreviewCells,
            int curSel,
            String layerName,
            ScenarioPreviewPaneBuilder.HostContext context) {
        if (previewStatusLabel == null || currentPreviewCells == null || currentPreviewCells.isEmpty()) return;

        int dims = context.getCultureVectorDimensions();
        String modeLower = layerName != null ? layerName.toLowerCase() : "";

        if (curSel == 0) {
            long totalHuman = 0;
            double maxDens = 0.0;
            int countLand = 0;
            for (H3Cell c : currentPreviewCells) {
                if (c == null) continue;
                long pop = c.getPopulation() != null ? c.getPopulation() : 0;
                totalHuman += pop;
                if (pop > maxDens) maxDens = pop;
                if (isLandCell(c, c.getElevation() != null ? c.getElevation() : 0.0)) {
                    countLand++;
                }
            }
            double avgPerLand = countLand > 0 ? (double) totalHuman / countLand : 0;
            previewStatusLabel.setText(String.format(
                    I18n.getOrDefault("scenario.map_info.demography",
                            "👥 Pop. globale : %s hab.  |  Moyenne/cellule : %.0f  |  Max locale : %s  |  Terres émergées : %d"),
                    String.format(Locale.FRANCE, "%,d", totalHuman),
                    avgPerLand,
                    String.format(Locale.FRANCE, "%,.0f", maxDens),
                    countLand));
        } else if (curSel >= 1 && curSel <= dims) {
            int tIndex = curSel - 1;
            String modeStr = context.getCulturalTensorTitle(tIndex);
            double diff = context.getCulturalDiffusionRate();
            double mut = context.getCulturalMutationRate();

            double sumVal = 0.0;
            double sumSq = 0.0;
            double minVal = Double.MAX_VALUE;
            double maxVal = -Double.MAX_VALUE;
            int count = 0;

            for (H3Cell c : currentPreviewCells) {
                if (c == null) continue;
                if (isLandCell(c, c.getElevation() != null ? c.getElevation() : 0.0)) {
                    Color col = getPreviewColorForCell(c, curSel, modeLower, context);
                    double val = (col.getRed() + col.getGreen() + col.getBlue()) / 3.0;
                    sumVal += val;
                    sumSq += val * val;
                    if (val < minVal) minVal = val;
                    if (val > maxVal) maxVal = val;
                    count++;
                }
            }
            double mean = count > 0 ? (sumVal / count) : 0.5;
            double variance = count > 1 ? Math.max(0.0, (sumSq - (sumVal * sumVal) / count) / (count - 1)) : 0.0;
            double stdDev = Math.sqrt(variance);
            if (count == 0) {
                minVal = 0.0;
                maxVal = 1.0;
            }

            previewStatusLabel.setText(String.format(
                    I18n.getOrDefault("scenario.map_info.tensor",
                            "⚙️ Mode : %s  |  📊 Dispersion : μ=%.2f ± %.2f [min=%.2f, max=%.2f]  |  🌀 Diffusion D=%.3f  |  🎲 Bruit=%.3f"),
                    modeStr, mean, stdDev, minVal, maxVal, diff, mut));
        } else {
            int derivedOffset = curSel - (dims + 1);
            String safeName = layerName != null ? layerName : "";
            double capK0 = context.computeAutoCapitalFromYear(context.getStartYear());
            double sum = 0.0;
            int count = 0;
            for (H3Cell c : currentPreviewCells) {
                if (c == null) continue;
                if (isLandCell(c, c.getElevation() != null ? c.getElevation() : 0.0)) {
                    count++;
                    if (derivedOffset == 1 || safeName.contains("K(x)")) {
                        sum += (c.getResourceCapital() != null ? c.getResourceCapital() : 0.0);
                    } else if (derivedOffset == 2 || safeName.contains("E(x)")) {
                        sum += (c.getEnergyFire() != null ? c.getEnergyFire() : 0.0);
                    } else if (derivedOffset == 3 || safeName.contains("F(x)")) {
                        sum += (c.getFoodResource() != null ? c.getFoodResource() : 0.0);
                    } else if (derivedOffset == 4 || safeName.contains("I(x)")) {
                        sum += org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c);
                    } else if (derivedOffset == 6 || safeName.contains("friction")) {
                        sum += (c.getMovementFriction() != null ? c.getMovementFriction() : 1.0);
                    } else {
                        sum += ScenarioCellPopulationDistributor.computeCellCarryingCapacity(c, capK0);
                    }
                }
            }
            double mean = count > 0 ? (sum / count) : 0.0;
            previewStatusLabel.setText(String.format(
                    I18n.getOrDefault("scenario.map_info.derived",
                            "📊 Moyenne : %.2f  |  ⚡ Stock global : %s  |  📍 Cellules actives : %s"),
                    mean,
                    String.format(Locale.FRANCE, "%,.0f", sum),
                    String.format(Locale.FRANCE, "%,d", count)));
        }
    }

    private static boolean isLandCell(H3Cell c, double elev) {
        return elev > 0.0 || (c.getBiome() != null && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN);
    }
}
