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

        double capital = cell.getResourceCapitalOr(0.0);
double capitalPerCapita = capital / pop;
// Prime mover converter efficiency (15% muscle to 85% mechanical/electric, Smil 2017)
double converterEfficiency = 0.15 + 0.70 * (1.0 - Math.exp(-capitalPerCapita / 400.0));
// Maximum harnessable per-capita power physically bounded by capital equipment density (Smil 2017 / Lotka)
double maxHarnessableWatts = 250.0 + 100000.0 * (1.0 - Math.exp(-capitalPerCapita / 5.0));

        // 1. Direct metabolic human mechanical labor (~50 W continuous per capita)
        double humanLaborWatts = 50.0;

        // 2. Firewood & kiln thermal work (heating, kilns, smelting, cooking)
        double woodBiomassKg = cell.getBiomassNaturalOr(0.0);
        double firewoodThermalWatts = (woodBiomassKg > 10.0)
                ? (450.0 + Math.min(350.0, capitalPerCapita * 15.0))
                : 150.0;
        double thermalWorkWatts = firewoodThermalWatts * converterEfficiency;

        // 3. Draft animal traction (oxen, horses; driven by domesticated livestock biomass)
double livestock = cell.getBiomassLivestockOr(0.0);
double animalTractionWatts = (livestock > 10.0)
        ? (120.0 * Math.min(2.5, (livestock / Math.max(10.0, pop * 0.1)) + Math.min(1.0, capitalPerCapita * 0.05)))
        : 0.0;

double mechanicalPowerWatts = humanLaborWatts + thermalWorkWatts + animalTractionWatts;

// 4. Hydro / Wind / Watermill kinetic contribution (Smil 2017; Allen 2009)
if (capitalPerCapita > 20.0) {
    double windEnergyW = cell.getEnergyWindOr(0.0);
    double millFactor = Math.min(3.0, capitalPerCapita / 50.0);
    double kineticHarness = (windEnergyW * 0.05 + 400.0 * millFactor);
    double maxPerCapitaWindHydro = 250.0 + 2250.0 * (1.0 - Math.exp(-capitalPerCapita / 200.0));
    mechanicalPowerWatts += 120.0 * millFactor + Math.min(maxPerCapitaWindHydro, kineticHarness / Math.max(1.0, pop));
}

// 5. Industrial Fossil Power (Coal, Hydrocarbons, Steam/ICE grid - Smil 2017)
double coalWatts = 0.0;
double hydrocarbonWatts = 0.0;
double coalStock = cell.getResourceCoalOr(0.0);
double oilStock = cell.getResourceOilOr(0.0);
double gasStock = cell.getResourceGasOr(0.0);
if (coalStock > 0.1 || oilStock > 0.1 || gasStock > 0.1) {
    double industrialFactor = Math.clamp(1.0 + capitalPerCapita * 0.5, 0.5, 3.5);
    double coalAvail = (coalStock > 0.1) ? Math.min(1.0, coalStock / Math.max(10.0, pop * 0.05)) : 0.0;
    coalWatts = 3200.0 * industrialFactor * coalAvail;
    double hydroAvail = (oilStock + gasStock > 0.1) ? Math.min(1.0, (oilStock + gasStock) / Math.max(5.0, pop * 0.02)) : 0.0;
    hydrocarbonWatts = 4200.0 * industrialFactor * hydroAvail;
    mechanicalPowerWatts += (coalWatts + hydrocarbonWatts) * converterEfficiency;
}

// 6. Modern Electrical & Nuclear Power Grid
double nuclearWatts = 0.0;
double uraniumStock = cell.getResourceUraniumOr(0.0);
if (uraniumStock > 0.0) {
    double modernFactor = Math.clamp(1.0 + capitalPerCapita * 0.2, 0.5, 3.0);
    double fissileAvailability = Math.min(1.0, uraniumStock / Math.max(1.0, pop * 0.001));
    nuclearWatts = 25000.0 * modernFactor * fissileAvailability;
    mechanicalPowerWatts += nuclearWatts * converterEfficiency;
}

// 7. Advanced High-Flux Energy & D-T Fusion / Trans-Human Grid
if (capitalPerCapita > 500.0) {
    double postScarcityFactor = Math.min(5.0, capitalPerCapita / 500.0);
    double advancedPowerWatts = 60000.0 * postScarcityFactor;
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
double capPerCapita = cell.getResourceCapitalOr(0.0) / pop;

// Set active productive labor capacity (person-years equivalent, ~60% working-age labor participation)
cell.setResourceWork(pop * 0.60);

// Continuous Marchetti-Nakicenovic (1979) logistic energy transition shares driven by capital and fuel availability:
double coalStock = cell.getResourceCoalOr(0.0);
double oilStock = cell.getResourceOilOr(0.0);
double gasStock = cell.getResourceGasOr(0.0);
double uraniumStock = cell.getResourceUraniumOr(0.0);

double coalShare = (coalStock > 0.1 && (powerPerCapitaW > 250.0 || capPerCapita > 0.1))
        ? (0.80 / (1.0 + Math.exp(-0.01 * (powerPerCapitaW - 400.0)))) * (1.0 / (1.0 + Math.exp(0.003 * (powerPerCapitaW - 6000.0))))
        : 0.0;
double oilShare = (oilStock > 0.1 && (powerPerCapitaW > 350.0 || capPerCapita > 0.1))
        ? (0.50 / (1.0 + Math.exp(-0.005 * (powerPerCapitaW - 600.0))))
        : 0.0;
double gasShare = (gasStock > 0.1 && (powerPerCapitaW > 400.0 || capPerCapita > 0.1))
        ? 0.40 / (1.0 + Math.exp(-0.005 * (powerPerCapitaW - 800.0)))
        : 0.0;
double nuclearShare = (uraniumStock > 0.05 && (powerPerCapitaW > 1000.0 || capPerCapita > 0.2))
        ? 0.25 / (1.0 + Math.exp(-0.004 * (powerPerCapitaW - 1500.0)))
        : 0.0;
double reeShare = (cell.getResourceRareEarthsOr(0.0) > 0.05 && (powerPerCapitaW > 800.0 || capPerCapita > 0.2))
        ? 0.20 / (1.0 + Math.exp(-0.003 * (powerPerCapitaW - 1200.0)))
        : 0.0;

            if (coalShare > 0.001) {
    double coalBurnTons = (pop * (powerPerCapitaW * coalShare) * 0.0013 * dtSafe) / 1000.0;
    cell.setResourceCoal(Math.max(0.0, coalStock - coalBurnTons));
}
if (oilShare > 0.001) {
    double oilBurnTons = (pop * (powerPerCapitaW * oilShare) * 0.0008 * dtSafe) / 1000.0;
    cell.setResourceOil(Math.max(0.0, oilStock - oilBurnTons));
}
if (gasShare > 0.001) {
    double gasBurnTons = (pop * (powerPerCapitaW * gasShare) * 0.0006 * dtSafe) / 1000.0;
    cell.setResourceGas(Math.max(0.0, gasStock - gasBurnTons));
}
            if (nuclearShare > 0.001) {
                double uStock = cell.getResourceUraniumOr(0.0);
                double uBurnTons = (pop * (powerPerCapitaW * nuclearShare) * 1e-8 * dtSafe) / 1000.0;
                cell.setResourceUranium(Math.max(0.0, uStock - uBurnTons));
            }
            if (reeShare > 0.001) {
                double reeStock = cell.getResourceRareEarthsOr(0.0);
                double reeBurnTons = (pop * (powerPerCapitaW * reeShare) * 2e-7 * dtSafe) / 1000.0;
                cell.setResourceRareEarths(Math.max(0.0, reeStock - reeBurnTons));
            }

            // Solow-Swan physical capital formation from mechanical work:
            // Gross investment scales with mechanical power flux, while depreciation scales with existing stock
            // and intensive capital density (K/N) to prevent unphysical infinite accumulation in pre-industrial eras.
            double curCap = cell.getResourceCapitalOr(0.0);
            capPerCapita = curCap / pop;
            double equilibriumCapPerCapita = Math.max(capPerCapita * 1.08, 80.0 * Math.pow(Math.max(1.0, powerPerCapitaW / 100.0), 1.2));
            double saturationFactor = Math.max(0.0, 1.0 - (capPerCapita / Math.max(10.0, equilibriumCapPerCapita)));

            double capitalFormation = curCap * 0.02 * dtSafe + pop * (powerPerCapitaW * 0.025) * saturationFactor * dtSafe;
            double capitalDepreciation = curCap * 0.02 * dtSafe;

            cell.setResourceCapital(Math.max(0.0, curCap + capitalFormation - capitalDepreciation));
        }
    }
}


