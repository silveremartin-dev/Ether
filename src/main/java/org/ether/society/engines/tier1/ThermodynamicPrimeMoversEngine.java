/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
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
 * @version 1.0.0-beta.2
 */
public class ThermodynamicPrimeMoversEngine implements ProceduralEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(ThermodynamicPrimeMoversEngine.class);

    @Override
    public String getName() {
        return "ThermodynamicPrimeMoversEngine";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        processPrimeMovers(cells, deltaYears);
    }

    /**
     * Executes one tick of thermodynamic prime mover power transduction across populated cells.
     *
     * @param cells      inhabited hexagonal cells
     * @param deltaYears time step fraction in years
     */
    public static void processPrimeMovers(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double capital = cell.getResourceCapitalOr(0.0);
            double capitalPerCapita = capital / pop;
            double metalStock = cell.getResourceMetalOr(0.0);
            double coalStock = cell.getResourceCoalOr(0.0);
            double oilStock = cell.getResourceOilOr(0.0);

            // 1. Somatic Muscle Labor: ~100-135 W human work output
            double somaticPower = 120.0;

            // 2. Draft Animal Power: governed by livestock biomass per capita
            double livestockBiomass = cell.getBiomassLivestockOr(0.0);
            double livestockPerCapita = livestockBiomass / pop;
            double animalPower = Math.min(150.0, livestockPerCapita * 0.45); // up to ~150 W/cap

            // 3. Kinetic Water & Wind: requires carpentry / milling capital (k_pc > 30 kg/cap)
            double riverFlow = Math.max(0.0, cell.getAccessibleAquiferOr(0.0) * 0.1);
            double millPower = 0.0;
            if (capitalPerCapita > 30.0) {
                double millCapitalRatio = Math.min(1.0, (capitalPerCapita - 30.0) / 200.0);
                millPower = 80.0 * millCapitalRatio * (1.0 + Math.tanh(riverFlow / 50.0));
            }

            // 4. Heat Engines: Carnot efficiency bounded by refractory metal cylinder metallurgy and fuel
            double thermalEnginePower = 0.0;
            if (capitalPerCapita > 200.0 && metalStock > 0.5) {
                // Efficiency rises from 0.01 (Newcomen) to ~0.35 (modern turbine) with precision capital
                double precisionMetallurgy = Math.min(1.0, (capitalPerCapita - 200.0) / 3000.0);
                double carnotEfficiency = 0.01 + 0.34 * precisionMetallurgy;

                double availableFuelEnthalpy = Math.tanh(coalStock / 10.0) + Math.tanh(oilStock / 5.0);
                thermalEnginePower = 2500.0 * carnotEfficiency * availableFuelEnthalpy;
            }

            double totalMechanicalWatts = somaticPower + animalPower + millPower + thermalEnginePower;
            cell.setEnergyFire(totalMechanicalWatts);
        }
    }
}
