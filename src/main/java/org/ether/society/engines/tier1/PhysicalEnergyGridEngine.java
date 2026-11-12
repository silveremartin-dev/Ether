/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;

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
public class PhysicalEnergyGridEngine extends AbstractSimulationEngine {
    public PhysicalEnergyGridEngine() {
        super("PhysicalEnergyGridEngine", "PhysicalEnergyGridEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }


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

    /**
     * Calculates available per-capita useful mechanical &amp; thermal power output (\(\text{W/capita}\))
     * from DoD {@link WorldBuffer} arrays at cell index {@code i}.
     */
    public static double calculatePerCapitaMechanicalPowerWatts(WorldBuffer buffer, int i) {
        if (buffer == null || i < 0 || i >= buffer.getCapacity()) return 0.0;

        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 0.0) return 0.0;

        double capital = buffer.getResourceCapital()[i];
        double capitalPerCapita = Math.max(0.0, capital / pop);

        double maxHarnessableWatts = 350.0 + 100000.0 * (1.0 - Math.exp(-capitalPerCapita / 5.0));
        double somaticWatts = 135.0;

        double woodBiomassKg = Math.max(buffer.getBiomassNatural()[i], buffer.getWoodResource()[i]);
        double woodThermalWatts = 212.0 * Math.tanh((woodBiomassKg + 50.0) / 50.0);

        double animalTractionWatts = 25.0 * Math.tanh(SmoothMath.softplus(capitalPerCapita - 150.0, 0.05) / 250.0);
        double millAndCharcoalWatts = 220.0 * Math.tanh(SmoothMath.softplus(capitalPerCapita - 520.0, 0.05) / 420.0);

        double coalStock = buffer.getResourceCoal()[i];
        double oilStock = buffer.getResourceOil()[i];
        double gasStock = buffer.getResourceGas()[i];
        double fossilReserves = coalStock + oilStock + gasStock;
        double fossilAvailability = (fossilReserves > 0.0)
                ? Math.tanh(fossilReserves / SmoothMath.softmax(5.0, pop * 0.02, 0.5))
                : 0.0;
        double steamCoalWatts = 600.0 * Math.tanh(SmoothMath.softplus(capitalPerCapita - 1800.0, 0.02) / 2500.0) * fossilAvailability;

        double uraniumStock = buffer.getResourceUranium()[i];
        double fissileBonus = 1.0 + 0.25 * Math.tanh(Math.max(0.0, uraniumStock) / SmoothMath.softmax(1.0, pop * 0.001, 2.0));
        double modernGridWatts = 1350.0 * Math.tanh(SmoothMath.softplus(capitalPerCapita - 4000.0, 0.01) / 10000.0) * fissileBonus;

        double highFluxGate = SmoothMath.sigmoid(capitalPerCapita, 28000.0, 4000.0);
        double postScarcityWatts = highFluxGate * 25000.0 * SmoothMath.softmin(5.0, capitalPerCapita / 28000.0, 2.0);

        double mechanicalPowerWatts = somaticWatts + woodThermalWatts + animalTractionWatts
                + millAndCharcoalWatts + steamCoalWatts + modernGridWatts + postScarcityWatts;

        return SmoothMath.softmin(maxHarnessableWatts, SmoothMath.softmax(50.0, mechanicalPowerWatts, 0.2), 0.01);
    }

    @Override
    public void tick(WorldBuffer buf, double dtYears, double dtMonthly) {
        processPhysicalEnergyGrid(buf, dtYears);
    }

    /** Executes one monthly physical energy grid tick across DoD buffer. */
    public static void processPhysicalEnergyGrid(WorldBuffer buffer) {
        processPhysicalEnergyGrid(buffer, 1.0 / 12.0);
    }

    /**
     * Executes physical energy grid tick, smoothly consuming finite fossil and fissile fuels
     * according to Marchetti-Nakicenovic (1979) logistic carrier shares and accumulating physical capital.
     */
    public static void processPhysicalEnergyGrid(WorldBuffer buffer, double dtYears) {
        if (buffer == null || buffer.getCapacity() == 0) return;
        double dtSafe = Math.max(0.0001, dtYears);

        float[] bioHuman = buffer.getBiomassHuman();
        float[] capArr = buffer.getResourceCapital();
        float[] coalArr = buffer.getResourceCoal();
        float[] oilArr = buffer.getResourceOil();
        float[] gasArr = buffer.getResourceGas();
        float[] uraniumArr = buffer.getResourceUranium();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            double pop = bioHuman[i] / 0.06;
            if (pop <= 0.0) continue;

            double powerPerCapitaW = calculatePerCapitaMechanicalPowerWatts(buffer, i);
            double curCap = capArr[i];
            double capPerCapita = curCap / pop;

            double coalStock = coalArr[i];
            double oilStock = oilArr[i];
            double gasStock = gasArr[i];
            double uraniumStock = uraniumArr[i];

            double coalShare = Math.tanh(coalStock / 2.0)
                    * 0.80 * SmoothMath.sigmoid(powerPerCapitaW, 400.0, 100.0)
                    * (1.0 - SmoothMath.sigmoid(powerPerCapitaW, 6000.0, 333.3));
            double oilShare = Math.tanh(oilStock / 2.0) * 0.50 * SmoothMath.sigmoid(powerPerCapitaW, 600.0, 200.0);
            double gasShare = Math.tanh(gasStock / 2.0) * 0.40 * SmoothMath.sigmoid(powerPerCapitaW, 800.0, 200.0);
            double nuclearShare = Math.tanh(uraniumStock / 0.5) * 0.25 * SmoothMath.sigmoid(powerPerCapitaW, 1500.0, 250.0);

            if (coalShare > 1e-4) {
                double coalBurnTons = (pop * (powerPerCapitaW * coalShare) * 0.0013 * dtSafe) / 1000.0;
                coalArr[i] = (float) Math.max(0.0, coalStock - coalBurnTons);
            }
            if (oilShare > 1e-4) {
                double oilBurnTons = (pop * (powerPerCapitaW * oilShare) * 0.0008 * dtSafe) / 1000.0;
                oilArr[i] = (float) Math.max(0.0, oilStock - oilBurnTons);
            }
            if (gasShare > 1e-4) {
                double gasBurnTons = (pop * (powerPerCapitaW * gasShare) * 0.0006 * dtSafe) / 1000.0;
                gasArr[i] = (float) Math.max(0.0, gasStock - gasBurnTons);
            }
            if (nuclearShare > 1e-4) {
                double uBurnTons = (pop * (powerPerCapitaW * nuclearShare) * 1e-8 * dtSafe) / 1000.0;
                uraniumArr[i] = (float) Math.max(0.0, uraniumStock - uBurnTons);
            }

            double kStar = 5.0
                    + 580.0 * Math.tanh(SmoothMath.softplus(powerPerCapitaW - 348.0, 0.2) / 38.0)
                    + 62.0 * Math.pow(SmoothMath.softplus(powerPerCapitaW - 400.0, 0.1) / 100.0, 1.95);
            double deltaAnnual = 0.04;
            double dCapitalPerCapita = deltaAnnual * (kStar - capPerCapita) * dtSafe;
            double newCapPerCapita = Math.max(0.0, capPerCapita + dCapitalPerCapita);
            capArr[i] = (float) (pop * newCapPerCapita);
        }
    }
}
