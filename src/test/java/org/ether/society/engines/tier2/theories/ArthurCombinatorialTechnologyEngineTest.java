package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adversarial unit test suite for {@link ArthurCombinatorialTechnologyEngine}.
 */
public class ArthurCombinatorialTechnologyEngineTest {

    @Test
    public void testCombinatorialInnovationAdvance() {
        ArthurCombinatorialTechnologyEngine engine = new ArthurCombinatorialTechnologyEngine();
        WorldBuffer buffer = new WorldBuffer(1);
        buffer.getHumanPopulation()[0] = 5000.0f;
        buffer.getResourceCapital()[0] = 1000.0f;

        engine.tick(buffer, 1.0, 12.0);

        assertTrue(buffer.getResourceCapital()[0] > 1000.0f, "Combinatorial innovation must advance productive capital stock");
        assertTrue(Float.isFinite(buffer.getResourceCapital()[0]), "Productive capital stock must remain finite");
    }

    @Test
    public void testNegativeCapitalRobustness() {
        ArthurCombinatorialTechnologyEngine engine = new ArthurCombinatorialTechnologyEngine();
        WorldBuffer buffer = new WorldBuffer(1);
        buffer.getHumanPopulation()[0] = 1000.0f;
        buffer.getResourceCapital()[0] = -100.0f; // Negative capital (debt/deficit)

        assertDoesNotThrow(() -> {
            engine.tick(buffer, 1.0, 12.0);
        }, "Negative capital must not cause Math.sqrt(negative) NaN exception or crash");

        assertFalse(Float.isNaN(buffer.getResourceCapital()[0]), "Capital stock must never become NaN under negative capital");
    }

    @Test
    public void testNullSafetyGuards() {
        ArthurCombinatorialTechnologyEngine engine = new ArthurCombinatorialTechnologyEngine();
        assertDoesNotThrow(() -> engine.tick(null, 1.0, 12.0));

        WorldBuffer zeroBuffer = new WorldBuffer(1);
        zeroBuffer.getHumanPopulation()[0] = 0.0f;
        zeroBuffer.getResourceCapital()[0] = 0.0f;

        assertDoesNotThrow(() -> {
            engine.tick(zeroBuffer, 1.0, 12.0);
        }, "Engine must safely process zeroed cells without throwing exceptions");
    }
}
