/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Experiment A — Structural Variance Decomposition (ANOVA-style Monte-Carlo).
 *
 * <p>Core null hypothesis (H₀): "Historical leader interventions contribute < 5% of
 * total trajectory variance, demonstrating epiphenomenal status relative to
 * underlying structural (geographical, thermodynamic, biological) forces."
 *
 * <p>Methodology (analogous to one-way ANOVA η² effect size):
 * <pre>
 *   σ²_structure  = Var(outcome | NO leaders, N seeds)
 *   σ²_total      = Var(outcome | leaders ENABLED, same N seeds)
 *   σ²_leader     = σ²_total − σ²_structure   (attributed leadership contribution)
 *   η²_leader     = σ²_leader / σ²_total       (effect size, 0..1)
 * </pre>
 *
 * <p>Interpretation scale:
 * <ul>
 *   <li>η² &lt; 0.05 → leaders epiphenomenal (macro-determinism supported)</li>
 *   <li>0.05 ≤ η² &lt; 0.30 → modest but non-trivial leader effect</li>
 *   <li>0.30 ≤ η² &lt; 0.50 → significant leader effect</li>
 *   <li>η² ≥ 0.50 → great-man hypothesis has strong support in this model</li>
 * </ul>
 *
 * <p>All results are logged via System.out for capture by the scientific documentation
 * pipeline and integration into SCIENTIFIC_MODEL_EVALUATION_AND_FALSIFICATION.md.
 *
 * @see TopologicalConvergenceTest Experiment B — Wasserstein counterfactual cluster
 * @see InformationEntropyLeaderTest Experiment C — Shannon entropy gain
 * @see ArchetypeSubstitutionTest Experiment D — Permutation / identity invariance
 */
public class VarianceDecompositionTest {

    private static final int N_RUNS = 40;        // Monte-Carlo ensemble size (per arm)
    private static final int SIM_YEARS = 200;    // Simulation horizon per run (years)
    private static final int N_CELLS = 20;       // H3 mock cells in Macedonia/Greece region

    // -------------------------------------------------------------------------
    // Helper: build a synthetic H3Cell cluster around the Macedonian epicenter
    // -------------------------------------------------------------------------
    private List<H3Cell> buildCells(double baseLat, double baseLng) {
        List<H3Cell> cells = new ArrayList<>();
        for (int i = 0; i < N_CELLS; i++) {
            H3Cell c = new H3Cell();
            c.setH3Index(2000L + i);
            c.setLatitude(baseLat + (i * 0.1));
            c.setLongitude(baseLng + (i * 0.1));
            c.setElevation(200.0);
            c.setTemperature(16.0);
            c.setRainfall(700.0);
            c.setBiome(Biome.FOREST);
            c.setPopulation(10_000);
            c.setFoodResource(50_000.0);
            c.setMovementFriction(1.0);
            c.setResourceCapital(100_000.0);
            cells.add(c);
        }
        return cells;
    }

    // -------------------------------------------------------------------------
    // Helper: run one simulation arm, return total capital at end of horizon
    // -------------------------------------------------------------------------
    private double runAndMeasureCapital(long seed, boolean enableLeaders, int startYear) {
        EventSystem sys = new EventSystem();
        sys.setSeed(seed);
        sys.setEnableEarthHistoricalLeaders(enableLeaders);
        sys.setEnableProceduralLeaders(false);
        sys.setEnableRandomEvents(false);       // isolate leader signal
        sys.setEnableHistoricalMilestones(false);

        List<H3Cell> cells = buildCells(40.0, 22.0);
        for (int yr = startYear; yr < startYear + SIM_YEARS; yr++) {
            sys.checkEvents(yr, 0, 500_000L, 2_500_000.0, cells);
        }
        return cells.stream().mapToDouble(H3Cell::getResourceCapital).sum();
    }

    // -------------------------------------------------------------------------
    // Helper: sample variance of a List<Double>
    // -------------------------------------------------------------------------
    private static double variance(List<Double> values) {
        if (values.size() < 2) return 0.0;
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return values.stream()
            .mapToDouble(v -> (v - mean) * (v - mean))
            .average()
            .orElse(0.0);
    }

    // =========================================================================
    // TEST A-1 — Full ANOVA η² decomposition across 40 Monte-Carlo runs
    // =========================================================================
    @Test
    @DisplayName("Experiment A-1 — ANOVA Variance Decomposition: η² Leader Effect Size (40 runs × 200 yr)")
    void testVarianceDecomposition() {
        List<Double> structureOutcomes = new ArrayList<>();
        List<Double> totalOutcomes = new ArrayList<>();

        /*
         * Starting at year -400 BC ensures Alexander (-334), Confucius (-551, already active
         * by -400), and Cyrus (-559) all fall within the simulation horizon.
         * This maximises the signal-to-noise ratio for the leader arm.
         */
        for (int run = 0; run < N_RUNS; run++) {
            long seed = 100L + run * 7919L;  // deterministic prime-stepped seeds
            structureOutcomes.add(runAndMeasureCapital(seed, false, -400));
            totalOutcomes.add(runAndMeasureCapital(seed, true,  -400));
        }

        double varianceStructure = variance(structureOutcomes);
        double varianceTotal     = variance(totalOutcomes);
        double varianceLeader    = Math.max(0.0, varianceTotal - varianceStructure);
        double eta2Leader        = (varianceTotal > 1e-9) ? (varianceLeader / varianceTotal) : 0.0;

        double meanStructure = structureOutcomes.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double meanTotal     = totalOutcomes.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double meanDeltaPct  = (meanStructure > 0) ? (meanTotal - meanStructure) / meanStructure * 100 : 0;

        System.out.println("\n=== EXPERIMENT A-1: ANOVA Variance Decomposition ===");
        System.out.printf("  N_RUNS = %d | SIM_YEARS = %d | N_CELLS = %d%n", N_RUNS, SIM_YEARS, N_CELLS);
        System.out.printf("  σ²_structure  (no leaders) = %.3e%n", varianceStructure);
        System.out.printf("  σ²_total     (with leaders)= %.3e%n", varianceTotal);
        System.out.printf("  σ²_leader    (attributed)  = %.3e%n", varianceLeader);
        System.out.printf("  η²_leader    (effect size) = %.4f (%.1f%% of total variance)%n", eta2Leader, eta2Leader * 100);
        System.out.printf("  μ_structure = %.0f | μ_total = %.0f | Δμ = %+.2f%%%n", meanStructure, meanTotal, meanDeltaPct);
        System.out.printf("  SCIENTIFIC VERDICT: %s%n",
            eta2Leader < 0.05  ? "MACRO-DETERMINISM SUPPORTED — leaders epiphenomenal (η² < 5%)" :
            eta2Leader < 0.30  ? "MODERATE LEADER EFFECT — structural forces dominant (5% ≤ η² < 30%)" :
            eta2Leader < 0.50  ? "SIGNIFICANT LEADER EFFECT — both forces active (30% ≤ η² < 50%)" :
                                  "GREAT-MAN HYPOTHESIS SUPPORTED — leaders dominant (η² ≥ 50%)");

        // Hard safety bound: if leaders explain > 95% of variance, the model is
        // pathologically overweighting individual agency — this is a calibration failure.
        assertTrue(eta2Leader < 0.95,
            String.format(
                "CRITICAL: η²=%.4f — leaders dominate >95%% of variance. " +
                "Model severely overweights individual agency over structural forces.",
                eta2Leader));

        // η² must be a valid probability
        assertTrue(eta2Leader >= 0.0, "η² must be non-negative");
        assertTrue(eta2Leader <= 1.0, "η² must not exceed 1.0");
    }

    // =========================================================================
    // TEST A-2 — Cross-archetype mean capital delta vs. structural baseline
    // =========================================================================
    @Test
    @DisplayName("Experiment A-2 — Archetype η² Decomposition: Relative Capital Effect per Archetype vs. Baseline")
    void testArchetypeVarianceDecomposition() {
        /*
         * For each of the 6 LeaderArchetype values, inject one synthetic leader of
         * magnitude 8.0 at the Macedonian epicenter and measure the mean capital
         * differential relative to the no-leader baseline.
         *
         * This decomposes the aggregate leader effect (A-1) into archetype-level
         * contributions, answering: "Which archetype drives the most variance?"
         */
        final int N_ARCHETYPE_RUNS = N_RUNS;

        // Compute no-leader baseline mean
        List<Double> baselineCapitals = new ArrayList<>();
        for (int run = 0; run < N_ARCHETYPE_RUNS; run++) {
            baselineCapitals.add(runAndMeasureCapital(200L + run * 3571L, false, -400));
        }
        double baselineMean = baselineCapitals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        System.out.println("\n=== EXPERIMENT A-2: Cross-Archetype Capital Effect ===");
        System.out.printf("  Structural baseline mean capital (no leaders, %d runs): %.0f%n", N_ARCHETYPE_RUNS, baselineMean);
        System.out.printf("  %-40s  %12s  %8s%n", "Archetype", "Mean Capital", "Δ vs Null");

        Map<LeaderArchetype, Double> archetypeEffects = new LinkedHashMap<>();
        for (LeaderArchetype archetype : LeaderArchetype.values()) {
            List<Double> archetypeOutcomes = new ArrayList<>();
            for (int run = 0; run < N_ARCHETYPE_RUNS; run++) {
                long seed = 300L + run * 2237L;
                EventSystem sys = new EventSystem();
                sys.setSeed(seed);
                sys.setEnableEarthHistoricalLeaders(false);
                sys.setEnableProceduralLeaders(false);
                sys.setEnableRandomEvents(false);
                sys.setEnableHistoricalMilestones(false);

                HistoricalIntervention syntheticLeader = new HistoricalIntervention(
                    "SYNTH_" + archetype.name(), "Synthetic " + archetype.name(),
                    "Monte-Carlo synthetic leader for archetype variance decomposition",
                    -380, 30, 40.64, 22.94, 2000.0, archetype, 8.0
                );
                sys.injectCustomIntervention(syntheticLeader);

                List<H3Cell> cells = buildCells(40.0, 22.0);
                for (int yr = -400; yr < -400 + SIM_YEARS; yr++) {
                    sys.checkEvents(yr, 0, 500_000L, 2_500_000.0, cells);
                }
                archetypeOutcomes.add(cells.stream().mapToDouble(H3Cell::getResourceCapital).sum());
            }

            double archetypeMean = archetypeOutcomes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double relativeEffect = (baselineMean > 0) ? (archetypeMean - baselineMean) / baselineMean : 0.0;
            archetypeEffects.put(archetype, relativeEffect);

            System.out.printf("  %-40s  %12.0f  %+7.2f%%%n", archetype.name(), archetypeMean, relativeEffect * 100);
        }

        // Assertion: physical capital archetypes must produce at least as much capital as conquerors
        double builderEffect   = archetypeEffects.getOrDefault(LeaderArchetype.INFRASTRUCTURE_BUILDER,   0.0);
        double hydraulicEffect = archetypeEffects.getOrDefault(LeaderArchetype.HYDRAULIC_AGRARIAN_INNOVATOR, 0.0);
        double conquerorEffect = archetypeEffects.getOrDefault(LeaderArchetype.MILITARY_CONQUEROR,       0.0);

        System.out.printf("  Builder=%.3f  Hydraulic=%.3f  Conqueror=%.3f%n",
            builderEffect, hydraulicEffect, conquerorEffect);

        assertTrue(builderEffect >= conquerorEffect || hydraulicEffect >= conquerorEffect,
            String.format("Physical capital archetype (builder=%.3f, hydraulic=%.3f) must produce "
                + ">= capital effect compared to military conqueror (%.3f).",
                builderEffect, hydraulicEffect, conquerorEffect));
    }
}
