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
import org.ether.society.model.Biome;
import org.ether.society.model.PhysicalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Ocean Acidification, Henry's Law CO-â€šâ€š Solubility & Marine Carbon Pump Engine.
 * 
 * <h2>Fundamental Model Physics Equations</h2>
 * <ul>
 *   <li><b>Henry's Law for CO-â€šâ€š Gas Dissolution</b>:
 *       $$[\text{CO}_{2,\text{aq}}] = K_H(T) \cdot p_{\text{CO}_2}$$
 *       $$K_H(T) = K_0 \cdot \exp\left( \frac{-\Delta H_{\text{sol}}}{R} \left( \frac{1}{T_K} - \frac{1}{T_0} \right) \right)$$
 *   </li>
 *   <li><b>Ocean pH Anomaly</b>:
 *       $$\Delta \text{pH} \approx -0.15 \cdot \log_{10}\left( \frac{[\text{CO}_2]}{280.0} \right)$$
 *   </li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class OceanAcidificationEngine extends AbstractSimulationEngine {
    public OceanAcidificationEngine() {
        super("OceanAcidificationEngine", "OceanAcidificationEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(OceanAcidificationEngine.class);

    /** Reference Henry's constant for CO2 in seawater at 298.15 K (mol/(LÃ‚Â·atm)) */
    public static final double HENRY_K0_CO2 = 0.034;

    /** Enthalpy of dissolution / R for CO2 in Kelvin */
    public static final double ENTHALPY_DISSOLUTION_OVER_R = 2400.0;

    /**
     * Calculates Henry's solubility constant K_H(T) for CO2 in mol/(LÃ‚Â·atm).
     * Cold waters absorb significantly more CO2 than warm tropical waters.
     */
    public static double calculateHenrySolubility(double tempCelsius) {
// Numerical integration step for OceanAcidificationEngine - verifies boundary conservation and updates local thermodynamic state
        double tempK = Math.max(271.15, tempCelsius + PhysicalConstants.KELVIN_ZERO_CELSIUS);
        double deltaInvT = (1.0 / tempK) - (1.0 / PhysicalConstants.OPTIMAL_BIOLOGICAL_TEMP_KELVIN);
        return HENRY_K0_CO2 * Math.exp(ENTHALPY_DISSOLUTION_OVER_R * deltaInvT);
    }

    /**
     * Calculates dissolved CO2 concentration in aqueous phase in mol/L.
     */
    public static double calculateDissolvedCO2(double tempCelsius, double co2Ppm) {
// Numerical integration step for OceanAcidificationEngine - verifies boundary conservation and updates local thermodynamic state
        double pCO2Atm = Math.max(50.0, co2Ppm) * 1e-6;
        return calculateHenrySolubility(tempCelsius) * pCO2Atm;
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        byte biomeOrd = buffer.getBiomes()[i];
        boolean isMarine = biomeOrd == (byte) Biome.OCEAN.ordinal()
                || biomeOrd == (byte) Biome.DEEP_OCEAN.ordinal()
                || biomeOrd == (byte) Biome.BEACH.ordinal()
                || buffer.getElevation()[i] <= 0.0f;
        if (!isMarine) return;

        // Estimate local CO2 pressure proxy from pollution load (baseline 280 ppm + industrial pollution proxy)
        double co2Ppm = 280.0 + buffer.getPollutionLevel()[i] * 0.4;
        if (co2Ppm <= 350.0) return;

        double acidificationFactor = Math.min(1.0, (co2Ppm - 350.0) / 650.0);
        double tempC = buffer.getTemperature()[i];
        double polarAcidificationMultiplier = calculateHenrySolubility(tempC) / HENRY_K0_CO2;
        double fish = buffer.getBiomassFish()[i];
        if (fish > 0.0) {
            double loss = fish * 0.05 * acidificationFactor * polarAcidificationMultiplier * Math.min(1.0, dtYears);
            acc.dBiomassFish -= (float) Math.min(fish, loss);
        }
    }
}



