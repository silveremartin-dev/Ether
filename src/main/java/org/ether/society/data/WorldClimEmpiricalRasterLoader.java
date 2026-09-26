/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * WorldClim v2.1 Empirical Baseline Ingestion & Paleoclimatic Delta Engine.
 * Ingests 100% empirical GeoTIFF rasters (Bio1 mean temp, Bio12 annual precipitation, Bio4 seasonality)
 * using a high-performance native TIFF LZW float decoder, applies physical ocean SST/marine precipitation
 * baselines, and computes paleoclimatic delta anomalies (EPICA, Milankovitch, ICE-6G, African Humid Period).
 *
 * @author Silvere Martin-Michiellot & Gemini AI
 * @version 1.0.0-beta.1
 */
public class WorldClimEmpiricalRasterLoader {
    private static final Logger logger = LoggerFactory.getLogger(WorldClimEmpiricalRasterLoader.class);

    public static final int GRID_W = 2048;
    public static final int GRID_H = 1024;

    private static float[][] sstTempBaseline;
    private static float[][] precipBaseline;
    private static float[][] seasonBaseline;
    private static boolean isInitialized = false;

    // Biome Color Constants matching Ether Palette
    public static final int BIOME_DEEP_OCEAN = 0x172554;
    public static final int BIOME_OCEAN      = 0x1E3A8A;
    public static final int BIOME_GLACIER    = 0xF8FAFC;
    public static final int BIOME_SNOW       = 0xE2E8F0;
    public static final int BIOME_TUNDRA     = 0x94A3B8;
    public static final int BIOME_FOREST     = 0x15803D;
    public static final int BIOME_JUNGLE     = 0x14532D;
    public static final int BIOME_PLAINS     = 0x84CC16;
    public static final int BIOME_DESERT     = 0xEAB308;
    public static final int BIOME_HILLS      = 0x78716C;
    public static final int BIOME_MOUNTAINS  = 0x475569;

    public static synchronized void ensureInitialized() {
        if (isInitialized) return;

        sstTempBaseline = new float[GRID_H][GRID_W];
        precipBaseline  = new float[GRID_H][GRID_W];
        seasonBaseline  = new float[GRID_H][GRID_W];

        File tempFile = new File("data/maps/worldclim/bio_10m/wc2.1_10m_bio_1.tif");
        File precipFile = new File("data/maps/worldclim/bio_10m/wc2.1_10m_bio_12.tif");
        File seasonFile = new File("data/maps/worldclim/bio_10m/wc2.1_10m_bio_4.tif");

        boolean hasWorldClim = tempFile.exists() && precipFile.exists() && seasonFile.exists();

        if (hasWorldClim) {
            try {
                logger.info("Loading empirical WorldClim v2.1 GeoTIFF rasters via native TIFF LZW decoder...");
                float[][] tRaw = readWorldClimTiff(tempFile);
                float[][] pRaw = readWorldClimTiff(precipFile);
                float[][] sRaw = readWorldClimTiff(seasonFile);

                int srcW = tRaw[0].length;
                int srcH = tRaw.length;

                for (int y = 0; y < GRID_H; y++) {
                    double lat = 90.0 - (y + 0.5) * 180.0 / GRID_H;
                    int srcY = (int) Math.clamp(((y + 0.5) / GRID_H) * srcH, 0, srcH - 1);

                    for (int x = 0; x < GRID_W; x++) {
                        double lon = -180.0 + (x + 0.5) * 360.0 / GRID_W;
                        int srcX = (int) Math.clamp(((x + 0.5) / GRID_W) * srcW, 0, srcW - 1);

                        // 1. Mean Annual Temperature (°C)
                        float tv = tRaw[srcY][srcX];
                        if (Float.isNaN(tv) || tv < -70.0f || tv > 60.0f) {
                            sstTempBaseline[y][x] = (float) computePhysicalOceanSST(lat, lon);
                        } else {
                            sstTempBaseline[y][x] = tv;
                        }

                        // 2. Annual Precipitation (mm/year)
                        float pv = pRaw[srcY][srcX];
                        if (Float.isNaN(pv) || pv < 0.0f || pv > 35000.0f) {
                            precipBaseline[y][x] = (float) computePhysicalMarinePrecipitation(lat, lon);
                        } else {
                            precipBaseline[y][x] = pv;
                        }

                        // 3. Temperature Seasonality (WorldClim Bio4 = std dev * 100 -> convert to annual amplitude °C)
                        float sv = sRaw[srcY][srcX];
                        if (Float.isNaN(sv) || sv < 0.0f || sv > 50000.0f) {
                            seasonBaseline[y][x] = (float) computePhysicalMarineSeasonality(lat, lon);
                        } else {
                            double rangeC = (sv / 100.0) * 0.28;
                            seasonBaseline[y][x] = (float) Math.clamp(rangeC, 0.5, 65.0);
                        }
                    }
                }
                logger.info("Successfully ingested WorldClim v2.1 empirical datasets into 2048x1024 baseline grids.");
            } catch (Exception e) {
                logger.error("Failed to ingest WorldClim v2.1 GeoTIFFs: {}", e.getMessage(), e);
                initPhysicalFallbacks();
            }
        } else {
            logger.warn("WorldClim GeoTIFF files missing, initializing analytical physical baseline.");
            initPhysicalFallbacks();
        }

        isInitialized = true;
    }

    private static void initPhysicalFallbacks() {
        for (int y = 0; y < GRID_H; y++) {
            double lat = 90.0 - (y + 0.5) * 180.0 / GRID_H;
            for (int x = 0; x < GRID_W; x++) {
                double lon = -180.0 + (x + 0.5) * 360.0 / GRID_W;
                sstTempBaseline[y][x] = (float) computePhysicalOceanSST(lat, lon);
                precipBaseline[y][x]  = (float) computePhysicalMarinePrecipitation(lat, lon);
                seasonBaseline[y][x]  = (float) computePhysicalMarineSeasonality(lat, lon);
            }
        }
    }

    /**
     * Decode WorldClim GeoTIFF Float32 rasters with LZW strip compression.
     */
    public static float[][] readWorldClimTiff(File file) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] header = new byte[8];
            raf.readFully(header);
            boolean isLittleEndian = (header[0] == 'I' && header[1] == 'I');
            long ifdOffset = isLittleEndian
                    ? (header[4] & 0xFFL) | ((header[5] & 0xFFL) << 8) | ((header[6] & 0xFFL) << 16) | ((header[7] & 0xFFL) << 24)
                    : ((header[4] & 0xFFL) << 24) | ((header[5] & 0xFFL) << 16) | ((header[6] & 0xFFL) << 8) | (header[7] & 0xFFL);

            raf.seek(ifdOffset);
            int numEntries = isLittleEndian ? (raf.readUnsignedByte() | (raf.readUnsignedByte() << 8))
                                            : ((raf.readUnsignedByte() << 8) | raf.readUnsignedByte());

            int imgW = 0, imgH = 0, compression = 0;
            long[] stripOffsets = null;
            long[] stripByteCounts = null;

            for (int i = 0; i < numEntries; i++) {
                byte[] entry = new byte[12];
                raf.readFully(entry);
                ByteBuffer bb = ByteBuffer.wrap(entry).order(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
                int tag = bb.getShort() & 0xFFFF;
                int type = bb.getShort() & 0xFFFF;
                long count = bb.getInt() & 0xFFFFFFFFL;
                long valOrOffset = bb.getInt() & 0xFFFFFFFFL;

                if (tag == 256) imgW = (int) valOrOffset;
                else if (tag == 257) imgH = (int) valOrOffset;
                else if (tag == 259) compression = (int) valOrOffset;
                else if (tag == 273) {
                    stripOffsets = new long[(int) count];
                    long saved = raf.getFilePointer();
                    raf.seek(valOrOffset);
                    for (int c = 0; c < count; c++) {
                        if (type == 3) stripOffsets[c] = isLittleEndian ? (raf.readUnsignedByte() | (raf.readUnsignedByte() << 8)) : ((raf.readUnsignedByte() << 8) | raf.readUnsignedByte());
                        else if (type == 4) {
                            byte[] b4 = new byte[4];
                            raf.readFully(b4);
                            stripOffsets[c] = ByteBuffer.wrap(b4).order(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN).getInt() & 0xFFFFFFFFL;
                        }
                    }
                    raf.seek(saved);
                } else if (tag == 279) {
                    stripByteCounts = new long[(int) count];
                    long saved = raf.getFilePointer();
                    raf.seek(valOrOffset);
                    for (int c = 0; c < count; c++) {
                        if (type == 3) stripByteCounts[c] = isLittleEndian ? (raf.readUnsignedByte() | (raf.readUnsignedByte() << 8)) : ((raf.readUnsignedByte() << 8) | raf.readUnsignedByte());
                        else if (type == 4) {
                            byte[] b4 = new byte[4];
                            raf.readFully(b4);
                            stripByteCounts[c] = ByteBuffer.wrap(b4).order(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN).getInt() & 0xFFFFFFFFL;
                        }
                    }
                    raf.seek(saved);
                }
            }

            float[][] grid = new float[imgH][imgW];
            byte[] outRowBytes = new byte[imgW * 4];

            for (int row = 0; row < imgH; row++) {
                long offset = stripOffsets[row];
                int len = (int) stripByteCounts[row];
                byte[] comp = new byte[len];
                raf.seek(offset);
                raf.readFully(comp);

                if (compression == 5) {
                    decodeTiffLzw(comp, outRowBytes);
                } else {
                    System.arraycopy(comp, 0, outRowBytes, 0, Math.min(len, outRowBytes.length));
                }

                ByteBuffer rowBb = ByteBuffer.wrap(outRowBytes).order(ByteOrder.LITTLE_ENDIAN);
                for (int col = 0; col < imgW; col++) {
                    grid[row][col] = rowBb.getFloat();
                }
            }
            return grid;
        }
    }

    private static void decodeTiffLzw(byte[] compressed, byte[] uncompressed) {
        int[][] stringTable = new int[4096][];
        for (int i = 0; i < 256; i++) {
            stringTable[i] = new int[]{i};
        }
        int tableSize = 258;
        int codeSize = 9;

        int bitPos = 0;
        int totalBits = compressed.length * 8;
        int outPos = 0;
        int oldCode = -1;

        while (bitPos + codeSize <= totalBits && outPos < uncompressed.length) {
            int code = getBits(compressed, bitPos, codeSize);
            bitPos += codeSize;

            if (code == 257) break; // EOI
            if (code == 256) { // Clear table
                tableSize = 258;
                codeSize = 9;
                oldCode = -1;
                continue;
            }

            if (oldCode == -1) {
                int[] str = stringTable[code];
                if (str != null) {
                    for (int b : str) if (outPos < uncompressed.length) uncompressed[outPos++] = (byte) b;
                }
                oldCode = code;
            } else {
                int[] str;
                if (code < tableSize && stringTable[code] != null) {
                    str = stringTable[code];
                    for (int b : str) if (outPos < uncompressed.length) uncompressed[outPos++] = (byte) b;

                    int[] newEntry = Arrays.copyOf(stringTable[oldCode], stringTable[oldCode].length + 1);
                    newEntry[newEntry.length - 1] = str[0];
                    if (tableSize < 4096) stringTable[tableSize++] = newEntry;
                } else {
                    int[] oldStr = stringTable[oldCode];
                    int[] newEntry = Arrays.copyOf(oldStr, oldStr.length + 1);
                    newEntry[newEntry.length - 1] = oldStr[0];
                    for (int b : newEntry) if (outPos < uncompressed.length) uncompressed[outPos++] = (byte) b;
                    if (tableSize < 4096) stringTable[tableSize++] = newEntry;
                }
                oldCode = code;
            }

            if (tableSize == (1 << codeSize) - 1 && codeSize < 12) {
                codeSize++;
            }
        }
    }

    private static int getBits(byte[] data, int bitOffset, int bitLength) {
        int bytePos = bitOffset / 8;
        int bitInByte = bitOffset % 8;
        int val = 0;
        int bitsNeeded = bitLength;

        while (bitsNeeded > 0 && bytePos < data.length) {
            int bitsAvailable = 8 - bitInByte;
            int take = Math.min(bitsNeeded, bitsAvailable);
            int mask = (1 << take) - 1;
            int shift = bitsAvailable - take;
            int chunk = ((data[bytePos] & 0xFF) >> shift) & mask;
            val = (val << take) | chunk;
            bitsNeeded -= take;
            bytePos++;
            bitInByte = 0;
        }
        return val;
    }

    /**
     * Compute authentic Sea Surface Temperature (SST) for oceans (°C).
     */
    /**
     * Compute authentic Sea Surface Temperature (SST) for oceans (°C).
     */
    public static double computePhysicalOceanSST(double lat, double lon) {
        double absLat = Math.abs(lat - 6.0); // Thermal equator at 6°N
        double frac = Math.clamp(absLat / 86.0, 0.0, 1.0);
        // Smooth C-infinity cosine glide from tropical peak (28.5°C) to polar sea-ice freezing point (-1.8°C)
        double sst = -1.8 + (28.5 - (-1.8)) * Math.pow(Math.cos(frac * (Math.PI / 2.0)), 1.35);

        // Gulf Stream & North Atlantic Drift (+7.5°C in NE Atlantic)
        double dGulf = Math.exp(-(Math.pow(lat - 55.0, 2) / 220.0 + Math.pow(lon - (-10.0), 2) / 500.0));
        sst += dGulf * 7.5;

        // Kuroshio Current (+4.0°C off Japan)
        double dKuro = Math.exp(-(Math.pow(lat - 35.0, 2) / 140.0 + Math.pow(lon - 142.0, 2) / 300.0));
        sst += dKuro * 4.0;

        // Cold Humboldt / Peru current (-5.5°C)
        double dHumb = Math.exp(-(Math.pow(lat - (-20.0), 2) / 220.0 + Math.pow(lon - (-78.0), 2) / 60.0));
        sst -= dHumb * 5.5;

        // Cold Benguela current (-4.5°C)
        double dBeng = Math.exp(-(Math.pow(lat - (-22.0), 2) / 180.0 + Math.pow(lon - 11.0, 2) / 50.0));
        sst -= dBeng * 4.5;

        // Cold California & Canary currents
        double dCalif = Math.exp(-(Math.pow(lat - 32.0, 2) / 140.0 + Math.pow(lon - (-124.0), 2) / 60.0));
        sst -= dCalif * 4.0;
        double dCanary = Math.exp(-(Math.pow(lat - 25.0, 2) / 140.0 + Math.pow(lon - (-20.0), 2) / 60.0));
        sst -= dCanary * 3.5;

        return Math.clamp(sst, -1.8, 31.0);
    }

    /**
     * Compute realistic marine precipitation over oceans (mm/year).
     */
    public static double computePhysicalMarinePrecipitation(double lat, double lon) {
        double radLon = Math.toRadians(lon);
        double itczLat = 6.0 + 3.0 * Math.sin(radLon * 2.0 + 0.5) + 2.0 * Math.cos(radLon * 3.0);
        double dItcz = Math.abs(lat - itczLat);

        // Narrow equatorial ITCZ band (2200 mm/yr)
        double itczRain = 2200.0 * Math.exp(-(dItcz * dItcz) / 60.0);

        // Subtropical high pressure dry belts (15° to 30° latitude) - down to 100 mm/yr
        double dryNorth = Math.exp(-Math.pow(lat - 24.0, 2) / 55.0);
        double drySouth = Math.exp(-Math.pow(lat - (-22.0), 2) / 50.0);
        double baseRain = (itczRain + 450.0) * (1.0 - Math.max(dryNorth, drySouth) * 0.82);

        // Mid-latitude oceanic storm tracks (40° to 60° N/S)
        double stormNorth = Math.exp(-Math.pow(lat - 50.0, 2) / 75.0) * 1150.0;
        double stormSouth = Math.exp(-Math.pow(lat - (-50.0), 2) / 70.0) * 1350.0;

        double total = baseRain + stormNorth + stormSouth;

        // Desiccated cold current marine upwellings (SE Pacific & South Atlantic)
        double dAtacamaOcean = Math.exp(-(Math.pow(lat - (-20.0), 2) / 100.0 + Math.pow(lon - (-85.0), 2) / 120.0));
        total *= (1.0 - dAtacamaOcean * 0.85);

        double dNamibOcean = Math.exp(-(Math.pow(lat - (-22.0), 2) / 90.0 + Math.pow(lon - 5.0, 2) / 90.0));
        total *= (1.0 - dNamibOcean * 0.85);

        // Smooth polar decay
        if (Math.abs(lat) > 55.0) {
            double pPolar = Math.exp(-Math.pow((Math.abs(lat) - 55.0) / 16.0, 2));
            total = 60.0 + (total - 60.0) * pPolar;
        }

        return Math.clamp(total, 50.0, 3500.0);
    }

    /**
     * Compute realistic marine seasonality (°C annual range).
     */
    public static double computePhysicalMarineSeasonality(double lat, double lon) {
        double absLat = Math.abs(lat);
        double oceanRange = 2.2 + (absLat / 90.0) * 5.8;
        return Math.clamp(oceanRange, 1.5, 9.0);
    }

    /**
     * Sample baseline empirical temperature with paleoclimatic delta anomaly (°C).
     */
    public static double getTemperature(double lat, double lon, double elevM, long year) {
        ensureInitialized();
        double baseT = sampleBilinear(sstTempBaseline, lat, lon);
        double deltaT = computePaleoTemperatureDelta(lat, lon, elevM, year);
        return Math.clamp(baseT + deltaT, -60.0, 50.0);
    }

    /**
     * Sample baseline empirical precipitation with paleoclimatic delta anomaly (mm/year).
     */
    public static double getPrecipitation(double lat, double lon, double elevM, long year) {
        ensureInitialized();
        double baseP = sampleBilinear(precipBaseline, lat, lon);
        double factorP = computePaleoPrecipitationFactor(lat, lon, elevM, year);
        double deltaP = computePaleoPrecipitationDelta(lat, lon, elevM, year);
        return Math.clamp(baseP * factorP + deltaP, 0.0, 4000.0);
    }

    /**
     * Sample baseline empirical seasonality with paleoclimatic delta anomaly (°C range).
     */
    public static double getSeasonality(double lat, double lon, double elevM, long year) {
        ensureInitialized();
        double baseS = sampleBilinear(seasonBaseline, lat, lon);
        double factorS = computePaleoSeasonalityFactor(year);
        return Math.clamp(baseS * factorS, 0.5, 65.0);
    }

    private static double sampleBilinear(float[][] grid, double lat, double lon) {
        double normX = (lon + 180.0) / 360.0 * GRID_W - 0.5;
        double normY = (90.0 - lat) / 180.0 * GRID_H - 0.5;

        int x0 = (int) Math.floor(normX);
        int y0 = (int) Math.floor(normY);
        int x1 = x0 + 1;
        int y1 = y0 + 1;

        double fx = normX - x0;
        double fy = normY - y0;

        x0 = ((x0 % GRID_W) + GRID_W) % GRID_W;
        x1 = ((x1 % GRID_W) + GRID_W) % GRID_W;
        y0 = Math.clamp(y0, 0, GRID_H - 1);
        y1 = Math.clamp(y1, 0, GRID_H - 1);

        float v00 = grid[y0][x0];
        float v10 = grid[y0][x1];
        float v01 = grid[y1][x0];
        float v11 = grid[y1][x1];

        return (1.0 - fx) * (1.0 - fy) * v00 +
               fx * (1.0 - fy) * v10 +
               (1.0 - fx) * fy * v01 +
               fx * fy * v11;
    }

    // =========================================================================
    // PALEOCLIMATIC DELTA-ANOMALY MODELS (PMIP4 / TraCE-21ka / EPICA delta18O)
    // =========================================================================

    public static double computePaleoTemperatureDelta(double lat, double lon, double elevM, long year) {
        if (year >= 1950) return 0.0; // Modern baseline

        double absLat = Math.abs(lat);
        double radLat = Math.toRadians(lat);
        double sinLat = Math.sin(radLat);

        // Global mean temperature anomaly relative to pre-industrial (EPICA / NGRIP delta18O)
        double globalAnom;
        if (year <= -70000L) {
            globalAnom = -1.8;
        } else if (year <= -40000L) {
            globalAnom = -3.8;
        } else if (year <= -18000L) {
            globalAnom = -5.8;
        } else if (year <= -10500L) {
            globalAnom = -2.2;
        } else if (year <= -5000L) {
            globalAnom = 0.6; // Mid-Holocene thermal maximum
        } else {
            globalAnom = -0.3; // Neoglacial late Holocene
        }

        // 2D Smooth Polar & Continental Amplification (Smooth continuous spherical function):
        // Polar amplification factor: 0.65 at equator -> 2.0 at poles (continuous smooth sin^2(lat))
        double polarFactor = 0.65 + 1.35 * (sinLat * sinLat);

        // Continental land cooling factor: land cools 35% more than oceans during cold stadials
        double landAmplification = (elevM >= 0.0) ? 1.30 : 0.85;

        double deltaT = globalAnom * polarFactor * landAmplification;

        // Ice sheet albedo & topographic lapse rate cooling over ice domes
        if (year <= -40000L && year > -70000L) {
            double dFenno = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_FENNOSCANDIA_MIS3);
            double dLaur = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_LAURENTIDE_MIS3);
            if (dFenno <= 4.0) deltaT -= 8.0 * Math.exp(-Math.max(0, dFenno) / 2.0);
            if (dLaur <= 4.0) deltaT -= 9.0 * Math.exp(-Math.max(0, dLaur) / 2.0);
        } else if (year <= -18000L) {
            double dLaur = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_LAURENTIDE_LGM);
            double dFenno = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_FENNOSCANDIA_LGM);
            double dAlps = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_ALPS_LGM);
            double dPat = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_PATAGONIA_LGM);

            if (dLaur <= 5.0) deltaT -= 14.0 * Math.exp(-Math.max(0, dLaur) / 2.5);
            if (dFenno <= 5.0) deltaT -= 12.0 * Math.exp(-Math.max(0, dFenno) / 2.5);
            if (dAlps <= 3.0) deltaT -= 7.0 * Math.exp(-Math.max(0, dAlps) / 1.5);
            if (dPat <= 3.0) deltaT -= 6.0 * Math.exp(-Math.max(0, dPat) / 1.5);
        } else if (year <= -10500L) {
            double amocPlume = Math.exp(-(Math.pow(lat - 56.0, 2) / 180.0 + Math.pow(lon - (-15.0), 2) / 350.0));
            double ydLaur = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_LAURENTIDE_YD);
            double ydFenno = HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_FENNOSCANDIA_YD);

            if (ydLaur <= 4.0) deltaT -= 10.0 * Math.exp(-Math.max(0, ydLaur) / 2.0);
            if (ydFenno <= 4.0) deltaT -= 9.0 * Math.exp(-Math.max(0, ydFenno) / 2.0);
            deltaT -= (amocPlume * 7.5);
        }

        return deltaT;
    }

    public static double computePaleoPrecipitationFactor(double lat, double lon, double elevM, long year) {
        if (year >= 1950) return 1.0;

        double absLat = Math.abs(lat);

        if (year <= -18000L && year >= -25000L) {
            return 1.0 - 0.35 * Math.sin(Math.toRadians(absLat));
        } else if (year <= -10500L && year > -12500L) {
            if (lat >= 5.0 && lat <= 35.0 && lon >= 60.0 && lon <= 130.0) {
                return 0.75; // Asian monsoon drought
            }
            return 0.92;
        }
        return 1.0;
    }

    public static double computePaleoPrecipitationDelta(double lat, double lon, double elevM, long year) {
        if (year >= 1950) return 0.0;

        // African Humid Period / Green Sahara & Arabian wet corridor (-100,000 BP & -10,000 to -5,000 BP)
        if (year <= -70000L || (year <= -4500L && year >= -10500L)) {
            double greenSahara = Math.exp(-(Math.pow(lat - 21.0, 2) / 80.0 + Math.pow(lon - 15.0, 2) / 450.0));
            double greenArabia = Math.exp(-(Math.pow(lat - 22.0, 2) / 50.0 + Math.pow(lon - 48.0, 2) / 120.0));
            return (greenSahara * 850.0) + (greenArabia * 550.0);
        }

        return 0.0;
    }

    public static double computePaleoSeasonalityFactor(long year) {
        if (year >= 1950) return 1.0;

        if (year <= -70000L || (year <= -6000L && year >= -11000L)) {
            return 1.12;
        } else if (year <= -18000L && year >= -25000L) {
            return 0.94;
        }
        return 1.0;
    }

    /**
     * Classify holdridge/whittaker ecological biome with glacial ice sheet overrides.
     */
    public static int classifyBiome(double tempC, double precipMm, double elevM, double lat, double lon, long year) {
        // 1. Permanent Polar Ice Sheets & Glaciers
        if (lat < -60.0) return BIOME_GLACIER; // Antarctica
        if (lat > 60.0 && lon > -55.0 && lon < -18.0) return BIOME_GLACIER; // Greenland

        // Historical Glacial Polygons
        if (year <= -40000L && year > -70000L) {
            if (HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_FENNOSCANDIA_MIS3) <= 0 ||
                HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_LAURENTIDE_MIS3) <= 0) {
                return BIOME_GLACIER;
            }
        } else if (year <= -18000L) {
            if (HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_LAURENTIDE_LGM) <= 0 ||
                HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_FENNOSCANDIA_LGM) <= 0 ||
                HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_ALPS_LGM) <= 0 ||
                HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_PATAGONIA_LGM) <= 0) {
                return BIOME_GLACIER;
            }
        } else if (year <= -10500L && year > -18000L) {
            if (HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_LAURENTIDE_YD) <= 0 ||
                HistoricalMapGenerator.signedDistanceToPolygon(lon, lat, HistoricalMapGenerator.POLY_FENNOSCANDIA_YD) <= 0) {
                return BIOME_GLACIER;
            }
        }

        // Mountain Snow & Extreme Peaks
        if (elevM > 5200.0 || (tempC < -12.0 && elevM > 3800.0)) {
            return BIOME_SNOW;
        }
        if (elevM > 3800.0 && tempC < -4.0) {
            return BIOME_MOUNTAINS;
        }

        // Tundra & High Alpine Permafrost
        if (tempC < -2.0) {
            return BIOME_TUNDRA;
        }
        if (elevM > 3200.0 && tempC < 6.0 && precipMm < 600.0) {
            return BIOME_TUNDRA; // Tibetan / Andean Alpine Tundra & Steppe
        }

        // Hills & Rugged Highlands
        if (elevM > 1800.0 && precipMm < 800.0 && tempC < 14.0) {
            return BIOME_HILLS;
        }

        // Temperate & Boreal Zone (tempC < 15.0°C)
        if (tempC < 15.0) {
            if (precipMm < 250.0) {
                return BIOME_DESERT; // Cold continental desert (Gobi, Taklamakan, Patagonia)
            } else if (precipMm < 600.0) {
                return BIOME_PLAINS; // Steppe / Temperate Grassland / Prairie
            } else {
                return BIOME_FOREST; // Boreal Taiga & Temperate Deciduous Forest
            }
        }

        // Subtropical & Tropical Zones (tempC >= 15.0°C)
        if (precipMm < 250.0) {
            return BIOME_DESERT; // Hyper-arid desert (Sahara, Arabia, Namib, Australian Outback)
        } else if (precipMm < 850.0) {
            return BIOME_PLAINS; // Savanna / Sahel / Shrubland / Cerrado
        } else if (tempC >= 19.0 && precipMm >= 1350.0) {
            return BIOME_JUNGLE; // Tropical Rainforest (Amazon, Congo, Sundaland, New Guinea)
        } else {
            return BIOME_FOREST; // Subtropical & Monsoon Deciduous Forest
        }
    }
}
