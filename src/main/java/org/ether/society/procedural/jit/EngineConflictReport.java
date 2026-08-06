/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural.jit;

import java.util.ArrayList;
import java.util.List;

/**
 * Report containing detected model incompatibilities, conflicts, and performance recommendations.
 */
public class EngineConflictReport {

    public enum ConflictSeverity {
        INFO,
        WARNING,
        INCOMPATIBLE
    }

    public static class ConflictEntry {
        private final String variableName;
        private final String engineA;
        private final String engineB;
        private final ConflictSeverity severity;
        private final String description;

        public ConflictEntry(String variableName, String engineA, String engineB, ConflictSeverity severity, String description) {
            this.variableName = variableName;
            this.engineA = engineA;
            this.engineB = engineB;
            this.severity = severity;
            this.description = description;
        }

        public String getVariableName() { return variableName; }
        public String getEngineA() { return engineA; }
        public String getEngineB() { return engineB; }
        public ConflictSeverity getSeverity() { return severity; }
        public String getDescription() { return description; }

        @Override
        public String toString() {
            return String.format("[%s] Variable: %s | %s vs %s -> %s", severity, variableName, engineA, engineB, description);
        }
    }

    private final List<ConflictEntry> entries = new ArrayList<>();

    public void addConflict(String variableName, String engineA, String engineB, ConflictSeverity severity, String description) {
        entries.add(new ConflictEntry(variableName, engineA, engineB, severity, description));
    }

    public List<ConflictEntry> getEntries() {
        return entries;
    }

    public boolean hasIncompatibilities() {
        return entries.stream().anyMatch(e -> e.getSeverity() == ConflictSeverity.INCOMPATIBLE);
    }

    public String generateSummary() {
        if (entries.isEmpty()) {
            return "✅ No model conflicts or incompatibilities detected. Engines are fully compatible.";
        }
        StringBuilder sb = new StringBuilder("⚠️ Scenario Engine Diagnostic & Compatibility Report:\n");
        for (ConflictEntry entry : entries) {
            sb.append("  - ").append(entry.toString()).append("\n");
        }
        return sb.toString();
    }
}
