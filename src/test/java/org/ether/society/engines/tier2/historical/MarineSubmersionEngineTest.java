package org.ether.society.engines.tier2.historical;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adversarial unit test suite for {@link MarineSubmersionEngine}.
 */
public class MarineSubmersionEngineTest {

    @Test
    public void testCoastalFloodingAndEvacuation() {
        WorldBuffer buf = new WorldBuffer(2);
        buf.getElevation()[0] = -1.0f; // Submerged below sea level
        buf.getBiomassHuman()[0] = 5000.0f;
        buf.getResourceCapital()[0] = 10000.0f;
        buf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();

        buf.getElevation()[1] = 25.0f; // Safe inland neighbor cell
        buf.getBiomassHuman()[1] = 1000.0f;
        buf.getResourceCapital()[1] = 2000.0f;
        buf.getBiomes()[1] = (byte) Biome.PLAINS.ordinal();

        buf.getNeighborIndexes()[0] = new int[]{1};
        buf.getNeighborIndexes()[1] = new int[]{0};

        new MarineSubmersionEngine().tick(buf, 30.0, 30.0);

        assertTrue(buf.getBiomassHuman()[0] < 5000.0f, "Flooded cell must evacuate population");
        assertTrue(buf.getBiomassHuman()[1] > 1000.0f, "Safe neighbor must absorb evacuated refugees");
        assertTrue(buf.getResourceCapital()[1] > 2000.0f, "Refugees must transfer portable capital");
    }

    @Test
    public void testPolderDikeBreachWithLowCapital() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getElevation()[0] = -0.5f;
        buf.getResourceCapital()[0] = 0.0f; // Zero capital to maintain dikes
        buf.getBiomassHuman()[0] = 2000.0f;
        buf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();

        new MarineSubmersionEngine().tick(buf, 30.0, 30.0);

        assertTrue(buf.getBiomassHuman()[0] < 2000.0f, "Submerged cell with zero maintenance capital must suffer casualty/evacuation");
    }

    @Test
    public void testNullSafetyGuards() {
        MarineSubmersionEngine engine = new MarineSubmersionEngine();
        assertDoesNotThrow(() -> engine.tick(null, 30.0, 30.0));
        assertDoesNotThrow(() -> engine.tick(new WorldBuffer(0), 30.0, 30.0));
        assertDoesNotThrow(() -> engine.tick(new WorldBuffer(1), 30.0, 30.0),
                "Marine submersion must not throw when encountering uninitialized buffer");
    }
}
