/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.compiler;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Symbolic Expression Composition, AST Constant Folding,
 * and JIT Kernel Fusion for differential state transformations.
 */
public class ScenarioEngineJITCompilerTest {

    @Test
    @DisplayName("SymbolicExpression affine composition g(f(x)) evaluates identically to chained operations")
    public void testSymbolicExpressionComposition() {
        // f(x) = 2.0 * x + 5.0
        SymbolicExpression f = new SymbolicExpression("temperature", 2.0, 5.0);
        // g(y) = 3.0 * y - 4.0
        SymbolicExpression g = new SymbolicExpression("temperature", 3.0, -4.0);

        // Chained: g(f(x)) = 3.0 * (2.0 * x + 5.0) - 4.0 = 6.0 * x + 11.0
        SymbolicExpression fused = f.compose(g);

        assertEquals("temperature", fused.getVariableName());
        assertEquals(6.0, fused.getCoefficient(), 1e-6, "Composed coefficient must equal A1 * A2 = 2.0 * 3.0 = 6.0");
        assertEquals(11.0, fused.getOffset(), 1e-6, "Composed offset must equal A2 * B1 + B2 = 3.0 * 5.0 - 4.0 = 11.0");

        double x = 10.0;
        double chainedResult = g.evaluate(f.evaluate(x)); // f(10) = 25, g(25) = 71
        double fusedResult = fused.evaluate(x);            // 6 * 10 + 11 = 71
        assertEquals(71.0, chainedResult, 1e-6);
        assertEquals(chainedResult, fusedResult, 1e-6, "Fused AST expression must evaluate bit-identically to sequential chained evaluation");
    }

    @Test
    @DisplayName("ScenarioEngineJITCompiler compiles pipeline into fused kernel and updates cell state deterministically")
    public void testScenarioJITCompilerExecution() {
        ScenarioEngineJITCompiler compiler = new ScenarioEngineJITCompiler();

        // Step 1: Solar radiative warming: delta = 1.05 * T + 2.0
        compiler.registerEngineStep("SolarRadiation", "temperature",
                new SymbolicExpression("temperature", 1.05, 2.0), 25.0);

        // Step 2: Atmospheric albedo cooling: delta = 0.90 * T - 1.0
        compiler.registerEngineStep("AlbedoCooling", "temperature",
                new SymbolicExpression("temperature", 0.90, -1.0), 20.0);

        CompiledEngineKernel kernel = compiler.compile();
        assertNotNull(kernel, "JIT compiler must produce a non-null compiled kernel");

        EngineConflictReport report = compiler.getConflictReport();
        assertNotNull(report, "Conflict report must be generated during compilation");

        // Fused: 0.90 * (1.05 * T + 2.0) - 1.0 = 0.945 * T + 0.80
        H3Cell cell = new H3Cell(3001L, 45.0, 5.0);
        cell.setTemperature(20.0);

        compiler.execute(List.of(cell), 1.0);

        // Expected temperature: 0.945 * 20.0 + 0.80 = 18.90 + 0.80 = 19.70
        assertEquals(19.70, cell.getTemperature(), 1e-4,
                "JIT fused kernel execution over cell must evaluate fused affine transformation exactly");
    }

    @Test
    @DisplayName("CompiledEngineKernel directly fuses expressions and updates WorldBuffer and H3Cell states")
    public void testCompiledEngineKernelDirectExecution() {
        CompiledEngineKernel kernel = new CompiledEngineKernel();
        kernel.addFusedExpression("temperature", new SymbolicExpression("temperature", 1.1, 2.0));
        kernel.addFusedExpression("biomassHuman", new SymbolicExpression("biomassHuman", 1.05, 10.0));
        kernel.addFusedExpression("resourceCapital", new SymbolicExpression("resourceCapital", 1.02, 5.0));
        kernel.addFusedExpression("technologyLevel", new SymbolicExpression("technologyLevel", 1.01, 0.1));

        assertFalse(kernel.getFusedExpressions().isEmpty());
        assertTrue(kernel.toString().contains("Fused Compiled CPU Kernel Expressions"));

        // 1. Test List<H3Cell> fused execution
        H3Cell cell = new H3Cell(4001L, 10.0, 10.0);
        cell.setTemperature(15.0);
        cell.setPopulation(100);
        cell.setResourceCapital(50.0);
        cell.setTechnologyLevel(2.0);

        kernel.executeFusedKernel(List.of(cell), 1.0);

        assertEquals(1.1 * 15.0 + 2.0, cell.getTemperature(), 1e-4);
        assertEquals((int) (1.05 * 100 + 10.0), cell.getPopulation());
        assertEquals(1.02 * 50.0 + 5.0, cell.getResourceCapital(), 1e-4);
        assertEquals(1.01 * 2.0 + 0.1, cell.getTechnologyLevel(), 1e-4);

        // 2. Test WorldBuffer fused execution
        org.ether.society.core.dod.WorldBuffer wb = new org.ether.society.core.dod.WorldBuffer(4);
        wb.getTemperature()[0] = 10.0f;
        wb.getBiomassHuman()[0] = 50.0f;
        wb.getResourceCapital()[0] = 20.0f;
        wb.getTechnologyLevel()[0] = 1.0f;

        kernel.executeFusedKernel(wb, 1.0f);

        assertEquals(1.1f * 10.0f + 2.0f, wb.getTemperature()[0], 1e-3f);
        assertEquals(1.05f * 50.0f + 10.0f, wb.getBiomassHuman()[0], 1e-3f);
        assertEquals(1.02f * 20.0f + 5.0f, wb.getResourceCapital()[0], 1e-3f);
        assertEquals(1.01f * 1.0f + 0.1f, wb.getTechnologyLevel()[0], 1e-3f);
    }

    @Test
    @DisplayName("ProceduralEnginePlugin interface contract default methods behave consistently")
    public void testProceduralEnginePluginDefaultMethods() {
        org.ether.society.engines.ProceduralEnginePlugin plugin = (cells, dt) -> {
            for (H3Cell c : cells) {
                if (c.getResourceWork() != null) {
                    c.setResourceWork(c.getResourceWork() + dt * 10.0);
                }
            }
        };

        H3Cell cell = new H3Cell(5001L, 0.0, 0.0);
        cell.setResourceWork(50.0);

        plugin.process(List.of(cell), 2.0);
        assertEquals(70.0, cell.getResourceWork(), 1e-4);

        assertNotNull(plugin.getName());
        assertNotNull(plugin.getDescription());
        assertTrue(plugin.getEquationsTooltip().contains("Mathematical"));
        assertEquals("Tier 2: Cliodynamics", plugin.getCategory());
    }
}

