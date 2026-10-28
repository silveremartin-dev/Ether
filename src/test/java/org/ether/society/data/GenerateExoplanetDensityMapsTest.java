package org.ether.society.data;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generates calibrated physical population density maps for Mars (2050) and Venus (2080).
 * Outputs both standardized grayscale density tensors [0..255] and full composite visual maps.
 */
public class GenerateExoplanetDensityMapsTest {

    private static final String ARTIFACT_DIR = "C:/Users/silve/.gemini/antigravity/brain/5cc4325b-7d3b-4ad4-810d-c81e2978ec09";

    @Test
    public void generateMarsAndVenusDensityMaps() throws Exception {
        generateMars2050DensityMap();
        generateVenus2080DensityMap();
    }

    private void generateMars2050DensityMap() throws Exception {
        File marsElevFile = new File("data/maps/ether/mars/2026/mars_2026_elevation.png");
        assertTrue(marsElevFile.exists(), "Mars elevation raster must exist");
        BufferedImage srcElev = ImageIO.read(marsElevFile);
        int w = srcElev.getWidth();
        int h = srcElev.getHeight();

        // 1. Pure Grayscale Density Raster (0 everywhere except outposts)
        BufferedImage grayDensity = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D gGray = grayDensity.createGraphics();
        gGray.setColor(Color.BLACK);
        gGray.fillRect(0, 0, w, h);

        // 2. High-Contrast Visual Relief + Demographic Heatmap
        BufferedImage visualMap = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D gVis = visualMap.createGraphics();
        gVis.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gVis.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Render darkened Mars topography backdrop
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = new Color(srcElev.getRGB(x, y));
                // Shift to a rusty Martian hue
                int r = Math.min(255, (int)(c.getRed() * 0.75 + 30));
                int g = Math.min(255, (int)(c.getGreen() * 0.45 + 15));
                int b = Math.min(255, (int)(c.getBlue() * 0.35 + 10));
                visualMap.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }

        // Draw Graticule
        gVis.setColor(new Color(255, 255, 255, 35));
        for (int lat = -60; lat <= 60; lat += 30) {
            int y = (int) ((90.0 - lat) / 180.0 * h);
            gVis.drawLine(0, y, w, y);
        }
        for (int lon = -150; lon <= 180; lon += 30) {
            int x = (int) ((lon + 180.0) / 360.0 * w);
            gVis.drawLine(x, 0, x, h);
        }

        // Outposts: Chryse (22.4°N, -49.8°E, 300 colons) & Jezero (18.38°N, 77.58°E, 200 colons)
        renderOutpostBeacon(w, h, gGray, gVis, 22.4, -49.8, 300, "Chryse Planitia Outpost (300 hab)", new Color(255, 75, 43));
        renderOutpostBeacon(w, h, gGray, gVis, 18.38, 77.58, 200, "Jezero Crater Science Hub (200 hab)", new Color(0, 230, 246));

        // Annotate Landmark Features
        gVis.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, w / 160)));
        drawFeatureLabel(gVis, w, h, 18.65, -133.8, "Olympus Mons (+21.2 km)", new Color(220, 180, 160, 200));
        drawFeatureLabel(gVis, w, h, -14.0, -59.0, "Valles Marineris (-7.0 km)", new Color(220, 180, 160, 200));
        drawFeatureLabel(gVis, w, h, -42.4, 70.5, "Hellas Basin (-8.2 km)", new Color(220, 180, 160, 200));

        // Legend & Header
        drawMapBanner(gVis, w, h, "MARS (2050) — CARTE DE DENSITÉ DÉMOGRAPHIQUE & AVANT-POSTES",
                "Population Totale: 500 Pionniers | Chryse Planitia (300) • Jezero Crater (200) | Densité globale: 0 hab/km²");

        gGray.dispose();
        gVis.dispose();

        // Save files
        File outDir = new File("data/maps/ether/mars/2050");
        outDir.mkdirs();
        File grayFile = new File(outDir, "mars_2050_density.png");
        File visFile = new File(outDir, "mars_2050_density_visual.png");
        ImageIO.write(grayDensity, "PNG", grayFile);
        ImageIO.write(visualMap, "PNG", visFile);

        // Copy to brain artifact directory
        File artifactTarget = new File(ARTIFACT_DIR, "mars_2050_density_visual.png");
        Files.copy(visFile.toPath(), artifactTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("✅ Generated Mars 2050 Density Maps: " + visFile.getAbsolutePath() + " and artifact " + artifactTarget.getAbsolutePath());
    }

    private void generateVenus2080DensityMap() throws Exception {
        File venusElevFile = new File("data/maps/ether/venus/2026/venus_2026_elevation.png");
        assertTrue(venusElevFile.exists(), "Venus elevation raster must exist");
        BufferedImage srcElev = ImageIO.read(venusElevFile);
        int w = srcElev.getWidth();
        int h = srcElev.getHeight();

        // 1. Pure Grayscale Density Raster
        BufferedImage grayDensity = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D gGray = grayDensity.createGraphics();
        gGray.setColor(Color.BLACK);
        gGray.fillRect(0, 0, w, h);

        // 2. High-Contrast Visual Relief + Demographic Heatmap
        BufferedImage visualMap = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D gVis = visualMap.createGraphics();
        gVis.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gVis.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Render sulfuric golden-amber Venusian radar topography backdrop
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = new Color(srcElev.getRGB(x, y));
                // Golden amber radar false-color
                int r = Math.min(255, (int)(c.getRed() * 0.85 + 40));
                int g = Math.min(255, (int)(c.getGreen() * 0.65 + 25));
                int b = Math.min(255, (int)(c.getBlue() * 0.20 + 5));
                visualMap.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }

        // Draw Graticule
        gVis.setColor(new Color(255, 255, 255, 35));
        for (int lat = -60; lat <= 60; lat += 30) {
            int y = (int) ((90.0 - lat) / 180.0 * h);
            gVis.drawLine(0, y, w, y);
        }
        for (int lon = -150; lon <= 180; lon += 30) {
            int x = (int) ((lon + 180.0) / 360.0 * w);
            gVis.drawLine(x, 0, x, h);
        }

        // Outposts: Aphrodite Cloud Station (0.0°N, 0.0°E, 105 hab) & Ishtar Aerostat (0.0°N, 120.0°E, 45 hab)
        renderOutpostBeacon(w, h, gGray, gVis, 0.0, 0.0, 105, "Aphrodite Cloud Station (105 hab / Alt: 55 km)", new Color(250, 204, 21));
        renderOutpostBeacon(w, h, gGray, gVis, 0.0, 120.0, 45, "Ishtar Aerostat Station (45 hab / Alt: 55 km)", new Color(52, 211, 153));

        // Annotate Landmark Features
        gVis.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, w / 160)));
        drawFeatureLabel(gVis, w, h, 65.0, 0.0, "Ishtar Terra / Maxwell Montes (+11 km)", new Color(245, 230, 180, 200));
        drawFeatureLabel(gVis, w, h, -10.0, 100.0, "Aphrodite Terra Highlands", new Color(245, 230, 180, 200));
        drawFeatureLabel(gVis, w, h, 25.0, -78.0, "Beta Regio Volcanic Shield", new Color(245, 230, 180, 200));

        // Legend & Header
        drawMapBanner(gVis, w, h, "VÉNUS (2080) — CARTE DE DENSITÉ DES CITÉS AÉROSTATIQUES",
                "Population Totale: 150 Scientifiques | Aphrodite Flotilla (105) • Ishtar Flotilla (45) | Altitude: 55 km (1 atm, 25°C)");

        gGray.dispose();
        gVis.dispose();

        // Save files
        File outDir = new File("data/maps/ether/venus/2080");
        outDir.mkdirs();
        File grayFile = new File(outDir, "venus_2080_density.png");
        File visFile = new File(outDir, "venus_2080_density_visual.png");
        ImageIO.write(grayDensity, "PNG", grayFile);
        ImageIO.write(visualMap, "PNG", visFile);

        // Copy to brain artifact directory
        File artifactTarget = new File(ARTIFACT_DIR, "venus_2080_density_visual.png");
        Files.copy(visFile.toPath(), artifactTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("✅ Generated Venus 2080 Density Maps: " + visFile.getAbsolutePath() + " and artifact " + artifactTarget.getAbsolutePath());
    }

    private void renderOutpostBeacon(int w, int h, Graphics2D gGray, Graphics2D gVis,
                                     double lat, double lon, int pop, String label, Color beaconColor) {
        int cx = (int) ((lon + 180.0) / 360.0 * w);
        int cy = (int) ((90.0 - lat) / 180.0 * h);

        // 1. Grayscale pixel injection (normalized luminance: 255 at core)
        int grayVal = Math.min(255, 120 + pop / 2);
        gGray.setColor(new Color(grayVal, grayVal, grayVal));
        gGray.fillOval(cx - 2, cy - 2, 5, 5);

        // 2. Visual Radiant Glow
        int glowR = Math.max(16, w / 60);
        for (int r = glowR; r >= 4; r -= 3) {
            float alpha = (float) Math.pow(1.0 - (float) r / glowR, 1.8) * 0.75f;
            gVis.setColor(new Color(beaconColor.getRed() / 255f, beaconColor.getGreen() / 255f, beaconColor.getBlue() / 255f, alpha));
            gVis.fillOval(cx - r, cy - r, r * 2, r * 2);
        }

        // Core white/neon point
        gVis.setColor(Color.WHITE);
        gVis.fillOval(cx - 3, cy - 3, 7, 7);
        gVis.setColor(beaconColor);
        gVis.drawOval(cx - 5, cy - 5, 11, 11);

        // Coordinates Crosshair & Pin Label
        gVis.setColor(new Color(0, 0, 0, 180));
        int textY = (cy < h - 40) ? cy + 22 : cy - 14;
        int textX = Math.clamp(cx - 90, 10, w - 240);
        Font font = new Font("SansSerif", Font.BOLD, Math.max(12, w / 140));
        gVis.setFont(font);
        FontMetrics fm = gVis.getFontMetrics();
        int tw = fm.stringWidth(label);
        gVis.fillRoundRect(textX - 6, textY - 14, tw + 12, 18, 6, 6);

        gVis.setColor(Color.WHITE);
        gVis.drawString(label, textX, textY);
    }

    private void drawFeatureLabel(Graphics2D g, int w, int h, double lat, double lon, String text, Color col) {
        int x = (int) ((lon + 180.0) / 360.0 * w);
        int y = (int) ((90.0 - lat) / 180.0 * h);
        g.setColor(col);
        g.drawString(text, Math.clamp(x + 5, 10, w - 200), Math.clamp(y, 20, h - 20));
    }

    private void drawMapBanner(Graphics2D g, int w, int h, String title, String subtitle) {
        // Top banner
        g.setColor(new Color(15, 23, 42, 220));
        g.fillRect(0, 0, w, 48);
        g.setColor(new Color(51, 65, 85, 200));
        g.drawLine(0, 48, w, 48);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString(title, 20, 24);

        g.setColor(new Color(148, 163, 184));
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString(subtitle, 20, 41);
    }
}
