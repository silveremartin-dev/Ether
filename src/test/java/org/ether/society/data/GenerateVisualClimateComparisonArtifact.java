/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.procedural.PhysicalPlanetaryAtmosphereSolver;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.Base64;

/**
 * Generates comprehensive side-by-side comparison rasters and builds the interactive
 * Generative UI HTML dashboard 'climate_visual_comparison.html'.
 */
public class GenerateVisualClimateComparisonArtifact {

    private static final String ARTIFACT_DIR = "C:/Users/silve/.gemini/antigravity/brain/32078e09-a5c4-4806-a0a7-96efa9ac3c26";

    @Test
    public void generateVisualArtifact() throws Exception {
        File outDir = new File(ARTIFACT_DIR, "climate_renders");
        outDir.mkdirs();

        // 1. Force re-initialize WorldClim loader
        WorldClimEmpiricalRasterLoader.ensureInitialized();

        // 2. Generate Real Official Empirical 2025 Rasters
        System.out.println("Generating 2025 Official Empirical Rasters...");
        BufferedImage realBio12Precip = HistoricalMapGenerator.rasterizePrecipitationMap(2025);
        BufferedImage realBio1Temp     = HistoricalMapGenerator.rasterizeTemperatureMap(2025);
        BufferedImage realBio4Season   = HistoricalMapGenerator.rasterizeSeasonalityMap(2025);
        BufferedImage realBiomes       = HistoricalMapGenerator.rasterizeBiomesMap(2025);

        ImageIO.write(realBio12Precip, "PNG", new File(outDir, "earth_2025_empirical_precipitation.png"));
        ImageIO.write(realBio1Temp,     "PNG", new File(outDir, "earth_2025_empirical_temperature.png"));
        ImageIO.write(realBio4Season,   "PNG", new File(outDir, "earth_2025_empirical_seasonality.png"));
        ImageIO.write(realBiomes,       "PNG", new File(outDir, "earth_2025_empirical_biomes.png"));

        // 3. Generate 2.5D Physical Atmospheric Solver / Synthetic Rasters for 2025
        System.out.println("Generating 2.5D Physical Atmospheric Solver Rasters for 2025...");
        float[][] etopo = EtopoGeoTiffReader.loadEtopoGrid(2048, 1024);
        var physicalResult = PhysicalPlanetaryAtmosphereSolver.solveClimate(etopo, 2048, 1024, 1.0, 1.0, 0.71, 23.44);

        BufferedImage synthPrecip = renderGridToImage(physicalResult.precipitationGrid(), 0.0, 3000.0);
        BufferedImage synthTemp   = renderGridToImage(physicalResult.temperatureGrid(), -50.0, 50.0);
        BufferedImage synthSeason = renderGridToImage(physicalResult.seasonalityGrid(), 0.0, 50.0);
        BufferedImage synthBiomes = renderBiomeGrid(physicalResult.biomeGrid());

        ImageIO.write(synthPrecip, "PNG", new File(outDir, "earth_2025_synthetic_precipitation.png"));
        ImageIO.write(synthTemp,   "PNG", new File(outDir, "earth_2025_synthetic_temperature.png"));
        ImageIO.write(synthSeason, "PNG", new File(outDir, "earth_2025_synthetic_seasonality.png"));
        ImageIO.write(synthBiomes, "PNG", new File(outDir, "earth_2025_synthetic_biomes.png"));

        // 4. Create Direct Side-by-Side Stitched Images for Instant Chat Inspection
        createSideBySideComposite(
            realBio12Precip, "RÉFÉRENCE OFFICIELLE RÉELLE (WorldClim Bio12)",
            synthPrecip,     "MODÈLE SYNTHÉTIQUE (Solveur Physique 2.5D)",
            new File(ARTIFACT_DIR, "precipitation_side_by_side.png"),
            new File(outDir, "precipitation_side_by_side.png")
        );
        createSideBySideComposite(
            realBio1Temp, "RÉFÉRENCE OFFICIELLE RÉELLE (WorldClim Bio1)",
            synthTemp,    "MODÈLE SYNTHÉTIQUE (Bilan Radiatif 2.5D)",
            new File(ARTIFACT_DIR, "temperature_side_by_side.png"),
            new File(outDir, "temperature_side_by_side.png")
        );
        createSideBySideComposite(
            realBiomes,  "RÉFÉRENCE OFFICIELLE RÉELLE (ESA / WorldClim Biomes)",
            synthBiomes, "MODÈLE SYNTHÉTIQUE (Classification Holdridge 2.5D)",
            new File(ARTIFACT_DIR, "biomes_side_by_side.png"),
            new File(outDir, "biomes_side_by_side.png")
        );

        // 5. Generate ALL Historical Paleoclimatic Epochs (-100k, -50k, -25k, -20k, -10.9k, -10k, -8k, -6k, -3k, -1.9k, -1k, 0, 1492, 2026)
        long[] epochs = new long[] { -100000, -50000, -25000, -20000, -10900, -10000, -8000, -6000, -3000, -1900, -1000, 0, 1492, 2026 };
        for (long epoch : epochs) {
            System.out.println("Generating Paleoclimatic Rasters for epoch: " + epoch + "...");
            BufferedImage b = HistoricalMapGenerator.rasterizeBiomesMap(epoch);
            BufferedImage t = HistoricalMapGenerator.rasterizeTemperatureMap(epoch);
            BufferedImage p = HistoricalMapGenerator.rasterizePrecipitationMap(epoch);
            BufferedImage s = HistoricalMapGenerator.rasterizeSeasonalityMap(epoch);

            ImageIO.write(b, "PNG", new File(outDir, "earth_" + epoch + "_biomes.png"));
            ImageIO.write(t, "PNG", new File(outDir, "earth_" + epoch + "_temperature.png"));
            ImageIO.write(p, "PNG", new File(outDir, "earth_" + epoch + "_precipitation.png"));
            ImageIO.write(s, "PNG", new File(outDir, "earth_" + epoch + "_seasonality.png"));

            // Also persist directly into data/maps/ether/earth/<epoch>/
            File earthEpochDir = new File("data/maps/ether/earth/" + epoch);
            earthEpochDir.mkdirs();
            ImageIO.write(b, "PNG", new File(earthEpochDir, "earth_" + epoch + "_biomes.png"));
            ImageIO.write(t, "PNG", new File(earthEpochDir, "earth_" + epoch + "_temperature.png"));
            ImageIO.write(p, "PNG", new File(earthEpochDir, "earth_" + epoch + "_precipitation.png"));
            ImageIO.write(s, "PNG", new File(earthEpochDir, "earth_" + epoch + "_seasonality.png"));
        }

        // 6. Build rich interactive Generative UI dashboard
        buildGenerativeUIHtml(outDir);
        System.out.println("Enhanced Visual Climate Comparison Dashboard successfully created!");
    }

    private static void createSideBySideComposite(BufferedImage left, String leftTitle, BufferedImage right, String rightTitle, File... targets) throws Exception {
        int w = 1024;
        int h = 512;
        int bannerH = 40;
        int compW = w * 2 + 20;
        int compH = h + bannerH + 10;

        BufferedImage comp = new BufferedImage(compW, compH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = comp.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(new Color(15, 23, 42)); // Slate 900 background
        g.fillRect(0, 0, compW, compH);

        // Draw Left
        g.drawImage(left, 0, bannerH, w, h, null);
        g.setColor(new Color(16, 185, 129)); // Emerald
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString(leftTitle, 20, 26);

        // Separator
        g.setColor(new Color(51, 65, 85));
        g.fillRect(w + 5, 0, 10, compH);

        // Draw Right
        g.drawImage(right, w + 20, bannerH, w, h, null);
        g.setColor(new Color(245, 158, 11)); // Amber
        g.drawString(rightTitle, w + 40, 26);

        g.dispose();

        for (File target : targets) {
            target.getParentFile().mkdirs();
            ImageIO.write(comp, "PNG", target);
        }
    }

    private static BufferedImage renderGridToImage(float[][] grid, double minVal, double maxVal) {
        int h = grid.length;
        int w = grid[0].length;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double v = grid[y][x];
                double norm = Math.clamp((v - minVal) / (maxVal - minVal), 0.0, 1.0);
                int gray = (int) Math.round(norm * 255.0);
                img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return img;
    }

    private static BufferedImage renderBiomeGrid(int[][] grid) {
        int h = grid.length;
        int w = grid[0].length;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, grid[y][x]);
            }
        }
        return img;
    }

    private static void buildGenerativeUIHtml(File renderDir) throws Exception {
        File htmlFile = new File(ARTIFACT_DIR, "climate_visual_comparison.html");

        // Encode Base64 thumbnails
        String b64CompRain   = encodeThumbnail(new File(renderDir, "precipitation_side_by_side.png"));
        String b64CompTemp   = encodeThumbnail(new File(renderDir, "temperature_side_by_side.png"));
        String b64CompBiomes = encodeThumbnail(new File(renderDir, "biomes_side_by_side.png"));

        // Historical Epochs
        String b64Biomes100k = encodeThumbnail(new File(renderDir, "earth_-100000_biomes.png"));
        String b64Temp100k   = encodeThumbnail(new File(renderDir, "earth_-100000_temperature.png"));
        String b64Rain100k   = encodeThumbnail(new File(renderDir, "earth_-100000_precipitation.png"));

        String b64Biomes50k  = encodeThumbnail(new File(renderDir, "earth_-50000_biomes.png"));
        String b64Temp50k    = encodeThumbnail(new File(renderDir, "earth_-50000_temperature.png"));
        String b64Rain50k    = encodeThumbnail(new File(renderDir, "earth_-50000_precipitation.png"));

        String b64Biomes20k  = encodeThumbnail(new File(renderDir, "earth_-20000_biomes.png"));
        String b64Temp20k    = encodeThumbnail(new File(renderDir, "earth_-20000_temperature.png"));
        String b64Rain20k    = encodeThumbnail(new File(renderDir, "earth_-20000_precipitation.png"));

        String b64Biomes8k   = encodeThumbnail(new File(renderDir, "earth_-8000_biomes.png"));
        String b64Temp8k     = encodeThumbnail(new File(renderDir, "earth_-8000_temperature.png"));
        String b64Rain8k     = encodeThumbnail(new File(renderDir, "earth_-8000_precipitation.png"));

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"fr\">\n<head>\n");
        sb.append("  <meta charset=\"UTF-8\">\n");
        sb.append("  <title>Ether - Comparatif Côte-à-Côte Référence Réelle vs. Modèle Synthétique</title>\n");
        sb.append("  <script src=\"https://www.gstatic.com/antigravity/web/dev/tailwindcss.min.js\"></script>\n");
        sb.append("</head>\n");
        sb.append("<body class=\"bg-[var(--background)] text-[var(--foreground)] antialiased p-6 font-sans\">\n");
        sb.append("  <div class=\"max-w-7xl mx-auto space-y-8\">\n");

        // Header
        sb.append("    <header class=\"bg-[var(--card)] border border-[var(--border)] rounded-2xl p-6 shadow-sm\">\n");
        sb.append("      <div class=\"flex flex-col md:flex-row items-start md:items-center justify-between gap-4\">\n");
        sb.append("        <div>\n");
        sb.append("          <h1 class=\"text-2xl font-bold tracking-tight text-[var(--foreground)]\">🌍 Ether Climate Laboratory - Comparatif Côte-à-Côte Direct</h1>\n");
        sb.append("          <p class=\"text-sm text-[var(--muted-foreground)] mt-1\">Confrontation directe : Référence Empirique Réelle (WorldClim v2.1) vs Modèle Synthétique / Solveur Physique 2.5D & Paléoclimat</p>\n");
        sb.append("        </div>\n");
        sb.append("        <span class=\"px-3 py-1.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-600 border border-emerald-500/20 shadow-sm\">Option 3 : Hybride Empirique + Delta</span>\n");
        sb.append("      </div>\n");
        sb.append("    </header>\n");

        // Section 1: Direct Side-by-Side Precipitation Comparison
        sb.append("    <section class=\"bg-[var(--card)] border border-[var(--border)] rounded-2xl p-6 shadow-sm space-y-4\">\n");
        sb.append("      <div class=\"border-b border-[var(--border)] pb-3\">\n");
        sb.append("        <h2 class=\"text-lg font-bold text-[var(--foreground)] flex items-center gap-2\">🌧️ 1. Précipitations (An 2025) : Référence Réelle (Gauche) vs. Modèle Synthétique (Droite)</h2>\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"border border-[var(--border)] rounded-xl p-3 bg-[var(--background)]\">\n");
        sb.append("        <img src=\"").append(b64CompRain).append("\" class=\"w-full rounded-lg shadow-md\" alt=\"Precipitation Side-by-Side\" />\n");
        sb.append("      </div>\n");
        sb.append("    </section>\n");

        // Section 2: Direct Side-by-Side Temperature Comparison
        sb.append("    <section class=\"bg-[var(--card)] border border-[var(--border)] rounded-2xl p-6 shadow-sm space-y-4\">\n");
        sb.append("      <div class=\"border-b border-[var(--border)] pb-3\">\n");
        sb.append("        <h2 class=\"text-lg font-bold text-[var(--foreground)] flex items-center gap-2\">🌡️ 2. Température (An 2025) : Référence Réelle (Gauche) vs. Modèle Synthétique (Droite)</h2>\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"border border-[var(--border)] rounded-xl p-3 bg-[var(--background)]\">\n");
        sb.append("        <img src=\"").append(b64CompTemp).append("\" class=\"w-full rounded-lg shadow-md\" alt=\"Temperature Side-by-Side\" />\n");
        sb.append("      </div>\n");
        sb.append("    </section>\n");

        // Section 3: Direct Side-by-Side Biomes Comparison
        sb.append("    <section class=\"bg-[var(--card)] border border-[var(--border)] rounded-2xl p-6 shadow-sm space-y-4\">\n");
        sb.append("      <div class=\"border-b border-[var(--border)] pb-3\">\n");
        sb.append("        <h2 class=\"text-lg font-bold text-[var(--foreground)] flex items-center gap-2\">🌿 3. Biomes (An 2025) : Référence Réelle (Gauche) vs. Modèle Synthétique (Droite)</h2>\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"border border-[var(--border)] rounded-xl p-3 bg-[var(--background)]\">\n");
        sb.append("        <img src=\"").append(b64CompBiomes).append("\" class=\"w-full rounded-lg shadow-md\" alt=\"Biomes Side-by-Side\" />\n");
        sb.append("      </div>\n");
        sb.append("    </section>\n");

        // Section 4: Historical Epochs Gallery (-100k, -50k, -20k, -8k)
        sb.append("    <section class=\"bg-[var(--card)] border border-[var(--border)] rounded-2xl p-6 shadow-sm space-y-4\">\n");
        sb.append("      <div class=\"border-b border-[var(--border)] pb-3\">\n");
        sb.append("        <h2 class=\"text-lg font-bold text-[var(--foreground)]\">⏳ 4. Évolution des Époques Historiques (-100 000 BP à -8 000 BP)</h2>\n");
        sb.append("        <p class=\"text-xs text-[var(--muted-foreground)] mt-1\">Régénération complète de tous les calques cartographiques (continuité polaire et dérive paléoclimatique 2D sans bandes)</p>\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4\">\n");

        // -100 000 BP
        sb.append("        <div class=\"border border-[var(--border)] rounded-xl p-3 bg-[var(--background)] space-y-2\">\n");
        sb.append("          <div class=\"text-xs font-bold text-emerald-600\">-100 000 BP (MIS 5d)</div>\n");
        sb.append("          <img src=\"").append(b64Biomes100k).append("\" class=\"w-full rounded-lg border border-[var(--border)]\" alt=\"Biomes -100k\" />\n");
        sb.append("          <img src=\"").append(b64Temp100k).append("\" class=\"w-full rounded-lg border border-[var(--border)]\" alt=\"Temp -100k\" />\n");
        sb.append("          <img src=\"").append(b64Rain100k).append("\" class=\"w-full rounded-lg border border-[var(--border)]\" alt=\"Rain -100k\" />\n");
        sb.append("        </div>\n");

        // -50 000 BP
        sb.append("        <div class=\"border border-[var(--border)] rounded-xl p-3 bg-[var(--background)] space-y-2\">\n");
        sb.append("          <div class=\"text-xs font-bold text-blue-600\">-50 000 BP (MIS 3)</div>\n");
        sb.append("          <img src=\"").append(b64Biomes50k).append("\" class=\"w-full rounded-lg border border-[var(--border)]\" alt=\"Biomes -50k\" />\n");
        sb.append("          <img src=\"").append(b64Temp50k).append("\" class=\"w-full rounded-lg border border-[var(--border)]\" alt=\"Temp -50k\" />\n");
        sb.append("          <img src=\"").append(b64Rain50k).append("\" class=\"w-full rounded-lg border border-[var(--border)]\" alt=\"Rain -50k\" />\n");
        sb.append("        </div>\n");

        // -20 000 BP
        sb.append("        <div class=\"border border-[var(--border)] rounded-xl p-3 bg-[var(--background)] space-y-2\">\n");
        sb.append("          <div class=\"text-xs font-bold text-cyan-600\">-20 000 BP (LGM)</div>\n");
        sb.append("          <img src=\"").append(b64Biomes20k).append("\" class=\"w-full rounded-lg border border-[var(--border)]\" alt=\"Biomes -20k\" />\n");
        sb.append("          <img src=\"").append(b64Temp20k).append("\" class=\"w-full rounded-lg border border-[var(--border)]\" alt=\"Temp -20k\" />\n");
        sb.append("          <img src=\"").append(b64Rain20k).append("\" class=\"w-full rounded-lg border border-[var(--border)]\" alt=\"Rain -20k\" />\n");
        sb.append("        </div>\n");

        // -8 000 BP
        sb.append("        <div class=\"border border-[var(--border)] rounded-xl p-3 bg-[var(--background)] space-y-2\">\n");
        sb.append("          <div class=\"text-xs font-bold text-amber-600\">-8 000 BP (Sahara Vert)</div>\n");
        sb.append("          <img src=\"").append(b64Biomes8k).append("\" class=\"w-full rounded-lg border border-[var(--border)]\" alt=\"Biomes -8k\" />\n");
        sb.append("          <img src=\"").append(b64Temp8k).append("\" class=\"w-full rounded-lg border border-[var(--border)]\" alt=\"Temp -8k\" />\n");
        sb.append("          <img src=\"").append(b64Rain8k).append("\" class=\"w-full rounded-lg border border-[var(--border)]\" alt=\"Rain -8k\" />\n");
        sb.append("        </div>\n");

        sb.append("      </div>\n");
        sb.append("    </section>\n");

        sb.append("  </div>\n");
        sb.append("</body>\n</html>\n");

        try (FileWriter fw = new FileWriter(htmlFile)) {
            fw.write(sb.toString());
        }
    }

    private static String encodeThumbnail(File file) {
        if (!file.exists()) return "";
        try {
            BufferedImage orig = ImageIO.read(file);
            int tw = orig.getWidth() > 1024 ? 1024 : orig.getWidth();
            int th = (int) (orig.getHeight() * (tw / (double) orig.getWidth()));
            BufferedImage thumb = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(orig, 0, 0, tw, th, null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumb, "PNG", baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return "";
        }
    }
}
