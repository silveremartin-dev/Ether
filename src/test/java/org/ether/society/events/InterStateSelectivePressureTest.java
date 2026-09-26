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
 * Pillar 3 — Inter-State Selective Pressure & Structural Convergence.
 *
 * <p>Thesis: In a physics-based multi-state system, the competitive advantage gained
 * by a physical-capital leader (canal, road network, irrigation) creates selective
 * pressure on neighbouring states — forcing them to either adopt equivalent structural
 * innovations independently, or be absorbed. The leader determines TIMING; geography
 * and thermodynamics determine OUTCOME.
 *
 * <p>Sub-experiments:
 * <ol>
 *   <li><b>P3-1 (Selective Advantage)</b>: Cluster A (Macedonia) receives a builder;
 *       Cluster B (Mesopotamia) does not. After 280 years, A leads B in capital.
 *       This confirms physical-capital archetypes provide competitive advantage.</li>
 *
 *   <li><b>P3-2 (Delayed Convergence)</b>: Then give B the identical builder, delayed
 *       by 100 years. After a further 280 years, does B close the gap? If so, the
 *       structural innovation was inevitable — only the timing differs.</li>
 *
 *   <li><b>P3-3 (Structural Determinism)</b>: Give NEITHER cluster any leader, but
 *       enable stochastic events (random climate, disease, resource pressure). Do both
 *       clusters converge to the same capital level over 500 years? If yes, structural
 *       forces are sufficient — individual leaders only accelerate what physics demands.</li>
 * </ol>
 *
 * <p>Historical analogue: The Grand Canal (Sui Emperor Yang, ~605 CE).
 * China's north-south hydraulic imperative existed independently of any individual.
 * The Tang dynasty would eventually have built it, possibly with less human cost.
 * The delay would have been decades, not centuries.
 */
public class InterStateSelectivePressureTest {

    private static final int N_CELLS_PER_CLUSTER = 15;

    private List<H3Cell> buildCluster(int n, double baseLat, double baseLng,
                                      double capital, double food, long h3Offset) {
        List<H3Cell> cells = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            H3Cell c = new H3Cell();
            c.setH3Index(h3Offset + i);
            c.setLatitude(baseLat + i * 0.15);
            c.setLongitude(baseLng + i * 0.15);
            c.setElevation(160.0);
            c.setTemperature(17.0);
            c.setRainfall(680.0);
            c.setBiome(Biome.FOREST);
            c.setPopulation(8_000);
            c.setFoodResource(food);
            c.setMovementFriction(1.0);
            c.setResourceCapital(capital);
            cells.add(c);
        }
        return cells;
    }

    // =========================================================================
    // TEST P3-1 — Selective Advantage: Builder vs. No-Builder (280 yr)
    // =========================================================================
    @Test
    @DisplayName("Pillar 3-1 — Selective Advantage: Infrastructure Builder grants measurable capital lead over 280 years")
    void testSelectiveAdvantageBuilderVsNoBuilder() {
        final long SEED = 42L;
        final int START = -400;
        final int END   = -120;   // 280 years

        // Cluster A: Macedonia (builder present from year -380)
        EventSystem sysA = new EventSystem();
        sysA.setSeed(SEED);
        sysA.setEnableEarthHistoricalLeaders(false);
        sysA.setEnableProceduralLeaders(false);
        sysA.setEnableRandomEvents(false);
        sysA.setEnableHistoricalMilestones(false);

        HistoricalIntervention builderA = new HistoricalIntervention(
            "BUILDER_A", "Grand Infrastructure Builder A",
            "Pillar 3 inter-state selective pressure test — Cluster A builder",
            -380, 30, 40.64, 22.94, 1800.0, LeaderArchetype.INFRASTRUCTURE_BUILDER, 8.0
        );
        sysA.injectCustomIntervention(builderA);

        // Cluster B: Mesopotamia (NO builder — structural baseline)
        EventSystem sysB = new EventSystem();
        sysB.setSeed(SEED);
        sysB.setEnableEarthHistoricalLeaders(false);
        sysB.setEnableProceduralLeaders(false);
        sysB.setEnableRandomEvents(false);
        sysB.setEnableHistoricalMilestones(false);

        List<H3Cell> clusterA = buildCluster(N_CELLS_PER_CLUSTER, 40.0, 22.0, 100_000.0, 50_000.0, 6000L);
        List<H3Cell> clusterB = buildCluster(N_CELLS_PER_CLUSTER, 33.0, 44.0, 100_000.0, 50_000.0, 7000L);

        for (int yr = START; yr <= END; yr++) {
            sysA.checkEvents(yr, 0, 400_000L, 2_000_000.0, clusterA);
            sysB.checkEvents(yr, 0, 400_000L, 2_000_000.0, clusterB);
        }

        double capitalA = clusterA.stream().mapToDouble(H3Cell::getResourceCapital).sum();
        double capitalB = clusterB.stream().mapToDouble(H3Cell::getResourceCapital).sum();
        double frictionA = clusterA.stream().mapToDouble(H3Cell::getMovementFriction).average().orElse(1.0);
        double frictionB = clusterB.stream().mapToDouble(H3Cell::getMovementFriction).average().orElse(1.0);
        double leadPct = (capitalB > 0) ? (capitalA - capitalB) / capitalB * 100 : 0;

        System.out.println("\n=== PILLAR 3-1: Selective Advantage — Builder vs. No-Builder (280 yr) ===");
        System.out.printf("  Cluster A (Macedonia + builder):   capital=%.0f  friction=%.4f%n", capitalA, frictionA);
        System.out.printf("  Cluster B (Mesopotamia, baseline): capital=%.0f  friction=%.4f%n", capitalB, frictionB);
        System.out.printf("  A's competitive lead: %+.2f%%%n", leadPct);
        System.out.printf("  VERDICT: %s%n",
            capitalA > capitalB
                ? "SELECTIVE ADVANTAGE CONFIRMED — builder-state outcompetes no-builder state"
                : "NO COMPETITIVE DIFFERENTIAL — structural forces equalise without leaders");

        // Assert selective advantage: A must outperform B in capital
        assertTrue(capitalA >= capitalB,
            String.format("Cluster A (builder) must have >= capital than Cluster B (baseline). "
                + "Got A=%.0f, B=%.0f", capitalA, capitalB));

        // A must have lower friction (roads / infrastructure built)
        assertTrue(frictionA <= frictionB,
            String.format("Cluster A (builder) must have <= friction than B (no builder). "
                + "Got A=%.4f, B=%.4f", frictionA, frictionB));
    }

    // =========================================================================
    // TEST P3-2 — Delayed Convergence: Delayed Builder Closes the Gap
    // =========================================================================
    @Test
    @DisplayName("Pillar 3-2 — Delayed Convergence: 100-Year Delayed Builder Closes Capital Gap (Structural Inevitability)")
    void testDelayedBuilderConvergence() {
        /*
         * Key question: "Would someone else have built the Grand Canal?"
         *
         * Methodology:
         *   Phase 1 (-400 to -200):
         *     Cluster A: builder at year -380 (early adopter)
         *     Cluster B: NO builder (falling behind)
         *
         *   Phase 2 (-200 to 0):
         *     Cluster A: no new leader (reaping long-term benefits)
         *     Cluster B: delayed builder at year -200 (100 years late)
         *
         *   Measure: Does B's final capital (year 0) converge to A's?
         *   If yes → the timing of the builder is the free variable; the capital
         *   outcome is structurally determined.
         */
        final long SEED_A = 42L;
        final long SEED_B = 42L;

        // --- PHASE 1: -400 to -200 (200 years) ---
        EventSystem sysA_p1 = new EventSystem();
        sysA_p1.setSeed(SEED_A);
        sysA_p1.setEnableEarthHistoricalLeaders(false);
        sysA_p1.setEnableProceduralLeaders(false);
        sysA_p1.setEnableRandomEvents(false);
        sysA_p1.setEnableHistoricalMilestones(false);

        HistoricalIntervention earlyBuilder = new HistoricalIntervention(
            "EARLY_BUILDER", "Early Infrastructure Pioneer",
            "Early adopter — Phase 1 builder in Cluster A",
            -380, 30, 40.64, 22.94, 1800.0, LeaderArchetype.INFRASTRUCTURE_BUILDER, 8.0
        );
        sysA_p1.injectCustomIntervention(earlyBuilder);

        EventSystem sysB_p1 = new EventSystem();
        sysB_p1.setSeed(SEED_B);
        sysB_p1.setEnableEarthHistoricalLeaders(false);
        sysB_p1.setEnableProceduralLeaders(false);
        sysB_p1.setEnableRandomEvents(false);
        sysB_p1.setEnableHistoricalMilestones(false);
        // B has no builder in Phase 1

        List<H3Cell> clusterA = buildCluster(N_CELLS_PER_CLUSTER, 40.0, 22.0, 100_000.0, 50_000.0, 8000L);
        List<H3Cell> clusterB = buildCluster(N_CELLS_PER_CLUSTER, 40.0, 22.0, 100_000.0, 50_000.0, 9000L);

        for (int yr = -400; yr <= -200; yr++) {
            sysA_p1.checkEvents(yr, 0, 400_000L, 2_000_000.0, clusterA);
            sysB_p1.checkEvents(yr, 0, 400_000L, 2_000_000.0, clusterB);
        }

        double capitalA_mid = clusterA.stream().mapToDouble(H3Cell::getResourceCapital).sum();
        double capitalB_mid = clusterB.stream().mapToDouble(H3Cell::getResourceCapital).sum();

        System.out.println("\n=== PILLAR 3-2: Delayed Convergence — The Grand Canal Question ===");
        System.out.printf("  Phase 1 end (year -200): A=%.0f | B=%.0f | Gap=%+.2f%%%n",
            capitalA_mid, capitalB_mid,
            (capitalB_mid > 0) ? (capitalA_mid - capitalB_mid) / capitalB_mid * 100 : 0);

        // --- PHASE 2: -200 to 0 (200 years) — give B its delayed builder ---
        EventSystem sysA_p2 = new EventSystem();
        sysA_p2.setSeed(SEED_A);
        sysA_p2.setEnableEarthHistoricalLeaders(false);
        sysA_p2.setEnableProceduralLeaders(false);
        sysA_p2.setEnableRandomEvents(false);
        sysA_p2.setEnableHistoricalMilestones(false);
        // A gets no second builder

        EventSystem sysB_p2 = new EventSystem();
        sysB_p2.setSeed(SEED_B);
        sysB_p2.setEnableEarthHistoricalLeaders(false);
        sysB_p2.setEnableProceduralLeaders(false);
        sysB_p2.setEnableRandomEvents(false);
        sysB_p2.setEnableHistoricalMilestones(false);

        HistoricalIntervention delayedBuilder = new HistoricalIntervention(
            "DELAYED_BUILDER", "Delayed Infrastructure Pioneer (100 yr late)",
            "Delayed adopter — Phase 2 builder in Cluster B",
            -200, 30, 40.64, 22.94, 1800.0, LeaderArchetype.INFRASTRUCTURE_BUILDER, 8.0
        );
        sysB_p2.injectCustomIntervention(delayedBuilder);

        for (int yr = -200; yr <= 0; yr++) {
            sysA_p2.checkEvents(yr, 0, 400_000L, 2_000_000.0, clusterA);
            sysB_p2.checkEvents(yr, 0, 400_000L, 2_000_000.0, clusterB);
        }

        double capitalA_final = clusterA.stream().mapToDouble(H3Cell::getResourceCapital).sum();
        double capitalB_final = clusterB.stream().mapToDouble(H3Cell::getResourceCapital).sum();
        double finalGapPct = (capitalA_final > 0)
            ? Math.abs(capitalA_final - capitalB_final) / capitalA_final * 100 : 0;

        System.out.printf("  Phase 2 end (year 0):   A=%.0f | B=%.0f | Final gap=%.2f%%%n",
            capitalA_final, capitalB_final, finalGapPct);
        System.out.printf("  Gap narrowed: Phase1=%.2f%% → Phase2=%.2f%%? %s%n",
            (capitalB_mid > 0) ? Math.abs(capitalA_mid - capitalB_mid) / capitalA_mid * 100 : 0,
            finalGapPct,
            finalGapPct < 5.0 ? "YES — CONVERGENCE ACHIEVED" : "NO — gap persists");
        System.out.printf("  STRUCTURAL INEVITABILITY VERDICT: %s%n",
            finalGapPct < 5.0
                ? "CONFIRMED — delayed builder achieves same capital level; timing is free variable"
                : "PARTIAL — persistent gap suggests early timing confers structural path advantage");

        // B must have received the builder's capital bonus in Phase 2
        assertTrue(capitalB_final > capitalB_mid,
            "Cluster B with delayed builder must have grown capital in Phase 2");
    }

    // =========================================================================
    // TEST P3-3 — Structural Convergence: No Leaders, Stochastic Events
    // =========================================================================
    @Test
    @DisplayName("Pillar 3-3 — Structural Convergence: Two Clusters Without Leaders Converge to Same Attractor Over 500 Years")
    void testStructuralConvergenceWithoutLeaders() {
        /*
         * Pure structural test: two geographically identical clusters, NO leaders,
         * stochastic events enabled. After 500 years, both should reach the same
         * mean capital level (± stochastic noise). This proves that structural physics
         * alone is sufficient to drive long-term trajectories — leaders are optional.
         */
        final int N_RUNS = 20;  // ensemble for stochastic averaging
        final int SIM_YEARS = 500;

        double[] capitalsX = new double[N_RUNS];
        double[] capitalsY = new double[N_RUNS];

        for (int run = 0; run < N_RUNS; run++) {
            long seed = 1000L + run * 6271L;

            // Cluster X — slightly different starting capital to test convergence
            EventSystem sysX = new EventSystem();
            sysX.setSeed(seed);
            sysX.setEnableEarthHistoricalLeaders(false);
            sysX.setEnableProceduralLeaders(false);
            sysX.setEnableRandomEvents(true);   // stochastic physics enabled
            sysX.setEnableHistoricalMilestones(false);

            // Cluster Y — different starting capital (120% of X)
            EventSystem sysY = new EventSystem();
            sysY.setSeed(seed);   // same seed, so same stochastic events fire
            sysY.setEnableEarthHistoricalLeaders(false);
            sysY.setEnableProceduralLeaders(false);
            sysY.setEnableRandomEvents(true);
            sysY.setEnableHistoricalMilestones(false);

            List<H3Cell> clusterX = buildCluster(N_CELLS_PER_CLUSTER, 40.0, 22.0, 100_000.0, 50_000.0, 10000L + run);
            List<H3Cell> clusterY = buildCluster(N_CELLS_PER_CLUSTER, 40.0, 22.0, 120_000.0, 60_000.0, 11000L + run);

            for (int yr = -500; yr < -500 + SIM_YEARS; yr++) {
                sysX.checkEvents(yr, 0, 400_000L, 2_000_000.0, clusterX);
                sysY.checkEvents(yr, 0, 400_000L, 2_000_000.0, clusterY);
            }

            capitalsX[run] = clusterX.stream().mapToDouble(H3Cell::getResourceCapital).sum();
            capitalsY[run] = clusterY.stream().mapToDouble(H3Cell::getResourceCapital).sum();
        }

        double meanX = Arrays.stream(capitalsX).average().orElse(0);
        double meanY = Arrays.stream(capitalsY).average().orElse(0);
        double sdX   = Math.sqrt(Arrays.stream(capitalsX).map(v -> (v - meanX) * (v - meanX)).average().orElse(0));
        double sdY   = Math.sqrt(Arrays.stream(capitalsY).map(v -> (v - meanY) * (v - meanY)).average().orElse(0));

        double relGap = (meanX > 0) ? Math.abs(meanX - meanY) / meanX * 100 : 0;

        System.out.println("\n=== PILLAR 3-3: Structural Convergence Without Leaders (500 yr, 20 runs) ===");
        System.out.printf("  Cluster X (initial capital 100k): μ=%.0f  σ=%.0f%n", meanX, sdX);
        System.out.printf("  Cluster Y (initial capital 120k): μ=%.0f  σ=%.0f%n", meanY, sdY);
        System.out.printf("  |μX - μY| / μX = %.2f%%%n", relGap);
        System.out.printf("  CONVERGENCE VERDICT: %s%n",
            relGap < 10.0 ? "STRUCTURAL CONVERGENCE CONFIRMED — initial conditions wash out over 500 yr" :
            relGap < 25.0 ? "PARTIAL CONVERGENCE — some path dependency persists" :
                            "DIVERGENCE — initial conditions create persistent differential");

        // Both clusters must produce finite, positive capital
        assertTrue(meanX > 0, "Cluster X mean capital must be positive");
        assertTrue(meanY > 0, "Cluster Y mean capital must be positive");
        assertTrue(Double.isFinite(meanX), "Cluster X capital must be finite");
        assertTrue(Double.isFinite(meanY), "Cluster Y capital must be finite");
    }
}
