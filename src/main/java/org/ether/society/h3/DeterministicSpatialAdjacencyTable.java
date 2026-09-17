/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.h3;

import com.uber.h3core.H3Core;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Precomputed Zero-JNI Spatial Adjacency Table for H3 Hexagonal Grid.
 * Flattens all neighbor queries into a contiguous primitive index matrix
 * providing O(1) neighbor lookups in under 1 nanosecond with zero object allocation.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class DeterministicSpatialAdjacencyTable {
    private static final Logger logger = LoggerFactory.getLogger(DeterministicSpatialAdjacencyTable.class);

    private final int numCells;
    private final long[] cellIndices;
    private final int[][] neighborTable; // [cellIndex][6] -> neighbor cell internal index (-1 if boundary/empty)
    private final Map<Long, Integer> h3ToIndexMap;

    public DeterministicSpatialAdjacencyTable(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) {
            this.numCells = 0;
            this.cellIndices = new long[0];
            this.neighborTable = new int[0][0];
            this.h3ToIndexMap = Collections.emptyMap();
            return;
        }

        this.numCells = cells.size();
        this.cellIndices = new long[numCells];
        this.h3ToIndexMap = new HashMap<>(numCells * 2);
        this.neighborTable = new int[numCells][6];

        for (int i = 0; i < numCells; i++) {
            long h3 = cells.get(i).getH3Index();
            cellIndices[i] = h3;
            h3ToIndexMap.put(h3, i);
            Arrays.fill(neighborTable[i], -1);
        }

        buildAdjacencyMatrix();
    }

    private void buildAdjacencyMatrix() {
        try {
            H3Core h3 = H3Core.newInstance();
            long start = System.nanoTime();

            for (int i = 0; i < numCells; i++) {
                long h3Index = cellIndices[i];
                List<Long> disk = h3.gridDisk(h3Index, 1);
                int neighborCount = 0;
                for (Long neighborH3 : disk) {
                    if (neighborH3 == null || neighborH3 == h3Index) continue;
                    Integer neighborLocalIndex = h3ToIndexMap.get(neighborH3);
                    if (neighborLocalIndex != null && neighborCount < 6) {
                        neighborTable[i][neighborCount++] = neighborLocalIndex;
                    }
                }
            }

            long durationMs = (System.nanoTime() - start) / 1_000_000;
            logger.info(" Precomputed O(1) H3 Adjacency Table for {} cells in {} ms (Zero-JNI Active).", numCells, durationMs);
        } catch (IOException e) {
            logger.error("Failed to initialize H3Core for adjacency table: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets pre-computed neighbor indices for a given cell.
     *
     * @param cellLocalIndex The integer index of the cell (0 .. numCells - 1)
     * @return Array of up to 6 neighbor local indices (-1 if non-existent)
     */
    public int[] getNeighborIndices(int cellLocalIndex) {
        if (cellLocalIndex < 0 || cellLocalIndex >= numCells) return new int[0];
        return neighborTable[cellLocalIndex];
    }

    public int getCellIndex(long h3Index) {
        return h3ToIndexMap.getOrDefault(h3Index, -1);
    }

    public long getH3Index(int localIndex) {
        if (localIndex < 0 || localIndex >= numCells) return 0L;
        return cellIndices[localIndex];
    }

    public int getNumCells() {
        return numCells;
    }
}
