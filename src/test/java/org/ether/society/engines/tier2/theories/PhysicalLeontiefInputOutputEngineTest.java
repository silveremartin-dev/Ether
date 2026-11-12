package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adversarial unit test suite for {@link PhysicalLeontiefInputOutputEngine}.
 */
public class PhysicalLeontiefInputOutputEngineTest {

    @Test
    public void testLeontiefCapitalExpansionUnderAbundantResources() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomassHuman()[0] = 10000.0f;
        buf.getResourceCapital()[0] = 5000.0f;
        buf.getMetalResource()[0] = 5000.0f;
        buf.getEnergySolar()[0] = 500.0f;
        buf.getEnergyWind()[0] = 500.0f;
        buf.getEnergyFire()[0] = 1000.0f;

        new PhysicalLeontiefInputOutputEngine().tick(buf, 1.0, 1.0);

        assertTrue(buf.getResourceCapital()[0] > 5000.0f, "Abundant inputs must yield positive net capital growth");
        assertTrue(buf.getMetalResource()[0] < 5000.0f, "Metal input must be consumed proportionally to capital output");
    }

    @Test
    public void testLeontiefBottleneckWhenEnergyConstrained() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomassHuman()[0] = 10000.0f;
        buf.getResourceCapital()[0] = 5000.0f;
        buf.getMetalResource()[0] = 5000.0f;
        buf.getEnergySolar()[0] = 0.0f;
        buf.getEnergyWind()[0] = 0.0f;
        buf.getEnergyFire()[0] = 0.0f;

        new PhysicalLeontiefInputOutputEngine().tick(buf, 1.0, 1.0);

        assertTrue(buf.getResourceCapital()[0] - 5000.0f < 5.0f,
                "Zero energy input must bottleneck Leontief production to near-zero baseline capital gain");
    }

    @Test
    public void testNullSafetyWithUninitializedEnergyAndDemographics() {
        PhysicalLeontiefInputOutputEngine engine = new PhysicalLeontiefInputOutputEngine();
        assertDoesNotThrow(() -> engine.tick(null, 1.0, 1.0));
        assertDoesNotThrow(() -> engine.tick(new WorldBuffer(0), 1.0, 1.0));
        assertDoesNotThrow(() -> engine.tick(new WorldBuffer(1), 1.0, 1.0),
                "Engine must safely tolerate zeroed energy and demographic fields without throwing NPE");
    }
}
