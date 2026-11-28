/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;

/**
 * Krugman Core-Periphery &amp; New Economic Geography Engine (Paul Krugman, Nobel 1991).
 *
 * <p>Models endogenous spatial agglomeration, trade costs, and forward-backward linkages
 * in strictly H3-resolution-invariant spatial density coordinates (\(\text{kg/km}^2\) and
 * \(\text{hab/km}^2\), {@code AGENTS.md} Contract 3):</p>
 * <ul>
 *   <li><b>Centripetal Forces</b>: Increasing returns in manufacturing, labor market pooling, low transport costs.</li>
 *   <li><b>Centrifugal Forces</b>: Agricultural dispersion, land rents, transport congestion.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class KrugmanCorePeripheryEngine extends AbstractSimulationEngine {

    private transient double meanCapitalDensityPerKm2 = 10.0;

    /**
     * Constructs the Krugman Core-Periphery NEG engine.
     */
    public KrugmanCorePeripheryEngine() {
        super("KrugmanCorePeripheryEngine", "KrugmanCorePeripheryEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public String getName() {
        return "Krugman Core-Periphery Spatial Agglomeration (NEG)";
    }

    @Override
    public String getDescription() {
        return "Models endogenous industrial clustering, forward/backward linkages, and bifurcation from dispersed to core-periphery geography.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Krugman New Economic Geography (JPE 1991)]
               • Market Potential:      Ω_i = Σ_j [ Income_j · exp(-τ · dist_ij) ]
               • Agglomeration Factor:  A_i = σ(2.0 · (k_area,i / k_area,mean - 1)) · (Access_i / (1 + Access_i))
               • Friction Reduction:    Δμ = -2% · A_i · Δt
               • Bifurcation Threshold: Symmetrical distribution destabilizes when τ falls below critical value.
               Units: k_area [kg/km^2], Access [dimensionless], τ [km^-1]
               Ref: P. Krugman (1991) "Increasing Returns and Economic Geography", Journal of Political Economy
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Spatial Economics";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void prepareTick(WorldBuffer buffer, double dtYears) {
        meanCapitalDensityPerKm2 = 10.0;
        if (buffer == null || buffer.getResourceCapital() == null || buffer.getHumanPopulation() == null) return;
        float[] capArr = buffer.getResourceCapital();
        float[] popArr = buffer.getHumanPopulation();
        double capSum = 0.0;
        double areaSumKm2 = 0.0;
        int n = buffer.getCapacity();
        for (int i = 0; i < n; i++) {
            if (popArr[i] > 1.0f && capArr[i] > 0.0f) {
                capSum += capArr[i];
                areaSumKm2 += Math.max(0.01, buffer.getCellAreaKm2(i));
            }
        }
        meanCapitalDensityPerKm2 = areaSumKm2 > 0.0 ? Math.max(1.0, capSum / areaSumKm2) : 10.0;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getHumanPopulation() == null || dtYears <= 0.0) return;
        double pop = buffer.getHumanPopulation()[i];
        if (pop <= 1.0) return;

        double capital = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 10.0;
        double areaKm2 = Math.max(0.01, buffer.getCellAreaKm2(i));
        double popDensityKm2 = pop / areaKm2;
        double capDensityKm2 = capital / areaKm2;
        double friction = buffer.getFluxPressure() != null ? Math.max(0.2, 1.0 + buffer.getFluxPressure()[i]) : 1.0;

        // Resolution-invariant accessibility index (equivalent to pop * 0.001 at Res 4 ~ 1,770 km^2)
        double accessibilityIndex = (popDensityKm2 * 1.77) / friction;
        double relCapital = capDensityKm2 / meanCapitalDensityPerKm2;
        double capitalAdvantage = 1.0 / (1.0 + Math.exp(-2.0 * (relCapital - 1.0)));
        double accessAdvantage = accessibilityIndex / (1.0 + accessibilityIndex);
        double centripetalFactor = 2.0 * capitalAdvantage * accessAdvantage;

        acc.dFluxPressure -= (float) (0.02 * centripetalFactor * dtYears);
        double centrifugalCongestion = 0.01 * (relCapital / Math.max(1.0, accessibilityIndex));
        double netAgglomerationRate = Math.clamp((0.015 * centripetalFactor - centrifugalCongestion) * dtYears, -0.05 * dtYears, 0.08 * dtYears);
        acc.dResourceCapital += (float) (capital * netAgglomerationRate);
    }
}
