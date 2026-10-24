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

        int r = Math.max(1, (int) Math.round(targetW / Math.sqrt(cells.size() * 3.5)));
        if (r > 15) r = 15;

        String chLower = channel != null ? channel.toLowerCase(Locale.ROOT) : "";

        for (H3Cell c : cells) {
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            int x = (int) Math.round(((lng + 180.0) / 360.0) * targetW);
            int y = (int) Math.round(((90.0 - lat) / 180.0) * targetH);

            Color col;
            if (chLower.contains("densité") || chLower.contains("demographic") || chLower.contains("density") || chLower.contains("人口")) {
                long pop = c.getPopulation() != null ? c.getPopulation() : 0;
                if (pop > 0) {
                    double norm = Math.min(1.0, Math.log10(pop + 1.0) / 6.0);
                    col = getDensityColor(norm);
                } else {
                    col = new Color(30, 41, 59);
                }
            } else if (chLower.contains("technologie") || chLower.contains("technology") || chLower.contains("outillage") || chLower.contains("科技")) {
                double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
                double norm = Math.min(1.0, tech / 100.0);
                col = getTechColor(norm);
            } else if (chLower.contains("température") || chLower.contains("temperature") || chLower.contains("climat") || chLower.contains("气温")) {
                double temp = c.getTemperature() != null ? c.getTemperature() : 15.0;
                double norm = Math.max(0.0, Math.min(1.0, (temp + 20.0) / 60.0));
                col = getTemperatureColor(norm);
            } else if (chLower.contains("aquifère") || chLower.contains("aquifer") || chLower.contains("eau") || chLower.contains("water") || chLower.contains("含水层")) {
                double aqua = c.getFreshwaterAquifer() != null ? c.getFreshwaterAquifer() : 50.0;
                double norm = Math.max(0.0, Math.min(1.0, aqua / 100.0));
                col = getAquiferColor(norm);
            } else if (chLower.contains("biomasse") || chLower.contains("biomass") || chLower.contains("agricole") || chLower.contains("agriculture") || chLower.contains("农业")) {
                double bio = c.getBiomassAgriculture() != null ? c.getBiomassAgriculture() : 0.0;
                double norm = Math.max(0.0, Math.min(1.0, bio / 1000.0));
                col = getAgricultureColor(norm);
            } else if (chLower.contains("souveraineté") || chLower.contains("sovereignty") || chLower.contains("主权")) {
                long polity = (c.getLanguageGroup() != null) ? (long) Math.abs(c.getLanguageGroup().hashCode()) : (c.getId() != null ? (long) Math.abs(c.getId().hashCode()) : 0L);
                col = getPolityColor(polity);
            } else if (chLower.contains("parent") || chLower.contains("kinship") || chLower.contains("famill") || chLower.contains("亲属")) {
                long hash = c.getLanguageGroup() != null ? (long) Math.abs((c.getLanguageGroup() + "_kin").hashCode()) : (c.getId() != null ? (long) Math.abs(c.getId().hashCode() * 31) : 0L);
                col = getPolityColor(hash);
            } else if (chLower.contains("rituel") || chLower.contains("ritual") || chLower.contains("croyan") || chLower.contains("belief") || chLower.contains("信仰")) {
                long hash = c.getLanguageGroup() != null ? (long) Math.abs((c.getLanguageGroup() + "_rit").hashCode()) : (c.getId() != null ? (long) Math.abs(c.getId().hashCode() * 73) : 0L);
                col = getPolityColor(hash);
            } else if (chLower.contains("commerc") || chLower.contains("trade") || chLower.contains("route") || chLower.contains("贸易")) {
                double friction = c.getMovementFriction() != null ? c.getMovementFriction() : 1.0;
                double norm = Math.max(0.0, Math.min(1.0, 1.0 - (friction / 5.0)));
                col = getTechColor(norm);
            } else if (chLower.contains("institut") || chLower.contains("seshat") || chLower.contains("制度")) {
                double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
                col = getTechColor(Math.min(1.0, tech / 80.0));
            } else if (chLower.contains("écolog") || chLower.contains("ecolog") || chLower.contains("empreint") || chLower.contains("footprint") || chLower.contains("生态")) {
                double soc = c.getSoilOrganicCarbon() != null ? c.getSoilOrganicCarbon() : 50.0;
                col = getAgricultureColor(Math.max(0.0, Math.min(1.0, soc / 100.0)));
            } else if (chLower.contains("pathog") || chLower.contains("sanit") || chLower.contains("immunit") || chLower.contains("health") || chLower.contains("zoonot") || chLower.contains("病原")) {
                int recovered = c.getEpidemicRecovered() != null ? c.getEpidemicRecovered() : 0;
                double norm = Math.min(1.0, recovered / 500.0);
                col = getDensityColor(norm);
            } else {
                long customHash = Math.abs((chLower + "_" + (c.getLanguageGroup() != null ? c.getLanguageGroup() : c.getId())).hashCode());
                col = getPolityColor(customHash);
            }

            g2.setColor(col);
            g2.fillOval(x - r, y - r, r * 2, r * 2);
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
