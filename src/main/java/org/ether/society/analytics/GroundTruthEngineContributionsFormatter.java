/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Partial Tier 2 engine contributions and parameter delta formatter for
 * {@link GroundTruthAuditReportBuilder}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
final class GroundTruthEngineContributionsFormatter {

    private GroundTruthEngineContributionsFormatter() {
        // Utility formatter
    }

    static void appendCrossScenarioEngineContributionMatrix(
            StringBuilder sb,
            List<EngineContributionReport> reports) {
        sb.append("### 🔀 Cross-Scenario Tier 2 Engine Contribution Matrix (%)\n\n");
        java.util.Set<String> allEngines = new java.util.TreeSet<>();
        for (EngineContributionReport r : reports) {
            for (EngineContributionReport.EngineEntry e : r.getEntries()) {
                allEngines.add(e.engineId());
            }
        }
        if (allEngines.isEmpty()) return;

        sb.append("| Tier 2 Engine |");
        for (EngineContributionReport r : reports) {
            sb.append(" ").append(r.getScenarioName() != null ? r.getScenarioName() : r.getRunId()).append(" |");
        }
        sb.append("\n|---|");
        for (int i = 0; i < reports.size(); i++) sb.append("---:|");
        sb.append("\n");

        List<Map<String, EngineContributionReport.EngineEntry>> maps = reports.stream()
                .map(EngineContributionReport::toEngineMap).toList();
        for (String engineId : allEngines) {
            sb.append("| `").append(engineId).append("` |");
            for (Map<String, EngineContributionReport.EngineEntry> m : maps) {
                EngineContributionReport.EngineEntry entry = m.get(engineId);
                if (entry == null) {
                    sb.append(" OFF |");
                } else {
                    sb.append(String.format(Locale.US, " %.2f%% |", entry.globalVarianceSharePct()));
                }
            }
            sb.append("\n");
        }
        sb.append("\n");
    }
}
