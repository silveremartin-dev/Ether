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

        // Physical exosomatic power ceiling bounded smoothly by installed prime-mover capital stock (Garrett 2011; Lotka 1922)
        double maxHarnessableWatts = 350.0 + 100000.0 * (1.0 - Math.exp(-capitalPerCapita / 5.0));

        // 1. Somatic basal + mechanical labor (~135 W continuous somatic metabolism + work output, Smil 2017)
        double somaticWatts = 135.0;

        // 2. Smooth domestic hearth & wood/biomass thermal flux (~212 W/cap in foraging & agrarian regimes, Smil 2017)
        double woodBiomassKg = Math.max(cell.getBiomassNaturalOr(0.0), cell.getWoodResourceOr(0.0));
        double woodThermalWatts = 212.0 * Math.tanh((woodBiomassKg + 50.0) / 50.0);

        // 3. Smooth draft animal traction & plows (activates smoothly above kpc ~ 150 kg/cap, up to +25 W/cap)
        double animalTractionWatts = 25.0 * Math.tanh(SmoothMath.softplus(capitalPerCapita - 150.0, 0.05) / 250.0);

        // 4. Smooth Wind / Watermill kinetic & charcoal metallurgical harvesting (activates smoothly above kpc ~ 520 kg/cap, up to +220 W/cap)
        double millAndCharcoalWatts = 220.0 * Math.tanh(SmoothMath.softplus(capitalPerCapita - 520.0, 0.05) / 420.0);

        // 5. Smooth Industrial Fossil Power (Coal, Oil, Gas) - Gated strictly by steam/heat engine capital (kpc >= 1800 kg/cap)
        double coalStock = cell.getResourceCoalOr(0.0);
        double oilStock = cell.getResourceOilOr(0.0);
        double gasStock = cell.getResourceGasOr(0.0);
        double fossilReserves = coalStock + oilStock + gasStock;
        double fossilAvailability = (fossilReserves > 0.0)
                ? Math.tanh(fossilReserves / SmoothMath.softmax(5.0, pop * 0.02, 0.5))
                : 0.0;
        double steamCoalWatts = 600.0 * Math.tanh(SmoothMath.softplus(capitalPerCapita - 1800.0, 0.02) / 2500.0) * fossilAvailability;

        // 6. Smooth Modern Electric / Hydrocarbon / Fissile Grid - Gated by 20th/21st century capital (kpc >= 4000 kg/cap)
        double uraniumStock = cell.getResourceUraniumOr(0.0);
        double fissileBonus = 1.0 + 0.25 * Math.tanh(Math.max(0.0, uraniumStock) / SmoothMath.softmax(1.0, pop * 0.001, 2.0));
        double modernGridWatts = 1350.0 * Math.tanh(SmoothMath.softplus(capitalPerCapita - 4000.0, 0.01) / 10000.0) * fissileBonus;

        // 7. Smooth High-Capital Post-Scarcity Solar/Fusion Grid (activates smoothly above K/N ~ 28000, plus explicit habitatEnergyKw)
        double highFluxGate = SmoothMath.sigmoid(capitalPerCapita, 28000.0, 4000.0);
        double postScarcityWatts = highFluxGate * 25000.0 * SmoothMath.softmin(5.0, capitalPerCapita / 28000.0, 2.0);

        double mechanicalPowerWatts = somaticWatts + woodThermalWatts + animalTractionWatts
                + millAndCharcoalWatts + steamCoalWatts + modernGridWatts + postScarcityWatts;

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

            // Authentic Solow-Swan capital accumulation model grounded in useful mechanical power (Solow 1956, Garrett 2011, Krausmann et al. 2017)
            // Steady-state material capital stock per capita k* scales thermodynamically with available power above somatic hearth baseline (~348 W):
            double kStar = 5.0
                    + 580.0 * Math.tanh(SmoothMath.softplus(powerPerCapitaW - 348.0, 0.2) / 38.0)
                    + 62.0 * Math.pow(SmoothMath.softplus(powerPerCapitaW - 400.0, 0.1) / 100.0, 1.95);
            // Capital depreciation rate delta = 4.0% per year (entropy of physical structures/tools)
            double deltaAnnual = 0.04;
            // Convergence towards thermodynamic steady state k*: dk/dt = delta * (k* - k)
            double dCapitalPerCapita = deltaAnnual * (kStar - capPerCapita) * dtSafe;
            double newCapPerCapita = Math.max(0.0, capPerCapita + dCapitalPerCapita);
            cell.setResourceCapital(pop * newCapPerCapita);
        }
    }
}
