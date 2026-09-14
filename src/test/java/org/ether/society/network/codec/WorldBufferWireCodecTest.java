/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.codec;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.network.EtherSecurityManager;
import org.ether.society.network.cluster.H3HaloBoundaryExchanger;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class WorldBufferWireCodecTest {

    @Test
    public void testEncodeDecodeChunkFidelity() {
        int capacity = 50;
        WorldBuffer source = new WorldBuffer(capacity);

        // Populate test data
        source.getElevation()[5] = 1250.5f;
        source.getTemperature()[5] = 22.4f;
        source.getRainfall()[5] = 850.0f;
        source.getBiomassHuman()[5] = 45000.0f;
        source.getFluxPressure()[5] = 0.88f;
        source.getBiomes()[5] = 3;
        source.getH3Indexes()[5] = 0x881f1d4887fffffL;

        byte[] encoded = WorldBufferWireCodec.encodeChunk(source, 0, 10, 42L);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);

        WorldBuffer destination = new WorldBuffer(capacity);
        WorldBufferWireCodec.ChunkPayload payload = WorldBufferWireCodec.decodeChunkInto(encoded, destination);

        assertEquals(42L, payload.getTickId());
        assertEquals(0, payload.getChunkStart());
        assertEquals(10, payload.getChunkCount());

        assertEquals(1250.5f, destination.getElevation()[5], 1e-4);
        assertEquals(22.4f, destination.getTemperature()[5], 1e-4);
        assertEquals(850.0f, destination.getRainfall()[5], 1e-4);
        assertEquals(45000.0f, destination.getBiomassHuman()[5], 1e-4);
        assertEquals(0.88f, destination.getFluxPressure()[5], 1e-4);
        assertEquals(3, destination.getBiomes()[5]);
        assertEquals(0x881f1d4887fffffL, destination.getH3Indexes()[5]);
    }

    @Test
    public void testEncryptedChunkSerialization() throws Exception {
        EtherSecurityManager sec = new EtherSecurityManager();
        WorldBuffer source = new WorldBuffer(20);
        source.getBiomassHuman()[2] = 99999.0f;

        String encrypted = WorldBufferWireCodec.encodeAndEncryptChunk(source, 0, 10, 100L, sec);
        assertNotNull(encrypted);

        WorldBuffer target = new WorldBuffer(20);
        WorldBufferWireCodec.ChunkPayload decoded = WorldBufferWireCodec.decryptAndDecodeChunk(encrypted, target, sec);

        assertEquals(100L, decoded.getTickId());
        assertEquals(99999.0f, target.getBiomassHuman()[2], 1e-4);
    }

    @Test
    public void testHaloBoundaryDeltaExchange() {
        WorldBuffer source = new WorldBuffer(30);
        source.getFluxPressure()[7] = 12.34f;
        source.getTemperature()[7] = 18.5f;

        Set<Integer> boundary = new HashSet<>();
        boundary.add(7);

        List<H3HaloBoundaryExchanger.HaloCellDelta> deltas = H3HaloBoundaryExchanger.extractHaloDeltas(source, boundary);
        assertEquals(1, deltas.size());

        byte[] serialized = H3HaloBoundaryExchanger.serializeHaloDeltas(deltas);
        WorldBuffer target = new WorldBuffer(30);
        H3HaloBoundaryExchanger.applyHaloDeltas(serialized, target);

        assertEquals(12.34f, target.getFluxPressure()[7], 1e-4);
        assertEquals(18.5f, target.getTemperature()[7], 1e-4);
    }
}
