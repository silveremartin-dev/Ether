package org.ether.society.core.dod;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldBufferTest {

    private WorldBuffer worldBuffer;

    @BeforeEach
    void setUp() {
        worldBuffer = new WorldBuffer(100);
    }

    @Test
    @DisplayName("WorldBuffer should allocate correct array capacities")
    void testBufferCapacities() {
        assertEquals(100, worldBuffer.getCapacity());
        assertEquals(100, worldBuffer.getH3Indexes().length);
        assertEquals(100, worldBuffer.getElevation().length);
        assertEquals(100, worldBuffer.getTemperature().length);
        assertEquals(100, worldBuffer.getRainfall().length);
        assertEquals(100, worldBuffer.getBiomes().length);
        assertEquals(100, worldBuffer.getFoodResource().length);
        assertEquals(100, worldBuffer.getBiomassHuman().length);
        assertEquals(100, worldBuffer.getNeighborIndexes().length);
        assertEquals(6, worldBuffer.getNeighborIndexes()[0].length);
    }

    @Test
    @DisplayName("WorldBuffer arrays can be populated and read")
    void testDataAccess() {
        worldBuffer.getElevation()[5] = 1250.5f;
        worldBuffer.getFoodResource()[5] = 300.0f;
        worldBuffer.getBiomassHuman()[5] = 50.0f;
        worldBuffer.getH3Indexes()[5] = 0x8828308281fffffL;

        assertEquals(1250.5f, worldBuffer.getElevation()[5]);
        assertEquals(300.0f, worldBuffer.getFoodResource()[5]);
        assertEquals(50.0f, worldBuffer.getBiomassHuman()[5]);
        assertEquals(0x8828308281fffffL, worldBuffer.getH3Indexes()[5]);
    }
}
