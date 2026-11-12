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
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getBiomassHuman() == null || dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 10.0) return;

        double cap = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 0.0;
        double kpc = cap / pop;
        double trade = buffer.getCommercialFluxVolume() != null ? buffer.getCommercialFluxVolume()[i] : 0.0;

        double marketCommodification = SmoothMath.sigmoid(kpc, COMMODIFICATION_KPC_THRESHOLD, 20.0)
                * SmoothMath.sigmoid(trade, TRADE_INTEGRATION_THRESHOLD, 5.0);

        if (marketCommodification > 0.02) {
            double agri = buffer.getBiomassAgriculture() != null ? buffer.getBiomassAgriculture()[i] : 0.0;
            acc.dBiomassAgriculture += (float) (agri * 0.08 * marketCommodification * dtYears);

            double food = buffer.getFoodResource() != null ? buffer.getFoodResource()[i] : 0.0;
            double subsistenceStress = Math.max(0.0, 1.0 - ((food / pop) / 1.20));
            if (subsistenceStress > 0.10) {
                acc.dFluxPressure += (float) (0.05 * marketCommodification * subsistenceStress * dtYears);
                acc.dInstitutionalComplexity -= (float) (0.03 * marketCommodification * subsistenceStress * dtYears);
            }
        }
    }
}
