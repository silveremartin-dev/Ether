/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Universal Life-History Invariants &amp; Demographic Allocation Engine.
 *
 * <p>Formalizes the dimensionless evolutionary symmetries and life-history optimization
 * established by Eric L. Charnov (1991, 1993, <i>Life History Invariants</i>, Oxford Univ. Press),
 * Stephen C. Stearns (1992), and Promislow &amp; Harvey (1990).</p>
 *
 * <p>Key Bioenergetic &amp; Evolutionary Mechanics:</p>
 * <ol>
 *   <li><b>Charnov's Dimensionless Symmetry (\(M \cdot \alpha = C_{\text{charnov}}\))</b>:
 *       Across mammalian taxa and human populations, the dimensionless product of adult mortality hazard
 *       \(\mu_{\text{adult}}\) and age at reproductive maturity \(\alpha\) is an evolutionary invariant:
 *       \[
 *       \mu_{\text{adult}} \cdot \alpha = C_{\text{charnov}} \approx 1.80 \pm 0.20
 *       \]
 *       linking extrinsic environmental hazard directly to reproductive ontogeny.</li>
 *   <li><b>Optimal Reproductive Maturity Age (\(\alpha^*\))</b>:
 *       Under fluctuating extrinsic mortality \(\mu_{\text{total}} = \mu_{\text{ext}} + \mu_{\text{intrinsic}}\):
 *       \[
 *       \alpha^*(\mu) = \frac{C_{\text{charnov}}}{\mu_{\text{total}}} \quad [\text{years}]
 *       \]
 *       bounded by somatic physiological constraints (\(\alpha \in [\alpha_{\min}, \alpha_{\max}]\)).
 *       High-mortality paleolithic or crisis conditions accelerate maturation ("fast" life-history strategy),
 *       whereas safe, low-mortality environments lengthen juvenile investment and somatic encephalization.</li>
 *   <li><b>Juvenile Survivorship &amp; Parental Allocation</b>:
 *       Survival probability from birth to maturity \(\alpha\):
 *       \[
 *       l_\alpha = \exp(-\mu_{\text{juv}} \cdot \alpha)
 *       \]
 *       where juvenile mortality scales proportionally to adult environmental hazard:
 *       \(\mu_{\text{juv}} = q_{\text{juv}} \cdot \mu_{\text{adult}}\).</li>
 *   <li><b>Adaptive Reproductive Effort &amp; Fertility Rate (\(b^*\))</b>:
 *       At demographic replacement equilibrium (\(R_0 = l_\alpha \cdot \frac{b}{\mu} \approx 1.0\)),
 *       the equilibrium fertility rate per reproductive female balances mortality:
 *       \[
 *       b^*(\mu) = \frac{\mu_{\text{adult}}}{l_\alpha} = \mu_{\text{adult}} \cdot \exp(q_{\text{juv}} \cdot C_{\text{charnov}})
 *       \]</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://doi.org/10.1093/oso/9780198540724.001.0001">Charnov (1993), Life History Invariants</a>
 * @see <a href="https://doi.org/10.1111/j.1469-7998.1990.tb04316.x">Promislow &amp; Harvey (1990), J. Zool. 220</a>
 */
public class LifeHistoryInvariantsEngine {
    private static final Logger logger = LoggerFactory.getLogger(LifeHistoryInvariantsEngine.class);

    // --- Fundamental Evolutionary & Demographic Invariants ---

    /** Conserved Charnov dimensionless life-history invariant \(C_{\text{charnov}} \approx 1.80\) for mammals and primates. */
    public static final double CHARNOV_INVARIANT_C = 1.80;

    /** Ratio of juvenile mortality rate to adult mortality rate \(q_{\text{juv}} \approx 0.65\). */
    public static final double JUVENILE_TO_ADULT_MORTALITY_RATIO = 0.65;

    /** Baseline intrinsic adult mortality rate under optimal conditions (\(\mu_0 \approx 0.08\text{ yr}^{-1}\)). */
    public static final double BASELINE_ADULT_MORTALITY_RATE = 0.08;

    /** Absolute minimum physiological reproductive maturity age for hominins (\(\alpha_{\min} = 12.0\text{ years}\)). */
    public static final double MIN_REPRODUCTIVE_AGE_YEARS = 12.0;

    /** Maximum somatic maturity age in safe environments (\(\alpha_{\max} = 24.0\text{ years}\)). */
    public static final double MAX_REPRODUCTIVE_AGE_YEARS = 24.0;

    /** Inter-generational life-history adaptation relaxation timescale (\(\tau_{\text{lh}} \approx 25.0\text{ years}\)). */
    public static final double LIFE_HISTORY_RELAXATION_YEARS = 25.0;

    /**
     * Calculates the optimal age at reproductive maturity \(\alpha^*\) in years
     * based on total adult mortality hazard \(\mu\):
     * \[
     * \alpha^*(\mu) = \text{clamp}\left(\frac{C_{\text{charnov}}}{\mu}, \; \alpha_{\min}, \; \alpha_{\max}\right)
     * \]
     *
     * @param adultMortalityRate annual instantaneous adult mortality hazard (\(\text{yr}^{-1}\))
     * @return optimal age at first reproduction in years
     */
    public static double calculateOptimalReproductiveAge(double adultMortalityRate) {
        double safeMortality = Math.max(0.01, adultMortalityRate);
        double unconstrainedAge = CHARNOV_INVARIANT_C / safeMortality;
        return Math.clamp(unconstrainedAge, MIN_REPRODUCTIVE_AGE_YEARS, MAX_REPRODUCTIVE_AGE_YEARS);
    }

    /**
     * Calculates juvenile survivorship to maturity \(l_\alpha = \exp(-\mu_{\text{juv}} \cdot \alpha)\):
     *
     * @param adultMortalityRate annual adult mortality hazard (\(\text{yr}^{-1}\))
     * @param reproductiveAgeYears age at maturity in years
     * @return survival probability \(l_\alpha \in (0, 1]\)
     */
    public static double calculateJuvenileSurvivorship(double adultMortalityRate, double reproductiveAgeYears) {
        double muJuv = JUVENILE_TO_ADULT_MORTALITY_RATIO * Math.max(0.01, adultMortalityRate);
        double safeAlpha = Math.max(1.0, reproductiveAgeYears);
        return Math.exp(-muJuv * safeAlpha);
    }

    /**
     * Calculates the equilibrium birth rate per reproductive individual required for demographic persistence:
     * \[
     * b^*(\mu) = \frac{\mu_{\text{adult}}}{l_\alpha}
     * \]
     *
     * @param adultMortalityRate annual adult mortality hazard (\(\text{yr}^{-1}\))
     * @param juvenileSurvivorship survival fraction to maturity
     * @return annual births required per reproductive adult
     */
    public static double calculateEquilibriumBirthRate(double adultMortalityRate, double juvenileSurvivorship) {
        double safeSurv = Math.max(0.02, juvenileSurvivorship);
        return Math.max(0.01, adultMortalityRate) / safeSurv;
    }

    /**
     * Executes one time-stepped life-history demographic allocation update across cells.
     * Adapts population fertility and reproductive pacing according to environmental hazard.
     *
     * @param cells List of populated H3 cells
     * @param dtYears elapsed simulation time step in years
     */
    public static void processLifeHistoryDynamics(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty() || dtYears <= 0.0) return;

        double dtSafe = Math.max(1e-4, dtYears);

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            // Environmental stress contributes to adult mortality hazard
            double tempC = cell.getTemperatureOr(15.0);
            double thermalMortalityHazard = 0.0;
            if (tempC < -10.0) {
                thermalMortalityHazard = Math.min(0.15, (-10.0 - tempC) * 0.005);
            } else if (tempC > 38.0) {
                thermalMortalityHazard = Math.min(0.20, (tempC - 38.0) * 0.015);
            }

            // Total adult mortality hazard (baseline + environmental stress)
            double totalAdultMortality = BASELINE_ADULT_MORTALITY_RATE + thermalMortalityHazard;

            // Optimal maturity age and juvenile survival
            double optAlpha = calculateOptimalReproductiveAge(totalAdultMortality);
            double juvSurv = calculateJuvenileSurvivorship(totalAdultMortality, optAlpha);

            // Equilibrium birth rate under current mortality hazard
            double eqBirthRate = calculateEquilibriumBirthRate(totalAdultMortality, juvSurv);

            // Net demographic growth: stable near zero at optimal baseline (+0.005),
            // but contracts under extrinsic environmental mortality hazard, mitigated by adaptive earlier maturation
            double baselineAlpha = calculateOptimalReproductiveAge(BASELINE_ADULT_MORTALITY_RATE);
            double adaptiveMitigation = optAlpha / baselineAlpha; // < 1.0 when accelerating maturity
            double excessStress = thermalMortalityHazard * adaptiveMitigation;
            double netGrowthRate = Math.clamp(0.005 - excessStress, -0.15, 0.03);
            int newPop = (int) Math.round(pop * (1.0 + netGrowthRate * dtSafe));

            if (newPop != pop && newPop > 0) {
                cell.setPopulation(newPop);
            }
        }
    }
}
