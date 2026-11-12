/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.core.math.SmoothMath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * W. Arthur Lewis Dual-Sector Model &amp; Turning Point Engine.
 *
 * <p>Implements the foundational developmental macroeconomics formulated by
 * Sir W. Arthur Lewis (1954, <i>Economic Development with Unlimited Supplies of Labour</i>; Nobel Prize 1979):</p>
 * <ul>
 *   <li><b>Surplus Rural Labor Phase:</b> In early industrializing societies ($k_{\text{pc}} \in [80, 250]$),
 *       the modern urban/industrial sector expands by drawing subsistence agricultural labor at a constant
 *       low real wage, maximizing capitalist profit margins and physical capital accumulation.</li>
 *   <li><b>The Lewis Turning Point:</b> When rural surplus labor is fully absorbed and urban-industrial
 *       employment exceeds the demographic absorption threshold, real agricultural wages begin to rise.</li>
 *   <li><b>Induced Mechanization &amp; Capital-Deepening:</b> Once past the turning point, rising labor costs
 *       compel manufacturers and agrarian estates to substitute machinery for human muscle, triggering
 *       accelerated capital-deepening and higher energy power dissipation ($P_{\text{watts}}/N$).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://doi.org/10.1111/j.1467-9957.1954.tb00021.x">Lewis, W. A. (1954), The Manchester School</a>
 * @see WestBettencourtAllometryEngine
 * @see SolowSwanCapitalEngine
 */
public class LewisDualSectorTurningPointEngine extends AbstractSimulationEngine {
    public LewisDualSectorTurningPointEngine() {
        super("LewisDualSectorTurningPointEngine", "LewisDualSectorTurningPointEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(LewisDualSectorTurningPointEngine.class);

    /** Capital per capita threshold marking the onset of dual-sector industrial absorption (kg/cap). */
    public static final double DUAL_SECTOR_KPC_ONSET = 80.0;

    /** Capital per capita threshold corresponding to the Lewis Turning Point (kg/cap). */
    public static final double LEWIS_TURNING_POINT_KPC = 250.0;

    @Override
    public String getName() {
        return "LewisDualSectorTurningPointEngine";
    }

    @Override
    public String getDescription() {
        return "Models W. Arthur Lewis (1954) dual-sector development: surplus rural labor fuels early industrial profits until the Lewis turning point forces real wage surges and mechanization.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [W. Arthur Lewis Dual-Sector Transition (Lewis 1954)]
               • Early Accumulation:  If k_pc < 250 -> ΔK = +0.035 · σ(k_pc - 80, 20) · K · dt (Surplus Labor)
               • Turning Point:       Φ_turning = σ(k_pc - 250, 40) · σ(TradeNet - 20, 5)
               • Induced Mechanize:   ΔK_deepening = +0.05 · Φ_turning · K · dt
               • Wage Share Growth:   ΔWork_Value = +0.03 · Φ_turning · Work · dt
               Ref: W. A. Lewis (1954), "Economic Development with Unlimited Supplies of Labour", The Manchester School.
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Developmental Macroeconomics";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getBiomassHuman() == null || dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 50.0) return;

        double capital = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 0.0;
        double kpc = capital / pop;
        if (kpc < DUAL_SECTOR_KPC_ONSET) return;

        double trade = buffer.getCommercialFluxVolume() != null ? buffer.getCommercialFluxVolume()[i] : 0.0;
        double earlyAbsorptionFactor = SmoothMath.sigmoid(kpc, DUAL_SECTOR_KPC_ONSET, 25.0)
                * (1.0 - SmoothMath.sigmoid(kpc, LEWIS_TURNING_POINT_KPC, 40.0));

        if (earlyAbsorptionFactor > 0.05) {
            double extGrowth = capital * 0.030 * earlyAbsorptionFactor * dtYears;
            acc.dResourceCapital += (float) extGrowth;
            capital += extGrowth;
        }

        double turningPointIntensity = SmoothMath.sigmoid(kpc, LEWIS_TURNING_POINT_KPC, 40.0)
                * SmoothMath.sigmoid(trade, 20.0, 5.0);

        if (turningPointIntensity > 0.05) {
            acc.dResourceCapital += (float) (capital * 0.045 * turningPointIntensity * dtYears);
            acc.dGiniIndex -= (float) (0.015 * turningPointIntensity * dtYears);
        }
    }
}
