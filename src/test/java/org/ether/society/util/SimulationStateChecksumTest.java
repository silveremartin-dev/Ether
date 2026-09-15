/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.util;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationStateChecksumTest {

    @Test
    public void testChecksumDeterminismAndSensitivity() {
        List<H3Cell> cells = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            H3Cell c = new H3Cell();
            c.setH3Index((long) (i + 10));
            c.setBiome(Biome.FOREST);
            c.setPopulation(1000 * i);
            c.setTemperature(15.0 + i);
            c.setElevation(100.0);
            cells.add(c);
        }

        String hash1 = SimulationStateChecksum.computeStateChecksum(cells, 2026L, 1, 420.0);
        String hash2 = SimulationStateChecksum.computeStateChecksum(cells, 2026L, 1, 420.0);

        assertEquals(hash1, hash2, "Identical state must produce identical checksum");
        assertEquals(64, hash1.length(), "SHA-256 hex string must be 64 characters");

        // Small mutation alters hash completely
        cells.get(0).setPopulation(1);
        String mutatedHash = SimulationStateChecksum.computeStateChecksum(cells, 2026L, 1, 420.0);

        assertNotEquals(hash1, mutatedHash, "Any state alteration must produce a distinct checksum");
    }
}