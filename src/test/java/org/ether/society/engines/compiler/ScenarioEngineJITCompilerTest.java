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
}
