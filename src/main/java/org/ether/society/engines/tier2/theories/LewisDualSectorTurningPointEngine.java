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
public class LewisDualSectorTurningPointEngine implements ProceduralEnginePlugin {

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
    public void process(List<H3Cell> cells, double deltaYears) {
        processDualSectorDynamics(cells, deltaYears);
    }

    /**
     * Evaluates dual-sector labor reallocation and the Lewis turning point across cells.
     *
     * @param cells      simulation cells
     * @param deltaYears time step duration in astronomical Julian years
     */
    public static void processDualSectorDynamics(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 50) continue;

            double capital = cell.getResourceCapitalOr(0.0);
            double kpc = capital / (double) pop;
            if (kpc < DUAL_SECTOR_KPC_ONSET) continue;

            double trade = cell.getTradeNetworkOr(0.0);

            // 1. Phase 1: Unlimited Surplus Labor Absorption (k_pc < 250)
            double earlyAbsorptionFactor = SmoothMath.sigmoid(kpc, DUAL_SECTOR_KPC_ONSET, 25.0)
                    * (1.0 - SmoothMath.sigmoid(kpc, LEWIS_TURNING_POINT_KPC, 40.0));

            if (earlyAbsorptionFactor > 0.05) {
                // High profit margins reinvested into extensive capital expansion
                double extensiveCapitalGrowth = 0.030 * earlyAbsorptionFactor * deltaYears;
                cell.setResourceCapital(capital * (1.0 + extensiveCapitalGrowth));
                capital = cell.getResourceCapitalOr(0.0);
            }

            // 2. Phase 2: Post-Lewis Turning Point (k_pc >= 250)
            // Surplus labor is exhausted; real wages rise, inducing labor-saving capital-deepening
            double turningPointIntensity = SmoothMath.sigmoid(kpc, LEWIS_TURNING_POINT_KPC, 40.0)
                    * SmoothMath.sigmoid(trade, 20.0, 5.0);

            if (turningPointIntensity > 0.05) {
                // Induced mechanization: capital accumulation accelerates as machinery replaces scarce labor
                double deepeningGrowth = 0.045 * turningPointIntensity * deltaYears;
                cell.setResourceCapital(capital * (1.0 + deepeningGrowth));

                // Work efficiency increases as productive power tools augment human hours
                double currentWork = cell.getResourceWorkOr(50.0);
                double workAugmentation = 0.025 * turningPointIntensity * deltaYears;
                cell.setResourceWork(Math.min(100.0, currentWork * (1.0 + workAugmentation)));

                // Income inequality moderates as rising real wages compress the landlord/capitalist Gini wedge
                double gini = cell.getGiniIndexOr(0.40);
                double giniCompression = 0.015 * turningPointIntensity * deltaYears;
                cell.setGiniIndex(Math.max(0.25, gini - giniCompression));
            }
        }
    }
}
