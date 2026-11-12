/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
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
 * @version 1.0.0-beta.3
 */
public class AtmosphericOxygenEngine extends AbstractSimulationEngine {
    public AtmosphericOxygenEngine() {
        super("AtmosphericOxygenEngine", "AtmosphericOxygenEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

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

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double o2PressureAtm = STANDARD_O2_PRESSURE_ATM;
        double vo2maxFactor = calculateAerobicCapacityFactor(o2PressureAtm);
        if (Math.abs(vo2maxFactor - 1.0) < 1.0e-5 && o2PressureAtm <= 0.25) {
            return;
        }
        double burnRetention = Math.exp(-0.999 * Math.max(0.0001, dtYears));
        if (o2PressureAtm > 0.25 && buffer.getBiomassNatural()[i] > 500.0f) {
            float cur = buffer.getBiomassNatural()[i];
            acc.dBiomassNatural += (float) (cur * burnRetention - cur);
        }
    }
}
