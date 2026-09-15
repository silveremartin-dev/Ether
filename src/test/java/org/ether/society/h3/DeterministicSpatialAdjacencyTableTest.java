/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.h3;

import com.uber.h3core.H3Core;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DeterministicSpatialAdjacencyTableTest {

    @Test
    public void testAdjacencyTableCreationAndLookup() throws Exception {
        H3Core h3 = H3Core.newInstance();
        long centerH3 = h3.latLngToCell(48.8566, 2.3522, 6); // Paris at res 6
        List<Long> disk = h3.gridDisk(centerH3, 1); // Center + 6 neighbors = 7 cells

        List<H3Cell> cells = new ArrayList<>();
        for (Long index : disk) {
            H3Cell c = new H3Cell();
            c.setH3Index(index);
            c.setBiome(Biome.FOREST);
            cells.add(c);
        }

        DeterministicSpatialAdjacencyTable table = new DeterministicSpatialAdjacencyTable(cells);

        assertEquals(7, table.getNumCells());
        int centerLocalIndex = table.getCellIndex(centerH3);
        assertTrue(centerLocalIndex >= 0);

        int[] neighbors = table.getNeighborIndices(centerLocalIndex);
        assertNotNull(neighbors);
        assertEquals(6, neighbors.length);

        int validNeighborCount = 0;
        for (int n : neighbors) {
            if (n >= 0) validNeighborCount++;
        }
        assertEquals(6, validNeighborCount, "Center cell must have all 6 neighboring cells indexed in table");
    }
}