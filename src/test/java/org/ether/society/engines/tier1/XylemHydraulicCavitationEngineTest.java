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
        List<H3Cell> cells = new ArrayList<>(cellCount);

        for (int i = 0; i < cellCount; i++) {
            H3Cell cell = new H3Cell();
            cell.setH3Index((long) (100000 + i));
            cell.setElevation(100.0);
            cell.setTemperature(20.0);

            if (i < 200) {
                // Agricultural cells facing severe megadrought (50 mm/yr rainfall vs 750 mm petDemand)
                cell.setBiome(Biome.PLAINS);
                cell.setRainfall(50.0);
                cell.setBiomassAgriculture(1000.0);
                cell.setFoodResource(500.0);
            } else if (i < 350) {
                // Forest drought cells (80 mm/yr rainfall)
                cell.setBiome(Biome.FOREST);
                cell.setRainfall(80.0);
                cell.setBiomassNatural(2500.0);
            } else {
                // Hydrated control group (800 mm/yr rainfall)
                cell.setBiome(Biome.TUNDRA);
                cell.setRainfall(800.0);
                cell.setBiomassNatural(1500.0);
            }
            cells.add(cell);
        }

        // Simulate 3-year severe megadrought
        for (int yr = 0; yr < 3; yr++) {
            XylemHydraulicCavitationEngine.processXylemCavitation(cells, 1.0);
        }

        // Cropland verification: severe runaway cavitation collapses agricultural biomass
        for (int i = 0; i < 200; i++) {
            H3Cell cropCell = cells.get(i);
            assertTrue(cropCell.getBiomassAgriculture() < 600.0,
                    "3-year agricultural megadrought must cause severe crop failure and dieback.");
        }

        // Forest verification: cavitation dieback reduces canopy biomass
        for (int i = 200; i < 350; i++) {
            H3Cell forestCell = cells.get(i);
            assertTrue(forestCell.getBiomassNatural() < 2500.0,
                    "Forest megadrought must induce canopy dieback.");
        }

        // Tundra control verification: hydrated cells maintain healthy natural biomass
        for (int i = 350; i < 500; i++) {
            H3Cell tundraCell = cells.get(i);
            assertEquals(1500.0, tundraCell.getBiomassNatural(), 1e-4,
                    "Hydrated cells with adequate rainfall must not suffer cavitation dieback.");
        }
    }

    @Test
    @DisplayName("1.4 Bitwise Determinism Verification")
    public void testBitwiseDeterminism() {
        List<H3Cell> run1 = createTestDomain(100);
        List<H3Cell> run2 = createTestDomain(100);

        for (int yr = 0; yr < 5; yr++) {
            XylemHydraulicCavitationEngine.processXylemCavitation(run1, 1.0);
            XylemHydraulicCavitationEngine.processXylemCavitation(run2, 1.0);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(run1.get(i).getBiomassAgriculture(), run2.get(i).getBiomassAgriculture(), 0.0,
                    "Xylem cavitation trajectories must be bitwise identical.");
            assertEquals(run1.get(i).getBiomassNatural(), run2.get(i).getBiomassNatural(), 0.0);
        }
    }

    private List<H3Cell> createTestDomain(int count) {
        List<H3Cell> cells = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            H3Cell cell = new H3Cell();
            cell.setH3Index((long) (50000 + i));
            cell.setBiome(i % 2 == 0 ? Biome.PLAINS : Biome.FOREST);
            cell.setTemperature(18.0);
            cell.setRainfall(70.0);
            cell.setBiomassAgriculture(800.0);
            cell.setBiomassNatural(1200.0);
            cells.add(cell);
        }
        return cells;
    }
}
