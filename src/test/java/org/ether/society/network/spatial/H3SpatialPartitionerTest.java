/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.spatial;

import org.ether.society.database.H3Cell;
import org.ether.society.network.spatial.H3SpatialPartitioner.SpatialPartition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class H3SpatialPartitionerTest {

    @Test
    public void testHilbertKeyMonotonicityAndDeterminism() {
        long key1 = H3SpatialPartitioner.computeHilbertKey(48.8566, 2.3522); // Paris
        long key2 = H3SpatialPartitioner.computeHilbertKey(48.8566, 2.3522); // Paris (identical)
        long key3 = H3SpatialPartitioner.computeHilbertKey(35.6762, 139.6503); // Tokyo

        assertEquals(key1, key2, "Hilbert key calculation must be 100% deterministic");
        assertNotEquals(key1, key3, "Distant geographical coordinates should produce distinct Hilbert keys");
    }

    @Test
    public void testCellSortingByHilbertCurve() {
        List<H3Cell> cells = new ArrayList<>();

        H3Cell tokyo = new H3Cell();
        tokyo.setLatitude(35.6762);
        tokyo.setLongitude(139.6503);

        H3Cell paris = new H3Cell();
        paris.setLatitude(48.8566);
        paris.setLongitude(2.3522);

        H3Cell london = new H3Cell();
        london.setLatitude(51.5074);
        london.setLongitude(-0.1278);

        cells.add(tokyo);
        cells.add(paris);
        cells.add(london);

        H3SpatialPartitioner.sortCellsByHilbertCurve(cells);

        assertEquals(3, cells.size());
        // London and Paris (Western Europe) should be adjacent compared to Tokyo
        int londonIdx = cells.indexOf(london);
        int parisIdx = cells.indexOf(paris);
        assertEquals(1, Math.abs(londonIdx - parisIdx), "Geographically neighboring cities (London/Paris) should be clustered together");
    }

    @Test
    public void testPartitionDivision() {
        int totalCells = 1000;
        int numPartitions = 4;

        List<SpatialPartition> partitions = H3SpatialPartitioner.partition(totalCells, numPartitions);

        assertEquals(4, partitions.size());
        assertEquals(0, partitions.get(0).getStartIndex());
        assertEquals(249, partitions.get(0).getEndIndex());
        assertEquals(250, partitions.get(0).getCellCount());

        assertEquals(250, partitions.get(1).getStartIndex());
        assertEquals(499, partitions.get(1).getEndIndex());

        assertEquals(750, partitions.get(3).getStartIndex());
        assertEquals(999, partitions.get(3).getEndIndex());
    }

    @Test
    public void testBoundaryDetection() {
        SpatialPartition partition = new SpatialPartition(0, 0, 10);
        int[][] neighborIndices = new int[20][6];

        // Cell 0 has neighbors inside partition [0..10]
        neighborIndices[0] = new int[]{1, 2, 3, 4, 5, 6};

        // Cell 10 has a neighbor (15) outside the partition bounds
        neighborIndices[10] = new int[]{7, 8, 9, 15, -1, -1};

        H3SpatialPartitioner.computeBoundaries(partition, neighborIndices);

        assertFalse(partition.getBoundaryIndices().contains(0));
        assertTrue(partition.getBoundaryIndices().contains(10), "Cell 10 should be recognized as boundary cell");
    }

    @Test
    public void testComputationalWeightPartitioning() {
        float[] weights = new float[100];
        // Cells 0..9 are high density mega-city (weight 10.0 each -> 100 total)
        for (int i = 0; i < 10; i++) weights[i] = 10.0f;
        // Cells 10..99 are empty ocean (weight 1.0 each -> 90 total)
        for (int i = 10; i < 100; i++) weights[i] = 1.0f;

        List<SpatialPartition> partitions = H3SpatialPartitioner.partitionByComputationalWeights(weights, 2);
        assertEquals(2, partitions.size());

        // Partition 0 should have fewer cells (dense area)
        assertTrue(partitions.get(0).getCellCount() < partitions.get(1).getCellCount(),
                "High-density partition should have fewer cells to balance computational weight");
    }
}
