package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adversarial and physical verification suite for {@link TerraformingEngine}.
 */
public class TerraformingEngineTest {

    @BeforeEach
    public void setUp() {
        TerraformingEngine.setTerraformingActive(true);
        TerraformingEngine.setCo2Ppm(400.0);
        TerraformingEngine.setSolarMirrorInsolationMultiplier(1.0);
        TerraformingEngine.setAsteroidMiningFluxTonnesPerYear(0.0);
    }

    @AfterEach
    public void tearDown() {
        TerraformingEngine.setTerraformingActive(false);
        TerraformingEngine.setCo2Ppm(280.0);
        TerraformingEngine.setSolarMirrorInsolationMultiplier(1.0);
        TerraformingEngine.setAsteroidMiningFluxTonnesPerYear(0.0);
    }

    @Test
    public void testTerraformingWarmingRadiativeForcing() {
        H3Cell cell = new H3Cell(613503380827930800L, 0.0, 0.0);
        cell.setTemperature(15.0);
        cell.setPopulation(5000);

        // CO2 doubled (560 ppm) -> radiative forcing ~ 5.35 * ln(2) = 3.71 W/m²
        TerraformingEngine.setCo2Ppm(560.0);
        TerraformingEngine.processTerraforming(List.of(cell), 1.0);

        assertTrue(cell.getTemperature() > 15.0, "Increased CO2 must produce positive radiative warming");
        assertTrue(Double.isFinite(cell.getTemperature()), "Temperature must remain finite");
    }

    @Test
    public void testSingularityGuardsOnCo2AndEmptyCells() {
        // Edge case: co2Ppm <= 0 must not produce -Infinity or NaN
        TerraformingEngine.setCo2Ppm(0.0);
        H3Cell cell = new H3Cell(613503380827930801L, 10.0, 10.0);
        cell.setTemperature(15.0);
        cell.setPopulation(null); // Null population

        assertDoesNotThrow(() -> {
            TerraformingEngine.processTerraforming(List.of(cell), 1.0);
        }, "CO2 <= 0 and null population must not throw an exception");

        assertTrue(Double.isFinite(cell.getTemperature()), "Temperature must not become -Infinity or NaN with 0 CO2");

        // Edge case: empty or null list
        assertDoesNotThrow(() -> TerraformingEngine.processTerraforming(null, 1.0));
        assertDoesNotThrow(() -> TerraformingEngine.processTerraforming(new ArrayList<>(), 1.0));
    }

    @Test
    public void testAsteroidMiningMetalFlux() {
        H3Cell cell = new H3Cell(613503380827930802L, 20.0, 20.0);
        cell.setPopulation(10000);
        cell.setResourceMetal(50.0);

        TerraformingEngine.setAsteroidMiningFluxTonnesPerYear(100000.0);
        TerraformingEngine.processTerraforming(List.of(cell), 1.0);

        assertTrue(cell.getResourceMetal() > 50.0, "Asteroid mining flux must enrich cell metal reserves");
    }
}
