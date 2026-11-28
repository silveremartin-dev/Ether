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
import org.ether.society.core.math.SmoothMath;
import org.ether.society.model.PhysicalConstants;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kenneth Pomeranz &amp; E. A. Wrigley Great Divergence &amp; Ghost Acreage Engine.
 *
 * <p>Implements the ecological-energetic Great Divergence framework of Kenneth Pomeranz
 * (2000, <i>The Great Divergence: China, Europe, and the Making of the Modern World Economy</i>)
 * and E. A. Wrigley (2010, <i>Energy and the English Industrial Revolution</i>):</p>
 * \[
 * A_{\text{ghost,coal}} = \frac{\dot{M}_{\text{coal}} \cdot \text{LHV}_{\text{coal}}}{y_{\text{wood}}}, \qquad
 * A_{\text{ghost,trade}} = \sum_c \frac{\dot{M}_{\text{import},c}}{y_{\text{land},c}}
 * \]
 * <ul>
 *   <li><b>Subterranean Forest Relief (Wrigley 2010):</b> Water-accessible bituminous coal
 *       (\(\text{LHV}_{\text{coal}} = 24\,\text{GJ/t}\)) substitutes for sustainable coppice woodland
 *       thermal yield (\(y_{\text{wood}} = 7\,500\,\text{GJ/(km}^2\cdot\text{yr)}\)), releasing domestic
 *       land from firewood competition.</li>
 *   <li><b>Transoceanic Colonial Ghost Acreage (Pomeranz 2000):</b> Maritime bulk imports of land-intensive
 *       commodities (timber, cotton, sugar, grain) further expand effective ecological acreage, whereas
 *       landlocked organic economies without accessible coal draw down standing forest biomass and involute.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see <a href="https://press.princeton.edu">Pomeranz, K. (2000), The Great Divergence</a>
 * @see AsymmetricColonialTradeEngine
 * @see KummelAyresExergyEngine
 */
public class PomeranzGreatDivergenceEngine extends AbstractSimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(PomeranzGreatDivergenceEngine.class);

    /** Minimum trade connectivity representing transoceanic colonial ghost acreage access. */
    public static final double GHOST_ACREAGE_TRADE_THRESHOLD = 25.0;

    /** Minimum coal stock threshold [metric tonnes] for commercial seam extraction. */
    public static final double COAL_BREAKOUT_STOCK_THRESHOLD = 500.0;

    /** Sustainable temperate coppice forestry thermal yield [GJ / (km^2 * yr)] (Wrigley 2010; Smil 2017). */
    public static final double COPPICE_WOODLAND_YIELD_GJ_PER_KM2_YR = 7_500.0;

    /** Reference subterranean forest land-equivalence scale [km^2] for industrial takeoff. */
    public static final double REFERENCE_SUBTERRANEAN_FOREST_KM2 =
            (COAL_BREAKOUT_STOCK_THRESHOLD * PhysicalConstants.COAL_LHV_GJ_PER_TONNE)
                    / COPPICE_WOODLAND_YIELD_GJ_PER_KM2_YR;

    public PomeranzGreatDivergenceEngine() {
        super("PomeranzGreatDivergenceEngine", "PomeranzGreatDivergenceEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

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
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        evaluateCellDivergence(buffer, i, dtYears, acc, false);
    }

    private static void evaluateCellDivergence(
            WorldBuffer buffer,
            int i,
            double dtYears,
            DerivativeAccumulator acc,
            boolean directMutate) {
        if (buffer == null || dtYears <= 0.0 || buffer.getHumanPopulation() == null) return;
        float p = buffer.getHumanPopulation()[i];
        if (p <= 0.5f) return;
        float[] capArr = buffer.getResourceCapital();
        float capital = (capArr != null) ? capArr[i] : 0.0f;
        double kpc = capital / p;
        if (kpc < 50.0) return;

        double coalTonnes = (buffer.getResourceCoal() != null) ? buffer.getResourceCoal()[i] : 0.0;
        double trade = (buffer.getCommercialFluxVolume() != null) ? buffer.getCommercialFluxVolume()[i] : 0.0;
        double friction = (!directMutate && buffer.getFluxPressure() != null && buffer.getFluxPressure()[i] > 0.0f)
                ? buffer.getFluxPressure()[i]
                : 0.50;

        // Wrigley (2010) subterranean forest equivalence: A_ghost,coal = (M_coal * LHV_coal) / y_wood [km^2]
        double subterraneanForestKm2 = (coalTonnes * PhysicalConstants.COAL_LHV_GJ_PER_TONNE)
                / COPPICE_WOODLAND_YIELD_GJ_PER_KM2_YR;
        double scaleWidthKm2 = (100.0 * PhysicalConstants.COAL_LHV_GJ_PER_TONNE)
                / COPPICE_WOODLAND_YIELD_GJ_PER_KM2_YR;
        double coalAvailability = SmoothMath.sigmoid(subterraneanForestKm2, REFERENCE_SUBTERRANEAN_FOREST_KM2, scaleWidthKm2);
        double transportAccessibility = 1.0 - SmoothMath.sigmoid(friction, 0.70, 0.15);
        double coalProximityFactor = coalAvailability * transportAccessibility;

        // Pomeranz (2000) transoceanic colonial ghost acreage factor
        double ghostAcreageFactor = SmoothMath.sigmoid(trade, GHOST_ACREAGE_TRADE_THRESHOLD, 5.0)
                * SmoothMath.sigmoid(kpc, 100.0, 30.0);
        double divergencePotential = coalProximityFactor * (1.0 + 0.80 * ghostAcreageFactor);

        if (divergencePotential > 0.15) {
            double dCap = capital * 0.055 * divergencePotential * dtYears;
            if (directMutate && capArr != null) {
                capArr[i] = (float) (capital + dCap);
            } else if (acc != null) {
                acc.dResourceCapital += (float) dCap;
            }
        } else if (kpc >= 80.0 && p >= 10.0f) {
            double dCapDrag = Math.min(Math.max(0.0, capital - 10.0), capital * 0.006 * dtYears);
            if (directMutate && capArr != null) {
                capArr[i] = (float) Math.max(10.0, capital - dCapDrag);
            } else if (acc != null) {
                acc.dResourceCapital -= (float) dCapDrag;
            }
            // Wrigley (2010) organic-economy forestry bottleneck: absent mineral coal, proto-industrial heat draws down standing wood biomass
            float[] natArr = buffer.getBiomassNatural();
            if (natArr != null && natArr[i] > 10.0f) {
                double woodDrawdown = natArr[i] * 0.012 * (1.0 - coalProximityFactor) * dtYears;
                if (directMutate) {
                    natArr[i] = (float) Math.max(10.0, natArr[i] - woodDrawdown);
                } else if (acc != null) {
                    acc.dBiomassNatural -= (float) woodDrawdown;
                }
            }
        }
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        super.tick(buffer, dtYears, dtMonthly);
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
        return "Models Kenneth Pomeranz (2000) and E. A. Wrigley (2010) Great Divergence: subterranean forest coal relief and transoceanic ghost acreage.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Pomeranz-Wrigley Great Divergence (Pomeranz 2000, Wrigley 2010)]
               • Subterranean Forest: A_coal = (M_coal · LHV_coal) / y_wood  [km²]
               • Water Access:        Φ_coal = σ(A_coal; A_0, s_A) · (1 - σ(μ; 0.70, 0.15))
               • Colonial Acreage:    Φ_ghost = σ(Trade; 25, 5) · σ(k_pc; 100, 30)
               • Divergence Relief:   Ω_div = Φ_coal · (1 + 0.80 · Φ_ghost)
               Ref: K. Pomeranz (2000), "The Great Divergence"; E. A. Wrigley (2010).
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
     * Evaluates Pomeranz-Wrigley subterranean forest and colonial ghost acreage relief directly over WorldBuffer.
     *
     * @param buffer contiguous world buffer
     * @param deltaYears time step duration in astronomical Julian years
     */
    public static void processGreatDivergenceDynamics(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || deltaYears <= 0.0) return;
        int n = buffer.getCapacity();
        for (int i = 0; i < n; i++) {
            evaluateCellDivergence(buffer, i, deltaYears, null, true);
        }
    }
}
