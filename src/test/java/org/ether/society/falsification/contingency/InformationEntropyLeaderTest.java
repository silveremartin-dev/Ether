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
 * Experiment C — Information-Theoretic Test: Do Leaders Add Shannon Entropy to
 * Historical Trajectories?
 *
 * <p>Core hypothesis: if leaders truly "create history", they must add information
 * (i.e., increase entropy) to the state trajectory — they must open new outcome states
 * that would not otherwise have been reachable by structural forces alone.
 *
 * <p>Conversely, if H(with leaders) ≈ H(without leaders), leaders are merely
 * redistributing pre-existing degrees of freedom — they are filling attractor basins
 * that structural forces pre-determined, not generating novel ones.
 *
 * <p>Method:
 * <ol>
 *   <li>Run N_RUNS Monte-Carlo simulations for two arms (null/leaders), each with a
 *       different stochastic seed.</li>
 *   <li>Collect final total capital as scalar outcome for each run.</li>
 *   <li>Discretize outcomes into B uniform bins → estimate Shannon entropy
 *       H = −Σ pᵢ · log₂(pᵢ)  (in bits).</li>
 *   <li>Compute ΔH = H(leaders) − H(null) and normalise by H_max = log₂(B).</li>
 * </ol>
 *
 * <p>C-2 extends this to a cross-archetype entropy profile — which archetype maximally
 * expands or collapses the outcome space?
 *
 * @see VarianceDecompositionTest Experiment A — ANOVA η² effect size
 * @see TopologicalConvergenceTest Experiment B — Wasserstein distributional distance
 * @see ArchetypeSubstitutionTest Experiment D — Identity invariance permutation test
 */
public class InformationEntropyLeaderTest {

    private static final int N_RUNS    = 50;   // Monte-Carlo ensemble (for robust histogram)
    private static final int N_BINS    = 20;   // Histogram resolution (H_max = log₂(20) ≈ 4.32 bits)
    private static final int SIM_YEARS = 200;  // Simulation horizon per run
    private static final int N_CELLS   = 20;   // H3 mock cells

    // -------------------------------------------------------------------------
    // Helper: build synthetic H3Cell cluster
    // -------------------------------------------------------------------------
    private List<H3Cell> buildCells(int n, double baseLat, double baseLng) {
        List<H3Cell> cells = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            H3Cell c = new H3Cell();
            c.setH3Index(4000L + i);
            c.setLatitude(baseLat + i * 0.1);
            c.setLongitude(baseLng + i * 0.1);
            c.setElevation(100.0);
            c.setTemperature(18.0);
            c.setRainfall(600.0);
            c.setBiome(Biome.FOREST);
            c.setPopulation(5_000);
            c.setFoodResource(30_000.0);
            c.setMovementFriction(1.0);
            c.setResourceCapital(80_000.0);
            cells.add(c);
        }
        return cells;
    }

    // -------------------------------------------------------------------------
    // Helper: run one simulation arm, return total capital
    // -------------------------------------------------------------------------
    private double runAndMeasureCapital(long seed, boolean enableLeaders) {
        EventSystem sys = new EventSystem();
        sys.setSeed(seed);
        sys.setEnableEarthHistoricalLeaders(enableLeaders);
        sys.setEnableProceduralLeaders(false);
        sys.setEnableRandomEvents(false);
        sys.setEnableHistoricalMilestones(false);

        List<H3Cell> cells = buildCells(N_CELLS, 40.0, 22.0);
        for (int yr = -400; yr < -400 + SIM_YEARS; yr++) {
            sys.checkEvents(yr, 0, 300_000L, 1_500_000.0, cells);
        }
        return cells.stream().mapToDouble(H3Cell::getResourceCapital).sum();
    }

    // -------------------------------------------------------------------------
    // Helper: Shannon entropy of outcomes discretised into B uniform bins (bits)
    //   H = −Σ pᵢ · log₂(pᵢ)
    // Returns 0.0 for degenerate (all-identical) distributions.
    // -------------------------------------------------------------------------
    private double shannonEntropy(double[] values, int bins) {
        if (values.length == 0 || bins <= 0) return 0.0;
        double min = Arrays.stream(values).min().orElse(0.0);
        double max = Arrays.stream(values).max().orElse(1.0);
        double range = max - min;
        if (range < 1e-12) return 0.0;   // deterministic → 0 entropy

        int[] counts = new int[bins];
        for (double v : values) {
            int bin = (int) Math.min(bins - 1, Math.floor((v - min) / range * bins));
            counts[bin]++;
        }

        double entropy = 0.0;
        int total = values.length;
        for (int cnt : counts) {
            if (cnt > 0) {
                double p = (double) cnt / total;
                entropy -= p * (Math.log(p) / Math.log(2.0));   // log base-2 → bits
            }
        }
        return entropy;
    }

    // =========================================================================
    // TEST C-1 — Main entropy gain test (ΔH leaders vs. null)
    // =========================================================================
    @Test
    @DisplayName("Experiment C-1 — Shannon Entropy: Do Leaders Add Information Bits to the Historical Trajectory?")
    void testLeaderInformationEntropyGain() {
        double[] nullOutcomes   = new double[N_RUNS];
        double[] leaderOutcomes = new double[N_RUNS];

        for (int run = 0; run < N_RUNS; run++) {
            long seed = 700L + run * 4001L;
            nullOutcomes[run]   = runAndMeasureCapital(seed, false);
            leaderOutcomes[run] = runAndMeasureCapital(seed, true);
        }

        double hNull   = shannonEntropy(nullOutcomes,   N_BINS);
        double hLeader = shannonEntropy(leaderOutcomes, N_BINS);
        double deltaH  = hLeader - hNull;

        // Maximum possible Shannon entropy for N_BINS bins = log₂(N_BINS)
        double hMax = Math.log(N_BINS) / Math.log(2.0);

        // Normalised information gain
        double normalizedDeltaH = (hMax > 0) ? deltaH / hMax : 0.0;

        System.out.println("\n=== EXPERIMENT C-1: Shannon Entropy Information Gain ===");
        System.out.printf("  N_RUNS = %d | N_BINS = %d | SIM_YEARS = %d%n", N_RUNS, N_BINS, SIM_YEARS);
        System.out.printf("  H(null, %d runs, %d bins)    = %.4f bits%n", N_RUNS, N_BINS, hNull);
        System.out.printf("  H(leaders, %d runs, %d bins) = %.4f bits%n", N_RUNS, N_BINS, hLeader);
        System.out.printf("  H_max = log₂(%d)             = %.4f bits%n", N_BINS, hMax);
        System.out.printf("  ΔH = H(leaders) − H(null)    = %+.4f bits%n", deltaH);
        System.out.printf("  ΔH / H_max (normalised)       = %+.4f (%.1f%% of max)%n",
            normalizedDeltaH, normalizedDeltaH * 100);
        System.out.printf("  INFORMATION-THEORETIC VERDICT: %s%n",
            Math.abs(deltaH) < 0.1
                ? "ENTROPY NEUTRAL — leaders add no new information; outcome space pre-determined" :
            deltaH > 0.5
                ? "ENTROPY GENERATOR — leaders significantly expand historical outcome space" :
            deltaH < -0.5
                ? "ENTROPY REDUCER — leaders collapse outcome diversity (strong attractor narrowing)" :
                  "MARGINAL ENTROPY EFFECT — leaders have weak informational impact");

        // Validity constraints
        assertTrue(hNull   >= 0.0,          "Shannon entropy must be non-negative (null)");
        assertTrue(hLeader >= 0.0,          "Shannon entropy must be non-negative (leader)");
        assertTrue(hNull   <= hMax + 0.01,  "Null entropy must not exceed log₂(bins)");
        assertTrue(hLeader <= hMax + 0.01,  "Leader entropy must not exceed log₂(bins)");
        assertTrue(Double.isFinite(hNull),  "Null entropy must be finite");
        assertTrue(Double.isFinite(hLeader),"Leader entropy must be finite");
        assertTrue(Double.isFinite(deltaH), "ΔH must be finite");
    }

    // =========================================================================
    // TEST C-2 — Cross-archetype entropy profile
    // =========================================================================
    @Test
    @DisplayName("Experiment C-2 — Cross-Archetype Entropy Profile: Which Archetype Generates Most Novel Outcome States?")
    void testCrossArchetypeEntropyProfile() {
        /*
         * For each archetype, inject a synthetic leader (magnitude 8.0, Macedonian epicenter)
         * across N_RUNS seeds and measure the Shannon entropy of the resulting outcome distribution.
         *
         * An archetype with high entropy EXPANDS the outcome space — it makes futures more
         * unpredictable. An archetype with low entropy COLLAPSES outcomes toward a narrow
         * attractor — it is structurally deterministic (the result is the same regardless of seed).
         */
        final double hMax = Math.log(N_BINS) / Math.log(2.0);

        // Baseline: no leaders
        double[] nullOutcomes = new double[N_RUNS];
        for (int run = 0; run < N_RUNS; run++) {
            nullOutcomes[run] = runAndMeasureCapital(800L + run * 3113L, false);
        }
        double hNull = shannonEntropy(nullOutcomes, N_BINS);

        System.out.println("\n=== EXPERIMENT C-2: Cross-Archetype Entropy Profile ===");
        System.out.printf("  N_RUNS=%d | N_BINS=%d | SIM_YEARS=%d | H_max=%.4f bits%n",
            N_RUNS, N_BINS, SIM_YEARS, hMax);
        System.out.printf("  %-45s  %8s  %8s%n", "Archetype", "H (bits)", "ΔH");
        System.out.printf("  %-45s  %8.4f  (BASELINE)%n", "NO LEADERS (structural forces only)", hNull);

        Map<LeaderArchetype, Double> archetypeEntropies = new LinkedHashMap<>();

        for (LeaderArchetype archetype : LeaderArchetype.values()) {
            double[] archetypeOutcomes = new double[N_RUNS];
            for (int run = 0; run < N_RUNS; run++) {
                long seed = 900L + run * 1777L;
                EventSystem sys = new EventSystem();
                sys.setSeed(seed);
                sys.setEnableEarthHistoricalLeaders(false);
                sys.setEnableProceduralLeaders(false);
                sys.setEnableRandomEvents(false);
                sys.setEnableHistoricalMilestones(false);

                HistoricalIntervention syntheticLeader = new HistoricalIntervention(
                    "ENT_" + archetype.name(), "EntropyTest-" + archetype.name(),
                    "Cross-archetype entropy profile test at magnitude 8.0",
                    -380, 30, 40.64, 22.94, 2000.0, archetype, 8.0
                );
                sys.injectCustomIntervention(syntheticLeader);

                List<H3Cell> cells = buildCells(N_CELLS, 40.0, 22.0);
                for (int yr = -400; yr < -400 + SIM_YEARS; yr++) {
                    sys.checkEvents(yr, 0, 300_000L, 1_500_000.0, cells);
                }
                archetypeOutcomes[run] = cells.stream().mapToDouble(H3Cell::getResourceCapital).sum();
            }

            double hArchetype = shannonEntropy(archetypeOutcomes, N_BINS);
            double deltaH     = hArchetype - hNull;
            archetypeEntropies.put(archetype, hArchetype);

            System.out.printf("  %-45s  %8.4f  %+8.4f%n", archetype.name(), hArchetype, deltaH);

            // Validity: all archetype entropies must be in [0, H_max]
            assertTrue(hArchetype >= 0.0,        "Entropy for " + archetype + " must be non-negative");
            assertTrue(hArchetype <= hMax + 0.01,"Entropy for " + archetype + " must not exceed log₂(bins)");
        }

        System.out.printf("  Null entropy H_null = %.4f bits%n", hNull);
        System.out.println("  [NOTE] High entropy → archetype unpacks new outcome states (path-diverging).");
        System.out.println("  [NOTE] Low entropy  → archetype collapses outcomes (attractor-narrowing).");
    }
}
