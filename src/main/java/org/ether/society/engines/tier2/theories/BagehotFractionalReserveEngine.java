/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Walter Bagehot Fractional-Reserve Liquidity &amp; Lender of Last Resort Engine.
 *
 * <p>Implements Walter Bagehot's foundational central banking and financial stability doctrine
 * (1873, <i>Lombard Street: A Description of the Money Market</i>):</p>
 * <ul>
 *   <li><b>Fractional-Reserve Banking Expansion:</b> Advanced commercial economies ($k_{\text{pc}} \ge 150$,
 *       trade network $\ge 20$) expand liquid working capital through credit circulation, amplifying
 *       investment velocity.</li>
 *   <li><b>Liquidity Panics:</b> When confidence or systemic stability is shocked (severe food shortage,
 *       war rupture, or sovereign debt distress), private credit contracts violently as depositors and
 *       banks hoard specie/bullion.</li>
 *   <li><b>Bagehot's Rule (Lender of Last Resort):</b> In a panic, the central bank must <i>"lend freely,
 *       at a high rate of interest, against good banking collateral"</i>. If public reserves are injected,
 *       the liquidity spiral is arrested; if absent or withheld, debt deflation (Irving Fisher 1933)
 *       destroys real productive capital.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://www.gutenberg.org">Bagehot, W. (1873), Lombard Street</a>
 * @see SovereignTreasuryBondEngine
 */
public class BagehotFractionalReserveEngine extends AbstractSimulationEngine {
    public BagehotFractionalReserveEngine() {
        super("BagehotFractionalReserveEngine", "BagehotFractionalReserveEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(BagehotFractionalReserveEngine.class);

    /** Capital per capita threshold for commercial credit expansion. */
    public static final double BANKING_KPC_FLOOR = 150.0;

    @Override
    public String getName() {
        return "BagehotFractionalReserveEngine";
    }

    @Override
    public String getDescription() {
        return "Models Walter Bagehot (1873) fractional-reserve liquidity panics and the stabilizing role of the central bank lender of last resort.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Bagehot Fractional Reserve & Lender of Last Resort (Bagehot 1873, Fisher 1933)]
               • Credit Multiplier:   M_credit = 1.0 + 0.35 · σ(k_pc - 150, 40)
               • Panic Trigger:       If Stress > 0.40 -> Private Credit Contraction ΔCredit = -0.15 · M · dt
               • Bagehot Injection:   If Lender_Last_Resort -> State Liquidity Buffer stabilizes Active Capital
               • Debt Deflation:      If No Lender -> Real Capital decays at 5-10%/yr under liquidity freeze
               Ref: W. Bagehot (1873), "Lombard Street: A Description of the Money Market", King & Co.
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Institutional Macroeconomics";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getBiomassHuman() == null || dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 20.0) return;

        double cap = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 0.0;
        double kpc = cap / pop;
        double trade = buffer.getCommercialFluxVolume() != null ? buffer.getCommercialFluxVolume()[i] : 0.0;

        double bankingActivation = SmoothMath.sigmoid(kpc, BANKING_KPC_FLOOR, 35.0)
                * SmoothMath.sigmoid(trade, 15.0, 5.0);

        if (bankingActivation > 0.05) {
            double food = buffer.getFoodResource() != null ? buffer.getFoodResource()[i] : 0.0;
            double foodPerCapita = food / pop;
            double systemicStress = Math.max(0.0, 1.0 - (foodPerCapita / 1.10));

            if (systemicStress > 0.25) {
                double creditFreezeLoss = 0.06 * bankingActivation * systemicStress * dtYears;
                double metalReserves = buffer.getMetalResource() != null ? buffer.getMetalResource()[i] : 0.0;
                boolean hasLenderOfLastResort = (metalReserves / pop) >= 15.0;

                if (hasLenderOfLastResort) {
                    double liquidityInjected = Math.min(metalReserves * 0.10, cap * creditFreezeLoss);
                    acc.dMetalResource -= (float) liquidityInjected;
                } else {
                    acc.dResourceCapital -= (float) (cap * creditFreezeLoss);
                }
            } else {
                double creditExpansion = 0.02 * bankingActivation * dtYears;
                acc.dResourceCapital += (float) (cap * creditExpansion);
            }
        }
    }
}
