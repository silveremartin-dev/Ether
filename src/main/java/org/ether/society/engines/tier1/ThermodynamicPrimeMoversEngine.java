/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.model.PhysicalConstants;
import org.ether.society.model.PhysicalUnitsHelper;

/**
 * <h1>Thermodynamic Prime Movers Engine</h1>
 * <p>
 * Models the physical conversion of primary environmental enthalpy and potential energy into
 * mechanical shaft work and domestic thermal flux strictly via First- and Second-Law thermodynamics:
 * </p>
 * <ul>
 *   <li><b>Carnot &amp; Second-Law Heat Engine Limit (Carnot 1824; Ayres &amp; Warr 2009)</b>:
 *       $$\eta_{\text{Carnot}} = 1 - \frac{T_C}{T_H}, \qquad \eta_{\text{th}} = \varepsilon_{\text{II}} \left(1 - \frac{T_C}{T_H}\right)$$
 *       where \(T_C\) is ambient Kelvin temperature and \(T_H\) is boiler/cylinder working fluid temperature.</li>
 *   <li><b>Hydraulic Gravitational Power (Weisbach 1845; Smil 2017)</b>:
 *       $$P_{\text{hydro}} = \eta_w \, \rho_w \, g \, Q \, H$$</li>
 *   <li><b>First-Law Fuel Enthalpy Conservation</b>:
 *       $$\Delta M_{\text{fuel}} = -\frac{\dot{W}_{\text{shaft}} \, \Delta t}{\eta_{\text{th}} \cdot \text{LHV}_{\text{fuel}}}$$
 *       with \(\text{LHV}_{\text{coal}} = 24.0\,\text{GJ/t}\) and \(\text{LHV}_{\text{oil}} = 42.0\,\text{GJ/t}\).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.5
 */
public class ThermodynamicPrimeMoversEngine extends AbstractSimulationEngine {

    /** Lower heating value of bituminous/sub-bituminous coal (\(\text{GJ/t}\), Smil 2017). */
    public static final double COAL_LHV_GJ_PER_TONNE = 24.0;

    /** Lower heating value of crude petroleum distillates (\(\text{GJ/t}\), Smil 2017). */
    public static final double OIL_LHV_GJ_PER_TONNE = 42.0;

    /** Liquid water density \(\rho_w = 1000.0\,\text{kg/m}^3\). */
    public static final double WATER_DENSITY_KG_M3 = 1000.0;

    /**
     * Constructs the thermodynamic prime movers engine.
     */
    public ThermodynamicPrimeMoversEngine() {
        super("ThermodynamicPrimeMoversEngine", "ThermodynamicPrimeMoversEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    @Override
    public String getName() {
        return "ThermodynamicPrimeMoversEngine";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Carnot, Weisbach & First-Law Prime Movers (Carnot 1824; Smil 2017)]
               • Carnot Thermal Limit:   η_th = ε_II · (1 - T_C / T_H)
               • Hydraulic Wheel Power:  P_hydro = η_w · ρ_w · g · Q · H
               • First-Law Fuel Debit:   ΔM_fuel = - (W_shaft · Δt) / (η_th · LHV_fuel)
               Parameters: LHV_coal = 24 GJ/t, LHV_oil = 42 GJ/t, ρ_w = 1000 kg/m^3, g = 9.80665 m/s^2
               Ref: S. Carnot (1824); R. U. Ayres & B. Warr (2009); V. Smil (2017) "Energy and Civilization"
               """;
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || dtYears <= 0.0) {
            return;
        }

        double pop = buffer.getHumanPopulation()[i];
        if (pop <= 0.0) {
            return;
        }

        double capital = buffer.getResourceCapital()[i];
        double capitalPerCapita = Math.max(0.0, capital / pop);
        double metalStock = Math.max(0.0, buffer.getMetalResource()[i]);
        double coalStock = Math.max(0.0, buffer.getResourceCoal()[i]);
        double oilStock = Math.max(0.0, buffer.getResourceOil()[i]);

        // 1. Domestic firewood/biomass hearth thermal power (Monod saturation, 0 W when wood stock == 0)
        double woodBiomassKg = PhysicalUnitsHelper.metricTonnesToKg(
                Math.max(0.0, buffer.getBiomassNatural()[i]) + Math.max(0.0, buffer.getWoodResource()[i]));
        double woodThermalPowerW = woodBiomassKg > 0.0
                ? 212.0 * (woodBiomassKg / (woodBiomassKg + 50.0 * pop))
                : 0.0;

        // 2. Domesticated draft animal muscular work (eta_muscle ~ 0.18, Smil 2017)
        double livestockTonnesPerCapita = Math.max(0.0, buffer.getBiomassLivestock()[i] / pop);
        double animalPowerW = 65.0 * (livestockTonnesPerCapita / (livestockTonnesPerCapita + 0.25))
                * SmoothMath.sigmoid(capitalPerCapita, 25.0, 12.0);

        // 3. Hydraulic waterwheel & turbine power P_hydro = eta_w * rho_w * g * Q * H (Weisbach 1845)
        double elevM = buffer.getElevation() != null ? Math.max(0.0, buffer.getElevation()[i]) : 0.0;
        double hydraulicHeadM = 1.5 + Math.min(25.0, elevM * 0.015);
        double aquiferMm = buffer.getFreshwaterAquifer() != null ? Math.max(0.0, buffer.getFreshwaterAquifer()[i]) : 0.0;
        double specificDischargeM3PerS = aquiferMm * 1.0e-4;
        double wheelEfficiency = 0.25 + 0.50 * SmoothMath.sigmoid(capitalPerCapita, 400.0, 150.0);
        double rawHydraulicCellWatts = wheelEfficiency * WATER_DENSITY_KG_M3 * PhysicalConstants.STANDARD_GRAVITY_M_PER_S2
                * specificDischargeM3PerS * hydraulicHeadM;
        double millCapitalActivation = Math.tanh(SmoothMath.softplus(capitalPerCapita - 120.0, 0.04) / 300.0);
        double millPowerW = Math.min(160.0 * millCapitalActivation, (rawHydraulicCellWatts * millCapitalActivation) / pop);

        // 4. Carnot & Second-Law industrial heat engines (Carnot 1824; Ayres & Warr 2009)
        double tempC = buffer.getTemperature() != null ? buffer.getTemperature()[i] : 15.0;
        double tColdK = Math.clamp(PhysicalUnitsHelper.celsiusToKelvin(tempC), 220.0, 330.0);
        double industrialCapitalDepth = SmoothMath.softplus(capitalPerCapita - 950.0, 0.02);
        // Working fluid temperature T_H rises from saturated atmospheric steam (~390 K) to high-pressure turbine/ICE (~1840 K)
        double tHotK = Math.max(tColdK + 60.0, 390.0 + 1450.0 * (industrialCapitalDepth / (industrialCapitalDepth + 2800.0)));
        double etaCarnot = 1.0 - (tColdK / tHotK);
        // Second-Law exergetic effectiveness epsilon_II rises from early Savery/Newcomen (~0.14) to combined-cycle (~0.52)
        double secondLawEffectiveness = 0.14 + 0.38 * (industrialCapitalDepth / (industrialCapitalDepth + 3200.0));
        double thermalEfficiency = Math.clamp(secondLawEffectiveness * etaCarnot, 0.02, 0.48);

        double metallurgyGate = metalStock / (metalStock + 2.0);
        double coalSteamActivation = metallurgyGate * Math.tanh(SmoothMath.softplus(capitalPerCapita - 950.0, 0.025) / 1800.0);
        double oilIceActivation = metallurgyGate * Math.tanh(SmoothMath.softplus(capitalPerCapita - 2800.0, 0.015) / 3500.0);

        double coalAvail = coalStock / (coalStock + 10.0);
        double oilAvail = oilStock / (oilStock + 5.0);

        double desiredCoalShaftW = 220.0 * coalSteamActivation * coalAvail;
        double desiredOilShaftW = 520.0 * oilIceActivation * oilAvail;

        double desiredCoalThermalGJ = PhysicalUnitsHelper.wattsToGjPerYear(pop * (desiredCoalShaftW / thermalEfficiency)) * dtYears;
        double desiredOilThermalGJ = PhysicalUnitsHelper.wattsToGjPerYear(pop * (desiredOilShaftW / thermalEfficiency)) * dtYears;

        double actualFossilThermalWattsPerCap = 0.0;
        if (desiredCoalThermalGJ > 1.0e-9 || desiredOilThermalGJ > 1.0e-9) {
            double actualCoalT = Math.min(coalStock, desiredCoalThermalGJ / COAL_LHV_GJ_PER_TONNE);
            double actualOilT = Math.min(oilStock, desiredOilThermalGJ / OIL_LHV_GJ_PER_TONNE);
            acc.dResourceCoal -= (float) actualCoalT;
            acc.dResourceOil -= (float) actualOilT;
            double actualFossilThermalGJ = actualCoalT * COAL_LHV_GJ_PER_TONNE + actualOilT * OIL_LHV_GJ_PER_TONNE;
            actualFossilThermalWattsPerCap = PhysicalUnitsHelper.gjPerYearToWatts(
                    actualFossilThermalGJ / Math.max(1.0e-6, dtYears)) / pop;
        }

        double exosomaticWattsPerCapita = woodThermalPowerW + animalPowerW + millPowerW + actualFossilThermalWattsPerCap;
        double targetExosomaticGJPerYr = PhysicalUnitsHelper.wattsToGjPerYear(pop * exosomaticWattsPerCapita);
        double curFireGJPerYr = buffer.getEnergyFire()[i];
        acc.dEnergyFire += (float) ((targetExosomaticGJPerYr - curFireGJPerYr) * Math.min(1.0, dtYears));
    }
}
