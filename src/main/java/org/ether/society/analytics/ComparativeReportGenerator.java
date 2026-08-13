/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates formatted comparative analytical reports in Markdown, HTML, and CSV.
 */
public class ComparativeReportGenerator {

    public static String generateMarkdownReport(RootCauseAnalyzer.ComparisonResult result) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        sb.append("# 📊 Rapport d'Analyse Comparative & Métriques de Sensibilité\n\n");
        sb.append("**Date de Génération** : ").append(java.time.LocalDateTime.now().format(dtf)).append("\n\n");

        sb.append("## 📌 1. Contexte du Benchmark\n");
        sb.append("- **Scénario de Référence (Baseline)** : `").append(result.getBaselineRun().getScenarioName()).append("` (`").append(result.getBaselineRun().getRunId()).append("`)\n");
        sb.append("- **Scénario de Comparaison (Cible)** : `").append(result.getTargetRun().getScenarioName()).append("` (`").append(result.getTargetRun().getRunId()).append("`)\n\n");

        sb.append("## 🔍 2. Synthèse Explicative & Cause Racinaire\n");
        sb.append("> ").append(result.getPrimaryRootCauseExplanation()).append("\n\n");

        if (result.getDivergenceYear() != -1) {
            sb.append("**Point de Rupture ($T_{divergence}$)** : An ").append(result.getDivergenceYear()).append("\n\n");
        }

        sb.append("### Matrice des Perturbations / Variations Initiale :\n");
        if (result.getKeyDifferences().isEmpty()) {
            sb.append("- *Aucune différence paramétrique enregistrée (Scénarios identiques).* \n\n");
        } else {
            for (String diff : result.getKeyDifferences()) {
                sb.append("- ").append(diff).append("\n");
            }
            sb.append("\n");
        }

        sb.append("## 📈 3. Évolution des Deltas de Téléométrie\n\n");
        sb.append("| Indicateur | Référence (Baseline) | Cible (Target) | Variation Absolue | Variation (%) |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- |\n");

        for (RootCauseAnalyzer.MetricDelta delta : result.getFinalDeltas()) {
            sb.append(String.format("| %s | %,.1f | %,.1f | %+.1f | %+.2f%% |\n",
                delta.getMetricName(),
                delta.getBaselineValue(),
                delta.getTargetValue(),
                delta.getAbsoluteChange(),
                delta.getPercentageChange()));
        }

        sb.append("\n## 🛠️ 4. Recommandations de Correction Automatisée (\"Corriger\")\n\n");
        if (result.getProposedCorrections().isEmpty()) {
            sb.append("✅ *Aucune correction paramétrique requise. La trajectoire est conforme aux tolérances.* \n\n");
        } else {
            sb.append("| Paramètre à Ajuster | Valeur Actuelle | Valeur Proposée | Justification & Impact Estímé |\n");
            sb.append("| :--- | :--- | :--- | :--- |\n");
            for (RootCauseAnalyzer.ParameterCorrection corr : result.getProposedCorrections()) {
                sb.append(String.format("| `%s` | `%s` | `%s` | %s |\n",
                    corr.getParameterName(),
                    corr.getCurrentValue(),
                    corr.getProposedValue(),
                    corr.getRationale()));
            }
            sb.append("\n");
        }

        sb.append("---\n*Rapport généré automatiquement par Ether Analytics Engine (v4.5.0)*\n");
        return sb.toString();
    }

    public static String generateCsvExport(List<SimulationRunRecord> runs) {
        StringBuilder sb = new StringBuilder();
        sb.append("RunId,ScenarioName,Year,Population,Food,AvgTech,Stability,PopulatedCells\n");

        for (SimulationRunRecord run : runs) {
            for (var entry : run.getTimeSeriesData().entrySet()) {
                int year = entry.getKey();
                var snap = entry.getValue();
                sb.append(String.format("%s,\"%s\",%d,%d,%.2f,%.2f,%.2f,%d\n",
                    run.getRunId(),
                    run.getScenarioName(),
                    year,
                    snap.getPopulation(),
                    snap.getFood(),
                    snap.getAvgTech(),
                    snap.getStability(),
                    snap.getPopulatedCellCount()));
            }
        }
        return sb.toString();
    }
}
