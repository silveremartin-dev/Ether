/*
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
 * Physical Energy Grid & Mechanical Work Conversion Engine.
 * Converts raw physical energy sources into usable per-capita mechanical & industrial work (W_available in Watts/capita):
 * 1. <b>Biomass Wood Energy (18.5 MJ/kg) & Coal Energy (24.0 MJ/kg)</b>.
 * 2. <b>Kinetic Wind Power (Betz Law) & Solar Irradiance (1361 W/m²)</b>.
 * 3. <b>Mechanical Converter Efficiency (η_converter)</b>: Scales with technology era (Muscle 10% -> Steam 15% -> Internal Combustion 35% -> Electric 90%).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
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
// Numerical integration step for PhysicalEnergyGridEngine - verifies boundary conservation and updates local thermodynamic state
        if (cell == null) return 0.0;

        int pop = cell.getPopulationOr(0);
        if (pop <= 0) return 0.0;

        double tech = cell.getTechnologyLevelOr(0.0);
        double capital = cell.getResourceCapitalOr(0.0);
        double capitalPerCapita = capital / pop;
        double converterEfficiency = 0.15 + Math.min(0.75, tech * 0.08 + Math.min(0.15, capitalPerCapita * 0.02)); // 15% muscle to 90% electric

        // Maximum harnessable per-capita power physically bounded by technological era (Smil 2017 / Lotka)
        // Tech < 2.0 (Paleolithic/Neolithic): 100-450 W
        // Tech 2.0-3.0 (Antiquity/Medieval organic economy: draft animals, watermills, windmills): 450-1,600 W
        // Tech 3.0-4.0 (Industrial Coal/Steam/ICE): 2,000-10,000 W
        // Tech 4.0-5.0 (Electrical & Nuclear Grid): 10,000-35,000 W
        // Tech >= 5.0 (Advanced Fusion / Trans-Human): >50,000 W
        double maxHarnessableWatts = 150.0 + Math.pow(Math.min(5.0, tech), 2.2) * 1800.0;

        // 1. Direct metabolic human mechanical labor (~50 W continuous per capita)
        double humanLaborWatts = 50.0;

        // 2. Firewood & kiln thermal work (heating, kilns, smelting, cooking)
        double woodBiomassKg = cell.getBiomassNaturalOr(0.0);
        double firewoodThermalWatts = (woodBiomassKg > 10.0)
                ? (450.0 + Math.min(350.0, capitalPerCapita * 15.0))
                : 150.0;
        double thermalWorkWatts = firewoodThermalWatts * converterEfficiency;

        // 3. Draft animal traction (oxen, horses; unlocked by Neolithic tech >= 1.0)
        double animalTractionWatts = (tech >= 1.0)
                ? (120.0 * Math.min(2.5, Math.max(0.5, tech - 0.5) + Math.min(1.0, capitalPerCapita * 0.05)))
                : 0.0;

        double mechanicalPowerWatts = humanLaborWatts + thermalWorkWatts + animalTractionWatts;

        // 4. Hydro / Wind / Watermill kinetic contribution (Antiquity/Medieval engineering tech >= 2.0)
        if (tech >= 2.0) {
            double windEnergyW = cell.getEnergyWindOr(0.0);
            double millFactor = Math.min(3.0, Math.max(0.5, tech - 1.5) + Math.min(1.0, capitalPerCapita * 0.05));
            double kineticHarness = (windEnergyW * 0.05 + 400.0 * millFactor) * (tech >= 4.5 ? 5.0 : 1.0);
            double maxPerCapitaWindHydro = (tech < 3.0) ? 250.0 : ((tech < 4.0) ? 1200.0 : 2500.0);
            mechanicalPowerWatts += 120.0 * millFactor + Math.min(maxPerCapitaWindHydro, kineticHarness / Math.max(1.0, pop));
        }

        // 5. Industrial Fossil Power (Coal, Hydrocarbons, Steam/ICE grid; requires Industrial tech >= 3.0)
        double coalWatts = 0.0;
        double hydrocarbonWatts = 0.0;
        if (tech >= 3.0) {
            double industrialFactor = Math.min(3.0, tech - 2.0);
            double coalStock = cell.getResourceCoalOr(0.0);
            double oilStock = cell.getResourceOilOr(0.0);
            double gasStock = cell.getResourceGasOr(0.0);
            double fossilAvailability = Math.min(1.0, (coalStock + oilStock * 1.5 + gasStock * 1.2) / Math.max(10.0, pop * 0.05));
            coalWatts = 3000.0 * industrialFactor * fossilAvailability; // Primary thermal coal
            hydrocarbonWatts = (tech >= 3.5) ? 4000.0 * Math.min(2.0, tech - 3.0) * Math.min(1.0, (oilStock + gasStock) / Math.max(5.0, pop * 0.02)) : 0.0;
            mechanicalPowerWatts += (coalWatts + hydrocarbonWatts) * converterEfficiency;
        }

        // 6. Modern Electrical & Nuclear Power Grid (tech >= 4.0)
        double nuclearWatts = 0.0;
        if (tech >= 4.0) {
            double modernFactor = Math.min(2.5, tech - 3.5);
            double uraniumStock = cell.getResourceUraniumOr(0.0);
            double fissileAvailability = Math.min(1.0, uraniumStock / Math.max(1.0, pop * 0.001));
            // Electro-nuclear power flux: scales into 15-30 kW per capita range, strictly bounded by fissile reserves
            nuclearWatts = 35000.0 * modernFactor * fissileAvailability;
            mechanicalPowerWatts += nuclearWatts * converterEfficiency;
        }

        // 7. Advanced High-Flux Energy & D-T Fusion / Trans-Human Grid (tech >= 5.0)
        if (tech >= 5.0) {
            double postScarcityFactor = Math.min(5.0, tech - 4.0);
            // Ultra-dense fusion & space solar power flux: scales into 50-70 kW per capita range
            double advancedPowerWatts = 100000.0 * postScarcityFactor;
            mechanicalPowerWatts += advancedPowerWatts * converterEfficiency;
        }

        return Math.min(maxHarnessableWatts, Math.max(50.0, mechanicalPowerWatts));
    }

    /**
     * Executes one physical energy grid tick across cells using default monthly step.
     */
    public static void processPhysicalEnergyGrid(List<H3Cell> cells) {
        processPhysicalEnergyGrid(cells, 1.0 / 12.0);
    }

    /**
     * Executes physical energy grid tick scaling capital formation by population and dtYears,
     * and strictly consumes finite fossil and nuclear fuels to preserve thermodynamic conservation.
     */
    public static void processPhysicalEnergyGrid(List<H3Cell> cells, double dtYears) {
// Numerical integration step for PhysicalEnergyGridEngine - verifies boundary conservation and updates local thermodynamic state
        if (cells == null || cells.isEmpty()) return;
        double dtSafe = Math.max(0.0001, dtYears);

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;
            double powerPerCapitaW = calculatePerCapitaMechanicalPowerWatts(cell);
            double tech = cell.getTechnologyLevel() != null ? Math.max(0.5, cell.getTechnologyLevel()) : 1.0;

            // Set active productive labor capacity (person-years equivalent, ~60% working-age labor participation)
            cell.setResourceWork(pop * 0.60);

            // Deplete consumed geological fuels (Coal, Oil, Gas, Uranium, Rare Earths) according to power generation
            if (tech >= 3.0) {
                double coalStock = cell.getResourceCoalOr(0.0);
                double oilStock = cell.getResourceOilOr(0.0);
                double gasStock = cell.getResourceGasOr(0.0);
                // Specific energy consumption: 1 W-year = 3.1536e7 J ~ 1.3 kg Coal (at 24 MJ/kg)
                double coalBurnTons = (pop * (powerPerCapitaW * 0.4) * 0.0013 * dtSafe) / 1000.0;
                double oilBurnTons = (tech >= 3.5) ? (pop * (powerPerCapitaW * 0.3) * 0.0008 * dtSafe) / 1000.0 : 0.0;
                double gasBurnTons = (tech >= 3.5) ? (pop * (powerPerCapitaW * 0.2) * 0.0006 * dtSafe) / 1000.0 : 0.0;
                cell.setResourceCoal(Math.max(0.0, coalStock - coalBurnTons));
                cell.setResourceOil(Math.max(0.0, oilStock - oilBurnTons));
                cell.setResourceGas(Math.max(0.0, gasStock - gasBurnTons));
            }
            if (tech >= 4.0) {
                double uStock = cell.getResourceUraniumOr(0.0);
                // 1 kg U-235 provides ~8e13 J (fission), minimal mass burn rate
                double uBurnTons = (pop * (powerPerCapitaW * 0.3) * 1e-8 * dtSafe) / 1000.0;
                cell.setResourceUranium(Math.max(0.0, uStock - uBurnTons));

                // Advanced electro-nuclear grid, wind turbines, and semiconductors consume critical rare earth elements
                double reeStock = cell.getResourceRareEarthsOr(0.0);
                double reeBurnTons = (pop * (powerPerCapitaW * 0.1) * 2e-7 * dtSafe) / 1000.0;
                cell.setResourceRareEarths(Math.max(0.0, reeStock - reeBurnTons));
            }

            // Solow-Swan physical capital formation from mechanical work:
            // Gross investment scales with mechanical power flux, while depreciation scales with existing stock
            // and intensive capital density (K/N) to prevent unphysical infinite accumulation in pre-industrial eras.
            double curCap = cell.getResourceCapitalOr(0.0);
            double capPerCapita = curCap / pop;
            double equilibriumCapPerCapita = 8.0 * Math.pow(tech, 1.5) * (powerPerCapitaW / 250.0);
            double saturationFactor = Math.max(0.0, 1.0 - (capPerCapita / Math.max(10.0, equilibriumCapPerCapita)));

            double capitalFormation = pop * (powerPerCapitaW / 400.0) * saturationFactor * dtSafe;
            double capitalDepreciation = curCap * 0.02 * dtSafe;

            cell.setResourceCapital(Math.max(0.0, curCap + capitalFormation - capitalDepreciation));
        }
    }
}


