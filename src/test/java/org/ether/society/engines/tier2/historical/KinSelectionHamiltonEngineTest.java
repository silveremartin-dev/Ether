package org.ether.society.engines.tier2.historical;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link KinSelectionHamiltonEngine}.
 */
public class KinSelectionHamiltonEngineTest {

    @Test
    public void testHamiltonRuleThreshold() {
        assertTrue(KinSelectionHamiltonEngine.checkHamiltonRule(0.5, 100.0, 40.0), "0.5 * 100 > 40 -> true");
        assertFalse(KinSelectionHamiltonEngine.checkHamiltonRule(0.25, 100.0, 40.0), "0.25 * 100 < 40 -> false");
        assertTrue(KinSelectionHamiltonEngine.checkHamiltonRule(0.125, 1000.0, 100.0), "0.125 * 1000 > 100 -> true");
    }

    @Test
    public void testScarcityKinSupportDerivative() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomassHuman()[0] = 500.0f;
        buf.getFoodResource()[0] = 5.0f; // foodPerCapita = 0.01 < 0.05 -> scarcityStress > 0.01
        buf.getResourceCapital()[0] = 100.0f;

        DerivativeAccumulator acc = new DerivativeAccumulator();
        new KinSelectionHamiltonEngine().accumulateCellularDerivatives(buf, 0, 1.0, 1.0, acc);

        assertTrue(acc.dResourceCapital > 0.0f,
                "Severe scarcity should activate kin-selected mutual aid capital buffer");
    }

    @Test
    public void testNullAndZeroPopulationSafety() {
        KinSelectionHamiltonEngine engine = new KinSelectionHamiltonEngine();
        assertDoesNotThrow(() -> engine.tick(null, 1.0, 1.0));
        assertDoesNotThrow(() -> engine.tick(new WorldBuffer(0), 1.0, 1.0));

        WorldBuffer zeroPop = new WorldBuffer(1);
        DerivativeAccumulator acc = new DerivativeAccumulator();
        assertDoesNotThrow(() -> engine.accumulateCellularDerivatives(zeroPop, 0, 1.0, 1.0, acc));
        assertEquals(0.0f, acc.dResourceCapital, 1e-6f);
    }
}
