package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adversarial and dynamical verification suite for {@link SpatialMetapopulationSEIREngine}.
 */
public class SpatialMetapopulationSEIREngineTest {

    @Test
    public void testSEIRTransmissionAndDecay() {
        SpatialMetapopulationSEIREngine engine = new SpatialMetapopulationSEIREngine();
        WorldBuffer buffer = new WorldBuffer(1);
        buffer.getHumanPopulation()[0] = 10000.0f;

        // First tick allocates SEIR arrays on capacity=1, then seed infection
        engine.tick(buffer, 0.001, 0.012);
        engine.seedInfection(0, 200.0f);
        engine.tick(buffer, 0.1, 1.2); // ~36 days

        assertTrue(buffer.getHumanPopulation()[0] >= 0.0f, "Surviving population must be non-negative");
        assertTrue(buffer.getHumanPopulation()[0] < 10000.0f, "SEIR virulence fatalities must decrement total population");
    }

    @Test
    public void testInversionResistanceWhenInfectedExceedsPopulation() {
        SpatialMetapopulationSEIREngine engine = new SpatialMetapopulationSEIREngine();
        WorldBuffer buffer = new WorldBuffer(1);
        buffer.getHumanPopulation()[0] = 100.0f;

        engine.tick(buffer, 0.001, 0.012);
        engine.seedInfection(0, 500.0f); // 500 > 100

        assertDoesNotThrow(() -> {
            engine.tick(buffer, 0.05, 0.6);
        }, "Engine must safely clamp infected count without producing negative exposed cohorts");

        assertTrue(buffer.getHumanPopulation()[0] >= 0.0f, "Population must remain non-negative");
        assertTrue(buffer.getHumanPopulation()[0] <= 100.0f, "Population cannot exceed initial cohort under epidemic mortality");
    }

    @Test
    public void testNullSafetyAndZeroPopulation() {
        SpatialMetapopulationSEIREngine engine = new SpatialMetapopulationSEIREngine();

        assertDoesNotThrow(() -> engine.tick(null, 1.0, 12.0));

        WorldBuffer emptyBuffer = new WorldBuffer(1);
        emptyBuffer.getHumanPopulation()[0] = 0.0f;

        assertDoesNotThrow(() -> engine.tick(emptyBuffer, 1.0, 12.0));
    }
}
