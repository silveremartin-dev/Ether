package org.ether.society.engines.tier2.historical;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LandReclamationEngine}.
 */
public class LandReclamationEngineTest {

    @Test
    public void testDutchPolderization() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomes()[0] = (byte) Biome.BEACH.ordinal();
        buf.getResourceCapital()[0] = 350.0f;
        buf.getSoilOrganicCarbon()[0] = 0.0f;

        new LandReclamationEngine().tick(buf, 1.0, 1.0);

        assertEquals((byte) Biome.PLAINS.ordinal(), buf.getBiomes()[0],
                "Beach with high capital must be polderized into fertile PLAINS");
        assertTrue(buf.getSoilOrganicCarbon()[0] >= 20.0f,
                "Reclaimed polder must have fertile soil organic carbon");
    }

    @Test
    public void testFuturisticSeasteading() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomes()[0] = (byte) Biome.DEEP_OCEAN.ordinal();
        buf.getResourceCapital()[0] = 3000.0f;
        buf.getEnergySolar()[0] = 0.0f;

        new LandReclamationEngine().tick(buf, 1.0, 1.0);

        assertTrue(buf.getEnergySolar()[0] > 0.0f,
                "Deep ocean with extreme capital must establish seasteading solar infrastructure");
    }

    @Test
    public void testNullSafety() {
        LandReclamationEngine engine = new LandReclamationEngine();
        assertDoesNotThrow(() -> engine.tick(null, 1.0, 1.0));
        assertDoesNotThrow(() -> engine.tick(new WorldBuffer(0), 1.0, 1.0));
        assertDoesNotThrow(() -> engine.tick(new WorldBuffer(1), 1.0, 1.0));
    }
}
