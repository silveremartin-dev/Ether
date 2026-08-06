/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.gpu;

import org.ether.society.procedural.jit.CompiledEngineKernel;
import org.ether.society.procedural.jit.SymbolicExpression;

import java.util.Map;

/**
 * Generator for OpenCL / SPIR-V GPU Compute Kernels.
 * Converts symbolic fused engine expressions into high-performance OpenCL C shader code
 * for execution on graphics hardware when available.
 */
public class GPUFusedKernelGenerator {

    /**
     * Generates OpenCL C source code for the fused engine scenario steps.
     */
    public static String generateOpenCLKernelSource(CompiledEngineKernel compiledKernel) {
        StringBuilder sb = new StringBuilder();

        sb.append("/* Auto-Generated Fused Scenario OpenCL Kernel - Ether Engine */\n");
        sb.append("__kernel void executeFusedScenarioStep(\n");
        sb.append("    __global float* temperature,\n");
        sb.append("    __global float* biomassHuman,\n");
        sb.append("    __global float* resourceCapital,\n");
        sb.append("    __global float* technologyLevel,\n");
        sb.append("    const int capacity,\n");
        sb.append("    const float dt\n");
        sb.append(") {\n");
        sb.append("    int id = get_global_id(0);\n");
        sb.append("    if (id >= capacity) return;\n\n");

        if (compiledKernel != null && compiledKernel.getFusedExpressions() != null) {
            Map<String, SymbolicExpression> exprs = compiledKernel.getFusedExpressions();

            if (exprs.containsKey("temperature")) {
                SymbolicExpression expr = exprs.get("temperature");
                sb.append(String.format("    temperature[id] = %.6ff * temperature[id] + (%.6ff * dt);\n",
                        expr.getCoefficient(), expr.getOffset()));
            }

            if (exprs.containsKey("biomassHuman")) {
                SymbolicExpression expr = exprs.get("biomassHuman");
                sb.append(String.format("    float newPop = %.6ff * biomassHuman[id] + (%.6ff * dt);\n",
                        expr.getCoefficient(), expr.getOffset()));
                sb.append("    biomassHuman[id] = (newPop > 0.0f) ? newPop : 0.0f;\n");
            }

            if (exprs.containsKey("resourceCapital")) {
                SymbolicExpression expr = exprs.get("resourceCapital");
                sb.append(String.format("    float newCap = %.6ff * resourceCapital[id] + (%.6ff * dt);\n",
                        expr.getCoefficient(), expr.getOffset()));
                sb.append("    resourceCapital[id] = (newCap > 0.0f) ? newCap : 0.0f;\n");
            }

            if (exprs.containsKey("technologyLevel")) {
                SymbolicExpression expr = exprs.get("technologyLevel");
                sb.append(String.format("    float newTech = %.6ff * technologyLevel[id] + (%.6ff * dt);\n",
                        expr.getCoefficient(), expr.getOffset()));
                sb.append("    technologyLevel[id] = (newTech > 0.0f) ? newTech : 0.0f;\n");
            }
        }

        sb.append("}\n");

        return sb.toString();
    }
}
