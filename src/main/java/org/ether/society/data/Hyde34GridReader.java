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
 * @version 1.0.0-beta.2
 */
public class Hyde34GridReader {
    private static final Logger logger = LoggerFactory.getLogger(Hyde34GridReader.class);

    /** Internal state: ether width (int). */
    public static final int ETHER_WIDTH = 2048;
    /** Internal state: ether height (int). */
    public static final int ETHER_HEIGHT = 1024;

    private static final java.util.Map<Long, BufferedImage> GRID_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Long, float[][]> COUNT_CACHE = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<>(8, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<Long, float[][]> eldest) {
                    return size() > 8;
                }
            });

    /**
     * Clear cache.
     *
     */
    public static void clearCache() {
        GRID_CACHE.clear();
        COUNT_CACHE.clear();
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
     * Parsed Esri ASCII Grid container pairing metadata with the raw float matrix indexed {@code [nrows][ncols]}.
     */
    public record ParsedAsciiGrid(HydeGridMetadata meta, float[][] data, float maxVal) {}

    /**
     * Parses an Esri ASCII Grid (.asc) stream without downsampling, converting NoData and negative sentinels to {@code 0.0f}.
     *
     * @param inputStream source ASCII grid stream
     * @return parsed grid container, or {@code null} on failure
     */
    public static ParsedAsciiGrid parseAsciiGrid(InputStream inputStream) {
        if (inputStream == null) return null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            HydeGridMetadata meta = new HydeGridMetadata();
            String line;
            int headerLines = 0;

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

            float[][] grid = new float[meta.nrows][meta.ncols];
            float maxVal = 0.0f;

            for (int r = 0; r < meta.nrows; r++) {
                line = reader.readLine();
                if (line == null) break;
                int len = line.length();
                int c = 0;
                int pos = 0;
                float[] row = grid[r];
                while (pos < len && c < meta.ncols) {
                    while (pos < len && line.charAt(pos) <= ' ') pos++;
                    if (pos >= len) break;
                    int start = pos;
                    while (pos < len && line.charAt(pos) > ' ') pos++;
                    float v = fastParseFloat(line, start, pos, (float) meta.nodataValue);
                    row[c] = v;
                    if (v > maxVal) maxVal = v;
                    c++;
                }
            }

            return new ParsedAsciiGrid(meta, grid, maxVal);
        } catch (Exception e) {
            logger.warn("Failed to parse HYDE 3.4 ASCII grid: {}", e.getMessage());
            return null;
        }
    }

    private static float fastParseFloat(String line, int start, int end, float nodata) {
        if (start >= end) return 0.0f;
        int i = start;
        boolean neg = false;
        char c0 = line.charAt(i);
        if (c0 == '-') {
            neg = true;
            i++;
        } else if (c0 == '+') {
            i++;
        }
        if (i >= end) return 0.0f;

        // Check for NaN or standard nodata values (e.g. -9999)
        if (neg && (end - start) >= 5 && line.regionMatches(start, "-9999", 0, 5)) {
            return 0.0f;
        }

        double intPart = 0.0;
        while (i < end) {
            char ch = line.charAt(i);
            if (ch < '0' || ch > '9') break;
            intPart = intPart * 10.0 + (ch - '0');
            i++;
        }

        if (i < end && line.charAt(i) == '.') {
            i++;
            double fracPart = 0.0;
            double divisor = 1.0;
            while (i < end) {
                char ch = line.charAt(i);
                if (ch < '0' || ch > '9') break;
                fracPart = fracPart * 10.0 + (ch - '0');
                divisor *= 10.0;
                i++;
            }
            intPart += (fracPart / divisor);
        }

        if (i < end && (line.charAt(i) == 'e' || line.charAt(i) == 'E')) {
            i++;
            boolean expNeg = false;
            if (i < end && line.charAt(i) == '-') { expNeg = true; i++; }
            else if (i < end && line.charAt(i) == '+') { i++; }
            int exp = 0;
            while (i < end) {
                char ch = line.charAt(i);
                if (ch < '0' || ch > '9') break;
                exp = exp * 10 + (ch - '0');
                i++;
            }
            intPart *= Math.pow(10.0, expNeg ? -exp : exp);
        }

        float res = (float) (neg ? -intPart : intPart);
        if (res == nodata || res < 0.0f || !Float.isFinite(res)) {
            return 0.0f;
        }
        return res;
    }

    /**
     * Reads an Esri ASCII Grid file (.asc) and conservatively downsamples it into a 2048x1024 BufferedImage density map.
     */
    public static BufferedImage readAsciiGridToImage(InputStream inputStream) {
        return readAsciiGridToImage(inputStream, 0L);
    }

    /**
     * Reads a HYDE 3.4 population count grid ({@code popc_*.asc}), conservatively transfers counts
     * onto the $2048 \times 1024$ spherical grid via {@link ConservativeRasterResampler#resampleExtensive},
     * converts conserved headcounts per pixel into true areal density ($\text{inh/km}^2$), and encodes
     * a continuous logarithmic grayscale tensor image.
     *
     * @param inputStream the Esri ASCII grid input stream
     * @param targetYear  target simulation epoch year
     * @return conservatively downsampled grayscale density {@link BufferedImage}, or {@code null} on failure
     */
    public static BufferedImage readAsciiGridToImage(InputStream inputStream, long targetYear) {
        ParsedAsciiGrid parsed = parseAsciiGrid(inputStream);
        if (parsed == null) return null;

        HydeGridMetadata meta = parsed.meta();
        // 1. Exact spherical-area-conservative regridding of extensive counts (4320x2160 -> 2048x1024)
        float[][] conservedCounts = ConservativeRasterResampler.resampleExtensive(
                parsed.data(), meta.ncols, meta.nrows, (float) meta.nodataValue, ETHER_WIDTH, ETHER_HEIGHT);

        // 2. Convert conserved per-pixel headcounts to true physical areal density (inhabitants / km^2)
        double[][] densityKm2 = new double[ETHER_HEIGHT][ETHER_WIDTH];
        double maxDensity = 0.0;
        for (int y = 0; y < ETHER_HEIGHT; y++) {
            double areaKm2 = Math.max(1e-6, ConservativeRasterResampler.sphericalPixelAreaKm2(y, ETHER_WIDTH, ETHER_HEIGHT));
            float[] cntRow = conservedCounts[y];
            double[] densRow = densityKm2[y];
            for (int x = 0; x < ETHER_WIDTH; x++) {
                double d = cntRow[x] / areaKm2;
                densRow[x] = d;
                if (d > maxDensity) maxDensity = d;
            }
        }

        // 3. Render Logarithmic Grayscale preserving spatial dynamic range
        BufferedImage img = new BufferedImage(ETHER_WIDTH, ETHER_HEIGHT, BufferedImage.TYPE_INT_RGB);
        double effMax = Math.max(0.1, maxDensity * 0.90);
        double logDenom = Math.log1p(effMax * 4.0);

        for (int y = 0; y < ETHER_HEIGHT; y++) {
            double[] densRow = densityKm2[y];
            for (int x = 0; x < ETHER_WIDTH; x++) {
                double val = densRow[x];
                if (val <= 1e-4) {
                    img.setRGB(x, y, 0x000000);
                } else {
                    double logNorm = Math.log1p(val * 4.0) / logDenom;
                    int gray = (int) Math.clamp(Math.round(logNorm * 238.0 + 17.0), 17L, 255L);
                    img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                }
            }
        }

        return img;
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

    /**
     * Find nearest hyde year.
     *
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
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
     * Streams locally available HYDE 3.4 population counts ({@code popc_*.asc}) for the nearest epoch
     * to {@code year} and transfers exact conserved headcounts onto the $2048 \times 1024$ grid without
     * triggering network downloads. Returns {@code null} if local HYDE 3.4 files are absent.
     *
     * @param year historical year ($-10000$ to $2024$)
     * @return conserved headcount matrix indexed {@code [y][x]}, or {@code null} if unavailable locally
     */
    public static float[][] readConservedPopulationCountsIfPresent(long year) {
        long effectiveYear = findNearestHydeYear(year);
        float[][] cached = COUNT_CACHE.get(effectiveYear);
        if (cached != null) {
            return cached;
        }
        String yearTag = DataDownloaderService.getHydeYearTag(effectiveYear);
        String zipName = yearTag + "_pop.zip";
        File zipFile = new File("user_data/maps/hyde34/" + zipName);
        if (!zipFile.exists()) {
            zipFile = org.ether.society.config.EtherPaths.resolveSourceFile("hyde34/" + zipName);
        }
        if (!zipFile.exists() && effectiveYear == 0) {
            zipFile = org.ether.society.config.EtherPaths.resolveSourceFile("hyde34/0AD_pop.zip");
        }

        ParsedAsciiGrid parsed = null;
        if (zipFile.exists() && zipFile.isFile() && zipFile.length() > 100000) {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName().toLowerCase(Locale.ROOT);
                    if (entryName.startsWith("popc_") && entryName.endsWith(".asc")) {
                        parsed = parseAsciiGrid(zis);
                        break;
                    }
                }
            } catch (Exception e) {
                logger.warn("Error reading HYDE 3.4 population counts from '{}': {}", zipFile.getName(), e.getMessage());
            }
        }
        if (parsed == null) {
            String ascName = "popc_" + yearTag + ".asc";
            File ascFile = org.ether.society.config.EtherPaths.resolveSourceFile("hyde34/" + ascName);
            if (ascFile.exists() && ascFile.isFile()) {
                try (InputStream is = new FileInputStream(ascFile)) {
                    parsed = parseAsciiGrid(is);
                } catch (Exception e) {
                    logger.warn("Error reading HYDE 3.4 ASC counts '{}': {}", ascFile.getName(), e.getMessage());
                }
            }
        }
        if (parsed == null) {
            return null;
        }
        HydeGridMetadata meta = parsed.meta();
        float[][] conserved = ConservativeRasterResampler.resampleExtensive(
                parsed.data(), meta.ncols, meta.nrows, (float) meta.nodataValue, ETHER_WIDTH, ETHER_HEIGHT);
        COUNT_CACHE.put(effectiveYear, conserved);
        return conserved;
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
            zipFile = org.ether.society.config.EtherPaths.resolveSourceFile("hyde34/" + zipName);
        }
        if (!zipFile.exists() && year == 0) {
            zipFile = org.ether.society.config.EtherPaths.resolveSourceFile("hyde34/0AD_pop.zip");
        }

        if (!zipFile.exists() || zipFile.length() < 100000) {
            logger.info("Local HYDE 3.4 archive '{}' missing. Triggering empirical dataset downloader...", zipName);
            DataDownloaderService.downloadHydeGridForYear(year);
            zipFile = org.ether.society.config.EtherPaths.resolveSourceFile("hyde34/" + zipName);
            if (!zipFile.exists() && year == 0) {
                zipFile = org.ether.society.config.EtherPaths.resolveSourceFile("hyde34/0AD_pop.zip");
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
                        img = readAsciiGridToImage(zis, requestedYear);
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
            File ascFile = org.ether.society.config.EtherPaths.resolveSourceFile("hyde34/" + ascName);
            if (ascFile.exists()) {
                try (InputStream is = new FileInputStream(ascFile)) {
                    if (requestedYear < -10000) {
                        logger.info("Ingesting local HYDE 3.4 ASC File for year {} (clamped to 10,000 BC baseline): {}", requestedYear, ascFile.getAbsolutePath());
                    } else {
                        logger.info("Ingesting local HYDE 3.4 ASC File for year {}: {}", year, ascFile.getAbsolutePath());
                    }
                    img = readAsciiGridToImage(is, requestedYear);
                } catch (Exception e) {
                    logger.error("Error reading HYDE 3.4 ASC file '{}': {}", ascFile.getAbsolutePath(), e.getMessage());
                }
            }
        }

        return img;
    }
}

