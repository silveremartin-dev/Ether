/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.gpu;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.procedural.jit.CompiledEngineKernel;
import org.ether.society.procedural.jit.ScenarioEngineJITCompiler;
import org.ether.society.procedural.jit.SymbolicExpression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GPU Manager Acceleration & Automatic Fallback Test Suite")
public class GPUManagerFallbackTest {

    private GPUManager gpuManager;
    private ScenarioEngineJITCompiler compiler;

    @BeforeEach
    public void setUp() {
        gpuManager = new GPUManager();
        compiler = new ScenarioEngineJITCompiler();
    }

    @Test
    @DisplayName("Verify GPU manager toggle preferences")
    public void testGPUManagerToggle() {
        assertTrue(gpuManager.isGpuEnabled(), "GPU acceleration should be enabled by default");

        gpuManager.setGpuEnabled(false);
        assertFalse(gpuManager.isGpuEnabled(), "GPU acceleration should be disabled after toggle");

        gpuManager.setGpuEnabled(true);
        assertTrue(gpuManager.isGpuEnabled());
    }

    @Test
    @DisplayName("Verify safe automatic CPU fallback when GPU runtime is not present")
    public void testAutomaticCPUFallbackOnFusedKernel() {
        WorldBuffer buffer = new WorldBuffer(100);
        float[] temp = buffer.getTemperature();
        for (int i = 0; i < 100; i++) {
            temp[i] = 20.0f;
        }

        compiler.registerEngineStep("SolarInsolation", "temperature", new SymbolicExpression("temperature", 1.05, 1.0), 25.0);
        CompiledEngineKernel kernel = compiler.compile();

        // Should attempt GPU execution and seamlessly fall back to CPU JIT if GPU native driver is missing
        assertDoesNotThrow(() -> {
            gpuManager.executeFusedKernelWithGPUFallback(kernel, buffer, 1.0f);
        }, "Fused kernel execution must never throw an exception under GPU fallback mode");

        // Verify state updated properly: 20 * 1.05 + 1 = 22.0
        for (int i = 0; i < 100; i++) {
            assertEquals(22.0f, temp[i], 1e-4f, "Cell temperature should be correctly updated by CPU fallback kernel");
        }
    }
}
