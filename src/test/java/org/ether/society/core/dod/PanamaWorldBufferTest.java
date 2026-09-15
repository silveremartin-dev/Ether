/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PanamaWorldBufferTest {

    @Test
    public void testPanamaOffHeapBufferAllocationAndAccess() {
        int capacity = 50;
        try (PanamaWorldBuffer buffer = new PanamaWorldBuffer(capacity)) {
            List<H3Cell> cells = new ArrayList<>();
            for (int i = 0; i < capacity; i++) {
                H3Cell c = new H3Cell();
                c.setH3Index((long) (i + 1));
                c.setBiome(Biome.FOREST);
                c.setTemperature(20.0 + i);
                c.setElevation(100.0 * i);
                c.setPopulation(1000 + i);
                c.setWaterResource(50.0);
                c.setFoodResource(200.0);
                cells.add(c);
            }

            buffer.ingestCells(cells);

            assertEquals(20.0, buffer.getTemperature(0), 1e-5);
            assertEquals(29.0, buffer.getTemperature(9), 1e-5);
            assertEquals(1000L, buffer.getPopulation(0));
            assertEquals(1049L, buffer.getPopulation(49));

            // Test Off-Heap mutation
            buffer.setTemperature(0, 35.5);
            assertEquals(35.5, buffer.getTemperature(0), 1e-5);
        }
    }
}