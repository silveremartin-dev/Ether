package org.ether.society.persistence;

import org.ether.society.analytics.HistoryManager;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.ui.DisplayMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * High-Resolution 2D Dynamic Territory Atlas Video & Animated GIF Exporter.
 * Renders multi-layer spatiotemporal historical snapshots into high-fidelity animated media assets
 * with telemetry HUD overlays (Simulation Step, Historical Year, Moran's I Autocorrelation, Layer Legend).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public class AtlasVideoExporter {
    private static final Logger logger = LoggerFactory.getLogger(AtlasVideoExporter.class);
    private static final String EXPORT_DIR = "saves/exports";

    private static final ExecutorService EXPORT_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Ether-AtlasVideoExport-Worker");
        t.setDaemon(true);
        return t;
    });

    /**
     * Asynchronously export the 2D Dynamic Atlas animation into an Animated GIF / Video file.
     *
     * @param historyManager The simulation history manager with recorded snapshots
     * @param activeLayers   The multi-layer stack of active display modes
     * @param startIndex     Starting snapshot index (0-based)
     * @param endIndex       Ending snapshot index (0-based)
     * @param frameDelayMs   Delay between frames in milliseconds (e.g. 150ms)
     * @param targetFile     Target destination file (or null for default auto-named path)
     * @param scenarioName   Name of active scenario for metadata
     * @param progressCb     Optional progress callback (0.0 to 1.0)
     * @return CompletableFuture completing with the generated video File
     */
    public static CompletableFuture<File> exportVideoAsync(
            HistoryManager historyManager,
            Set<DisplayMode> activeLayers,
            int startIndex,
            int endIndex,
            int frameDelayMs,
            File targetFile,
            String scenarioName,
            Consumer<Double> progressCb
    ) {
        CompletableFuture<File> future = new CompletableFuture<>();

        if (historyManager == null || historyManager.getWorldSnapshots().isEmpty()) {
            future.completeExceptionally(new IllegalStateException("No historical snapshots available for video export."));
            return future;
        }

        NavigableMap<Long, List<H3Cell>> snapshots = historyManager.getWorldSnapshots();
        List<Long> tickKeys = new ArrayList<>(snapshots.keySet());

        int effectiveStart = Math.clamp(startIndex, 0, tickKeys.size() - 1);
        int effectiveEnd = Math.clamp(endIndex, effectiveStart, tickKeys.size() - 1);
        int totalFrames = (effectiveEnd - effectiveStart) + 1;

        if (totalFrames <= 0) {
            future.completeExceptionally(new IllegalArgumentException("Invalid snapshot range for export."));
            return future;
        }

        EXPORT_EXECUTOR.submit(() -> {
            try {
                Path exportDir = Paths.get(EXPORT_DIR);
                Files.createDirectories(exportDir);

                File outFile = targetFile;
                if (outFile == null) {
                    String safeScenario = (scenarioName != null && !scenarioName.isBlank())
                            ? scenarioName.replaceAll("[^a-zA-Z0-9_\\-]", "_")
                            : "Atlas";
                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    outFile = exportDir.resolve("AtlasVideo_" + safeScenario + "_" + timestamp + ".gif").toFile();
                }

                logger.info("🎬 Exporting Atlas Dynamic Video to {} ({} frames, delay {}ms)...",
                        outFile.getAbsolutePath(), totalFrames, frameDelayMs);

                int width = 800;
                int height = 440;

                try (ImageOutputStream ios = new FileImageOutputStream(outFile);
                     GifSequenceWriter gifWriter = new GifSequenceWriter(ios, BufferedImage.TYPE_INT_RGB, frameDelayMs, true)) {

                    for (int frameIdx = 0; frameIdx < totalFrames; frameIdx++) {
                        int currentSnapIdx = effectiveStart + frameIdx;
                        long stepKey = tickKeys.get(currentSnapIdx);
                        List<H3Cell> cells = snapshots.get(stepKey);

                        BufferedImage frame = renderFrame(width, height, cells, activeLayers, stepKey, currentSnapIdx + 1, tickKeys.size());
                        gifWriter.writeToSequence(frame);

                        if (progressCb != null) {
                            double progress = (double) (frameIdx + 1) / totalFrames;
                            progressCb.accept(progress);
                        }

                        if ((frameIdx + 1) % 25 == 0 || frameIdx == totalFrames - 1) {
                            logger.info("  Atlas video frame {}/{} encoded (Pas {})...", frameIdx + 1, totalFrames, stepKey);
                        }
                    }
                }

                logger.info("✅ Atlas Video export completed successfully: {}", outFile.getAbsolutePath());
                future.complete(outFile);

            } catch (Exception ex) {
                logger.error("Failed to export Atlas video", ex);
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    /**
     * Renders a single crisp off-screen BufferedImage frame with all layers and HUD telemetry.
     */
    public static BufferedImage renderFrame(
            int width,
            int height,
            List<H3Cell> cells,
            Set<DisplayMode> activeLayers,
            long stepNumber,
            int snapshotIndex,
            int totalSnapshots
    ) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();

        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // 1. Canvas Background
            g2.setColor(new Color(15, 23, 42)); // Dark Slate
            g2.fillRect(0, 0, width, height);

            int mapTop = 45;
            int mapBottom = height - 35;
            int mapHeight = mapBottom - mapTop;
            int mapLeft = 16;
            int mapRight = width - 16;
            int mapWidth = mapRight - mapLeft;

            // Map Area Container
            g2.setColor(new Color(30, 41, 59));
            g2.fill(new RoundRectangle2D.Double(mapLeft - 4, mapTop - 4, mapWidth + 8, mapHeight + 8, 12, 12));

            if (cells != null && !cells.isEmpty()) {
                double minLat = 90, maxLat = -90, minLng = 180, maxLng = -180;
                for (H3Cell c : cells) {
                    if (c.getLatitude() != null) {
                        minLat = Math.min(minLat, c.getLatitude());
                        maxLat = Math.max(maxLat, c.getLatitude());
                    }
                    if (c.getLongitude() != null) {
                        minLng = Math.min(minLng, c.getLongitude());
                        maxLng = Math.max(maxLng, c.getLongitude());
                    }
                }

                if (minLat < -60 && maxLat > 60) {
                    minLat = -90.0;
                    maxLat = 90.0;
                }
                if (minLng < -150 && maxLng > 150) {
                    minLng = -180.0;
                    maxLng = 180.0;
                }

                double latRange = Math.max(0.01, maxLat - minLat);
                double lngRange = Math.max(0.01, maxLng - minLng);
                double dotScale = 1.4;

                // 2. Base Biomes Background
                for (H3Cell c : cells) {
                    if (c.getLatitude() == null || c.getLongitude() == null) continue;
                    double x = ((c.getLongitude() - minLng) / lngRange) * (mapWidth - 16) + mapLeft + 8;
                    double y = (1.0 - ((c.getLatitude() - minLat) / latRange)) * (mapHeight - 16) + mapTop + 8;

                    g2.setColor(getBiomeAwtColor(c.getBiome()));
                    double s = 4.0 * dotScale;
                    g2.fill(new Ellipse2D.Double(x - s / 2, y - s / 2, s, s));
                }

                // 3. Composite Thematic Layers
                DisplayMode primaryScalar = null;
                for (DisplayMode mode : activeLayers) {
                    if (mode == DisplayMode.BIOME) continue;
                    if (primaryScalar == null && mode.getEncodingType() == DisplayMode.EncodingType.SCALAR_1D) {
                        primaryScalar = mode;
                    }

                    double minVal = Double.MAX_VALUE;
                    double maxVal = -Double.MAX_VALUE;
                    for (H3Cell c : cells) {
                        double val = getCellValue(c, mode);
                        minVal = Math.min(minVal, val);
                        maxVal = Math.max(maxVal, val);
                    }
                    double valRange = Math.max(0.001, maxVal - (minVal < 0 ? minVal : 0.0));

                    for (H3Cell c : cells) {
                        if (c.getLatitude() == null || c.getLongitude() == null) continue;
                        double val = getCellValue(c, mode);
                        if (val <= 0 && isSparseZeroSkipped(mode)) continue;

                        double x = ((c.getLongitude() - minLng) / lngRange) * (mapWidth - 16) + mapLeft + 8;
                        double y = (1.0 - ((c.getLatitude() - minLat) / latRange)) * (mapHeight - 16) + mapTop + 8;

                        double ratio = Math.clamp((val - (minVal < 0 ? minVal : 0.0)) / valRange, 0.0, 1.0);
                        Color color = getLayerAwtColor(ratio, mode);
                        g2.setColor(color);

                        double size = Math.max(4.0, 4.0 + ratio * 8.0) * dotScale;
                        g2.fill(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size));
                    }
                }
            }

            // 4. Top Header HUD
            g2.setColor(new Color(56, 189, 248)); // Cyan Accent
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("🗺️ ETHER — ATLAS DYNAMIQUE DES TERRITOIRES", mapLeft, 26);

            g2.setColor(new Color(148, 163, 184)); // Muted Slate
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            String stepStr = String.format(Locale.ROOT, "Pas : %,d  |  Instantané %d / %d", stepNumber, snapshotIndex, totalSnapshots);
            g2.drawString(stepStr, width - mapLeft - g2.getFontMetrics().stringWidth(stepStr), 26);

            // 5. Bottom Footer HUD (Active Layers & Autocorrelation)
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(new Color(203, 213, 225));

            StringBuilder layersStr = new StringBuilder("Couches Actives : ");
            int lCount = 0;
            for (DisplayMode mode : activeLayers) {
                if (lCount > 0) layersStr.append(" • ");
                layersStr.append(mode.getDisplayName());
                lCount++;
                if (lCount >= 4) {
                    if (activeLayers.size() > 4) layersStr.append(" (+").append(activeLayers.size() - 4).append(")");
                    break;
                }
            }
            g2.drawString(layersStr.toString(), mapLeft, height - 12);

            if (cells != null && !cells.isEmpty()) {
                double moranI = computeMoranI(cells, activeLayers);
                String moranStr = String.format(Locale.US, "Moran's I : %.3f (%s)", moranI,
                        moranI > 0.3 ? "Clusters" : (moranI < -0.1 ? "Dispersé" : "Aléatoire"));
                g2.setColor(new Color(255, 215, 0)); // Gold
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                g2.drawString(moranStr, width - mapLeft - g2.getFontMetrics().stringWidth(moranStr), height - 12);
            }

        } finally {
            g2.dispose();
        }

        return img;
    }

    private static boolean isSparseZeroSkipped(DisplayMode mode) {
        return switch (mode) {
            case POPULATION, FLUX, MIGRATION, CONFLICT, EPIDEMIC -> true;
            default -> false;
        };
    }

    private static Color getBiomeAwtColor(Biome biome) {
        if (biome == null) return new Color(30, 41, 59, 180);
        return switch (biome) {
            case OCEAN -> new Color(14, 45, 80, 200);
            case DEEP_OCEAN -> new Color(10, 30, 60, 230);
            case BEACH -> new Color(180, 160, 100, 200);
            case DESERT -> new Color(160, 110, 50, 200);
            case PLAINS -> new Color(45, 95, 45, 210);
            case SAVANNAH -> new Color(130, 140, 50, 210);
            case FOREST -> new Color(25, 75, 40, 220);
            case JUNGLE -> new Color(15, 85, 30, 220);
            case MOUNTAINS -> new Color(110, 110, 120, 220);
            case HILLS -> new Color(90, 100, 80, 210);
            case TUNDRA -> new Color(130, 140, 140, 210);
            case SNOW, GLACIER -> new Color(200, 215, 225, 230);
            case LAKE -> new Color(20, 70, 120, 200);
        };
    }

    private static Color getLayerAwtColor(double ratio, DisplayMode mode) {
        int alpha = 200;
        return switch (mode.getCategory()) {
            case PHYSICAL -> {
                if (mode == DisplayMode.TEMPERATURE) {
                    yield new Color((float) ratio, 0.2f, (float) (1.0 - ratio), alpha / 255.0f);
                } else if (mode == DisplayMode.PRECIPITATION || mode == DisplayMode.WATER) {
                    yield new Color(0.1f, (float) (0.4 + ratio * 0.4), (float) Math.min(1.0, 0.6 + ratio * 0.4), alpha / 255.0f);
                } else {
                    yield new Color((float) Math.min(1.0, ratio * 1.2), (float) Math.min(1.0, 0.8 - ratio * 0.4), 0.2f, alpha / 255.0f);
                }
            }
            case DEMOGRAPHICS -> {
                if (ratio < 0.3) {
                    yield new Color((int) (255 * (ratio / 0.3)), (int) (220 * (ratio / 0.3)), 0, alpha);
                } else if (ratio < 0.7) {
                    double t = (ratio - 0.3) / 0.4;
                    yield new Color(255, (int) (220 - t * 140), 0, alpha);
                } else {
                    double t = (ratio - 0.7) / 0.3;
                    yield new Color(255, (int) (80 - t * 60), (int) (t * 40), alpha);
                }
            }
            case ECOLOGY -> new Color(0.2f, (float) Math.min(1.0, 0.3 + ratio * 0.7), 0.3f, alpha / 255.0f);
            case MINING -> new Color((float) Math.min(1.0, 0.4 + ratio * 0.6), 0.3f, (float) Math.min(1.0, 0.6 + ratio * 0.4), alpha / 255.0f);
            case SOCIETY_POLITICS -> {
                if (mode == DisplayMode.COLLAPSE_RISK || mode == DisplayMode.CONFLICT) {
                    yield new Color((float) Math.min(1.0, 0.5 + ratio * 0.5), 0.1f, 0.1f, alpha / 255.0f);
                } else if (mode == DisplayMode.GDP_WEALTH) {
                    yield new Color(0.2f, (float) Math.min(1.0, 0.4 + ratio * 0.6), (float) Math.min(1.0, ratio * 1.2), alpha / 255.0f);
                } else {
                    yield new Color((float) Math.min(1.0, 0.3 + ratio * 0.7), 0.4f, (float) Math.min(1.0, 0.8 - ratio * 0.4), alpha / 255.0f);
                }
            }
        };
    }

    private static double getCellValue(H3Cell c, DisplayMode mode) {
        if (c == null || mode == null) return 0.0;
        return switch (mode) {
            case BIOME -> c.getBiome() != null ? c.getBiome().ordinal() : 0.0;
            case TEMPERATURE -> c.getTemperature() != null ? c.getTemperature() : 15.0;
            case PRECIPITATION -> c.getRainfall() != null ? c.getRainfall() : 500.0;
            case WATER -> c.getWaterResource() != null ? c.getWaterResource() : 100.0;
            case ALBEDO -> c.getDynamicAlbedo() != null ? c.getDynamicAlbedo() : 0.3;
            case FRICTION -> c.getMovementFriction() != null ? c.getMovementFriction() : 1.0;
            case ENERGY_CAPACITY -> c.getEnergySolar() != null ? c.getEnergySolar() : 0.0;
            case ENTROPY_POLLUTION -> c.getPollutionLevel() != null ? c.getPollutionLevel() : 0.0;
            case SOIL_QUALITY -> c.getSoilOrganicCarbon() != null ? c.getSoilOrganicCarbon() : (c.getFoodResource() != null ? c.getFoodResource() / 50.0 : 50.0);
            case BIODIVERSITY -> c.getBiomassNatural() != null ? c.getBiomassNatural() : 50.0;
            case OCEAN_PH -> 8.1;
            case PERMAFROST -> c.getIceSheetThicknessMeters() != null ? c.getIceSheetThicknessMeters() : 0.0;
            case POPULATION -> c.getPopulation() != null ? c.getPopulation().doubleValue() : 0.0;
            case MIGRATION -> c.getFluxPressure();
            case AGE_PYRAMID -> c.getPopElderly() != null && c.getPopulation() != null && c.getPopulation() > 0 ? ((double) c.getPopElderly() / c.getPopulation()) * 100.0 : 15.0;
            case EPIDEMIC -> c.getPollutionLevel() != null ? c.getPollutionLevel() * 0.8 : 0.0;
            case HEALTH_LIFE_EXPECTANCY -> c.getLifespan() != null ? c.getLifespan() : 45.0;
            case EDUCATION_LEVEL -> c.getTechnologyLevel() != null ? Math.min(100.0, c.getTechnologyLevel() * 12.0) : 10.0;
            case MALTHUSIAN_PRESSURE -> {
                double food = c.getFoodResource() != null ? c.getFoodResource() : 10.0;
                double pop = c.getPopulation() != null ? c.getPopulation() : 0.0;
                yield pop > 0 ? (pop * 3.362) / Math.max(1.0, food) : 0.0;
            }
            case CAPACITY -> c.getFoodResource() != null ? c.getFoodResource() / 3.362 : 1000.0;
            case FOOD -> c.getFoodResource() != null ? c.getFoodResource() : 0.0;
            case WOOD -> c.getWoodResource() != null ? c.getWoodResource() : 100.0;
            case MINERAL_RESOURCES -> c.getResourceMetal() != null ? c.getResourceMetal() : 0.0;
            case MINING_EXPLOITATION -> c.getResourceWork() != null ? c.getResourceWork() : 0.0;
            case TECHNOLOGY -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
            case CULTURE -> c.getOwner() != null ? Math.abs(c.getOwner().hashCode() % 100) : 1.0;
            case POLITICAL -> c.getOwner() != null ? 100.0 : 0.0;
            case INEQUALITY -> c.getGiniIndex() != null ? c.getGiniIndex() : 0.35;
            case ASABIYYAH -> 0.70;
            case FLUX -> c.getFluxPressure();
            case HAPPINESS -> 65.0;
            case CONFLICT -> c.getOwner() != null ? 75.0 : 5.0;
            case INSTITUTIONAL_MATURITY -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() * 10.0 : 5.0;
            case GDP_WEALTH -> {
                double pop = c.getPopulation() != null ? c.getPopulation() : 0.0;
                double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
                yield pop * tech * 15.0;
            }
            case ELITE_DENSITY -> 2.5;
            case COLLECTIVE_MEMORY -> c.getTechnologyLevel() != null ? Math.pow(c.getTechnologyLevel(), 2.0) * 100.0 : 10.0;
            case COLLAPSE_RISK -> {
                double pop = c.getPopulation() != null ? c.getPopulation() : 0.0;
                double food = c.getFoodResource() != null ? c.getFoodResource() : 1.0;
                yield pop > 0 && food < pop ? Math.min(100.0, ((pop - food) / pop) * 100.0) : 5.0;
            }
        };
    }

    private static double computeMoranI(List<H3Cell> cells, Set<DisplayMode> activeLayers) {
        DisplayMode scalarMode = null;
        for (DisplayMode dm : activeLayers) {
            if (dm != DisplayMode.BIOME && dm.getEncodingType() == DisplayMode.EncodingType.SCALAR_1D) {
                scalarMode = dm;
                break;
            }
        }
        if (scalarMode == null || cells.size() < 4) return 0.0;

        double sum = 0;
        for (H3Cell c : cells) sum += getCellValue(c, scalarMode);
        double mean = sum / cells.size();

        double num = 0, denom = 0;
        int n = cells.size();

        for (int i = 0; i < n; i++) {
            double zi = getCellValue(cells.get(i), scalarMode) - mean;
            denom += zi * zi;
            for (int j = i + 1; j < Math.min(n, i + 10); j++) {
                double zj = getCellValue(cells.get(j), scalarMode) - mean;
                num += zi * zj;
            }
        }
        return denom > 0 ? Math.clamp(num / denom, -1.0, 1.0) : 0.0;
    }
}
