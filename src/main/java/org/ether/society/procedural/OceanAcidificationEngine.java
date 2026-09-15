/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.PhysicalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Ocean Acidification, Henry's Law CO₂ Solubility & Marine Carbon Pump Engine.
 * 
 * <h2>Fundamental Model Physics Equations</h2>
 * <ul>
 *   <li><b>Henry's Law for CO₂ Gas Dissolution</b>:
 *       $$[\text{CO}_{2,\text{aq}}] = K_H(T) \cdot p_{\text{CO}_2}$$
 *       $$K_H(T) = K_0 \cdot \exp\left( \frac{-\Delta H_{\text{sol}}}{R} \left( \frac{1}{T_K} - \frac{1}{T_0} \right) \right)$$
 *   </li>
 *   <li><b>Ocean pH Anomaly</b>:
 *       $$\Delta \text{pH} \approx -0.15 \cdot \log_{10}\left( \frac{[\text{CO}_2]}{280.0} \right)$$
 *   </li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 4.2.0
 */
public class OceanAcidificationEngine {
    private static final Logger logger = LoggerFactory.getLogger(OceanAcidificationEngine.class);

    /** Reference Henry's constant for CO2 in seawater at 298.15 K (mol/(L·atm)) */
    public static final double HENRY_K0_CO2 = 0.034;

    /** Enthalpy of dissolution / R for CO2 in Kelvin */
    public static final double ENTHALPY_DISSOLUTION_OVER_R = 2400.0;

    /**
     * Calculates Henry's solubility constant K_H(T) for CO2 in mol/(L·atm).
     * Cold waters absorb significantly more CO2 than warm tropical waters.
     */
    public static double calculateHenrySolubility(double tempCelsius) {
        double tempK = Math.max(271.15, tempCelsius + PhysicalConstants.KELVIN_ZERO_CELSIUS);
        double deltaInvT = (1.0 / tempK) - (1.0 / PhysicalConstants.OPTIMAL_BIOLOGICAL_TEMP_KELVIN);
        return HENRY_K0_CO2 * Math.exp(ENTHALPY_DISSOLUTION_OVER_R * deltaInvT);
    }

    /**
     * Calculates dissolved CO2 concentration in aqueous phase in mol/L.
     */
    public static double calculateDissolvedCO2(double tempCelsius, double co2Ppm) {
        double pCO2Atm = Math.max(50.0, co2Ppm) * 1e-6;
        return calculateHenrySolubility(tempCelsius) * pCO2Atm;
    }

    /**
     * Executes one ocean acidification and Henry solubility pump tick.
     *
     * @param cells   List of H3 cells
     * @param co2Ppm  Global atmospheric CO₂ concentration in ppm
     */
    public static void processOceanAcidification(List<H3Cell> cells, double co2Ppm) {
        if (cells == null || cells.isEmpty()) return;

        // Baseline CO₂ = 280 ppm. Acidification occurs when CO₂ > 350 ppm
        if (co2Ppm <= 350.0) return;

        double acidificationFactor = (co2Ppm - 350.0) / 650.0; // 0.0 to 1.0
        int impactedCells = 0;

        for (H3Cell cell : cells) {
            Biome biome = cell.getBiome();
            if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.BEACH) {
                impactedCells++;
                double tempC = cell.getTemperature() != null ? cell.getTemperature() : 15.0;
                double henrySolubility = calculateHenrySolubility(tempC);

                // Colder polar waters absorb more CO2 and acidify faster
                double polarAcidificationMultiplier = henrySolubility / HENRY_K0_CO2;

                double fish = cell.getBiomassFish() != null ? cell.getBiomassFish() : 500.0;
                // Acidification & coral reef degradation reduces fish nursery capacity
                double updatedFish = Math.max(0.0, fish * (1.0 - 0.05 * acidificationFactor * polarAcidificationMultiplier));
                cell.setBiomassFish(updatedFish);
            }
        }

        if (impactedCells > 0 && acidificationFactor > 0.05) {
            logger.info("Ocean Acidification Engine: Marine stock degradation across {} ocean/coastal cells (CO2: {} ppm).",
                    impactedCells, String.format("%.1f", co2Ppm));
        }
    }
}

