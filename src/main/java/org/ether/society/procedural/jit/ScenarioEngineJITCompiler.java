/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural.jit;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scenario Engine JIT Compiler.
 * Performs AST reduction, constant folding, static model incompatibility analysis,
 * and Kernel Fusion for CPU execution of procedural scenarios.
 */
public class ScenarioEngineJITCompiler {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioEngineJITCompiler.class);

    private final Map<String, List<SymbolicExpression>> pipelineExpressions = new HashMap<>();
    private final List<EngineConflictAnalyzer.EngineVariableDescriptor> descriptors = new ArrayList<>();
    private CompiledEngineKernel compiledKernel;
    private EngineConflictReport conflictReport;

    /**
     * Registers a engine transformation step for a specific target state variable.
     */
    public void registerEngineStep(String engineName, String targetVariable, SymbolicExpression expression, double targetEquilibrium) {
        pipelineExpressions.computeIfAbsent(targetVariable, k -> new ArrayList<>()).add(expression);
        descriptors.add(new EngineConflictAnalyzer.EngineVariableDescriptor(
                engineName, targetVariable, expression.getCoefficient() - 1.0, targetEquilibrium
        ));
    }

    /**
     * Compiles the registered scenario engine pipeline.
     * Performs static conflict analysis, AST reduction, constant folding, and Kernel Fusion.
     */
    public CompiledEngineKernel compile() {
        logger.info("⚙️ Starting Scenario Engine JIT Compilation & AST Reduction...");

        // 1. Static Conflict Analysis
        this.conflictReport = EngineConflictAnalyzer.analyze(descriptors);
        if (conflictReport.hasIncompatibilities()) {
            logger.warn("⚠️ Severe model incompatibilities detected during scenario JIT compilation:\n{}", conflictReport.generateSummary());
        } else {
            logger.info("✅ Static analysis completed cleanly:\n{}", conflictReport.generateSummary());
        }

        // 2. AST Reduction & Composition (Constant Folding)
        this.compiledKernel = new CompiledEngineKernel();

        for (Map.Entry<String, List<SymbolicExpression>> entry : pipelineExpressions.entrySet()) {
            String varName = entry.getKey();
            List<SymbolicExpression> exprList = entry.getValue();

            if (exprList.isEmpty()) continue;

            // Compose expressions sequentially: g(f(x))
            SymbolicExpression fused = exprList.get(0);
            for (int i = 1; i < exprList.size(); i++) {
                fused = fused.compose(exprList.get(i));
            }

            logger.info("  ⚡ Fused [{}] from {} engine steps -> {}", varName, exprList.size(), fused);
            compiledKernel.addFusedExpression(varName, fused);
        }

        return compiledKernel;
    }

    public EngineConflictReport getConflictReport() {
        return conflictReport;
    }

    public CompiledEngineKernel getCompiledKernel() {
        return compiledKernel;
    }

    /**
     * Executes the compiled fused kernel over a WorldBuffer.
     */
    public void execute(WorldBuffer worldBuffer, float dt) {
        if (compiledKernel == null) {
            compile();
        }
        compiledKernel.executeFusedKernel(worldBuffer, dt);
    }

    /**
     * Executes the compiled fused kernel over a list of H3Cells.
     */
    public void execute(List<H3Cell> cells, double dt) {
        if (compiledKernel == null) {
            compile();
        }
        compiledKernel.executeFusedKernel(cells, dt);
    }
}
