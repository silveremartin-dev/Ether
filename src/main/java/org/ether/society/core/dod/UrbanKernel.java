package org.ether.society.core.dod;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.model.PhysicalConstants;

/**
 * Data-Oriented Urban Agglomeration, Capital Accumulation &amp; Institutional Entropy Kernel.
 * <p>
 * Models urban emergence, Solow-Swan / Ayres-K-mmel physical capital accumulation ($K$),
 * and Tainter's thermodynamic maintenance constraints ($C$) across all active cells using
 * strictly smooth ($C^\infty$) differential equations of conserved physical observables
 * ({@code AGENTS.md} Â§10.1, Â§10.2). Zero state-level {@code technologyLevel} reads or mutations.
 * </p>
 * <p>
 * Mathematically synchronized with {@code native/ether-core-native/src/urban.rs}.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class UrbanKernel {

    /**
     * Updates global infrastructure physical capital ($K$) and institutional complexity ($C$)
     * across all active cells using the canonical {@link org.ether.society.core.SimulationTimeStep}.
     *
     * @param world    contiguous world state buffer
     * @param timeStep canonical time step representation
     */
    public void tick(WorldBuffer world, org.ether.society.core.SimulationTimeStep timeStep) {
        tickWithDtYears(world, timeStep != null ? timeStep.dtYearsF() : (1.0f / 12.0f));
    }

    /**
     * Updates global infrastructure physical capital ($K$) and institutional complexity ($C$)
     * across all active cells under thermodynamic conservation and entropic depreciation laws.
     *
     * @param world contiguous world state buffer
     * @param dt    integration time step (years if &le; 20, days if &le; 1000, seconds if &gt; 1000)
     */
    public void tick(WorldBuffer world, float dt) {
        float dtInYears = (float) Math.max(0.0001, dt > 1000.0f
                ? (dt / PhysicalConstants.SECONDS_PER_JULIAN_YEAR)
                : (dt > 20.0f ? dt / 365.25 : dt));
        tickWithDtYears(world, dtInYears);
    }

    private void tickWithDtYears(WorldBuffer world, float dtYears) {
        float[] prices = world.getLocalPrice();
        float[] capital = world.getResourceCapital();
        float[] pop = world.getHumanPopulation();
        float[] complexity = world.getInstitutionalComplexity();

        float dtInYears = dtYears;
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
            double baselineTurnover = 0.04;
            double complexityOverhead = 0.0008 * Math.pow(Math.max(0.0, cappedComp), 1.5);
            double effectiveDepreciationRate = SmoothMath.softmin(0.075, baselineTurnover + complexityOverhead, 40.0);

            // 3. Physical Capital Steady-State Convergence (Solow 1956, Swan 1956, Garrett 2011, Krausmann et al. 2017):
            // Single-responsibility base physical capital ODE: \dot{k} = \delta_0 \cdot k^* - \delta_{\text{eff}}(C) \cdot k
            double powerPerCapitaW = world.computeCellPowerWattsPerCapita(i);
            double kStar = 5.0
                    + 580.0 * Math.tanh(SmoothMath.softplus(powerPerCapitaW - 348.0, 0.2) / 38.0)
                    + 62.0 * Math.pow(SmoothMath.softplus(powerPerCapitaW - 400.0, 0.1) / 100.0, 1.95);

            // In mobile hunter-gatherer bands (low population p < 50), somatic transport bounds capital stock to portable tools (AGENTS.md §8)
            double bandConstraint = SmoothMath.sigmoid(p, 50.0, 15.0);
            kStar = 5.0 * (1.0 - bandConstraint) + kStar * bandConstraint;

            // Sedentary infrastructure (mudbrick/masonry/granaries/plows) requires agricultural biomass
            // or high-power industrial grids; mobile foraging bands cannot transport stationary infrastructure (AGENTS.md §8).
            float[] agri = world.getBiomassAgriculture();
            float cellAgri = (agri != null && i < agri.length) ? agri[i] : 0.0f;
            double agriActivation = SmoothMath.sigmoid(cellAgri, 150.0, 25.0);
            double industrialActivation = SmoothMath.sigmoid(powerPerCapitaW, 520.0, 25.0);
            double sedentaryCapability = Math.max(agriActivation, industrialActivation);
            // Krausmann et al. (2017): sedentary agrarian masonry, granaries, canals & plows sustain ~145 kg/cap above foraging tools
            double agrarianMasonryStock = 145.0 * Math.tanh(Math.max(0.0, cellAgri) / (p * 180.0 + 1.0)) * bandConstraint;
            kStar = 5.0 + (kStar - 5.0 + agrarianMasonryStock) * sedentaryCapability;

            double dCapitalPerCapita = (baselineTurnover * kStar - effectiveDepreciationRate * capitalPerCapita) * dtInYears;
            double targetCapital = Math.max(0.0, (capitalPerCapita + dCapitalPerCapita) * p);
            double decayedCap = capital[i] * decayCapital;
            float nextCap = (float) (activeSettlement * targetCapital + (1.0 - activeSettlement) * decayedCap);

            // Lavoisier mass conservation on net positive capital formation (Krausmann et al. 2017 stoichiometry: 5% metal, 7% timber, 10% clay)
            double netAddedKg = nextCap - capital[i];
            if (netAddedKg > 0.0) {
                double consumedTonnes = netAddedKg * 1.0e-3;
                double dtClamped = Math.min(1.0, Math.max(0.0001, dtInYears));
                float[] metalArr = world.getMetalResource();
                if (metalArr != null && i < metalArr.length && metalArr[i] > 0.0f) {
                    metalArr[i] = (float) Math.max(0.0, metalArr[i] - Math.min(metalArr[i] * 0.05 * dtClamped, consumedTonnes * 0.05));
                }
                float[] woodArr = world.getWoodResource();
                if (woodArr != null && i < woodArr.length && woodArr[i] > 0.0f) {
                    woodArr[i] = (float) Math.max(0.0, woodArr[i] - Math.min(woodArr[i] * 0.05 * dtClamped, consumedTonnes * 0.07));
                }
                float[] clayArr = world.getClayResource();
                if (clayArr != null && i < clayArr.length && clayArr[i] > 0.0f) {
                    clayArr[i] = (float) Math.max(0.0, clayArr[i] - Math.min(clayArr[i] * 0.05 * dtClamped, consumedTonnes * 0.10));
                }
            }
            capital[i] = nextCap;

            // 4. Smooth Tainter Simplification: when complexity overextends beyond physical capital, complexity contracts
            double maintenanceDrag = SmoothMath.sigmoid(newComp, 4.0, 0.8);
            complexity[i] = (float) (newComp * (1.0 - 0.05 * maintenanceDrag * dtInYears));
        }
    }
}
