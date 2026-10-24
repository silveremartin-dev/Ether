/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import java.awt.Point;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Geometric Vector Coastlines and Global Land/Ocean Spatial Index.
 * 
 * <p>Provides high-precision vector polygon definitions for major continental landmasses,
 * archipelagos, inland seas, and a precomputed $720 \times 360$ spatial lookup grid for
 * instantaneous $O(1)$ land/ocean classification.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class HistoricalLandGeometryCatalog {

    private static final List<Path2D> LAND_POLYGONS = new ArrayList<>();
    private static final List<Path2D> SEA_POLYGONS = new ArrayList<>();
    private static final boolean[][] FAST_LAND_GRID = new boolean[720][360];

    static {
        initHighPrecisionGeographicPolygons();
    }

    private HistoricalLandGeometryCatalog() {
        // Static utility class
    }

    /**
     * Determines whether a given geographic coordinate is situated on land.
     *
     * @param lng longitude coordinate in degrees [-180, 180]
     * @param lat latitude coordinate in degrees [-90, 90]
     * @return {@code true} if coordinate is on land, {@code false} if ocean
     */
    public static boolean isLand(double lng, double lat) {
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();
        if (mask != null) {
            int mx = Math.clamp((int) ((lng + 180.0) / 360.0 * mask.getWidth()), 0, mask.getWidth() - 1);
            int my = Math.clamp((int) ((90.0 - lat) / 180.0 * mask.getHeight()), 0, mask.getHeight() - 1);
            return mask.getRaster().getSample(mx, my, 0) > 0;
        }
        int gx = Math.clamp((int) ((lng + 180.0) / 360.0 * 720), 0, 719);
        int gy = Math.clamp((int) ((90.0 - lat) / 180.0 * 360), 0, 359);
        return FAST_LAND_GRID[gx][gy];
    }

    private static void initHighPrecisionGeographicPolygons() {
        // Iberian Peninsula
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-9.5, 36.0}, {-9.0, 37.0}, {-9.5, 38.8}, {-9.4, 41.8}, {-8.9, 43.3}, {-3.5, 43.5}, {1.7, 42.5}, {3.3, 42.4}, {0.2, 38.0}, {-5.4, 36.0}, {-7.5, 37.0}
        }));

        // Italian Peninsula & Sicily & Sardinia/Corsica
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {7.5, 43.7}, {9.8, 44.4}, {12.2, 45.8}, {13.5, 45.8}, {13.8, 44.8}, {15.0, 43.5}, {18.5, 40.2}, {17.5, 39.8}, {16.0, 38.0}, {15.5, 41.0}, {12.2, 41.8}, {9.8, 44.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {12.4, 37.8}, {15.6, 38.3}, {15.2, 36.6}, {12.4, 37.8}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {8.5, 38.8}, {9.7, 43.0}, {8.5, 43.0}, {8.0, 38.8}
        }));

        // Greece & Aegean & Peloponnese & Crete
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {19.5, 39.8}, {20.5, 39.5}, {22.5, 40.5}, {24.0, 41.0}, {26.5, 40.5}, {24.0, 37.5}, {22.5, 36.4}, {21.5, 36.5}, {20.5, 38.5}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {23.5, 35.0}, {26.3, 35.3}, {26.0, 35.0}, {23.5, 35.0}
        }));

        // Anatolia & Caucasus
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {26.2, 40.2}, {29.0, 41.2}, {35.0, 42.0}, {38.0, 42.1}, {41.5, 41.6}, {44.0, 39.0}, {36.0, 36.5}, {32.5, 36.2}, {27.2, 36.8}
        }));

        // Levant & Near East & Arabian Peninsula
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {35.0, 36.5}, {36.5, 37.0}, {41.0, 37.0}, {42.0, 34.0}, {36.0, 31.0}, {34.2, 31.3}, {35.5, 33.8}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {35.0, 28.0}, {43.0, 12.5}, {54.0, 16.0}, {59.0, 22.5}, {56.0, 26.0}, {48.0, 30.0}, {35.0, 30.0}
        }));

        // Nile Delta & Nile Valley
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {29.5, 31.5}, {32.5, 31.5}, {34.0, 27.8}, {35.0, 22.0}, {31.0, 22.0}, {29.5, 30.0}
        }));

        // Maghreb & North Africa Coast
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-10.0, 28.0}, {-5.4, 35.8}, {3.0, 36.8}, {10.0, 37.5}, {11.5, 33.0}, {15.0, 32.5}, {25.0, 32.0}, {30.0, 31.5}, {30.0, 28.0}, {10.0, 22.0}, {-10.0, 20.0}
        }));

        // Sub-Saharan Africa
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-17.5, 14.8}, {-17.0, 21.0}, {-16.0, 16.5}, {-14.0, 12.0}, {-7.5, 4.4}, {2.0, 6.2}, {9.5, 4.5}, {9.0, 2.0}, {9.5, -1.0}, {13.0, -12.0}, {12.0, -17.0},
            {15.0, -23.0}, {18.0, -34.5}, {26.0, -33.0}, {33.0, -27.0}, {40.0, -15.0}, {41.0, -4.0}, {51.2, 11.8}, {43.0, 12.5}, {37.0, 19.5}, {33.0, 27.0},
            {25.0, 12.0}, {10.0, 12.0}, {-3.0, 16.8}, {-17.5, 14.8}
        }));

        // Madagascar
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {43.5, -12.0}, {50.0, -12.5}, {50.5, -16.0}, {47.0, -25.5}, {43.0, -25.0}, {43.5, -12.0}
        }));

        // Western & Central & Eastern Europe
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-4.8, 48.4}, {2.5, 51.0}, {7.5, 53.5}, {10.0, 54.5}, {14.0, 54.5}, {22.0, 54.5}, {30.0, 60.0}, {30.0, 46.0}, {26.5, 40.5}, {13.5, 45.8}, {7.5, 43.7}, {-1.8, 43.4}, {-4.8, 48.4}
        }));

        // British Isles & Ireland
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-5.5, 50.0}, {1.8, 51.3}, {1.5, 53.0}, {-0.5, 54.5}, {-2.0, 57.5}, {-4.5, 58.5}, {-6.5, 56.5}, {-4.5, 52.0}, {-5.5, 50.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-10.5, 51.5}, {-6.0, 52.0}, {-5.8, 55.3}, {-10.5, 54.0}
        }));

        // Indian Subcontinent
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {61.5, 25.0}, {68.0, 24.0}, {72.8, 21.0}, {73.0, 15.5}, {75.0, 11.0}, {77.5, 8.1}, {79.8, 10.0}, {80.2, 13.0}, {85.0, 19.5}, {88.5, 21.5}, {92.0, 22.0},
            {94.0, 28.0}, {88.0, 27.0}, {80.0, 30.0}, {74.0, 35.0}, {68.0, 30.0}, {61.5, 25.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {79.5, 9.8}, {81.8, 9.8}, {81.8, 6.0}, {79.5, 6.0}
        }));

        // China & East Asia
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {98.0, 21.0}, {108.0, 20.0}, {113.0, 22.5}, {118.0, 24.5}, {121.5, 28.5}, {122.0, 31.5}, {119.5, 35.0}, {122.5, 37.5}, {124.0, 40.0},
            {130.0, 42.5}, {135.0, 48.0}, {140.0, 55.0}, {120.0, 53.0}, {100.0, 50.0}, {90.0, 45.0}, {80.0, 40.0}, {98.0, 21.0}
        }));

        // Southeast Asia & Indochina
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {92.5, 20.0}, {98.5, 10.0}, {100.5, 6.0}, {104.0, 1.3}, {104.0, 10.0}, {109.0, 12.0}, {108.0, 16.0}, {106.0, 21.0}, {98.0, 21.0}
        }));

        // Sahul / Australia
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {113.5, -26.0}, {114.0, -22.0}, {122.0, -18.0}, {130.0, -14.0}, {136.0, -12.0}, {142.0, -10.5}, {150.0, -23.0}, {153.5, -28.0}, {150.0, -37.5}, {140.0, -38.5}, {138.0, -35.0}, {129.0, -32.0}, {115.0, -34.5}, {113.5, -26.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {95.0, 5.5}, {106.0, -6.0}, {105.0, -2.0}, {95.0, 5.5}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {108.5, 7.0}, {119.0, 4.0}, {115.0, -4.0}, {109.0, -3.0}
        }));

        // Japan
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {130.0, 31.0}, {132.0, 33.5}, {135.0, 34.5}, {139.0, 35.5}, {141.0, 38.0}, {141.5, 41.5}, {140.0, 40.5}, {136.0, 36.0}, {130.5, 33.0}
        }));
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {140.0, 41.5}, {145.5, 44.0}, {145.0, 45.5}, {141.0, 45.5}, {140.0, 41.5}
        }));

        // South America
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-77.0, 8.5}, {-73.0, 11.5}, {-62.0, 10.5}, {-50.0, 1.5}, {-35.0, -5.0}, {-35.0, -8.0}, {-38.0, -13.0}, {-41.0, -21.0},
            {-48.0, -28.0}, {-53.0, -33.0}, {-57.0, -38.0}, {-65.0, -45.0}, {-68.0, -54.0}, {-75.0, -52.0}, {-74.0, -42.0}, {-72.0, -30.0},
            {-76.0, -14.0}, {-81.0, -5.0}, {-79.0, 2.0}, {-77.0, 8.5}
        }));

        // Central America & Caribbean
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-92.0, 16.0}, {-88.0, 15.0}, {-83.0, 8.5}, {-77.0, 8.5}, {-83.0, 10.0}, {-90.0, 14.0}
        }));

        // North America
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-168.0, 65.0}, {-150.0, 60.0}, {-135.0, 55.0}, {-124.0, 48.0}, {-123.0, 38.0}, {-117.0, 32.0}, {-105.0, 20.0}, {-97.0, 26.0},
            {-90.0, 29.0}, {-81.0, 25.0}, {-80.0, 30.0}, {-75.0, 35.0}, {-70.0, 42.0}, {-64.0, 46.0}, {-55.0, 52.0}, {-65.0, 60.0},
            {-80.0, 65.0}, {-100.0, 68.0}, {-120.0, 70.0}, {-140.0, 70.0}, {-168.0, 65.0}
        }));

        // Iranian Plateau & Central Asia
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {44.0, 38.0}, {46.0, 47.0}, {50.0, 55.0}, {85.0, 55.0}, {87.0, 48.0}, {80.0, 35.0}, {74.0, 35.0}, {68.0, 24.0}, {62.0, 25.0}, {51.0, 36.0}, {44.0, 38.0}
        }));

        // Sahel & Central African Hinterland
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-17.0, 21.0}, {10.0, 22.0}, {30.0, 28.0}, {33.0, 27.0}, {37.0, 19.5}, {25.0, 12.0}, {10.0, 12.0}, {-3.0, 16.8}, {-17.5, 14.8}
        }));

        // Greenland
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-73.0, 78.0}, {-60.0, 83.0}, {-18.0, 82.0}, {-20.0, 70.0}, {-40.0, 60.0}, {-55.0, 60.0}, {-68.0, 75.0}
        }));

        // Northern Europe & Scandinavia
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {5.0, 58.0}, {10.0, 62.0}, {14.0, 68.0}, {18.0, 70.0}, {30.0, 70.0}, {30.0, 55.0}, {10.0, 54.0}
        }));

        // Siberia & Northern Eurasia
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {30.0, 55.0}, {30.0, 75.0}, {80.0, 74.0}, {120.0, 74.0}, {175.0, 68.0}, {170.0, 60.0}, {140.0, 50.0}, {80.0, 50.0}, {50.0, 50.0}
        }));

        // Antarctica
        LAND_POLYGONS.add(createPolygon(new double[][]{
            {-180.0, -65.0}, {180.0, -65.0}, {180.0, -90.0}, {-180.0, -90.0}
        }));

        // INLAND SEAS & MAJOR GULFS
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {-5.4, 35.9}, {-1.8, 35.7}, {0.5, 37.5}, {3.2, 37.1}, {8.0, 37.5},
            {10.5, 38.2}, {12.5, 38.1}, {15.5, 38.1}, {17.0, 39.4}, {19.5, 39.0},
            {20.9, 37.0}, {23.5, 37.5}, {26.0, 36.2}, {28.2, 36.5}, {29.5, 36.2},
            {30.5, 32.2}, {32.5, 32.1}, {34.8, 32.0}, {36.0, 33.5}, {36.3, 35.1},
            {35.5, 36.5}, {36.5, 37.0}, {35.8, 37.8}, {28.0, 38.5}, {26.0, 40.5},
            {23.5, 41.0}, {20.0, 40.5}, {16.5, 41.0}, {13.5, 44.0}, {12.0, 44.2},
            {8.5, 44.3}, {3.5, 43.3}, {2.0, 42.5}, {-0.5, 40.5}, {-1.5, 38.5},
            {-3.8, 36.8}, {-5.4, 35.9}
        }));
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {22.5, 40.5}, {24.0, 41.5}, {26.5, 40.9}, {27.5, 39.0}, {26.5, 37.5},
            {24.5, 37.0}, {22.5, 37.5}, {22.5, 38.5}, {23.5, 39.5}, {22.5, 40.5}
        }));
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {28.0, 41.0}, {29.0, 41.2}, {30.0, 42.5}, {33.0, 44.0}, {36.0, 45.0},
            {38.0, 46.0}, {41.0, 43.5}, {41.5, 41.5}, {38.0, 41.0}, {34.5, 41.5},
            {31.0, 41.8}, {28.5, 41.3}, {28.0, 41.0}
        }));
        SEA_POLYGONS.add(createPolygon(new double[][]{
            {32.5, 29.5}, {34.0, 28.0}, {36.5, 25.0}, {38.0, 22.0}, {40.0, 18.0},
            {41.5, 14.0}, {43.5, 12.5}, {43.0, 12.0}, {40.0, 12.5}, {36.5, 14.0},
            {33.5, 17.0}, {32.0, 20.0}, {32.0, 24.0}, {32.5, 27.0}, {32.5, 29.5}
        }));

        int w = HistoricalMapGenerator.WIDTH, h = HistoricalMapGenerator.HEIGHT;
        for (int gy = 0; gy < 360; gy++) {
            double lat = 90.0 - (gy + 0.5) / 360.0 * 180.0;
            for (int gx = 0; gx < 720; gx++) {
                double lng = -180.0 + (gx + 0.5) / 720.0 * 360.0;
                int x = (int) ((lng + 180.0) / 360.0 * w);
                int y = (int) ((90.0 - lat) / 180.0 * h);
                Point p = new Point(x, y);
                boolean isSea = false;
                for (Path2D sea : SEA_POLYGONS) {
                    if (sea.contains(p)) { isSea = true; break; }
                }
                if (!isSea) {
                    for (Path2D land : LAND_POLYGONS) {
                        if (land.contains(p)) { FAST_LAND_GRID[gx][gy] = true; break; }
                    }
                }
            }
        }
    }

    private static Path2D createPolygon(double[][] points) {
        Path2D p = new Path2D.Double();
        if (points.length == 0) return p;

        List<double[]> densified = new ArrayList<>();
        int n = points.length;
        for (int i = 0; i < n; i++) {
            double[] p1 = points[i];
            double[] p2 = points[(i + 1) % n];
            densified.add(p1);
            int subSteps = 10;
            for (int s = 1; s < subSteps; s++) {
                double t = s / (double) subSteps;
                double lng = p1[0] + t * (p2[0] - p1[0]);
                double lat = p1[1] + t * (p2[1] - p1[1]);
                densified.add(new double[]{lng, lat});
            }
        }

        int w = HistoricalMapGenerator.WIDTH, h = HistoricalMapGenerator.HEIGHT;
        int startX = (int) ((densified.get(0)[0] + 180.0) / 360.0 * w);
        int startY = (int) ((90.0 - densified.get(0)[1]) / 180.0 * h);
        p.moveTo(startX, startY);

        for (int i = 1; i < densified.size(); i++) {
            int px = (int) ((densified.get(i)[0] + 180.0) / 360.0 * w);
            int py = (int) ((90.0 - densified.get(i)[1]) / 180.0 * h);
            p.lineTo(px, py);
        }
        p.closePath();
        return p;
    }
}
