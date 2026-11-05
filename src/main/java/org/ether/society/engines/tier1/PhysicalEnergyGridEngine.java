/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;

import java.util.List;

/**
 * Physical Energy Grid &amp; Exergy-to-Work Conversion Engine.
 * <p>
 * Converts conserved primary energy stocks and fluxes (somatic metabolism, wood biomass, draft livestock,
 * wind/hydro kinetic flux, coal, oil, gas, fissile uranium) into usable per-capita mechanical and
 * electrical power ($P_{\text{watts}}$ in $\text{W/capita}$) using strictly smooth ($C^\infty$) converter
 * activation curves driven by physical capital density $K/N$ (Smil 2017; Ayres &amp; Warr 2009;
 * Marchetti &amp; Nakicenovic 1979) ({@code AGENTS.md} §10.1, §10.2).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PhysicalEnergyGridEngine {

    /** Specific energy density of wood biomass in MJ/kg. */
    public static final double WOOD_ENERGY_DENSITY_MJ_PER_KG = 18.5;

    /** Specific energy density of bituminous/anthracite coal in MJ/kg. */
    public static final double COAL_ENERGY_DENSITY_MJ_PER_KG = 24.0;

    /**
     * Calculates available per-capita useful mechanical &amp; thermal power output ($\text{W/capita}$)
     * as a strictly $C^\infty$ smooth function of cellular fuel reserves and physical capital per capita ($K/N$).
     *
     * @param cell H3 terrain cell
     * @return available useful power in Watts/capita
     */
    public static double calculatePerCapitaMechanicalPowerWatts(H3Cell cell) {
        if (cell == null) return 0.0;

        int pop = cell.getPopulationOr(0);
        if (pop <= 0) return 0.0;

        double capital = cell.getResourceCapitalOr(0.0);
        double capitalPerCapita = Math.max(0.0, capital / pop);

        // Smooth prime-mover Carnot/electromechanical converter efficiency (15% somatic -> 85% electric grid, Smil 2017)
        double converterEfficiency = 0.15 + 0.70 * (1.0 - Math.exp(-capitalPerCapita / 400.0));
        // Physical exosomatic power ceiling bounded smoothly by installed prime-mover capital stock (Garrett 2011; Lotka 1922)
        double maxHarnessableWatts = 250.0 + 100000.0 * (1.0 - Math.exp(-capitalPerCapita / 5.0));

        // 1. Direct somatic mechanical labor (~50 W continuous work output)
        double humanLaborWatts = 50.0;

        // 2. Smooth wood/biomass thermal work (hearths, kilns, bloomeries)
        double woodBiomassKg = Math.max(cell.getBiomassNaturalOr(0.0), cell.getWoodResourceOr(0.0));
        double woodGate = SmoothMath.sigmoid(woodBiomassKg, 10.0, 4.0);
        double firewoodThermalWatts = 150.0 + woodGate * (300.0 + SmoothMath.softmin(350.0, capitalPerCapita * 15.0, 0.05));
        double thermalWorkWatts = firewoodThermalWatts * converterEfficiency;

        // 3. Smooth draft animal traction (oxen, horses, camelids)
        double livestock = cell.getBiomassLivestockOr(0.0);
        double livestockGate = SmoothMath.sigmoid(livestock, 10.0, 4.0);
        double draftRatio = SmoothMath.softmin(2.5,
                (livestock / SmoothMath.softmax(10.0, pop * 0.1, 0.5)) + SmoothMath.softmin(1.0, capitalPerCapita * 0.05, 5.0),
                2.0);
        double animalTractionWatts = 120.0 * livestockGate * draftRatio;

        double mechanicalPowerWatts = humanLaborWatts + thermalWorkWatts + animalTractionWatts;

        // 4. Smooth Wind / Watermill kinetic harvesting (activates smoothly around K/N ~ 20)
        double millGate = SmoothMath.sigmoid(capitalPerCapita, 20.0, 6.0);
        double windEnergyW = cell.getEnergyWindOr(0.0);
        double millFactor = SmoothMath.softmin(3.0, capitalPerCapita / 50.0, 3.0);
        double kineticHarness = windEnergyW * 0.05 + 400.0 * millFactor;
        double maxPerCapitaWindHydro = 250.0 + 2250.0 * (1.0 - Math.exp(-capitalPerCapita / 200.0));
        mechanicalPowerWatts += millGate * (120.0 * millFactor + SmoothMath.softmin(maxPerCapitaWindHydro, kineticHarness / pop, 0.02));

        // 5. Smooth Industrial Fossil Power (Coal, Oil, Gas) - Gated by authentic steam/heat engine capital (kpc >= 700)
        double coalStock = cell.getResourceCoalOr(0.0);
        double oilStock = cell.getResourceOilOr(0.0);
        double gasStock = cell.getResourceGasOr(0.0);
        double industrialFactor = SmoothMath.smoothClamp(1.0 + (capitalPerCapita - 600.0) * 0.005, 0.5, 3.5, 3.0);
        double coalAvail = Math.tanh(Math.max(0.0, coalStock) / SmoothMath.softmax(10.0, pop * 0.05, 0.5));
        double hydroAvail = Math.tanh(Math.max(0.0, oilStock + gasStock) / SmoothMath.softmax(5.0, pop * 0.02, 0.5));
        double fossilPrimeMoverGate = SmoothMath.sigmoid(capitalPerCapita, 800.0, 150.0);
        mechanicalPowerWatts += fossilPrimeMoverGate * (3200.0 * coalAvail + 4200.0 * hydroAvail) * industrialFactor * converterEfficiency;

        // 6. Smooth Fissile Nuclear Power Grid - Gated by 20th century high-capital facilities (kpc >= 4000)
        double uraniumStock = cell.getResourceUraniumOr(0.0);
        double reactorCapitalGate = SmoothMath.sigmoid(capitalPerCapita, 4500.0, 800.0);
        double modernFactor = SmoothMath.smoothClamp(1.0 + capitalPerCapita * 0.0002, 0.5, 3.0, 3.0);
        double fissileAvailability = Math.tanh(Math.max(0.0, uraniumStock) / SmoothMath.softmax(1.0, pop * 0.001, 2.0));
        mechanicalPowerWatts += reactorCapitalGate * 25000.0 * modernFactor * fissileAvailability * converterEfficiency;

        // 7. Smooth High-Capital Solar/Renewables/Fusion Grid (activates smoothly above K/N ~ 12000, plus explicit habitatEnergyKw)
        double highFluxGate = SmoothMath.sigmoid(capitalPerCapita, 12000.0, 2500.0);
        double postScarcityFactor = SmoothMath.softmin(5.0, capitalPerCapita / 12000.0, 2.0);
        mechanicalPowerWatts += highFluxGate * 60000.0 * postScarcityFactor * converterEfficiency;

        double habitatKw = cell.getHabitatEnergyKwOr(0.0);
        if (habitatKw > 0.0) {
            mechanicalPowerWatts += (habitatKw * 1000.0) / pop;
        }

        return SmoothMath.softmin(maxHarnessableWatts, SmoothMath.softmax(50.0, mechanicalPowerWatts, 0.2), 0.01);
    }

    /** Executes one monthly physical energy grid tick across cells. */
    public static void processPhysicalEnergyGrid(List<H3Cell> cells) {
        processPhysicalEnergyGrid(cells, 1.0 / 12.0);
    }

    /**
     * Executes physical energy grid tick, smoothly consuming finite fossil and fissile fuels
     * according to Marchetti-Nakicenovic (1979) logistic carrier shares and accumulating physical capital.
     */
    public static void processPhysicalEnergyGrid(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty()) return;
        double dtSafe = Math.max(0.0001, dtYears);

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double powerPerCapitaW = calculatePerCapitaMechanicalPowerWatts(cell);
            double curCap = cell.getResourceCapitalOr(0.0);
            double capPerCapita = curCap / pop;

            cell.setResourceWork(pop * 0.60);

            double coalStock = cell.getResourceCoalOr(0.0);
            double oilStock = cell.getResourceOilOr(0.0);
            double gasStock = cell.getResourceGasOr(0.0);
            double uraniumStock = cell.getResourceUraniumOr(0.0);
            double reeStock = cell.getResourceRareEarthsOr(0.0);

            double coalShare = Math.tanh(coalStock / 2.0)
                    * 0.80 * SmoothMath.sigmoid(powerPerCapitaW, 400.0, 100.0)
                    * (1.0 - SmoothMath.sigmoid(powerPerCapitaW, 6000.0, 333.3));
            double oilShare = Math.tanh(oilStock / 2.0) * 0.50 * SmoothMath.sigmoid(powerPerCapitaW, 600.0, 200.0);
            double gasShare = Math.tanh(gasStock / 2.0) * 0.40 * SmoothMath.sigmoid(powerPerCapitaW, 800.0, 200.0);
            double nuclearShare = Math.tanh(uraniumStock / 0.5) * 0.25 * SmoothMath.sigmoid(powerPerCapitaW, 1500.0, 250.0);
            double reeShare = Math.tanh(reeStock / 0.5) * 0.20 * SmoothMath.sigmoid(powerPerCapitaW, 1200.0, 333.3);

            if (coalShare > 1e-4) {
                double coalBurnTons = (pop * (powerPerCapitaW * coalShare) * 0.0013 * dtSafe) / 1000.0;
                cell.setResourceCoal(Math.max(0.0, coalStock - coalBurnTons));
            }
            if (oilShare > 1e-4) {
                double oilBurnTons = (pop * (powerPerCapitaW * oilShare) * 0.0008 * dtSafe) / 1000.0;
                cell.setResourceOil(Math.max(0.0, oilStock - oilBurnTons));
            }
            if (gasShare > 1e-4) {
                double gasBurnTons = (pop * (powerPerCapitaW * gasShare) * 0.0006 * dtSafe) / 1000.0;
                cell.setResourceGas(Math.max(0.0, gasStock - gasBurnTons));
            }
            if (nuclearShare > 1e-4) {
                double uBurnTons = (pop * (powerPerCapitaW * nuclearShare) * 1e-8 * dtSafe) / 1000.0;
                cell.setResourceUranium(Math.max(0.0, uraniumStock - uBurnTons));
            }
            if (reeShare > 1e-4) {
                double reeBurnTons = (pop * (powerPerCapitaW * reeShare) * 2e-7 * dtSafe) / 1000.0;
                cell.setResourceRareEarths(Math.max(0.0, reeStock - reeBurnTons));
            }

            // Authentic Solow-Swan capital accumulation model grounded in useful mechanical power (Solow 1956, Garrett 2011)
            // Steady-state capital stock per capita k* scales thermodynamically with available mechanical power:
            // k* = 15.0 (stone/wood baseline) + 12.0 * (powerWatts / 100.0)^1.30
            double kStar = 15.0 + 12.0 * Math.pow(Math.max(0.1, powerPerCapitaW / 100.0), 1.30);
            // Capital depreciation rate delta = 4.0% per year (entropy of physical structures/tools)
            double deltaAnnual = 0.04;
            // Convergence towards thermodynamic steady state k*: dk/dt = delta * (k* - k)
            double dCapitalPerCapita = deltaAnnual * (kStar - capPerCapita) * dtSafe;
            double newCapPerCapita = Math.max(0.0, capPerCapita + dCapitalPerCapita);
            cell.setResourceCapital(pop * newCapPerCapita);
        }
    }
}
