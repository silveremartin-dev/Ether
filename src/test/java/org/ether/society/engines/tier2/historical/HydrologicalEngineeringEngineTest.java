package org.ether.society.engines.tier2.historical;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HydrologicalEngineeringEngine}.
 */
public class HydrologicalEngineeringEngineTest {

    @Test
    public void testChinampasTenochtitlanLakeReclamation() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomes()[0] = (byte) Biome.LAKE.ordinal();
        buf.getBiomassHuman()[0] = 2000.0f;
        buf.getResourceCapital()[0] = 200.0f;
        buf.getFoodResource()[0] = 100.0f;
        buf.getSoilOrganicCarbon()[0] = 20.0f;

        new HydrologicalEngineeringEngine().tick(buf, 30.0 / 365.25, 1.0);

        assertEquals((byte) Biome.PLAINS.ordinal(), buf.getBiomes()[0],
                "Lake with high population and capital should convert into Chinampas/PLAINS");
        assertTrue(buf.getFoodResource()[0] > 100.0f, "Chinampas should boost food reserves");
        assertTrue(buf.getSoilOrganicCarbon()[0] >= 100.0f, "Chinampas should enrich soil organic carbon");
    }

    @Test
    public void testAralSeaCollapse() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomes()[0] = (byte) Biome.LAKE.ordinal();
        buf.getRainfall()[0] = 100.0f;
        buf.getTemperature()[0] = 25.0f;
        buf.getResourceCapital()[0] = 200.0f;
        buf.getWaterResource()[0] = 55.0f;

        new HydrologicalEngineeringEngine().tick(buf, 30.0 / 365.25, 1.0);

        assertTrue(buf.getWaterResource()[0] < 55.0f, "Aggressive pumping should drain lake water resource");
        assertEquals((byte) Biome.DESERT.ordinal(), buf.getBiomes()[0],
                "Drained arid lake should collapse into DESERT");
    }

    @Test
    public void testMountainDamConstruction() {
        WorldBuffer buf = new WorldBuffer(1);
        buf.getBiomes()[0] = (byte) Biome.MOUNTAINS.ordinal();
        buf.getElevation()[0] = 500.0f;
        buf.getResourceCapital()[0] = 600.0f;
        buf.getWaterResource()[0] = 100.0f;
        buf.getEnergySolar()[0] = 0.0f;

        new HydrologicalEngineeringEngine().tick(buf, 1.0, 1.0);

        assertTrue(buf.getWaterResource()[0] >= 1000.0f, "Dam must stabilize water reserves");
        assertTrue(buf.getEnergySolar()[0] > 0.0f, "Dam must provide continuous hydro-energy output");
    }

    @Test
    public void testNullAndZeroSafety() {
        HydrologicalEngineeringEngine engine = new HydrologicalEngineeringEngine();
        assertDoesNotThrow(() -> engine.tick(null, 1.0, 1.0));
        assertDoesNotThrow(() -> engine.tick(new WorldBuffer(0), 1.0, 1.0));
        assertDoesNotThrow(() -> engine.tick(new WorldBuffer(1), 1.0, 1.0));
    }
}
