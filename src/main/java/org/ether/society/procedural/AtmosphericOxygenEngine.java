/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Atmospheric Oxygen Partial Pressure & Aerobic Capacity Engine.
 * Models:
 * 1. <b>Oxygen Partial Pressure (P_O2 in atm)</b>: P_O2 = P_atmo * O2_ratio.
 * 2. <b>Aerobic VO2max Capacity</b>: Human physical labor capacity scales with oxygen partial pressure.
 * 3. <b>Wildfire Hyper-Combustion</b>: High O2 (> 0.25 atm) accelerates forest fire propagation speed by up to 5x.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class AtmosphericOxygenEngine {
    private static final Logger logger = LoggerFactory.getLogger(AtmosphericOxygenEngine.class);

    /** Standard sea-level oxygen partial pressure in atm */
    public static final double STANDARD_O2_PRESSURE_ATM = 0.21;

    /**
     * Calculates aerobic physical labor capacity multiplier based on O2 partial pressure.
     *
     * @param o2PressureAtm Oxygen partial pressure in atm
     * @return VO2max efficiency factor between 0.1 and 1.2
     */
    public static double calculateAerobicCapacityFactor(double o2PressureAtm) {
        if (o2PressureAtm <= 0.05) return 0.1; // Severe hypoxia
        return Math.min(1.2, o2PressureAtm / STANDARD_O2_PRESSURE_ATM);
    }

    /**
     * Executes one atmospheric oxygen and hyper-combustion tick across cells.
     */
    public static void processAtmosphericOxygen(List<H3Cell> cells, double globalO2Ratio, double globalAtmoPressure) {
        if (cells == null || cells.isEmpty()) return;

        double o2PressureAtm = globalAtmoPressure * globalO2Ratio;
        double vo2maxFactor = calculateAerobicCapacityFactor(o2PressureAtm);

        int hyperCombustionEvents = 0;

        for (H3Cell cell : cells) {
            // Apply VO2max capacity limit to human physical labor output
            if (cell.getPopulation() != null && cell.getPopulation() > 0) {
                cell.setMovementFriction(Math.max(0.2, cell.getMovementFriction() / vo2maxFactor));
            }

            // Wildfire hyper-combustion under elevated O2 (> 0.25 atm)
            if (o2PressureAtm > 0.25 && cell.getBiomassNatural() != null && cell.getBiomassNatural() > 500.0) {
                hyperCombustionEvents++;
                cell.setBiomassNatural(cell.getBiomassNatural() * 0.92); // Accelerated forest burn
            }
        }

        if (hyperCombustionEvents > 0) {
            logger.info("Oxygen Engine: Hyper-combustion fire propagation active across {} high-O2 cells.", hyperCombustionEvents);
        }
    }
}

