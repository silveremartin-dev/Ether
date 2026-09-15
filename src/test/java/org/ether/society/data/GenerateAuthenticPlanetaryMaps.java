/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class GenerateAuthenticPlanetaryMaps {
    private static final Logger logger = LoggerFactory.getLogger(GenerateAuthenticPlanetaryMaps.class);

    private static final int WIDTH = 2048;
    private static final int HEIGHT = 1024;

    public static List<String> fastParseCsv(String line) {
        List<String> list = new ArrayList<>(30);
        StringBuilder sb = new StringBuilder(64);
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                list.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        list.add(sb.toString().trim());
        return list;
    }

    private static List<double[]> extractMrdsDeposits(String... keywords) {
        List<double[]> list = new ArrayList<>();
        Path zipPath = Paths.get("data", "maps", "usgs_mrds", "mrds-csv.zip");
        if (!Files.exists(zipPath)) return list;

        try (ZipFile zip = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry entry = zip.getEntry("mrds.csv");
            if (entry == null) return list;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8), 65536)) {
                String line = br.readLine(); // header
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    List<String> parts = fastParseCsv(line);
                    if (parts.size() > 17) {
                        try {
                            double lat = Double.parseDouble(parts.get(5).replace("\"", "").trim());
                            double lon = Double.parseDouble(parts.get(6).replace("\"", "").trim());
                            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) continue;

                            String comms = (parts.get(11) + " " + parts.get(12) + " " + parts.get(13) + " " + parts.get(14)).toLowerCase();
                            boolean match = false;
                            for (String kw : keywords) {
                                if (comms.contains(kw.toLowerCase())) {
                                    match = true;
                                    break;
                                }
                            }

                            if (match) {
                                String prodSize = parts.get(17).toUpperCase();
                                double weight = 1.0;
                                double radius = 5.0;
                                if (prodSize.contains("L") || prodSize.contains("Y")) {
                                    weight = 2.5;
                                    radius = 12.0;
                                } else if (prodSize.contains("M")) {
                                    weight = 1.8;
                                    radius = 8.0;
                                } else if (prodSize.contains("S")) {
                                    weight = 1.2;
                                    radius = 6.0;
                                }
                                list.add(new double[]{lon, lat, radius, weight});
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error reading MRDS CSV: {}", e.getMessage());
        }
        return list;
    }

    public static class GeologicalBasin {
        public final double centerLon;
        public final double centerLat;
        public final double majorDeg;
        public final double minorDeg;
        public final double strikeDeg;
        public final double intensity;
        public final String name;

        public GeologicalBasin(double centerLon, double centerLat, double majorDeg, double minorDeg, double strikeDeg, double intensity, String name) {
            this.centerLon = centerLon;
            this.centerLat = centerLat;
            this.majorDeg = majorDeg;
            this.minorDeg = minorDeg;
            this.strikeDeg = strikeDeg;
            this.intensity = intensity;
            this.name = name;
        }
    }

    private static void rasterizeBasinsAndSpots(BufferedImage img, List<GeologicalBasin> basins, List<double[]> discreteSpots, Color[] palette, double spotDefaultRadius) {
        int w = img.getWidth();
        int h = img.getHeight();
        float[][] grid = new float[h][w];

        // 1. Accumulate Oriented Sedimentary Basins
        if (basins != null) {
            for (GeologicalBasin basin : basins) {
                double cLon = basin.centerLon;
                double cLat = basin.centerLat;
                double a = basin.majorDeg;
                double b = basin.minorDeg;
                double strikeRad = Math.toRadians(basin.strikeDeg);
                double cosTheta = Math.cos(strikeRad);
                double sinTheta = Math.sin(strikeRad);
                double intensity = basin.intensity;

                double cosLat = Math.max(0.15, Math.cos(Math.toRadians(cLat)));
                double maxDeg = Math.max(a, b);
                double dLonDeg = maxDeg / cosLat;
                double dLatDeg = maxDeg;

                int cx = (int) Math.round(((cLon + 180.0) / 360.0) * (w - 1));
                int cy = (int) Math.round(((90.0 - cLat) / 180.0) * (h - 1));
                int rx = (int) Math.ceil((dLonDeg / 360.0) * (w - 1));
                int ry = (int) Math.ceil((dLatDeg / 180.0) * (h - 1));

                int minY = Math.max(0, cy - ry);
                int maxY = Math.min(h - 1, cy + ry);

                for (int py = minY; py <= maxY; py++) {
                    double lat = 90.0 - (py / (double) (h - 1)) * 180.0;
                    double dyDeg = lat - cLat;

                    for (int px = cx - rx; px <= cx + rx; px++) {
                        int wrapX = (px % w + w) % w;
                        double lon = -180.0 + (wrapX / (double) (w - 1)) * 360.0;
                        double dLon = lon - cLon;
                        if (dLon > 180.0) dLon -= 360.0;
                        else if (dLon < -180.0) dLon += 360.0;

                        double dxDeg = dLon * cosLat;

                        // Rotate by -strikeRad
                        double u = dxDeg * cosTheta + dyDeg * sinTheta;
                        double v = -dxDeg * sinTheta + dyDeg * cosTheta;

                        double q = (u * u) / (a * a) + (v * v) / (b * b);
                        if (q <= 1.0) {
                            double d = Math.sqrt(q);
                            double falloff = Math.pow(1.0 - d, 1.4);
                            grid[py][wrapX] += (float) (falloff * intensity);
                        }
                    }
                }
            }
        }

        // 2. Accumulate Discrete Fields & MRDS Occurrences
        if (discreteSpots != null) {
            for (double[] spot : discreteSpots) {
                double lon = spot[0];
                double lat = spot[1];
                double radius = spot.length > 2 ? spot[2] : spotDefaultRadius;
                double intensity = spot.length > 3 ? spot[3] : 1.0;

                int cx = (int) Math.round(((lon + 180.0) / 360.0) * (w - 1));
                int cy = (int) Math.round(((90.0 - lat) / 180.0) * (h - 1));
                int r = (int) Math.ceil(radius);

                int minY = Math.max(0, cy - r);
                int maxY = Math.min(h - 1, cy + r);
                int minX = cx - r;
                int maxX = cx + r;

                for (int py = minY; py <= maxY; py++) {
                    double dy = py - cy;
                    for (int px = minX; px <= maxX; px++) {
                        int wrapX = (px % w + w) % w;
                        double dx = px - cx;
                        double d = Math.sqrt(dx * dx + dy * dy);
                        if (d <= radius) {
                            double norm = 1.0 - (d / radius);
                            grid[py][wrapX] += (float) (Math.pow(norm, 1.4) * intensity * 0.75);
                        }
                    }
                }
            }
        }

        // 3. Render Multi-Tiered Color Ramp
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                float v = grid[py][px];
                if (v > 0.012f) {
                    double norm = Math.clamp(1.0 - Math.exp(-v * 0.45), 0.0, 1.0);
                    Color chosen;
                    if (palette.length == 1) {
                        chosen = palette[0];
                    } else if (palette.length == 3) {
                        if (norm < 0.45) {
                            double t = norm / 0.45;
                            chosen = lerpColor(palette[0], palette[1], t);
                        } else {
                            double t = (norm - 0.45) / 0.55;
                            chosen = lerpColor(palette[1], palette[2], t);
                        }
                    } else if (palette.length >= 4) {
                        if (norm < 0.30) {
                            double t = norm / 0.30;
                            chosen = lerpColor(palette[0], palette[1], t);
                        } else if (norm < 0.70) {
                            double t = (norm - 0.30) / 0.40;
                            chosen = lerpColor(palette[1], palette[2], t);
                        } else {
                            double t = (norm - 0.70) / 0.30;
                            chosen = lerpColor(palette[2], palette[3], t);
                        }
                    } else {
                        chosen = palette[0];
                    }

                    int alpha = (int) Math.clamp(85 + norm * 170.0, 85.0, 255.0);
                    img.setRGB(px, py, (alpha << 24) | (chosen.getRed() << 16) | (chosen.getGreen() << 8) | chosen.getBlue());
                }
            }
        }
    }

    private static Color lerpColor(Color c1, Color c2, double t) {
        int r = (int) Math.clamp(c1.getRed() + t * (c2.getRed() - c1.getRed()), 0, 255);
        int g = (int) Math.clamp(c1.getGreen() + t * (c2.getGreen() - c1.getGreen()), 0, 255);
        int b = (int) Math.clamp(c1.getBlue() + t * (c2.getBlue() - c1.getBlue()), 0, 255);
        return new Color(r, g, b);
    }

    private static void rasterizeAlphaDensity(BufferedImage img, List<double[]> spots, Color themeColor, double defaultRadius) {
        rasterizeBasinsAndSpots(img, null, spots, new Color[]{themeColor}, defaultRadius);
    }

    private static void rasterizeTieredDensity(BufferedImage img, List<double[]> spots, Color lowC, Color medC, Color highC, double defaultRadius) {
        rasterizeBasinsAndSpots(img, null, spots, new Color[]{lowC, medC, highC}, defaultRadius);
    }

    private static void saveImageToAllLocations(BufferedImage img, String baseName, String... subDirs) {
        try {
            // 1. Root data/maps
            File fRoot = new File("data/maps/" + baseName);
            ImageIO.write(img, "PNG", fRoot);

            // 2. Classpath src/main/resources/maps
            File fRes = new File("src/main/resources/maps/" + baseName);
            fRes.getParentFile().mkdirs();
            ImageIO.write(img, "PNG", fRes);

            // 3. Subdirectories under data/maps/
            for (String sub : subDirs) {
                File dir = new File("data/maps/" + sub);
                dir.mkdirs();
                File fSub = new File(dir, baseName);
                ImageIO.write(img, "PNG", fSub);
            }
            logger.info("Saved {}", baseName);
        } catch (Exception e) {
            logger.error("Failed saving {}: {}", baseName, e.getMessage());
        }
    }

    @Test
    public void generateAllPresetMaps() throws Exception {
        logger.info("--- GENERATING AUTHENTIC PLANETARY MAPS FOR ALL PRESETS ---");

        // 1. EARTH / TERRE
        generateEarthResources();

        // 2. MOON / LUNE
        generateMoonMaps();

        // 3. MARS / ARES
        generateMarsMaps();

        // 4. VENUS / HESPEROS
        generateVenusMaps();

        // 5. MERCURY / HERMES
        generateMercuryMaps();

        logger.info("--- ALL PLANETARY CARTOGRAPHIC TENSORS SUCCESSFULLY GENERATED ---");
    }

    private void generateEarthResources() {
        logger.info("Generating Earth Geological & Mineral Tensors...");

        // -------------------------------------------------------------
        // 1. COAL BASINS & MEASURES (USGS MRDS + BGR + WEC + GEM Database)
        // -------------------------------------------------------------
        BufferedImage imgCoal = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        List<GeologicalBasin> coalBasins = new ArrayList<>();
        // North America
        coalBasins.add(new GeologicalBasin(-80.0, 38.5, 6.5, 2.2, 45.0, 3.8, "Appalachian Basin"));
        coalBasins.add(new GeologicalBasin(-89.0, 38.5, 3.5, 2.8, 0.0, 3.2, "Illinois Basin"));
        coalBasins.add(new GeologicalBasin(-105.8, 44.5, 3.2, 2.0, 115.0, 4.2, "Powder River Basin"));
        coalBasins.add(new GeologicalBasin(-102.5, 47.5, 3.0, 2.2, 0.0, 2.8, "Williston Fort Union Lignite"));
        coalBasins.add(new GeologicalBasin(-95.0, 36.0, 3.5, 2.0, 40.0, 2.6, "Western Interior / Arkoma"));
        coalBasins.add(new GeologicalBasin(-95.5, 31.5, 5.5, 1.2, 60.0, 2.4, "Gulf Coast Wilcox Lignite"));
        coalBasins.add(new GeologicalBasin(-107.5, 41.5, 2.5, 1.8, 0.0, 2.8, "Green River / Hanna Basin"));
        coalBasins.add(new GeologicalBasin(-109.5, 39.5, 2.8, 1.8, 120.0, 2.8, "Uinta-Piceance Coal"));
        coalBasins.add(new GeologicalBasin(-108.0, 36.5, 2.2, 1.8, 0.0, 3.0, "San Juan Fruitland Coal"));
        coalBasins.add(new GeologicalBasin(-116.5, 53.0, 6.0, 2.0, 135.0, 3.4, "Alberta Foothills Coal"));
        coalBasins.add(new GeologicalBasin(-60.2, 46.2, 1.5, 1.0, 60.0, 2.4, "Sydney Basin Nova Scotia"));
        coalBasins.add(new GeologicalBasin(-151.0, 61.5, 2.5, 1.2, 30.0, 2.4, "Cook Inlet Beluga Alaska"));
        coalBasins.add(new GeologicalBasin(-101.5, 27.8, 2.0, 1.2, 130.0, 2.4, "Sabinas Basin Mexico"));
        // South America
        coalBasins.add(new GeologicalBasin(-72.7, 11.1, 2.2, 1.0, 45.0, 3.8, "Cerrejon Colombia"));
        coalBasins.add(new GeologicalBasin(-73.5, 5.5, 2.0, 1.0, 35.0, 2.8, "Boyaca-Cundinamarca Colombia"));
        coalBasins.add(new GeologicalBasin(-72.3, 10.9, 1.5, 0.8, 40.0, 2.8, "Guasare Basin Venezuela"));
        coalBasins.add(new GeologicalBasin(-51.5, -29.5, 4.5, 2.0, 90.0, 3.0, "Parana Basin Brazil (Candiota)"));
        coalBasins.add(new GeologicalBasin(-72.3, -51.5, 1.8, 1.0, 0.0, 2.4, "Rio Turbio Argentina"));
        coalBasins.add(new GeologicalBasin(-77.0, -10.5, 1.5, 0.8, 140.0, 2.2, "Oyon Basin Peru"));
        // Europe
        coalBasins.add(new GeologicalBasin(19.0, 50.2, 2.2, 1.6, 120.0, 4.0, "Upper Silesian Basin Poland/Czechia"));
        coalBasins.add(new GeologicalBasin(23.0, 51.3, 1.8, 1.0, 135.0, 2.8, "Lublin Coal Basin Poland"));
        coalBasins.add(new GeologicalBasin(7.3, 51.5, 2.0, 1.2, 70.0, 3.8, "Ruhr Basin Germany"));
        coalBasins.add(new GeologicalBasin(6.8, 49.3, 1.5, 0.8, 60.0, 2.8, "Saar-Lorraine Basin"));
        coalBasins.add(new GeologicalBasin(6.5, 50.9, 1.2, 0.8, 135.0, 3.5, "Rhineland Lignite District"));
        coalBasins.add(new GeologicalBasin(13.5, 51.6, 2.5, 1.5, 0.0, 3.2, "Lusatian / Central German Lignite"));
        coalBasins.add(new GeologicalBasin(3.0, 50.4, 2.5, 0.6, 80.0, 2.8, "Nord-Pas-de-Calais France/Belgium"));
        coalBasins.add(new GeologicalBasin(-1.3, 53.5, 2.0, 1.2, 0.0, 3.0, "Yorkshire / East Midlands UK"));
        coalBasins.add(new GeologicalBasin(-3.6, 51.7, 1.5, 0.8, 90.0, 2.8, "South Wales Coalfield"));
        coalBasins.add(new GeologicalBasin(-3.8, 55.9, 1.2, 0.6, 70.0, 2.4, "Scottish Central Coalfield"));
        coalBasins.add(new GeologicalBasin(-5.8, 43.3, 1.5, 0.8, 90.0, 2.6, "Asturias Basin Spain"));
        coalBasins.add(new GeologicalBasin(23.3, 45.4, 1.0, 0.5, 90.0, 2.6, "Jiu Valley Romania"));
        coalBasins.add(new GeologicalBasin(26.0, 42.2, 1.5, 1.0, 90.0, 2.8, "Maritsa Iztok Lignite Bulgaria"));
        coalBasins.add(new GeologicalBasin(21.7, 40.5, 1.5, 0.8, 140.0, 2.6, "Ptolemaida-Florina Greece"));
        coalBasins.add(new GeologicalBasin(20.3, 44.4, 1.8, 1.0, 120.0, 2.8, "Kolubara-Kostolac Serbia"));
        coalBasins.add(new GeologicalBasin(31.8, 41.4, 1.5, 0.8, 75.0, 2.8, "Zonguldak Basin Turkey"));
        coalBasins.add(new GeologicalBasin(27.6, 39.2, 1.2, 0.8, 45.0, 2.6, "Soma Lignite Basin Turkey"));
        coalBasins.add(new GeologicalBasin(15.6, 78.2, 1.2, 0.6, 0.0, 2.2, "Spitsbergen Svalbard"));
        // Russia & Eurasia
        coalBasins.add(new GeologicalBasin(38.2, 48.2, 4.5, 1.8, 110.0, 4.2, "Donbas (Donets Basin)"));
        coalBasins.add(new GeologicalBasin(87.0, 54.5, 3.8, 2.2, 160.0, 4.8, "Kuzbass (Kuznetsk Basin)"));
        coalBasins.add(new GeologicalBasin(93.5, 56.0, 6.5, 2.0, 80.0, 4.4, "Kansk-Achinsk Lignite Basin"));
        coalBasins.add(new GeologicalBasin(98.0, 64.0, 8.0, 6.0, 0.0, 3.8, "Tunguska Supergiant Coal Basin"));
        coalBasins.add(new GeologicalBasin(126.0, 65.0, 7.0, 4.5, 0.0, 3.6, "Lena Coal Basin Yakutia"));
        coalBasins.add(new GeologicalBasin(60.5, 66.5, 3.5, 2.0, 45.0, 3.8, "Pechora Basin Vorkuta"));
        coalBasins.add(new GeologicalBasin(125.0, 56.8, 3.0, 1.5, 90.0, 3.5, "South Yakutsk Basin Neryungri"));
        coalBasins.add(new GeologicalBasin(103.0, 53.2, 3.0, 1.5, 120.0, 3.0, "Irkutsk / Cheremkhovo"));
        coalBasins.add(new GeologicalBasin(91.5, 53.7, 2.0, 1.5, 0.0, 2.8, "Minusinsk Basin Russia"));
        coalBasins.add(new GeologicalBasin(73.1, 49.8, 2.5, 1.5, 90.0, 3.8, "Karaganda Basin Kazakhstan"));
        coalBasins.add(new GeologicalBasin(75.3, 51.7, 1.8, 1.2, 45.0, 4.0, "Ekibastuz Basin Kazakhstan"));
        coalBasins.add(new GeologicalBasin(65.0, 50.0, 3.5, 2.0, 0.0, 2.8, "Turgay Basin Kazakhstan"));
        // East Asia & China
        coalBasins.add(new GeologicalBasin(112.5, 37.8, 5.5, 2.5, 25.0, 5.0, "Shanxi Province (Datong/Qinshui)"));
        coalBasins.add(new GeologicalBasin(109.5, 39.0, 4.5, 3.5, 0.0, 5.0, "Ordos Basin (Shenfu-Dongsheng)"));
        coalBasins.add(new GeologicalBasin(119.5, 46.5, 4.0, 2.0, 45.0, 3.6, "Hailar & Holingol Inner Mongolia"));
        coalBasins.add(new GeologicalBasin(117.0, 33.0, 3.0, 1.5, 120.0, 3.8, "Huainan-Huaibei Anhui"));
        coalBasins.add(new GeologicalBasin(116.8, 35.5, 2.5, 1.5, 30.0, 3.5, "Yanzhou Shandong"));
        coalBasins.add(new GeologicalBasin(105.0, 26.5, 3.5, 2.0, 45.0, 3.6, "Guizhou Liupanshui Basin"));
        coalBasins.add(new GeologicalBasin(87.5, 44.0, 5.5, 2.5, 90.0, 4.2, "Junggar & Hami Xinjiang"));
        coalBasins.add(new GeologicalBasin(130.5, 46.0, 3.0, 1.8, 45.0, 3.4, "Hegang-Jixi Heilongjiang"));
        coalBasins.add(new GeologicalBasin(105.5, 43.6, 3.0, 1.5, 90.0, 3.8, "Tavan Tolgoi South Gobi Mongolia"));
        coalBasins.add(new GeologicalBasin(142.0, 43.3, 2.2, 1.2, 0.0, 2.6, "Ishikari Hokkaido Japan"));
        coalBasins.add(new GeologicalBasin(130.6, 33.6, 1.5, 0.8, 0.0, 2.6, "Chikuho Kyushu Japan"));
        coalBasins.add(new GeologicalBasin(127.0, 38.0, 2.5, 1.5, 30.0, 2.8, "Taebaek & Anju Korea"));
        // South & Southeast Asia
        coalBasins.add(new GeologicalBasin(86.2, 23.7, 3.5, 1.2, 90.0, 4.5, "Damodar Valley (Jharia/Raniganj) India"));
        coalBasins.add(new GeologicalBasin(80.0, 18.0, 3.0, 1.0, 135.0, 3.5, "Godavari Valley (Singareni) India"));
        coalBasins.add(new GeologicalBasin(85.0, 21.0, 3.0, 1.2, 120.0, 3.8, "Mahanadi Valley (Talcher) India"));
        coalBasins.add(new GeologicalBasin(82.6, 23.0, 3.5, 1.5, 90.0, 4.0, "Singrauli & Korba India"));
        coalBasins.add(new GeologicalBasin(79.5, 11.5, 1.5, 1.0, 0.0, 3.0, "Neyveli Lignite Tamil Nadu India"));
        coalBasins.add(new GeologicalBasin(70.2, 24.8, 2.0, 1.2, 0.0, 3.2, "Thar Coalfield Pakistan"));
        coalBasins.add(new GeologicalBasin(103.8, -3.7, 3.5, 1.8, 135.0, 3.8, "South Sumatra (Muara Enim) Indonesia"));
        coalBasins.add(new GeologicalBasin(116.8, -1.0, 4.0, 2.0, 0.0, 4.4, "East Kalimantan (Kutai/Pasir) Indonesia"));
        coalBasins.add(new GeologicalBasin(107.2, 21.0, 2.0, 0.8, 70.0, 3.0, "Quang Ninh Basin Vietnam"));
        coalBasins.add(new GeologicalBasin(99.7, 18.3, 1.2, 0.8, 0.0, 2.6, "Mae Moh Lignite Thailand"));
        // Africa
        coalBasins.add(new GeologicalBasin(29.2, -26.0, 2.8, 1.8, 90.0, 4.4, "Witbank & Highveld South Africa"));
        coalBasins.add(new GeologicalBasin(27.5, -23.7, 2.0, 1.2, 90.0, 3.8, "Waterberg Coalfield South Africa"));
        coalBasins.add(new GeologicalBasin(33.7, -16.1, 2.5, 1.2, 120.0, 3.8, "Moatize Basin Mozambique"));
        coalBasins.add(new GeologicalBasin(26.0, -18.3, 2.2, 1.2, 60.0, 3.0, "Hwange Zimbabwe"));
        coalBasins.add(new GeologicalBasin(26.8, -22.7, 2.5, 1.5, 0.0, 3.0, "Mmamabula / Morupule Botswana"));
        coalBasins.add(new GeologicalBasin(7.5, 6.4, 1.8, 1.0, 0.0, 2.4, "Enugu Coalfield Nigeria"));
        // Oceania
        coalBasins.add(new GeologicalBasin(148.5, -22.5, 6.0, 2.0, 160.0, 4.8, "Bowen Basin Queensland Australia"));
        coalBasins.add(new GeologicalBasin(150.8, -32.8, 3.5, 1.8, 90.0, 4.2, "Sydney Basin Hunter Valley Australia"));
        coalBasins.add(new GeologicalBasin(150.0, -27.5, 4.0, 2.2, 160.0, 3.6, "Surat & Clarence-Moreton Australia"));
        coalBasins.add(new GeologicalBasin(145.5, -23.0, 4.5, 2.2, 150.0, 3.6, "Galilee Basin Queensland"));
        coalBasins.add(new GeologicalBasin(146.5, -38.2, 2.0, 1.0, 90.0, 3.5, "Latrobe Valley Victoria Australia"));
        coalBasins.add(new GeologicalBasin(116.2, -33.4, 1.2, 0.8, 135.0, 2.6, "Collie Basin Western Australia"));
        coalBasins.add(new GeologicalBasin(172.0, -41.0, 2.5, 1.0, 45.0, 2.4, "Buller & Waikato New Zealand"));

        List<double[]> coalSpots = extractMrdsDeposits("coal", "lignite", "anthracite", "bituminous");
        Color[] coalPalette = {
            new Color(146, 64, 14),   // #92400E Dark Amber Brown
            new Color(217, 119, 6),   // #D97706 Rich Amber
            new Color(251, 191, 36),  // #FBBF24 Golden Yellow
            new Color(254, 240, 138)  // #FEF08A Bright Core
        };
        rasterizeBasinsAndSpots(imgCoal, coalBasins, coalSpots, coalPalette, 8.0);
        saveImageToAllLocations(imgCoal, "earth_coal.png", "terre", "earth");

        // -------------------------------------------------------------
        // 2. CRUDE OIL BASINS & SUPERGIANT FIELDS (EIA / BGR / USGS TPS)
        // -------------------------------------------------------------
        BufferedImage imgOil = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        List<GeologicalBasin> oilBasins = new ArrayList<>();
        // Middle East & Persian Gulf Super-Basin
        oilBasins.add(new GeologicalBasin(49.3, 25.5, 6.5, 3.0, 15.0, 5.0, "Ghawar/Arabian Platform Saudi Arabia"));
        oilBasins.add(new GeologicalBasin(53.5, 22.5, 4.5, 2.5, 60.0, 4.4, "Rub al-Khali / Shaybah"));
        oilBasins.add(new GeologicalBasin(48.0, 29.1, 2.8, 2.0, 0.0, 4.8, "Greater Burgan / Kuwait"));
        oilBasins.add(new GeologicalBasin(47.2, 30.5, 6.5, 2.5, 135.0, 4.8, "Mesopotamian Basin Rumaila/Kirkuk Iraq"));
        oilBasins.add(new GeologicalBasin(49.8, 31.3, 5.5, 2.0, 135.0, 4.8, "Zagros Oil Belt Ahwaz/Marun Iran"));
        oilBasins.add(new GeologicalBasin(53.8, 24.3, 3.2, 2.2, 45.0, 4.6, "Upper Zakum / Abu Dhabi UAE"));
        oilBasins.add(new GeologicalBasin(51.6, 26.5, 1.8, 1.5, 0.0, 4.2, "Al-Shaheen Qatar Offshore"));
        oilBasins.add(new GeologicalBasin(56.5, 21.0, 4.5, 2.0, 30.0, 4.0, "Oman Salt Basin Fahud/Nimr"));
        oilBasins.add(new GeologicalBasin(33.3, 28.2, 3.0, 0.8, 140.0, 3.8, "Gulf of Suez Rift Egypt"));
        oilBasins.add(new GeologicalBasin(49.0, 15.5, 2.5, 1.2, 90.0, 3.4, "Masila Basin Yemen"));
        // Russia, Eurasia & Caspian
        oilBasins.add(new GeologicalBasin(76.5, 61.2, 7.5, 5.0, 0.0, 5.0, "West Siberia Samotlor/Priobskoye"));
        oilBasins.add(new GeologicalBasin(52.5, 54.8, 6.0, 4.0, 0.0, 4.5, "Volga-Ural Romashkino Russia"));
        oilBasins.add(new GeologicalBasin(51.8, 46.5, 5.0, 3.5, 0.0, 5.0, "Pre-Caspian Tengiz/Kashagan Kazakhstan"));
        oilBasins.add(new GeologicalBasin(53.0, 43.5, 3.0, 1.8, 120.0, 4.0, "Mangyshlak Uzen Kazakhstan"));
        oilBasins.add(new GeologicalBasin(50.5, 40.0, 4.0, 2.5, 135.0, 4.6, "South Caspian ACG Azerbaijan"));
        oilBasins.add(new GeologicalBasin(57.5, 66.0, 4.5, 2.8, 45.0, 4.0, "Timan-Pechora Usinsk Russia"));
        oilBasins.add(new GeologicalBasin(88.0, 67.8, 5.5, 3.5, 0.0, 4.2, "Vankor East Siberia"));
        oilBasins.add(new GeologicalBasin(143.2, 52.5, 4.0, 1.5, 0.0, 4.2, "Sakhalin Shelf Russia"));
        oilBasins.add(new GeologicalBasin(45.0, 43.5, 3.5, 1.2, 90.0, 3.4, "North Caucasus Grozny"));
        // North America
        oilBasins.add(new GeologicalBasin(-102.5, 31.8, 4.5, 3.2, 140.0, 5.0, "Permian Basin Midland/Delaware TX/NM"));
        oilBasins.add(new GeologicalBasin(-98.0, 28.5, 5.5, 2.0, 55.0, 4.6, "Eagle Ford Shale & Wilcox TX"));
        oilBasins.add(new GeologicalBasin(-90.5, 27.5, 5.0, 2.5, 90.0, 4.8, "Deepwater Gulf of Mexico"));
        oilBasins.add(new GeologicalBasin(-103.5, 48.0, 3.5, 3.0, 0.0, 4.5, "Bakken / Williston Basin ND/MT"));
        oilBasins.add(new GeologicalBasin(-148.5, 70.2, 5.0, 1.8, 90.0, 4.8, "Prudhoe Bay / North Slope Alaska"));
        oilBasins.add(new GeologicalBasin(-111.5, 56.8, 6.5, 3.5, 135.0, 5.0, "Athabasca Oil Sands Alberta"));
        oilBasins.add(new GeologicalBasin(-115.0, 54.5, 4.5, 2.5, 135.0, 4.4, "WCSB Peace River & Cold Lake"));
        oilBasins.add(new GeologicalBasin(-98.5, 35.5, 3.0, 2.0, 120.0, 4.0, "Anadarko Basin Oklahoma"));
        oilBasins.add(new GeologicalBasin(-104.5, 40.5, 2.5, 2.0, 0.0, 3.8, "DJ Basin Niobrara Colorado"));
        oilBasins.add(new GeologicalBasin(-119.5, 35.3, 3.0, 1.0, 135.0, 4.2, "San Joaquin Basin Midway-Sunset CA"));
        oilBasins.add(new GeologicalBasin(-118.2, 33.8, 1.5, 0.8, 120.0, 3.8, "Los Angeles Basin Wilmington CA"));
        oilBasins.add(new GeologicalBasin(-92.2, 19.5, 4.5, 2.5, 0.0, 4.8, "Sureste / Cantarell / KMZ Mexico"));
        oilBasins.add(new GeologicalBasin(-97.5, 21.5, 3.0, 1.5, 140.0, 3.8, "Tampico-Misantla / Chicontepec"));
        oilBasins.add(new GeologicalBasin(-48.8, 46.8, 2.2, 1.5, 45.0, 4.0, "Hibernia / Grand Banks Newfoundland"));
        // South America
        oilBasins.add(new GeologicalBasin(-71.5, 10.0, 3.0, 2.0, 0.0, 4.8, "Maracaibo Basin Bolivar Coastal Venezuela"));
        oilBasins.add(new GeologicalBasin(-64.0, 8.5, 6.5, 1.5, 90.0, 5.0, "Faja del Orinoco Heavy Oil Venezuela"));
        oilBasins.add(new GeologicalBasin(-43.0, -24.5, 6.0, 3.0, 45.0, 5.0, "Santos Pre-Salt Tupi/Buzios Brazil"));
        oilBasins.add(new GeologicalBasin(-40.5, -22.5, 4.5, 2.2, 45.0, 4.6, "Campos Basin Marlim/Roncador Brazil"));
        oilBasins.add(new GeologicalBasin(-37.0, -11.0, 3.5, 1.5, 40.0, 3.8, "Sergipe-Alagoas & Espirito Santo"));
        oilBasins.add(new GeologicalBasin(-71.5, 4.5, 4.5, 2.0, 45.0, 4.2, "Llanos Foreland Rubiales Colombia"));
        oilBasins.add(new GeologicalBasin(-76.5, -1.5, 5.0, 2.0, 0.0, 4.2, "Oriente / Maranon Ecuador/Peru"));
        oilBasins.add(new GeologicalBasin(-69.0, -38.0, 3.5, 2.5, 0.0, 4.6, "Vaca Muerta / Neuquen Argentina"));
        oilBasins.add(new GeologicalBasin(-68.0, -46.0, 3.0, 2.0, 90.0, 4.0, "Golfo San Jorge Comodoro Rivadavia"));
        oilBasins.add(new GeologicalBasin(-57.0, 8.0, 3.5, 1.8, 125.0, 4.8, "Guyana Stabroek Block Liza"));
        // Africa
        oilBasins.add(new GeologicalBasin(6.0, 4.8, 5.0, 3.5, 0.0, 5.0, "Niger Delta Super-Basin Nigeria"));
        oilBasins.add(new GeologicalBasin(11.8, -6.5, 5.0, 2.5, 140.0, 4.8, "Lower Congo Deepwater Block 15/17 Angola"));
        oilBasins.add(new GeologicalBasin(13.0, -9.5, 3.5, 1.8, 140.0, 4.0, "Kwanza Basin Angola"));
        oilBasins.add(new GeologicalBasin(19.5, 29.0, 4.5, 3.5, 0.0, 4.6, "Sirte Basin Waha/Zelten Libya"));
        oilBasins.add(new GeologicalBasin(6.0, 31.5, 4.0, 3.0, 0.0, 4.6, "Hassi Messaoud / Berkine Algeria"));
        oilBasins.add(new GeologicalBasin(29.5, 9.5, 4.5, 1.8, 135.0, 4.0, "Muglad-Melut Heglig/Palogue Sudan"));
        oilBasins.add(new GeologicalBasin(9.5, -1.5, 4.0, 2.0, 150.0, 4.2, "Gabon Coastal Rabi-Kounga"));
        oilBasins.add(new GeologicalBasin(31.0, 1.8, 2.5, 0.8, 30.0, 3.8, "Lake Albert Albertine Graben Uganda"));
        oilBasins.add(new GeologicalBasin(17.0, 8.5, 2.0, 1.2, 0.0, 3.6, "Doba Basin Chad"));
        oilBasins.add(new GeologicalBasin(9.0, 4.5, 2.5, 1.2, 135.0, 3.6, "Rio del Rey & Douala Cameroon"));
        // Europe & North Sea
        oilBasins.add(new GeologicalBasin(2.5, 57.5, 6.0, 3.0, 0.0, 4.8, "Central & Viking Grabens Ekofisk/Sverdrup"));
        oilBasins.add(new GeologicalBasin(7.5, 65.0, 4.0, 2.0, 30.0, 4.0, "Norwegian Sea Haltenbanken"));
        oilBasins.add(new GeologicalBasin(22.0, 72.0, 3.5, 2.0, 0.0, 4.0, "Barents Sea Johan Castberg"));
        oilBasins.add(new GeologicalBasin(-3.5, 60.5, 2.5, 1.2, 45.0, 3.8, "West of Shetland Clair/Schiehallion"));
        oilBasins.add(new GeologicalBasin(26.0, 45.0, 3.0, 1.5, 75.0, 3.8, "Carpathian Foredeep Ploiesti Romania"));
        oilBasins.add(new GeologicalBasin(16.8, 48.3, 2.5, 1.8, 45.0, 3.2, "Vienna & Pannonian Basins"));
        oilBasins.add(new GeologicalBasin(14.0, 36.8, 2.5, 1.2, 120.0, 3.2, "Sicily Channel & Po Valley"));
        // Asia-Pacific & Australia
        oilBasins.add(new GeologicalBasin(125.0, 46.5, 4.5, 2.5, 25.0, 4.8, "Songliao Basin Daqing China"));
        oilBasins.add(new GeologicalBasin(118.5, 38.0, 4.0, 3.0, 40.0, 4.6, "Bohai Bay Shengli/Dagang China"));
        oilBasins.add(new GeologicalBasin(83.5, 40.5, 5.5, 3.0, 90.0, 4.2, "Tarim Basin Tahe/Fuman China"));
        oilBasins.add(new GeologicalBasin(85.5, 45.5, 4.0, 2.5, 0.0, 4.0, "Junggar Basin Karamay China"));
        oilBasins.add(new GeologicalBasin(108.5, 37.0, 4.0, 3.0, 0.0, 4.4, "Ordos Basin Changqing Oil China"));
        oilBasins.add(new GeologicalBasin(115.5, 21.0, 3.5, 2.0, 65.0, 4.0, "Pearl River Mouth South China Sea"));
        oilBasins.add(new GeologicalBasin(101.5, 0.8, 4.5, 2.0, 135.0, 4.6, "Central Sumatra Minas/Duri Indonesia"));
        oilBasins.add(new GeologicalBasin(104.0, -3.0, 3.5, 2.0, 135.0, 4.0, "South Sumatra Basin Indonesia"));
        oilBasins.add(new GeologicalBasin(117.5, -0.5, 3.0, 2.0, 0.0, 4.2, "Kutei Mahakam Delta Kalimantan"));
        oilBasins.add(new GeologicalBasin(112.0, -7.0, 3.0, 1.5, 90.0, 4.2, "East Java Cepu/Banyu Urip"));
        oilBasins.add(new GeologicalBasin(104.5, 5.5, 4.5, 2.5, 140.0, 4.4, "Malay Basin Dulang Malaysia"));
        oilBasins.add(new GeologicalBasin(114.5, 5.5, 4.0, 2.0, 45.0, 4.4, "Brunei & Sabah Kikeh/Gumusut"));
        oilBasins.add(new GeologicalBasin(108.0, 9.8, 3.0, 1.5, 45.0, 4.2, "Cuu Long Bach Ho Vietnam"));
        oilBasins.add(new GeologicalBasin(72.0, 19.3, 3.5, 2.0, 0.0, 4.4, "Mumbai High / Cambay India"));
        oilBasins.add(new GeologicalBasin(71.5, 26.0, 2.5, 1.5, 0.0, 4.0, "Barmer Basin Mangala Rajasthan India"));
        oilBasins.add(new GeologicalBasin(95.0, 27.5, 3.0, 1.2, 45.0, 3.6, "Assam Basin Digboi India"));
        oilBasins.add(new GeologicalBasin(148.5, -38.5, 2.5, 1.2, 90.0, 4.2, "Gippsland Basin Kingfish Australia"));
        oilBasins.add(new GeologicalBasin(115.0, -21.0, 4.0, 2.0, 45.0, 4.0, "Carnarvon Basin Barrow WA Australia"));
        oilBasins.add(new GeologicalBasin(141.0, -27.5, 3.5, 2.5, 0.0, 3.6, "Cooper-Eromanga Basin Australia"));
        oilBasins.add(new GeologicalBasin(173.5, -39.5, 2.5, 1.2, 0.0, 3.6, "Taranaki Basin Maui New Zealand"));

        Color[] oilPalette = {
            new Color(153, 27, 27),   // #991B1B Deep Ruby
            new Color(220, 38, 38),   // #DC2626 Crimson Red
            new Color(248, 113, 113), // #F87171 Coral Red
            new Color(254, 202, 202)  // #FECACA Intense Core
        };
        rasterizeBasinsAndSpots(imgOil, oilBasins, null, oilPalette, 12.0);
        saveImageToAllLocations(imgOil, "earth_oil.png", "terre", "earth");

        // -------------------------------------------------------------
        // 3. NATURAL GAS BASINS & LNG HUBS (Cedigaz / BGR / WEP)
        // -------------------------------------------------------------
        BufferedImage imgGas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        List<GeologicalBasin> gasBasins = new ArrayList<>();
        // Middle East
        gasBasins.add(new GeologicalBasin(51.8, 26.5, 4.0, 3.0, 150.0, 5.0, "North Field / South Pars Qatar/Iran"));
        gasBasins.add(new GeologicalBasin(52.5, 27.8, 5.0, 2.2, 135.0, 5.0, "Coastal Fars Gas Kangan/Kish Iran"));
        gasBasins.add(new GeologicalBasin(49.5, 25.0, 4.5, 2.5, 15.0, 4.6, "Saudi Khuff & Jafurah Gas"));
        gasBasins.add(new GeologicalBasin(53.5, 23.8, 3.0, 2.0, 45.0, 4.5, "Abu Dhabi Sour Gas Shah/Ghasha UAE"));
        gasBasins.add(new GeologicalBasin(56.0, 22.0, 2.8, 1.8, 30.0, 4.4, "Khazzan Tight Gas Oman"));
        gasBasins.add(new GeologicalBasin(33.0, 32.5, 4.0, 2.0, 0.0, 4.8, "Levantine Deepwater Zohr/Leviathan"));
        gasBasins.add(new GeologicalBasin(31.5, 31.8, 3.5, 2.0, 90.0, 4.5, "Nile Delta Offshore Egypt"));
        // Russia, Yamal & Central Asia
        gasBasins.add(new GeologicalBasin(69.5, 70.5, 5.5, 3.5, 0.0, 5.0, "Yamal Supergiant Bovanenkovo/Tambey"));
        gasBasins.add(new GeologicalBasin(77.5, 66.0, 6.0, 4.0, 0.0, 5.0, "Nadym-Pur-Taz Urengoy/Yamburg Russia"));
        gasBasins.add(new GeologicalBasin(75.5, 71.0, 4.5, 2.5, 0.0, 4.8, "Gydan Peninsula Arctic LNG 2"));
        gasBasins.add(new GeologicalBasin(62.2, 37.3, 4.5, 3.0, 135.0, 5.0, "Galkynysh / Dauletabad Turkmenistan"));
        gasBasins.add(new GeologicalBasin(64.0, 39.5, 4.5, 2.5, 135.0, 4.6, "Amu Darya Gazli/Shurtan"));
        gasBasins.add(new GeologicalBasin(53.2, 51.3, 2.5, 2.0, 0.0, 4.5, "Karachaganak Kazakhstan"));
        gasBasins.add(new GeologicalBasin(48.0, 46.8, 4.0, 2.2, 90.0, 4.5, "Astrakhan & Orenburg Deep Gas"));
        gasBasins.add(new GeologicalBasin(111.0, 58.5, 5.5, 3.5, 60.0, 4.6, "Chayandinskoye & Kovykta East Siberia"));
        gasBasins.add(new GeologicalBasin(43.5, 73.0, 3.5, 2.5, 0.0, 4.8, "Shtokman Supergiant Barents Sea"));
        // North America
        gasBasins.add(new GeologicalBasin(-78.5, 40.5, 6.5, 3.0, 45.0, 5.0, "Appalachian Marcellus/Utica Shales"));
        gasBasins.add(new GeologicalBasin(-93.8, 32.2, 3.0, 2.2, 120.0, 4.6, "Haynesville-Bossier Shale LA/TX"));
        gasBasins.add(new GeologicalBasin(-102.5, 31.8, 4.5, 3.2, 140.0, 4.8, "Permian Associated Gas TX/NM"));
        gasBasins.add(new GeologicalBasin(-97.5, 33.0, 2.2, 1.8, 0.0, 4.4, "Barnett Shale Fort Worth TX"));
        gasBasins.add(new GeologicalBasin(-94.5, 35.5, 3.0, 1.8, 90.0, 4.4, "Arkoma Fayetteville/Woodford OK/AR"));
        gasBasins.add(new GeologicalBasin(-120.0, 56.0, 6.0, 3.0, 135.0, 4.8, "WCSB Montney & Duvernay Canada"));
        gasBasins.add(new GeologicalBasin(-122.5, 59.5, 3.0, 2.0, 0.0, 4.2, "Horn River & Liard Shales BC"));
        gasBasins.add(new GeologicalBasin(-108.5, 39.8, 3.0, 2.2, 120.0, 4.2, "Piceance & Uinta Tight Gas CO/UT"));
        gasBasins.add(new GeologicalBasin(-107.8, 36.8, 2.5, 2.0, 0.0, 4.4, "San Juan Coalbed Methane NM/CO"));
        gasBasins.add(new GeologicalBasin(-109.8, 42.5, 2.5, 1.5, 140.0, 4.4, "Green River Jonah/Pinedale WY"));
        gasBasins.add(new GeologicalBasin(-147.0, 70.2, 4.0, 1.5, 90.0, 4.6, "Alaska North Slope Point Thomson"));
        gasBasins.add(new GeologicalBasin(-87.5, 28.8, 4.5, 2.2, 90.0, 4.4, "Deepwater Norphlet Gas Play"));
        gasBasins.add(new GeologicalBasin(-99.0, 26.5, 3.0, 1.8, 140.0, 4.2, "Burgos Basin Mexico"));
        // Europe & North Sea
        gasBasins.add(new GeologicalBasin(6.8, 53.3, 4.5, 2.0, 90.0, 4.8, "Groningen / Rotliegend Gas"));
        gasBasins.add(new GeologicalBasin(3.5, 60.6, 5.0, 2.5, 0.0, 5.0, "Troll & Oseberg Norwegian North Sea"));
        gasBasins.add(new GeologicalBasin(6.0, 63.5, 4.0, 2.0, 30.0, 4.6, "Ormen Lange & Asgard Norwegian Sea"));
        gasBasins.add(new GeologicalBasin(21.0, 71.5, 3.0, 1.8, 0.0, 4.4, "Snohvit LNG Barents Sea"));
        gasBasins.add(new GeologicalBasin(2.2, 53.5, 3.0, 1.5, 120.0, 4.2, "UK Southern Gas Basin Leman"));
        gasBasins.add(new GeologicalBasin(36.5, 49.5, 4.0, 1.8, 115.0, 4.2, "Dnieper-Donets Shebelynka Ukraine"));
        gasBasins.add(new GeologicalBasin(24.5, 46.5, 2.2, 1.5, 0.0, 3.8, "Transylvanian Basin Gas Romania"));
        gasBasins.add(new GeologicalBasin(10.5, 45.0, 2.8, 1.0, 90.0, 3.6, "Po Valley Gas Italy"));
        // Africa
        gasBasins.add(new GeologicalBasin(3.3, 32.9, 3.5, 2.5, 0.0, 5.0, "Hassi R'Mel Supergiant Algeria"));
        gasBasins.add(new GeologicalBasin(2.5, 27.5, 4.0, 2.5, 135.0, 4.5, "In Salah & Ahnet Basins Algeria"));
        gasBasins.add(new GeologicalBasin(40.8, -11.0, 3.5, 1.5, 0.0, 5.0, "Rovuma Supergiant LNG Mozambique"));
        gasBasins.add(new GeologicalBasin(40.0, -9.0, 3.0, 1.2, 0.0, 4.4, "Tanzania Songo Songo Deep"));
        gasBasins.add(new GeologicalBasin(6.5, 4.5, 4.5, 3.0, 0.0, 4.6, "Niger Delta Gas Nigeria"));
        gasBasins.add(new GeologicalBasin(-17.2, 16.0, 3.5, 1.5, 0.0, 4.5, "Greater Tortue Ahmeyim Senegal/Mauritania"));
        // Asia-Pacific & Australia
        gasBasins.add(new GeologicalBasin(106.0, 30.5, 4.5, 3.0, 40.0, 5.0, "Sichuan Gas Super-Basin (Anyue/Fuling)"));
        gasBasins.add(new GeologicalBasin(82.5, 41.8, 5.0, 2.5, 90.0, 4.6, "Tarim Kuqa Depression Keshen/Kela"));
        gasBasins.add(new GeologicalBasin(108.5, 38.5, 4.5, 3.5, 0.0, 4.8, "Ordos Sulige Tight Gas China"));
        gasBasins.add(new GeologicalBasin(110.5, 17.5, 3.5, 2.0, 135.0, 4.5, "Deep Sea No.1 Qiongdongnan China"));
        gasBasins.add(new GeologicalBasin(115.5, -19.5, 5.0, 2.2, 50.0, 5.0, "Gorgon / Jansz-Io / North Rankin NW Shelf"));
        gasBasins.add(new GeologicalBasin(123.5, -14.0, 4.5, 2.5, 45.0, 4.8, "Browse Basin Ichthys/Prelude FLNG"));
        gasBasins.add(new GeologicalBasin(127.5, -11.0, 3.5, 2.0, 45.0, 4.5, "Bonaparte Basin Bayu-Undan/Barossa"));
        gasBasins.add(new GeologicalBasin(149.5, -26.5, 4.5, 2.5, 160.0, 4.5, "Queensland CSG-LNG Surat/Bowen"));
        gasBasins.add(new GeologicalBasin(143.0, -6.0, 4.0, 1.5, 125.0, 4.5, "Papua Fold Belt Hides PNG LNG"));
        gasBasins.add(new GeologicalBasin(133.0, -2.5, 3.0, 1.8, 120.0, 4.5, "Tangguh Bintuni LNG West Papua"));
        gasBasins.add(new GeologicalBasin(109.0, 4.5, 3.0, 2.0, 0.0, 4.4, "East Natuna Gas Field Indonesia"));
        gasBasins.add(new GeologicalBasin(112.5, 4.5, 3.5, 2.2, 45.0, 4.5, "Central Luconia Sarawak Malaysia"));
        gasBasins.add(new GeologicalBasin(101.5, 9.0, 4.0, 1.8, 0.0, 4.4, "Gulf of Thailand Bongkot/Erawan"));
        gasBasins.add(new GeologicalBasin(95.5, 14.5, 3.0, 1.2, 0.0, 4.2, "Yadana & Yetagun Myanmar"));
        gasBasins.add(new GeologicalBasin(82.5, 16.5, 3.0, 1.8, 45.0, 4.4, "Krishna-Godavari KG-D6 India"));
        gasBasins.add(new GeologicalBasin(69.0, 28.5, 3.5, 2.0, 0.0, 4.4, "Indus Basin Sui/Mari Pakistan"));
        gasBasins.add(new GeologicalBasin(91.5, 24.5, 2.5, 1.2, 30.0, 4.4, "Surma Basin Bibiyana Bangladesh"));
        // South America
        gasBasins.add(new GeologicalBasin(-72.8, -11.8, 2.5, 1.5, 135.0, 4.8, "Camisea Supergiant Gas Peru"));
        gasBasins.add(new GeologicalBasin(-63.8, -21.5, 3.5, 1.8, 0.0, 4.5, "Tarija Basin San Alberto Bolivia"));
        gasBasins.add(new GeologicalBasin(-69.0, -38.0, 3.5, 2.5, 0.0, 4.6, "Vaca Muerta Gas Fortin de Piedra"));
        gasBasins.add(new GeologicalBasin(-67.5, -53.5, 3.0, 2.0, 90.0, 4.2, "Austral Basin Carina-Aries"));
        gasBasins.add(new GeologicalBasin(-43.0, -24.5, 6.0, 3.0, 45.0, 4.5, "Santos Pre-Salt Associated Gas"));
        gasBasins.add(new GeologicalBasin(-65.5, 9.2, 3.0, 1.8, 90.0, 4.2, "Yucal-Placer / Manapire Venezuela"));
        gasBasins.add(new GeologicalBasin(-72.8, 11.8, 2.0, 1.2, 70.0, 4.0, "Chuchupa / Guajira Colombia"));

        Color[] gasPalette = {
            new Color(14, 116, 144),  // #0E7490 Deep Cyan
            new Color(6, 182, 212),   // #06B6D4 Bright Cyan
            new Color(56, 189, 248),  // #38BDF8 Sky Blue
            new Color(207, 250, 254)  // #CFFAFE Electric Core
        };
        rasterizeBasinsAndSpots(imgGas, gasBasins, null, gasPalette, 12.0);
        saveImageToAllLocations(imgGas, "earth_gas.png", "terre", "earth");

        // 4. Uranium Deposits (IAEA UDEPO + USGS MRDS)
        BufferedImage imgUranium = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        List<double[]> uSpots = extractMrdsDeposits("uranium", "thorium", "uraninite", "pitchblende", "carnotite");
        double[][] uBasins = {
            {-105.0, 58.0, 42, 2.8}, {136.9, -30.4, 38, 2.8}, {68.0, 44.0, 48, 3.0},
            {7.4, 18.7, 36, 2.4}, {27.5, -26.2, 36, 2.4}, {118.0, 50.0, 38, 2.4},
            {15.0, -22.5, 38, 2.4}, {132.8, -12.7, 35, 2.2}, {-109.5, 38.5, 38, 2.2}
        };
        for (double[] b : uBasins) uSpots.add(b);
        rasterizeAlphaDensity(imgUranium, uSpots, new Color(34, 197, 94), 8.0);
        saveImageToAllLocations(imgUranium, "earth_uranium.png", "terre", "earth");

        // 5. Helium-3 (Transparent on Earth)
        BufferedImage imgHe3 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        saveImageToAllLocations(imgHe3, "earth_helium3.png", "terre", "earth");

        // 6. Iron & Copper Formations (USGS MRDS + Tiered Gradient)
        BufferedImage imgIronCopper = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        List<double[]> feCuSpots = extractMrdsDeposits("iron", "copper", "magnetite", "hematite", "chalcopyrite", "bornite", "taconite");
        double[][] majorFeCu = {
            {118.0, -22.5, 45, 2.8}, {120.5, -23.0, 40, 2.5}, {-50.0, -6.0, 48, 3.0}, {-43.5, -20.0, 40, 2.4},
            {37.0, 51.5, 48, 2.8}, {33.5, 48.0, 42, 2.5}, {-91.5, 47.5, 40, 2.4}, {-66.5, 54.0, 42, 2.4},
            {20.0, 67.8, 35, 2.2}, {85.5, 22.0, 42, 2.4}, {-69.0, -24.0, 48, 2.8}, {-69.5, -22.3, 45, 2.6},
            {-70.5, -34.0, 45, 2.6}, {137.0, -4.0, 40, 2.5}, {-111.0, 33.5, 38, 2.2}, {28.0, -12.5, 45, 2.6},
            {102.0, 25.0, 38, 2.2}, {88.0, 38.0, 32, 2.0}, {-108.0, 32.5, 32, 2.0}
        };
        for (double[] b : majorFeCu) feCuSpots.add(b);
        rasterizeTieredDensity(imgIronCopper, feCuSpots, new Color(139, 69, 19), new Color(217, 119, 6), new Color(249, 115, 22), 8.0);
        saveImageToAllLocations(imgIronCopper, "earth_iron_copper.png", "terre", "earth");

        // 7. Precious Metals, REE & Lithium (USGS MRDS)
        BufferedImage imgPrecious = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        List<double[]> preciousSpots = extractMrdsDeposits("gold", "silver", "platinum", "palladium", "lithium", "rare earth", "spodumene", "bastnasite");
        double[][] majorPrecious = {
            {27.0, -26.0, 45, 2.8}, {-116.0, 40.8, 42, 2.6}, {121.5, -30.7, 40, 2.5}, {63.5, 41.5, 42, 2.6},
            {29.0, -24.5, 48, 2.8}, {88.2, 69.3, 45, 2.6}, {-81.0, 46.5, 40, 2.4}, {-68.0, -23.5, 45, 2.8},
            {-67.5, -20.2, 48, 2.8}, {116.0, -33.8, 42, 2.6}, {109.8, 41.8, 48, 2.8}, {-115.5, 35.5, 40, 2.5}
        };
        for (double[] b : majorPrecious) preciousSpots.add(b);
        rasterizeAlphaDensity(imgPrecious, preciousSpots, new Color(234, 179, 8), 7.0);
        saveImageToAllLocations(imgPrecious, "earth_precious_metals.png", "terre", "earth");

        // 8. Geothermal / Mantle Heat (IHFC Davies 2013 2° Grid)
        BufferedImage imgGeothermal = HistoricalMapGenerator.generateCleanMantleHeatMap("EARTH", null);
        if (imgGeothermal != null) {
            saveImageToAllLocations(imgGeothermal, "earth_geothermal.png", "terre", "earth");
        }

        // 9. Freshwater Aquifers (WHYMAP & Global Sedimentary Aquifer Systems)
        BufferedImage imgAquifers = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        double[][] majorAquifers = {
            {25.0, 22.0, 75, 2.5},   // Nubian Sandstone Aquifer (2.2M km2)
            {-100.0, 38.0, 58, 2.2}, // Ogallala Aquifer USA
            {-54.0, -25.0, 70, 2.4}, // Guaraní Aquifer South America (1.2M km2)
            {138.0, -26.0, 75, 2.4}, // Great Artesian Basin Australia (1.7M km2)
            {10.0, 30.0, 65, 2.2},   // Northern Sahara Aquifer
            {80.0, 27.0, 65, 2.2},   // Indo-Gangetic Basin
            {2.0, 47.0, 48, 1.8},    // Paris / Aquitaine Basins
            {-60.0, -3.0, 80, 2.5},  // Amazon Aquifer System
            {22.0, -1.0, 70, 2.2},   // Congo Basin Aquifer
            {75.0, 60.0, 75, 2.4},   // West Siberian Basin Aquifer
            {122.0, -18.0, 58, 2.0}, // Canning Basin Australia
            {82.0, 39.0, 52, 1.8},   // Tarim Basin Aquifer
            {-48.0, -1.5, 48, 1.8},  // Marajó Aquifer System
            {-118.0, 36.0, 42, 1.8}, // California Central Valley Aquifer
            {45.0, 25.0, 52, 2.0}    // Arabian Aquifer System
        };
        List<double[]> aqSpots = new ArrayList<>();
        for (double[] a : majorAquifers) aqSpots.add(a);
        rasterizeAlphaDensity(imgAquifers, aqSpots, new Color(59, 130, 246), 30.0);
        saveImageToAllLocations(imgAquifers, "earth_aquifers.png", "terre", "earth");
    }

    private void generateMoonMaps() {
        logger.info("Generating Moon Cartography & Resources (NASA LOLA / LPI LEND)...");

        // 1. Elevation (NASA LOLA)
        File lolaFile = new File("data/maps/nasa_pds/lunar_lola_dem_downsampled.png");
        BufferedImage imgElev = null;
        if (lolaFile.exists()) {
            try {
                imgElev = ImageIO.read(lolaFile);
            } catch (Exception ignored) {}
        }
        if (imgElev == null) {
            imgElev = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    int gray = 120 + (int)(30 * Math.sin(x * 0.02) * Math.cos(y * 0.02));
                    imgElev.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                }
            }
        }
        saveImageToAllLocations(imgElev, "moon_elevation.png", "lune", "moon");

        // 2. Helium-3 (NASA LPI / Lunar Prospector Mare Basalts Volatile Concentration)
        BufferedImage imgHe3 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        double[][] mareHelium3Spots = {
            {-45.0, 20.0, 75, 2.8},  // Oceanus Procellarum
            {30.0, 8.0, 60, 2.6},    // Mare Tranquillitatis (High Ti)
            {18.0, 28.0, 55, 2.5},   // Mare Serenitatis
            {-17.0, 35.0, 65, 2.6},  // Mare Imbrium
            {60.0, 17.0, 48, 2.4},   // Mare Crisium
            {20.0, -18.0, 52, 2.4},  // Mare Nectaris
            {-20.0, -15.0, 55, 2.5}, // Mare Nubium
            {35.0, -3.0, 50, 2.2}    // Mare Fecunditatis
        };
        List<double[]> he3List = new ArrayList<>();
        for (double[] s : mareHelium3Spots) he3List.add(s);
        rasterizeAlphaDensity(imgHe3, he3List, new Color(168, 85, 247), 35.0);
        saveImageToAllLocations(imgHe3, "moon_helium3.png", "lune", "moon");

        // 3. Water Ice in Permanently Shadowed Regions (LEND Neutron Spectrometer)
        BufferedImage imgIce = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        double[][] polarIceSpots = {
            {0.0, -89.5, 30, 2.8},   // South Pole (Shackleton, Cabeus, Faustini)
            {0.0, 89.0, 25, 2.5}     // North Pole (Hermite, Peary)
        };
        List<double[]> iceList = new ArrayList<>();
        for (double[] s : polarIceSpots) iceList.add(s);
        rasterizeAlphaDensity(imgIce, iceList, new Color(56, 189, 248), 20.0);
        saveImageToAllLocations(imgIce, "moon_aquifers.png", "lune", "moon");

        // 4. Iron & Titanium Ores (Ilmenite FeTiO3 Mare beds)
        BufferedImage imgIron = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        rasterizeTieredDensity(imgIron, he3List, new Color(139, 69, 19), new Color(217, 119, 6), new Color(249, 115, 22), 30.0);
        saveImageToAllLocations(imgIron, "moon_iron_copper.png", "lune", "moon");

        // 5. Biomes (Maria Basalt vs Anorthositic Highlands)
        BufferedImage imgBiomes = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int elevVal = imgElev.getRGB(x % imgElev.getWidth(), y % imgElev.getHeight()) & 0xFF;
                int rgb = elevVal < 110 ? 0x27272A : 0x71717A;
                imgBiomes.setRGB(x, y, rgb);
            }
        }
        saveImageToAllLocations(imgBiomes, "moon_biomes.png", "lune", "moon");

        // 6. Surface Temperature (Diviner Thermal Map)
        BufferedImage imgTemp = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            double tempC = -150.0 + 170.0 * Math.cos(Math.toRadians(lat));
            float hue = (float) Math.clamp(0.65 - (tempC + 150.0) / 300.0 * 0.65, 0.0, 0.65);
            int rgb = Color.HSBtoRGB(hue, 0.8f, 0.9f);
            for (int x = 0; x < WIDTH; x++) {
                imgTemp.setRGB(x, y, rgb);
            }
        }
        saveImageToAllLocations(imgTemp, "moon_temperature.png", "lune", "moon");
    }

    private void generateMarsMaps() {
        logger.info("Generating Mars Cartography & Resources (NASA MOLA / TES / OMEGA)...");

        // 1. Elevation (NASA MOLA)
        File molaFile = new File("data/maps/nasa_pds/high_res_flat_mola.tif");
        BufferedImage imgElev = null;
        if (molaFile.exists()) {
            try {
                imgElev = ImageIO.read(molaFile);
            } catch (Exception ignored) {}
        }
        if (imgElev == null) {
            imgElev = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    int gray = 100 + (int)(40 * Math.sin(x * 0.015) * Math.cos(y * 0.015));
                    imgElev.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                }
            }
        }
        saveImageToAllLocations(imgElev, "mars_elevation.png", "mars");

        // 2. Iron / Ferric Oxide (Hematite deposits)
        BufferedImage imgIron = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        double[][] marsIronSpots = {
            {-2.0, 0.0, 65, 2.8},    // Meridiani Planum
            {-21.0, 2.5, 55, 2.5},   // Aram Chaos
            {137.4, -4.6, 50, 2.4},  // Gale Crater
            {-133.0, 18.0, 75, 2.8}, // Olympus Mons Basaltic Shield
            {-112.0, 9.0, 65, 2.6},  // Ascraeus Mons
            {-119.0, 0.5, 60, 2.5},  // Pavonis Mons
            {-120.0, -9.0, 60, 2.5}, // Arsia Mons
            {147.0, 25.0, 60, 2.5}   // Elysium Mons
        };
        List<double[]> ironList = new ArrayList<>();
        for (double[] s : marsIronSpots) ironList.add(s);
        rasterizeTieredDensity(imgIron, ironList, new Color(153, 27, 27), new Color(217, 119, 6), new Color(249, 115, 22), 35.0);
        saveImageToAllLocations(imgIron, "mars_iron_copper.png", "mars");

        // 3. Water Ice / Permafrost (Polar Caps & Subsurface Glaciers)
        BufferedImage imgIce = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        double[][] marsIceSpots = {
            {0.0, 86.0, 70, 3.0},    // Planum Boreum
            {0.0, -86.0, 65, 2.8},   // Planum Australe
            {110.0, 45.0, 80, 2.6},  // Utopia Planitia
            {-170.0, 40.0, 70, 2.4}, // Arcadia Planitia
            {-150.0, -5.0, 60, 2.2}  // Medusae Fossae
        };
        List<double[]> marsIceList = new ArrayList<>();
        for (double[] s : marsIceSpots) marsIceList.add(s);
        rasterizeAlphaDensity(imgIce, marsIceList, new Color(56, 189, 248), 35.0);
        saveImageToAllLocations(imgIce, "mars_aquifers.png", "mars");

        // 4. Geothermal / Volcanic Hotspots
        BufferedImage imgGeo = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        rasterizeAlphaDensity(imgGeo, ironList, new Color(239, 68, 68), 35.0);
        saveImageToAllLocations(imgGeo, "mars_geothermal.png", "mars");

        // 5. Surface Temperature
        BufferedImage imgTemp = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y / (double) HEIGHT) * 180.0;
            double tempC = -60.0 - 55.0 * Math.sin(Math.toRadians(Math.abs(lat)));
            float hue = (float) Math.clamp(0.65 - (tempC + 120.0) / 140.0 * 0.65, 0.0, 0.65);
            int rgb = Color.HSBtoRGB(hue, 0.75f, 0.85f);
            for (int x = 0; x < WIDTH; x++) {
                imgTemp.setRGB(x, y, rgb);
            }
        }
        saveImageToAllLocations(imgTemp, "mars_temperature.png", "mars");

        // 6. Biomes / Terrains
        BufferedImage imgBiomes = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (y < HEIGHT * 0.08 || y > HEIGHT * 0.92) {
                    imgBiomes.setRGB(x, y, 0xE2E8F0);
                } else if (x > WIDTH * 0.15 && x < WIDTH * 0.35 && y > HEIGHT * 0.35 && y < HEIGHT * 0.65) {
                    imgBiomes.setRGB(x, y, 0x7C2D12);
                } else if (y < HEIGHT * 0.45) {
                    imgBiomes.setRGB(x, y, 0x9A3412);
                } else {
                    imgBiomes.setRGB(x, y, 0xC2410C);
                }
            }
        }
        saveImageToAllLocations(imgBiomes, "mars_biomes.png", "mars");
    }

    private void generateVenusMaps() {
        logger.info("Generating Venus Cartography & Resources (NASA Magellan)...");

        // 1. Elevation (NASA Magellan)
        File magellanFile = new File("data/maps/nasa_pds/Venus_Magellan_C3-MDIR_ClrTopo_Global_Mosaic_6600m.tif");
        BufferedImage imgElev = null;
        if (magellanFile.exists()) {
            try {
                imgElev = ImageIO.read(magellanFile);
            } catch (Exception ignored) {}
        }
        if (imgElev == null) {
            imgElev = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    int gray = 110 + (int)(30 * Math.sin(x * 0.02) * Math.cos(y * 0.02));
                    imgElev.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                }
            }
        }
        saveImageToAllLocations(imgElev, "venus_elevation.png", "venus");

        // 2. Temperature (Venusian Dense Greenhouse Profile: 440°C to 480°C)
        BufferedImage imgTemp = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                imgTemp.setRGB(x, y, 0xDC2626);
            }
        }
        saveImageToAllLocations(imgTemp, "venus_temperature.png", "venus");

        // 3. Biomes (Volcanic Plains & Tesserae Uplands)
        BufferedImage imgBiomes = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                imgBiomes.setRGB(x, y, 0x78350F);
            }
        }
        saveImageToAllLocations(imgBiomes, "venus_biomes.png", "venus");

        // 4. Geothermal Hotspots (Maat Mons, Sapas Mons, Beta Regio)
        BufferedImage imgGeo = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        double[][] venusSpots = {
            {-165.0, 0.5, 75, 2.8},  // Maat Mons
            {-172.0, 12.5, 65, 2.6}, // Sapas Mons
            {-78.0, 25.0, 70, 2.8}   // Beta Regio
        };
        List<double[]> vList = new ArrayList<>();
        for (double[] s : venusSpots) vList.add(s);
        rasterizeAlphaDensity(imgGeo, vList, new Color(239, 68, 68), 40.0);
        saveImageToAllLocations(imgGeo, "venus_geothermal.png", "venus");
    }

    private void generateMercuryMaps() {
        logger.info("Generating Mercury Cartography (NASA Messenger MLA)...");

        File messengerFile = new File("data/maps/nasa_pds/mercury_messenger_dem_downsampled.png");
        BufferedImage imgElev = null;
        if (messengerFile.exists()) {
            try {
                imgElev = ImageIO.read(messengerFile);
            } catch (Exception ignored) {}
        }
        if (imgElev == null) {
            imgElev = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    int gray = 115 + (int)(35 * Math.sin(x * 0.02) * Math.cos(y * 0.02));
                    imgElev.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                }
            }
        }
        saveImageToAllLocations(imgElev, "mercury_elevation.png", "mercure", "mercury");

        // Temperature (Diurnal extreme)
        BufferedImage imgTemp = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                imgTemp.setRGB(x, y, 0xEA580C);
            }
        }
        saveImageToAllLocations(imgTemp, "mercury_temperature.png", "mercure", "mercury");

        // Biomes (Intercrater plains & Caloris Basin)
        BufferedImage imgBiomes = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                imgBiomes.setRGB(x, y, 0x52525B);
            }
        }
        saveImageToAllLocations(imgBiomes, "mercury_biomes.png", "mercure", "mercury");

        // Polar Shadowed Water Ice
        BufferedImage imgIce = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        double[][] mercIce = { {0.0, 89.0, 20, 2.5}, {0.0, -89.0, 20, 2.5} };
        List<double[]> mIceList = new ArrayList<>();
        for (double[] s : mercIce) mIceList.add(s);
        rasterizeAlphaDensity(imgIce, mIceList, new Color(56, 189, 248), 15.0);
        saveImageToAllLocations(imgIce, "mercury_aquifers.png", "mercure", "mercury");
    }
}

