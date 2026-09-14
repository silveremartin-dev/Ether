/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.codec;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.network.EtherSecurityManager;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

/**
 * High-speed binary wire serializer for Structure-of-Arrays (SoA) WorldBuffer chunks.
 * Packs primitive contiguous arrays directly into binary ByteBuffers for zero-copy
 * network transmission across distributed cluster nodes with AES-256 GCM encryption.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class WorldBufferWireCodec {

    private static final int MAGIC_HEADER = 0x45544852; // "ETHR"
    private static final byte PROTOCOL_VERSION = 1;

    public static class ChunkPayload {
        private final long tickId;
        private final int chunkStart;
        private final int chunkCount;
        private final byte[] binaryData;

        public ChunkPayload(long tickId, int chunkStart, int chunkCount, byte[] binaryData) {
            this.tickId = tickId;
            this.chunkStart = chunkStart;
            this.chunkCount = chunkCount;
            this.binaryData = binaryData;
        }

        public long getTickId() { return tickId; }
        public int getChunkStart() { return chunkStart; }
        public int getChunkCount() { return chunkCount; }
        public byte[] getBinaryData() { return binaryData; }
    }

    /**
     * Serializes a slice of WorldBuffer [start .. start + count - 1] into a compact binary byte array.
     */
    public static byte[] encodeChunk(WorldBuffer buffer, int start, int count, long tickId) {
        if (buffer == null || count <= 0) return new byte[0];

        int validCount = Math.min(count, buffer.getCapacity() - start);
        if (validCount <= 0) return new byte[0];

        // 27 float arrays + 1 byte array + 1 long array (h3Indexes) = 117 bytes per cell
        // Header: Magic(4) + Version(1) + Tick(8) + Start(4) + Count(4) = 21 bytes
        int headerSize = 21;
        int bytesPerCell = 27 * Float.BYTES + Byte.BYTES + Long.BYTES;
        int dataSize = validCount * bytesPerCell;
        ByteBuffer byteBuffer = ByteBuffer.allocate(headerSize + dataSize).order(ByteOrder.LITTLE_ENDIAN);

        // Write Header
        byteBuffer.putInt(MAGIC_HEADER);
        byteBuffer.put(PROTOCOL_VERSION);
        byteBuffer.putLong(tickId);
        byteBuffer.putInt(start);
        byteBuffer.putInt(validCount);

        // Write H3 Indexes
        long[] h3 = buffer.getH3Indexes();
        for (int i = 0; i < validCount; i++) {
            byteBuffer.putLong(h3 != null && (start + i) < h3.length ? h3[start + i] : 0L);
        }

        // Write Biomes
        byte[] biomes = buffer.getBiomes();
        for (int i = 0; i < validCount; i++) {
            byteBuffer.put(biomes != null && (start + i) < biomes.length ? biomes[start + i] : 0);
        }

        // Helper to serialize float array
        serializeFloatSlice(byteBuffer, buffer.getElevation(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getTemperature(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getRainfall(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getFoodResource(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getWaterResource(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getWoodResource(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getMetalResource(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getClayResource(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getBiomassHuman(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getBiomassLivestock(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getBiomassFish(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getBiomassAgriculture(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getBiomassNatural(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getEnergyWind(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getEnergySolar(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getEnergyFire(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getEnergySlaves(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getEnergyFoodConsumed(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getLifespan(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getFertility(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getGiniIndex(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getTechnologyLevel(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getResourceCapital(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getFluxPressure(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getLocalPrice(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getStorage(), start, validCount);
        serializeFloatSlice(byteBuffer, buffer.getInstitutionalComplexity(), start, validCount);

        return byteBuffer.array();
    }

    /**
     * Decodes a binary payload and writes values directly into target WorldBuffer.
     */
    public static ChunkPayload decodeChunkInto(byte[] data, WorldBuffer targetBuffer) {
        if (data == null || data.length < 21) {
            throw new IllegalArgumentException("Corrupted wire data payload");
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int magic = byteBuffer.getInt();
        if (magic != MAGIC_HEADER) {
            throw new IllegalArgumentException("Invalid magic header: " + Integer.toHexString(magic));
        }

        byte version = byteBuffer.get();
        long tickId = byteBuffer.getLong();
        int start = byteBuffer.getInt();
        int count = byteBuffer.getInt();

        if (targetBuffer != null && targetBuffer.getCapacity() > 0) {
            int writeCount = Math.min(count, targetBuffer.getCapacity() - start);

            long[] h3 = targetBuffer.getH3Indexes();
            for (int i = 0; i < writeCount; i++) {
                long val = byteBuffer.getLong();
                if (h3 != null && start + i < h3.length) h3[start + i] = val;
            }

            byte[] biomes = targetBuffer.getBiomes();
            for (int i = 0; i < writeCount; i++) {
                byte val = byteBuffer.get();
                if (biomes != null && start + i < biomes.length) biomes[start + i] = val;
            }

            deserializeFloatSlice(byteBuffer, targetBuffer.getElevation(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getTemperature(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getRainfall(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getFoodResource(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getWaterResource(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getWoodResource(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getMetalResource(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getClayResource(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getBiomassHuman(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getBiomassLivestock(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getBiomassFish(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getBiomassAgriculture(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getBiomassNatural(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getEnergyWind(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getEnergySolar(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getEnergyFire(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getEnergySlaves(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getEnergyFoodConsumed(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getLifespan(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getFertility(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getGiniIndex(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getTechnologyLevel(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getResourceCapital(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getFluxPressure(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getLocalPrice(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getStorage(), start, writeCount);
            deserializeFloatSlice(byteBuffer, targetBuffer.getInstitutionalComplexity(), start, writeCount);
        }

        return new ChunkPayload(tickId, start, count, data);
    }

    /**
     * Encodes and encrypts chunk into Base64 string.
     */
    public static String encodeAndEncryptChunk(WorldBuffer buffer, int start, int count, long tickId, EtherSecurityManager security) throws Exception {
        byte[] raw = encodeChunk(buffer, start, count, tickId);
        String rawBase64 = Base64.getEncoder().encodeToString(raw);
        return security != null ? security.encrypt(rawBase64) : rawBase64;
    }

    /**
     * Decrypts and decodes chunk Base64 string into buffer.
     */
    public static ChunkPayload decryptAndDecodeChunk(String payload, WorldBuffer targetBuffer, EtherSecurityManager security) throws Exception {
        String decrypted = (security != null) ? security.decrypt(payload) : payload;
        byte[] rawBytes = Base64.getDecoder().decode(decrypted);
        return decodeChunkInto(rawBytes, targetBuffer);
    }

    private static void serializeFloatSlice(ByteBuffer buf, float[] arr, int start, int count) {
        for (int i = 0; i < count; i++) {
            buf.putFloat(arr != null && (start + i) < arr.length ? arr[start + i] : 0.0f);
        }
    }

    private static void deserializeFloatSlice(ByteBuffer buf, float[] arr, int start, int count) {
        for (int i = 0; i < count; i++) {
            float val = buf.getFloat();
            if (arr != null && (start + i) < arr.length) arr[start + i] = val;
        }
    }
}
