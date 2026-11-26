/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.engines.compiler;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * High-Performance CPU Fused Execution Kernel.
 * Evaluates simplified symbolic expressions over DOD arrays in a single CPU pass (Kernel Fusion).
 */
public class CompiledEngineKernel {

    private final Map<String, SymbolicExpression> fusedExpressions = new HashMap<>();

    /**
     * Add fused expression.
     *
     * @param variableName the variable name parameter (String)
     * @param expr the expr parameter (SymbolicExpression)
     */
    public void addFusedExpression(String variableName, SymbolicExpression expr) {
        fusedExpressions.put(variableName, expr);
    }

    /**
     * Get fused expressions.
     *
     * @return the resulting computation or state reference
     */
    public Map<String, SymbolicExpression> getFusedExpressions() {
        return fusedExpressions;
    }

    /**
     * Executes Kernel Fusion over Data-Oriented WorldBuffer in a single CPU pass.
     * Memory bandwidth optimized (1 read/write pass for N fused engine steps).
     */
    public void executeFusedKernel(WorldBuffer worldBuffer, float dt) {
        if (worldBuffer == null) return;
        int capacity = worldBuffer.getCapacity();

        // 1. Oxygen / Atmosphere
        SymbolicExpression oxyExpr = fusedExpressions.get("temperature");
        if (oxyExpr != null) {
            float[] temp = worldBuffer.getTemperature();
            double coeff = oxyExpr.getCoefficient();
            double offset = oxyExpr.getOffset() * dt;
            for (int i = 0; i < capacity; i++) {
                temp[i] = (float) (coeff * temp[i] + offset);
            }
        }

        // 2. Biomass Human
        SymbolicExpression popExpr = fusedExpressions.get("humanPopulation");
        if (popExpr != null) {
            float[] pop = worldBuffer.getHumanPopulation();
            double coeff = popExpr.getCoefficient();
            double offset = popExpr.getOffset() * dt;
            for (int i = 0; i < capacity; i++) {
                if (pop[i] > 0.001f) {
                    pop[i] = (float) Math.max(0.0, coeff * pop[i] + offset);
                }
            }
        }

        // 3. Resource Capital
        SymbolicExpression capExpr = fusedExpressions.get("resourceCapital");
        if (capExpr != null) {
            float[] cap = worldBuffer.getResourceCapital();
            double coeff = capExpr.getCoefficient();
            double offset = capExpr.getOffset() * dt;
            for (int i = 0; i < capacity; i++) {
                cap[i] = (float) Math.max(0.0, coeff * cap[i] + offset);
            }
        }
    }

    /**
     * To string.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("⚡ Fused Compiled CPU Kernel Expressions:\n");
        fusedExpressions.forEach((var, expr) -> sb.append(String.format("  - %s: %s\n", var, expr)));
        return sb.toString();
    }
}

