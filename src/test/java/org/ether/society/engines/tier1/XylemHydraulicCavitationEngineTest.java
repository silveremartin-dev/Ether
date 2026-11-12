/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Epistemic verification and validation test suite for {@link XylemHydraulicCavitationEngine}.
 * Verifies Weibull vulnerability curve, soil matric potential coupling, runaway cavitation dieback,
 * 500-cell megadrought simulation, and bitwise determinism.
 */
public class XylemHydraulicCavitationEngineTest {

    @Test
    @DisplayName("1.1 Soil Matric Potential & Vulnerability Curve Exact Calculations")
    public void testVulnerabilityFormulas() {
        // Soil water potential at full saturation (Sw = 1.0)
        double psiWet = XylemHydraulicCavitationEngine.estimateSoilWaterPotentialMPa(1.0);
        assertEquals(-XylemHydraulicCavitationEngine.AIR_ENTRY_SUCTION_MPA, psiWet, 1e-6,
                "At full saturation, soil water potential must equal air-entry suction (-0.035 MPa).");

        // Drought soil water potential (Sw = 0.20)
        double psiDry = XylemHydraulicCavitationEngine.estimateSoilWaterPotentialMPa(0.20);
        assertTrue(psiDry < -2.0, "Dry soil (20% saturation) must develop significant suction (<-2 MPa).");

        // When psi == psi50, PLC must equal exactly 50%
        double plcAtPsi50 = XylemHydraulicCavitationEngine.calculatePercentageLossOfConductivity(-2.50, -2.50, 3.5);
        assertEquals(50.0, plcAtPsi50, 1e-4, "PLC at psi = psi50 must be exactly 50%.");

        // Near zero water stress (psi = -0.1 MPa)
        double plcWet = XylemHydraulicCavitationEngine.calculatePercentageLossOfConductivity(-0.10, -2.50, 3.5);
        assertTrue(plcWet < 1.0, "Hydrated plants must experience minimal PLC (< 1%).");

        // Extreme cavitation stress (psi = -6.0 MPa on crops)
        double plcExtreme = XylemHydraulicCavitationEngine.calculatePercentageLossOfConductivity(-6.0, -2.50, 3.5);
        assertTrue(plcExtreme > 95.0, "Severe water potential (-6.0 MPa) must induce near-complete cavitation (>95%).");
    }

    @Test
    @DisplayName("1.2 Biome-Specific Vulnerability Safety Margins")
    public void testBiomeSafetyMargins() {
        double psiPlains = XylemHydraulicCavitationEngine.getCriticalWaterPotentialMPa(Biome.PLAINS);
        double psiForest = XylemHydraulicCavitationEngine.getCriticalWaterPotentialMPa(Biome.FOREST);
        double psiTundra = XylemHydraulicCavitationEngine.getCriticalWaterPotentialMPa(Biome.TUNDRA);
        double psiDesert = XylemHydraulicCavitationEngine.getCriticalWaterPotentialMPa(Biome.DESERT);

        // Arid and boreal taxa have much more negative cavitation thresholds than agricultural/grassland crops
        assertTrue(psiPlains > psiForest, "Grasslands/crops are more vulnerable than temperate forests.");
        assertTrue(psiForest > psiTundra, "Temperate forests are more vulnerable than boreal conifers/tundra.");
        assertTrue(psiTundra > psiDesert, "Boreal conifers are more vulnerable than desert sclerophylls.");
    }

    @Test
    @DisplayName("1.3 500-Cell Megadrought Cavitation Simulation & Canopy Dieback")
    public void test500CellMegadroughtSimulation() {
        int cellCount = 500;
        WorldBuffer buf = new WorldBuffer(cellCount);

        for (int i = 0; i < cellCount; i++) {
            buf.getH3Indexes()[i] = 100000L + i;
            buf.getElevation()[i] = 100.0f;
            buf.getTemperature()[i] = 20.0f;

            if (i < 200) {
                // Agricultural cells facing severe megadrought (50 mm/yr rainfall vs 750 mm petDemand)
                buf.getBiomes()[i] = (byte) Biome.PLAINS.ordinal();
                buf.getRainfall()[i] = 50.0f;
                buf.getBiomassAgriculture()[i] = 1000.0f;
                buf.getFoodResource()[i] = 500.0f;
            } else if (i < 350) {
                // Forest drought cells (80 mm/yr rainfall)
                buf.getBiomes()[i] = (byte) Biome.FOREST.ordinal();
                buf.getRainfall()[i] = 80.0f;
                buf.getBiomassNatural()[i] = 2500.0f;
            } else {
                // Hydrated control group (800 mm/yr rainfall)
                buf.getBiomes()[i] = (byte) Biome.TUNDRA.ordinal();
                buf.getRainfall()[i] = 800.0f;
                buf.getBiomassNatural()[i] = 1500.0f;
            }
        }

        // Simulate 3-year severe megadrought
        XylemHydraulicCavitationEngine engine = new XylemHydraulicCavitationEngine();
        for (int yr = 0; yr < 3; yr++) {
            engine.tick(buf, 1.0, 12.0);
        }

        // Cropland verification: severe runaway cavitation collapses agricultural biomass
        for (int i = 0; i < 200; i++) {
            assertTrue(buf.getBiomassAgriculture()[i] < 600.0f,
                    "3-year agricultural megadrought must cause severe crop failure and dieback.");
        }

        // Forest verification: cavitation dieback reduces canopy biomass
        for (int i = 200; i < 350; i++) {
            assertTrue(buf.getBiomassNatural()[i] < 2500.0f,
                    "Forest megadrought must induce canopy dieback.");
        }

        // Tundra control verification: hydrated cells maintain healthy natural biomass
        for (int i = 350; i < 500; i++) {
            assertEquals(1500.0f, buf.getBiomassNatural()[i], 1e-4f,
                    "Hydrated cells with adequate rainfall must not suffer cavitation dieback.");
        }
    }

    @Test
    @DisplayName("1.4 Bitwise Determinism Verification")
    public void testBitwiseDeterminism() {
        WorldBuffer run1 = createTestDomain(100);
        WorldBuffer run2 = createTestDomain(100);

        XylemHydraulicCavitationEngine engine1 = new XylemHydraulicCavitationEngine();
        XylemHydraulicCavitationEngine engine2 = new XylemHydraulicCavitationEngine();
        for (int yr = 0; yr < 5; yr++) {
            engine1.tick(run1, 1.0, 12.0);
            engine2.tick(run2, 1.0, 12.0);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(run1.getBiomassAgriculture()[i], run2.getBiomassAgriculture()[i], 0.0f,
                    "Xylem cavitation trajectories must be bitwise identical.");
            assertEquals(run1.getBiomassNatural()[i], run2.getBiomassNatural()[i], 0.0f);
        }
    }

    private WorldBuffer createTestDomain(int count) {
        WorldBuffer buf = new WorldBuffer(count);
        for (int i = 0; i < count; i++) {
            buf.getH3Indexes()[i] = 50000L + i;
            buf.getBiomes()[i] = (byte) (i % 2 == 0 ? Biome.PLAINS.ordinal() : Biome.FOREST.ordinal());
            buf.getTemperature()[i] = 18.0f;
            buf.getRainfall()[i] = 70.0f;
            buf.getBiomassAgriculture()[i] = 800.0f;
            buf.getBiomassNatural()[i] = 1200.0f;
        }
        return buf;
    }
}
