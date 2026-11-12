package org.ether.society.core.dod;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.model.PhysicalConstants;

/**
 * Data-Oriented Urban Agglomeration, Capital Accumulation &amp; Institutional Entropy Kernel.
 * <p>
 * Models urban emergence, Solow-Swan / Ayres-KÃ¼mmel physical capital accumulation ($K$),
 * and Tainter's thermodynamic maintenance constraints ($C$) across all active cells using
 * strictly smooth ($C^\infty$) differential equations of conserved physical observables
 * ({@code AGENTS.md} Â§10.1, Â§10.2). Zero state-level {@code technologyLevel} reads or mutations.
 * </p>
 * <p>
 * Mathematically synchronized with {@code native/ether-core-native/src/urban.rs}.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class UrbanKernel {

    /**
     * Updates global infrastructure physical capital ($K$) and institutional complexity ($C$)
     * across all active cells under thermodynamic conservation and entropic depreciation laws.
     *
     * @param world contiguous world state buffer
     * @param dt    integration time step (years if &le; 20, days if &le; 1000, seconds if &gt; 1000)
     */
    public void tick(WorldBuffer world, float dt) {
        float[] prices = world.getLocalPrice();
        float[] capital = world.getResourceCapital();
        float[] pop = world.getBiomassHuman();
        float[] complexity = world.getInstitutionalComplexity();

        float dtInYears = (float) Math.max(0.0001, dt > 1000.0f
                ? (dt / PhysicalConstants.SECONDS_PER_JULIAN_YEAR)
                : (dt > 20.0f ? dt / 365.25 : dt));
        float decayCapital = (float) Math.pow(0.95, dtInYears);
        float decayComplexity = (float) Math.pow(0.90, dtInYears);

        int capacity = world.getCapacity();
        int[] landIndices = world.getLandIndices();
        int count = landIndices != null ? landIndices.length : capacity;
        for (int idx = 0; idx < count; idx++) {
            int i = landIndices != null ? landIndices[idx] : idx;
            if (i < 0 || i >= capacity) continue;

            float p = pop[i];
            // Smooth C^infty settlement activation gate around Dunbar sub-band threshold (N_0 = 12, s = 0.5)
            double activeSettlement = SmoothMath.sigmoid(p, 12.0, 0.5);

            if (p <= 0.001f || activeSettlement < 1e-6) {
                if (capital[i] > 0.0f) {
                    capital[i] = Math.max(0.0f, capital[i] * decayCapital);
                }
                if (complexity[i] > 0.0f) {
                    complexity[i] = Math.max(0.0f, complexity[i] * decayComplexity);
                }
                continue;
            }

            float capitalPerCapita = capital[i] / p;

            // 1. Institutional Complexity C (Tainter 1988, West-Bettencourt urban scaling):
            // Scales smoothly with intensive physical capital density (K/N) and settlement scale ln(1 + N)
            double targetComplexity = Math.log1p(capitalPerCapita * 0.25 + Math.log1p(p * 0.005));
            float newComp = (float) (activeSettlement * targetComplexity + (1.0 - activeSettlement) * complexity[i] * decayComplexity);
            complexity[i] = newComp;

            // 2. Institutional Maintenance Overhead (Tainter Law of Diminishing Marginal Returns):
            double cappedComp = SmoothMath.softmin(5.0, newComp, 4.0);
            double baselineDepreciation = 0.02;
            double complexityOverhead = 0.001 * Math.pow(Math.max(0.0, cappedComp), 1.5);
            double depreciationRate = SmoothMath.softmin(0.08, baselineDepreciation + complexityOverhead, 40.0);

            // 3. Physical Capital Steady-State Convergence (Solow-Swan mechanical power constraint, Solow 1956, Garrett 2011, Krausmann et al. 2017):
            // Steady-state material capital stock per capita k* scales thermodynamically with available power above somatic hearth baseline (~348 W):
            double powerPerCapitaW = world.computeCellPowerWattsPerCapita(i);
            double kStar = 5.0
                    + 580.0 * Math.tanh(SmoothMath.softplus(powerPerCapitaW - 348.0, 0.2) / 38.0)
                    + 62.0 * Math.pow(SmoothMath.softplus(powerPerCapitaW - 400.0, 0.1) / 100.0, 1.95);

            // In mobile hunter-gatherer bands (low population p < 50), somatic transport bounds capital stock to portable tools (AGENTS.md A§8)
            double bandConstraint = SmoothMath.sigmoid(p, 50.0, 15.0);
            kStar = 5.0 * (1.0 - bandConstraint) + kStar * bandConstraint;

            // Sedentary infrastructure (mudbrick/masonry/granaries/plows) requires agricultural biomass
            // or high-power industrial grids; mobile foraging bands cannot transport stationary infrastructure (AGENTS.md A§8).
            float[] agri = world.getBiomassAgriculture();
            float cellAgri = (agri != null && i < agri.length) ? agri[i] : 0.0f;
            double agriActivation = SmoothMath.sigmoid(cellAgri, 100.0, 30.0);
            double industrialActivation = SmoothMath.sigmoid(powerPerCapitaW, 480.0, 80.0);
            double sedentaryCapability = Math.max(agriActivation, industrialActivation);
            kStar = 5.0 + (kStar - 5.0) * sedentaryCapability;

            // Physical capital depreciation rate delta = 4.0% per year (entropy of physical structures/tools)
            double deltaAnnual = 0.04;
            double dCapitalPerCapita = deltaAnnual * (kStar - capitalPerCapita) * dtInYears;
            double targetCapital = Math.max(0.0, (capitalPerCapita + dCapitalPerCapita) * p);
            double decayedCap = capital[i] * decayCapital;
            capital[i] = (float) (activeSettlement * targetCapital + (1.0 - activeSettlement) * decayedCap);

            // 4. Smooth Tainter Simplification: when complexity overextends beyond physical capital, complexity contracts
            double maintenanceDrag = SmoothMath.sigmoid(newComp, 4.0, 0.8);
            complexity[i] = (float) (newComp * (1.0 - 0.05 * maintenanceDrag * dtInYears));
        }
    }
}
