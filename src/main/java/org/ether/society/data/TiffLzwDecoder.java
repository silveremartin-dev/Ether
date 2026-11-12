/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import java.util.Arrays;

/**
 * <h1>TiffLzwDecoder</h1>
 * <p>
 * High-performance bitstream decoder for LZW-compressed TIFF raster layers (e.g. WorldClim / GEBCO).
 * Implements the canonical TIFF 6.0 Section 13 LZW decompression specification with dynamic code sizing (9 to 12 bits).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class TiffLzwDecoder {

    private TiffLzwDecoder() {
        // Utility class
    }

    /**
     * Decodes an LZW-compressed byte buffer into the output uncompressed buffer.
     *
     * @param compressed   raw compressed byte array
     * @param uncompressed destination uncompressed byte array
     */
    public static void decodeTiffLzw(byte[] compressed, byte[] uncompressed) {
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

    /**
     * Extracts an arbitrary bit sequence from a byte array.
     *
     * @param data       byte buffer
     * @param bitOffset  starting bit index
     * @param bitLength  number of bits to extract
     * @return extracted integer value
     */
    public static int getBits(byte[] data, int bitOffset, int bitLength) {
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
