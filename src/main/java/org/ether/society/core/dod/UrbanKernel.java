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
                // Baseline institutional depreciation + non-linear institutional complexity overhead
                float baselineDepreciation = 0.02f;
                float complexityOverhead = 0.001f * (float) Math.pow(Math.min(5.0f, complexity[i]), 1.5);
                float depreciationRate = Math.min(0.08f, baselineDepreciation + complexityOverhead);
                float maintenanceCost = capital[i] * depreciationRate;

                // 3. Gross Capital Formation (Solow-Swan / Smil exergy surplus mobilized by labor):
                // Replaces physical & institutional depreciation at steady-state equilibrium k* while allowing
                // modest intensive growth proportional to technological efficiency and local trade terms.
                float priceFactor = (prices != null && prices[i] > 0.01f) ? Math.clamp(prices[i], 0.95f, 1.15f) : 1.0f;
                float tfp = (float) Math.pow(effectiveTech, 0.65) * priceFactor;
                float intensiveKpc = Math.max(1.0f, capitalPerCapita);
                float cobbDouglasOutputPerCapita = 12.0f * tfp * (float) Math.pow(intensiveKpc, 0.30);
                float savingsRate = (effectiveTech < 3.0f) ? 0.08f : 0.14f;
                float grossInvestment = pop[i] * savingsRate * cobbDouglasOutputPerCapita;

                // Crisis & warfare capital destruction / disinvestment (Broadberry & Harrison 2005)
                float warDamageRate = (complexity[i] > 3.8f) ? 0.012f * (complexity[i] - 3.8f) : 0.0f;
                float totalDepreciationRate = Math.min(0.09f, depreciationRate + warDamageRate);
                float actualMaintenanceCost = capital[i] * totalDepreciationRate;

                float netAccumulation = (grossInvestment - actualMaintenanceCost) * dtInYears;
                // Bound annual capital growth to historical macroscopic macro-frontier (+2.0%/yr max, Maddison 2020)
                float warDrag = (complexity[i] > 3.8f) ? Math.min(0.016f, 0.008f * (complexity[i] - 3.8f)) : 0.0f;
                float maxGrowthRate = Math.max(0.004f, 0.020f - warDrag) * capital[i] * dtInYears;
                if (netAccumulation > maxGrowthRate) {
                    netAccumulation = maxGrowthRate;
                }
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
                if (actualMaintenanceCost > grossInvestment) {
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
