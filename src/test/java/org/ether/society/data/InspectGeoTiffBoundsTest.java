package org.ether.society.data;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class InspectGeoTiffBoundsTest {
    @Test
    public void inspectPaleoClimGeoTiffBounds() throws Exception {
        String[] files = {
            "data/maps/paleoclim/LIG_v1_2_5m/bio_1.tif",
            "data/maps/paleoclim/CHELSA_cur_V1_2B_r2_5m/2_5min/bio_1.tif"
        };
        for (String p : files) {
            File f = new File(p);
            if (!f.exists()) continue;
            System.out.println("=== " + p + " ===");
            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r")) {
                byte[] header = new byte[8];
                raf.readFully(header);
                boolean isLE = (header[0] == 'I');
                long ifd = isLE ? (header[4]&0xFFL)|((header[5]&0xFFL)<<8)|((header[6]&0xFFL)<<16)|((header[7]&0xFFL)<<24)
                                : ((header[4]&0xFFL)<<24)|((header[5]&0xFFL)<<16)|((header[6]&0xFFL)<<8)|(header[7]&0xFFL);
                raf.seek(ifd);
                int num = isLE ? (raf.readUnsignedByte() | (raf.readUnsignedByte()<<8)) : ((raf.readUnsignedByte()<<8)|raf.readUnsignedByte());
                for (int i = 0; i < num; i++) {
                    byte[] entry = new byte[12];
                    raf.readFully(entry);
                    java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(entry).order(isLE ? java.nio.ByteOrder.LITTLE_ENDIAN : java.nio.ByteOrder.BIG_ENDIAN);
                    int tag = bb.getShort() & 0xFFFF;
                    int type = bb.getShort() & 0xFFFF;
                    long count = bb.getInt() & 0xFFFFFFFFL;
                    long valOrOffset = bb.getInt() & 0xFFFFFFFFL;
                    if (tag == 256) System.out.println("  ImageWidth: " + valOrOffset);
                    if (tag == 257) System.out.println("  ImageHeight: " + valOrOffset);
                    if (tag == 33550) { // ModelPixelScaleTag (3 doubles: scaleX, scaleY, scaleZ)
                        long pos = raf.getFilePointer();
                        raf.seek(valOrOffset);
                        byte[] dBuf = new byte[(int)count * 8];
                        raf.readFully(dBuf);
                        java.nio.ByteBuffer dbb = java.nio.ByteBuffer.wrap(dBuf).order(isLE ? java.nio.ByteOrder.LITTLE_ENDIAN : java.nio.ByteOrder.BIG_ENDIAN);
                        System.out.printf("  PixelScale: (dx=%.8f, dy=%.8f, dz=%.8f)%n", dbb.getDouble(), dbb.getDouble(), dbb.getDouble());
                        raf.seek(pos);
                    }
                    if (tag == 33922) { // ModelTiepointTag (6 doubles: i, j, k, x, y, z)
                        long pos = raf.getFilePointer();
                        raf.seek(valOrOffset);
                        byte[] dBuf = new byte[(int)count * 8];
                        raf.readFully(dBuf);
                        java.nio.ByteBuffer dbb = java.nio.ByteBuffer.wrap(dBuf).order(isLE ? java.nio.ByteOrder.LITTLE_ENDIAN : java.nio.ByteOrder.BIG_ENDIAN);
                        System.out.printf("  TiePoint: (i=%.2f, j=%.2f, k=%.2f -> X0=%.8f, Y0=%.8f, Z0=%.8f)%n",
                                dbb.getDouble(), dbb.getDouble(), dbb.getDouble(), dbb.getDouble(), dbb.getDouble(), dbb.getDouble());
                        raf.seek(pos);
                    }
                }
            }
        }
    }

    @Test
    public void testDirectTiffReader() throws Exception {
        File f = new File("data/maps/usgs/ETOPO_2022_v1_60s_N90W180_bed.tif");
        if (!f.exists()) return;
        long t0 = System.currentTimeMillis();

        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r")) {
            byte[] header = new byte[8];
            raf.readFully(header);
            boolean isLittleEndian = (header[0] == 'I' && header[1] == 'I');
            int magic = isLittleEndian ? (header[2] & 0xFF) | ((header[3] & 0xFF) << 8)
                                       : ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
            System.out.printf("TIFF LittleEndian=%b, Magic=%d%n", isLittleEndian, magic);

            long ifdOffset = isLittleEndian
                    ? (header[4] & 0xFFL) | ((header[5] & 0xFFL) << 8) | ((header[6] & 0xFFL) << 16) | ((header[7] & 0xFFL) << 24)
                    : ((header[4] & 0xFFL) << 24) | ((header[5] & 0xFFL) << 16) | ((header[6] & 0xFFL) << 8) | (header[7] & 0xFFL);

            System.out.printf("IFD offset: %d%n", ifdOffset);
            raf.seek(ifdOffset);

            int numEntries = isLittleEndian ? (raf.readUnsignedByte() | (raf.readUnsignedByte() << 8))
                                            : ((raf.readUnsignedByte() << 8) | raf.readUnsignedByte());
            System.out.printf("IFD num entries: %d%n", numEntries);

            int imgW = 0, imgH = 0, tileW = 0, tileH = 0, compression = 0, predictor = 1, sampleFormat = 1;
            long[] tileOffsets = null;
            long[] tileByteCounts = null;

            for (int i = 0; i < numEntries; i++) {
                byte[] entry = new byte[12];
                raf.readFully(entry);
                java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(entry).order(isLittleEndian ? java.nio.ByteOrder.LITTLE_ENDIAN : java.nio.ByteOrder.BIG_ENDIAN);
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
                else if (tag == 339) sampleFormat = (int) valOrOffset;
                else if (tag == 324) {
                    // TileOffsets
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
                            tileOffsets[c] = java.nio.ByteBuffer.wrap(buf4).order(isLittleEndian ? java.nio.ByteOrder.LITTLE_ENDIAN : java.nio.ByteOrder.BIG_ENDIAN).getInt() & 0xFFFFFFFFL;
                        }
                    }
                    raf.seek(savedPos);
                } else if (tag == 325) {
                    // TileByteCounts
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
                            tileByteCounts[c] = java.nio.ByteBuffer.wrap(buf4).order(isLittleEndian ? java.nio.ByteOrder.LITTLE_ENDIAN : java.nio.ByteOrder.BIG_ENDIAN).getInt() & 0xFFFFFFFFL;
                        }
                    }
                    raf.seek(savedPos);
                }
            }

            System.out.printf("Direct TIFF parsed: %dx%d, Tile: %dx%d, Compression=%d, Predictor=%d, SampleFormat=%d, Tiles=%d%n",
                    imgW, imgH, tileW, tileH, compression, predictor, sampleFormat, tileOffsets != null ? tileOffsets.length : 0);

            // Now decode tiles into target grid
            int targetW = 2048;
            int targetH = 1024;
            float[][] elevGrid = new float[targetH][targetW];

            int numTilesX = (imgW + tileW - 1) / tileW;
            int numTilesY = (imgH + tileH - 1) / tileH;

            byte[] compBuffer = new byte[256 * 256 * 4 * 2];
            byte[] decompBuffer = new byte[256 * 256 * 4];
            java.util.zip.Inflater inflater = new java.util.zip.Inflater();

            for (int ty = 0; ty < numTilesY; ty++) {
                for (int tx = 0; tx < numTilesX; tx++) {
                    int tileIdx = ty * numTilesX + tx;
                    long offset = tileOffsets[tileIdx];
                    int byteCount = (int) tileByteCounts[tileIdx];

                    raf.seek(offset);
                    raf.readFully(compBuffer, 0, byteCount);

                    // Decompress
                    int decompSize = tileW * tileH * 4;
                    if (compression == 8) { // Deflate
                        inflater.reset();
                        inflater.setInput(compBuffer, 0, byteCount);
                        int read = inflater.inflate(decompBuffer, 0, decompSize);
                    } else if (compression == 1) { // Uncompressed
                        System.arraycopy(compBuffer, 0, decompBuffer, 0, byteCount);
                    }

                    // Undo Floating Point Horizontal Differencing Predictor (Predictor 3)
                    // For each row of tileW pixels (tileW * 4 bytes):
                    // The row has tileW bytes of byte 0, tileW bytes of byte 1, tileW bytes of byte 2, tileW bytes of byte 3.
                    // Each component is differentially encoded horizontally.
                    int bytesPerRow = tileW * 4;
                    float[] tileFloats = new float[tileW * tileH];

                    for (int row = 0; row < tileH; row++) {
                        int rowOffset = row * bytesPerRow;
                        if (predictor == 3) {
                            // Accumulate differentials per byte plane (4 planes of tileW bytes)
                            for (int p = 0; p < 4; p++) {
                                int planeOffset = rowOffset + p * tileW;
                                for (int col = 1; col < tileW; col++) {
                                    decompBuffer[planeOffset + col] = (byte) ((decompBuffer[planeOffset + col] & 0xFF) + (decompBuffer[planeOffset + col - 1] & 0xFF));
                                }
                            }
                            // Reorder planar bytes into IEEE 754 32-bit floats
                            // In TIFF Spec (Floating Point Predictor), bytes are stored MSB first (plane 0 = MSB, plane 3 = LSB)
                            for (int col = 0; col < tileW; col++) {
                                byte b0 = decompBuffer[rowOffset + 0 * tileW + col]; // MSB
                                byte b1 = decompBuffer[rowOffset + 1 * tileW + col];
                                byte b2 = decompBuffer[rowOffset + 2 * tileW + col];
                                byte b3 = decompBuffer[rowOffset + 3 * tileW + col]; // LSB

                                int bits = ((b0 & 0xFF) << 24) | ((b1 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b3 & 0xFF);
                                tileFloats[row * tileW + col] = Float.intBitsToFloat(bits);
                            }
                        } else {
                            // Plain IEEE floats
                            for (int col = 0; col < tileW; col++) {
                                int idx = rowOffset + col * 4;
                                byte b0 = decompBuffer[idx];
                                byte b1 = decompBuffer[idx + 1];
                                byte b2 = decompBuffer[idx + 2];
                                byte b3 = decompBuffer[idx + 3];
                                int bits;
                                if (isLittleEndian) {
                                    bits = (b0 & 0xFF) | ((b1 & 0xFF) << 8) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 24);
                                } else {
                                    bits = ((b0 & 0xFF) << 24) | ((b1 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b3 & 0xFF);
                                }
                                tileFloats[row * tileW + col] = Float.intBitsToFloat(bits);
                            }
                        }
                    }

                    // Map tileFloats to elevGrid
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

                            elevGrid[gy][gx] = tileFloats[tileY * tileW + tileX];
                        }
                    }
                }
            }

            System.out.printf("Direct ETOPO sampling (%dx%d) completed in %d ms!%n",
                    targetW, targetH, (System.currentTimeMillis() - t0));

            // Test points:
            double[][] pts = {
                {86.92, 27.98},   // Everest
                {142.2, 11.3},    // Mariana Trench
                {2.35, 48.85},    // Paris
                {15.0, 25.0},     // Sahara
                {7.0, 46.0},      // Alps
                {-70.0, -33.0},   // Andes
                {0.0, -90.0},     // South Pole
                {-40.0, 30.0}     // Atlantic
            };
            String[] names = {"Everest", "Mariana", "Paris", "Sahara", "Alps", "Andes", "South Pole", "Atlantic"};
            for (int i = 0; i < pts.length; i++) {
                int gx = (int) Math.clamp(((pts[i][0] + 180.0) / 360.0) * targetW, 0, targetW - 1);
                int gy = (int) Math.clamp(((90.0 - pts[i][1]) / 180.0) * targetH, 0, targetH - 1);
                System.out.printf("  %s (%f, %f) -> Elevation: %.1f m%n", names[i], pts[i][0], pts[i][1], elevGrid[gy][gx]);
            }
        }
    }

    @Test
    public void testRecoverReferencePngs() throws Exception {
        String[] pngs = {"data/maps/reference_earth_elevation.png", "data/maps/reference_earth_biomes.png"};
        for (String p : pngs) {
            File f = new File(p);
            if (!f.exists()) continue;
            byte[] raw = java.nio.file.Files.readAllBytes(f.toPath());
            if (raw.length >= 2 && raw[0] == (byte)0xFF && raw[1] == (byte)0xFE) {
                System.out.println("Recovering UTF-16 encoded PNG: " + p);
                // The file was written with PowerShell UTF-16 LE
                String str = new String(raw, java.nio.charset.StandardCharsets.UTF_16LE);
                byte[] recovered = str.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                // Fix first byte 0xEB -> 0x89 if needed (PNG magic is 0x89 0x50 0x4E 0x47)
                if (recovered.length > 4 && recovered[1] == 'P' && recovered[2] == 'N' && recovered[3] == 'G') {
                    recovered[0] = (byte) 0x89;
                }
                File recFile = new File(p + ".recovered.png");
                java.nio.file.Files.write(recFile.toPath(), recovered);
                BufferedImage img = ImageIO.read(recFile);
                if (img != null) {
                    System.out.printf("SUCCESSFULLY RECOVERED %s: %dx%d type=%d%n", p, img.getWidth(), img.getHeight(), img.getType());
                    // Sample recovered elevation
                    double[][] pts = {{86.9, 27.98}, {142.2, 11.3}, {2.35, 48.85}, {15.0, 25.0}, {0.0, -90.0}, {-40.0, 30.0}};
                    String[] ptNames = {"Everest", "Mariana", "Paris", "Sahara", "South Pole", "Atlantic"};
                    for (int i = 0; i < pts.length; i++) {
                        int x = (int) Math.clamp(((pts[i][0] + 180.0) / 360.0) * img.getWidth(), 0, img.getWidth() - 1);
                        int y = (int) Math.clamp(((90.0 - pts[i][1]) / 180.0) * img.getHeight(), 0, img.getHeight() - 1);
                        int rgb = img.getRGB(x, y);
                        System.out.printf("    %s -> RGB=(%d, %d, %d) / #%06X%n",
                                ptNames[i], (rgb>>16)&0xFF, (rgb>>8)&0xFF, rgb&0xFF, rgb & 0xFFFFFF);
                    }
                } else {
                    System.out.println("Could not decode recovered PNG for " + p);
                }
            }
        }
    }

    @Test
    public void inspectAllElevationFiles() throws Exception {
        String[] paths = {
            "data/maps/reference_earth_elevation.png",
            "data/maps/reference_earth_biomes.png",
            "data/maps/ether/earth/-100000/earth_-100000_elevation.png",
            "data/maps/ether/earth/-100000/earth_-100000_biomes.png",
            "data/maps/ether/earth/2026/earth_2026_elevation.png"
        };
        for (String p : paths) {
            File f = new File(p);
            if (!f.exists()) {
                System.out.println("FILE NOT FOUND: " + p);
                continue;
            }
            byte[] header = new byte[32];
            try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
                int read = fis.read(header);
                System.out.print("Header of " + p + ": ");
                for (int i = 0; i < read; i++) System.out.printf("%02X ", header[i]);
                System.out.print(" | ASCII: ");
                for (int i = 0; i < read; i++) System.out.print((header[i] >= 32 && header[i] <= 126) ? (char)header[i] : '.');
                System.out.println();
            }
            BufferedImage img = ImageIO.read(f);
            if (img == null) {
                System.out.println("COULD NOT DECODE PNG: " + p + " (length=" + f.length() + ")");
                continue;
            }
            System.out.println("=== " + p + " === (" + img.getWidth() + "x" + img.getHeight() + ") type=" + img.getType());
            int minR = 255, maxR = 0, minG = 255, maxG = 0, minB = 255, maxB = 0;
            for (int y = 0; y < img.getHeight(); y += 10) {
                for (int x = 0; x < img.getWidth(); x += 10) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    minR = Math.min(minR, r); maxR = Math.max(maxR, r);
                    minG = Math.min(minG, g); maxG = Math.max(maxG, g);
                    minB = Math.min(minB, b); maxB = Math.max(maxB, b);
                }
            }
            System.out.printf("  Range R: [%d..%d], G: [%d..%d], B: [%d..%d]%n", minR, maxR, minG, maxG, minB, maxB);
            // Sample some points: Everest (86.9, 27.98), Mariana Trench (142.2, 11.3), Paris (2.35, 48.85), Sahara (15.0, 25.0)
            double[][] pts = {{86.9, 27.98}, {142.2, 11.3}, {2.35, 48.85}, {15.0, 25.0}, {0.0, -90.0}, {-40.0, 30.0}};
            String[] ptNames = {"Everest", "Mariana", "Paris", "Sahara", "South Pole", "Mid-Atlantic Ocean"};
            for (int i = 0; i < pts.length; i++) {
                int x = (int) Math.clamp(((pts[i][0] + 180.0) / 360.0) * img.getWidth(), 0, img.getWidth() - 1);
                int y = (int) Math.clamp(((90.0 - pts[i][1]) / 180.0) * img.getHeight(), 0, img.getHeight() - 1);
                int rgb = img.getRGB(x, y);
                System.out.printf("    %s (%f, %f) -> RGB=(%d, %d, %d) / #%06X%n",
                        ptNames[i], pts[i][0], pts[i][1], (rgb>>16)&0xFF, (rgb>>8)&0xFF, rgb&0xFF, rgb & 0xFFFFFF);
            }
        }
    }
}
