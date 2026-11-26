/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Irrigated Soil Salinization via Darcy-Van Genuchten Coupling.
 * Verifies salt mass accumulation in arid irrigated floodplains, groundwater capillary rise,
 * Maas-Hoffman yield attenuation, and drainage leaching.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class SoilSalinizationCouplingTest {

    @Test
    /*
     * Test salt accumulation and yield degradation in arid irrigated plain.
     */
    public void testSaltAccumulationAndYieldDegradationInAridBasin() {
        SoilSalinizationHydrologyEngine engine = new SoilSalinizationHydrologyEngine();
        WorldBuffer buffer = new WorldBuffer(1);

        buffer.getHumanPopulation()[0] = 800.0f; // Dense agrarian population
        buffer.getRainfall()[0] = 180.0f;        // Arid climate (< 450 mm)
        buffer.getTemperature()[0] = 26.0f;      // Warm temperatures (> 18°C)
        buffer.getElevation()[0] = 120.0f;       // Lowland floodplain
        buffer.getFreshwaterAquifer()[0] = 150.0f; // Shallow water table providing capillary solute flux
        buffer.getFoodResource()[0] = 5000.0f;
        buffer.getBiomassAgriculture()[0] = 1000.0f;
        buffer.getResourceCapital()[0] = 80000.0f; // Pre-industrial Bronze Age (k_pc = 100 kg/cap < 1200 tile drainage threshold)

        float initialFood = buffer.getFoodResource()[0];
        float initialAgri = buffer.getBiomassAgriculture()[0];

        // Simulate 20 annual cycles of intensive irrigation
        for (int y = 0; y < 20; y++) {
            engine.tick(buffer, 1.0, 12.0);
        }

        assertTrue(buffer.getFoodResource()[0] < initialFood,
                "Accumulated soil salinity must degrade food production (Maas-Hoffman curve).");
        assertTrue(buffer.getBiomassAgriculture()[0] < initialAgri,
                "Accumulated soil salinity must attenuate standing agricultural biomass.");
    }

    @Test
    /*
     * Test high rainfall or advanced modern drainage leaches soil salts.
     */
    public void testHighRainfallAndModernDrainageLeachesSalts() {
        SoilSalinizationHydrologyEngine engine = new SoilSalinizationHydrologyEngine();
        WorldBuffer buffer = new WorldBuffer(1);

        buffer.getHumanPopulation()[0] = 600.0f;
        buffer.getRainfall()[0] = 430.0f; // Near-humid leaching
        buffer.getTemperature()[0] = 22.0f;
        buffer.getElevation()[0] = 100.0f;
        buffer.getFreshwaterAquifer()[0] = 0.0f; // Deep water table, no capillary rise
        buffer.getResourceCapital()[0] = 3000000.0f; // Modern industrial drainage tile infrastructure (k_pc = 5000 kg/cap)
        buffer.getFoodResource()[0] = 2000.0f;
        buffer.getBiomassAgriculture()[0] = 1000.0f;

        // Simulate 10 years with modern tile drainage: leaching prevents yield collapse
        for (int y = 0; y < 10; y++) {
            engine.tick(buffer, 1.0, 12.0);
        }

        assertEquals(2000.0f, buffer.getFoodResource()[0], 1e-3f,
                "Advanced drainage systems must leach salts and preserve food production.");
    }
}
