/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Draft Animal Traction & Fodder Allocation Tradeoff Engine (Vaclav Smil, 2017; Wrigley, 2010).
 *
 * <p>Models the metabolic and agronomic competition between human food crops and draft animals
 * (oxen, draft horses, mules) in pre-industrial and early industrial agrarian societies:</p>
 * <ul>
 *   <li><b>Work Capacity Benefit:</b> Each draft animal delivers ~500 to 750 W of mechanical work,
 *       multiplying agricultural labor productivity (plowing, harrowing, transport).</li>
 *   <li><b>Caloric & Land Penalty:</b> Each working horse consumes 15,000 to 25,000 kcal/day (~60-100 MJ/day),
 *       requiring 1.0 to 1.5 hectares of fertile pasture/oats/hay, displacing direct human caloric production.</li>
 *   <li><b>Mechanical Substitution Phase (1900-1950):</b> Transition to fossil-fueled tractors (Coal/Diesel)
 *       eliminates draft animal fodder acreage, releasing up to 25% of arable land directly for human food.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class DraftAnimalFodderAllocationEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(DraftAnimalFodderAllocationEngine.class);

    @Override
    public String getName() {
        return "Draft Animal Traction & Fodder Allocation";
    }

    @Override
    public String getDescription() {
        return "Models the energetic trade-off between draft animal mechanical work multiplier and fodder land competition.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Draft Animal Energetics & Fodder Trade-Off (Smil 2017, Wrigley 2010)]
               • Mechanical Power:   P_draft = N_animals · 600 Watts
               • Work Multiplier:    Productivity_agri = Baseline · (1.0 + 0.45 · Animal_Labor_Ratio)
               • Fodder Competition: Area_fodder = N_animals · (1.2 ha/horse)
               • Net Caloric Yield:  Food_net = Food_gross · (1.0 - 0.22 · Animal_Density_Share)
               • Motor Transition:   Tech > 4.5 (Internal Combustion) -> Fodder Land Freed (elimination of draft fodder preemption)
               Ref: V. Smil (2017) "Energy and Civilization: A History", MIT Press
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Agrarian Energetics";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop < 50) continue;

            double capital = cell.getResourceCapitalOr(10.0);
            double kpc = capital / pop;
            double powerW = org.ether.society.engines.tier1.PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double food = cell.getFoodResourceOr(1000.0);

            // Continuous draft animal presence index phi_draft in [0, 1] (Smil 2017; Wrigley 2010):
            // Rises with agrarian harness/plow capital (kpc >= 40 kg/cap) and phases out with internal combustion tractor power (p >= 1400 W/cap)
            double draftPresence = (1.0 / (1.0 + Math.exp(-(kpc - 40.0) / 15.0)))
                    * (1.0 / (1.0 + Math.exp((powerW - 1400.0) / 300.0)));

            if (draftPresence > 0.02) {
                // Draft animal adoption scales with capital per capita
                double animalRatio = Math.min(0.35, (capital / (pop * 20.0)));
                // Fodder consumption reduces net food available for humans by up to 20%
                double fodderCostFraction = 0.20 * animalRatio * draftPresence;
                // Mechanical labor boosts capital creation
                double productivityBoost = 0.02 * animalRatio * draftPresence * deltaYears;

                cell.setFoodResource(Math.max(0.0, food * Math.exp(-fodderCostFraction * deltaYears * 0.1)));
                cell.setResourceCapital(capital + (capital * productivityBoost));
            } else if (powerW >= 1400.0 || kpc >= 2800.0) {
                // Mechanization transition (Wrigley 2010; Smil 2017): internal combustion tractors replace horses,
                // reclaiming fodder acreage smoothly as mechanical power p >= 1400 W/cap or k_pc >= 2800 kg/cap
                double agriBiomass = cell.getBiomassAgricultureOr(0.0);
                if (agriBiomass > 0.0) {
                    double tractorSubstitution = 1.0 / (1.0 + Math.exp(-(kpc - 2800.0) / 700.0));
                    double reclamationBoost = Math.min(0.02 * agriBiomass, 0.015 * agriBiomass * tractorSubstitution * deltaYears);
                    cell.setBiomassAgriculture(agriBiomass + reclamationBoost);
                }
            }
        }
    }
}
