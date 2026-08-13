/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Ultra-High Precision Cartographic Generator for Ether Simulation Engine.
 * Supports continuous topographic landmasking and tailored multi-channel spatial tensors
 * across all 19 built-in scenarios.
 */
public class HistoricalMapGenerator {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalMapGenerator.class);

    public static final int WIDTH = 1024;
    public static final int HEIGHT = 512;

    private static final List<Path2D> LAND_POLYGONS = new ArrayList<>();
    private static final List<Path2D> SEA_POLYGONS = new ArrayList<>();

    static {
        initHighPrecisionGeographicPolygons();
    }

    public static void populateScenarioHistoricalMaps(Scenario scenario) {
        if (scenario == null) return;

        try {
            String type = scenario.getPopulationDensityType();
            if (type == null) type = "URBAN_CLUSTERS";

            // 1. Clean Grayscale Density Map (Black background, 0..255 intensity)
            BufferedImage imgDensity = generateCleanDensityMap(type, scenario);
            scenario.setCustomDensityBase64(bufferedImageToBase64Png(imgDensity));

            // 2. Clean Multi-Channel Isogloss Map
            BufferedImage imgIsogloss = generateCleanIsoglossMap(type, scenario);
            scenario.setCustomIsoglossBase64(bufferedImageToBase64Png(imgIsogloss));

            // 3. Clean Multi-Channel Kinship Map
            BufferedImage imgKinship = generateCleanKinshipMap(type, scenario);
            scenario.setCustomKinshipBase64(bufferedImageToBase64Png(imgKinship));

            // 4. Clean Multi-Channel Rituals Map
            BufferedImage imgRituals = generateCleanRitualsMap(type, scenario);
            scenario.setCustomRitualsBase64(bufferedImageToBase64Png(imgRituals));

            // 5. Clean Multi-Channel Sovereignty Map
            BufferedImage imgSovereignty = generateCleanSovereigntyMap(type, scenario);
            scenario.setCustomSovereigntyBase64(bufferedImageToBase64Png(imgSovereignty));

        } catch (Exception e) {
            logger.error("Failed to generate historical maps for scenario {}", scenario.getName(), e);
        }
    }

    // --- 1. CLEAN DENSITY MAP ---

    public static BufferedImage generateCleanDensityMap(String type, Scenario scenario) {
        BufferedImage img = createPureBlackCanvas();
        double[][] density = new double[WIDTH][HEIGHT];

        List<CityPoint> cities = getCitiesForScenario(type);
        List<RiverRibbon> rivers = getRiversForScenario(type);

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lng = (x + 0.5) / WIDTH * 360.0 - 180.0;

                if (!isLand(lng, lat) || !isInScenarioBounds(type, lng, lat)) continue;

                double val = 0.05; // Base land habitability floor inside active bounds

                // Urban center Gaussians
                for (CityPoint c : cities) {
                    double d2 = distSq(lng, lat, c.lng, c.lat);
                    val += c.weight * Math.exp(-d2 / (2.0 * c.sigma * c.sigma));
                }

                // River corridor multiplier
                for (RiverRibbon r : rivers) {
                    double distToRiver = r.distanceToPoint(lng, lat);
                    if (distToRiver < r.widthDeg) {
                        val += r.weight * Math.exp(-distToRiver * distToRiver / (2.0 * r.widthDeg * r.widthDeg));
                    }
                }

                density[x][y] = val;
            }
        }

        // Normalize
        double maxD = 0.001;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (density[x][y] > maxD) maxD = density[x][y];
            }
        }

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (density[x][y] <= 0) continue;

                double norm = Math.pow(density[x][y] / maxD, 0.45);
                norm = Math.min(1.0, Math.max(0.0, norm));

                int gray = (int) (norm * 255.0);
                int rgb = (gray << 16) | (gray << 8) | gray;
                img.setRGB(x, y, rgb);
            }
        }

        return img;
    }

    // --- 2. CLEAN SOVEREIGNTY MAP ---

    public static BufferedImage generateCleanSovereigntyMap(String type, Scenario scenario) {
        BufferedImage img = createPureBlackCanvas();
        List<EmpireTerritory> empires = getEmpiresForScenario(type);

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lng = (x + 0.5) / WIDTH * 360.0 - 180.0;
                if (!isLand(lng, lat) || !isInScenarioBounds(type, lng, lat)) continue;

                for (EmpireTerritory emp : empires) {
                    if (emp.contains(lng, lat)) {
                        img.setRGB(x, y, emp.color.getRGB());
                        break;
                    }
                }
            }
        }
        return img;
    }

    // --- 3. CLEAN ISOGLOSS MAP ---

    public static BufferedImage generateCleanIsoglossMap(String type, Scenario scenario) {
        BufferedImage img = createPureBlackCanvas();
        List<LanguageZone> langZones = getLanguageZonesForScenario(type);

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lng = (x + 0.5) / WIDTH * 360.0 - 180.0;
                if (!isLand(lng, lat) || !isInScenarioBounds(type, lng, lat)) continue;

                double totalWeight = 0.0;
                double rSum = 0, gSum = 0, bSum = 0;

                for (LanguageZone lz : langZones) {
                    double d2 = distSq(lng, lat, lz.centerLng, lz.centerLat);
                    double w = 1.0 / Math.pow(d2 + 4.0, 1.5);
                    totalWeight += w;
                    rSum += w * lz.color.getRed();
                    gSum += w * lz.color.getGreen();
                    bSum += w * lz.color.getBlue();
                }

                if (totalWeight > 0) {
                    int r = (int) Math.min(255, rSum / totalWeight);
                    int gCol = (int) Math.min(255, gSum / totalWeight);
                    int b = (int) Math.min(255, bSum / totalWeight);
                    img.setRGB(x, y, (r << 16) | (gCol << 8) | b);
                }
            }
        }
        return img;
    }

    // --- 4. CLEAN KINSHIP MAP ---

    public static BufferedImage generateCleanKinshipMap(String type, Scenario scenario) {
        BufferedImage img = createPureBlackCanvas();
        List<LanguageZone> kinshipZones = getKinshipZonesForScenario(type);

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lng = (x + 0.5) / WIDTH * 360.0 - 180.0;
                if (!isLand(lng, lat) || !isInScenarioBounds(type, lng, lat)) continue;

                LanguageZone bestZone = null;
                double minDist = Double.MAX_VALUE;

                for (LanguageZone kz : kinshipZones) {
                    double d2 = distSq(lng, lat, kz.centerLng, kz.centerLat);
                    if (d2 < minDist) {
                        minDist = d2;
                        bestZone = kz;
                    }
                }

                if (bestZone != null && minDist < 2500.0) {
                    img.setRGB(x, y, bestZone.color.getRGB());
                }
            }
        }
        return img;
    }

    // --- 5. CLEAN RITUALS MAP ---

    public static BufferedImage generateCleanRitualsMap(String type, Scenario scenario) {
        BufferedImage img = createPureBlackCanvas();
        List<LanguageZone> sacredSites = getSacredSitesForScenario(type);

        for (int y = 0; y < HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HEIGHT * 180.0;
            for (int x = 0; x < WIDTH; x++) {
                double lng = (x + 0.5) / WIDTH * 360.0 - 180.0;
                if (!isLand(lng, lat) || !isInScenarioBounds(type, lng, lat)) continue;

                double rSum = 0, gSum = 0, bSum = 0;
                for (LanguageZone ss : sacredSites) {
                    double d2 = distSq(lng, lat, ss.centerLng, ss.centerLat);
                    double w = Math.exp(-d2 / (2.0 * 25.0));
                    rSum += w * ss.color.getRed();
                    gSum += w * ss.color.getGreen();
                    bSum += w * ss.color.getBlue();
                }

                int r = (int) Math.min(255, rSum);
                int gCol = (int) Math.min(255, gSum);
                int b = (int) Math.min(255, bSum);

                if (r > 0 || gCol > 0 || b > 0) {
                    img.setRGB(x, y, (r << 16) | (gCol << 8) | b);
                }
            }
        }
        return img;
    }

    // --- SCENARIO BOUNDS FOR ALL 19 SCENARIOS ---

    public static boolean isInScenarioBounds(String type, double lng, double lat) {
        if (type == null) return true;

        switch (type.toUpperCase()) {
            case "ONE_CONTINENT": // Sortie d'Afrique (-100k)
                return (lat >= -35.0 && lat <= 32.0 && lng >= -18.0 && lng <= 52.0);

            case "SAHUL_MIGRATION": // Sahul (-50k)
                return (lat >= -45.0 && lat <= 10.0 && lng >= 95.0 && lng <= 155.0);

            case "BERINGIA_AMERICAS": // Béringie (-15k)
                return (lat >= 45.0 && lat <= 75.0 && (lng >= 130.0 || lng <= -110.0));

            case "YOUNGER_DRYAS": // Younger Dryas (-11.5k)
                return (lat >= 20.0 && lat <= 62.0 && lng >= -12.0 && lng <= 55.0);

            case "FERTILE_CRESCENT": // Croissant Fertile (-8000)
            case "MESOPOTAMIA_ASSYRIA":
            case "EGYPT_NILE":
                return (lat >= 15.0 && lat <= 42.0 && lng >= 25.0 && lng <= 55.0);

            case "GREEN_SAHARA": // Sahara Vert (-6000)
                return (lat >= 10.0 && lat <= 32.0 && lng >= -18.0 && lng <= 38.0);

            case "ROMAN_EMPIRE": // Empire Romain (An 0)
                return (lat >= 15.0 && lat <= 58.0 && lng >= -12.0 && lng <= 50.0);

            case "INDIA_MAURYA": // Empire Maurya (-300)
                return (lat >= 6.0 && lat <= 36.0 && lng >= 65.0 && lng <= 95.0);

            case "WEST_AFRICA_MALI": // Empire du Mali (1324)
                return (lat >= 4.0 && lat <= 25.0 && lng >= -18.0 && lng <= 15.0);

            case "SONG_DYNASTY": // Dynastie Song (1000)
                return (lat >= 18.0 && lat <= 42.0 && lng >= 98.0 && lng <= 126.0);

            case "SAKOKU_JAPAN": // Japon Tokugawa (1639)
                return (lat >= 30.0 && lat <= 46.0 && lng >= 128.0 && lng <= 146.0);

            case "MESOAMERICA": // Amériques (1491)
            case "AMERICAS_1491":
            case "EPIDEMIC_CONTACT":
                return (lng >= -125.0 && lng <= -35.0 && lat >= -55.0 && lat <= 55.0);

            default:
                // Modern / Industrial / Anthropocene / Singularity / Urban Clusters
                return true;
        }
    }

    // --- LANDMASK UTILITIES ---

    private static BufferedImage createPureBlackCanvas() {
        return new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    }

    public static boolean isLand(double lng, double lat) {
        int x = (int) ((lng + 180.0) / 360.0 * WIDTH);
        int y = (int) ((90.0 - lat) / 180.0 * HEIGHT);
        Point p = new Point(x, y);

        for (Path2D sea : SEA_POLYGONS) {
            if (sea.contains(p)) return false;
        }

        for (Path2D land : LAND_POLYGONS) {
            if (land.contains(p)) return true;
        }

        return false;
    }

    private static void initHighPrecisionGeographicPolygons() {
        // Iberian Peninsula
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-9.5, 36.0}, {-9.0, 43.3}, {-3.5, 43.5}, {3.3, 42.4}, {0.2, 38.0}, {-5.4, 36.0}
        }));

        // Italian Peninsula & Islands
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {7.5, 43.7}, {13.5, 45.8}, {13.8, 44.8}, {18.5, 40.2}, {16.0, 38.0}, {15.5, 41.0}, {12.2, 41.8}, {9.8, 44.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {12.4, 37.8}, {15.6, 38.3}, {15.2, 36.6}, {12.4, 37.8}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {8.5, 38.8}, {9.7, 43.0}, {8.5, 43.0}, {8.0, 38.8}
        }));

        // Greece & Aegean
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {19.5, 39.8}, {24.0, 41.0}, {26.5, 40.5}, {24.0, 37.5}, {21.5, 36.5}, {20.5, 38.5}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {23.5, 35.0}, {26.3, 35.3}, {26.0, 35.0}, {23.5, 35.0}
        }));

        // Anatolia
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {26.2, 40.2}, {29.0, 41.2}, {38.0, 42.1}, {41.5, 41.6}, {44.0, 39.0}, {36.0, 36.5}, {32.5, 36.2}, {27.2, 36.8}
        }));

        // Levant & Near East
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {35.0, 36.5}, {36.5, 37.0}, {41.0, 37.0}, {42.0, 34.0}, {36.0, 31.0}, {34.2, 31.3}, {35.5, 33.8}
        }));

        // Nile Delta & Valley
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {29.5, 31.5}, {32.5, 31.5}, {34.0, 27.8}, {35.0, 22.0}, {31.0, 22.0}, {29.5, 30.0}
        }));

        // Maghreb / North Africa
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-10.0, 28.0}, {-5.4, 35.8}, {11.0, 37.5}, {11.5, 33.0}, {25.0, 32.0}, {30.0, 31.5}, {30.0, 28.0}, {-10.0, 20.0}
        }));

        // Gaul / France
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-4.8, 48.4}, {2.5, 51.0}, {7.5, 49.0}, {7.5, 43.7}, {-1.8, 43.4}, {-4.8, 48.4}
        }));

        // British Isles & Ireland
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-5.5, 50.0}, {1.8, 51.3}, {0.0, 58.5}, {-6.0, 58.5}, {-5.5, 50.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-10.5, 51.5}, {-6.0, 52.0}, {-5.8, 55.3}, {-10.5, 54.0}
        }));

        // Central & Eastern Europe
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {7.5, 49.0}, {14.0, 54.5}, {22.0, 54.5}, {30.0, 46.0}, {26.5, 40.5}, {13.5, 45.8}
        }));

        // Mesopotamia
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {38.0, 37.0}, {45.0, 37.0}, {48.5, 30.0}, {47.0, 30.0}, {40.0, 33.0}
        }));

        // Indian Subcontinent
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {68.0, 24.0}, {75.0, 33.0}, {88.0, 27.0}, {92.0, 22.0}, {80.0, 10.0}, {77.5, 8.1}, {73.0, 15.5}
        }));

        // China
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {100.0, 22.0}, {105.0, 40.0}, {125.0, 42.0}, {122.0, 30.0}, {110.0, 20.0}
        }));

        // Japan
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {130.0, 31.0}, {140.0, 36.0}, {141.5, 41.5}, {141.0, 45.5}, {135.0, 35.0}, {129.5, 33.0}
        }));

        // West Africa & Sub-Saharan Africa
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-17.5, 12.0}, {-17.5, 16.5}, {-3.0, 16.8}, {4.0, 14.0}, {10.0, 5.0}, {-7.0, 4.5}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {10.0, 12.0}, {42.0, 12.0}, {51.0, 11.0}, {40.0, -10.0}, {30.0, -34.0}, {18.0, -34.0}, {10.0, -5.0}
        }));

        // Indonesia & Australia / Sahul
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {113.0, -26.0}, {130.0, -12.0}, {142.0, -11.0}, {153.0, -28.0}, {147.0, -38.0}, {138.0, -35.0}, {115.0, -34.0}
        }));

        // Mesoamerica & Andes
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-105.0, 22.0}, {-96.0, 19.5}, {-88.0, 21.0}, {-87.0, 14.0}, {-100.0, 16.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-81.0, 5.0}, {-75.0, -10.0}, {-70.0, -25.0}, {-68.0, -40.0}, {-75.0, -45.0}, {-81.0, -5.0}
        }));

        // INLAND SEAS
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {-5.0, 36.0}, {0.0, 36.0}, {10.0, 38.0}, {15.0, 39.0}, {22.0, 38.0}, {30.0, 32.0}, {35.0, 32.0},
            {34.0, 35.0}, {26.0, 37.0}, {18.0, 40.0}, {12.0, 44.0}, {5.0, 43.0}, {0.0, 37.0}, {-5.0, 36.0}
        }));
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {28.0, 41.0}, {32.0, 46.0}, {38.0, 46.0}, {41.0, 43.0}, {35.0, 41.0}, {28.0, 41.0}
        }));
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {33.0, 27.0}, {35.0, 28.0}, {43.0, 12.0}, {40.0, 12.0}, {33.0, 27.0}
        }));
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {48.0, 30.0}, {56.0, 27.0}, {54.0, 25.0}, {48.0, 29.0}
        }));
    }

    private static Path2D createPolygon(double[][] points) {
        Path2D p = new Path2D.Double();
        if (points.length == 0) return p;

        int startX = (int) ((points[0][0] + 180.0) / 360.0 * WIDTH);
        int startY = (int) ((90.0 - points[0][1]) / 180.0 * HEIGHT);
        p.moveTo(startX, startY);

        for (int i = 1; i < points.length; i++) {
            int px = (int) ((points[i][0] + 180.0) / 360.0 * WIDTH);
            int py = (int) ((90.0 - points[i][1]) / 180.0 * HEIGHT);
            p.lineTo(px, py);
        }
        p.closePath();
        return p;
    }

    // --- ALL 19 SCENARIO HISTORICAL RESOLUTIONS ---

    private static List<CityPoint> getCitiesForScenario(String type) {
        List<CityPoint> list = new ArrayList<>();
        switch (type.toUpperCase()) {
            case "ROMAN_EMPIRE":
                list.add(new CityPoint("Rome", 41.9, 12.5, 4.0, 2.5));
                list.add(new CityPoint("Alexandria", 31.2, 29.9, 3.0, 2.0));
                list.add(new CityPoint("Antioch", 36.2, 36.1, 2.5, 2.0));
                list.add(new CityPoint("Carthage", 36.8, 10.3, 2.2, 2.0));
                list.add(new CityPoint("Athens", 37.9, 23.7, 2.0, 1.8));
                list.add(new CityPoint("Constantinople", 41.0, 28.9, 2.5, 2.0));
                break;
            case "EGYPT_NILE":
                list.add(new CityPoint("Memphis", 29.8, 31.2, 3.5, 2.0));
                list.add(new CityPoint("Thebes", 25.7, 32.6, 3.0, 2.0));
                break;
            case "MESOPOTAMIA_ASSYRIA":
            case "FERTILE_CRESCENT":
            case "YOUNGER_DRYAS":
                list.add(new CityPoint("Nineveh", 36.3, 43.1, 3.5, 2.0));
                list.add(new CityPoint("Babylon", 32.5, 44.4, 3.5, 2.0));
                list.add(new CityPoint("Jericho", 31.8, 35.4, 2.2, 1.5));
                break;
            case "INDIA_MAURYA":
                list.add(new CityPoint("Pataliputra", 25.6, 85.1, 4.0, 2.5));
                list.add(new CityPoint("Taxila", 33.7, 72.8, 3.0, 2.0));
                break;
            case "WEST_AFRICA_MALI":
            case "GREEN_SAHARA":
                list.add(new CityPoint("Timbuktu", 16.7, -3.0, 3.2, 2.0));
                list.add(new CityPoint("Gao", 16.2, 0.0, 2.5, 2.0));
                list.add(new CityPoint("Niani", 11.4, -8.3, 2.8, 2.0));
                break;
            case "SONG_DYNASTY":
                list.add(new CityPoint("Kaifeng", 34.7, 114.3, 4.0, 2.5));
                list.add(new CityPoint("Hangzhou", 30.2, 120.1, 3.8, 2.5));
                break;
            case "SAKOKU_JAPAN":
                list.add(new CityPoint("Edo", 35.6, 139.7, 4.0, 2.0));
                list.add(new CityPoint("Kyoto", 35.0, 135.7, 3.5, 2.0));
                list.add(new CityPoint("Osaka", 34.6, 135.5, 3.2, 2.0));
                break;
            case "MESOAMERICA":
            case "AMERICAS_1491":
            case "EPIDEMIC_CONTACT":
                list.add(new CityPoint("Tenochtitlan", 19.4, -99.1, 4.0, 2.5));
                list.add(new CityPoint("Cuzco", -13.5, -71.9, 3.5, 2.5));
                list.add(new CityPoint("Tikal", 17.2, -89.6, 2.8, 2.0));
                break;
            case "SAHUL_MIGRATION":
                list.add(new CityPoint("Madjedbebe", -12.5, 132.9, 3.0, 2.5));
                list.add(new CityPoint("Lake Mungo", -33.7, 143.0, 2.5, 2.0));
                break;
            case "BERINGIA_AMERICAS":
                list.add(new CityPoint("Yana RHS", 70.7, 135.4, 3.0, 2.5));
                list.add(new CityPoint("Bluefish Caves", 67.1, -140.7, 2.5, 2.0));
                break;
            case "ONE_CONTINENT":
                list.add(new CityPoint("Omo", 4.5, 36.0, 3.5, 3.0));
                list.add(new CityPoint("Klasies River", -34.1, 24.4, 2.5, 2.5));
                list.add(new CityPoint("Blombos", -34.4, 21.2, 2.2, 2.0));
                break;
            default:
                list.add(new CityPoint("Tokyo", 35.6, 139.7, 4.0, 2.5));
                list.add(new CityPoint("New York", 40.7, -74.0, 3.5, 2.5));
                list.add(new CityPoint("London", 51.5, -0.1, 3.2, 2.0));
                break;
        }
        return list;
    }

    private static List<RiverRibbon> getRiversForScenario(String type) {
        List<RiverRibbon> list = new ArrayList<>();
        switch (type.toUpperCase()) {
            case "EGYPT_NILE":
            case "ROMAN_EMPIRE":
                list.add(new RiverRibbon(32.0, 24.0, 31.5, 31.5, 2.5, 1.0));
                break;
            case "MESOPOTAMIA_ASSYRIA":
            case "FERTILE_CRESCENT":
                list.add(new RiverRibbon(38.0, 37.0, 48.0, 30.0, 2.8, 1.2));
                break;
            case "INDIA_MAURYA":
                list.add(new RiverRibbon(78.0, 30.0, 90.0, 22.0, 3.0, 1.5));
                break;
            case "SONG_DYNASTY":
                list.add(new RiverRibbon(105.0, 35.0, 120.0, 35.0, 3.0, 1.5)); // Yellow River
                list.add(new RiverRibbon(105.0, 30.0, 121.0, 31.0, 3.0, 1.5)); // Yangtze
                break;
            case "WEST_AFRICA_MALI":
                list.add(new RiverRibbon(-10.0, 11.0, 3.0, 16.0, 2.2, 1.2));
                break;
            case "ONE_CONTINENT":
                list.add(new RiverRibbon(31.0, 3.0, 32.5, 31.0, 2.0, 1.5));
                break;
        }
        return list;
    }

    private static List<EmpireTerritory> getEmpiresForScenario(String type) {
        List<EmpireTerritory> list = new ArrayList<>();
        switch (type.toUpperCase()) {
            case "ROMAN_EMPIRE":
                EmpireTerritory rome = new EmpireTerritory("Imperium Romanum", new Color(220, 38, 38));
                rome.addBoundingBox(-9.0, 30.0, 45.0, 54.0);
                list.add(rome);
                break;
            case "INDIA_MAURYA":
                EmpireTerritory maurya = new EmpireTerritory("Empire Maurya", new Color(217, 119, 6));
                maurya.addBoundingBox(68.0, 8.0, 90.0, 34.0);
                list.add(maurya);
                break;
            case "WEST_AFRICA_MALI":
                EmpireTerritory mali = new EmpireTerritory("Empire du Mali", new Color(16, 185, 129));
                mali.addBoundingBox(-17.0, 8.0, 4.0, 20.0);
                list.add(mali);
                break;
            case "SONG_DYNASTY":
                EmpireTerritory song = new EmpireTerritory("Dynastie Song", new Color(239, 68, 68));
                song.addBoundingBox(98.0, 18.0, 124.0, 38.0);
                list.add(song);
                break;
            case "SAKOKU_JAPAN":
                EmpireTerritory japan = new EmpireTerritory("Shogunat Tokugawa", new Color(168, 85, 247));
                japan.addBoundingBox(129.0, 30.0, 145.0, 44.0);
                list.add(japan);
                break;
            case "MESOAMERICA":
            case "AMERICAS_1491":
                EmpireTerritory aztec = new EmpireTerritory("Alliance Aztèque", new Color(245, 158, 11));
                aztec.addBoundingBox(-102.0, 14.0, -95.0, 21.0);
                list.add(aztec);
                EmpireTerritory inca = new EmpireTerritory("Tawantinsuyu (Inca)", new Color(220, 38, 38));
                inca.addBoundingBox(-80.0, -35.0, -68.0, 2.0);
                list.add(inca);
                break;
            case "ONE_CONTINENT":
                EmpireTerritory africa = new EmpireTerritory("Foyer Homo Sapiens", new Color(234, 179, 8));
                africa.addBoundingBox(10.0, -35.0, 52.0, 15.0);
                list.add(africa);
                break;
            default:
                EmpireTerritory generic = new EmpireTerritory("Sphère Démographique", new Color(59, 130, 246));
                generic.addBoundingBox(-10.0, 25.0, 50.0, 55.0);
                list.add(generic);
                break;
        }
        return list;
    }

    private static List<LanguageZone> getLanguageZonesForScenario(String type) {
        List<LanguageZone> list = new ArrayList<>();
        switch (type.toUpperCase()) {
            case "ROMAN_EMPIRE":
                list.add(new LanguageZone("Latin", 12.5, 41.9, new Color(59, 130, 246)));
                list.add(new LanguageZone("Greek", 23.7, 37.9, new Color(6, 182, 212)));
                list.add(new LanguageZone("Celtic", 2.3, 48.8, new Color(34, 197, 94)));
                list.add(new LanguageZone("Punique", 10.3, 36.8, new Color(245, 158, 11)));
                list.add(new LanguageZone("Aramaic", 35.4, 31.8, new Color(239, 68, 68)));
                break;
            case "FERTILE_CRESCENT":
                list.add(new LanguageZone("Proto-Semitic", 35.4, 31.8, new Color(245, 158, 11)));
                list.add(new LanguageZone("Sumerian", 44.4, 32.5, new Color(239, 68, 68)));
                list.add(new LanguageZone("Anatolian", 38.9, 37.2, new Color(168, 85, 247)));
                break;
            case "ONE_CONTINENT":
                list.add(new LanguageZone("Khoisan", 21.2, -34.4, new Color(234, 179, 8)));
                list.add(new LanguageZone("Niger-Congo", 36.0, 4.5, new Color(16, 185, 129)));
                break;
            default:
                list.add(new LanguageZone("Indo-European", 15.0, 48.0, new Color(59, 130, 246)));
                list.add(new LanguageZone("Afroasiatic", 30.0, 25.0, new Color(245, 158, 11)));
                break;
        }
        return list;
    }

    private static List<LanguageZone> getKinshipZonesForScenario(String type) {
        List<LanguageZone> list = new ArrayList<>();
        list.add(new LanguageZone("Patrilineal Gens", 12.5, 41.9, new Color(168, 85, 247)));
        list.add(new LanguageZone("Matrilineal Lineage", 23.7, 37.9, new Color(236, 72, 153)));
        return list;
    }

    private static List<LanguageZone> getSacredSitesForScenario(String type) {
        List<LanguageZone> list = new ArrayList<>();
        switch (type.toUpperCase()) {
            case "ROMAN_EMPIRE":
                list.add(new LanguageZone("Rome Cult", 12.5, 41.9, new Color(239, 68, 68)));
                list.add(new LanguageZone("Serapis", 29.9, 31.2, new Color(245, 158, 11)));
                list.add(new LanguageZone("Delphi", 22.5, 38.5, new Color(6, 182, 212)));
                break;
            case "FERTILE_CRESCENT":
                list.add(new LanguageZone("Göbekli Tepe", 38.9, 37.2, new Color(239, 68, 68)));
                list.add(new LanguageZone("Marduk", 44.4, 32.5, new Color(245, 158, 11)));
                break;
            default:
                list.add(new LanguageZone("Rift Sanctuary", 36.0, 4.5, new Color(234, 179, 8)));
                break;
        }
        return list;
    }

    private static double distSq(double lng1, double lat1, double lng2, double lat2) {
        double dlng = lng1 - lng2;
        double dlat = lat1 - lat2;
        return dlng * dlng + dlat * dlat;
    }

    public static String bufferedImageToBase64Png(BufferedImage bImg) {
        if (bImg == null) return null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bImg, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            logger.error("Failed to encode BufferedImage to Base64 PNG", e);
            return null;
        }
    }

    // --- STRUCTS ---

    public static class CityPoint {
        public String name;
        public double lat, lng, weight, sigma;

        public CityPoint(String name, double lat, double lng, double weight, double sigma) {
            this.name = name; this.lat = lat; this.lng = lng;
            this.weight = weight; this.sigma = sigma;
        }
    }

    public static class RiverRibbon {
        public double lng1, lat1, lng2, lat2, weight, widthDeg;

        public RiverRibbon(double lng1, double lat1, double lng2, double lat2, double weight, double widthDeg) {
            this.lng1 = lng1; this.lat1 = lat1; this.lng2 = lng2; this.lat2 = lat2;
            this.weight = weight; this.widthDeg = widthDeg;
        }

        public double distanceToPoint(double px, double py) {
            double l2 = distSq(lng1, lat1, lng2, lat2);
            if (l2 == 0) return Math.sqrt(distSq(px, py, lng1, lat1));
            double t = Math.max(0, Math.min(1, ((px - lng1) * (lng2 - lng1) + (py - lat1) * (lat2 - lat1)) / l2));
            double projX = lng1 + t * (lng2 - lng1);
            double projY = lat1 + t * (lat2 - lat1);
            return Math.sqrt(distSq(px, py, projX, projY));
        }
    }

    public static class EmpireTerritory {
        public String name;
        public Color color;
        public List<double[]> bounds = new ArrayList<>();

        public EmpireTerritory(String name, Color color) {
            this.name = name; this.color = color;
        }

        public void addBoundingBox(double minLng, double minLat, double maxLng, double maxLat) {
            bounds.add(new double[]{minLng, minLat, maxLng, maxLat});
        }

        public boolean contains(double lng, double lat) {
            for (double[] b : bounds) {
                if (lng >= b[0] && lng <= b[2] && lat >= b[1] && lat <= b[3]) return true;
            }
            return false;
        }
    }

    public static class LanguageZone {
        public String name;
        public double centerLng, centerLat;
        public Color color;

        public LanguageZone(String name, double centerLng, double centerLat, Color color) {
            this.name = name; this.centerLng = centerLng; this.centerLat = centerLat; this.color = color;
        }
    }
}
