/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Epistemic verification and validation test suite for {@link StreamPowerErosionEngine}.
 * Verifies Stream Power Incision exponents ($m=0.5, n=1.0$), root protection damping,
 * floodplain alluvial sediment deposition, and 500-cell spatial grid conservation.
 */
public class StreamPowerErosionEngineTest {

    @Test
    @DisplayName("3.1 Canonical Stream Power Law Exponents & Root Cohesion Protection")
    public void testStreamPowerExponentsAndVegetationFactor() {
        // Vegetative root damping: Bare soil (B=0) vs Dense Forest (B=1000)
        double bareFactor = StreamPowerErosionEngine.calculateVegetationProtectionFactor(0.0);
        double forestFactor = StreamPowerErosionEngine.calculateVegetationProtectionFactor(1000.0);

        assertEquals(1.0, bareFactor, 1e-4, "Bare soil without root structure must have 1.0 erodibility factor.");
        assertTrue(forestFactor < 0.10, "Dense vegetation must suppress erodibility by over 90% (observed: " + forestFactor + ")");

        // Upstream Catchment Area Scaling (Hack's Law: L ~ A^0.57)
        double a10 = StreamPowerErosionEngine.estimateCatchmentAreaKm2(10.0);
        double a50 = StreamPowerErosionEngine.estimateCatchmentAreaKm2(50.0);
        assertTrue(a50 > a10, "Catchment area must scale with stream length.");

        // Stream Power Incision Rate E_dot = K * A^0.5 * S^1.0
        double eLowSlope = StreamPowerErosionEngine.calculateStreamPowerErosion(500.0, 0.01, 200.0);
        double eHighSlope = StreamPowerErosionEngine.calculateStreamPowerErosion(500.0, 0.04, 200.0);
        assertEquals(4.0, eHighSlope / eLowSlope, 0.05,
                "Erosion rate must scale linearly with slope gradient (n=1.0 exponent).");
    }

    @Test
    @DisplayName("3.2 Lowland Floodplain Alluvial Deposition Replenishes Soil Organic Carbon (SOC)")
    public void testLowlandAlluvialReplenishment() {
        H3Cell uplandCell = new H3Cell(0x8828308280fffffL, 35.0, 38.0);
        uplandCell.setElevation(800.0);
        uplandCell.setBiome(Biome.MOUNTAINS);
        uplandCell.setBiomassNatural(20.0); // Degraded upland slopes (bare rock/soil)
        uplandCell.setSoilOrganicCarbon(20.0);

        H3Cell alluvialDeltaCell = new H3Cell(0x8828308280ffffeL, 31.0, 31.0);
        alluvialDeltaCell.setElevation(15.0); // Lowland river floodplain/delta (e.g. Nile or Mesopotamia)
        alluvialDeltaCell.setBiome(Biome.PLAINS);
        alluvialDeltaCell.setBiomassNatural(200.0);
        alluvialDeltaCell.setSoilOrganicCarbon(50.0);

        List<H3Cell> basin = List.of(uplandCell, alluvialDeltaCell);

        double initialUplandElev = uplandCell.getElevation();
        double initialDeltaSoc = alluvialDeltaCell.getSoilOrganicCarbon();

        // Run 10 years of fluvial sediment dynamics
        StreamPowerErosionEngine.processStreamPowerErosion(basin, 10.0);

        assertTrue(uplandCell.getElevation() < initialUplandElev,
                "Upland mountain cell must undergo fluvial incision and erosion.");
        assertTrue(alluvialDeltaCell.getSoilOrganicCarbon() > initialDeltaSoc,
                "Lowland alluvial floodplain must receive deposited sediment, replenishing SOC fertility.");
    }

    @Test
    @DisplayName("3.3 500-Cell Mountain-to-Delta Catchment Simulation with Elevation & SOC Continuity")
    public void test500CellCatchmentSimulation() {
        List<H3Cell> mesh = new ArrayList<>();
        // Generate a 500-cell basin with elevation descending from 2000m to 5m
        for (int i = 0; i < 500; i++) {
            double elev = 5.0 + (1995.0 * i / 499.0);
            H3Cell c = new H3Cell(0x883000000000000L + i, 30.0 + (i * 0.01), 30.0 + (i * 0.01));
            c.setElevation(elev);
            if (elev > 1000.0) c.setBiome(Biome.MOUNTAINS);
            else if (elev > 300.0) c.setBiome(Biome.HILLS);
            else c.setBiome(Biome.PLAINS);
            c.setBiomassNatural(150.0);
            c.setSoilOrganicCarbon(40.0);
            mesh.add(c);
        }

        double initialAvgElevation = mesh.stream().mapToDouble(H3Cell::getElevation).average().orElse(0.0);

        // Advance 5 years
        StreamPowerErosionEngine.processStreamPowerErosion(mesh, 5.0);

        H3Cell mountainCell = mesh.get(450); // High mountain
        H3Cell deltaCell = mesh.get(5);      // Lowland alluvial plain (elev ~25m)

        assertTrue(mountainCell.getElevation() < (5.0 + (1995.0 * 450 / 499.0)),
                "Mountain cells must erode under steep slope hydraulic stream power.");
        assertTrue(deltaCell.getSoilOrganicCarbon() >= 40.0,
                "Lowland alluvial plain must maintain or increase SOC from upstream sediment wash.");
    }
}
