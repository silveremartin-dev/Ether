package org.ether.society;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class GenerateEarthMaps {

    // Simple noise generator for terrain detail
    private static class SimplexNoise {
        private final int[] p = new int[512];
        public SimplexNoise(long seed) {
            int[] perm = new int[256];
            for (int i = 0; i < 256; i++) perm[i] = i;
            java.util.Random rand = new java.util.Random(seed);
            for (int i = 255; i > 0; i--) {
                int j = rand.nextInt(i + 1);
                int tmp = perm[i]; perm[i] = perm[j]; perm[j] = tmp;
            }
            for (int i = 0; i < 512; i++) p[i] = perm[i & 255];
        }

        private double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
        private double lerp(double t, double a, double b) { return a + t * (b - a); }
        private double grad(int hash, double x, double y) {
            int h = hash & 7;
            double u = h < 4 ? x : y;
            double v = h < 4 ? y : x;
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }

        public double eval(double x, double y) {
            int X = (int) Math.floor(x) & 255;
            int Y = (int) Math.floor(y) & 255;
            x -= Math.floor(x);
            y -= Math.floor(y);
            double u = fade(x);
            double v = fade(y);
            int A = p[X] + Y, B = p[X + 1] + Y;
            return lerp(v, lerp(u, grad(p[A], x, y), grad(p[B], x - 1, y)),
                           lerp(u, grad(p[A + 1], x, y - 1), grad(p[B + 1], x - 1, y - 1)));
        }
    }

    public static void main(String[] args) throws IOException {
        int width = 1024;
        int height = 512;

        BufferedImage elevImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        BufferedImage biomeImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        SimplexNoise noise1 = new SimplexNoise(42L);
        SimplexNoise noise2 = new SimplexNoise(100L);

        // Biome colors from ImageMapLoader/README.txt
        Color DEEP_OCEAN = new Color(0, 0, 100);
        Color OCEAN = new Color(0, 50, 200);
        Color BEACH = new Color(240, 220, 150);
        Color PLAINS = new Color(100, 200, 50);
        Color FOREST = new Color(20, 120, 20);
        Color JUNGLE = new Color(0, 80, 0);
        Color DESERT = new Color(255, 200, 50);
        Color HILLS = new Color(150, 150, 100);
        Color MOUNTAINS = new Color(100, 100, 100);
        Color TUNDRA = new Color(150, 200, 220);
        Color SNOW = new Color(255, 255, 255);

        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y / (double) height) * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = -180.0 + (x / (double) width) * 360.0;

                double landDist = getEarthLandDistance(lat, lon);

                // Multi-octave noise detail
                double nx = (lon + 180.0) / 360.0 * 8.0;
                double ny = (lat + 90.0) / 180.0 * 8.0;
                double detail = 0.5 * noise1.eval(nx, ny) + 0.25 * noise2.eval(nx * 2.0, ny * 2.0);

                double rawHeight = landDist + detail * 0.45;

                // Grayscale mapping for elevation:
                // Sea level = 141 (0.554 brightness)
                int gray;
                Color biomeColor;

                if (rawHeight < 0) { // Ocean
                    if (rawHeight < -0.4) {
                        gray = (int) Math.max(0, 50 + rawHeight * 80);
                        biomeColor = DEEP_OCEAN;
                    } else {
                        gray = (int) Math.max(50, 140 + rawHeight * 220);
                        biomeColor = OCEAN;
                    }
                } else { // Land
                    if (rawHeight < 0.05) {
                        gray = 142 + (int)(rawHeight * 100);
                        biomeColor = BEACH;
                    } else if (rawHeight > 0.65) {
                        gray = (int) Math.min(255, 210 + (rawHeight - 0.65) * 120);
                        biomeColor = MOUNTAINS;
                    } else if (rawHeight > 0.45) {
                        gray = (int) (180 + rawHeight * 50);
                        biomeColor = HILLS;
                    } else {
                        gray = (int) (145 + rawHeight * 80);
                        // Latitude climate mapping for land biomes
                        double absLat = Math.abs(lat);
                        if (absLat > 68) {
                            biomeColor = SNOW;
                        } else if (absLat > 55) {
                            biomeColor = TUNDRA;
                        } else if (absLat < 18 && (lon > -15 && lon < 55 || lon > 35 && lon < 70 || lon > -115 && lon < -100)) {
                            biomeColor = DESERT;
                        } else if (absLat < 15) {
                            biomeColor = JUNGLE;
                        } else if (absLat > 25 && absLat < 45 && (lon > -15 && lon < 50)) {
                            biomeColor = DESERT;
                        } else if (detail > 0.1) {
                            biomeColor = FOREST;
                        } else {
                            biomeColor = PLAINS;
                        }
                    }
                }

                gray = Math.max(0, Math.min(255, gray));
                elevImg.setRGB(x, y, new Color(gray, gray, gray).getRGB());
                biomeImg.setRGB(x, y, biomeColor.getRGB());
            }
        }

        File targetDir = new File("src/main/resources/maps");
        if (!targetDir.exists()) targetDir.mkdirs();

        File elevFile = new File(targetDir, "earth_elevation.png");
        File biomeFile = new File(targetDir, "earth_biomes.png");

        ImageIO.write(elevImg, "png", elevFile);
        ImageIO.write(biomeImg, "png", biomeFile);

        System.out.println("Generated Earth maps at " + elevFile.getAbsolutePath() + " and " + biomeFile.getAbsolutePath());
    }

    private static double getEarthLandDistance(double lat, double lon) {
        // Smooth distance field to continents
        double dist = -0.6; // Default deep ocean

        // 1. North America
        dist = Math.max(dist, circleField(lat, lon, 48, -100, 30, 35));
        dist = Math.max(dist, circleField(lat, lon, 60, -115, 20, 25));
        dist = Math.max(dist, circleField(lat, lon, 35, -90, 18, 20));

        // 2. South America
        dist = Math.max(dist, circleField(lat, lon, -10, -55, 20, 25));
        dist = Math.max(dist, circleField(lat, lon, -30, -60, 15, 18));

        // 3. Europe
        dist = Math.max(dist, circleField(lat, lon, 50, 15, 16, 20));
        dist = Math.max(dist, circleField(lat, lon, 60, 40, 18, 22));

        // 4. Africa
        dist = Math.max(dist, circleField(lat, lon, 5, 20, 24, 28));
        dist = Math.max(dist, circleField(lat, lon, -20, 24, 16, 18));
        dist = Math.max(dist, circleField(lat, lon, 22, 12, 18, 25));

        // 5. Asia
        dist = Math.max(dist, circleField(lat, lon, 55, 80, 30, 45));
        dist = Math.max(dist, circleField(lat, lon, 35, 100, 25, 35));
        dist = Math.max(dist, circleField(lat, lon, 20, 78, 15, 18)); // India
        dist = Math.max(dist, circleField(lat, lon, 65, 140, 25, 35)); // Siberia

        // 6. Australia
        dist = Math.max(dist, circleField(lat, lon, -25, 134, 16, 20));

        // 7. Antarctica
        if (lat < -65) {
            dist = Math.max(dist, 0.4 + (lat + 65) * (-0.02));
        }

        // 8. Greenland
        dist = Math.max(dist, circleField(lat, lon, 72, -40, 10, 15));

        // 9. British Isles & Japan
        dist = Math.max(dist, circleField(lat, lon, 54, -2, 5, 7));
        dist = Math.max(dist, circleField(lat, lon, 36, 138, 5, 8));

        return dist;
    }

    private static double circleField(double lat, double lon, double clat, double clon, double rLat, double rLon) {
        double dLat = (lat - clat) / rLat;
        double dLon = (lon - clon) / rLon;
        double d2 = dLat * dLat + dLon * dLon;
        if (d2 > 2.2) return -0.6;
        return 0.5 * (1.0 - d2);
    }
}
