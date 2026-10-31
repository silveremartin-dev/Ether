package org.ether.society.data;

import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TitanMapIngestorTest {

    @Test
    public void testIngestTitanTopographyAndGeology() throws Exception {
        File topoRaw = new File("data/ether/titan/2150/titan_topography_raw.png");
        File geolRaw = new File("data/ether/titan/2150/titan_geology_raw.png");
        assertTrue(topoRaw.exists(), "Topo raw image must exist");
        assertTrue(geolRaw.exists(), "Geol raw image must exist");

        BufferedImage imgTopo = ImageIO.read(topoRaw);
        BufferedImage imgGeol = ImageIO.read(geolRaw);
        System.out.println("Loaded Topo: " + imgTopo.getWidth() + "x" + imgTopo.getHeight());
        System.out.println("Loaded Geol: " + imgGeol.getWidth() + "x" + imgGeol.getHeight());

        // Standard planetary tensor grid dimensions: 2048 x 1024
        int outW = 2048;
        int outH = 1024;

        // 1. TOPOGRAPHY INGESTION
        // In imgTopo:
        // Top banner contains the colorbar (-1800m to 600m).
        // Let's find map area: the black bounding rectangle of the equirectangular map.
        // In imgTopo (approx 1024x512 or similar):
        // Top 10% is colorbar and axis labels (0 to 90N, 0 to 90S).
        // Let's sample the colorbar at y ~ 35 (or find colorbar row) to build exact color->meters LUT.
        // Alternatively, map each pixel RGB using HSV/Jet color mapping:
        // Jet/Rainbow colormap: Blue (-1800m) -> Cyan (-1000m) -> Green (-500m) -> Yellow (0m) -> Orange (+300m) -> Red (+600m).
        
        // Find map bounding box inside imgTopo:
        // Search for the inner border of the map
        int mapX0 = 0, mapY0 = 0, mapX1 = imgTopo.getWidth(), mapY1 = imgTopo.getHeight();
        for (int y = 0; y < imgTopo.getHeight() / 3; y++) {
            int blackCount = 0;
            for (int x = 0; x < imgTopo.getWidth(); x++) {
                int rgb = imgTopo.getRGB(x, y) & 0xFFFFFF;
                if (rgb == 0) blackCount++;
            }
            if (blackCount > imgTopo.getWidth() * 0.7) {
                mapY0 = y + 1; // map top border
                break;
            }
        }
        for (int y = imgTopo.getHeight() - 1; y > imgTopo.getHeight() * 2 / 3; y--) {
            int blackCount = 0;
            for (int x = 0; x < imgTopo.getWidth(); x++) {
                int rgb = imgTopo.getRGB(x, y) & 0xFFFFFF;
                if (rgb == 0) blackCount++;
            }
            if (blackCount > imgTopo.getWidth() * 0.7) {
                mapY1 = y; // map bottom border
                break;
            }
        }
        for (int x = 0; x < imgTopo.getWidth() / 4; x++) {
            int blackCount = 0;
            for (int y = mapY0; y < mapY1; y++) {
                int rgb = imgTopo.getRGB(x, y) & 0xFFFFFF;
                if (rgb == 0) blackCount++;
            }
            if (blackCount > (mapY1 - mapY0) * 0.7) {
                mapX0 = x + 1;
                break;
            }
        }
        for (int x = imgTopo.getWidth() - 1; x > imgTopo.getWidth() * 3 / 4; x--) {
            int blackCount = 0;
            for (int y = mapY0; y < mapY1; y++) {
                int rgb = imgTopo.getRGB(x, y) & 0xFFFFFF;
                if (rgb == 0) blackCount++;
            }
            if (blackCount > (mapY1 - mapY0) * 0.7) {
                mapX1 = x;
                break;
            }
        }

        System.out.printf("Map bounds detected: [%d, %d] to [%d, %d] (size %dx%d)\n",
                mapX0, mapY0, mapX1, mapY1, (mapX1 - mapX0), (mapY1 - mapY0));
        if (mapX1 <= mapX0 + 100 || mapY1 <= mapY0 + 100) {
            mapX0 = (int) (imgTopo.getWidth() * 0.08);
            mapX1 = (int) (imgTopo.getWidth() * 0.96);
            mapY0 = (int) (imgTopo.getHeight() * 0.12);
            mapY1 = (int) (imgTopo.getHeight() * 0.94);
        }

        BufferedImage elevOut = new BufferedImage(outW, outH, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < outH; y++) {
            double v = (double) y / (outH - 1);
            int srcY = Math.clamp((int) Math.round(mapY0 + v * (mapY1 - mapY0)), 0, imgTopo.getHeight() - 1);
            for (int x = 0; x < outW; x++) {
                double u = (double) x / (outW - 1);
                int srcX = Math.clamp((int) Math.round(mapX0 + u * (mapX1 - mapX0)), 0, imgTopo.getWidth() - 1);

                int rgb = imgTopo.getRGB(srcX, srcY);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Convert RGB in rainbow/jet colormap to elevation in meters [-1800, 600]:
                // Blue (-1800m) -> Cyan (-900m) -> Yellow (0m) -> Orange (+300m) -> Red (+600m)
                float[] hsv = Color.RGBtoHSB(r, g, b, null);
                float hue = hsv[0]; // Red = 0.0, Yellow = 0.16, Green = 0.33, Cyan = 0.5, Blue = 0.66
                float sat = hsv[1];
                float val = hsv[2];

                double elevMeters;
                if (sat < 0.15) {
                    // Grayscale / grid line / label
                    elevMeters = 0.0;
                } else if (hue >= 0.55f) { // Blue / deep basin
                    double frac = (hue - 0.55f) / 0.15f; // 0 (cyan-blue) to 1 (deep blue)
                    elevMeters = -900.0 - (frac * 900.0); // -900 to -1800
                } else if (hue >= 0.30f) { // Cyan to Green
                    double frac = (hue - 0.30f) / 0.25f;
                    elevMeters = -200.0 - (frac * 700.0); // -200 to -900
                } else if (hue >= 0.14f) { // Green to Yellow
                    double frac = (hue - 0.14f) / 0.16f;
                    elevMeters = 150.0 - (frac * 350.0); // +150 to -200
                } else { // Yellow to Orange to Red
                    double frac = hue / 0.14f; // 0 (red) to 1 (yellow)
                    elevMeters = 600.0 - (frac * 450.0); // +600 to +150
                }

                // Strictly map [-1800m, +600m] to 8-bit [0, 255]
                double normalized = (elevMeters - (-1800.0)) / (600.0 - (-1800.0));
                int grayVal = Math.clamp((int) Math.round(normalized * 255.0), 0, 255);

                int grayRgb = (grayVal << 16) | (grayVal << 8) | grayVal;
                elevOut.setRGB(x, y, grayRgb);
            }
        }

        File elevFile = new File("data/ether/titan/2150/titan_2150_elevation.png");
        ImageIO.write(elevOut, "png", elevFile);
        System.out.println("Saved normalized Titan elevation tensor to: " + elevFile.getAbsolutePath());

        // 2. GEOLOGY & BIOMES INGESTION from imgGeol
        // Top half of imgGeol is the equirectangular map (-90 to +90 lat, 0 to 360 lon)
        // Bottom half are the two polar stereographic projections
        int geolMapY0 = (int) (imgGeol.getHeight() * 0.04);
        int geolMapY1 = (int) (imgGeol.getHeight() * 0.44);
        int geolMapX0 = (int) (imgGeol.getWidth() * 0.06);
        int geolMapX1 = (int) (imgGeol.getWidth() * 0.94);

        BufferedImage biomeOut = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);
        BufferedImage oilOut = new BufferedImage(outW, outH, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage gasOut = new BufferedImage(outW, outH, BufferedImage.TYPE_BYTE_GRAY);

        int lakePixelCount = 0;
        int dunePixelCount = 0;
        int plainPixelCount = 0;
        int hummockyPixelCount = 0;
        int labyrinthPixelCount = 0;
        int craterPixelCount = 0;

        for (int y = 0; y < outH; y++) {
            double v = (double) y / (outH - 1);
            int srcY = Math.clamp((int) Math.round(geolMapY0 + v * (geolMapY1 - geolMapY0)), 0, imgGeol.getHeight() - 1);
            for (int x = 0; x < outW; x++) {
                double u = (double) x / (outW - 1);
                int srcX = Math.clamp((int) Math.round(geolMapX0 + u * (geolMapX1 - geolMapX0)), 0, imgGeol.getWidth() - 1);

                int rgb = imgGeol.getRGB(srcX, srcY);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                Biome targetBiome;
                int oilVal = 0;
                int gasVal = 0;

                // Color classifications from USGS / Lopes et al. 2020:
                // Blue: Lakes (Liquid Methane/Ethane) -> b > 150 && r < 80
                // Purple: Dunes (Organics) -> r > 100 && b > 120 && g < 80
                // Orange: Hummocky/Mountainous -> r > 180 && g > 120 && b < 80
                // Pink/Magenta: Labyrinth (Fluvial channels) -> r > 180 && b > 150 && g < 100
                // Red: Impact Craters -> r > 180 && g < 60 && b < 60
                // Cyan/Light Green: Plains -> g > 160 && b > 120
                if (b > 160 && r < 90) {
                    targetBiome = Biome.LAKE; // Liquid Hydrocarbon Seas (Kraken Mare, Ligeia Mare)
                    oilVal = 255;
                    gasVal = 240;
                    lakePixelCount++;
                } else if (r > 90 && b > 120 && g < 90) {
                    targetBiome = Biome.DESERT; // Organic dunes (Shangri-La, Fensal, Belet)
                    oilVal = 70; // Solid tholins / tar
                    dunePixelCount++;
                } else if (r > 170 && b > 140 && g < 110) {
                    targetBiome = Biome.HILLS; // Labyrinth channels
                    oilVal = 90;
                    gasVal = 100;
                    labyrinthPixelCount++;
                } else if (r > 180 && g < 80 && b < 80) {
                    targetBiome = Biome.SNOW; // Impact craters (Menrva, Selk)
                    craterPixelCount++;
                } else if (r > 170 && g > 110 && b < 90) {
                    targetBiome = Biome.MOUNTAINS; // Water ice hummocky ridges (Mithrim Montes)
                    hummockyPixelCount++;
                } else {
                    targetBiome = Biome.TUNDRA; // Broad undifferentiated organic plains
                    plainPixelCount++;
                }

                int biomeArgb = switch (targetBiome) {
                    case OCEAN -> (255 << 24) | (0 << 16) | (50 << 8) | 200;
                    case LAKE -> (255 << 24) | (30 << 16) | (120 << 8) | 220;
                    case DESERT -> (255 << 24) | (255 << 16) | (200 << 8) | 50;
                    case HILLS -> (255 << 24) | (150 << 16) | (150 << 8) | 100;
                    case MOUNTAINS -> (255 << 24) | (100 << 16) | (100 << 8) | 100;
                    case SNOW -> (255 << 24) | (255 << 16) | (255 << 8) | 255;
                    case TUNDRA -> (255 << 24) | (150 << 16) | (200 << 8) | 220;
                    default -> (255 << 24) | (100 << 16) | (200 << 8) | 50;
                };
                biomeOut.setRGB(x, y, biomeArgb);

                int oilRgb = (oilVal << 16) | (oilVal << 8) | oilVal;
                oilOut.setRGB(x, y, oilRgb);

                int gasRgb = (gasVal << 16) | (gasVal << 8) | gasVal;
                gasOut.setRGB(x, y, gasRgb);
            }
        }

        System.out.printf("Titan Biome Ingestion Stats: Lakes=%d, Dunes=%d, Plains=%d, Hummocky=%d, Labyrinths=%d, Craters=%d\n",
                lakePixelCount, dunePixelCount, plainPixelCount, hummockyPixelCount, labyrinthPixelCount, craterPixelCount);

        File biomeFile = new File("data/ether/titan/2150/titan_2150_biomes.png");
        File oilFile = new File("data/ether/titan/2150/titan_2150_oil.png");
        File gasFile = new File("data/ether/titan/2150/titan_2150_gas.png");

        ImageIO.write(biomeOut, "png", biomeFile);
        ImageIO.write(oilOut, "png", oilFile);
        ImageIO.write(gasOut, "png", gasFile);

        System.out.println("Ingestion completed successfully!");
    }
}

