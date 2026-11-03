/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.falsification.formal;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import org.ether.society.database.H3Cell;
import org.ether.society.h3.DeterministicSpatialAdjacencyTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Low-level mathematical verification of spatial isotropy, rotational symmetry,
 * and continuum diffusion invariance across the Uber H3 hexagonal planetary mesh.
 *
 * <p>Validates that discrete 2D spatial diffusion over hexagonal adjacency graphs:
 * 1. Preserves total integrated mass exactly (Conservation of Mass).
 * 2. Exhibits minimal directional anisotropy (relative directional variance &lt; 3.5%),
 *    verifying that the discrete hexagonal lattice approximates continuous isotropic
 *    Euclidean/Riemannian diffusion without preferential coordinate axes or grid-alignment artifacts.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
@Tag("academic")
@Tag("scientific")
public class H3SpatialIsotropyAndGridInvarianceTest {

    @Test
    @DisplayName("H3 Discrete Diffusion: Radial Isotropy & Rotational Symmetry Verification")
    public void testHexagonalDiffusionIsotropyAndRotationalSymmetry() throws Exception {
        H3Core h3 = H3Core.newInstance();
        int resolution = 6;
        // Central origin: Paris (latitude 48.8566, longitude 2.3522)
        long centerH3 = h3.latLngToCell(48.8566, 2.3522, resolution);
        int diskRadius = 4;
        List<Long> diskIndices = h3.gridDisk(centerH3, diskRadius);
        int totalCells = diskIndices.size(); // 1 + 3*k*(k+1) = 61 cells for radius 4

        // Build cell array and map index to local slot
        Map<Long, Integer> indexToSlot = new HashMap<>();
        List<H3Cell> cellList = new ArrayList<>();
        for (int i = 0; i < totalCells; i++) {
            long idx = diskIndices.get(i);
            indexToSlot.put(idx, i);
            H3Cell cell = new H3Cell();
            cell.setH3Index(idx);
            LatLng coords = h3.cellToLatLng(idx);
            cell.setLatitude(coords.lat);
            cell.setLongitude(coords.lng);
            cellList.add(cell);
        }

        DeterministicSpatialAdjacencyTable table = new DeterministicSpatialAdjacencyTable(cellList);
        int centerSlot = indexToSlot.get(centerH3);

        // State vector: scalar concentration (e.g. population, heat, or solute mass)
        double[] concentration = new double[totalCells];
        double initialPulseMass = 10_000.0;
        concentration[centerSlot] = initialPulseMass;

        // Diffusion parameters: D * dt / (dx^2) <= 1/6 (von Neumann stability limit for hexagonal mesh)
        double diffusionCoeff = 0.12; // Stable CFL factor (< 1.0 / 6.0 = 0.1667)
        int diffusionSteps = 50;

        for (int step = 0; step < diffusionSteps; step++) {
            double[] nextConc = new double[totalCells];
            System.arraycopy(concentration, 0, nextConc, 0, totalCells);

            for (int i = 0; i < totalCells; i++) {
                int[] neighbors = table.getNeighborIndices(i);
                double fluxSum = 0.0;
                for (int n : neighbors) {
                    if (n >= 0) {
                        fluxSum += (concentration[n] - concentration[i]);
                    }
                }
                nextConc[i] += diffusionCoeff * fluxSum;
            }
            concentration = nextConc;
        }

        // Assertion 1: Conservation of Mass (Closed System Diffusion)
        double totalMass = 0.0;
        for (double c : concentration) {
            totalMass += c;
        }
        assertEquals(initialPulseMass, totalMass, 1e-4,
                "Diffusion process must conserve 100% of initial mass across the hexagonal sub-lattice");

        // Assertion 2: Strict Monotonic Radial Decay (C(k) decreases with topological distance k)
        List<Long> disk0 = List.of(centerH3);
        List<Long> disk1 = h3.gridDisk(centerH3, 1);
        List<Long> disk2 = h3.gridDisk(centerH3, 2);
        List<Long> disk3 = h3.gridDisk(centerH3, 3);

        List<Long> ring1 = getRingFromDisks(disk1, disk0);
        List<Long> ring2 = getRingFromDisks(disk2, disk1);
        List<Long> ring3 = getRingFromDisks(disk3, disk2);

        double meanRing1 = computeMean(ring1, indexToSlot, concentration);
        double meanRing2 = computeMean(ring2, indexToSlot, concentration);
        double meanRing3 = computeMean(ring3, indexToSlot, concentration);

        assertTrue(concentration[centerSlot] > meanRing1, "Center concentration must exceed Ring 1");
        assertTrue(meanRing1 > meanRing2, "Ring 1 concentration must exceed Ring 2");
        assertTrue(meanRing2 > meanRing3, "Ring 2 concentration must exceed Ring 3");

        // Assertion 3: Radial Isotropy & Rotational Symmetry (Directional Standard Deviation < 3.5%)
        // Ring 1 has exactly 6 neighbors at identical distances in 6 directions (60° separation)
        double stdRing1 = computeStdDev(ring1, indexToSlot, concentration, meanRing1);
        double relAnisotropyRing1 = stdRing1 / meanRing1;

        assertTrue(relAnisotropyRing1 < 0.035,
                String.format("Ring 1 relative directional anisotropy (%.4f) must be < 3.5%% on H3 grid", relAnisotropyRing1));

        // Ring 2 (12 neighbors: 6 along main axes, 6 bridge neighbors)
        double stdRing2 = computeStdDev(ring2, indexToSlot, concentration, meanRing2);
        double relAnisotropyRing2 = stdRing2 / meanRing2;

        assertTrue(relAnisotropyRing2 < 0.065,
                String.format("Ring 2 relative directional anisotropy (%.4f) must remain bounded (< 6.5%%)", relAnisotropyRing2));
    }

    private List<Long> getRingFromDisks(List<Long> outerDisk, List<Long> innerDisk) {
        Set<Long> ring = new LinkedHashSet<>(outerDisk);
        ring.removeAll(innerDisk);
        return new ArrayList<>(ring);
    }

    private double computeMean(List<Long> ring, Map<Long, Integer> indexToSlot, double[] concentration) {
        double sum = 0.0;
        int count = 0;
        for (Long idx : ring) {
            Integer slot = indexToSlot.get(idx);
            if (slot != null) {
                sum += concentration[slot];
                count++;
            }
        }
        return count > 0 ? sum / count : 0.0;
    }

    private double computeStdDev(List<Long> ring, Map<Long, Integer> indexToSlot, double[] concentration, double mean) {
        double varSum = 0.0;
        int count = 0;
        for (Long idx : ring) {
            Integer slot = indexToSlot.get(idx);
            if (slot != null) {
                double diff = concentration[slot] - mean;
                varSum += diff * diff;
                count++;
            }
        }
        return count > 0 ? Math.sqrt(varSum / count) : 0.0;
    }
}
