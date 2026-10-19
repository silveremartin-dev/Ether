package org.ether.society.core.dod;

import org.ether.society.model.PhysicalConstants;

/**
 * Data-Oriented Urban Agglomeration, Capital Accumulation & Institutional Entropy Kernel.
 * <p>
 * Models urban emergence at resource flux concentration nodes subject to Tainter's
 * thermodynamic maintenance constraints (diminishing marginal returns on complexity)
 * and West-Bettencourt allometric scaling.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class UrbanKernel {

    /**
     * Updates global infrastructure capital, technological spillover, and institutional complexity
     * across all active cells under thermodynamic conservation laws.
     *
     * @param world contiguous world state buffer
     * @param dt integration time step (years if &le; 20, days if &le; 1000, seconds if &gt; 1000)
     */
    public void tick(WorldBuffer world, float dt) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        float[] prices = world.getLocalPrice();
        float[] capital = world.getResourceCapital();
        float[] tech = world.getTechnologyLevel();
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
            if (pop[i] > 10.0f) {
                float effectiveTech = tech != null ? Math.max(0.5f, tech[i]) : 1.0f;
                float capitalPerCapita = pop[i] > 0.001f ? (capital[i] / pop[i]) : 0.0f;

                // 1. Institutional Complexity C (Tainter 1988, West-Bettencourt urban scaling):
                // Scales with intensive capital density (K/N) and settlement scale log(N)
                complexity[i] = (float) Math.log1p(capitalPerCapita * 0.25f + Math.log1p(pop[i] * 0.005f));

                // 2. Institutional Maintenance Overhead (Tainter Law of Diminishing Marginal Returns):
                // Baseline institutional depreciation (2%/yr, paired with 2%/yr physical grid depreciation = 4%/yr total)
                // + non-linear institutional complexity overhead + intensive capital maintenance
                float baselineDepreciation = 0.02f;
                float complexityOverhead = 0.012f * (float) Math.pow(complexity[i], PhysicalConstants.TAINTER_COMPLEXITY_EXPONENT);
                float intensiveMaintenanceDrag = 0.0015f * (capitalPerCapita / Math.max(1.0f, effectiveTech));
                float depreciationRate = Math.min(0.85f, baselineDepreciation + complexityOverhead + intensiveMaintenanceDrag);
                float maintenanceCost = capital[i] * depreciationRate;

                // 3. Gross Capital Formation (Solow-Swan / Smil exergy surplus mobilized by labor):
                // Pre-industrial societies (tech < 3.0) have low surplus fraction; industrial societies scale higher
                float priceFactor = prices != null ? Math.clamp(prices[i], 0.5f, 2.0f) : 1.0f;
                float surplusCoefficient = (effectiveTech < 3.0f) ? 0.06f : 0.12f;
                float grossProduction = priceFactor * pop[i] * surplusCoefficient * (float) Math.pow(effectiveTech, 0.85);
                float netAccumulation = (grossProduction - maintenanceCost) * dtInYears;

                capital[i] = Math.max(0.0f, capital[i] + netAccumulation);

                // 4. Endogenous Urban Agglomeration Innovation (Romer 1990 / Bairoch 1988):
                // True urban R&D spillover requires urban agglomeration threshold (pop >= 250) and intensive K/N > 10,
                // rather than raw extensive capital sum, and exhibits strong diminishing returns against current tech level.
                if (pop[i] >= 250.0f && capitalPerCapita > 10.0f && tech != null) {
                    float urbanScaleFactor = (float) Math.min(2.0, Math.log10(pop[i] / 100.0f));
                    float intensiveSurplus = (float) Math.log1p(capitalPerCapita - 10.0f);
                    // Innovation difficulty scales with tech^2 (Jones 1995 "Fishing Out" / Burden of Knowledge effect)
                    float knowledgeBurden = 1.0f + effectiveTech * effectiveTech * 0.5f;
                    float annualTechGain = (0.0015f * urbanScaleFactor * intensiveSurplus) / knowledgeBurden;
                    tech[i] += annualTechGain * dtInYears;
                }

                // 5. Tainter Simplification: when maintenance cost exceeds gross surplus, complexity contracts
                if (maintenanceCost > grossProduction) {
                    complexity[i] *= decayCapital;
                }
            } else {
                // Physical decay of abandoned infrastructure and institutional dissolution
                if (capital[i] > 0.0f) {
                    capital[i] = Math.max(0.0f, capital[i] * decayCapital);
                }
                if (complexity[i] > 0.0f) {
                    complexity[i] = Math.max(0.0f, complexity[i] * decayComplexity);
                }
            }
        }
    }
}
