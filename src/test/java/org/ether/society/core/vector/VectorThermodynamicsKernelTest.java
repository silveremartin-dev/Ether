/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.vector;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VectorThermodynamicsKernelTest {

    @Test
    public void testVectorizedRadiativeEquilibriumDeterminism() {
        List<H3Cell> run1Cells = createSampleCells(100);
        List<H3Cell> run2Cells = createSampleCells(100);

        // Run SIMD calculation on Run 1
        VectorThermodynamicsKernel.computeRadiativeEquilibrium(run1Cells, 1361.0, 3.5, 0.0833);

        // Run SIMD calculation on Run 2
        VectorThermodynamicsKernel.computeRadiativeEquilibrium(run2Cells, 1361.0, 3.5, 0.0833);

        // Verify strict 100% bit-to-bit determinism
        for (int i = 0; i < 100; i++) {
            assertEquals(run1Cells.get(i).getTemperature(), run2Cells.get(i).getTemperature(),
                    "Vectorized calculations must produce strictly identical bitwise results");
        }
    }

    private List<H3Cell> createSampleCells(int count) {
        List<H3Cell> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            H3Cell c = new H3Cell();
            c.setH3Index((long) (i + 100));
            c.setBiome(Biome.FOREST);
            c.setDynamicAlbedo(0.15 + (i % 10) * 0.05);
            c.setTemperature(15.0 + (i % 5));
            c.setLatitude(40.0 + (i % 30));
            c.setLongitude(5.0 + (i % 30));
            list.add(c);
        }
        return list;
    }
}