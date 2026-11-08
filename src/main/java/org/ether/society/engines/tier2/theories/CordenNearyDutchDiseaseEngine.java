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
public class CordenNearyDutchDiseaseEngine implements ProceduralEnginePlugin {

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
    public void process(List<H3Cell> cells, double deltaYears) {
        processDutchDiseaseDynamics(cells, deltaYears);
    }

    /**
     * Evaluates Dutch Disease factor crowding-out and lagging-sector de-industrialization across cells.
     *
     * @param cells      simulation cells
     * @param deltaYears time step duration in astronomical Julian years
     */
    public static void processDutchDiseaseDynamics(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 20) continue;

            double cap = cell.getResourceCapitalOr(0.0);
            double metal = cell.getResourceMetalOr(0.0);
            double coal = cell.getResourceCoalOr(0.0);
            double oil = cell.getResourceOilOr(0.0);

            // Total extractive resource wealth
            double extractiveSurplus = metal + (coal * 0.5) + (oil * 0.8);
            double rentRatio = extractiveSurplus / Math.max(100.0, cap + extractiveSurplus);

            // Continuous crowding-out activation when extractive rents dominate the local economy
            double dutchDiseaseIntensity = SmoothMath.sigmoid(rentRatio, EXTRACTIVE_RENT_THRESHOLD, 0.08);

            if (dutchDiseaseIntensity > 0.05) {
                // 1. Spending effect: windfall rents appreciate costs and crowd out domestic agricultural diversification
                double agri = cell.getBiomassAgricultureOr(0.0);
                if (agri > 50.0) {
                    double agriculturalCrowdOut = 0.03 * dutchDiseaseIntensity * deltaYears;
                    cell.setBiomassAgriculture(Math.max(20.0, agri * (1.0 - agriculturalCrowdOut)));
                }

                // 2. High transport friction on non-extractive goods due to port and infrastructure monopolization by raw ore
                double currentFriction = cell.getMovementFrictionOr(1.0);
                double factorAppreciation = 0.02 * dutchDiseaseIntensity * deltaYears;
                cell.setMovementFriction(Math.min(3.0, currentFriction + factorAppreciation));

                // 3. Extractive capital expands while diversified domestic capital suffers a technological stall
                double windfallInvestment = extractiveSurplus * 0.015 * deltaYears;
                cell.setResourceCapital(cap + windfallInvestment);
            }
        }
    }
}
