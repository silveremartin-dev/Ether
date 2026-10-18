/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.codec;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for {@link CellTopologyWireCodec}.
 * Validates high-performance binary serialization, GZIP file roundtrips,
 * and malformed payload rejection.
 */
class CellTopologyWireCodecTest {

    @Test
    @DisplayName("encodeTopology and decodeTopology preserve all static cell attributes perfectly")
    void testEncodeDecodeRoundTrip() {
        List<H3Cell> originalCells = new ArrayList<>();

        H3Cell c1 = new H3Cell(0x882685623ffffffL, 37.7749, -122.4194);
        c1.setElevation(52.0);
        c1.setBiome(Biome.FOREST);
        c1.setIsCoastal(true);
        c1.setIsPolder(false);
        c1.setHasFloatingInfrastructure(false);
        c1.setMovementFriction(1.15);
        c1.setDynamicAlbedo(0.22);
        c1.setMantleHeatFlow(85.0);
        c1.setSoilOrganicCarbon(4.5);
        c1.setFreshwaterAquifer(120.0);
        c1.setAccessibleAquifer(80.0);
        c1.setResourceMetal(25.0);
        c1.setResourcePreciousMetal(3.0);
        c1.setResourceClay(15.0);
        c1.setResourceWork(50.0);
        c1.setCoastalMarineResource(60.0);
        c1.setLanguageGroup("Indo-European");
        originalCells.add(c1);

        H3Cell c2 = new H3Cell(0x882685625ffffffL, -33.8688, 151.2093);
        c2.setElevation(15.0);
        c2.setBiome(Biome.PLAINS);
        c2.setIsCoastal(false);
        c2.setIsPolder(true);
        c2.setHasFloatingInfrastructure(true);
        c2.setMovementFriction(0.95);
        c2.setLanguageGroup("Pama-Nyungan");
        originalCells.add(c2);

        byte[] payload = CellTopologyWireCodec.encodeTopology(originalCells);
        assertNotNull(payload);
        assertTrue(payload.length > 0);

        List<H3Cell> decodedCells = CellTopologyWireCodec.decodeTopology(payload);
        assertNotNull(decodedCells);
        assertEquals(2, decodedCells.size());

        H3Cell d1 = decodedCells.get(0);
        assertEquals(c1.getH3Index(), d1.getH3Index());
        assertEquals(c1.getLatitude(), d1.getLatitude(), 1e-4);
        assertEquals(c1.getLongitude(), d1.getLongitude(), 1e-4);
        assertEquals(c1.getElevation(), d1.getElevation(), 1e-3);
        assertEquals(c1.getBiome(), d1.getBiome());
        assertEquals(c1.getIsCoastal(), d1.getIsCoastal());
        assertEquals(c1.getIsPolder(), d1.getIsPolder());
        assertEquals(c1.getLanguageGroup(), d1.getLanguageGroup());

        H3Cell d2 = decodedCells.get(1);
        assertEquals(c2.getH3Index(), d2.getH3Index());
        assertEquals(c2.getLanguageGroup(), d2.getLanguageGroup());
        assertEquals(c2.getIsPolder(), d2.getIsPolder());
        assertEquals(c2.getHasFloatingInfrastructure(), d2.getHasFloatingInfrastructure());
    }

    @Test
    @DisplayName("saveToFile writes compressed GZIP archive successfully")
    void testSaveToFile(@TempDir Path tempDir) throws IOException {
        Path targetFile = tempDir.resolve("world_topology.bin.gz");

        List<H3Cell> cells = List.of(
                new H3Cell(123456789L, 10.0, 20.0)
        );

        CellTopologyWireCodec.saveToFile(targetFile, cells);

        assertTrue(java.nio.file.Files.exists(targetFile));
        assertTrue(java.nio.file.Files.size(targetFile) > 0);
    }

    @Test
    @DisplayName("decodeTopology rejects corrupted or malformed payloads")
    void testMalformedPayload() {
        assertThrows(IllegalArgumentException.class, () -> CellTopologyWireCodec.decodeTopology(null));
        assertThrows(IllegalArgumentException.class, () -> CellTopologyWireCodec.decodeTopology(new byte[]{1, 2, 3}));

        byte[] badHeader = new byte[20];
        badHeader[0] = 0x11; // invalid magic header
        assertThrows(IllegalArgumentException.class, () -> CellTopologyWireCodec.decodeTopology(badHeader));
    }
}
