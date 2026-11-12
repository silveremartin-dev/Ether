package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adversarial unit test suite for {@link LanguageLinguisticEngine}.
 */
public class LanguageLinguisticEngineTest {

    @Test
    public void testMountainIslandIsolationPreservesLanguage() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomassHuman()[0] = 1000.0f;
        buf.getElevation()[0] = 2500.0f; // High mountain isolate

        DerivativeAccumulator acc = new DerivativeAccumulator();
        new LanguageLinguisticEngine().accumulateCellularDerivatives(buf, 0, 1.0, 1.0, acc);

        assertEquals(0.0f, acc.dInstitutionalComplexity, 1e-6f,
                "Cultural isolate must actively dampen institutional/linguistic drift");
    }

    @Test
    public void testDialectDivergenceOnInlandCorridor() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomassHuman()[0] = 5000.0f;
        buf.getElevation()[0] = 200.0f;

        DerivativeAccumulator acc = new DerivativeAccumulator();
        new LanguageLinguisticEngine().accumulateCellularDerivatives(buf, 0, 1.0, 1.0, acc);

        assertTrue(acc.dInstitutionalComplexity > 0.0f,
                "Connected plain must accumulate linguistic/institutional drift");
    }

    @Test
    public void testZeroAndEmptyBufferRobustness() {
        LanguageLinguisticEngine engine = new LanguageLinguisticEngine();
        assertDoesNotThrow(() -> engine.tick(null, 1.0, 1.0));
        assertDoesNotThrow(() -> engine.tick(new WorldBuffer(0), 1.0, 1.0));

        WorldBuffer zeroPop = new WorldBuffer(1);
        DerivativeAccumulator acc = new DerivativeAccumulator();
        assertDoesNotThrow(() -> engine.accumulateCellularDerivatives(zeroPop, 0, 1.0, 1.0, acc),
                "Linguistic engine must cleanly tolerate zero-population cells");
        assertEquals(0.0f, acc.dInstitutionalComplexity, 1e-6f);
    }
}
