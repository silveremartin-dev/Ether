*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Entropic Metal Dissipation Engine (Bihouix Recycling Limits).
 * Models irreversible physical dissipation of metals into environment due to entropy generation.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class EntropicMetalDissipationEngine {
    private static final Logger logger = LoggerFactory.getLogger(EntropicMetalDissipationEngine.class);

    private static double annualDissipationRate = 0.015; // 1.5% annual thermodynamic loss rate

    /**
     * Integrates irreversible entropic metal dissipation over elapsed temporal intervals.
     * <p>
     * Implements continuous exponential decay based on Philippe Bihouix's physical dissipation limits:
     * \[
     * M(t + \Delta t) = M(t) \cdot e^{-\lambda_{\text{diss}} \cdot \Delta t}
     * \]
     * where \(\lambda_{\text{diss}}\) is the annual dissipation rate (\(\text{yr}^{-1}\)).
     * Lost metallic mass is converted into persistent particulate pollution \(\Delta P = 0.1 \cdot \Delta M\).
     * </p>
     *
     * @param cells target planetary hex cells containing metallic capital reserves
     * @param deltaYears time step interval in terrestrial decimal years (\(\Delta t > 0\))
     */
    public static void processEntropicDissipation(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null || cell.getResourceMetal() == null) continue;

            double metal = cell.getResourceMetal();
            // Irreversible entropic loss (continuous exponential decay)
            double remainingMetal = Math.max(0.0, metal * Math.exp(-annualDissipationRate * deltaYears));
            double dissipated = metal - remainingMetal;
            cell.setResourceMetal(remainingMetal);

            // Dissipated metals add to micro-pollution level
            if (cell.getPollutionLevel() != null && dissipated > 0.0) {
                cell.setPollutionLevel(cell.getPollutionLevel() + dissipated * 0.1);
            }
        }
    }

    /**
     * Gets current annual thermodynamic dissipation rate \(\lambda_{\text{diss}}\).
     *
     * @return annual dissipation fraction in \(\text{yr}^{-1}\)
     */
    public static double getAnnualDissipationRate() { return annualDissipationRate; }

    /**
     * Sets annual thermodynamic dissipation rate with physical clamping in \([0.0, 0.20]\).
     *
     * @param rate dissipation fraction in \(\text{yr}^{-1}\)
     */
    public static void setAnnualDissipationRate(double rate) { annualDissipationRate = Math.max(0.0, Math.min(0.20, rate)); }
    
    /**
     * Resets thermodynamic metal dissipation rate for scenario isolation.
     */
    public static void resetState() {
        annualDissipationRate = 0.015;
    }
}


