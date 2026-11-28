/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
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
 * <h1>Thermodynamic Prime Movers Engine</h1>
 * <p>
 * Models the historical and thermodynamic evolution of prime movers converting primary environmental
 * exergy (somatic food, biomass combustion, kinetic fluid flows, fossil chemical enthalpy, and nuclear fission)
 * into useful mechanical shaft power and electrical work.
 * </p>
 *
 * <h2>Physical &amp; Historical Foundations</h2>
 * <ul>
 *   <li><b>Vaclav Smil (2017)</b> <i>Energy and Civilization: A History</i>, MIT Press.</li>
 *   <li><b>Carlo M. Cipolla (1962)</b> <i>The Economic History of World Population</i>, Penguin.</li>
 *   <li><b>Robert U. Ayres &amp; Benjamin Warr (2009)</b> <i>The Economic Growth Engine: How Energy and Work Drive Material Prosperity</i>, Edward Elgar.</li>
 *   <li><b>Sadi Carnot (1824)</b> Maximum thermodynamic thermal engine conversion limit:
 *       $$\eta_{\text{Carnot}} = 1 - \frac{T_C}{T_H}$$</li>
 * </ul>
 *
 * <h2>Mechanistic Prime Mover Regimes</h2>
 * <ol>
 *   <li><b>Somatic Muscular Work</b>: Human basal metabolism ($\sim 100\text{ W}$) + muscle conversion efficiency ($\eta \approx 15\text{--}20\%$).
 *       Traction animals (oxen $\sim 300\text{ W}$, horses $\sim 500\text{ W}$) constrained by livestock biomass.</li>
 *   <li><b>Kinetic Fluid Wheels</b>: Waterwheels (undershot $\eta \approx 20\%$, breastshot $\eta \approx 50\%$, overshot $\eta \approx 75\%$)
 *       and windmills, constrained by local river flow (Manning-Strickler) and surface wind velocity.</li>
 *   <li><b>Heat Engines</b>:
 *     <ul>
 *       <li>Atmospheric Savery / Newcomen steam engines: $\eta \approx 0.5\text{--}1.0\%$ (requires high fuel, limited to coal pithead drainage).</li>
 *       <li>Watt separate condenser &amp; high-pressure Cornish engines: $\eta \approx 3\text{--}8\%$, requires precision iron cylinder boring.</li>
 *       <li>Triple-expansion marine steam &amp; Parsons steam turbines: $\eta \approx 15\text{--}35\%$, enabling long-distance bulk transit.</li>
 *       <li>Internal combustion engines &amp; combined-cycle gas turbines: $\eta \approx 35\text{--}55\%$.</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class ThermodynamicPrimeMoversEngine extends AbstractSimulationEngine {
    public ThermodynamicPrimeMoversEngine() {
        super("ThermodynamicPrimeMoversEngine", "ThermodynamicPrimeMoversEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(ThermodynamicPrimeMoversEngine.class);

    @Override
    public String getName() {
        return "ThermodynamicPrimeMoversEngine";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) {
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

        // 1. Renewable domestic firewood/biomass hearth thermal baseline (~212 W/cap, Smil 2017)
        double woodBiomassKg = org.ether.society.model.PhysicalUnitsHelper.metricTonnesToKg(
                Math.max(0.0, buffer.getBiomassNatural()[i]) + Math.max(0.0, buffer.getWoodResource()[i]));
        double woodThermalPowerW = woodBiomassKg > 0.0 ? 212.0 * Math.tanh((woodBiomassKg + 50.0) / 50.0) : 0.0;

        // 2. Domesticated draft animal traction (oxen/horses/mules)
        double livestockPerCapita = Math.max(0.0, buffer.getBiomassLivestock()[i] / pop);
        double animalPowerW = 65.0 * Math.tanh(livestockPerCapita / 0.25)
                * org.ether.society.core.math.SmoothMath.sigmoid(capitalPerCapita, 25.0, 12.0);

        // 3. Smooth C^infty hydraulic waterwheels & mechanical mills (activates above k_pc ~ 120 kg/cap)
        double riverFlow = Math.max(0.0, buffer.getFreshwaterAquifer()[i] * 0.1);
        double millCapitalActivation = Math.tanh(
                org.ether.society.core.math.SmoothMath.softplus(capitalPerCapita - 120.0, 0.04) / 300.0);
        double millPowerW = 80.0 * millCapitalActivation * (1.0 + Math.tanh(riverFlow / 50.0));

        // 4. Smooth C^infty industrial heat engines (Smil 2017; Ayres & Warr 2009):
        //    - Coal steam boilers & pistons require precision iron metallurgy (k_pc >= 950 kg/cap)
        //    - Petroleum internal combustion & turbines require industrial refining & machining (k_pc >= 2,800 kg/cap)
        double metallurgyGate = Math.tanh(metalStock / 2.0);
        double coalSteamActivation = metallurgyGate * Math.tanh(
                org.ether.society.core.math.SmoothMath.softplus(capitalPerCapita - 950.0, 0.025) / 1800.0);
        double oilIceActivation = metallurgyGate * Math.tanh(
                org.ether.society.core.math.SmoothMath.softplus(capitalPerCapita - 2800.0, 0.015) / 3500.0);

        double coalAvail = Math.tanh(coalStock / 10.0);
        double oilAvail = Math.tanh(oilStock / 5.0);

        // Carnot-bounded thermal efficiency eta_th in [0.03, 0.38] rising smoothly with precision capital
        double carnotEfficiency = 0.03 + 0.35 * Math.tanh(
                org.ether.society.core.math.SmoothMath.softplus(capitalPerCapita - 950.0, 0.02) / 3500.0);

        // Desired useful shaft work W_shaft [W/cap], converted via First/Second Law Q_in = W_shaft / eta_th
        double desiredCoalShaftW = 220.0 * coalSteamActivation * coalAvail;
        double desiredOilShaftW = 520.0 * oilIceActivation * oilAvail;

        double desiredCoalThermalGJ = org.ether.society.model.PhysicalUnitsHelper.wattsToGjPerYear(
                pop * (desiredCoalShaftW / carnotEfficiency)) * dtYears;
        double desiredOilThermalGJ = org.ether.society.model.PhysicalUnitsHelper.wattsToGjPerYear(
                pop * (desiredOilShaftW / carnotEfficiency)) * dtYears;

        double actualFossilThermalWattsPerCap = 0.0;
        if (desiredCoalThermalGJ > 1.0e-9 || desiredOilThermalGJ > 1.0e-9) {
            double actualCoalT = Math.min(coalStock, desiredCoalThermalGJ / 24.0);
            double actualOilT = Math.min(oilStock, desiredOilThermalGJ / 42.0);
            acc.dResourceCoal -= (float) actualCoalT;
            acc.dResourceOil -= (float) actualOilT;
            double actualFossilThermalGJ = actualCoalT * 24.0 + actualOilT * 42.0;
            actualFossilThermalWattsPerCap = org.ether.society.model.PhysicalUnitsHelper.gjPerYearToWatts(
                    actualFossilThermalGJ / Math.max(1.0e-6, dtYears)) / pop;
        }

        // Extensive cellular exosomatic combustion & prime-mover flux target in GJ/yr (WorldBuffer.energyFire)
        double exosomaticWattsPerCapita = woodThermalPowerW + animalPowerW + millPowerW + actualFossilThermalWattsPerCap;
        double targetExosomaticGJPerYr = org.ether.society.model.PhysicalUnitsHelper.wattsToGjPerYear(pop * exosomaticWattsPerCapita);
        double curFireGJPerYr = buffer.getEnergyFire()[i];
        acc.dEnergyFire += (float) ((targetExosomaticGJPerYr - curFireGJPerYr) * Math.min(1.0, dtYears));
    }
}
