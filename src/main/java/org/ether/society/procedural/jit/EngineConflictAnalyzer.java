/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural.jit;

import java.util.List;
import java.util.Map;

/**
 * Static analyzer for detecting model incompatibilities, opposing derivatives,
 * and contradictory feedback loops across procedural simulation engines.
 */
public class EngineConflictAnalyzer {

    public static class EngineVariableDescriptor {
        private final String engineName;
        private final String targetVariable;
        private final double rateOrCoeff; // Derivative coefficient (positive or negative)
        private final double targetEquilibrium; // Expected equilibrium bound if any

        public EngineVariableDescriptor(String engineName, String targetVariable, double rateOrCoeff, double targetEquilibrium) {
            this.engineName = engineName;
            this.targetVariable = targetVariable;
            this.rateOrCoeff = rateOrCoeff;
            this.targetEquilibrium = targetEquilibrium;
        }

        public String getEngineName() { return engineName; }
        public String getTargetVariable() { return targetVariable; }
        public double getRateOrCoeff() { return rateOrCoeff; }
        public double getTargetEquilibrium() { return targetEquilibrium; }
    }

    /**
     * Analyzes a set of engine descriptors to detect incompatibilities.
     */
    public static EngineConflictReport analyze(List<EngineVariableDescriptor> descriptors) {
        EngineConflictReport report = new EngineConflictReport();

        // Group by variable
        Map<String, List<EngineVariableDescriptor>> byVariable = descriptors.stream()
                .collect(java.util.stream.Collectors.groupingBy(EngineVariableDescriptor::getTargetVariable));

        for (Map.Entry<String, List<EngineVariableDescriptor>> entry : byVariable.entrySet()) {
            String varName = entry.getKey();
            List<EngineVariableDescriptor> engines = entry.getValue();

            for (int i = 0; i < engines.size(); i++) {
                for (int j = i + 1; j < engines.size(); j++) {
                    EngineVariableDescriptor e1 = engines.get(i);
                    EngineVariableDescriptor e2 = engines.get(j);

                    // Check for opposing unconstrained derivatives
                    if ((e1.getRateOrCoeff() > 0 && e2.getRateOrCoeff() < 0) || (e1.getRateOrCoeff() < 0 && e2.getRateOrCoeff() > 0)) {
                        if (Double.isNaN(e1.getTargetEquilibrium()) && Double.isNaN(e2.getTargetEquilibrium())) {
                            report.addConflict(varName, e1.getEngineName(), e2.getEngineName(),
                                    EngineConflictReport.ConflictSeverity.WARNING,
                                    "Opposing derivatives without bound equilibrium. May cause numerical oscillation.");
                        } else if (Math.abs(e1.getTargetEquilibrium() - e2.getTargetEquilibrium()) > 0.5) {
                            report.addConflict(varName, e1.getEngineName(), e2.getEngineName(),
                                    EngineConflictReport.ConflictSeverity.INCOMPATIBLE,
                                    String.format("Divergent equilibrium targets: %.2f vs %.2f. Models are physically incompatible.",
                                            e1.getTargetEquilibrium(), e2.getTargetEquilibrium()));
                        }
                    }

                    // Check for redundant identical transformations
                    if (Math.abs(e1.getRateOrCoeff() - e2.getRateOrCoeff()) < 1e-6 
                            && Math.abs(e1.getTargetEquilibrium() - e2.getTargetEquilibrium()) < 1e-6) {
                        report.addConflict(varName, e1.getEngineName(), e2.getEngineName(),
                                EngineConflictReport.ConflictSeverity.INFO,
                                "Redundant transformations on the same variable. Can be fused or deduplicated.");
                    }
                }
            }
        }

        return report;
    }
}
