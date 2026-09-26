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
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Experiment B — Topological Convergence (Counterfactual Cluster Analysis).
 *
 * <p>Core null hypothesis (H₀): "The historical configuration WITH leaders falls within
 * the natural distributional attractor of the system WITHOUT leaders — i.e., the same
 * end-state would have been reached by structural forces alone."
 *
 * <p>Two sub-experiments:
 * <ol>
 *   <li><b>B-1 (Wasserstein-1 distributional distance)</b>: Run N_NULL stochastic simulations
 *       WITHOUT leaders → distributional null D_null. Run N_NULL WITH leaders → D_leader.
 *       Compute z-score and approximate Wasserstein-1 distance between the two distributions.
 *       If |z| &lt; 2.0, leader outcomes lie within the 2σ natural variance band → H₀ retained.</li>
 *   <li><b>B-2 (Identity invariance)</b>: Swap 'Alexandre le Grand' for an anonymous
 *       MILITARY_CONQUEROR with identical archetype/magnitude/coordinates. Outcomes must be
 *       bit-identical — proving that the individual's NAME is a free variable irrelevant to
 *       the simulation trajectory.</li>
 * </ol>
 *
 * @see VarianceDecompositionTest Experiment A — ANOVA η² effect size
 * @see InformationEntropyLeaderTest Experiment C — Shannon entropy gain
 * @see ArchetypeSubstitutionTest Experiment D — Cross-archetype permutation ranking
 */
public class TopologicalConvergenceTest {

    private static final int N_NULL    = 40;   // ensemble size for null/leader distributions
    private static final int SIM_YEARS = 150;  // years per simulation run
    private static final int N_CELLS   = 20;   // H3 mock cells

    // -------------------------------------------------------------------------
    // Helper: build a synthetic H3Cell cluster
    // -------------------------------------------------------------------------
    private List<H3Cell> buildCells(int n, double baseLat, double baseLng,
                                    double initCapital, double initFood) {
        List<H3Cell> cells = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            H3Cell c = new H3Cell();
            c.setH3Index(3000L + i);
            c.setLatitude(baseLat + (i * 0.15));
            c.setLongitude(baseLng + (i * 0.15));
            c.setElevation(180.0);
            c.setTemperature(17.0);
            c.setRainfall(650.0);
            c.setBiome(Biome.FOREST);
            c.setPopulation(8_000);
            c.setFoodResource(initFood);
            c.setMovementFriction(1.0);
            c.setResourceCapital(initCapital);
            cells.add(c);
        }
        return cells;
    }

    // -------------------------------------------------------------------------
    // Helper: run an ensemble of N_NULL simulations and return outcome array
    // -------------------------------------------------------------------------
    private double[] runEnsemble(boolean enableLeaders, int startYear) {
        double[] outcomes = new double[N_NULL];
        for (int run = 0; run < N_NULL; run++) {
            long seed = 500L + run * 6271L;
            EventSystem sys = new EventSystem();
            sys.setSeed(seed);
            sys.setEnableEarthHistoricalLeaders(enableLeaders);
            sys.setEnableProceduralLeaders(false);
            sys.setEnableRandomEvents(false);
            sys.setEnableHistoricalMilestones(false);

            List<H3Cell> cells = buildCells(N_CELLS, 40.0, 22.0, 100_000.0, 50_000.0);
            for (int yr = startYear; yr < startYear + SIM_YEARS; yr++) {
                sys.checkEvents(yr, 0, 400_000L, 2_000_000.0, cells);
            }
            outcomes[run] = cells.stream().mapToDouble(H3Cell::getResourceCapital).sum();
        }
        return outcomes;
    }

    // =========================================================================
    // TEST B-1 — Wasserstein-1 distributional distance between null and leader
    // =========================================================================
    @Test
    @DisplayName("Experiment B-1 — Topological Convergence: Leader Distribution vs. Structural Null Attractor (z-score + W₁)")
    void testTopologicalConvergenceAlexanderEpoch() {
        double[] nullOutcomes   = runEnsemble(false, -400);
        double[] leaderOutcomes = runEnsemble(true,  -400);

        // Null distribution statistics
        double nullMean = Arrays.stream(nullOutcomes).average().orElse(0.0);
        double nullSd = Math.sqrt(
            Arrays.stream(nullOutcomes)
                .map(v -> (v - nullMean) * (v - nullMean))
                .average().orElse(0.0));

        double leaderMean = Arrays.stream(leaderOutcomes).average().orElse(0.0);

        // z-score: (leaderMean − nullMean) / nullSd
        double zScore = (nullSd > 1e-9) ? Math.abs(leaderMean - nullMean) / nullSd : 0.0;

        // Approximate 1D Wasserstein-1 distance W₁ = (1/N) Σ |sort(null)ᵢ − sort(leader)ᵢ|
        double[] sortedNull   = Arrays.stream(nullOutcomes).sorted().toArray();
        double[] sortedLeader = Arrays.stream(leaderOutcomes).sorted().toArray();
        double wasserstein1 = IntStream.range(0, N_NULL)
            .mapToDouble(i -> Math.abs(sortedNull[i] - sortedLeader[i]))
            .average()
            .orElse(0.0);

        // W₁ as fraction of null mean (scale-invariant)
        double relativeW1 = (nullMean > 0) ? wasserstein1 / nullMean : 0.0;

        System.out.println("\n=== EXPERIMENT B-1: Topological Convergence — Wasserstein-1 ===");
        System.out.printf("  N_NULL = %d | SIM_YEARS = %d%n", N_NULL, SIM_YEARS);
        System.out.printf("  Null distribution:   μ_null   = %.0f  σ_null = %.0f%n", nullMean, nullSd);
        System.out.printf("  Leader distribution: μ_leader = %.0f%n", leaderMean);
        System.out.printf("  z-score  = %.4f  (|z| < 2.0 → H₀ retained: leader in null attractor)%n", zScore);
        System.out.printf("  W₁       = %.0f  (%.2f%% of null mean)%n", wasserstein1, relativeW1 * 100);
        System.out.printf("  TOPOLOGICAL VERDICT: %s%n",
            zScore < 1.0 ? "STRONG CONVERGENCE — leaders epiphenomenal to structural attractor" :
            zScore < 2.0 ? "MODERATE CONVERGENCE — leaders within 2σ natural variance band" :
            zScore < 3.0 ? "MARGINAL DIVERGENCE — leaders shift trajectory beyond 2σ" :
                           "STRONG BIFURCATION — leaders drive trajectory far from structural attractor");

        // Sanity: both distributions non-trivial
        assertTrue(nullMean > 0,   "Null distribution must produce non-zero capital");
        assertTrue(leaderMean > 0, "Leader distribution must produce non-zero capital");

        // W₁ must be finite and non-negative
        assertTrue(wasserstein1 >= 0.0, "Wasserstein-1 distance must be non-negative");
        assertTrue(Double.isFinite(wasserstein1), "Wasserstein-1 distance must be finite");

        // z-score must be finite and non-negative
        assertTrue(zScore >= 0.0, "z-score must be non-negative");
        assertTrue(Double.isFinite(zScore), "z-score must be finite");
    }

    // =========================================================================
    // TEST B-2 — Identity Invariance: name substitution → bit-identical outcome
    // =========================================================================
    @Test
    @DisplayName("Experiment B-2 — Identity Invariance: 'Alexandre le Grand' vs. 'Anonymus Macedonicus' → Same Outcome")
    void testIdentityInvarianceUnderNameSubstitution() {
        /*
         * Theoretical claim: in a physics-based cliodynamic model, the NAME of the leader
         * is a free parameter. Only archetype + magnitude + coordinates + duration drive outcomes.
         * If Alexandre and an anonymous conqueror at the same parameters produce identical
         * trajectories, the individual's identity is causally irrelevant.
         *
         * This directly tests the endogeneity thesis:
         * "Someone would have done the same — Alexandre or not."
         */
        final long FIXED_SEED = 42L;
        final int  START_YEAR = -400;

        // Arm A: named historical leader "Alexandre le Grand"
        EventSystem sysAlexander = new EventSystem();
        sysAlexander.setSeed(FIXED_SEED);
        sysAlexander.setEnableEarthHistoricalLeaders(false);
        sysAlexander.setEnableProceduralLeaders(false);
        sysAlexander.setEnableRandomEvents(false);
        sysAlexander.setEnableHistoricalMilestones(false);

        HistoricalIntervention alexander = new HistoricalIntervention(
            "ALEX_NAMED", "Alexandre le Grand", "Conquête hellénique",
            -334, 11, 40.64, 22.94, 3500.0, LeaderArchetype.MILITARY_CONQUEROR, 9.5
        );
        sysAlexander.injectCustomIntervention(alexander);

        // Arm B: structurally identical but anonymised
        EventSystem sysAnon = new EventSystem();
        sysAnon.setSeed(FIXED_SEED);
        sysAnon.setEnableEarthHistoricalLeaders(false);
        sysAnon.setEnableProceduralLeaders(false);
        sysAnon.setEnableRandomEvents(false);
        sysAnon.setEnableHistoricalMilestones(false);

        HistoricalIntervention anonConqueror = new HistoricalIntervention(
            "ANON_CONQUEROR", "Anonymus Macedonicus", "Conquête macédonienne anonyme structurellement identique",
            -334, 11, 40.64, 22.94, 3500.0, LeaderArchetype.MILITARY_CONQUEROR, 9.5
        );
        sysAnon.injectCustomIntervention(anonConqueror);

        List<H3Cell> cellsAlexander = buildCells(N_CELLS, 40.0, 22.0, 100_000.0, 50_000.0);
        List<H3Cell> cellsAnon      = buildCells(N_CELLS, 40.0, 22.0, 100_000.0, 50_000.0);

        for (int yr = START_YEAR; yr < START_YEAR + SIM_YEARS; yr++) {
            sysAlexander.checkEvents(yr, 0, 400_000L, 2_000_000.0, cellsAlexander);
            sysAnon.checkEvents(yr, 0, 400_000L, 2_000_000.0, cellsAnon);
        }

        double capitalAlexander  = cellsAlexander.stream().mapToDouble(H3Cell::getResourceCapital).sum();
        double capitalAnon       = cellsAnon.stream().mapToDouble(H3Cell::getResourceCapital).sum();
        double frictionAlexander = cellsAlexander.stream().mapToDouble(H3Cell::getMovementFriction).average().orElse(1.0);
        double frictionAnon      = cellsAnon.stream().mapToDouble(H3Cell::getMovementFriction).average().orElse(1.0);
        double foodAlexander     = cellsAlexander.stream().mapToDouble(H3Cell::getFoodResource).sum();
        double foodAnon          = cellsAnon.stream().mapToDouble(H3Cell::getFoodResource).sum();

        double capitalDeltaPct = (capitalAlexander > 0)
            ? Math.abs(capitalAlexander - capitalAnon) / capitalAlexander * 100 : 0;

        System.out.println("\n=== EXPERIMENT B-2: Identity Invariance Under Name Substitution ===");
        System.out.printf("  Alexandre capital:   %.2f | Anon capital:   %.2f | Δ = %.6f%%%n",
            capitalAlexander, capitalAnon, capitalDeltaPct);
        System.out.printf("  Alexandre friction:  %.6f | Anon friction:  %.6f%n", frictionAlexander, frictionAnon);
        System.out.printf("  Alexandre food:      %.2f | Anon food:      %.2f%n", foodAlexander, foodAnon);
        System.out.println("  IDENTITY INVARIANCE VERDICT: " +
            (capitalDeltaPct < 0.001 ? "CONFIRMED — the individual's name is a free variable" :
             "VIOLATED — implementation must be corrected"));

        // IDENTITY INVARIANCE: outcomes must be bit-identical when only name changes
        assertEquals(capitalAlexander, capitalAnon, 0.001,
            String.format("IDENTITY INVARIANCE VIOLATED: different names with identical parameters "
                + "produced different capital outcomes (%.4f ≠ %.4f)", capitalAlexander, capitalAnon));

        assertEquals(frictionAlexander, frictionAnon, 1e-9,
            "IDENTITY INVARIANCE VIOLATED: friction must be identical for name-substituted leaders");

        assertEquals(foodAlexander, foodAnon, 0.001,
            "IDENTITY INVARIANCE VIOLATED: food resource must be identical for name-substituted leaders");
    }
}
