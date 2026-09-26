package org.ether.society.data;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class TestLzwDecoder {

    @Test
    public void testDecode() throws Exception {
        File file = new File("data/maps/worldclim/bio_10m/wc2.1_10m_bio_1.tif");
        float[][] grid = readWorldClimTiff(file);
        System.out.printf("Successfully decoded TIFF grid: %dx%d%n", grid[0].length, grid.length);

        // Paris lat 48.8, lon 2.3
        int px = (int) ((2.35 + 180.0) / 360.0 * 2160);
        int py = (int) ((90.0 - 48.85) / 180.0 * 1080);
        System.out.printf("Decoded Paris Temp: %.2f °C%n", grid[py][px]);

        // Amazon lat -3.0, lon -60.0
        int ax = (int) ((-60.0 + 180.0) / 360.0 * 2160);
        int ay = (int) ((90.0 - (-3.0)) / 180.0 * 1080);
        System.out.printf("Decoded Amazon Temp: %.2f °C%n", grid[ay][ax]);

        // Sahara lat 24.0, lon 15.0
        int sx = (int) ((15.0 + 180.0) / 360.0 * 2160);
        int sy = (int) ((90.0 - 24.0) / 180.0 * 1080);
        System.out.printf("Decoded Sahara Temp: %.2f °C%n", grid[sy][sx]);
    }

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
                    // LZW
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
}
