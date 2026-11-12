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
import org.ether.society.database.H3Cell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Karl Polanyi Fictitious Commodities &amp; The Great Transformation Engine.
 *
 * <p>Implements the institutional macroeconomic and sociological model formulated by
 * Karl Polanyi (1944, <i>The Great Transformation: The Political and Economic Origins of Our Time</i>):</p>
 * <ul>
 *   <li><b>Commodification of Land and Labor:</b> As commercial market density expands ($k_{\text{pc}} \ge 80$,
 *       trade integration $\ge 15$), customary communal land use (the commons) and traditional kinship-based
 *       labor reciprocity are transformed into "fictitious commodities" bought and sold on unregulated markets
 *       (e.g. British Enclosure Acts, Speenhamland abolition).</li>
 *   <li><b>Short-Term Efficiency Gain:</b> Market allocation accelerates agricultural and mineral output yield
 *       by $+5\text{--}12\%$ through efficient capital mobilization.</li>
 *   <li><b>Destruction of Social Safety Nets:</b> Commodification strips traditional communal reserves,
 *       rendering populations intensely vulnerable to market price fluctuations, wage compression, and crop failures.</li>
 *   <li><b>The Double Movement (Countermovement):</b> Popular immiseration and dislocation trigger protective
 *       social counter-reactions (labor unions, agrarian protectionism, riots, legislative caps), increasing
 *       sociopolitical stress ($\text{PSI}$) when market exposure exceeds biological coping thresholds.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://www.beacon.org">Polanyi, K. (1944), The Great Transformation</a>
 * @see TurchinGoldstoneSDTEngine
 * @see OstromCommonsPureEngine
 */
public class PolanyiFictitiousCommoditiesEngine extends AbstractSimulationEngine {
    public PolanyiFictitiousCommoditiesEngine() {
        super("PolanyiFictitiousCommoditiesEngine", "PolanyiFictitiousCommoditiesEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(PolanyiFictitiousCommoditiesEngine.class);

    /** Capital per capita threshold where market enclosure and commodification begin (kg/cap). */
    public static final double COMMODIFICATION_KPC_THRESHOLD = 80.0;

    /** Trade integration threshold for market commodification. */
    public static final double TRADE_INTEGRATION_THRESHOLD = 15.0;

    @Override
    public String getName() {
        return "PolanyiFictitiousCommoditiesEngine";
    }

    @Override
    public String getDescription() {
        return "Models Karl Polanyi (1944) Great Transformation: commodification of land/labor yields allocation efficiency gains but dissolves social safety nets, triggering the protective double movement.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Polanyi Fictitious Commodities & Double Movement (Polanyi 1944)]
               • Commodification Index: M_com = σ(k_pc - 80, 20) · σ(Trade_Net - 15, 5)
               • Allocation Efficiency: ΔYield = +0.08 · M_com · Biomass_Agri · dt
               • Social Dislocation:    Dislocation = M_com · (1.0 - Food / (Pop · 1.2)) · 0.15
               • Double Movement:       If Dislocation > 0.05 -> Political Stress PSI increases smoothly
               Ref: K. Polanyi (1944), "The Great Transformation: The Political and Economic Origins of Our Time", Beacon Press.
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Institutional Macroeconomics";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        processPolanyiTransformation(cells, deltaYears);
    }

    /**
     * Evaluates market commodification and protective countermovement across cells.
     *
     * @param cells      simulation cells
     * @param deltaYears time step duration in astronomical Julian years
     */
    public static void processPolanyiTransformation(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 10) continue;

            double cap = cell.getResourceCapitalOr(0.0);
            double kpc = cap / (double) pop;
            double trade = cell.getTradeNetworkOr(0.0);

            // Commodification emerges continuously with capital density and commercial market access
            double marketCommodification = SmoothMath.sigmoid(kpc, COMMODIFICATION_KPC_THRESHOLD, 20.0)
                    * SmoothMath.sigmoid(trade, TRADE_INTEGRATION_THRESHOLD, 5.0);

            if (marketCommodification > 0.02) {
                // 1. Allocation efficiency gain: enclosure and market incentives boost agricultural yield
                double agri = cell.getBiomassAgricultureOr(0.0);
                double efficiencyBoost = 0.08 * marketCommodification * deltaYears;
                cell.setBiomassAgriculture(agri * (1.0 + efficiencyBoost));

                // 2. Vulnerability to subsistence stress without customary communal buffers
                double food = cell.getFoodResourceOr(0.0);
                double perCapitaFood = food / (double) pop;
                double subsistenceStress = Math.max(0.0, 1.0 - (perCapitaFood / 1.20));

                if (subsistenceStress > 0.10) {
                    // Polanyi Double Movement: dislocated population pushes back against unbuffered market shocks
                    // Friction increases as protective regulations and social resistance emerge
                    double currentFriction = cell.getMovementFrictionOr(1.0);
                    double resistanceFriction = 0.05 * marketCommodification * subsistenceStress * deltaYears;
                    cell.setMovementFriction(Math.min(3.0, currentFriction + resistanceFriction));

                    // Work capacity is partially diverted toward social unrest and collective protective action
                    double currentWork = cell.getResourceWorkOr(50.0);
                    double workDiversion = 0.04 * marketCommodification * subsistenceStress * deltaYears;
                    cell.setResourceWork(Math.max(15.0, currentWork * (1.0 - workDiversion)));
                }
            }
        }
    }
}
