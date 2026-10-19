*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Nordhaus DICE Climate Damage Model Variant B8.1 (William Nordhaus, Nobel Prize 2018).
 * Pure standalone model of economic growth coupled with quadratic climate damage functions.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class NordhausDicePureEngine {
    private static final Logger logger = LoggerFactory.getLogger(NordhausDicePureEngine.class);

    /** Internal state: gdp (double). */
    private double gdp = 100.0;
    private double temperatureAnomaly = 1.2; // +1.2°C above pre-industrial baseline

    /**
     * Process tick.
     *
     * @param deltaYears the delta years parameter (double)
     */
    public void processTick(double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        // Nordhaus Quadratic Damage Function D(T) = 0.00236 * T^2
        double damageFraction = 0.00236 * Math.pow(temperatureAnomaly, 2.0);
        double netGdp = gdp * (1.0 - damageFraction);

        // CO2 emissions proportional to gross GDP
        double emissions = netGdp * 0.05;
        temperatureAnomaly += emissions * 0.001 * deltaYears;

        gdp = Math.max(0.0, netGdp * (1.0 + 0.02 * deltaYears));
    }

    /**
     * Get gdp.
     *
     * @return the resulting computation or state reference
     */
    public double getGdp() { return gdp; }
    /**
     * Get temperature anomaly.
     *
     * @return the resulting computation or state reference
     */
    public double getTemperatureAnomaly() { return temperatureAnomaly; }

    /**
     * Executes hybrid spatial coupling for populated grid cells applying Nordhaus quadratic climate damage.
     *
     * @param cells List of populated cells
     * @param deltaYears Simulation time step in years
     */
    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        NordhausDiceHybridEngine.processPlugin(cells, deltaYears);
    }
}


