/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.engines.compiler;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>Engine Conflict Report</h1>
 * <p>
 * Core operational component for the Ether civilizational and planetary simulation framework.<br>
 * Integrates cellular dynamics, data structures, and deterministic state transitions.
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class EngineConflictReport {

    public enum ConflictSeverity {
        INFO,
        WARNING,
        INCOMPATIBLE
    }

    public static class ConflictEntry {
        /* Internal state variable for variable name (String). */
        private final String variableName;
        /* Internal state variable for engine a (String). */
        private final String engineA;
        /* Internal state variable for engine b (String). */
        private final String engineB;
        private final ConflictSeverity severity;
        /* Internal state variable for description (String). */
        private final String description;

        /*
         * Conflict entry.
         * Enforces physical invariants and updates associated state variables within {@code EngineConflictReport}.
         *
         * @param variableName the variable name parameter (String)
         * @param engineA the engine a parameter (String)
         * @param engineB the engine b parameter (String)
         * @param severity the severity parameter (ConflictSeverity)
         * @param description the description parameter (String)
         * @return the resulting computation or state reference
         */
        public ConflictEntry(String variableName, String engineA, String engineB, ConflictSeverity severity, String description) {
            this.variableName = variableName;
            this.engineA = engineA;
            this.engineB = engineB;
            this.severity = severity;
            this.description = description;
        }

        /*
         * Get variable name.
         * Enforces physical invariants and updates associated state variables within {@code EngineConflictReport}.
         *
         * @return the resulting computation or state reference
         */
        public String getVariableName() { return variableName; }
        /*
         * Get engine a.
         * Enforces physical invariants and updates associated state variables within {@code EngineConflictReport}.
         *
         * @return the resulting computation or state reference
         */
        public String getEngineA() { return engineA; }
        /*
         * Get engine b.
         * Enforces physical invariants and updates associated state variables within {@code EngineConflictReport}.
         *
         * @return the resulting computation or state reference
         */
        public String getEngineB() { return engineB; }
        /*
         * Get severity.
         * Enforces physical invariants and updates associated state variables within {@code EngineConflictReport}.
         *
         * @return the resulting computation or state reference
         */
        public ConflictSeverity getSeverity() { return severity; }
        /*
         * Get description.
         * Enforces physical invariants and updates associated state variables within {@code EngineConflictReport}.
         *
         * @return the resulting computation or state reference
         */
        public String getDescription() { return description; }

        @Override
        /*
         * To string.
         * Enforces physical invariants and updates associated state variables within {@code EngineConflictReport}.
         *
         * @return the resulting computation or state reference
         */
        public String toString() {
            return String.format("[%s] Variable: %s | %s vs %s -> %s", severity, variableName, engineA, engineB, description);
        }
    }

    private final List<ConflictEntry> entries = new ArrayList<>();

    /*
     * Add conflict.
     * Enforces physical invariants and updates associated state variables within {@code EngineConflictReport}.
     *
     * @param variableName the variable name parameter (String)
     * @param engineA the engine a parameter (String)
     * @param engineB the engine b parameter (String)
     * @param severity the severity parameter (ConflictSeverity)
     * @param description the description parameter (String)
     */
    public void addConflict(String variableName, String engineA, String engineB, ConflictSeverity severity, String description) {
        entries.add(new ConflictEntry(variableName, engineA, engineB, severity, description));
    }

    /*
     * Get entries.
     * Enforces physical invariants and updates associated state variables within {@code EngineConflictReport}.
     *
     * @return the resulting computation or state reference
     */
    public List<ConflictEntry> getEntries() {
        return entries;
    }

    /*
     * Has incompatibilities.
     * Enforces physical invariants and updates associated state variables within {@code EngineConflictReport}.
     *
     * @return the resulting computation or state reference
     */
    public boolean hasIncompatibilities() {
        return entries.stream().anyMatch(e -> e.getSeverity() == ConflictSeverity.INCOMPATIBLE);
    }

    /*
     * Generate summary.
     * Enforces physical invariants and updates associated state variables within {@code EngineConflictReport}.
     *
     * @return the resulting computation or state reference
     */
    public String generateSummary() {
        if (entries.isEmpty()) {
            return "âœ… No model conflicts or incompatibilities detected. Engines are fully compatible.";
        }
        StringBuilder sb = new StringBuilder("âš ï¸ Scenario Engine Diagnostic & Compatibility Report:\n");
        for (ConflictEntry entry : entries) {
            sb.append("  - ").append(entry.toString()).append("\n");
        }
        return sb.toString();
    }
}

