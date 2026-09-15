/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.gpu;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GPUComputeShaderPipelineTest {

    @Test
    public void testComputeShaderInitializationAndDispatch() {
        GPUComputeShaderPipeline pipeline = new GPUComputeShaderPipeline();

        assertTrue(pipeline.isCompiled(), "Compute shader kernel source must be compiled");
        assertNotNull(pipeline.getShaderSource(), "Shader source must not be null");

        List<H3Cell> cells = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            H3Cell c = new H3Cell();
            c.setH3Index((long) (i + 1));
            c.setBiome(Biome.FOREST);
            c.setDynamicAlbedo(0.20);
            c.setTemperature(15.0);
            c.setLatitude(45.0);
            cells.add(c);
        }

        assertDoesNotThrow(() -> {
            pipeline.executeRadiativeEquilibrium(cells, 1361.0, 3.5, 0.0833);
        }, "GPU compute shader execution (or SIMD fallback) must execute smoothly");

        for (H3Cell c : cells) {
            assertTrue(c.getTemperature() > -100.0 && c.getTemperature() < 100.0,
                    "Cell temperatures must be physically bounded after radiative step");
        }
    }
}