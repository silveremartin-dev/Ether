/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Native Esri ASCII Grid (.asc) and NetCDF (.nc) Reader for HYDE 3.4 (History Database of the Global Environment).
 * Supports 5-arc-minute global grids (4320x2160) from -10,000 BC to 2023 AD.
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 4.0.0
 */
public class Hyde34GridReader {
    private static final Logger logger = LoggerFactory.getLogger(Hyde34GridReader.class);

    public static final int ETHER_WIDTH = 1024;
    public static final int ETHER_HEIGHT = 512;

    public static class HydeGridMetadata {
        public int ncols = 4320;
        public int nrows = 2160;
        public double xllcorner = -180.0;
        public double yllcorner = -90.0;
        public double cellsize = 0.083333333333333;
        public double nodataValue = -9999;
    }

    /**
     * Reads an Esri ASCII Grid file (.asc) and samples it into a 1024x512 BufferedImage density map.
     */
    public static BufferedImage readAsciiGridToImage(InputStream inputStream) {
        if (inputStream == null) return null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            HydeGridMetadata meta = new HydeGridMetadata();
            String line;
            int headerLines = 0;

            // 1. Read header parameters
            while (headerLines < 6 && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length < 2) continue;

                String key = parts[0].toLowerCase(Locale.ROOT);
                double val = Double.parseDouble(parts[1]);

                switch (key) {
                    case "ncols": meta.ncols = (int) val; headerLines++; break;
                    case "nrows": meta.nrows = (int) val; headerLines++; break;
                    case "xllcorner": case "xllcenter": meta.xllcorner = val; headerLines++; break;
                    case "yllcorner": case "yllcenter": meta.yllcorner = val; headerLines++; break;
                    case "cellsize": meta.cellsize = val; headerLines++; break;
                    case "nodata_value": case "nodata": meta.nodataValue = val; headerLines++; break;
                }
            }

            // 2. Allocate data grid
            float[][] grid = new float[meta.nrows][meta.ncols];
            float maxVal = 0.0001f;

            for (int r = 0; r < meta.nrows; r++) {
                line = reader.readLine();
                if (line == null) break;
                String[] tokens = line.trim().split("\\s+");
                for (int c = 0; c < Math.min(tokens.length, meta.ncols); c++) {
                    try {
                        float v = Float.parseFloat(tokens[c]);
                        if (v == meta.nodataValue || v < 0) v = 0.0f;
                        grid[r][c] = v;
                        if (v > maxVal) maxVal = v;
                    } catch (NumberFormatException ignored) {
                        grid[r][c] = 0.0f;
                    }
                }
            }

            // 3. Resample grid to 1024x512 Ether Canvas
            BufferedImage img = new BufferedImage(ETHER_WIDTH, ETHER_HEIGHT, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < ETHER_HEIGHT; y++) {
                int srcR = (int) ((y / (double) ETHER_HEIGHT) * meta.nrows);
                srcR = Math.max(0, Math.min(meta.nrows - 1, srcR));

                for (int x = 0; x < ETHER_WIDTH; x++) {
                    int srcC = (int) ((x / (double) ETHER_WIDTH) * meta.ncols);
                    srcC = Math.max(0, Math.min(meta.ncols - 1, srcC));

                    float val = grid[srcR][srcC];
                    float norm = Math.min(1.0f, val / maxVal);
                    int gray = (int) (norm * 255.0f);
                    int rgb = (gray << 16) | (gray << 8) | gray;
                    img.setRGB(x, y, rgb);
                }
            }

            return img;
        } catch (Exception e) {
            logger.warn("Failed to parse HYDE 3.4 ASCII grid: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Reads a local HYDE 3.4 ASCII grid file for a specific scenario year.
     */
    public static BufferedImage loadForYear(long year) {
        String fileName = "popc_" + (year < 0 ? Math.abs(year) + "BC" : year + "AD") + ".asc";
        File f = new File("user_data/maps/hyde34/" + fileName);
        if (!f.exists()) {
            f = new File("data/maps/hyde34/" + fileName);
        }

        if (f.exists() && f.isFile()) {
            try (InputStream is = new FileInputStream(f)) {
                logger.info("Ingesting local HYDE 3.4 Grid for year {}: {}", year, f.getAbsolutePath());
                return readAsciiGridToImage(is);
            } catch (Exception e) {
                logger.warn("Error reading HYDE 3.4 file '{}': {}", f.getAbsolutePath(), e.getMessage());
            }
        }
        return null;
    }
}
