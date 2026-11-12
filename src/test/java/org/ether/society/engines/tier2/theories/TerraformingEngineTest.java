package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        WorldBuffer buf = new WorldBuffer(1);
        buf.getTemperature()[0] = 15.0f;
        buf.getBiomassHuman()[0] = 5000.0f;

        // CO2 doubled (560 ppm) -> radiative forcing ~ 5.35 * ln(2) = 3.71 W/m²
        TerraformingEngine.setCo2Ppm(560.0);
        new TerraformingEngine().tick(buf, 1.0, 1.0);

        assertTrue(buf.getTemperature()[0] > 15.0f, "Increased CO2 must produce positive radiative warming");
        assertTrue(Double.isFinite(buf.getTemperature()[0]), "Temperature must remain finite");
    }

    @Test
    public void testSingularityGuardsOnCo2AndEmptyCells() {
        TerraformingEngine engine = new TerraformingEngine();
        TerraformingEngine.setCo2Ppm(0.0);
        WorldBuffer buf = new WorldBuffer(1);
        buf.getTemperature()[0] = 15.0f;

        assertDoesNotThrow(() -> engine.tick(buf, 1.0, 1.0),
                "CO2 <= 0 and zero population must not throw an exception");

        assertTrue(Double.isFinite(buf.getTemperature()[0]), "Temperature must not become -Infinity or NaN with 0 CO2");

        assertDoesNotThrow(() -> engine.tick(null, 1.0, 1.0));
        assertDoesNotThrow(() -> engine.tick(new WorldBuffer(0), 1.0, 1.0));
    }

    @Test
    public void testAsteroidMiningMetalFlux() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomassHuman()[0] = 10000.0f;
        buf.getMetalResource()[0] = 50.0f;

        TerraformingEngine.setAsteroidMiningFluxTonnesPerYear(100000.0);
        new TerraformingEngine().tick(buf, 1.0, 1.0);

        assertTrue(buf.getMetalResource()[0] > 50.0f, "Asteroid mining flux must enrich cell metal reserves");
    }
}
