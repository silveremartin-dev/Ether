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
 * David Ricardo Differential Land Rent &amp; Extensive Agricultural Margin Engine.
 *
 * <p>Implements David Ricardo's classical political economy of land rent and diminishing returns
 * (1817, <i>On the Principles of Political Economy and Taxation</i>):</p>
 * <ul>
 *   <li><b>Extensive &amp; Intensive Margin:</b> As population density expands, agrarian production
 *       is pushed onto lower-fertility soils or steeper terrain, raising the marginal cost of food production.</li>
 *   <li><b>Differential Rent Extraction:</b> High-fertility intramarginal lands yield a surplus rent
 *       over marginal land ($R_i = Y_i - Y_{\text{marginal}}$) that is captured by non-producing landlords.</li>
 *   <li><b>Ricardian Stationary Drag:</b> Rising food prices and rental shares compress industrial/manufacturing
 *       profits and capital accumulation, driving the agrarian economy toward a stationary state unless
 *       relieved by free grain imports or technological breakthrough.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://www.econlib.org">Ricardo, D. (1817), Principles of Political Economy and Taxation</a>
 * @see BoserupAgriculturalIntensificationEngine
 * @see SoilNutrientNPKEngine
 */
public class RicardoDifferentialLandRentEngine extends AbstractSimulationEngine {
    public RicardoDifferentialLandRentEngine() {
        super("RicardoDifferentialLandRentEngine", "RicardoDifferentialLandRentEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(RicardoDifferentialLandRentEngine.class);

    /** Minimum agricultural capital per capita to generate substantial differential rent (kg/cap). */
    public static final double RENT_KPC_FLOOR = 40.0;

    @Override
    public String getName() {
        return "RicardoDifferentialLandRentEngine";
    }

    @Override
    public String getDescription() {
        return "Models David Ricardo (1817) differential land rent: demographic expansion onto marginal soils elevates landlord rent shares, inflating food costs and compressing capital accumulation.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [David Ricardo Differential Land Rent (Ricardo 1817)]
               • Pressure on Margin:   Φ_margin = σ((N · 1.20 / (Biomass_Agri + 1)) - 1.0, 0.25)
               • Differential Rent:    R_diff = 0.20 · Φ_margin · Biomass_Agri
               • Profit Compression:   dK/dt = -0.035 · Φ_margin · K · dt (Ricardian drag)
               • Inequality Surge:     dGini/dt = +0.02 · Φ_margin · dt
               Ref: D. Ricardo (1817), "On the Principles of Political Economy and Taxation", John Murray.
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Classical Macroeconomics";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        processDifferentialRentDynamics(cells, deltaYears);
    }

    /**
     * Evaluates Ricardian differential rent extraction, profit compression, and inequality across cells.
     *
     * @param cells      simulation cells
     * @param deltaYears time step duration in astronomical Julian years
     */
    public static void processDifferentialRentDynamics(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 20) continue;

            double agriBiomass = cell.getBiomassAgricultureOr(0.0);
            if (agriBiomass <= 50.0) continue;

            double capital = cell.getResourceCapitalOr(0.0);
            double kpc = capital / (double) pop;
            if (kpc < RENT_KPC_FLOOR) continue;

            // Subsistence food demand relative to local agricultural biomass endowment
            double foodDemand = (double) pop * 1.20;
            double marginalPressure = foodDemand / (agriBiomass + 1.0);

            // Continuous sigmoid activation of the extensive margin
            double marginFactor = SmoothMath.sigmoid(marginalPressure - 1.0, 0.0, 0.25);

            if (marginFactor > 0.05) {
                // 1. Rent capture by landed elite: converts productive agricultural surplus into landlord consumption
                double rentExtraction = 0.04 * marginFactor * deltaYears;
                cell.setBiomassAgriculture(Math.max(20.0, agriBiomass * (1.0 - rentExtraction * 0.30)));

                // 2. Ricardian profit squeeze: rising food costs and rents reduce capital reinvestment
                double capitalDrag = 0.035 * marginFactor * deltaYears;
                cell.setResourceCapital(Math.max(10.0, capital * (1.0 - capitalDrag)));

                // 3. Inequality increase: land concentration elevates Gini index
                double currentGini = cell.getGiniIndexOr(0.35);
                double deltaGini = 0.02 * marginFactor * deltaYears;
                cell.setGiniIndex(Math.min(0.85, currentGini + deltaGini));

                // 4. Transport/trade friction increases slightly due to localized rentier tolls and tariff walls
                double friction = cell.getMovementFrictionOr(1.0);
                cell.setMovementFriction(Math.min(3.0, friction + (0.015 * marginFactor * deltaYears)));
            }
        }
    }
}
