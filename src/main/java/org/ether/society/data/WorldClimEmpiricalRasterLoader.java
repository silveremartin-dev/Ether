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
 * @version 1.0.0-beta.2
 */
public class WorldClimEmpiricalRasterLoader {
    private static final Logger logger = LoggerFactory.getLogger(WorldClimEmpiricalRasterLoader.class);

    /** Internal state: grid w (int). */
    public static final int GRID_W = 2048;
    /** Internal state: grid h (int). */
    public static final int GRID_H = 1024;

    private static float[][] sstTempBaseline;
    private static float[][] precipBaseline;
    private static float[][] seasonBaseline;
    /** Internal state: is initialized (boolean). */
    private static boolean isInitialized = false;

    // Biome Color Constants matching Ether Palette
    /** Internal state: biome deep ocean (int). */
    public static final int BIOME_DEEP_OCEAN = 0x172554;
    /** Internal state: biome ocean (int). */
    public static final int BIOME_OCEAN      = 0x1E3A8A;
    /** Internal state: biome glacier (int). */
    public static final int BIOME_GLACIER    = 0xF8FAFC;
    /** Internal state: biome snow (int). */
    public static final int BIOME_SNOW       = 0xE2E8F0;
    /** Internal state: biome tundra (int). */
    public static final int BIOME_TUNDRA     = 0x94A3B8;
    /** Internal state: biome forest (int). */
    public static final int BIOME_FOREST     = 0x15803D;
    /** Internal state: biome jungle (int). */
    public static final int BIOME_JUNGLE     = 0x14532D;
    /** Internal state: biome plains (int). */
    public static final int BIOME_PLAINS     = 0x84CC16;
    /** Internal state: biome desert (int). */
    public static final int BIOME_DESERT     = 0xEAB308;
    /** Internal state: biome hills (int). */
    public static final int BIOME_HILLS      = 0x78716C;
    /** Internal state: biome mountains (int). */
    public static final int BIOME_MOUNTAINS  = 0x475569;

    /**
     * Ensure initialized.
     *
     */
    public static synchronized void ensureInitialized() {
        if (isInitialized) return;

        sstTempBaseline = new float[GRID_H][GRID_W];
        precipBaseline  = new float[GRID_H][GRID_W];
        seasonBaseline  = new float[GRID_H][GRID_W];

        File zip5m = org.ether.society.config.EtherPaths.resolveSourceFile("worldclim/wc2.1_5m_bio.zip");
        File tempFile = org.ether.society.config.EtherPaths.resolveSourceFile("worldclim/bio_10m/wc2.1_10m_bio_1.tif");
        File precipFile = org.ether.society.config.EtherPaths.resolveSourceFile("worldclim/bio_10m/wc2.1_10m_bio_12.tif");
        File seasonFile = org.ether.society.config.EtherPaths.resolveSourceFile("worldclim/bio_10m/wc2.1_10m_bio_4.tif");

        boolean has5mZip = zip5m.exists() && zip5m.isFile();
        boolean has10m = tempFile.exists() && precipFile.exists() && seasonFile.exists();

        if (has5mZip || has10m) {
            try {
                float[][] tRaw = null, pRaw = null, sRaw = null;
                if (has5mZip) {
                    logger.info("Loading empirical WorldClim v2.1 5-arc-minute (4320x2160) GeoTIFF rasters from '{}'...", zip5m.getName());
                    try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zip5m)) {
                        tRaw = readWorldClimTiffFromZip(zf, "wc2.1_5m_bio_1.tif");
                        pRaw = readWorldClimTiffFromZip(zf, "wc2.1_5m_bio_12.tif");
                        sRaw = readWorldClimTiffFromZip(zf, "wc2.1_5m_bio_4.tif");
                    }
                }
                if ((tRaw == null || pRaw == null || sRaw == null) && has10m) {
                    logger.info("Loading empirical WorldClim v2.1 10-arc-minute GeoTIFF rasters via native TIFF LZW decoder...");
                    tRaw = readWorldClimTiff(tempFile);
                    pRaw = readWorldClimTiff(precipFile);
                    sRaw = readWorldClimTiff(seasonFile);
                }
                if (tRaw == null || pRaw == null || sRaw == null) {
                    throw new IllegalStateException("WorldClim GeoTIFF streams incomplete");
                }

                int srcW = tRaw[0].length;
                int srcH = tRaw.length;

                float[][] tResampled = ConservativeRasterResampler.resampleIntensiveMasked(
                        tRaw, srcW, srcH, GRID_W, GRID_H, tv -> tv >= -70.0f && tv <= 60.0f, Float.NaN);
                float[][] pResampled = ConservativeRasterResampler.resampleIntensiveMasked(
                        pRaw, srcW, srcH, GRID_W, GRID_H, pv -> pv >= 0.0f && pv <= 35000.0f, Float.NaN);
                float[][] sResampled = ConservativeRasterResampler.resampleIntensiveMasked(
                        sRaw, srcW, srcH, GRID_W, GRID_H, sv -> sv >= 0.0f && sv <= 50000.0f, Float.NaN);

                for (int y = 0; y < GRID_H; y++) {
                    double lat = 90.0 - (y + 0.5) * 180.0 / GRID_H;
                    for (int x = 0; x < GRID_W; x++) {
                        double lon = -180.0 + (x + 0.5) * 360.0 / GRID_W;

                        float tv = tResampled[y][x];
                        sstTempBaseline[y][x] = Float.isNaN(tv)
                                ? (float) computePhysicalOceanSST(lat, lon)
                                : tv;

                        float pv = pResampled[y][x];
                        precipBaseline[y][x] = Float.isNaN(pv)
                                ? (float) computePhysicalMarinePrecipitation(lat, lon)
                                : pv;

                        // Temperature Seasonality (WorldClim Bio4 = std dev * 100 -> peak-to-peak amplitude A°C = 2 * sqrt(2) * SD)
                        float sv = sResampled[y][x];
                        seasonBaseline[y][x] = Float.isNaN(sv)
                                ? (float) computePhysicalMarineSeasonality(lat, lon)
                                : (float) Math.clamp((sv / 100.0) * 2.8284, 1.5, 65.0);
                    }
                }
                logger.info("Successfully ingested WorldClim v2.1 empirical datasets via conservative spherical regridding into 2048x1024 baseline grids.");
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

    private static float[][] readWorldClimTiffFromZip(java.util.zip.ZipFile zf, String entryName) throws Exception {
        java.util.zip.ZipEntry entry = zf.getEntry(entryName);
        if (entry == null) return null;
        try (java.io.InputStream is = zf.getInputStream(entry)) {
            return readWorldClimTiffFromBytes(is.readAllBytes());
        }
    }

    /**
     * Decode WorldClim GeoTIFF Float32 rasters with LZW strip compression from disk file.
     */
    public static float[][] readWorldClimTiff(File file) throws Exception {
        return readWorldClimTiffFromBytes(java.nio.file.Files.readAllBytes(file.toPath()));
    }

    /**
     * Decode WorldClim GeoTIFF Float32 rasters with LZW strip compression from in-memory bytes.
     */
    public static float[][] readWorldClimTiffFromBytes(byte[] bytes) throws Exception {
        boolean isLittleEndian = (bytes[0] == 'I' && bytes[1] == 'I');
        ByteOrder order = isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        ByteBuffer full = ByteBuffer.wrap(bytes).order(order);

        int ifdOffset = full.getInt(4);
        int numEntries = full.getShort(ifdOffset) & 0xFFFF;

        int imgW = 0, imgH = 0, compression = 0;
        long[] stripOffsets = null;
        long[] stripByteCounts = null;

        for (int i = 0; i < numEntries; i++) {
            int base = ifdOffset + 2 + i * 12;
            int tag = full.getShort(base) & 0xFFFF;
            int type = full.getShort(base + 2) & 0xFFFF;
            int count = full.getInt(base + 4);
            int valOrOffset = full.getInt(base + 8);

            if (tag == 256) imgW = valOrOffset;
            else if (tag == 257) imgH = valOrOffset;
            else if (tag == 259) compression = valOrOffset;
            else if (tag == 273) {
                stripOffsets = readTiffIntArray(full, type, count, valOrOffset);
            } else if (tag == 279) {
                stripByteCounts = readTiffIntArray(full, type, count, valOrOffset);
            }
        }

        float[][] grid = new float[imgH][imgW];
        byte[] outRowBytes = new byte[imgW * 4];
        ByteBuffer rowBb = ByteBuffer.wrap(outRowBytes).order(order);

        for (int row = 0; row < imgH; row++) {
            int offset = (int) stripOffsets[row];
            int len = (int) stripByteCounts[row];
            if (compression == 5) {
                byte[] comp = Arrays.copyOfRange(bytes, offset, offset + len);
                decodeTiffLzw(comp, outRowBytes);
            } else {
                System.arraycopy(bytes, offset, outRowBytes, 0, Math.min(len, outRowBytes.length));
            }
            rowBb.rewind();
            float[] dstRow = grid[row];
            for (int col = 0; col < imgW; col++) {
                dstRow[col] = rowBb.getFloat();
            }
        }
        return grid;
    }

    private static long[] readTiffIntArray(ByteBuffer full, int type, int count, int offset) {
        long[] arr = new long[count];
        for (int c = 0; c < count; c++) {
            if (type == 3) {
                arr[c] = full.getShort(offset + c * 2) & 0xFFFFL;
            } else if (type == 4) {
                arr[c] = full.getInt(offset + c * 4) & 0xFFFFFFFFL;
            }
        }
        return arr;
    }

    private static void decodeTiffLzw(byte[] compressed, byte[] uncompressed) {
        TiffLzwDecoder.decodeTiffLzw(compressed, uncompressed);
    }

    /**
     * Compute authentic Sea Surface Temperature (SST) for oceans (Â°C).
     */
    public static double computePhysicalOceanSST(double lat, double lon) {
        return MarineClimatePhysics.computePhysicalOceanSST(lat, lon);
    }

    public static double computePhysicalMarinePrecipitation(double lat, double lon) {
        return MarineClimatePhysics.computePhysicalMarinePrecipitation(lat, lon);
    }

    public static double computePhysicalMarineSeasonality(double lat, double lon) {
        return MarineClimatePhysics.computePhysicalMarineSeasonality(lat, lon);
    }

    /**
     * Sample baseline empirical temperature with paleoclimatic delta anomaly (Â°C).
     */
    public static double getTemperature(double lat, double lon, double elevM, long year) {
        ensureInitialized();
        double baseT = sampleBilinear(sstTempBaseline, lat, lon);
        double deltaT = computePaleoTemperatureDelta(lat, lon, elevM, year);

        // Bowen ratio: Sensible heat dominance over hyper-arid cloudless deserts vs. evaporative cooling over dense tropical rainforests
        double sensibleDesertBoost = 0.0;
        // Sahara & Arabian Peninsula Core
        if (lat >= 14.0 && lat <= 32.0 && lon >= -16.0 && lon <= 58.0) {
            double dSahara = Math.exp(-(Math.pow(lat - 23.0, 2) / 60.0 + Math.pow(lon - 20.0, 2) / 500.0));
            double dArabia = Math.exp(-(Math.pow(lat - 22.0, 2) / 45.0 + Math.pow(lon - 48.0, 2) / 100.0));
            sensibleDesertBoost += (dSahara * 6.5) + (dArabia * 5.8);
        }
        // Iranian Lut Desert & Thar Desert
        if (lat >= 24.0 && lat <= 36.0 && lon >= 54.0 && lon <= 76.0) {
            double dLut = Math.exp(-(Math.pow(lat - 30.0, 2) / 35.0 + Math.pow(lon - 62.0, 2) / 90.0));
            sensibleDesertBoost += (dLut * 5.0);
        }
        // Evaporative Canopy Cooling & Cloud Albedo in Dense Tropical Rainforests
        double evaporativeCooling = 0.0;
        if (lat >= -10.0 && lat <= 6.0 && lon >= -78.0 && lon <= -48.0) { // Amazon
            double dAmazon = Math.exp(-(Math.pow(lat - (-2.0), 2) / 45.0 + Math.pow(lon - (-63.0), 2) / 180.0));
            evaporativeCooling -= (dAmazon * 4.0);
        }
        if (lat >= -6.0 && lat <= 6.0 && lon >= 10.0 && lon <= 30.0) { // Congo
            double dCongo = Math.exp(-(Math.pow(lat - 0.0, 2) / 40.0 + Math.pow(lon - 20.0, 2) / 90.0));
            evaporativeCooling -= (dCongo * 3.5);
        }

        return Math.clamp(baseT + deltaT + sensibleDesertBoost + evaporativeCooling, -60.0, 55.0);
    }

    /**
     * Sample baseline empirical precipitation with paleoclimatic delta anomaly (mm/year).
     */
    public static double getPrecipitation(double lat, double lon, double elevM, long year) {
        ensureInitialized();
        double baseP = sampleBilinear(precipBaseline, lat, lon);

        // 1. Indian & Indochina Monsoon windward orographic lifting (Western Ghats, Meghalaya/Cherrapunji, Assam)
        if (lat >= 8.0 && lat <= 30.0 && lon >= 68.0 && lon <= 105.0) {
            // Western Ghats convective marine lifting
            double dGhats = Math.exp(-(Math.pow(lat - 14.0, 2) / 35.0 + Math.pow(lon - 74.5, 2) / 10.0));
            // Cherrapunji / Meghalaya funneling against Himalayan southern foothills
            double dCherra = Math.exp(-(Math.pow(lat - 25.3, 2) / 12.0 + Math.pow(lon - 91.7, 2) / 20.0));
            // Indo-Gangetic floodplain monsoon corridor
            double dIndoGangetic = Math.exp(-(Math.pow(lat - 24.5, 2) / 35.0 + Math.pow(lon - 85.0, 2) / 60.0));
            // Suppress monsoon boost if high above the crest on Tibetan Plateau side
            double slopeAtten = (lat > 28.0 && elevM > 3500.0) ? 0.15 : 1.0;
            baseP += ((dGhats * 1800.0) + (dCherra * 3200.0) + (dIndoGangetic * 800.0)) * slopeAtten;
        }

        // 2. High Tibetan Plateau & Central Asian / Tarim Basin extreme alpine rain shadow
        if (lat >= 28.0 && lat <= 48.0 && lon >= 75.0 && lon <= 105.0) {
            // Tibetan Plateau alpine rain shadow (leeward of Himalayas)
            if (elevM > 3200.0 && lat >= 29.0 && lat <= 36.5 && lon >= 78.0 && lon <= 100.0) {
                baseP *= 0.22; // Extreme high-altitude continental desiccation
            }
            // Tarim Basin / Taklamakan dry depression
            double dTarim = Math.exp(-(Math.pow(lat - 40.0, 2) / 30.0 + Math.pow(lon - 86.0, 2) / 90.0));
            baseP *= (1.0 - dTarim * 0.80);
        }

        // 3. Atacama Desert & Coastal Peru Humboldt rain shadow (blocked by the High Andes)
        if (lat >= -30.0 && lat <= -14.0 && lon >= -76.0 && lon <= -67.0) {
            double dAtacama = Math.exp(-(Math.pow(lat - (-22.5), 2) / 40.0 + Math.pow(lon - (-70.0), 2) / 8.0));
            baseP *= (1.0 - dAtacama * 0.90);
        }

        // 4. North American Great Basin & Colorado Plateau rain shadow (behind Sierra Nevada & Cascades)
        if (lat >= 34.0 && lat <= 45.0 && lon >= -121.0 && lon <= -110.0) {
            double dGreatBasin = Math.exp(-(Math.pow(lat - 39.5, 2) / 30.0 + Math.pow(lon - (-116.0), 2) / 30.0));
            baseP *= (1.0 - dGreatBasin * 0.70);
        }

        // 5. Patagonian rain shadow (east of Southern Andes)
        if (lat >= -52.0 && lat <= -38.0 && lon >= -72.0 && lon <= -64.0) {
            double dPatagonia = Math.exp(-(Math.pow(lat - (-45.0), 2) / 40.0 + Math.pow(lon - (-68.0), 2) / 20.0));
            baseP *= (1.0 - dPatagonia * 0.75);
        }

        double factorP = computePaleoPrecipitationFactor(lat, lon, elevM, year);
        double deltaP = computePaleoPrecipitationDelta(lat, lon, elevM, year);
        return Math.clamp(baseP * factorP + deltaP, 0.0, 4500.0);
    }

    /**
     * Sample baseline empirical seasonality with paleoclimatic delta anomaly (Â°C range).
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

    /**
     * Compute paleo temperature delta.
     *
     * @param lat the lat parameter (double)
     * @param lon the lon parameter (double)
     * @param elevM the elev m parameter (double)
     * @param year the year parameter (long)
     * @return the resulting computation or state reference
     */
    public static double computePaleoTemperatureDelta(double lat, double lon, double elevM, long year) {
        return PaleoClimateAnomalyModel.computePaleoTemperatureDelta(lat, lon, elevM, year);
    }

    public static double computePaleoPrecipitationFactor(double lat, double lon, double elevM, long year) {
        return PaleoClimateAnomalyModel.computePaleoPrecipitationFactor(lat, lon, elevM, year);
    }

    public static double computePaleoPrecipitationDelta(double lat, double lon, double elevM, long year) {
        return PaleoClimateAnomalyModel.computePaleoPrecipitationDelta(lat, lon, elevM, year);
    }

    public static double computePaleoSeasonalityFactor(long year) {
        return PaleoClimateAnomalyModel.computePaleoSeasonalityFactor(year);
    }

    public static int classifyBiome(double tempC, double precipMm, double elevM, double lat, double lon, long year) {
        return EmpiricalBiomeClassifier.classifyBiome(tempC, precipMm, elevM, lat, lon, year);
    }
}
