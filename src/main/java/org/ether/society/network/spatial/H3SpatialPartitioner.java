/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.spatial;

import org.ether.society.database.H3Cell;

import java.util.*;

/**
 * High-performance spatial domain partitioner for distributed planetary simulation.
 * Uses 2D Hilbert Space-Filling Curves to project spherical coordinates (Lat/Lng) onto
 * a 1D locality-preserving index, ensuring that each cluster worker node computes a
 * compact, contiguous geographical domain (minimizing frontier boundary exchanges).
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class H3SpatialPartitioner {

    private static final int HILBERT_ORDER = 16; // 16-bit per axis -> 32-bit Hilbert index
    private static final int MAX_COORD = (1 << HILBERT_ORDER) - 1;

    public static class SpatialPartition {
        private final int partitionIndex;
        private final int startIndex;
        private final int endIndex;
        private final int cellCount;
        private final Set<Integer> boundaryIndices = new HashSet<>();

        public SpatialPartition(int partitionIndex, int startIndex, int endIndex) {
            this.partitionIndex = partitionIndex;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.cellCount = Math.max(0, endIndex - startIndex + 1);
        }

        public int getPartitionIndex() { return partitionIndex; }
        public int getStartIndex() { return startIndex; }
        public int getEndIndex() { return endIndex; }
        public int getCellCount() { return cellCount; }
        public Set<Integer> getBoundaryIndices() { return boundaryIndices; }
        public void addBoundaryIndex(int index) { boundaryIndices.add(index); }
    }

    /**
     * Sorts a list of H3Cell instances by their 2D Hilbert index to maximize spatial locality.
     * Modifies the list in-place.
     */
    public static void sortCellsByHilbertCurve(List<H3Cell> cells) {
        if (cells == null || cells.size() <= 1) return;

        try {
            cells.sort(Comparator.comparingLong(c -> {
                double lat = c.getLatitude() != null ? c.getLatitude() : 0.0;
                double lng = c.getLongitude() != null ? c.getLongitude() : 0.0;
                return computeHilbertKey(lat, lng);
            }));
        } catch (UnsupportedOperationException e) {
            // List is unmodifiable (e.g. List.of), ignore in-place sort
        }
    }

    /**
     * Generates N spatial partitions over a sorted cell array.
     */
    public static List<SpatialPartition> partition(int totalCells, int numPartitions) {
        List<SpatialPartition> partitions = new ArrayList<>();
        if (totalCells <= 0 || numPartitions <= 0) return partitions;

        int baseSize = totalCells / numPartitions;
        int remainder = totalCells % numPartitions;

        int currentStart = 0;
        for (int i = 0; i < numPartitions; i++) {
            int count = baseSize + (i < remainder ? 1 : 0);
            int currentEnd = currentStart + count - 1;
            if (count > 0) {
                partitions.add(new SpatialPartition(i, currentStart, currentEnd));
            }
            currentStart += count;
        }

        return partitions;
    }

    /**
     * Identifies boundary cells in a partition whose neighbor indices fall outside the partition bounds.
     */
    public static void computeBoundaries(SpatialPartition partition, int[][] neighborIndices) {
        if (partition == null || neighborIndices == null) return;
        int start = partition.getStartIndex();
        int end = partition.getEndIndex();

        for (int i = start; i <= end && i < neighborIndices.length; i++) {
            int[] neighbors = neighborIndices[i];
            if (neighbors != null) {
                for (int neighbor : neighbors) {
                    if (neighbor >= 0 && (neighbor < start || neighbor > end)) {
                        partition.addBoundaryIndex(i);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Computes the 1D Hilbert key from latitude [-90, +90] and longitude [-180, +180].
     */
    public static long computeHilbertKey(double latitude, double longitude) {
        // Normalize lat [-90, +90] to [0, MAX_COORD]
        double normLat = Math.max(0.0, Math.min(1.0, (latitude + 90.0) / 180.0));
        int x = (int) Math.round(normLat * MAX_COORD);

        // Normalize lng [-180, +180] to [0, MAX_COORD]
        double normLng = Math.max(0.0, Math.min(1.0, (longitude + 180.0) / 360.0));
        int y = (int) Math.round(normLng * MAX_COORD);

        return xy2d(HILBERT_ORDER, x, y);
    }

    /**
     * Calculates the computational weight per cell based on human population density,
     * trade/migration flux pressure, and institutional complexity.
     */
    public static float[] calculateWeights(org.ether.society.core.dod.WorldBuffer buffer) {
        if (buffer == null) return new float[0];
        int cap = buffer.getCapacity();
        float[] weights = new float[cap];

        float[] pop = buffer.getBiomassHuman();
        float[] flux = buffer.getFluxPressure();
        float[] complexity = buffer.getInstitutionalComplexity();

        for (int i = 0; i < cap; i++) {
            float p = (pop != null && i < pop.length) ? pop[i] : 0f;
            float f = (flux != null && i < flux.length) ? flux[i] : 0f;
            float c = (complexity != null && i < complexity.length) ? complexity[i] : 0f;

            // Baseline weight 1.0 + population scale + flux + institutional complexity
            weights[i] = 1.0f + (p * 0.0001f) + (f * 2.0f) + (c * 0.5f);
        }
        return weights;
    }

    /**
     * Generates N spatial partitions balanced by cumulative computational weight.
     */
    public static List<SpatialPartition> partitionByComputationalWeights(float[] weights, int numPartitions) {
        List<SpatialPartition> partitions = new ArrayList<>();
        if (weights == null || weights.length == 0 || numPartitions <= 0) return partitions;

        int totalCells = weights.length;
        if (numPartitions == 1) {
            partitions.add(new SpatialPartition(0, 0, totalCells - 1));
            return partitions;
        }

        double totalWeight = 0;
        for (float w : weights) totalWeight += Math.max(0.1f, w);

        double targetWeightPerPartition = totalWeight / numPartitions;

        int currentStart = 0;
        double currentWeight = 0;
        int partitionIdx = 0;

        for (int i = 0; i < totalCells; i++) {
            currentWeight += Math.max(0.1f, weights[i]);

            // When reaching target weight or last partition
            if (currentWeight >= targetWeightPerPartition && partitionIdx < numPartitions - 1) {
                partitions.add(new SpatialPartition(partitionIdx++, currentStart, i));
                currentStart = i + 1;
                currentWeight = 0;
            }
        }

        // Add remaining cells to final partition
        if (currentStart < totalCells) {
            partitions.add(new SpatialPartition(partitionIdx, currentStart, totalCells - 1));
        }

        return partitions;
    }

    /**
     * Standard Hilbert Curve mapping from (x,y) to 1D distance 'd'.
     */
    private static long xy2d(int n, int x, int y) {
        long d = 0;
        int max = 1 << n;
        for (int s = max / 2; s > 0; s /= 2) {
            int rx = (x & s) > 0 ? 1 : 0;
            int ry = (y & s) > 0 ? 1 : 0;
            d += (long) s * s * ((3 * rx) ^ ry);
            
            // Rotate
            if (ry == 0) {
                if (rx == 1) {
                    x = (max - 1) - x;
                    y = (max - 1) - y;
                }
                // Swap x and y
                int t = x;
                x = y;
                y = t;
            }
        }
        return d;
    }
}
