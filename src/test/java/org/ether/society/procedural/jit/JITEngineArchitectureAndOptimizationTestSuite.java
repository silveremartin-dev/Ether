/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural.jit;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.gpu.GPUFusedKernelGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Critical JIT Engine Architecture & Optimization Test Suite")
public class JITEngineArchitectureAndOptimizationTestSuite {

    private ScenarioEngineJITCompiler compiler;

    @BeforeEach
    public void setUp() {
        compiler = new ScenarioEngineJITCompiler();
    }

    @Test
    @DisplayName("Verify multi-level chained expression composition h(g(f(x)))")
    public void testMultiLevelExpressionComposition() {
        // f(x) = 1.05 * x + 2.0
        SymbolicExpression f = new SymbolicExpression("biomassHuman", 1.05, 2.0);
        // g(x) = 0.98 * x - 1.0
        SymbolicExpression g = new SymbolicExpression("biomassHuman", 0.98, -1.0);
        // h(x) = 1.10 * x + 0.5
        SymbolicExpression h = new SymbolicExpression("biomassHuman", 1.10, 0.5);

        // Chain: h(g(f(x)))
        SymbolicExpression gf = f.compose(g);
        SymbolicExpression hgf = gf.compose(h);

        // Manual math check for x = 100:
        // f(100) = 105 + 2 = 107
        // g(107) = 107 * 0.98 - 1 = 104.86 - 1 = 103.86
        // h(103.86) = 103.86 * 1.10 + 0.5 = 114.246 + 0.5 = 114.746
        double expected = 114.746;
        double actual = hgf.evaluate(100.0);

        assertEquals(expected, actual, 1e-4, "Chained AST expression evaluation should be mathematically exact");
    }

    @Test
    @DisplayName("Verify numerical equivalence between sequential uncompiled execution and JIT fused kernel")
    public void testSequentialVsFusedEquivalence() {
        int cellCount = 1000;
        WorldBuffer bufferUncompiled = new WorldBuffer(cellCount);
        WorldBuffer bufferFused = new WorldBuffer(cellCount);

        float[] tempUncompiled = bufferUncompiled.getTemperature();
        float[] tempFused = bufferFused.getTemperature();

        for (int i = 0; i < cellCount; i++) {
            tempUncompiled[i] = 15.0f + (i % 10);
            tempFused[i] = 15.0f + (i % 10);
        }

        // Register 3 engines
        compiler.registerEngineStep("SolarInsolation", "temperature", new SymbolicExpression("temperature", 1.02, 0.5), 25.0);
        compiler.registerEngineStep("AtmosphericRadiative", "temperature", new SymbolicExpression("temperature", 0.99, -0.2), 25.0);
        compiler.registerEngineStep("AlbedoFeedback", "temperature", new SymbolicExpression("temperature", 1.01, 0.1), 25.0);

        // 1. Run Fused Kernel
        compiler.compile();
        compiler.execute(bufferFused, 1.0f);

        // 2. Run Uncompiled Sequential Steps
        for (int i = 0; i < cellCount; i++) {
            double t = tempUncompiled[i];
            t = 1.02 * t + 0.5;
            t = 0.99 * t - 0.2;
            t = 1.01 * t + 0.1;
            tempUncompiled[i] = (float) t;
        }

        // Compare all cells
        for (int i = 0; i < cellCount; i++) {
            assertEquals(tempUncompiled[i], tempFused[i], 1e-4f, 
                    "Fused JIT kernel output must be numerically identical to sequential execution at cell " + i);
        }
    }

    @Test
    @DisplayName("Verify zero population clamp and boundary safety")
    public void testBoundarySafetyAndZeroClamping() {
        WorldBuffer buffer = new WorldBuffer(5);
        float[] pop = buffer.getBiomassHuman();
        pop[0] = 0.0f;
        pop[1] = 10.0f;
        pop[2] = 100.0f;
        pop[3] = 0.0001f;

        // Extreme negative delta engine
        compiler.registerEngineStep("CatastropheEngine", "biomassHuman", new SymbolicExpression("biomassHuman", 0.1, -500.0), 0.0);
        compiler.compile();
        compiler.execute(buffer, 1.0f);

        for (int i = 0; i < 5; i++) {
            assertTrue(pop[i] >= 0.0f, "Biomass human should never become negative after fused execution");
        }
    }

    @Test
    @DisplayName("Verify static conflict analyzer correctly flags divergent target equilibriums")
    public void testConflictAnalyzerRules() {
        // Redundant engines (INFO)
        compiler.registerEngineStep("Engine1", "resourceCapital", new SymbolicExpression("resourceCapital", 1.05, 0.0), 50.0);
        compiler.registerEngineStep("Engine2", "resourceCapital", new SymbolicExpression("resourceCapital", 1.05, 0.0), 50.0);

        // Incompatible engines (INCOMPATIBLE)
        compiler.registerEngineStep("Engine3_Explosion", "technologyLevel", new SymbolicExpression("technologyLevel", 1.20, 0.0), 1000.0);
        compiler.registerEngineStep("Engine4_Collapse", "technologyLevel", new SymbolicExpression("technologyLevel", 0.80, 0.0), 10.0);

        CompiledEngineKernel kernel = compiler.compile();
        EngineConflictReport report = compiler.getConflictReport();

        assertNotNull(kernel);
        assertNotNull(report);
        assertTrue(report.hasIncompatibilities(), "Divergent technology targets (1000 vs 10) must be flagged as INCOMPATIBLE");

        boolean foundInfo = report.getEntries().stream().anyMatch(e -> e.getSeverity() == EngineConflictReport.ConflictSeverity.INFO);
        assertTrue(foundInfo, "Identical engines must be flagged as INFO for potential fusion/deduplication");
    }

    @Test
    @DisplayName("Verify GPU OpenCL kernel generator emits valid OpenCL C code")
    public void testGPUKernelGeneratorOutput() {
        compiler.registerEngineStep("SolarInsolation", "temperature", new SymbolicExpression("temperature", 1.02, 0.5), 25.0);
        compiler.registerEngineStep("GrowthEngine", "biomassHuman", new SymbolicExpression("biomassHuman", 1.05, 10.0), 1000.0);

        CompiledEngineKernel kernel = compiler.compile();
        String openClCode = GPUFusedKernelGenerator.generateOpenCLKernelSource(kernel);

        assertNotNull(openClCode);
        assertTrue(openClCode.contains("__kernel void executeFusedScenarioStep"), "Generated OpenCL kernel should contain kernel entry point");
        assertTrue(openClCode.contains("temperature[id]"), "Generated OpenCL kernel should manipulate temperature array");
        assertTrue(openClCode.contains("biomassHuman[id]"), "Generated OpenCL kernel should manipulate biomassHuman array");
        assertTrue(openClCode.contains("get_global_id(0)"), "Generated OpenCL kernel should query global thread ID");
    }

    @Test
    @DisplayName("Verify DOD array memory sweep performance over 100,000 cells")
    public void testHighCapacityDODArrayPerformance() {
        int capacity = 100_000;
        WorldBuffer buffer = new WorldBuffer(capacity);
        float[] temp = buffer.getTemperature();
        float[] pop = buffer.getBiomassHuman();
        float[] cap = buffer.getResourceCapital();

        for (int i = 0; i < capacity; i++) {
            temp[i] = 20.0f;
            pop[i] = 50.0f;
            cap[i] = 10.0f;
        }

        compiler.registerEngineStep("EngineA", "temperature", new SymbolicExpression("temperature", 1.01, 0.1), 25.0);
        compiler.registerEngineStep("EngineB", "biomassHuman", new SymbolicExpression("biomassHuman", 1.02, 0.5), 500.0);
        compiler.registerEngineStep("EngineC", "resourceCapital", new SymbolicExpression("resourceCapital", 1.03, 1.0), 100.0);

        compiler.compile();

        long startTime = System.nanoTime();
        // Execute 100 simulation ticks
        for (int tick = 0; tick < 100; tick++) {
            compiler.execute(buffer, 1.0f);
        }
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        assertTrue(durationMs < 500, "100 ticks over 100,000 cells with fused JIT kernel should execute in under 500ms (took " + durationMs + "ms)");
        assertTrue(pop[0] > 50.0f, "Population should have grown after 100 ticks");
    }
}
