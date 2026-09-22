package org.ether.society.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Inflater;

/**
 * Direct high-performance reader for official NOAA ETOPO 2022 Global Relief Model GeoTIFFs
 * (tiled 21600x10800 Float32 rasters with Deflate + Floating-Point horizontal predictor).
 * Decodes exact topography (-11000 m to +8848 m) without external library dependencies.
 */
public final class EtopoGeoTiffReader {
    private static final Logger logger = LoggerFactory.getLogger(EtopoGeoTiffReader.class);

    private static final Object CACHE_LOCK = new Object();
    private static volatile float[][] cached2048x1024Grid = null;

    private EtopoGeoTiffReader() {}

    /**
     * Reads and samples ETOPO 2022 to target grid [height][width] in meters.
     */
    public static float[][] loadEtopoGrid(int targetW, int targetH) {
        if (targetW == 2048 && targetH == 1024 && cached2048x1024Grid != null) {
            return cached2048x1024Grid;
        }

        synchronized (CACHE_LOCK) {
            if (targetW == 2048 && targetH == 1024 && cached2048x1024Grid != null) {
                return cached2048x1024Grid;
            }

            File etopoFile = new File("data/maps/usgs/ETOPO_2022_v1_60s_N90W180_bed.tif");
            if (!etopoFile.exists()) {
                etopoFile = new File("data/maps/usgs/ETOPO_2022_v1_60s_N90W180_surface.tif");
            }
            if (!etopoFile.exists()) {
                logger.warn("NOAA ETOPO 2022 GeoTIFF file not found in data/maps/usgs/.");
                return new float[targetH][targetW];
            }

            long t0 = System.currentTimeMillis();
            float[][] grid = new float[targetH][targetW];

            try (RandomAccessFile raf = new RandomAccessFile(etopoFile, "r")) {
                byte[] header = new byte[8];
                raf.readFully(header);
                boolean isLittleEndian = (header[0] == 'I' && header[1] == 'I');

                long ifdOffset = isLittleEndian
                        ? (header[4] & 0xFFL) | ((header[5] & 0xFFL) << 8) | ((header[6] & 0xFFL) << 16) | ((header[7] & 0xFFL) << 24)
                        : ((header[4] & 0xFFL) << 24) | ((header[5] & 0xFFL) << 16) | ((header[6] & 0xFFL) << 8) | (header[7] & 0xFFL);

                raf.seek(ifdOffset);
                int numEntries = isLittleEndian ? (raf.readUnsignedByte() | (raf.readUnsignedByte() << 8))
                                                : ((raf.readUnsignedByte() << 8) | raf.readUnsignedByte());

                int imgW = 0, imgH = 0, tileW = 0, tileH = 0, compression = 0, predictor = 1;
                long[] tileOffsets = null;
                long[] tileByteCounts = null;

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
                    else if (tag == 317) predictor = (int) valOrOffset;
                    else if (tag == 322) tileW = (int) valOrOffset;
                    else if (tag == 323) tileH = (int) valOrOffset;
                    else if (tag == 324) {
                        tileOffsets = new long[(int) count];
                        long savedPos = raf.getFilePointer();
                        raf.seek(valOrOffset);
                        for (int c = 0; c < count; c++) {
                            if (type == 3) {
                                tileOffsets[c] = isLittleEndian ? (raf.readUnsignedByte() | (raf.readUnsignedByte() << 8))
                                                                : ((raf.readUnsignedByte() << 8) | raf.readUnsignedByte());
                            } else if (type == 4) {
                                byte[] buf4 = new byte[4];
                                raf.readFully(buf4);
                                tileOffsets[c] = ByteBuffer.wrap(buf4).order(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN).getInt() & 0xFFFFFFFFL;
                            }
                        }
                        raf.seek(savedPos);
                    } else if (tag == 325) {
                        tileByteCounts = new long[(int) count];
                        long savedPos = raf.getFilePointer();
                        raf.seek(valOrOffset);
                        for (int c = 0; c < count; c++) {
                            if (type == 3) {
                                tileByteCounts[c] = isLittleEndian ? (raf.readUnsignedByte() | (raf.readUnsignedByte() << 8))
                                                                   : ((raf.readUnsignedByte() << 8) | raf.readUnsignedByte());
                            } else if (type == 4) {
                                byte[] buf4 = new byte[4];
                                raf.readFully(buf4);
                                tileByteCounts[c] = ByteBuffer.wrap(buf4).order(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN).getInt() & 0xFFFFFFFFL;
                            }
                        }
                        raf.seek(savedPos);
                    }
                }

                if (tileOffsets == null || tileByteCounts == null || imgW <= 0 || imgH <= 0 || tileW <= 0 || tileH <= 0) {
                    logger.warn("Corrupted or unsupported ETOPO TIFF structure.");
                    return grid;
                }

                int numTilesX = (imgW + tileW - 1) / tileW;
                int numTilesY = (imgH + tileH - 1) / tileH;

                byte[] compBuffer = new byte[tileW * tileH * 8];
                byte[] decompBuffer = new byte[tileW * tileH * 4];
                float[] tileFloats = new float[tileW * tileH];
                Inflater inflater = new Inflater();
                int bytesPerRow = tileW * 4;

                for (int ty = 0; ty < numTilesY; ty++) {
                    for (int tx = 0; tx < numTilesX; tx++) {
                        int tileIdx = ty * numTilesX + tx;
                        long offset = tileOffsets[tileIdx];
                        int byteCount = (int) tileByteCounts[tileIdx];

                        raf.seek(offset);
                        raf.readFully(compBuffer, 0, byteCount);

                        int decompSize = tileW * tileH * 4;
                        if (compression == 8) {
                            inflater.reset();
                            inflater.setInput(compBuffer, 0, byteCount);
                            inflater.inflate(decompBuffer, 0, decompSize);
                        } else {
                            System.arraycopy(compBuffer, 0, decompBuffer, 0, byteCount);
                        }

                        for (int row = 0; row < tileH; row++) {
                            int rowOffset = row * bytesPerRow;
                            if (predictor == 3) {
                                for (int p = 0; p < 4; p++) {
                                    int planeOffset = rowOffset + p * tileW;
                                    for (int col = 1; col < tileW; col++) {
                                        decompBuffer[planeOffset + col] = (byte) ((decompBuffer[planeOffset + col] & 0xFF) + (decompBuffer[planeOffset + col - 1] & 0xFF));
                                    }
                                }
                                for (int col = 0; col < tileW; col++) {
                                    byte b0 = decompBuffer[rowOffset + 0 * tileW + col];
                                    byte b1 = decompBuffer[rowOffset + 1 * tileW + col];
                                    byte b2 = decompBuffer[rowOffset + 2 * tileW + col];
                                    byte b3 = decompBuffer[rowOffset + 3 * tileW + col];
                                    int bits = ((b0 & 0xFF) << 24) | ((b1 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b3 & 0xFF);
                                    tileFloats[row * tileW + col] = Float.intBitsToFloat(bits);
                                }
                            } else {
                                for (int col = 0; col < tileW; col++) {
                                    int idx = rowOffset + col * 4;
                                    byte b0 = decompBuffer[idx];
                                    byte b1 = decompBuffer[idx + 1];
                                    byte b2 = decompBuffer[idx + 2];
                                    byte b3 = decompBuffer[idx + 3];
                                    int bits = isLittleEndian
                                            ? ((b0 & 0xFF) | ((b1 & 0xFF) << 8) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 24))
                                            : (((b0 & 0xFF) << 24) | ((b1 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b3 & 0xFF));
                                    tileFloats[row * tileW + col] = Float.intBitsToFloat(bits);
                                }
                            }
                        }

                        int minX = tx * tileW;
                        int minY = ty * tileH;
                        int maxX = Math.min(minX + tileW, imgW);
                        int maxY = Math.min(minY + tileH, imgH);

                        int startGX = (int) Math.floor((minX / (double) imgW) * targetW);
                        int endGX = (int) Math.ceil((maxX / (double) imgW) * targetW);
                        int startGY = (int) Math.floor((minY / (double) imgH) * targetH);
                        int endGY = (int) Math.ceil((maxY / (double) imgH) * targetH);

                        for (int gy = startGY; gy < endGY && gy < targetH; gy++) {
                            double lat = 90.0 - (gy + 0.5) / (double) targetH * 180.0;
                            int py = (int) Math.clamp(((90.0 - lat) / 180.0) * (imgH - 1), 0, imgH - 1);
                            if (py < minY || py >= maxY) continue;
                            int tileY = py - minY;

                            for (int gx = startGX; gx < endGX && gx < targetW; gx++) {
                                double lon = -180.0 + (gx + 0.5) / (double) targetW * 360.0;
                                int px = (int) Math.clamp(((lon + 180.0) / 360.0) * (imgW - 1), 0, imgW - 1);
                                if (px < minX || px >= maxX) continue;
                                int tileX = px - minX;

                                grid[gy][gx] = tileFloats[tileY * tileW + tileX];
                            }
                        }
                    }
                }

                logger.info("Direct NOAA ETOPO 2022 relief model ({}x{}) sampled to {}x{} in {} ms.",
                        imgW, imgH, targetW, targetH, (System.currentTimeMillis() - t0));

                if (targetW == 2048 && targetH == 1024) {
                    cached2048x1024Grid = grid;
                }
                return grid;
            } catch (Exception e) {
                logger.error("Error reading NOAA ETOPO 2022 GeoTIFF: {}", e.getMessage(), e);
                return grid;
            }
        }
    }

    /**
     * Renders pure grayscale elevation image [0..255] where:
     * - Bathymetry [-11000m .. 0m] -> [0 .. 122]
     * - Topography [0m .. +8848m] -> [122 .. 255]
     * Sea level (0m) is exactly 122 (normalized 0.478).
     */
    public static BufferedImage renderGrayscaleElevationImage(float[][] etopoGrid, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float altM = etopoGrid[y][x];
                double norm;
                if (altM <= 0.0f) {
                    norm = 0.478 * (1.0 + Math.max(-11000.0f, altM) / 11000.0);
                } else {
                    norm = 0.478 + 0.522 * Math.min(1.0, altM / 8848.0);
                }
                int gray = (int) Math.clamp(norm * 255.0, 0.0, 255.0);
                img.getRaster().setSample(x, y, 0, gray);
            }
        }
        return img;
    }

    /**
     * Creates a binary land/ocean mask (255=land, 0=ocean) based on elevation relative to sea level.
     * @param seaLevelOffsetMeters Sea level relative to present day (e.g. 0.0 for present, -20.0 to -120.0 for ice ages).
     */
    public static BufferedImage createLandMask(float[][] etopoGrid, int width, int height, double seaLevelOffsetMeters) {
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isLand = (etopoGrid[y][x] >= seaLevelOffsetMeters);
                mask.getRaster().setSample(x, y, 0, isLand ? 255 : 0);
            }
        }
        return mask;
    }
}
