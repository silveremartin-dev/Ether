/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.model.Scenario;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * <h1>HistoricalTradeNetworkRasterizer</h1>
 * <p>
 * Cartographic rasterizer for historical overland, maritime, and riverine trade corridors.
 * Translates archaeological exchange networks and modern supply chains onto 2D discrete global grids.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class HistoricalTradeNetworkRasterizer {

    private HistoricalTradeNetworkRasterizer() {
        // Utility class
    }

    /**
     * Rasterizes the global trade network map for a specific historical epoch.
     *
     * @param type     layer type descriptor
     * @param scenario scenario specifying epoch and parameters
     * @return 2D rasterized trade network image
     */
    public static BufferedImage rasterizeTradeNetworkMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 1000L;
        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();

        if (year >= 1900L) {
            BufferedImage modernImg = NaturalEarthVectorIngestor.rasterizeModernTradeNetworkMap(
                    year, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, mask);
            if (modernImg != null) {
                return modernImg;
            }
        }

        BufferedImage img = new BufferedImage(HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (year <= -85000L) {
            drawTradeRoute(g, new double[][]{{36.1, 2.5}, {36.5, 0.5}, {36.4, -1.5}, {38.5, 8.5}}, new Color(180, 120, 40), 2.0);
            drawTradeRoute(g, new double[][]{{20.5, -34.5}, {22.1, -34.2}, {24.4, -34.1}}, new Color(180, 120, 40), 2.0);
            drawTradeRoute(g, new double[][]{{35.0, 32.7}, {35.3, 32.7}, {35.6, 33.0}}, new Color(160, 70, 50), 2.0);
            drawTradeRoute(g, new double[][]{{31.5, 26.0}, {32.6, 25.7}, {32.9, 24.1}}, new Color(160, 70, 50), 2.0);
            drawTradeRoute(g, new double[][]{{-4.0, 43.4}, {-1.5, 43.5}, {1.0, 45.0}, {0.3, 45.2}}, new Color(200, 140, 30), 2.0);
            drawTradeRoute(g, new double[][]{{9.5, 48.5}, {13.0, 47.8}, {15.8, 46.1}, {17.1, 49.2}}, new Color(200, 140, 30), 2.0);
            drawTradeRoute(g, new double[][]{{34.2, 44.9}, {38.5, 44.5}, {40.2, 44.2}}, new Color(200, 140, 30), 2.0);
            drawTradeRoute(g, new double[][]{{44.2, 36.8}, {47.1, 34.4}}, new Color(200, 140, 30), 2.0);
            drawTradeRoute(g, new double[][]{{83.9, 51.4}, {85.5, 51.7}}, new Color(200, 140, 30), 2.0);
            drawTradeRoute(g, new double[][]{{114.5, 40.2}, {115.9, 39.7}}, new Color(200, 140, 30), 2.0);
        } else if (year <= -65000L) {
            drawTradeRoute(g, new double[][]{{20.5, -34.5}, {22.1, -34.2}, {24.4, -34.1}}, new Color(180, 100, 40), 2.0);
            drawTradeRoute(g, new double[][]{{36.1, 2.5}, {36.5, 0.5}, {38.5, 8.5}}, new Color(180, 100, 40), 2.0);
            drawTradeRoute(g, new double[][]{{-2.4, 34.8}, {-4.0, 35.5}}, new Color(160, 70, 50), 2.0);
            drawTradeRoute(g, new double[][]{{-4.0, 43.4}, {-1.5, 43.5}, {1.0, 44.9}}, new Color(190, 130, 30), 2.0);
            drawTradeRoute(g, new double[][]{{44.2, 36.8}, {46.0, 35.5}}, new Color(190, 130, 30), 2.0);
        } else if (year <= -40000L) {
            drawTradeRoute(g, new double[][]{{20.5, -34.5}, {22.1, -34.2}, {24.4, -34.1}, {31.9, -27.0}}, new Color(204, 120, 40), 2.5);
            drawTradeRoute(g, new double[][]{{36.1, 2.5}, {36.5, 0.5}, {36.4, -1.5}, {38.5, 8.5}}, new Color(204, 120, 40), 2.5);
            drawTradeRoute(g, new double[][]{{35.0, 32.7}, {35.3, 32.7}, {35.6, 33.0}, {36.2, 34.0}}, new Color(200, 100, 80), 2.5);
            drawTradeRoute(g, new double[][]{{31.5, 26.0}, {32.6, 25.7}, {32.9, 24.1}}, new Color(204, 120, 40), 2.0);
            drawTradeRoute(g, new double[][]{{-4.0, 43.4}, {-1.5, 43.5}, {1.0, 45.0}, {3.5, 47.5}}, new Color(200, 100, 80), 2.5);
            drawTradeRoute(g, new double[][]{{9.5, 48.5}, {13.0, 47.8}, {15.8, 46.1}, {17.1, 49.2}, {25.0, 43.0}}, new Color(200, 100, 80), 2.5);
            drawTradeRoute(g, new double[][]{{44.2, 36.8}, {47.1, 34.4}}, new Color(200, 100, 80), 2.0);
            drawTradeRoute(g, new double[][]{{83.9, 51.4}, {85.5, 51.7}}, new Color(200, 100, 80), 2.0);
            drawTradeRoute(g, new double[][]{{132.9, -12.5}, {130.0, -14.0}, {125.0, -16.0}}, new Color(30, 150, 140), 2.5);
            drawTradeRoute(g, new double[][]{{143.0, -33.7}, {138.5, -34.5}, {134.0, -24.0}}, new Color(30, 150, 140), 2.5);
            drawTradeRoute(g, new double[][]{{111.5, 25.5}, {108.0, 23.0}, {102.0, 20.0}}, new Color(200, 100, 80), 2.0);
        } else if (year <= -22000L) {
            drawTradeRoute(g, new double[][]{{-0.5, 44.5}, {2.0, 46.5}, {5.0, 47.0}, {14.0, 50.0}, {20.0, 50.0}}, new Color(220, 130, 90), 2.8);
            drawTradeRoute(g, new double[][]{{30.5, 50.5}, {35.0, 52.0}, {38.0, 53.0}}, new Color(220, 130, 90), 2.8);
            drawTradeRoute(g, new double[][]{{90.0, 56.0}, {95.0, 56.0}, {103.0, 53.0}}, new Color(220, 130, 90), 2.8);
            drawTradeRoute(g, new double[][]{{35.0, 32.7}, {36.0, 34.0}, {37.0, 36.0}}, new Color(210, 110, 70), 2.5);
            drawTradeRoute(g, new double[][]{{31.5, 26.0}, {32.6, 25.7}, {32.9, 24.1}}, new Color(200, 120, 40), 2.0);
            drawTradeRoute(g, new double[][]{{20.5, -34.5}, {22.1, -34.2}, {28.0, -29.0}}, new Color(200, 120, 40), 2.5);
            drawTradeRoute(g, new double[][]{{132.9, -12.5}, {136.0, -18.0}, {140.0, -25.0}}, new Color(40, 160, 150), 2.5);
            drawTradeRoute(g, new double[][]{{116.0, 40.0}, {118.0, 39.0}}, new Color(200, 100, 80), 2.0);
        } else if (year <= -10000L) {
            drawTradeRoute(g, new double[][]{{35.5, 31.5}, {35.8, 32.8}, {36.5, 34.5}, {38.0, 37.0}}, new Color(230, 140, 50), 3.0);
            drawTradeRoute(g, new double[][]{{42.0, 37.5}, {44.0, 37.0}, {46.5, 36.0}}, new Color(230, 140, 50), 2.5);
            drawTradeRoute(g, new double[][]{{31.2, 29.5}, {32.5, 26.0}, {34.0, 27.5}}, new Color(220, 120, 60), 2.5);
            drawTradeRoute(g, new double[][]{{-2.0, 43.0}, {2.5, 45.0}, {7.0, 46.5}, {12.0, 48.0}}, new Color(210, 130, 80), 2.5);
            drawTradeRoute(g, new double[][]{{23.0, 37.5}, {24.5, 36.7}}, new Color(100, 180, 220), 2.5);
            drawTradeRoute(g, new double[][]{{113.0, 28.0}, {115.0, 29.5}, {118.0, 31.0}}, new Color(210, 120, 70), 2.5);
            drawTradeRoute(g, new double[][]{{130.0, -15.0}, {135.0, -20.0}, {140.0, -30.0}}, new Color(50, 170, 160), 2.5);
            drawTradeRoute(g, new double[][]{{-105.0, 40.0}, {-102.0, 38.0}, {-98.0, 35.0}}, new Color(200, 100, 90), 2.5);
            drawTradeRoute(g, new double[][]{{-75.0, -10.0}, {-72.0, -12.0}}, new Color(200, 100, 90), 2.0);
        } else {
            drawTradeRoute(g, new double[][]{{114.0, 34.5}, {108.9, 34.3}, {102.0, 38.0}, {94.0, 40.0}, {88.0, 43.0}, {75.0, 40.0}, {68.0, 38.5}, {60.0, 36.5}, {51.0, 35.7}, {44.0, 36.0}, {36.0, 36.0}, {29.0, 41.0}}, new Color(255, 180, 50), 3.5);
            drawTradeRoute(g, new double[][]{{94.0, 40.0}, {84.0, 44.0}, {76.0, 43.0}, {69.0, 42.5}, {60.0, 45.0}, {50.0, 47.0}, {40.0, 47.0}, {35.0, 46.5}}, new Color(230, 150, 40), 3.0);
            drawTradeRoute(g, new double[][]{{121.0, 31.0}, {113.0, 23.0}, {105.0, 10.0}, {100.0, 4.0}, {95.0, 5.5}, {80.0, 8.0}, {75.0, 15.0}, {58.0, 23.0}, {48.0, 14.0}, {43.0, 12.5}, {35.0, 24.0}, {32.5, 30.0}}, new Color(80, 200, 255), 3.5);
            drawTradeRoute(g, new double[][]{{48.0, 14.0}, {40.0, -5.0}, {40.5, -15.0}, {35.0, -20.0}}, new Color(60, 170, 240), 3.0);
            drawTradeRoute(g, new double[][]{{-5.0, 36.0}, {0.0, 28.0}, {1.0, 23.0}, {-1.0, 18.0}, {-3.0, 16.5}}, new Color(255, 120, 60), 3.0);
            drawTradeRoute(g, new double[][]{{10.0, 33.0}, {14.0, 27.0}, {13.0, 19.0}, {12.0, 13.0}}, new Color(255, 120, 60), 3.0);
            drawTradeRoute(g, new double[][]{{30.0, 30.0}, {31.0, 24.0}, {32.0, 18.0}, {32.5, 15.5}}, new Color(255, 140, 70), 3.0);
            drawTradeRoute(g, new double[][]{{20.0, 55.0}, {18.0, 50.0}, {14.0, 46.0}, {12.0, 44.0}}, new Color(240, 200, 80), 2.8);
            drawTradeRoute(g, new double[][]{{10.0, 54.0}, {5.0, 51.0}, {-2.0, 48.0}, {-4.0, 43.0}, {-3.0, 40.0}}, new Color(220, 190, 80), 2.8);
            drawTradeRoute(g, new double[][]{{-99.0, 19.5}, {-96.0, 18.0}, {-93.0, 17.0}, {-90.0, 16.0}, {-88.0, 15.0}}, new Color(180, 100, 220), 3.0);
            drawTradeRoute(g, new double[][]{{-77.0, -12.0}, {-75.0, -14.0}, {-71.0, -16.5}, {-68.0, -16.5}, {-65.0, -19.0}}, new Color(200, 120, 240), 3.2);

            drawNavigableRiverCorridors(g, year);
        }

        g.dispose();
        return HistoricalMapGenerator.applyAltimetryCoastlineMask(img);
    }

    public static void drawNavigableRiverCorridors(Graphics2D g, long year) {
        Color riverCol = new Color(70, 180, 240, 210);
        drawTradeRoute(g, new double[][]{{31.5, 24.0}, {32.6, 25.7}, {32.0, 28.0}, {31.2, 30.0}, {30.5, 31.3}}, riverCol, 3.2);
        drawTradeRoute(g, new double[][]{{40.0, 38.0}, {42.0, 36.5}, {44.5, 33.5}, {47.5, 31.0}, {48.5, 30.0}}, riverCol, 3.0);
        drawTradeRoute(g, new double[][]{{38.5, 38.5}, {40.5, 35.0}, {44.0, 32.5}, {47.0, 30.5}}, riverCol, 2.8);
        drawTradeRoute(g, new double[][]{{74.0, 34.0}, {71.5, 30.0}, {68.5, 25.5}, {67.5, 24.0}}, riverCol, 3.0);
        drawTradeRoute(g, new double[][]{{78.5, 30.0}, {82.0, 26.5}, {85.0, 25.5}, {88.0, 24.0}, {90.0, 22.5}}, riverCol, 3.2);
        drawTradeRoute(g, new double[][]{{103.0, 34.0}, {108.5, 34.5}, {111.0, 34.8}, {115.0, 35.0}, {118.0, 37.5}}, riverCol, 3.2);
        drawTradeRoute(g, new double[][]{{100.0, 29.0}, {107.0, 29.5}, {112.5, 30.5}, {116.0, 31.0}, {121.5, 31.3}}, riverCol, 3.6);
        drawTradeRoute(g, new double[][]{{11.0, 44.5}, {8.5, 48.0}, {7.5, 50.0}, {6.0, 51.5}, {4.5, 52.0}}, riverCol, 3.0);
        drawTradeRoute(g, new double[][]{{12.0, 49.0}, {16.5, 48.2}, {19.0, 47.5}, {26.0, 44.5}, {29.0, 45.2}}, riverCol, 3.4);
        drawTradeRoute(g, new double[][]{{32.0, 54.8}, {30.5, 50.4}, {35.0, 47.8}, {32.6, 46.6}}, riverCol, 2.6);
        drawTradeRoute(g, new double[][]{{33.0, 57.0}, {39.8, 57.6}, {49.1, 55.8}, {48.7, 51.5}, {44.5, 48.7}, {48.0, 46.3}}, riverCol, 3.0);
        drawTradeRoute(g, new double[][]{{-10.7, 10.0}, {-8.3, 11.4}, {-3.0, 16.8}, {0.0, 16.2}, {6.5, 6.0}}, riverCol, 2.6);
        drawTradeRoute(g, new double[][]{{-80.0, 40.4}, {-84.5, 39.1}, {-89.0, 37.0}, {-90.0, 35.0}, {-91.0, 32.5}, {-90.0, 29.9}}, riverCol, 3.0);
        drawTradeRoute(g, new double[][]{{-73.2, -3.7}, {-60.0, -3.1}, {-54.7, -2.4}, {-50.0, -1.0}}, riverCol, 3.2);
    }

    public static void drawTradeRoute(Graphics2D g, double[][] coords, Color col, double strokeWidth) {
        if (coords == null || coords.length < 2) return;
        int n = coords.length;
        int[] xs = new int[n];
        int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = (int) ((coords[i][0] + 180.0) / 360.0 * HistoricalMapGenerator.WIDTH);
            ys[i] = (int) ((90.0 - coords[i][1]) / 180.0 * HistoricalMapGenerator.HEIGHT);
        }

        Color haloCol = new Color(col.getRed(), col.getGreen(), col.getBlue(), 60);
        g.setColor(haloCol);
        g.setStroke(new BasicStroke((float) (strokeWidth * 3.5), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(xs, ys, n);

        g.setColor(col);
        g.setStroke(new BasicStroke((float) strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(xs, ys, n);

        int r = (int) Math.max(3, strokeWidth * 1.5);
        for (int i = 0; i < n; i++) {
            g.setColor(Color.WHITE);
            g.fillOval(xs[i] - r, ys[i] - r, r * 2, r * 2);
            g.setColor(col);
            g.drawOval(xs[i] - r, ys[i] - r, r * 2, r * 2);
        }
    }
}
