/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.typeb;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Nordhaus DICE Climate Damage Model Variant B8.1 (William Nordhaus, Nobel Prize 2018).
 * Pure standalone model of economic growth coupled with quadratic climate damage functions.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class NordhausDicePureEngine {
    private static final Logger logger = LoggerFactory.getLogger(NordhausDicePureEngine.class);

    private double gdp = 100.0;
    private double temperatureAnomaly = 1.2; // +1.2°C above pre-industrial baseline

    public void processTick(double deltaYears) {
        // Nordhaus Quadratic Damage Function D(T) = 0.00236 * T^2
        double damageFraction = 0.00236 * Math.pow(temperatureAnomaly, 2.0);
        double netGdp = gdp * (1.0 - damageFraction);

        // CO2 emissions proportional to gross GDP
        double emissions = netGdp * 0.05;
        temperatureAnomaly += emissions * 0.001 * deltaYears;

        gdp = netGdp * 1.02; // 2% baseline growth
    }

    public double getGdp() { return gdp; }
    public double getTemperatureAnomaly() { return temperatureAnomaly; }
}
