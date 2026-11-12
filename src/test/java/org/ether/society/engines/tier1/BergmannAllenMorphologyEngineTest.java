/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Epistemic verification and validation test suite for {@link BergmannAllenMorphologyEngine}.
 * Verifies Bergmann body mass scaling, Allen surface area ratios, Foster island dwarfism,
 * 500-cell latitudinal thermal gradient adaptation, and bitwise determinism.
 */
public class BergmannAllenMorphologyEngineTest {

    @Test
    @DisplayName("2.1 Bergmann Body Mass & Allen SA/M Analytical Calculations")
    public void testBergmannAndAllenFormulas() {
        // Bergmann's rule: At reference temp (15°C), mass equals reference mass (50 kg)
        double refMass = BergmannAllenMorphologyEngine.calculateBergmannEquilibriumMass(15.0, 0.050);
        assertEquals(0.050, refMass, 1e-6, "At reference temperature (15°C), mass must equal reference mass.");

        // Cold periglacial climate (-10°C): mass increases to conserve heat
        double coldMass = BergmannAllenMorphologyEngine.calculateBergmannEquilibriumMass(-10.0, 0.050);
        assertTrue(coldMass > 0.050, "Cold climate must drive larger structural body mass (Bergmann's rule).");
        assertTrue(coldMass <= 0.050 * (1.0 + BergmannAllenMorphologyEngine.BERGMANN_SCALING_AMPLITUDE) + 1e-6);

        // Hyperthermic equatorial climate (35°C): mass decreases to facilitate heat dissipation
        double hotMass = BergmannAllenMorphologyEngine.calculateBergmannEquilibriumMass(35.0, 0.050);
        assertTrue(hotMass < 0.050, "Hot climate must favor smaller structural body mass.");
        assertTrue(hotMass >= 0.050 * (1.0 - BergmannAllenMorphologyEngine.BERGMANN_SCALING_AMPLITUDE) - 1e-6);

        // Allen's rule: SA/M ratio decreases as mass increases (c * M^(-1/3))
        double saRatioSmall = BergmannAllenMorphologyEngine.calculateSurfaceAreaToMassRatio(40.0);
        double saRatioLarge = BergmannAllenMorphologyEngine.calculateSurfaceAreaToMassRatio(80.0);
        assertTrue(saRatioSmall > saRatioLarge, "Smaller organisms have higher SA/M ratio than larger organisms.");

        // Foster's island rule
        assertEquals(1.0, BergmannAllenMorphologyEngine.calculateFosterIslandDwarfismFactor(false));
        assertEquals(0.35, BergmannAllenMorphologyEngine.calculateFosterIslandDwarfismFactor(true));
    }

    @Test
    @DisplayName("2.2 Thermoregulatory Somatic Maintenance Multiplier")
    public void testThermoregulatoryMaintenance() {
        // In thermal comfort zone (>= 20°C), multiplier is exactly 1.0 (no extra thermoregulatory cost)
        double warmCost = BergmannAllenMorphologyEngine.calculateThermoregulatoryMaintenanceMultiplier(22.0, 60.0);
        assertEquals(1.0, warmCost, 1e-6, "Above comfort temperature, thermoregulatory conductance multiplier must be 1.0.");

        // In periglacial cold (-5°C), multiplier increases above 1.0
        double coldCost = BergmannAllenMorphologyEngine.calculateThermoregulatoryMaintenanceMultiplier(-5.0, 60.0);
        assertTrue(coldCost > 1.0, "Cold temperatures must increase somatic maintenance energy dissipation.");

        // Small mass (high SA/M) pays higher cold maintenance than large mass (low SA/M)
        double smallColdCost = BergmannAllenMorphologyEngine.calculateThermoregulatoryMaintenanceMultiplier(-5.0, 40.0);
        assertTrue(smallColdCost > coldCost, "Smaller bodies with higher SA/M must dissipate more heat in cold environments.");
    }

    @Test
    @DisplayName("2.3 500-Cell Latitudinal Thermal Gradient Adaptation Simulation")
    public void test500CellLatitudinalAdaptation() {
        int cellCount = 500;
        List<H3Cell> cells = new ArrayList<>(cellCount);

        for (int i = 0; i < cellCount; i++) {
            H3Cell cell = new H3Cell();
            cell.setH3Index((long) (200000 + i));
            cell.setPopulation(1000);
            cell.setFoodResource(2000.0);

            // Latitudinal temperature gradient: from -15°C (polar) to +32°C (equatorial)
            double tempC = -15.0 + (47.0 * i) / (cellCount - 1);
            cell.setTemperature(tempC);
            cells.add(cell);
        }

        // Polar cells (index 0 to 50): adapted body mass must be heavier (> 55 kg)
        for (int i = 0; i < 50; i++) {
            double massKg = BergmannAllenMorphologyEngine.getAdaptedBodyMassKg(cells.get(i).getTemperature(), false);
            assertTrue(massKg > 55.0, "Polar population must adapt to larger body mass (> 55 kg). Observed: " + massKg);
        }

        // Equatorial cells (index 450 to 500): adapted body mass must be lighter (< 45 kg)
        for (int i = 450; i < 500; i++) {
            double massKg = BergmannAllenMorphologyEngine.getAdaptedBodyMassKg(cells.get(i).getTemperature(), false);
            assertTrue(massKg < 45.0, "Equatorial population must adapt to lighter body mass (< 45 kg). Observed: " + massKg);
        }

        // Simulate multi-generational morphological adaptation over 10 years
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(cells.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(cells, buf, false);
        BergmannAllenMorphologyEngine engine = new BergmannAllenMorphologyEngine();
        for (int yr = 0; yr < 10; yr++) {
            engine.tick(buf, 1.0, 12.0);
        }

        // Cold polar cells must experience higher thermoregulatory food consumption than warm cells
        assertTrue(buf.getFoodResource()[0] < buf.getFoodResource()[499],
                "Cold polar cells must dissipate more thermoregulatory energy than warm equatorial cells.");
    }

    @Test
    @DisplayName("2.4 Bitwise Determinism Verification")
    public void testBitwiseDeterminism() {
        org.ether.society.core.dod.WorldBuffer run1 = createDomain(100);
        org.ether.society.core.dod.WorldBuffer run2 = createDomain(100);

        BergmannAllenMorphologyEngine engine1 = new BergmannAllenMorphologyEngine();
        BergmannAllenMorphologyEngine engine2 = new BergmannAllenMorphologyEngine();
        for (int yr = 0; yr < 5; yr++) {
            engine1.tick(run1, 1.0, 12.0);
            engine2.tick(run2, 1.0, 12.0);
        }

        for (int i = 0; i < 100; i++) {
            assertEquals(run1.getFoodResource()[i], run2.getFoodResource()[i], 0.0f,
                    "Morphological adaptation trajectories must be bitwise identical.");
        }
    }

    private org.ether.society.core.dod.WorldBuffer createDomain(int count) {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(count);
        for (int i = 0; i < count; i++) {
            buf.getH3Indexes()[i] = 60000L + i;
            buf.getBiomassHuman()[i] = 500.0f * 0.06f;
            buf.getFoodResource()[i] = 1500.0f;
            buf.getTemperature()[i] = (i % 2 == 0) ? -5.0f : 28.0f;
        }
        return buf;
    }
}
