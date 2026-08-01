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
 * Physical Energy Grid & Mechanical Work Conversion Engine.
 * Converts raw physical energy sources into usable per-capita mechanical & industrial work (W_available in Watts/capita):
 * 1. <b>Biomass Wood Energy (18.5 MJ/kg) & Coal Energy (24.0 MJ/kg)</b>.
 * 2. <b>Kinetic Wind Power (Betz Law) & Solar Irradiance (1361 W/m²)</b>.
 * 3. <b>Mechanical Converter Efficiency (η_converter)</b>: Scales with technology era (Muscle 10% -> Steam 15% -> Internal Combustion 35% -> Electric 90%).
 *
 * @author Silvere Martin-Michiellot
 * @version 3.3.0
 */
public class PhysicalEnergyGridEngine {
    private static final Logger logger = LoggerFactory.getLogger(PhysicalEnergyGridEngine.class);

    /** Specific energy density of wood biomass in MJ/kg */
    public static final double WOOD_ENERGY_DENSITY_MJ_PER_KG = 18.5;

    /** Specific energy density of coal in MJ/kg */
    public static final double COAL_ENERGY_DENSITY_MJ_PER_KG = 24.0;

    /**
     * Calculates available per-capita mechanical work output (Watts/person).
     *
     * @param cell H3 terrain cell
     * @return Available mechanical power in Watts/capita
     */
    public static double calculatePerCapitaMechanicalPowerWatts(H3Cell cell) {
        if (cell == null) return 100.0;

        int pop = cell.getPopulation() != null ? cell.getPopulation() : 1;
        if (pop <= 0) pop = 1;

        double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
        double converterEfficiency = 0.10 + Math.min(0.80, tech * 0.08); // 10% muscle to 90% electric

        double woodBiomassKg = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 0.0;
        double solarEnergyW = cell.getEnergySolar() != null ? cell.getEnergySolar() : 0.0;
        double windEnergyW = cell.getEnergyWind() != null ? cell.getEnergyWind() : 0.0;

        // Total thermal & kinetic energy pool (Joules/sec = Watts)
        double totalThermalPowerWatts = (woodBiomassKg * WOOD_ENERGY_DENSITY_MJ_PER_KG * 1e6 / 86400.0) + solarEnergyW + windEnergyW;

        double usableMechanicalPowerWatts = totalThermalPowerWatts * converterEfficiency;

        return Math.max(100.0, usableMechanicalPowerWatts / pop);
    }

    /**
     * Executes one physical energy grid tick across cells.
     */
    public static void processPhysicalEnergyGrid(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            double powerPerCapitaW = calculatePerCapitaMechanicalPowerWatts(cell);
            // Expand cell resource capital based on per-capita mechanical power output
            cell.setResourceCapital(cell.getResourceCapital() + (powerPerCapitaW / 10.0));
        }
    }
}
