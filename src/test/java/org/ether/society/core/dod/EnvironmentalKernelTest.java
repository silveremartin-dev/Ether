package org.ether.society.core.dod;

import org.ether.society.model.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentalKernelTest {

    private EnvironmentalKernel environmentalKernel;
    private WorldBuffer worldBuffer;

    @BeforeEach
    void setUp() {
        environmentalKernel = new EnvironmentalKernel();
        worldBuffer = new WorldBuffer(2);

        // Cell 0: Jungle (warm, rainy)
        worldBuffer.getBiomes()[0] = (byte) Biome.JUNGLE.ordinal();
        worldBuffer.getTemperature()[0] = 22.0f;
        worldBuffer.getRainfall()[0] = 1000.0f;

        // Cell 1: Desert (hot, dry)
        worldBuffer.getBiomes()[1] = (byte) Biome.DESERT.ordinal();
        worldBuffer.getTemperature()[1] = 40.0f;
        worldBuffer.getRainfall()[1] = 50.0f;
    }

    @Test
    @DisplayName("Food production should grow faster in fertile biomes")
    void testFoodProductionByBiome() {
        environmentalKernel.tick(worldBuffer, 6, 1.0f);

        assertTrue(worldBuffer.getFoodResource()[0] > worldBuffer.getFoodResource()[1],
                "Jungle should generate more food than desert");
    }

    @Test
    @DisplayName("Food resource cap is respected")
    void testFoodCap() {
        worldBuffer.getFoodResource()[0] = 49999.0f;
        for (int i = 0; i < 50; i++) {
            environmentalKernel.tick(worldBuffer, 6, 1.0f);
        }

        assertTrue(worldBuffer.getFoodResource()[0] <= 50000.0f, "Food should not exceed 50000 GJ cap");
    }
}
