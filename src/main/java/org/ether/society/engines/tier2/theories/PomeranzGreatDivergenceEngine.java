/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.engine.SimulationEngineModule;
import org.ether.society.database.H3Cell;

import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Kenneth Pomeranz Great Divergence &amp; Ecological Ghost Acreage Engine.
 *
 * <p>Implements Kenneth Pomeranz's foundational comparative historical cliodynamics
 * (2000, <i>The Great Divergence: China, Europe, and the Making of the Modern World Economy</i>; Robert Allen 2009):</p>
 * <ul>
 *   <li><b>Symmetric Pre-Industrial Commercialization:</b> As of ~1750 CE, core advanced regions of Eurasia
 *       (Britain and the Yangtze River Delta) shared comparable levels of life expectancy, literacy,
 *       free product and labor markets, and proto-industrial textile crafts.</li>
 *   <li><b>The Coal Transport Proximity Factor:</b> British industrial breakout was catalyzed by the accidental
 *       geographic proximity of thick, surface coal deposits to navigable water and iron ore, driving down
 *       thermal energy costs. By contrast, Chinese coal was situated 1,500 km away in the arid, landlocked northwest (Shanxi).</li>
 *   <li><b>Colonial "Ghost Acreage":</b> Transoceanic colonial imports (American timber, cotton, and sugar)
 *       substituted for millions of hectares of domestic agrarian/forest land, preventing Britain from choking
 *       on domestic ecological resource limits, whereas the Yangtze delta was constrained to labor-intensive involution.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://press.princeton.edu">Pomeranz, K. (2000), The Great Divergence</a>
 * @see AsymmetricColonialTradeEngine
 * @see KummelAyresExergyEngine
 */
public class PomeranzGreatDivergenceEngine extends AbstractSimulationEngine {
    public PomeranzGreatDivergenceEngine() {
        super("PomeranzGreatDivergenceEngine", "PomeranzGreatDivergenceEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(PomeranzGreatDivergenceEngine.class);

    /** Minimum trade connectivity representing transoceanic colonial ghost acreage access. */
    public static final double GHOST_ACREAGE_TRADE_THRESHOLD = 25.0;

    /** Minimum coal stock threshold for local mineral fuel breakout. */
    public static final double COAL_BREAKOUT_STOCK_THRESHOLD = 500.0;

    @Override
    public String id() {
        return "PomeranzGreatDivergenceEngine";
    }

    @Override
    public String name() {
        return getName();
    }

    @Override
    public EngineTier tier() {
        return EngineTier.TIER_2_CLIODYNAMIC;
    }

    @Override
    public boolean isEnabled(Scenario scenario) {
        return scenario != null && scenario.getTypeBEngineStates() != null
                && Boolean.TRUE.equals(scenario.getTypeBEngineStates().get(id()));
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        processBuffer(buffer, dtYears);
    }

    @Override
    public void reset() {
        // Stateless physical operator
    }

    @Override
    public String getName() {
        return "PomeranzGreatDivergenceEngine";
    }

    @Override
    public String getDescription() {
        return "Models Kenneth Pomeranz (2000) Great Divergence: industrial breakout driven by low-friction coal proximity and colonial ghost acreage relieving domestic ecological ceilings.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Kenneth Pomeranz Great Divergence (Pomeranz 2000, Allen 2009)]
               • Coal Access:         Φ_coal = σ(Coal - 500, 100) · (1.0 - σ(Friction - 0.70, 0.15))
               • Ghost Acreage:       Φ_ghost = σ(TradeNet - 25, 5) · σ(k_pc - 100, 30)
               • Divergence Relief:   Ω_div = Φ_coal · (1.0 + 0.80 · Φ_ghost)
               • Takeoff vs Stagnate: If Ω_div > 0.15 -> ΔK = +0.055 · Ω_div · K · dt (Industrial Takeoff)
                                      Else if High Pop -> ΔK = +0.005 · K · dt (Involution Drag)
               Ref: K. Pomeranz (2000), "The Great Divergence", Princeton University Press.
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Cliodynamics & Comparative History";
    }

    @Override
    public void processBuffer(WorldBuffer buffer, double deltaYears) {
        processGreatDivergenceDynamics(buffer, deltaYears);
    }

    /**
     * Evaluates Pomeranz coal proximity and colonial ghost acreage relief directly over WorldBuffer.
     *
     * @param buffer contiguous world buffer
     * @param deltaYears time step duration in astronomical Julian years
     */
    public static void processGreatDivergenceDynamics(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || deltaYears <= 0.0) return;
        int n = buffer.getCapacity();
        float[] popArr = buffer.getBiomassHuman();
        float[] capArr = buffer.getResourceCapital();
        float[] coalArr = buffer.getResourceCoal();
        float[] natArr = buffer.getBiomassNatural();
        float[] commArr = buffer.getCommercialFluxVolume();

        for (int i = 0; i < n; i++) {
            float p = popArr[i];
            if (p <= 0.5f) continue;
            float capital = (capArr != null) ? capArr[i] : 0.0f;
            double kpc = capital / p;
            if (kpc < 50.0) continue;

            double coal = (coalArr != null) ? coalArr[i] : 0.0;
            double trade = (commArr != null) ? commArr[i] : 0.0;
            double friction = 0.50; // navigable water baseline

            double coalAvailability = SmoothMath.sigmoid(coal, COAL_BREAKOUT_STOCK_THRESHOLD, 100.0);
            double transportAccessibility = 1.0 - SmoothMath.sigmoid(friction, 0.70, 0.15);
            double coalProximityFactor = coalAvailability * transportAccessibility;

            double ghostAcreageFactor = SmoothMath.sigmoid(trade, GHOST_ACREAGE_TRADE_THRESHOLD, 5.0)
                    * SmoothMath.sigmoid(kpc, 100.0, 30.0);

            double divergencePotential = coalProximityFactor * (1.0 + 0.80 * ghostAcreageFactor);

            if (divergencePotential > 0.15) {
                double takeoffGrowth = 0.055 * divergencePotential * deltaYears;
                if (capArr != null) {
                    capArr[i] = (float) (capital * (1.0 + takeoffGrowth));
                }
                if (natArr != null) {
                    natArr[i] = (float) (natArr[i] + (p * 0.02 * divergencePotential * deltaYears));
                }
            } else if (kpc >= 80.0 && p >= 10.0f) {
                double involutionDrag = 0.015 * deltaYears;
                if (capArr != null) {
                    capArr[i] = (float) Math.max(10.0, capital * (1.0 - involutionDrag * 0.40));
                }
            }
        }
    }
}
