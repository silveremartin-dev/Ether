/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import java.util.*;

/**
 * Analytical engine computing differences, divergence points, and parameter impact explanations.
 */
public class RootCauseAnalyzer {

    public static class MetricDelta {
        private final String metricName;
        private final double baselineValue;
        private final double targetValue;
        private final double absoluteChange;
        private final double percentageChange;

        public MetricDelta(String metricName, double baselineValue, double targetValue) {
            this.metricName = metricName;
            this.baselineValue = baselineValue;
            this.targetValue = targetValue;
            this.absoluteChange = targetValue - baselineValue;
            this.percentageChange = baselineValue != 0 ? (absoluteChange / baselineValue) * 100.0 : 0.0;
        }

        public String getMetricName() { return metricName; }
        public double getBaselineValue() { return baselineValue; }
        public double getTargetValue() { return targetValue; }
        public double getAbsoluteChange() { return absoluteChange; }
        public double getPercentageChange() { return percentageChange; }
    }

    public static class ParameterCorrection {
        private final String parameterName;
        private final String currentValue;
        private final String proposedValue;
        private final double adjustmentFactor;
        private final String rationale;

        public ParameterCorrection(String parameterName, String currentValue, String proposedValue, double adjustmentFactor, String rationale) {
            this.parameterName = parameterName;
            this.currentValue = currentValue;
            this.proposedValue = proposedValue;
            this.adjustmentFactor = adjustmentFactor;
            this.rationale = rationale;
        }

        public String getParameterName() { return parameterName; }
        public String getCurrentValue() { return currentValue; }
        public String getProposedValue() { return proposedValue; }
        public double getAdjustmentFactor() { return adjustmentFactor; }
        public String getRationale() { return rationale; }
    }

    public static class ComparisonResult {
        private final SimulationRunRecord baselineRun;
        private final SimulationRunRecord targetRun;
        private final int divergenceYear;
        private final List<MetricDelta> finalDeltas;
        private final List<String> keyDifferences;
        private final String primaryRootCauseExplanation;
        private final List<ParameterCorrection> proposedCorrections;

        public ComparisonResult(SimulationRunRecord baselineRun, SimulationRunRecord targetRun,
                                int divergenceYear, List<MetricDelta> finalDeltas,
                                List<String> keyDifferences, String primaryRootCauseExplanation,
                                List<ParameterCorrection> proposedCorrections) {
            this.baselineRun = baselineRun;
            this.targetRun = targetRun;
            this.divergenceYear = divergenceYear;
            this.finalDeltas = finalDeltas;
            this.keyDifferences = keyDifferences;
            this.primaryRootCauseExplanation = primaryRootCauseExplanation;
            this.proposedCorrections = proposedCorrections != null ? proposedCorrections : Collections.emptyList();
        }

        public SimulationRunRecord getBaselineRun() { return baselineRun; }
        public SimulationRunRecord getTargetRun() { return targetRun; }
        public int getDivergenceYear() { return divergenceYear; }
        public List<MetricDelta> getFinalDeltas() { return finalDeltas; }
        public List<String> getKeyDifferences() { return keyDifferences; }
        public String getPrimaryRootCauseExplanation() { return primaryRootCauseExplanation; }
        public List<ParameterCorrection> getProposedCorrections() { return proposedCorrections; }
    }

    public ComparisonResult compareRuns(SimulationRunRecord baseline, SimulationRunRecord target) {
        if (baseline == null || target == null) {
            throw new IllegalArgumentException("Baseline and target runs must not be null.");
        }

        // 1. Identify parameter differences
        List<String> keyDifferences = new ArrayList<>();
        Set<String> allKeys = new LinkedHashSet<>(baseline.getParameterMatrix().keySet());
        allKeys.addAll(target.getParameterMatrix().keySet());

        for (String key : allKeys) {
            String valA = baseline.getParameterMatrix().getOrDefault(key, "N/A");
            String valB = target.getParameterMatrix().getOrDefault(key, "N/A");
            if (!valA.equalsIgnoreCase(valB)) {
                keyDifferences.add(String.format("%s : '%s' ➔ '%s'", key, valA, valB));
            }
        }

        // 2. Find divergence year T_divergence (where population or tech differs by > 5%)
        int divergenceYear = -1;
        TreeMap<Integer, SimulationRunRecord.MetricSnapshot> tsA = baseline.getTimeSeriesData();
        TreeMap<Integer, SimulationRunRecord.MetricSnapshot> tsB = target.getTimeSeriesData();

        for (int year : tsA.keySet()) {
            SimulationRunRecord.MetricSnapshot sA = tsA.get(year);
            SimulationRunRecord.MetricSnapshot sB = tsB.get(year);
            if (sA != null && sB != null && sA.getPopulation() > 0) {
                double popDiffPct = Math.abs((double) (sB.getPopulation() - sA.getPopulation()) / sA.getPopulation()) * 100.0;
                if (popDiffPct >= 5.0 && divergenceYear == -1) {
                    divergenceYear = year;
                    break;
                }
            }
        }

        // 3. Calculate deltas at final common year
        int finalYear = Math.min(baseline.getEndYear(), target.getEndYear());
        SimulationRunRecord.MetricSnapshot finalA = baseline.getSnapshotAt(finalYear);
        SimulationRunRecord.MetricSnapshot finalB = target.getSnapshotAt(finalYear);

        List<MetricDelta> deltas = new ArrayList<>();
        if (finalA != null && finalB != null) {
            deltas.add(new MetricDelta("Population Totale", finalA.getPopulation(), finalB.getPopulation()));
            deltas.add(new MetricDelta("Réserves Alimentaires", finalA.getFood(), finalB.getFood()));
            deltas.add(new MetricDelta("Niveau Technologique Moyen", finalA.getAvgTech(), finalB.getAvgTech()));
            deltas.add(new MetricDelta("Indice de Stabilité", finalA.getStability(), finalB.getStability()));
        }

        // 4. Generate root cause explanation string
        StringBuilder explanation = new StringBuilder();
        if (divergenceYear != -1) {
            explanation.append(String.format("Divergence majeure détectée à l'An %d. ", divergenceYear));
        } else {
            explanation.append("Les trajectoires demeurent quasiment parallèles sur toute la durée. ");
        }

        if (!keyDifferences.isEmpty()) {
            explanation.append("Origine principale des écarts : ").append(String.join(", ", keyDifferences)).append(". ");
        }

        MetricDelta popDelta = deltas.stream().filter(d -> d.getMetricName().contains("Population")).findFirst().orElse(null);
        if (popDelta != null) {
            explanation.append(String.format("Impact final à l'An %d : %+.1f%% sur la population.", finalYear, popDelta.getPercentageChange()));
        }

        // 5. Generate Automated Parameter Correction Proposals ("Corriger")
        List<ParameterCorrection> proposedCorrections = generateParameterCorrections(baseline, target, deltas, divergenceYear);

        return new ComparisonResult(baseline, target, divergenceYear, deltas, keyDifferences, explanation.toString(), proposedCorrections);
    }

    private List<ParameterCorrection> generateParameterCorrections(SimulationRunRecord baseline, SimulationRunRecord target,
                                                                   List<MetricDelta> deltas, int divergenceYear) {
        List<ParameterCorrection> corrections = new ArrayList<>();
        MetricDelta popDelta = deltas.stream().filter(d -> d.getMetricName().contains("Population")).findFirst().orElse(null);
        MetricDelta techDelta = deltas.stream().filter(d -> d.getMetricName().contains("Technologique")).findFirst().orElse(null);
        MetricDelta stabDelta = deltas.stream().filter(d -> d.getMetricName().contains("Stabilité")).findFirst().orElse(null);

        if (popDelta != null && Math.abs(popDelta.getPercentageChange()) >= 5.0) {
            double changePct = popDelta.getPercentageChange();
            if (changePct < 0) { // Target population lags behind baseline
                double factor = 1.0 + (Math.abs(changePct) / 100.0) * 0.5;
                String curFood = target.getParameterMatrix().getOrDefault("Réserves Food F0", "6.0 mois");
                double numFood = parseDoubleWithFallback(curFood, 6.0);
                double newFood = numFood * factor;
                corrections.add(new ParameterCorrection(
                    "initialFoodReserveMonths",
                    String.format("%.1f mois", numFood),
                    String.format("%.1f mois", newFood),
                    factor,
                    String.format("Sous-performance démographique (%.1f%%). Augmenter les réserves alimentaires pour prévenir la mortalité malthusienne initiale.", changePct)
                ));

                String curCap = target.getParameterMatrix().getOrDefault("Capital Physique K0", "10.0 kg/hab");
                double numCap = parseDoubleWithFallback(curCap, 10.0);
                double newCap = numCap * (1.0 + (factor - 1.0) * 0.5);
                corrections.add(new ParameterCorrection(
                    "initialCapitalPerCapita",
                    String.format("%.1f kg/hab", numCap),
                    String.format("%.1f kg/hab", newCap),
                    newCap / numCap,
                    "Renforcer le capital d'outillage initial pour soutenir l'empreinte productive."
                ));
            } else { // Target population overshoots baseline
                double factor = Math.max(0.5, 1.0 - (changePct / 100.0) * 0.4);
                String curPop = target.getParameterMatrix().getOrDefault("Population Initiale", "1,000");
                long numPop = parseLongWithFallback(curPop, 1000L);
                long newPop = Math.max(10L, (long) (numPop * factor));
                corrections.add(new ParameterCorrection(
                    "initialHumanCount",
                    String.format("%,d", numPop),
                    String.format("%,d", newPop),
                    factor,
                    String.format("Surcroît démographique (%.1f%%). Ajuster la population initiale pour s'aligner sur la capacité de charge du benchmark.", changePct)
                ));
            }
        }

        if (techDelta != null && techDelta.getPercentageChange() < -5.0) {
            String curInfo = target.getParameterMatrix().getOrDefault("Information Initiale I0", "100.0 bits/hab");
            double numInfo = parseDoubleWithFallback(curInfo, 100.0);
            double newInfo = numInfo * 1.5;
            corrections.add(new ParameterCorrection(
                "initialInformationPerCapita",
                String.format("%.1f bits/hab", numInfo),
                String.format("%.1f bits/hab", newInfo),
                1.5,
                String.format("Retard technologique (%.1f%%). Augmenter la densité d'information initiale pour stimuler les taux d'innovation.", techDelta.getPercentageChange())
            ));
        }

        if (stabDelta != null && stabDelta.getTargetValue() < 40.0) {
            corrections.add(new ParameterCorrection(
                "typeBEngineStates:FrontierAsabiyyah",
                "Désactivé",
                "✅ Activé",
                1.0,
                "Instabilité chronique sous le seuil critique (40.0). Activer le moteur de cohésion sociale FrontierAsabiyyah."
            ));
        }

        return corrections;
    }

    private double parseDoubleWithFallback(String str, double fallback) {
        if (str == null) return fallback;
        try {
            String cleaned = str.replaceAll("[^0-9,.-]", "").replace(',', '.');
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return fallback;
        }
    }

    private long parseLongWithFallback(String str, long fallback) {
        if (str == null) return fallback;
        try {
            String cleaned = str.replaceAll("[^0-9-]", "");
            return Long.parseLong(cleaned);
        } catch (Exception e) {
            return fallback;
        }
    }
}
