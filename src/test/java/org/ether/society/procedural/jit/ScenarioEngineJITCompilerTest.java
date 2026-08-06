/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural.jit;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ScenarioEngineJITCompilerTest {

    private ScenarioEngineJITCompiler compiler;

    @BeforeEach
    public void setUp() {
        compiler = new ScenarioEngineJITCompiler();
    }

    @Test
    public void testSymbolicExpressionComposition() {
        // f(x) = 2.0 * x + 3.0
        SymbolicExpression f = new SymbolicExpression("biomassHuman", 2.0, 3.0);
        // g(x) = 5.0 * x - 1.0
        SymbolicExpression g = new SymbolicExpression("biomassHuman", 5.0, -1.0);

        // g(f(x)) = 5.0 * (2.0 * x + 3.0) - 1.0 = 10.0 * x + 14.0
        SymbolicExpression fused = f.compose(g);

        assertEquals(10.0, fused.getCoefficient(), 1e-6);
        assertEquals(14.0, fused.getOffset(), 1e-6);

        // Evaluate x = 4.0 -> 10*4 + 14 = 54
        assertEquals(54.0, fused.evaluate(4.0), 1e-6);
    }

    @Test
    public void testConflictAnalyzerDetection() {
        // Engine A: Growth (+0.05) with target 1000
        compiler.registerEngineStep("EngineA_Growth", "temperature", new SymbolicExpression("temperature", 1.05, 0.0), 1000.0);
        // Engine B: Cooling (-0.02) with incompatible target 200
        compiler.registerEngineStep("EngineB_Cooling", "temperature", new SymbolicExpression("temperature", 0.98, 0.0), 200.0);

        CompiledEngineKernel kernel = compiler.compile();
        assertNotNull(kernel);

        EngineConflictReport report = compiler.getConflictReport();
        assertNotNull(report);
        assertTrue(report.hasIncompatibilities(), "Should detect incompatible equilibrium targets (1000 vs 200)");
    }

    @Test
    public void testFusedKernelExecutionOverWorldBuffer() {
        WorldBuffer buffer = new WorldBuffer(10);
        float[] temp = buffer.getTemperature();
        float[] pop = buffer.getBiomassHuman();

        for (int i = 0; i < 10; i++) {
            temp[i] = 20.0f;
            pop[i] = 100.0f;
        }

        // Engine 1: Temp * 1.1 + 2
        compiler.registerEngineStep("SolarRadiance", "temperature", new SymbolicExpression("temperature", 1.1, 2.0), 25.0);
        // Engine 2: Temp * 0.9 - 1
        compiler.registerEngineStep("AlbedoCooling", "temperature", new SymbolicExpression("temperature", 0.9, -1.0), 25.0);

        // Fused: (20 * 1.1 + 2) * 0.9 - 1 = 24 * 0.9 - 1 = 21.6 - 1 = 20.6
        compiler.compile();
        compiler.execute(buffer, 1.0f);

        for (int i = 0; i < 10; i++) {
            assertEquals(20.6f, temp[i], 1e-4f);
        }
    }

    @Test
    public void testFusedKernelExecutionOverH3Cells() {
        List<H3Cell> cells = new ArrayList<>();
        H3Cell cell = new H3Cell(0x8828308281fffffL, 0.0, 0.0);
        cell.setPopulation(500);
        cells.add(cell);

        compiler.registerEngineStep("GrowthEngine", "biomassHuman", new SymbolicExpression("biomassHuman", 1.1, 10.0), 1000.0);
        compiler.compile();
        compiler.execute(cells, 1.0);

        // 500 * 1.1 + 10 = 560
        assertEquals(560, cell.getPopulation());
    }
}
