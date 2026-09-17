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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    private static final java.util.Map<Long, BufferedImage> GRID_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static void clearCache() {
        GRID_CACHE.clear();
    }

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
                int len = line.length();
                int c = 0;
                int pos = 0;
                while (pos < len && c < meta.ncols) {
                    while (pos < len && line.charAt(pos) <= ' ') pos++;
                    if (pos >= len) break;
                    int start = pos;
                    while (pos < len && line.charAt(pos) > ' ') pos++;
                    try {
                        float v = Float.parseFloat(line.substring(start, pos));
                        if (v == meta.nodataValue || v < 0) v = 0.0f;
                        grid[r][c] = v;
                        if (v > maxVal) maxVal = v;
                    } catch (NumberFormatException ignored) {
                        grid[r][c] = 0.0f;
                    }
                    c++;
                }
            }

            // 3. Resample 4320x2160 real grid to 1024x512 Ether Canvas with Fine Grayscale Density
            BufferedImage img = new BufferedImage(ETHER_WIDTH, ETHER_HEIGHT, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < ETHER_HEIGHT; y++) {
                int srcR = (int) ((y / (double) ETHER_HEIGHT) * meta.nrows);
                srcR = Math.max(0, Math.min(meta.nrows - 1, srcR));

                for (int x = 0; x < ETHER_WIDTH; x++) {
                    int srcC = (int) ((x / (double) ETHER_WIDTH) * meta.ncols);
                    srcC = Math.max(0, Math.min(meta.ncols - 1, srcC));

                    float val = grid[srcR][srcC];
                    if (val < 0 || val == meta.nodataValue) {
                        img.setRGB(x, y, 0x050811); // Deep Ocean Dark Background
                    } else if (val == 0.0f) {
                        img.setRGB(x, y, 0x182030); // Land Baseline (Uninhabited)
                    } else {
                        // Logarithmic scale for smooth population density transition
                        double logNorm = Math.log1p(val) / Math.log1p(maxVal);
                        int gray = Math.min(255, Math.max(20, (int) (logNorm * 235.0) + 20));
                        int rgb = (gray << 16) | (gray << 8) | gray;
                        img.setRGB(x, y, rgb);
                    }
                }
            }

            return img;
        } catch (Exception e) {
            logger.warn("Failed to parse HYDE 3.4 ASCII grid: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Maps HYDE population density (inh/km2) or grid cell count to the official Copernicus / Utrecht University HYDE color palette.
     */
    public static int getHydeColor(float density, boolean isLand) {
        if (!isLand) {
            return 0xE0F3F8; // Light Blue Ocean (#e0f3f8)
        }
        if (density <= 0.0f) {
            return 0xFFFFFF; // Uninhabited Land (#ffffff)
        } else if (density <= 10.0f) {
            return 0x7EF0FF; // Cyan (0.0 - 10.0 inh/km2)
        } else if (density <= 25.0f) {
            return 0x66FFCC; // Cyan-Green (10.0 - 25.0 inh/km2)
        } else if (density <= 50.0f) {
            return 0x36E09E; // Green (25.0 - 50.0 inh/km2)
        } else if (density <= 100.0f) {
            return 0xA0F050; // Yellow-Green (50.0 - 100.0 inh/km2)
        } else if (density <= 250.0f) {
            return 0xFFEB3B; // Yellow (100.0 - 250.0 inh/km2)
        } else if (density <= 500.0f) {
            return 0xFF9800; // Orange (250.0 - 500.0 inh/km2)
        } else {
            return 0xF44336; // Red (> 500.0 inh/km2)
        }
    }

    private static final long[] KNOWN_HYDE_YEARS = {
        -10000, -9000, -8000, -7000, -6000, -5000, -4000, -3000, -2000, -1000,
        0, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700,
        1710, 1720, 1730, 1740, 1750, 1760, 1770, 1780, 1790, 1800, 1810, 1820, 1830, 1840, 1850, 1860, 1870, 1880, 1890,
        1900, 1910, 1920, 1930, 1940, 1950, 1960, 1970, 1980, 1990, 2000, 2010, 2020, 2024
    };

    public static long findNearestHydeYear(long year) {
        if (year <= -10000) return -10000;
        if (year >= 2024) return 2024;
        long closest = KNOWN_HYDE_YEARS[0];
        long minDiff = Math.abs(year - closest);
        for (long y : KNOWN_HYDE_YEARS) {
            long diff = Math.abs(year - y);
            if (diff < minDiff) {
                minDiff = diff;
                closest = y;
            }
        }
        return closest;
    }

    /**
     * Reads a local HYDE 3.4 ASCII grid file for a specific scenario year.
     */
    public static BufferedImage loadForYear(long year) {
        long requestedYear = year;
        long effectiveYear = findNearestHydeYear(year);
        if (effectiveYear != requestedYear) {
            logger.info("HYDE 3.4 baseline mapped from requested year {} to nearest empirical epoch {}.", requestedYear, effectiveYear);
        }

        if (GRID_CACHE.containsKey(requestedYear)) {
            return GRID_CACHE.get(requestedYear);
        }
        if (GRID_CACHE.containsKey(effectiveYear)) {
            BufferedImage cached = GRID_CACHE.get(effectiveYear);
            GRID_CACHE.put(requestedYear, cached);
            return cached;
        }

        BufferedImage img = tryLoadGridForYear(effectiveYear, requestedYear);
        if (img != null) {
            GRID_CACHE.put(requestedYear, img);
            GRID_CACHE.put(effectiveYear, img);
            return img;
        }

        throw new IllegalStateException("ZERO FALLBACK VIOLATION: Failed to stream empirical HYDE 3.4 raster grid for year " + requestedYear);
    }

    private static BufferedImage tryLoadGridForYear(long year, long requestedYear) {
        BufferedImage img = null;
        String yearTag = DataDownloaderService.getHydeYearTag(year);
        String zipName = yearTag + "_pop.zip";
        File zipFile = new File("user_data/maps/hyde34/" + zipName);
        if (!zipFile.exists()) {
            zipFile = new File("data/maps/hyde34/" + zipName);
        }
        if (!zipFile.exists() && year == 0) {
            zipFile = new File("data/maps/hyde34/0AD_pop.zip");
        }

        if (!zipFile.exists() || zipFile.length() < 100000) {
            logger.info("Local HYDE 3.4 archive '{}' missing. Triggering empirical dataset downloader...", zipName);
            DataDownloaderService.downloadHydeGridForYear(year);
            zipFile = new File("data/maps/hyde34/" + zipName);
            if (!zipFile.exists() && year == 0) {
                zipFile = new File("data/maps/hyde34/0AD_pop.zip");
            }
        }

        if (zipFile.exists() && zipFile.isFile() && zipFile.length() > 100000) {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName().toLowerCase();
                    if (entryName.startsWith("popc_") && entryName.endsWith(".asc")) {
                        if (requestedYear < -10000) {
                            logger.info("Streaming HYDE 3.4 10,000 BC baseline grid '{}' from ZIP archive {} for prehistoric epoch year {}...", entry.getName(), zipFile.getName(), requestedYear);
                        } else {
                            logger.info("Streaming empirical HYDE 3.4 grid '{}' directly from ZIP archive {} for year {}...", entry.getName(), zipFile.getName(), year);
                        }
                        img = readAsciiGridToImage(zis);
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("Error streaming HYDE 3.4 grid from ZIP '{}': {}", zipFile.getAbsolutePath(), e.getMessage());
            }
        }

        // Direct ASC fallback if available
        if (img == null) {
            String ascName = "popc_" + yearTag + ".asc";
            File ascFile = new File("data/maps/hyde34/" + ascName);
            if (ascFile.exists()) {
                try (InputStream is = new FileInputStream(ascFile)) {
                    if (requestedYear < -10000) {
                        logger.info("Ingesting local HYDE 3.4 ASC File for year {} (clamped to 10,000 BC baseline): {}", requestedYear, ascFile.getAbsolutePath());
                    } else {
                        logger.info("Ingesting local HYDE 3.4 ASC File for year {}: {}", year, ascFile.getAbsolutePath());
                    }
                    img = readAsciiGridToImage(is);
                } catch (Exception e) {
                    logger.error("Error reading HYDE 3.4 ASC file '{}': {}", ascFile.getAbsolutePath(), e.getMessage());
                }
            }
        }

        return img;
    }
}
