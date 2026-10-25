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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Cartographic rasterizer for UNESCO WHYMAP global groundwater aquifers,
 * river networks, and paleo-recharge pluvial dynamics across historical epochs.
 */
public final class UnescoWhymapAquiferRasterizer {

    private static final Logger logger = LoggerFactory.getLogger(UnescoWhymapAquiferRasterizer.class);
    private static volatile float[][] cachedWhymapBlurredGrid = null;

    private UnescoWhymapAquiferRasterizer() {
        // Utility class
    }

    /**
     * Computes the paleo-recharge pluvial factor according to historical year and coordinates.
     *
     * @param lon longitude in degrees
     * @param lat latitude in degrees
     * @param year historical year
     * @return scaling factor
     */
    public static double getPaleoAquiferRechargeFactor(double lon, double lat, long year) {
        if (year <= -95000L) {
            double centerDist = Math.hypot((lon - 15.0) / 38.0, (lat - 22.0) / 12.0);
            double pluvial = Math.exp(-centerDist * centerDist);
            return 1.10 + 1.15 * pluvial;
        } else if (year <= -65000L) {
            double dAsianDrought = Math.exp(-(Math.pow(lat - 12.0, 2) / 350.0 + Math.pow(lon - 85.0, 2) / 1000.0));
            double dAfricanDrought = Math.exp(-(Math.pow(lat - 8.0, 2) / 350.0 + Math.pow(lon - 20.0, 2) / 600.0));
            return 0.75 - (dAsianDrought * 0.20) - (dAfricanDrought * 0.10);
        } else if (year <= -30000L) {
            double dSahul = Math.exp(-(Math.pow(lat - (-25.0), 2) / 200.0 + Math.pow(lon - 134.0, 2) / 350.0));
            double dMed = Math.exp(-(Math.pow(lat - 35.0, 2) / 150.0 + Math.pow(lon - 20.0, 2) / 450.0));
            return 0.85 + (dSahul * 0.75) + (dMed * 0.40);
        } else if (year <= -16000L) {
            double permafrostFactor = 1.0 / (1.0 + Math.exp(-(lat - 45.0) / 4.0));
            return 0.55 - (permafrostFactor * 0.15);
        } else if (year <= -5000L) {
            double centerDist = Math.hypot((lon - 18.0) / 36.0, (lat - 21.0) / 11.0);
            double pluvial = Math.exp(-centerDist * centerDist);
            return 1.05 + 1.10 * pluvial;
        }
        return 1.0;
    }

    /**
     * Reads or proceduralizes the UNESCO WHYMAP global groundwater aquifer baseline grid.
     *
     * @return 2048x1024 float array of base groundwater capacities
     */
    public static synchronized float[][] getOrComputeBaseWhymapGrid() {
        if (cachedWhymapBlurredGrid != null) return cachedWhymapBlurredGrid;

        int width = 2048, height = 1024;
        File shpFile = new File("data/maps/whymap_groundwater/extracted/WHYMAP_GWR/shp/whymap_GW_aquifers_v1_poly.shp");
        if (!shpFile.exists()) {
            shpFile = new File("data/maps/whymap_groundwater/whymap_GW_aquifers_v1_poly.shp");
        }
        File dbfFile = new File("data/maps/whymap_groundwater/extracted/WHYMAP_GWR/shp/whymap_GW_aquifers_v1_poly.dbf");
        if (!dbfFile.exists()) {
            dbfFile = new File("data/maps/whymap_groundwater/whymap_GW_aquifers_v1_poly.dbf");
        }
        File riversShp = new File("data/maps/whymap_groundwater/extracted/WHYMAP_GWR/shp/whymap_rivers__v1_line.shp");
        if (!riversShp.exists()) {
            riversShp = new File("data/maps/whymap_groundwater/whymap_rivers__v1_line.shp");
        }

        float[][] blurred = new float[height][width];

        if (shpFile.exists()) {
            try {
                int[] hygeoCodes = null;
                if (dbfFile.exists()) {
                    try (var dis = new java.io.DataInputStream(new java.io.BufferedInputStream(new java.io.FileInputStream(dbfFile)))) {
                        byte[] b32 = new byte[32];
                        dis.readFully(b32);
                        int numRecs = (b32[4] & 0xFF) | ((b32[5] & 0xFF) << 8) | ((b32[6] & 0xFF) << 16) | ((b32[7] & 0xFF) << 24);
                        int headerLen = (b32[8] & 0xFF) | ((b32[9] & 0xFF) << 8);
                        int recLen = (b32[10] & 0xFF) | ((b32[11] & 0xFF) << 8);
                        dis.skipBytes(headerLen - 32);

                        hygeoCodes = new int[numRecs];
                        for (int r = 0; r < numRecs; r++) {
                            byte del = dis.readByte();
                            byte[] bHygeo = new byte[5];
                            dis.readFully(bHygeo);
                            if (recLen > 6) {
                                dis.skipBytes(recLen - 6);
                            }
                            String sH = new String(bHygeo).trim();
                            try {
                                hygeoCodes[r] = Integer.parseInt(sH);
                            } catch (Exception ignored) {
                                hygeoCodes[r] = 33;
                            }
                        }
                    }
                }

                BufferedImage whymapImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
                Graphics2D gw = whymapImg.createGraphics();
                gw.setColor(new Color(60, 60, 60));
                gw.fillRect(0, 0, width, height);
                gw.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                try (var disShp = new java.io.DataInputStream(new java.io.BufferedInputStream(new java.io.FileInputStream(shpFile), 65536))) {
                    disShp.skipBytes(100);
                    int curRec = 0;
                    while (true) {
                        try {
                            int recNum = disShp.readInt();
                            int contentLenWords = disShp.readInt();
                            int contentLenBytes = contentLenWords * 2;
                            if (contentLenBytes <= 0) { curRec++; continue; }
                            byte[] recBytes = new byte[contentLenBytes];
                            disShp.readFully(recBytes);
                            ByteBuffer bb = ByteBuffer.wrap(recBytes).order(ByteOrder.LITTLE_ENDIAN);
                            int recShapeType = bb.getInt(0);
                            if (recShapeType == 5 || recShapeType == 15) {
                                int code = (hygeoCodes != null && curRec < hygeoCodes.length) ? hygeoCodes[curRec] : 11;
                                int colorVal;
                                if (code >= 10 && code <= 19) {
                                    colorVal = 215 + (code % 5) * 5;
                                } else if (code >= 20 && code <= 29) {
                                    colorVal = 135 + (code % 5) * 5;
                                } else if (code >= 80) {
                                    colorVal = 20;
                                } else {
                                    colorVal = 65 + (code % 5) * 4;
                                }

                                int numParts = bb.getInt(36);
                                int numPoints = bb.getInt(40);
                                int[] parts = new int[numParts];
                                for (int p = 0; p < numParts; p++) {
                                    parts[p] = bb.getInt(44 + p * 4);
                                }
                                int ptsOffset = 44 + numParts * 4;
                                for (int p = 0; p < numParts; p++) {
                                    int start = parts[p];
                                    int end = (p + 1 < numParts) ? parts[p + 1] : numPoints;
                                    if (end > start) {
                                        Path2D.Double path = new Path2D.Double();
                                        for (int pt = start; pt < end; pt++) {
                                            double px = bb.getDouble(ptsOffset + pt * 16);
                                            double py = bb.getDouble(ptsOffset + pt * 16 + 8);
                                            double rx = (px + 180.0) / 360.0 * width;
                                            double ry = (90.0 - py) / 180.0 * height;
                                            if (pt == start) path.moveTo(rx, ry);
                                            else path.lineTo(rx, ry);
                                        }
                                        path.closePath();
                                        gw.setColor(new Color(colorVal, colorVal, colorVal));
                                        gw.fill(path);
                                    }
                                }
                            }
                            curRec++;
                        } catch (EOFException eof) {
                            break;
                        }
                    }
                }

                if (riversShp.exists()) {
                    gw.setColor(new Color(230, 230, 230));
                    gw.setStroke(new BasicStroke(1.6f));
                    try (var disRiv = new java.io.DataInputStream(new java.io.BufferedInputStream(new java.io.FileInputStream(riversShp), 65536))) {
                        disRiv.skipBytes(100);
                        while (true) {
                            try {
                                int recNum = disRiv.readInt();
                                int contentLenWords = disRiv.readInt();
                                int contentLenBytes = contentLenWords * 2;
                                if (contentLenBytes <= 0) continue;
                                byte[] recBytes = new byte[contentLenBytes];
                                disRiv.readFully(recBytes);
                                ByteBuffer bb = ByteBuffer.wrap(recBytes).order(ByteOrder.LITTLE_ENDIAN);
                                int recShapeType = bb.getInt(0);
                                if (recShapeType == 3 || recShapeType == 13) {
                                    int numParts = bb.getInt(36);
                                    int numPoints = bb.getInt(40);
                                    int[] parts = new int[numParts];
                                    for (int p = 0; p < numParts; p++) parts[p] = bb.getInt(44 + p * 4);
                                    int ptsOffset = 44 + numParts * 4;
                                    for (int p = 0; p < numParts; p++) {
                                        int start = parts[p];
                                        int end = (p + 1 < numParts) ? parts[p + 1] : numPoints;
                                        if (end > start) {
                                            Path2D.Double path = new Path2D.Double();
                                            for (int pt = start; pt < end; pt++) {
                                                double px = bb.getDouble(ptsOffset + pt * 16);
                                                double py = bb.getDouble(ptsOffset + pt * 16 + 8);
                                                double rx = (px + 180.0) / 360.0 * width;
                                                double ry = (90.0 - py) / 180.0 * height;
                                                if (pt == start) path.moveTo(rx, ry);
                                                else path.lineTo(rx, ry);
                                            }
                                            gw.draw(path);
                                        }
                                    }
                                }
                            } catch (EOFException eof) {
                                break;
                            }
                        }
                    }
                }
                gw.dispose();

                float[][] k = {
                    {0.003f, 0.013f, 0.022f, 0.013f, 0.003f},
                    {0.013f, 0.059f, 0.097f, 0.059f, 0.013f},
                    {0.022f, 0.097f, 0.159f, 0.097f, 0.022f},
                    {0.013f, 0.059f, 0.097f, 0.059f, 0.013f},
                    {0.003f, 0.013f, 0.022f, 0.013f, 0.003f}
                };

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        float sum = 0.0f;
                        for (int dy = -2; dy <= 2; dy++) {
                            int ny = Math.clamp(y + dy, 0, height - 1);
                            for (int dx = -2; dx <= 2; dx++) {
                                int nx = (x + dx + width) % width;
                                int val = whymapImg.getRGB(nx, ny) & 0xFF;
                                sum += val * k[dy + 2][dx + 2];
                            }
                        }
                        blurred[y][x] = sum;
                    }
                }
                cachedWhymapBlurredGrid = blurred;
                return blurred;
            } catch (Exception e) {
                logger.warn("WHYMAP shapefile ingestion notice: {}, using procedural aquifer model", e.getMessage());
            }
        }

        BufferedImage mask = HistoricalMapGenerator.loadElevationMask();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int mx = Math.clamp((int) ((x + 0.5) * (mask != null ? mask.getWidth() : width) / width), 0, (mask != null ? mask.getWidth() : width) - 1);
                int my = Math.clamp((int) ((y + 0.5) * (mask != null ? mask.getHeight() : height) / height), 0, (mask != null ? mask.getHeight() : height) - 1);
                int land = (mask != null) ? mask.getRaster().getSample(mx, my, 0) : 255;
                double elevM = (mask != null) ? (land / 255.0) * 8848.0 : 200.0;
                double orographicCapacityFactor = elevM <= 350.0 ? 1.0 : (elevM <= 1000.0 ? 1.0 - (elevM - 350.0) / 650.0 * 0.70 : Math.max(0.08, 0.30 - (elevM - 1000.0) / 2000.0 * 0.22));
                blurred[y][x] = (float) (65.0 * orographicCapacityFactor);
            }
        }
        cachedWhymapBlurredGrid = blurred;
        return blurred;
    }

    /**
     * Rasterizes the UNESCO WHYMAP global groundwater aquifer and freshwater table tensor.
     *
     * @param type scenario type
     * @param scenario scenario model
     * @return rasterized 2048x1024 grayscale BufferedImage
     */
    public static BufferedImage rasterizeAquiferMap(String type, Scenario scenario) {
        long year = (scenario != null) ? scenario.getStartDateYear() : 2000L;
        int width = 2048, height = 1024;

        float[][] baseGrid = getOrComputeBaseWhymapGrid();
        BufferedImage outImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            double lat = 90.0 - (y + 0.5) / height * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = -180.0 + (x + 0.5) / width * 360.0;
                if (!HistoricalMapGenerator.isLand(lon, lat)) {
                    outImg.setRGB(x, y, 0x000000);
                    continue;
                }
                float rawVal = baseGrid[y][x];
                double rechargeFactor = getPaleoAquiferRechargeFactor(lon, lat, year);
                int lum = Math.clamp((int) Math.round(rawVal * rechargeFactor), 0, 255);
                outImg.setRGB(x, y, (lum << 16) | (lum << 8) | lum);
            }
        }
        return HistoricalMapGenerator.applyAltimetryCoastlineMask(outImg);
    }
}
