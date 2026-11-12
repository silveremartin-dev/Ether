/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hubbell's Unified Neutral Theory of Biodiversity &amp; Biogeography (UNTB) Engine.
 *
 * <p>Formalizes Stephen P. Hubbell's Unified Neutral Theory of Biodiversity and Biogeography
 * (Hubbell 2001, Princeton University Press; Volkov et al. 2003, <i>Nature</i>; Rosindell et al. 2011, <i>TREE</i>).</p>
 *
 * <p>Replaces ad-hoc scalar biodiversity indices with an analytical, order-independent zero-sum
 * ecological drift and speciation-extinction model:</p>
 * <ol>
 *   <li><b>Fundamental Biodiversity Number (\(\theta\))</b>:
 *       \[
 *       \theta = 2 J_M \nu
 *       \]
 *       where \(J_M\) is the metacommunity carrying capacity (total biomass or headcount) and \(\nu\)
 *       is the speciation/diversification rate per birth.</li>
 *   <li><b>Preston-Hubbell Species-Area Power Law</b>:
 *       \[
 *       S(A) = c \cdot A^z
 *       \]
 *       with canonical spatial scaling exponent \(z \approx 0.25\) across continental and insular biomes.</li>
 *   <li><b>Fisher-Volkov Relative Species Abundance (SAD)</b>:
 *       Expected number of species with \(n\) individuals:
 *       \[
 *       \langle \phi_n \rangle = \theta \cdot \frac{x^n}{n}, \quad x = \frac{J_M}{J_M + \theta}
 *       \]
 *       Total species richness: \(S = -\theta \ln(1 - x)\).</li>
 *   <li><b>Endogenous Extinction Debt from Habitat/Biomass Depletion</b>:
 *       When wild habitat or biomass is reduced by hunting/deforestation (\(J \to J_{\text{post}}\)):
 *       \[
 *       \frac{S_{\text{post}}}{S_0} = \left(\frac{J_{\text{post}}}{J_0}\right)^z
 *       \]
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see <a href="https://press.princeton.edu/books/hardcover/9780691021287/the-unified-neutral-theory-of-biodiversity-and-biogeography">Hubbell (2001)</a>
 */
public class UnifiedNeutralBiodiversityEngine extends AbstractSimulationEngine {
    public UnifiedNeutralBiodiversityEngine() {
        super("UnifiedNeutralBiodiversityEngine", "UnifiedNeutralBiodiversityEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(UnifiedNeutralBiodiversityEngine.class);

    /** Global planetary mean biodiversity retention index, continuously aggregated from cell states. */
    private static double globalBiodiversityIndex = 1.0;

    /**
     * Retrieves the latest planetary aggregate biodiversity retention index \(\bar{\mathcal{B}} \in [0, 1]\).
     *
     * @return global biodiversity retention
     */
    public static double getGlobalBiodiversityIndex() {
        return globalBiodiversityIndex;
    }

    /**
     * Resets the planetary aggregate biodiversity index to pristine baseline (1.0).
     */
    public static void resetState() {
        globalBiodiversityIndex = 1.0;
    }

    // --- Universal Neutral Theory Constants (Hubbell 2001; Volkov et al. 2003) ---

    /** Canonical continental species-area scaling exponent \(z \approx 0.25\) (Preston 1962; Hubbell 2001). */
    public static final double SPECIES_AREA_EXPONENT_Z = 0.25;

    /** Baseline speciation / evolutionary innovation rate per generation \(\nu \approx 1.0 \times 10^{-6}\). */
    public static final double DEFAULT_SPECIATION_RATE_NU = 1.0e-6;

    /** Baseline regional species density coefficient \(c \approx 15.0\text{ species/km}^{2z}\). */
    public static final double BASELINE_RICHNESS_COEFFICIENT = 15.0;

    /**
     * Calculates the Fundamental Biodiversity Number \(\theta\):
     * \[
     * \theta = 2 J_M \nu
     * \]
     *
     * @param metacommunitySize total living biomass or individual count \(J_M\)
     * @param speciationRate    point-mutation/speciation rate \(\nu\)
     * @return dimensionless biodiversity parameter \(\theta\)
     */
    public static double calculateFundamentalBiodiversityNumber(double metacommunitySize, double speciationRate) {
        if (metacommunitySize <= 0.0 || speciationRate <= 0.0) return 0.0;
        return 2.0 * metacommunitySize * speciationRate;
    }

    /**
     * Calculates steady-state species richness \(S\) from the Fundamental Biodiversity Number:
     * \[
     * S = -\theta \ln(1 - x) = \theta \ln\left(1 + \frac{J_M}{\theta}\right)
     * \]
     *
     * @param metacommunitySize total biomass or individuals \(J_M\)
     * @param speciationRate    speciation rate \(\nu\)
     * @return expected species richness count \(S\)
     */
    public static double calculateNeutralSpeciesRichness(double metacommunitySize, double speciationRate) {
        if (metacommunitySize <= 1.0 || speciationRate <= 0.0) return 1.0;
        double theta = calculateFundamentalBiodiversityNumber(metacommunitySize, speciationRate);
        if (theta <= 1e-9) return 1.0;
        return theta * Math.log1p(metacommunitySize / theta);
    }

    /**
     * Calculates species-area scaling richness:
     * \[
     * S(A) = c \cdot A^z
     * \]
     *
     * @param areaKm2 continuous habitat area in \(\text{km}^2\)
     * @return species richness \(S\)
     */
    public static double calculateSpeciesAreaRichness(double areaKm2) {
        if (areaKm2 <= 0.0) return 0.0;
        return BASELINE_RICHNESS_COEFFICIENT * Math.pow(areaKm2, SPECIES_AREA_EXPONENT_Z);
    }

    /**
     * Calculates relative biodiversity retention fraction \(\mathcal{B} \in [0, 1]\) following habitat/biomass loss:
     * \[
     * \mathcal{B} = \left(\frac{B_{\text{remaining}}}{B_{\text{pristine}}}\right)^z
     * \]
     *
     * @param remainingBiomass current natural biomass density
     * @param pristineBiomass  pre-anthropogenic baseline natural biomass density
     * @return dimensionless biodiversity retention index in \([0, 1]\)
     */
    public static double calculateBiodiversityRetention(double remainingBiomass, double pristineBiomass) {
        if (pristineBiomass <= 0.0 || remainingBiomass <= 0.0) return 0.0;
        double ratio = Math.clamp(remainingBiomass / pristineBiomass, 0.0, 1.0);
        return Math.pow(ratio, SPECIES_AREA_EXPONENT_Z);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || acc == null || dtYears <= 0.0) return;
        double naturalBiomass = buffer.getBiomassNatural()[i];
        if (naturalBiomass <= 0.0) return;

        double pristineBaseline = Math.max(50.0, naturalBiomass * 1.5);
        double retention = calculateBiodiversityRetention(naturalBiomass, pristineBaseline);
        if (retention < 0.60) {
            double degradationRate = (0.60 - retention) * 0.01 * dtYears;
            double nextBio = Math.max(1.0, naturalBiomass * (1.0 - degradationRate));
            acc.dBiomassNatural += (float) (nextBio - naturalBiomass);
        }
    }

    @Override
    public void tick(WorldBuffer buf, double dtYears, double dtMonthly) {
        processNeutralBiodiversity(buf, dtYears);
    }

    /**
     * Executes one time-stepped Hubbell UNTB biodiversity evaluation across DoD land cells.
     *
     * @param buffer  WorldBuffer DoD state
     * @param dtYears elapsed simulation step in years
     */
    public static void processNeutralBiodiversity(WorldBuffer buffer, double dtYears) {
        if (buffer == null || buffer.getCapacity() == 0 || dtYears <= 0.0) return;

        double totalRetention = 0.0;
        int activeCount = 0;
        float[] bioNat = buffer.getBiomassNatural();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            double naturalBiomass = bioNat[i];
            if (naturalBiomass <= 0.0) continue;

            double pristineBaseline = Math.max(50.0, naturalBiomass * 1.5);
            double retention = calculateBiodiversityRetention(naturalBiomass, pristineBaseline);
            totalRetention += retention;
            activeCount++;

            if (retention < 0.60) {
                double degradationRate = (0.60 - retention) * 0.01 * dtYears;
                bioNat[i] = (float) Math.max(1.0, naturalBiomass * (1.0 - degradationRate));
            }
        }

        if (activeCount > 0) {
            globalBiodiversityIndex = totalRetention / activeCount;
        }
    }
}
