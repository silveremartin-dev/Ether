/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
 * @version 1.0.0-beta.3
 */
public class DraftAnimalFodderAllocationEngine extends AbstractSimulationEngine {
    public DraftAnimalFodderAllocationEngine() {
        super("DraftAnimalFodderAllocationEngine", "DraftAnimalFodderAllocationEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }
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
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getBiomassHuman() == null || dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop < 50.0) return;

        double capital = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 10.0;
        double kpc = capital / pop;
        double fire = buffer.getEnergyFire() != null ? buffer.getEnergyFire()[i] : 0.0;
        double wind = buffer.getEnergyWind() != null ? buffer.getEnergyWind()[i] : 0.0;
        double solar = buffer.getEnergySolar() != null ? buffer.getEnergySolar()[i] : 0.0;
        double foodE = buffer.getEnergyFoodConsumed() != null ? buffer.getEnergyFoodConsumed()[i] : 0.0;
        double powerW = Math.max(85.0, ((fire + wind + solar + foodE) * 31.688) / pop);
        double food = buffer.getFoodResource() != null ? buffer.getFoodResource()[i] : 1000.0;

        double draftPresence = (1.0 / (1.0 + Math.exp(-(kpc - 40.0) / 15.0)))
                * (1.0 / (1.0 + Math.exp((powerW - 1400.0) / 300.0)));

        if (draftPresence > 0.02) {
            double animalRatio = Math.min(0.35, (capital / (pop * 20.0)));
            double fodderCostFraction = 0.20 * animalRatio * draftPresence;
            double productivityBoost = 0.02 * animalRatio * draftPresence * dtYears;
            acc.dFoodResource -= (float) (food * (1.0 - Math.exp(-fodderCostFraction * dtYears * 0.1)));
            acc.dResourceCapital += (float) (capital * productivityBoost);
        } else if (powerW >= 1400.0 || kpc >= 2800.0) {
            double agriBiomass = buffer.getBiomassAgriculture() != null ? buffer.getBiomassAgriculture()[i] : 0.0;
            if (agriBiomass > 0.0) {
                double tractorSubstitution = 1.0 / (1.0 + Math.exp(-(kpc - 2800.0) / 700.0));
                double reclamationBoost = Math.min(0.02 * agriBiomass, 0.015 * agriBiomass * tractorSubstitution * dtYears);
                acc.dBiomassAgriculture += (float) reclamationBoost;
            }
        }
    }
}
