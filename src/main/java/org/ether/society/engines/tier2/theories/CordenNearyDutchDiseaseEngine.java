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
 * Corden-Neary Dutch Disease &amp; Resource Curse Engine.
 *
 * <p>Implements the canonical macroeconomic structural model of Dutch Disease and the Resource Curse
 * (W. Max Corden &amp; J. Peter Neary 1982, <i>Booming Sector and De-Industrialisation in a Small Open Economy</i>,
 * The Economic Journal 92(368); Richard Auty 1993):</p>
 * <ul>
 *   <li><b>Resource Boom &amp; Spending Effect:</b> A massive discovery or surge in mineral, metallic,
 *       or fossil energy extraction generates windfall export rents, drawing capital and labor into the extractive sector.</li>
 *   <li><b>Lagging Sector De-Industrialization (Crowding Out):</b> Factor prices (local wages, non-tradables)
 *       rise, making domestic manufacturing and diversified agriculture uncompetitive compared to imports.</li>
 *   <li><b>Rentier State Fragility:</b> The regional economy loses domestic manufacturing innovation and becomes
 *       vulnerable to commodity price busts, depletion, and external supply interruptions.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://doi.org/10.2307/2232670">Corden & Neary (1982), The Economic Journal</a>
 * @see HotellingResourceDepletionEngine
 */
public class CordenNearyDutchDiseaseEngine extends AbstractSimulationEngine {
    public CordenNearyDutchDiseaseEngine() {
        super("CordenNearyDutchDiseaseEngine", "CordenNearyDutchDiseaseEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(CordenNearyDutchDiseaseEngine.class);

    /** Resource rent ratio threshold where Dutch Disease factor crowding-out begins. */
    public static final double EXTRACTIVE_RENT_THRESHOLD = 0.25;

    @Override
    public String getName() {
        return "CordenNearyDutchDiseaseEngine";
    }

    @Override
    public String getDescription() {
        return "Models Corden-Neary (1982) Dutch Disease and resource curse: mineral/fossil export booms appreciate local factor costs and crowd out diversified domestic manufacturing.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Corden-Neary Dutch Disease & Resource Curse (Corden & Neary 1982, Auty 1993)]
               • Rent Ratio:       R_boom = Metal_surplus / (Total_Capital + 1)
               • Spending Effect:  If R_boom > 0.25 -> Crowding out factor active
               • De-Industrialize: ΔAgri/ΔMfg = -0.05 · σ(R_boom - 0.25, 0.08) · dt
               • Rentier Shock:    When reserves deplete, non-resource economy has degraded capital
               Ref: W. M. Corden & J. P. Neary (1982), "Booming Sector and De-Industrialisation in a Small Open Economy", Econ. J.
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
        double metal = buffer.getMetalResource() != null ? buffer.getMetalResource()[i] : 0.0;
        double coal = buffer.getResourceCoal() != null ? buffer.getResourceCoal()[i] : 0.0;
        double oil = buffer.getResourceOil() != null ? buffer.getResourceOil()[i] : 0.0;

        double extractiveSurplus = metal + (coal * 0.5) + (oil * 0.8);
        double rentRatio = extractiveSurplus / Math.max(100.0, cap + extractiveSurplus);
        double dutchDiseaseIntensity = SmoothMath.sigmoid(rentRatio, EXTRACTIVE_RENT_THRESHOLD, 0.08);

        if (dutchDiseaseIntensity > 0.05) {
            double agri = buffer.getBiomassAgriculture() != null ? buffer.getBiomassAgriculture()[i] : 0.0;
            if (agri > 50.0) {
                acc.dBiomassAgriculture -= (float) (agri * 0.03 * dutchDiseaseIntensity * dtYears);
            }
            acc.dFluxPressure += (float) (0.02 * dutchDiseaseIntensity * dtYears);
            acc.dResourceCapital += (float) (extractiveSurplus * 0.015 * dtYears);
        }
    }
}
