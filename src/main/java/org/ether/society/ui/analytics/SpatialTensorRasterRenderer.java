/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.scene.image.Image;
import org.ether.society.database.H3Cell;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Procedural cartographic raster rendering pipeline for 2D spatial tensors.
 *
 * <p>Translates spatial hexagonal discrete cells (\(H_3\)) into projected raster pixel canvases
 * across multi-spectral demographic, metabolic, climatic, and sociological color palettes.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class SpatialTensorRasterRenderer {

    private SpatialTensorRasterRenderer() {
        // Utility class
    }

    /**
     * Determines optimal raster resolution based on total cell count to maximize visual fidelity.
     *
     * @param cellCount simulation cell count
     * @return recommended image width in pixels
     */
    public static int computeOptimalRasterWidth(int cellCount) {
        if (cellCount > 10_000_000) {
            long maxMem = Runtime.getRuntime().maxMemory();
            if (maxMem >= 8L * 1024 * 1024 * 1024) {
                return 8192;
            }
            return 4096;
        } else if (cellCount > 1_500_000) {
            return 4096;
        } else if (cellCount > 10_000) {
            return 2048;
        } else {
            return 1024;
        }
    }

    /**
     * Rasterizes a collection of H3 cells into a projected equi-rectangular BufferedImage.
     *
     * @param cells list of simulation cells with spatial coordinates and tensor values
     * @param channel target tensor channel key or description
     * @param width desired output image width in pixels
     * @param height desired output image height in pixels
     * @return rendered BufferedImage, or null if cell list is empty
     */
    public static BufferedImage rasterizeCellsToImage(List<H3Cell> cells, String channel, int width, int height) {
        if (cells == null || cells.isEmpty()) return null;
        int targetW = width > 0 ? width : computeOptimalRasterWidth(cells.size());
        int targetH = height > 0 ? height : targetW / 2;

        BufferedImage img = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(15, 23, 42));
        g2.fillRect(0, 0, targetW, targetH);

        int r = Math.max(2, (int) Math.ceil(targetW / Math.sqrt(cells.size() * 1.5)));
        if (r > 15) r = 15;

        String chLower = channel != null ? channel.toLowerCase(Locale.ROOT) : "";

        int[] cellColors = new int[cells.size()];
        java.util.Map<Long, Integer> cellIndexMap = java.util.HashMap.newHashMap(cells.size());
        int detectedRes = -1;
        boolean allValidH3 = true;
        com.uber.h3core.H3Core h3Ref = null;
        try {
            h3Ref = org.ether.society.h3.H3Service.getInstance().getH3();
        } catch (Throwable ignored) {
            allValidH3 = false;
        }
        for (int idx = 0; idx < cells.size(); idx++) {
            H3Cell c = cells.get(idx);
            if (c == null) continue;
            if (allValidH3 && c.getH3Index() != null && c.getH3Index() != 0L) {
                if (detectedRes < 0) detectedRes = h3Ref.getResolution(c.getH3Index());
                cellIndexMap.put(c.getH3Index(), idx);
            } else {
                allValidH3 = false;
            }
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            int x = (int) Math.round(((lng + 180.0) / 360.0) * targetW);
            int y = (int) Math.round(((90.0 - lat) / 180.0) * targetH);

            Color col;
            if (chLower.contains("densitÃ©") || chLower.contains("demographic") || chLower.contains("density") || chLower.contains("äººå£")) {
                long pop = c.getPopulation() != null ? c.getPopulation() : 0;
                if (pop > 0) {
                    double norm = Math.min(1.0, Math.log10(pop + 1.0) / 6.0);
                    col = getDensityColor(norm);
                } else {
                    col = new Color(30, 41, 59);
                }
            } else if (chLower.contains("technologie") || chLower.contains("technology") || chLower.contains("outillage") || chLower.contains("ç§‘æŠ€")) {
                double tech = org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(c);
                double norm = Math.min(1.0, tech / 100.0);
                col = getTechColor(norm);
            } else if (chLower.contains("tempÃ©rature") || chLower.contains("temperature") || chLower.contains("climat") || chLower.contains("æ°”æ¸©")) {
                double temp = c.getTemperature() != null ? c.getTemperature() : 15.0;
                double norm = Math.max(0.0, Math.min(1.0, (temp + 20.0) / 60.0));
                col = getTemperatureColor(norm);
            } else if (chLower.contains("aquifÃ¨re") || chLower.contains("aquifer") || chLower.contains("eau") || chLower.contains("water") || chLower.contains("å«æ°´å±‚")) {
                double aqua = c.getFreshwaterAquifer() != null ? c.getFreshwaterAquifer() : 50.0;
                double norm = Math.max(0.0, Math.min(1.0, aqua / 100.0));
                col = getAquiferColor(norm);
            } else if (chLower.contains("biomasse") || chLower.contains("biomass") || chLower.contains("agricole") || chLower.contains("agriculture") || chLower.contains("å†œä¸š")) {
                double bio = c.getBiomassAgriculture() != null ? c.getBiomassAgriculture() : 0.0;
                double norm = Math.max(0.0, Math.min(1.0, bio / 1000.0));
                col = getAgricultureColor(norm);
            } else if (chLower.contains("isogloss") || chLower.contains("linguist") || chLower.contains("lang") || chLower.contains("è¯­è¨€")) {
                long clade = (c.getLanguageGroup() != null && !c.getLanguageGroup().isBlank())
                        ? (long) Math.abs(c.getLanguageGroup().hashCode())
                        : (c.getId() != null ? (long) Math.abs(c.getId().hashCode()) : 0L);
                col = getPolityColor(clade);
            } else if (chLower.contains("souverainetÃ©") || chLower.contains("sovereignty") || chLower.contains("ä¸»æƒ")) {
                long polity = (c.getLanguageGroup() != null) ? (long) Math.abs(c.getLanguageGroup().hashCode()) : (c.getId() != null ? (long) Math.abs(c.getId().hashCode()) : 0L);
                col = getPolityColor(polity);
            } else if (chLower.contains("parent") || chLower.contains("kinship") || chLower.contains("famill") || chLower.contains("äº²å±ž")) {
                long hash = c.getLanguageGroup() != null ? (long) Math.abs((c.getLanguageGroup() + "_kin").hashCode()) : (c.getId() != null ? (long) Math.abs(c.getId().hashCode() * 31) : 0L);
                col = getPolityColor(hash);
            } else if (chLower.contains("rituel") || chLower.contains("ritual") || chLower.contains("croyan") || chLower.contains("belief") || chLower.contains("ä¿¡ä»°")) {
                long hash = c.getLanguageGroup() != null ? (long) Math.abs((c.getLanguageGroup() + "_rit").hashCode()) : (c.getId() != null ? (long) Math.abs(c.getId().hashCode() * 73) : 0L);
                col = getPolityColor(hash);
            } else if (chLower.contains("commerc") || chLower.contains("trade") || chLower.contains("route") || chLower.contains("è´¸æ˜“")) {
                double friction = c.getMovementFriction() != null ? c.getMovementFriction() : 1.0;
                double norm = Math.max(0.0, Math.min(1.0, 1.0 - (friction / 5.0)));
                col = getTechColor(norm);
            } else if (chLower.contains("institut") || chLower.contains("seshat") || chLower.contains("åˆ¶åº¦")) {
                long pop = c.getPopulation() != null ? c.getPopulation() : 0;
                double kpc = (c.getResourceCapital() != null && pop > 0) ? (c.getResourceCapital() / pop) : 0.0;
                double instNorm = Math.min(1.0, (Math.log10(Math.max(1.0, pop)) / 6.0) * 0.6 + (Math.min(1.0, kpc / 1500.0) * 0.4));
                col = getTechColor(instNorm);
            } else if (chLower.contains("Ã©colog") || chLower.contains("ecolog") || chLower.contains("empreint") || chLower.contains("footprint") || chLower.contains("ç”Ÿæ€")) {
                long pop = c.getPopulationOr(0);
                double bioAgr = c.getBiomassAgricultureOr(0.0);
                double bioLiv = c.getBiomassLivestock() != null ? c.getBiomassLivestock() : 0.0;
                double bioNat = c.getBiomassNaturalOr(0.0);
                double clearance = (bioAgr + bioLiv) / (bioAgr + bioLiv + bioNat + 100.0);
                double area = Math.max(1.0, c.getCellAreaKm2());
                double densityStrain = pop > 0 ? Math.min(1.0, Math.log1p((double) pop / area) / 6.0) : 0.0;
                col = getAgricultureColor(Math.clamp(0.75 * clearance + 0.25 * densityStrain, 0.0, 1.0));
            } else if (chLower.contains("pathog") || chLower.contains("sanit") || chLower.contains("immunit") || chLower.contains("health") || chLower.contains("zoonot") || chLower.contains("ç—…åŽŸ")) {
                int recovered = c.getEpidemicRecovered() != null ? c.getEpidemicRecovered() : 0;
                double norm = Math.min(1.0, recovered / 500.0);
                col = getDensityColor(norm);
            } else {
                long customHash = Math.abs((chLower + "_" + (c.getLanguageGroup() != null ? c.getLanguageGroup() : c.getId())).hashCode());
                col = getPolityColor(customHash);
            }

            Integer cIdx = (c.getH3Index() != null) ? cellIndexMap.get(c.getH3Index()) : null;
            if (cIdx != null) {
                cellColors[cIdx] = col.getRGB();
            }
            if (!allValidH3) {
                g2.setColor(col);
                g2.fillOval(x - r, y - r, r * 2, r * 2);
            }
        }
        g2.dispose();
        if (allValidH3 && detectedRes >= 0 && h3Ref != null) {
            final com.uber.h3core.H3Core h3Core = h3Ref;
            final int h3Res = detectedRes;
            final int bgArgb = new Color(15, 23, 42).getRGB();
            int[] outPixels = new int[targetW * targetH];
            java.util.stream.IntStream.range(0, targetH).parallel().forEach(py -> {
                double pLat = 90.0 - ((py + 0.5) / targetH) * 180.0;
                int rowOff = py * targetW;
                for (int px = 0; px < targetW; px++) {
                    double pLon = -180.0 + ((px + 0.5) / targetW) * 360.0;
                    long h3Idx = h3Core.latLngToCell(pLat, pLon, h3Res);
                    Integer hit = cellIndexMap.get(h3Idx);
                    outPixels[rowOff + px] = (hit != null) ? cellColors[hit] : bgArgb;
                }
            });
            img.setRGB(0, 0, targetW, targetH, outPixels, 0, targetW);
        }
        return img;
    }

    /**
     * Rasterizes H3 cell demographic densities into an equirectangular grayscale luminance image
     * directly comparable to empirical HYDE / paleodemographic ground-truth density rasters
     * ({@code data/ether/earth/<year>/earth_<year>_density.png}).
     * <p>
     * Unpopulated and ocean cells map to pure black ({@code RGB(0, 0, 0)}), while inhabited cells
     * map to grayscale luminance {@code RGB(L, L, L)} scaled to the empirical reference distribution.
     * </p>
     *
     * @param cells          list of simulated H3 cells
     * @param referenceImage optional empirical reference raster for luminance range calibration (may be null)
     * @param width          output width in pixels
     * @param height         output height in pixels
     * @return grayscale equirectangular BufferedImage
     */
    public static BufferedImage rasterizeCellsToGrayscaleDensityImage(
            List<H3Cell> cells, BufferedImage referenceImage, int width, int height) {
        if (cells == null || cells.isEmpty()) return null;
        int targetW = width > 0 ? width : (referenceImage != null ? referenceImage.getWidth() : 2048);
        int targetH = height > 0 ? height : (referenceImage != null ? referenceImage.getHeight() : 1024);

        BufferedImage img = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, targetW, targetH);

        long maxPop = 1L;
        for (H3Cell c : cells) {
            if (c != null && c.getPopulation() != null && c.getPopulation() > maxPop) {
                maxPop = c.getPopulation();
            }
        }

        // Determine reference max luminance in the domain of interest to preserve absolute scale calibration
        double refMax = 255.0;
        if (referenceImage != null) {
            double[] obsRef = org.ether.society.data.H3RasterAggregator.aggregateDensityMeans(referenceImage, cells);
            double peakObs = 10.0 / 255.0;
            for (double v : obsRef) {
                if (v > peakObs) peakObs = v;
            }
            refMax = Math.max(10.0, peakObs * 255.0);
        }

        // Attempt exact pixel-to-H3 inverse hexagonal rasterization (eliminates centroid oval aliasing)
        int[] cellGrays = new int[cells.size()];
        java.util.Map<Long, Integer> cellIndexMap = java.util.HashMap.newHashMap(cells.size());
        int detectedRes = -1;
        boolean allValidH3 = true;
        com.uber.h3core.H3Core h3Ref = null;
        try {
            h3Ref = org.ether.society.h3.H3Service.getInstance().getH3();
        } catch (Throwable ignored) {
            allValidH3 = false;
        }

        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            if (c == null) continue;
            if (allValidH3 && c.getH3Index() != null && c.getH3Index() != 0L) {
                if (detectedRes < 0) detectedRes = h3Ref.getResolution(c.getH3Index());
                cellIndexMap.put(c.getH3Index(), i);
            } else {
                allValidH3 = false;
            }
            long pop = c.getPopulation() != null ? c.getPopulation() : 0L;
            if (pop > 0L) {
                double simLog = Math.log10(pop + 1.0) / Math.log10(Math.max(10.0, (double) maxPop + 1.0));
                cellGrays[i] = Math.clamp((int) Math.round(simLog * refMax), 2, 255);
            }
        }

        if (allValidH3 && detectedRes >= 0 && h3Ref != null) {
            final com.uber.h3core.H3Core h3Core = h3Ref;
            final int h3Res = detectedRes;
            int[] outPixels = new int[targetW * targetH];
            java.util.stream.IntStream.range(0, targetH).parallel().forEach(py -> {
                double pLat = 90.0 - ((py + 0.5) / targetH) * 180.0;
                int rowOff = py * targetW;
                for (int px = 0; px < targetW; px++) {
                    double pLon = -180.0 + ((px + 0.5) / targetW) * 360.0;
                    long h3Idx = h3Core.latLngToCell(pLat, pLon, h3Res);
                    Integer hit = cellIndexMap.get(h3Idx);
                    int g = (hit != null) ? cellGrays[hit] : 0;
                    outPixels[rowOff + px] = (g << 16) | (g << 8) | g;
                }
            });
            img.setRGB(0, 0, targetW, targetH, outPixels, 0, targetW);
            g2.dispose();
            return img;
        }

        double pixelScaleKm = 40075.0 / (double) targetW;
        double approxCellRadiusKm = Math.sqrt(Math.max(1.0, 510000000.0 / Math.max(100.0, cells.size() * 10.0)) / Math.PI);
        int r = Math.max(1, Math.min(6, (int) Math.round(approxCellRadiusKm / pixelScaleKm)));

        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            if (c == null || c.getLatitude() == null || c.getLongitude() == null) continue;
            int gray = cellGrays[i];
            if (gray <= 0) continue;

            double lat = c.getLatitude();
            double lng = c.getLongitude();
            int x = (int) Math.round(((lng + 180.0) / 360.0) * targetW);
            int y = (int) Math.round(((90.0 - lat) / 180.0) * targetH);
            double cosLat = Math.max(0.25, Math.cos(Math.toRadians(lat)));
            int rx = Math.max(r, (int) Math.round(r / cosLat));
            int ry = r;
            for (int dy = -ry; dy <= ry; dy++) {
                int py = y + dy;
                if (py < 0 || py >= targetH) continue;
                for (int dx = -rx; dx <= rx; dx++) {
                    int px = x + dx;
                    if (px < 0 || px >= targetW) continue;
                    if ((dx * dx) * (ry * ry) + (dy * dy) * (rx * rx) <= (rx * rx * ry * ry)) {
                        int current = img.getRGB(px, py) & 0xFF;
                        int blend = Math.max(current, gray);
                        img.setRGB(px, py, (blend << 16) | (blend << 8) | blend);
                    }
                }
            }
        }
        g2.dispose();
        return img;
    }

    /**
     * Maps normalized temperature [0..1] to thermal gradient color.
     */
    public static Color getTemperatureColor(double norm) {
        int r = (int) (norm * 255);
        int g = (int) ((1.0 - Math.abs(norm - 0.5) * 2.0) * 200);
        int b = (int) ((1.0 - norm) * 255);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
    }

    /**
     * Maps normalized aquifer depth [0..1] to hydrogeological blue-cyan gradient.
     */
    public static Color getAquiferColor(double norm) {
        int r = (int) (20 + norm * 30);
        int g = (int) (100 + norm * 140);
        int b = (int) (200 + norm * 55);
        return new Color(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    /**
     * Maps normalized agricultural biomass [0..1] to fertile green palette.
     */
    public static Color getAgricultureColor(double norm) {
        int r = (int) (120 - norm * 80);
        int g = (int) (140 + norm * 100);
        int b = (int) (30 + norm * 20);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
    }

    /**
     * Maps demographic density [0..1] to standard traffic light gradient (emerald, amber, crimson).
     */
    public static Color getDensityColor(double norm) {
        if (norm < 0.33) {
            return new Color(16, 185, 129);
        } else if (norm < 0.66) {
            return new Color(245, 158, 11);
        } else {
            return new Color(239, 68, 68);
        }
    }

    /**
     * Maps technological level [0..1] to electric cyan/blue progression.
     */
    public static Color getTechColor(double norm) {
        int r = (int) (56 + norm * (245 - 56));
        int g = (int) (189 - norm * 80);
        int b = (int) (248 - norm * 150);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
    }

    /**
     * Generates golden ratio HSB pseudo-random distinct categorical polity color.
     */
    public static Color getPolityColor(long id) {
        if (id <= 0) return new Color(100, 116, 139);
        return Color.getHSBColor((float) ((id * 0.618033988749895) % 1.0), 0.75f, 0.85f);
    }

    /**
     * Converts a Java AWT BufferedImage to a JavaFX Image.
     */
    public static Image bufferedImageToFxImage(BufferedImage buf) {
        if (buf == null) return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buf, "PNG", baos);
            return new Image(new ByteArrayInputStream(baos.toByteArray()));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Decodes Base64 image payload into AWT BufferedImage.
     */
    public static BufferedImage base64ToBufferedImage(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            return null;
        }
    }
}
