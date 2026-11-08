/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
public class BagehotFractionalReserveEngine implements ProceduralEnginePlugin {

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
    public void process(List<H3Cell> cells, double deltaYears) {
        processBagehotBankingLiquidity(cells, deltaYears);
    }

    /**
     * Evaluates fractional-reserve credit dynamics and liquidity interventions across cells.
     *
     * @param cells      simulation cells
     * @param deltaYears time step duration in astronomical Julian years
     */
    public static void processBagehotBankingLiquidity(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 20) continue;

            double cap = cell.getResourceCapitalOr(0.0);
            double kpc = cap / (double) pop;
            double trade = cell.getTradeNetworkOr(0.0);

            // Fractional-reserve commercial banking density emerges continuously
            double bankingActivation = SmoothMath.sigmoid(kpc, BANKING_KPC_FLOOR, 35.0)
                    * SmoothMath.sigmoid(trade, 15.0, 5.0);

            if (bankingActivation > 0.05) {
                double food = cell.getFoodResourceOr(0.0);
                double foodPerCapita = food / (double) pop;
                double systemicStress = Math.max(0.0, 1.0 - (foodPerCapita / 1.10));

                if (systemicStress > 0.25) {
                    // Liquidity panic phase: private credit freeze threatens working capital
                    double creditFreezeLoss = 0.06 * bankingActivation * systemicStress * deltaYears;

                    // Evaluate whether sovereign reserves or state capacity acts as lender of last resort
                    double metalReserves = cell.getResourceMetalOr(0.0);
                    boolean hasLenderOfLastResort = (metalReserves / (double) pop) >= 15.0;

                    if (hasLenderOfLastResort) {
                        // Bagehot Intervention: State advances liquidity from bullion reserves to stem panic
                        double liquidityInjected = Math.min(metalReserves * 0.10, cap * creditFreezeLoss);
                        cell.setResourceMetal(metalReserves - liquidityInjected);
                        // Working capital preserved with zero debt-deflation destruction
                    } else {
                        // Unbuffered Panic: Debt deflation contracts productive active capital
                        cell.setResourceCapital(Math.max(10.0, cap * (1.0 - creditFreezeLoss)));
                        double work = cell.getResourceWorkOr(50.0);
                        cell.setResourceWork(Math.max(15.0, work * (1.0 - creditFreezeLoss * 0.5)));
                    }
                } else {
                    // Normal Expansion Phase: Credit multiplier deepens physical capital circulation smoothly
                    double creditExpansion = 0.02 * bankingActivation * deltaYears;
                    cell.setResourceCapital(cap * (1.0 + creditExpansion));
                }
            }
        }
    }
}
