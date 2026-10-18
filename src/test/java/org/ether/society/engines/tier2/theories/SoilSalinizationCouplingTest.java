/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Irrigated Soil Salinization via Darcy-Van Genuchten Coupling.
 * Verifies salt mass accumulation in arid irrigated floodplains, groundwater capillary rise,
 * Maas-Hoffman yield attenuation, and drainage leaching.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SoilSalinizationCouplingTest {

    @Test
    /*
     * Test salt accumulation and yield degradation in arid irrigated plain.
     */
    public void testSaltAccumulationAndYieldDegradationInAridBasin() {
        SoilSalinizationHydrologyEngine engine = new SoilSalinizationHydrologyEngine();
        List<H3Cell> cells = new ArrayList<>();

        H3Cell mesopotamianCell = new H3Cell();
        mesopotamianCell.setH3Index(6001L);
        mesopotamianCell.setPopulation(800); // Dense agrarian population
        mesopotamianCell.setRainfall(180.0); // Arid climate (< 450 mm)
        mesopotamianCell.setTemperature(26.0); // Warm temperatures (> 18°C)
        mesopotamianCell.setElevation(120.0); // Lowland floodplain
        mesopotamianCell.setAccessibleAquifer(150.0); // Shallow water table providing capillary solute flux
        mesopotamianCell.setFoodResource(5000.0);
        mesopotamianCell.setBiomassAgriculture(1000.0);
        mesopotamianCell.setTechnologyLevel(1.5); // Pre-industrial Bronze Age (lacks tile drainage)
        mesopotamianCell.setSoilSalinity(0.5); // Baseline low salinity
        cells.add(mesopotamianCell);

        double initialFood = mesopotamianCell.getFoodResource();
        double initialAgri = mesopotamianCell.getBiomassAgriculture();

        // Simulate 20 annual cycles of intensive irrigation
        for (int y = 0; y < 20; y++) {
            engine.process(cells, 1.0);
        }

        assertTrue(mesopotamianCell.getSoilSalinity() > 0.5,
                "Soil salinity must accumulate over decades of intensive irrigation in arid basins.");
        assertTrue(mesopotamianCell.getFoodResource() < initialFood,
                "Accumulated soil salinity must degrade food production (Maas-Hoffman curve).");
        assertTrue(mesopotamianCell.getBiomassAgriculture() < initialAgri,
                "Accumulated soil salinity must attenuate standing agricultural biomass.");
    }

    @Test
    /*
     * Test high rainfall or advanced modern drainage leaches soil salts.
     */
    public void testHighRainfallAndModernDrainageLeachesSalts() {
        SoilSalinizationHydrologyEngine engine = new SoilSalinizationHydrologyEngine();
        List<H3Cell> cells = new ArrayList<>();

        H3Cell drainedCell = new H3Cell();
        drainedCell.setH3Index(6002L);
        drainedCell.setPopulation(600);
        drainedCell.setRainfall(350.0);
        drainedCell.setTemperature(22.0);
        drainedCell.setElevation(100.0);
        drainedCell.setAccessibleAquifer(0.0); // Deep water table, no capillary rise
        drainedCell.setTechnologyLevel(7.0);  // Modern industrial drainage tile infrastructure
        drainedCell.setSoilSalinity(12.0);    // Previously heavily salinized soil
        drainedCell.setFoodResource(2000.0);
        cells.add(drainedCell);

        // Simulate 10 years with modern tile drainage
        for (int y = 0; y < 10; y++) {
            engine.process(cells, 1.0);
        }

        assertTrue(drainedCell.getSoilSalinity() < 12.0,
                "Advanced drainage systems must leach salts out of the rhizosphere.");
    }
}
